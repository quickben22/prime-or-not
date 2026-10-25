package com.primetinder.data

/** Daily streak maths kept pure so it can be unit-tested without DataStore. */
object DailyStreak {

    /**
     * Streak length after playing on [today].
     *
     * - same day as [lastPlayedDay] → unchanged
     * - the day after → [current] + 1
     * - any longer gap (or first ever visit) → 1
     */
    fun next(current: Int, lastPlayedDay: Long?, today: Long): Int = when {
        lastPlayedDay == null -> 1
        lastPlayedDay == today -> current.coerceAtLeast(1)
        lastPlayedDay == today - 1 -> current.coerceAtLeast(0) + 1
        else -> 1
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

/** UTC epoch day for [millis]. Avoids java.time, which needs API 26 at minSdk 24. */
fun epochDayOf(millis: Long): Long = Math.floorDiv(millis, MILLIS_PER_DAY)
