package com.primetinder.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimeCheckerTest {

    @Test
    fun identifiesPrimeNumbers() {
        listOf(2, 3, 5, 7, 89, 97, 7919).forEach { value ->
            assertTrue("$value should be prime", PrimeChecker.isPrime(value))
        }
    }

    @Test
    fun identifiesCompositeNumbers() {
        listOf(4, 9, 91, 100, 9_999).forEach { value ->
            assertFalse("$value should not be prime", PrimeChecker.isPrime(value))
        }
    }

    @Test
    fun rejectsValuesBelowTwo() {
        listOf(-7, 0, 1).forEach { value ->
            assertFalse("$value should not be prime", PrimeChecker.isPrime(value))
        }
    }

    @Test
    fun fallsBackToTrialDivisionAboveSieveLimit() {
        assertTrue(PrimeChecker.isPrime(100_003))
        assertFalse(PrimeChecker.isPrime(100_001)) // 11 x 9091
    }

    @Test
    fun factorizesNumbers() {
        assertEquals(listOf(7, 13), PrimeChecker.factorization(91))
        assertEquals(listOf(97), PrimeChecker.factorization(97))
        assertEquals(listOf(2, 2, 2, 3, 3, 5), PrimeChecker.factorization(360))
        assertEquals(emptyList<Int>(), PrimeChecker.factorization(1))
    }

    @Test
    fun formatsFactorization() {
        assertEquals("7 × 13", PrimeChecker.factorizationText(91))
        assertEquals("2³ × 3² × 5", PrimeChecker.factorizationText(360))
        assertEquals("2⁴ × 5", PrimeChecker.factorizationText(80))
        assertEquals("11²", PrimeChecker.factorizationText(121))
    }
}
