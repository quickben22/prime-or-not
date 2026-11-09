package com.companyname.primeornot.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeDeckTest {

    @Test
    fun everyDeckSatisfiesTheCompositionRules() {
        repeat(500) { seed ->
            listOf(true, false).forEachIndexed { variant, includeOne ->
                val cards = PracticeDeck.generate(
                    random = Random(seed * 2 + variant),
                    includeOne = includeOne,
                )
                val values = cards.map { it.value }

                assertEquals("seed $seed", PracticeDeck.SIZE, cards.size)
                assertEquals("seed $seed duplicates", PracticeDeck.SIZE, values.toSet().size)
                assertTrue("seed $seed range", values.all { it in 1..PracticeDeck.MAX_PRACTICE_NUMBER })
                assertTrue("seed $seed primes", cards.count { it.isPrime } >= 2)
                assertTrue(
                    "seed $seed composites",
                    cards.count { !it.isPrime && it.value > 1 } >= 2,
                )
                assertTrue(
                    "seed $seed trap",
                    values.any { it in PracticeDeck.TRAP_NUMBERS },
                )
                assertEquals(
                    "seed $seed single trap",
                    1,
                    values.count { it in PracticeDeck.TRAP_NUMBERS },
                )

                if (includeOne) {
                    assertEquals("seed $seed fifth card", 1, values.last())
                    assertEquals("seed $seed single one", 1, values.count { it == 1 })
                    assertTrue(
                        "seed $seed first four",
                        cards.take(4).count { it.isPrime } == 2 &&
                            cards.take(4).count { !it.isPrime } == 2,
                    )
                } else {
                    assertTrue("seed $seed no one", values.none { it == 1 })
                }
            }
        }
    }

    @Test
    fun trapsAreAllCompositeAndInRange() {
        assertEquals(listOf(9, 15, 21, 25, 27, 49, 91), PracticeDeck.TRAP_NUMBERS)
        PracticeDeck.TRAP_NUMBERS.forEach { value ->
            assertTrue("$value should be composite", !PrimeChecker.isPrime(value))
            assertTrue(value in 4..PracticeDeck.MAX_PRACTICE_NUMBER)
        }
    }

    @Test
    fun correctAnswerMappingMatchesClassic() {
        assertEquals(PracticeAnswer.Neither, PracticeDeck.correctAnswerFor(1))
        assertEquals(PracticeAnswer.Prime, PracticeDeck.correctAnswerFor(2))
        assertEquals(PracticeAnswer.Composite, PracticeDeck.correctAnswerFor(4))
        assertEquals(PracticeAnswer.Composite, PracticeDeck.correctAnswerFor(9))
        assertEquals(PracticeAnswer.Prime, PracticeDeck.correctAnswerFor(97))
        assertEquals(PracticeAnswer.Composite, PracticeDeck.correctAnswerFor(100))

        (1..PracticeDeck.MAX_PRACTICE_NUMBER).forEach { value ->
            val expected = when {
                value == 1 -> PracticeAnswer.Neither
                PrimeChecker.isPrime(value) -> PracticeAnswer.Prime
                else -> PracticeAnswer.Composite
            }
            assertEquals("$value", expected, PracticeDeck.correctAnswerFor(GameCard(value)))
        }
    }

    @Test
    fun maxClassicNumberAndPrimeDivisorAreConsistent() {
        assertEquals(GameRules.MAX_CLASSIC_NUMBER, GameRules.tierRanges.last().last)
        assertEquals(97, GameRules.MAX_CLASSIC_PRIME_DIVISOR)

        assertTrue(GameRules.MAX_CLASSIC_PRIME_DIVISOR * GameRules.MAX_CLASSIC_PRIME_DIVISOR <=
            GameRules.MAX_CLASSIC_NUMBER)
        assertTrue(101 * 101 > GameRules.MAX_CLASSIC_NUMBER)

        // Every composite in the supported range has a prime factor <= 97, so
        // the teaching rules can never miss a divisor.
        (2..GameRules.MAX_CLASSIC_NUMBER).forEach { value ->
            if (!PrimeChecker.isPrime(value)) {
                val factor = PrimeChecker.smallestPrimeFactor(value)
                assertTrue("$value", factor != null && factor <= GameRules.MAX_CLASSIC_PRIME_DIVISOR)
            }
        }
    }
}
