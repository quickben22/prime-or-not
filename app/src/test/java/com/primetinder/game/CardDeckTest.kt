package com.primetinder.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDeckTest {

    @Test
    fun staysInsideTheTierRange() {
        val lowDeck = CardDeck(Random(1))
        repeat(40) {
            assertTrue(lowDeck.nextCard(0).value in 2..99)
        }

        val midDeck = CardDeck(Random(2))
        repeat(40) {
            assertTrue(midDeck.nextCard(10).value in 100..999)
        }

        val highDeck = CardDeck(Random(3))
        repeat(40) {
            assertTrue(highDeck.nextCard(25).value in 1_000..9_999)
        }
    }

    @Test
    fun neverRepeatsWithinARun() {
        val deck = CardDeck(Random(11))
        val values = (1..60).map { deck.nextCard(0).value }

        assertEquals(60, values.toSet().size)
    }

    @Test
    fun resetAllowsValuesAgain() {
        val deck = CardDeck(Random(5))
        val first = deck.nextCard(0)
        deck.reset()

        val afterReset = (1..40).map { deck.nextCard(0).value }
        assertTrue(first.value in afterReset)
    }

    @Test
    fun cardTypeMatchesItsValue() {
        val deck = CardDeck(Random(21))
        repeat(120) {
            val card = deck.nextCard(10)
            assertEquals(card.isPrime, PrimeChecker.isPrime(card.value))
        }
    }

    @Test
    fun deckIsRoughlyHalfPrime() {
        var primes = 0
        var total = 0

        repeat(8) { run ->
            val deck = CardDeck(Random(100L + run))
            repeat(40) {
                if (deck.nextCard(10).isPrime) primes++
                total++
            }
        }

        val ratio = primes.toDouble() / total
        assertTrue("prime ratio was $ratio", ratio in 0.42..0.58)
    }
}
