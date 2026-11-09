package com.companyname.primeornot.data

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * A calendar date in the device's local time zone.
 *
 * Dates are compared through a time-zone-independent day number (Howard
 * Hinnant's civil-from-days algorithm), so "yesterday" stays one day away even
 * across daylight-saving changes. Kept as plain integers because java.time is
 * API 26 and the app supports API 24.
 */
data class CalendarDay(val year: Int, val month: Int, val day: Int) {

    /** Days since 1970-01-01 in the proleptic Gregorian calendar. */
    val epochDay: Long = daysFromCivil(year, month, day)

    fun toIso(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    fun isTodayOrYesterday(today: CalendarDay): Boolean =
        epochDay == today.epochDay || epochDay == today.epochDay - 1

    companion object {
        fun parse(value: String?): CalendarDay? {
            if (value.isNullOrBlank()) return null
            val parts = value.split('-')
            if (parts.size != 3) return null
            return runCatching {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                if (month !in 1..12 || day !in 1..31) null else CalendarDay(year, month, day)
            }.getOrNull()
        }

        fun fromEpochDay(epochDay: Long): CalendarDay {
            val z = epochDay + 719468L
            val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
            val doe = z - era * 146097L
            val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
            val year = yoe + era * 400L
            val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
            val mp = (5L * doy + 2L) / 153L
            val day = doy - (153L * mp + 2L) / 5L + 1L
            val month = mp + if (mp < 10L) 3L else -9L
            return CalendarDay(
                year = (year + if (month <= 2L) 1L else 0L).toInt(),
                month = month.toInt(),
                day = day.toInt(),
            )
        }

        /** The local calendar date at [clockMillis]. */
        fun today(
            clockMillis: Long,
            timeZone: TimeZone = TimeZone.getDefault(),
        ): CalendarDay {
            val calendar = Calendar.getInstance(timeZone)
            calendar.timeInMillis = clockMillis
            return CalendarDay(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH),
            )
        }

        private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
            val y = year - if (month <= 2) 1 else 0
            val era = if (y >= 0) y / 400 else (y - 399) / 400
            val yoe = y - era * 400
            val doy = (153 * (month + if (month > 2) -3 else 9) + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146097L + doe - 719468L
        }
    }
}

/** Daily-streak maths kept pure so it can be unit-tested without DataStore. */
object DailyStreak {

    data class Result(val current: Int, val longest: Int)

    /**
     * Streak after a *streak-qualifying* completed Classic game on [today].
     *
     * - first ever qualifying game → 1
     * - same day as [lastDay] → unchanged
     * - the day after → [current] + 1
     * - any longer gap → 1
     *
     * The longest streak never shrinks.
     */
    fun afterQualifyingGame(
        current: Int,
        longest: Int,
        lastDay: CalendarDay?,
        today: CalendarDay,
    ): Result {
        val next = when {
            lastDay == null -> 1
            lastDay == today -> current.coerceAtLeast(1)
            lastDay.epochDay == today.epochDay - 1 -> current.coerceAtLeast(0) + 1
            else -> 1
        }
        return Result(current = next, longest = maxOf(longest, next))
    }

    /** Used when a completed game does not qualify, so the streak is untouched. */
    fun unchanged(current: Int, longest: Int): Result = Result(current, longest)

    /**
     * The current streak as it should be displayed on [today]. A missed calendar
     * day resets the visible streak without rewriting stored data.
     */
    fun displayed(current: Int, lastDay: CalendarDay?, today: CalendarDay): Int = when {
        lastDay == null -> 0
        lastDay == today -> current.coerceAtLeast(0)
        lastDay.epochDay == today.epochDay - 1 -> current.coerceAtLeast(0)
        else -> 0
    }
}
