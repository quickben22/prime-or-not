package com.companyname.primeornot.game

object GameRules {
    const val BASE_TIME_LIMIT_MS = 5_000L
    const val EXTRA_DIGIT_TIME_MS = 1_000L

    private const val TIER_ONE_SCORE = 10
    private const val TIER_TWO_SCORE = 25

    val tierRanges: List<IntRange> = listOf(2..99, 100..999, 1_000..9_999)

    fun tierForScore(score: Int): Int = when {
        score < TIER_ONE_SCORE -> 0
        score < TIER_TWO_SCORE -> 1
        else -> 2
    }

    fun rangeForScore(score: Int): IntRange = tierRanges[tierForScore(score)]

    fun rangeForTier(tier: Int): IntRange = tierRanges[tier.coerceIn(0, tierRanges.lastIndex)]

    /** 5s for two digits, 6s for three, 7s for four. */
    fun timeLimitMsFor(value: Int): Long {
        val digits = value.toString().length.coerceAtLeast(2)
        return BASE_TIME_LIMIT_MS + (digits - 2) * EXTRA_DIGIT_TIME_MS
    }

    fun isCorrect(card: GameCard, guess: Guess): Boolean = when (guess) {
        Guess.Prime -> card.isPrime
        Guess.NotPrime -> !card.isPrime
    }
}
