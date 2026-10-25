package com.primetinder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ScoreStats(
    val best: Int = 0,
    val todayBest: Int = 0,
    val dailyStreak: Int = 0,
)

interface ScoreStore {
    val stats: Flow<ScoreStats>

    /** Records a finished run, updating both the all-time and today's best. */
    suspend fun recordScore(score: Int)

    /** Called once when the app opens; advances or resets the daily streak. */
    suspend fun registerVisit(todayEpochDay: Long)
}

private val Context.scoreDataStore by preferencesDataStore(name = "prime_tinder_scores")

class DataStoreScoreStore(
    private val context: Context,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : ScoreStore {

    override val stats: Flow<ScoreStats> =
        context.scoreDataStore.data.map { preferences ->
            val today = epochDayOf(clockMillis())
            val storedDay = preferences[TodayEpochDayKey]
            ScoreStats(
                best = preferences[BestScoreKey] ?: 0,
                todayBest = if (storedDay == today) preferences[TodayScoreKey] ?: 0 else 0,
                dailyStreak = preferences[StreakDaysKey] ?: 0,
            )
        }

    override suspend fun recordScore(score: Int) {
        val today = epochDayOf(clockMillis())
        context.scoreDataStore.edit { preferences ->
            if (score > (preferences[BestScoreKey] ?: 0)) {
                preferences[BestScoreKey] = score
            }
            if (preferences[TodayEpochDayKey] != today) {
                preferences[TodayEpochDayKey] = today
                preferences[TodayScoreKey] = score
            } else if (score > (preferences[TodayScoreKey] ?: 0)) {
                preferences[TodayScoreKey] = score
            }
        }
    }

    override suspend fun registerVisit(todayEpochDay: Long) {
        context.scoreDataStore.edit { preferences ->
            val lastPlayed = preferences[LastPlayedDayKey]
            val streak = DailyStreak.next(
                current = preferences[StreakDaysKey] ?: 0,
                lastPlayedDay = lastPlayed,
                today = todayEpochDay,
            )
            preferences[StreakDaysKey] = streak
            preferences[LastPlayedDayKey] = todayEpochDay

            if (preferences[TodayEpochDayKey] != todayEpochDay) {
                preferences[TodayEpochDayKey] = todayEpochDay
                preferences[TodayScoreKey] = 0
            }
        }
    }

    private companion object {
        val BestScoreKey = intPreferencesKey("best_score")
        val TodayScoreKey = intPreferencesKey("today_score")
        val TodayEpochDayKey = longPreferencesKey("today_epoch_day")
        val StreakDaysKey = intPreferencesKey("streak_days")
        val LastPlayedDayKey = longPreferencesKey("last_epoch_day")
    }
}
