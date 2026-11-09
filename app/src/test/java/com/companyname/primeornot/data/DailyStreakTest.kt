package com.companyname.primeornot.data

import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStreakTest {

    private val day1 = CalendarDay(2026, 11, 9)
    private val day2 = CalendarDay(2026, 11, 10)

    @Test
    fun firstQualifyingGameStartsTheStreak() {
        val result = DailyStreak.afterQualifyingGame(
            current = 0,
            longest = 0,
            lastDay = null,
            today = day1,
        )
        assertEquals(1, result.current)
        assertEquals(1, result.longest)
    }

    @Test
    fun sameDayKeepsTheStreak() {
        val result = DailyStreak.afterQualifyingGame(
            current = 4,
            longest = 6,
            lastDay = day1,
            today = day1,
        )
        assertEquals(4, result.current)
        assertEquals(6, result.longest)
    }

    @Test
    fun yesterdayExtendsTheStreak() {
        val result = DailyStreak.afterQualifyingGame(
            current = 4,
            longest = 4,
            lastDay = day1,
            today = day2,
        )
        assertEquals(5, result.current)
        assertEquals(5, result.longest)
    }

    @Test
    fun aGapResetsTheCurrentStreakButKeepsTheLongest() {
        val result = DailyStreak.afterQualifyingGame(
            current = 9,
            longest = 12,
            lastDay = CalendarDay(2026, 11, 1),
            today = day2,
        )
        assertEquals(1, result.current)
        assertEquals(12, result.longest)
    }

    @Test
    fun displayedStreakExpiresAfterAMissedDay() {
        assertEquals(4, DailyStreak.displayed(4, day1, day1))
        assertEquals(4, DailyStreak.displayed(4, day1, day2))
        assertEquals(0, DailyStreak.displayed(4, day1, CalendarDay(2026, 11, 11)))
        assertEquals(0, DailyStreak.displayed(4, null, day1))
    }

    @Test
    fun calendarDaysRoundTrip() {
        assertEquals(0L, CalendarDay(1970, 1, 1).epochDay)
        assertEquals(CalendarDay(1970, 1, 1), CalendarDay.fromEpochDay(0))
        assertEquals(day1, CalendarDay.fromEpochDay(day1.epochDay))
        assertEquals(day2, CalendarDay.fromEpochDay(day1.epochDay + 1))
    }

    @Test
    fun calendarDayParsesIsoSafely() {
        assertEquals(day1, CalendarDay.parse("2026-11-09"))
        assertEquals(null, CalendarDay.parse(null))
        assertEquals(null, CalendarDay.parse(""))
        assertEquals(null, CalendarDay.parse("not-a-date"))
        assertEquals(null, CalendarDay.parse("2026-13-09"))
    }

    @Test
    fun todayUsesTheDeviceTimeZone() {
        val millis = Instant.parse("2026-11-09T00:30:00Z").toEpochMilli()

        assertEquals(CalendarDay(2026, 11, 9), CalendarDay.today(millis, TimeZone.getTimeZone("UTC")))
        assertEquals(CalendarDay(2026, 11, 8), CalendarDay.today(millis, TimeZone.getTimeZone("GMT-5")))
    }
}
