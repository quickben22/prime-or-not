package com.companyname.primeornot.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.TimeZone

/** Local persistence for Classic records, streak, onboarding and the active run. */
interface GameStore {
    val records: Flow<ClassicRecords>
    val activeSession: Flow<ActiveClassicSession?>

    /** Converts version-1 score data into the version-2 record schema. */
    suspend fun migrateIfNeeded()

    /** Records a completed Classic run exactly once and drops the active session. */
    suspend fun completeClassicGame(session: ActiveClassicSession, completedAtMillis: Long)

    /** Persists an in-progress Classic run so it can be resumed. */
    suspend fun saveActiveSession(session: ActiveClassicSession)

    /** Manual abandonment, or discarding a saved run shown on the home screen. */
    suspend fun clearActiveSession(sessionId: String? = null)

    suspend fun setOnboardingComplete(complete: Boolean = true)
}

private val Context.gameDataStore by preferencesDataStore(name = "prime_or_not_scores")

class DataStoreGameStore(
    private val context: Context,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val timeZone: TimeZone = TimeZone.getDefault(),
) : GameStore {

    override val records: Flow<ClassicRecords> = context.gameDataStore.data.map { preferences ->
        readRecords(preferences, today())
    }.catch { emit(ClassicRecords()) }

    override val activeSession: Flow<ActiveClassicSession?> = context.gameDataStore.data.map {
        RecordCodec.decodeSession(it[ActiveSessionKey])
    }.catch { emit(null) }

    override suspend fun migrateIfNeeded() {
        val today = today()
        context.gameDataStore.edit { preferences ->
            val schema = preferences[SchemaVersionKey] ?: LEGACY_SCHEMA_VERSION
            if (schema >= ClassicRecords.SCHEMA_VERSION) return@edit

            val legacyStreak = (preferences[StreakDaysKey] ?: 0).coerceAtLeast(0)
            val legacyLastDay = preferences[LastPlayedDayKey]?.let(CalendarDay::fromEpochDay)

            if (legacyLastDay != null && legacyLastDay.isTodayOrYesterday(today)) {
                preferences[CurrentStreakKey] = legacyStreak.coerceAtLeast(1)
                preferences[LastQualifyingDayKey] = legacyLastDay.toIso()
            } else {
                preferences[CurrentStreakKey] = 0
            }
            preferences[LongestStreakKey] =
                maxOf(preferences[LongestStreakKey] ?: 0, legacyStreak)
            preferences[OnboardingKey] = preferences[OnboardingKey] ?: hasPriorPlay(preferences)
            preferences[SchemaVersionKey] = ClassicRecords.SCHEMA_VERSION
        }
    }

    override suspend fun completeClassicGame(
        session: ActiveClassicSession,
        completedAtMillis: Long,
    ) {
        val today = CalendarDay.today(completedAtMillis, timeZone)
        context.gameDataStore.edit { preferences ->
            val current = readRecords(preferences, today)
            val duplicate = current.lastCompletedGameId == session.id ||
                current.recentGames.any { it.id == session.id }

            if (duplicate) {
                preferences.remove(ActiveSessionKey)
                return@edit
            }

            val correct = session.correct.coerceAtLeast(0)
            val incorrect = session.incorrect.coerceAtLeast(0)
            val game = CompletedGame(
                id = session.id,
                completedAtMillis = completedAtMillis,
                score = session.score.coerceAtLeast(0),
                correct = correct,
                incorrect = incorrect,
                bestStreak = session.bestStreak.coerceAtLeast(0),
            )

            val streak = if (game.qualifiesForStreak) {
                DailyStreak.afterQualifyingGame(
                    current = current.currentDailyStreak,
                    longest = current.longestDailyStreak,
                    lastDay = current.lastQualifyingDay,
                    today = today,
                )
            } else {
                DailyStreak.unchanged(
                    current = current.currentDailyStreak,
                    longest = current.longestDailyStreak,
                )
            }

            preferences[BestScoreKey] = maxOf(current.bestScore, game.score)
            preferences[LongestCorrectKey] =
                maxOf(current.longestCorrectStreak, game.bestStreak)
            preferences[TotalGamesKey] = current.totalGames + 1
            preferences[TotalCorrectKey] = current.totalCorrect + correct
            preferences[TotalIncorrectKey] = current.totalIncorrect + incorrect
            preferences[CurrentStreakKey] = streak.current
            preferences[LongestStreakKey] = streak.longest
            if (game.qualifiesForStreak) {
                preferences[LastQualifyingDayKey] = today.toIso()
            }
            preferences[RecentGamesKey] = RecordCodec.encodeRecent(
                (listOf(game) + current.recentGames).take(MAX_RECENT_GAMES),
            )
            preferences[LastCompletedIdKey] = session.id
            preferences[SchemaVersionKey] = ClassicRecords.SCHEMA_VERSION
            preferences.remove(ActiveSessionKey)
        }
    }

    override suspend fun saveActiveSession(session: ActiveClassicSession) {
        context.gameDataStore.edit { preferences ->
            if (preferences[LastCompletedIdKey] == session.id) return@edit
            val recent = RecordCodec.decodeRecent(preferences[RecentGamesKey])
            if (recent.any { it.id == session.id }) return@edit
            preferences[ActiveSessionKey] = RecordCodec.encodeSession(session)
            preferences[SchemaVersionKey] = ClassicRecords.SCHEMA_VERSION
        }
    }

    override suspend fun clearActiveSession(sessionId: String?) {
        context.gameDataStore.edit { preferences ->
            val stored = RecordCodec.decodeSession(preferences[ActiveSessionKey])
            if (sessionId == null || stored?.id == sessionId) {
                preferences.remove(ActiveSessionKey)
            }
        }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        context.gameDataStore.edit { preferences ->
            preferences[OnboardingKey] = complete
            preferences[SchemaVersionKey] = ClassicRecords.SCHEMA_VERSION
        }
    }

    private fun today(): CalendarDay = CalendarDay.today(clockMillis(), timeZone)

    /**
     * Reads the effective record. Version-1 installations have no aggregate
     * counters, so those start at zero; the best score and a still-valid streak
     * are carried over so an update never wipes a record.
     */
    private fun readRecords(preferences: Preferences, today: CalendarDay): ClassicRecords {
        val best = (preferences[BestScoreKey] ?: 0).coerceAtLeast(0)
        val onboarding = preferences[OnboardingKey] ?: hasPriorPlay(preferences)
        val legacyStreak = (preferences[StreakDaysKey] ?: 0).coerceAtLeast(0)
        val legacyLastDay = preferences[LastPlayedDayKey]?.let(CalendarDay::fromEpochDay)
        val legacyStreakIsActive =
            legacyLastDay != null && legacyLastDay.isTodayOrYesterday(today)

        val storedLastDay = CalendarDay.parse(preferences[LastQualifyingDayKey])
        val lastQualifyingDay = storedLastDay ?: legacyLastDay?.takeIf { legacyStreakIsActive }
        val currentStreak = preferences[CurrentStreakKey]
            ?: if (legacyStreakIsActive) legacyStreak else 0
        val longestStreak = maxOf(
            preferences[LongestStreakKey] ?: 0,
            legacyStreak,
            currentStreak,
        )

        return ClassicRecords(
            schemaVersion = preferences[SchemaVersionKey] ?: LEGACY_SCHEMA_VERSION,
            bestScore = best,
            longestCorrectStreak = (preferences[LongestCorrectKey] ?: 0).coerceAtLeast(0),
            totalGames = (preferences[TotalGamesKey] ?: 0).coerceAtLeast(0),
            totalCorrect = (preferences[TotalCorrectKey] ?: 0).coerceAtLeast(0),
            totalIncorrect = (preferences[TotalIncorrectKey] ?: 0).coerceAtLeast(0),
            currentDailyStreak = currentStreak.coerceAtLeast(0),
            longestDailyStreak = longestStreak.coerceAtLeast(0),
            lastQualifyingDay = lastQualifyingDay,
            recentGames = RecordCodec.decodeRecent(preferences[RecentGamesKey]),
            onboardingComplete = onboarding,
            lastCompletedGameId = preferences[LastCompletedIdKey],
        )
    }

    private fun hasPriorPlay(preferences: Preferences): Boolean =
        (preferences[BestScoreKey] ?: 0) > 0 ||
            (preferences[TodayScoreKey] ?: 0) > 0 ||
            (preferences[StreakDaysKey] ?: 0) > 0 ||
            (preferences[TotalGamesKey] ?: 0) > 0 ||
            (preferences[TotalCorrectKey] ?: 0) > 0 ||
            (preferences[TotalIncorrectKey] ?: 0) > 0 ||
            !preferences[RecentGamesKey].isNullOrBlank()

    private companion object {
        const val LEGACY_SCHEMA_VERSION = 1

        val SchemaVersionKey = intPreferencesKey("schema_version")
        val BestScoreKey = intPreferencesKey("best_score")
        val TodayScoreKey = intPreferencesKey("today_score")
        val StreakDaysKey = intPreferencesKey("streak_days")
        val LastPlayedDayKey = longPreferencesKey("last_epoch_day")

        val LongestCorrectKey = intPreferencesKey("longest_correct_streak")
        val TotalGamesKey = intPreferencesKey("total_games")
        val TotalCorrectKey = intPreferencesKey("total_correct")
        val TotalIncorrectKey = intPreferencesKey("total_incorrect")
        val CurrentStreakKey = intPreferencesKey("current_daily_streak")
        val LongestStreakKey = intPreferencesKey("longest_daily_streak")
        val LastQualifyingDayKey = stringPreferencesKey("last_qualifying_day")
        val RecentGamesKey = stringPreferencesKey("recent_games")
        val OnboardingKey = booleanPreferencesKey("onboarding_complete")
        val ActiveSessionKey = stringPreferencesKey("active_classic_session")
        val LastCompletedIdKey = stringPreferencesKey("last_completed_game_id")
    }
}
