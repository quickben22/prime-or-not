package com.primetinder.game

import kotlin.random.Random

/**
 * Generates cards for a run.
 *
 * The answer is chosen before the number: a coin flip decides prime vs composite,
 * then a value is sampled from that tier's pool. This keeps the deck at 50/50
 * instead of inheriting the natural density of primes. Composites are weighted
 * toward harder cases (odd and not divisible by 5) so the game is not just
 * "is it even?". Values never repeat within a run.
 */
class CardDeck(private val random: Random = Random.Default) {

    private data class TierPool(
        val primes: IntArray,
        val hardComposites: IntArray,
        val easyComposites: IntArray,
        val all: IntArray,
    )

    private val used = mutableSetOf<Int>()
    private val pools: List<TierPool> by lazy { GameRules.tierRanges.map(::buildPool) }

    fun nextCard(score: Int): GameCard {
        val pool = pools[GameRules.tierForScore(score)]
        val value = if (random.nextBoolean()) {
            pickUnused(pool.primes)
                ?: pickUnused(pool.hardComposites)
                ?: pickUnused(pool.easyComposites)
                ?: pickAny(pool)
        } else {
            val preferHard = random.nextFloat() < HARD_COMPOSITE_WEIGHT
            val preferred = if (preferHard) pool.hardComposites else pool.easyComposites
            val other = if (preferHard) pool.easyComposites else pool.hardComposites
            pickUnused(preferred)
                ?: pickUnused(other)
                ?: pickUnused(pool.primes)
                ?: pickAny(pool)
        }

        checkNotNull(value) { "Card pool exhausted" }
        return GameCard(value)
    }

    fun reset() {
        used.clear()
    }

    /** Random starting point, then a linear scan so a nearly exhausted pool still yields a value. */
    private fun pickUnused(values: IntArray): Int? {
        if (values.isEmpty()) return null
        val start = random.nextInt(values.size)
        for (offset in values.indices) {
            val candidate = values[(start + offset) % values.size]
            if (used.add(candidate)) return candidate
        }
        return null
    }

    /** Last resort once a whole tier has been seen; only reachable on absurdly long runs. */
    private fun pickAny(pool: TierPool): Int? =
        pickUnused(pool.all) ?: run {
            used.removeAll(pool.all.toSet())
            pickUnused(pool.all)
        }

    private fun buildPool(range: IntRange): TierPool {
        val primes = mutableListOf<Int>()
        val hardComposites = mutableListOf<Int>()
        val easyComposites = mutableListOf<Int>()

        for (value in range) {
            if (value < 2) continue
            when {
                PrimeChecker.isPrime(value) -> primes += value
                value % 2 == 0 || value % 5 == 0 -> easyComposites += value
                else -> hardComposites += value
            }
        }

        return TierPool(
            primes = primes.toIntArray(),
            hardComposites = hardComposites.toIntArray(),
            easyComposites = easyComposites.toIntArray(),
            all = (primes + hardComposites + easyComposites).toIntArray(),
        )
    }

    private companion object {
        const val HARD_COMPOSITE_WEIGHT = 0.7f
    }
}
