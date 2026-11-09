package com.companyname.primeornot.game

import kotlin.random.Random

/** The three answers a Practice card offers. "Neither" exists for the number 1. */
enum class PracticeAnswer {
    Prime,
    Composite,
    Neither,
}

/**
 * Generates the five-card Practice deck.
 *
 * Rules enforced here:
 * - exactly five distinct numbers, all in 1..100
 * - at least two primes and at least two composites
 * - at least one trap composite (9, 15, 21, 25, 27, 49, 91)
 * - 1 only ever appears as the fifth card
 *
 * The trap is chosen first so no random draw can produce a deck without one.
 */
object PracticeDeck {

    val TRAP_NUMBERS: List<Int> = listOf(9, 15, 21, 25, 27, 49, 91)

    const val SIZE = 5
    const val MAX_PRACTICE_NUMBER = 100

    private val primes: List<Int> =
        (2..MAX_PRACTICE_NUMBER).filter(PrimeChecker::isPrime)

    private val composites: List<Int> =
        (4..MAX_PRACTICE_NUMBER).filter { !PrimeChecker.isPrime(it) }

    /**
     * @param includeOne when true the last card is 1, and the first four cards
     * are exactly two primes and two composites (one of them a trap).
     */
    fun generate(
        random: Random = Random.Default,
        includeOne: Boolean = random.nextBoolean(),
    ): List<GameCard> {
        val trap = TRAP_NUMBERS[random.nextInt(TRAP_NUMBERS.size)]
        val primeCount = if (includeOne) 2 else if (random.nextBoolean()) 2 else 3
        val compositeCount = SIZE - primeCount - if (includeOne) 1 else 0

        val pickedComposites = buildList {
            add(trap)
            composites
                .filter { it !in TRAP_NUMBERS }
                .shuffled(random)
                .take(compositeCount - 1)
                .forEach(::add)
        }
        val pickedPrimes = primes.shuffled(random).take(primeCount)

        val cards = (pickedPrimes + pickedComposites).shuffled(random)
        val numbers = if (includeOne) cards + 1 else cards
        check(numbers.size == SIZE && numbers.toSet().size == SIZE)
        return numbers.map(::GameCard)
    }

    fun correctAnswerFor(card: GameCard): PracticeAnswer = correctAnswerFor(card.value)

    fun correctAnswerFor(value: Int): PracticeAnswer = when {
        value == 1 -> PracticeAnswer.Neither
        PrimeChecker.isPrime(value) -> PracticeAnswer.Prime
        else -> PracticeAnswer.Composite
    }
}
