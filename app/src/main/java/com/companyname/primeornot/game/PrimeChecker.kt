package com.companyname.primeornot.game

/**
 * Primality and factorization. Backed by a sieve so deck generation and gameplay
 * never do repeated trial division for numbers in the supported range.
 */
object PrimeChecker {
    private const val SIEVE_LIMIT = 100_000

    private val primes: BooleanArray by lazy { buildSieve(SIEVE_LIMIT) }

    fun isPrime(value: Int): Boolean {
        if (value < 2) return false
        if (value <= SIEVE_LIMIT) return primes[value]
        return isPrimeByTrialDivision(value)
    }

    /** Prime factors in ascending order, e.g. 91 -> [7, 13]. Empty for values below 2. */
    fun factorization(value: Int): List<Int> {
        if (value < 2) return emptyList()

        val factors = mutableListOf<Int>()
        var remaining = value
        var divisor = 2
        while (divisor <= remaining / divisor) {
            while (remaining % divisor == 0) {
                factors += divisor
                remaining /= divisor
            }
            divisor = if (divisor == 2) 3 else divisor + 2
        }
        if (remaining > 1) factors += remaining
        return factors
    }

    /** Human-readable factors, e.g. 91 -> "7 × 13", 360 -> "2³ × 3² × 5". */
    fun factorizationText(value: Int): String =
        factorization(value)
            .groupingBy { it }
            .eachCount()
            .entries
            .joinToString(" × ") { (prime, power) ->
                if (power == 1) prime.toString() else "$prime${superscript(power)}"
            }

    /**
     * Unicode superscripts so the game-over answer reads "11²" rather than
     * "11^2". Only powers of a prime can reach two digits (2^30 is the largest
     * that fits an Int), and every digit has a superscript form.
     */
    private fun superscript(power: Int): String =
        power.toString().map { digit -> SUPERSCRIPT_DIGITS.getValue(digit) }.joinToString("")

    private val SUPERSCRIPT_DIGITS: Map<Char, Char> = mapOf(
        '0' to '\u2070', '1' to '\u00b9', '2' to '\u00b2', '3' to '\u00b3',
        '4' to '\u2074', '5' to '\u2075', '6' to '\u2076', '7' to '\u2077',
        '8' to '\u2078', '9' to '\u2079',
    )

    private fun isPrimeByTrialDivision(value: Int): Boolean {
        if (value % 2 == 0) return false
        var divisor = 3
        while (divisor <= value / divisor) {
            if (value % divisor == 0) return false
            divisor += 2
        }
        return true
    }

    private fun buildSieve(limit: Int): BooleanArray {
        val composite = BooleanArray(limit + 1)
        var divisor = 2
        while (divisor <= limit / divisor) {
            if (!composite[divisor]) {
                var multiple = divisor * divisor
                while (multiple <= limit) {
                    composite[multiple] = true
                    multiple += divisor
                }
            }
            divisor++
        }
        return BooleanArray(limit + 1) { it >= 2 && !composite[it] }
    }
}
