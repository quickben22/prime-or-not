package com.companyname.primeornot.data

/** Number of answered cards a completed Classic run needs to qualify for the streak. */
const val STREAK_QUALIFYING_ANSWERS = 5

/** How many completed Classic games My Records keeps. */
const val MAX_RECENT_GAMES = 20

/** One completed Classic run, newest first in [ClassicRecords.recentGames]. */
data class CompletedGame(
    val id: String,
    val completedAtMillis: Long,
    val score: Int,
    val correct: Int,
    val incorrect: Int,
    val bestStreak: Int,
) {
    val answered: Int get() = correct + incorrect

    /** Null when the run ended before any card was answered. */
    val accuracyPercent: Double?
        get() = if (answered <= 0) null else correct * 100.0 / answered

    /** A completed run qualifies only when enough cards were actually answered. */
    val qualifiesForStreak: Boolean get() = answered >= STREAK_QUALIFYING_ANSWERS
}

/** Everything My Records and the HUD need. Classic mode only. */
data class ClassicRecords(
    val schemaVersion: Int = SCHEMA_VERSION,
    val bestScore: Int = 0,
    val longestCorrectStreak: Int = 0,
    val totalGames: Int = 0,
    val totalCorrect: Int = 0,
    val totalIncorrect: Int = 0,
    val currentDailyStreak: Int = 0,
    val longestDailyStreak: Int = 0,
    val lastQualifyingDay: CalendarDay? = null,
    val recentGames: List<CompletedGame> = emptyList(),
    val onboardingComplete: Boolean = false,
    val lastCompletedGameId: String? = null,
) {
    val totalAnswered: Int get() = totalCorrect + totalIncorrect

    /** Aggregate accuracy, or null before the first answered card. */
    val accuracyPercent: Double?
        get() = if (totalAnswered <= 0) null else totalCorrect * 100.0 / totalAnswered

    fun currentStreakOn(today: CalendarDay): Int =
        DailyStreak.displayed(currentDailyStreak, lastQualifyingDay, today)

    companion object {
        const val SCHEMA_VERSION = 2
    }
}

/**
 * A Classic run in progress. Persisted after answers and when the app leaves the
 * foreground so an interruption can be resumed instead of being recorded as a
 * completed game.
 */
data class ActiveClassicSession(
    val id: String,
    val cardValue: Int,
    val nextCardValue: Int,
    val score: Int,
    val streak: Int,
    val bestStreak: Int,
    val cardsSeen: Int,
    val correct: Int,
    val incorrect: Int,
    val remainingMs: Long,
    val timeLimitMs: Long,
    val bestAtRunStart: Int,
    val fastestAnswerMs: Long?,
    val isNewBest: Boolean,
    val usedValues: List<Int>,
)
