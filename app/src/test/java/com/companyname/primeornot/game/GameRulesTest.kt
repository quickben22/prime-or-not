package com.companyname.primeornot.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {

    @Test
    fun tiersAdvanceWithScore() {
        assertEquals(0, GameRules.tierForScore(0))
        assertEquals(0, GameRules.tierForScore(9))
        assertEquals(1, GameRules.tierForScore(10))
        assertEquals(1, GameRules.tierForScore(24))
        assertEquals(2, GameRules.tierForScore(25))
        assertEquals(2, GameRules.tierForScore(500))
    }

    @Test
    fun rangesMatchTiers() {
        assertEquals(2..99, GameRules.rangeForScore(0))
        assertEquals(100..999, GameRules.rangeForScore(10))
        assertEquals(1_000..9_999, GameRules.rangeForScore(25))
    }

    @Test
    fun timeLimitGrowsWithDigits() {
        assertEquals(5_000L, GameRules.timeLimitMsFor(7))
        assertEquals(5_000L, GameRules.timeLimitMsFor(97))
        assertEquals(6_000L, GameRules.timeLimitMsFor(997))
        assertEquals(7_000L, GameRules.timeLimitMsFor(9_973))
    }

    @Test
    fun correctnessMatchesPrimality() {
        val prime = GameCard(97)
        val composite = GameCard(91)

        assertTrue(GameRules.isCorrect(prime, Guess.Prime))
        assertFalse(GameRules.isCorrect(prime, Guess.NotPrime))
        assertTrue(GameRules.isCorrect(composite, Guess.NotPrime))
        assertFalse(GameRules.isCorrect(composite, Guess.Prime))
    }
}
