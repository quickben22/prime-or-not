package com.companyname.primeornot.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStreakTest {

    @Test
    fun firstVisitStartsTheStreak() {
        assertEquals(1, DailyStreak.next(current = 0, lastPlayedDay = null, today = 20_000L))
    }

    @Test
    fun sameDayKeepsTheStreak() {
        assertEquals(4, DailyStreak.next(current = 4, lastPlayedDay = 20_000L, today = 20_000L))
    }

    @Test
    fun yesterdayExtendsTheStreak() {
        assertEquals(5, DailyStreak.next(current = 4, lastPlayedDay = 19_999L, today = 20_000L))
    }

    @Test
    fun aGapResetsTheStreak() {
        assertEquals(1, DailyStreak.next(current = 9, lastPlayedDay = 19_990L, today = 20_000L))
    }

    @Test
    fun epochDayCountsUtcMidnights() {
        assertEquals(0L, epochDayOf(0L))
        assertEquals(0L, epochDayOf(86_399_999L))
        assertEquals(1L, epochDayOf(86_400_000L))
        assertEquals(-1L, epochDayOf(-1L))
    }
}
