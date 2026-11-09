package com.companyname.primeornot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companyname.primeornot.game.GameRules
import com.companyname.primeornot.game.PrimeChecker
import com.companyname.primeornot.ui.theme.PrimeAccent
import com.companyname.primeornot.ui.theme.PrimeSurface
import com.companyname.primeornot.ui.theme.PrimeTextMuted
import com.companyname.primeornot.ui.theme.PrimeTextPrimary

data class TeachingTip(val title: String, val body: String)

/**
 * Divisibility content for the whole Classic range.
 *
 * sqrt([GameRules.MAX_CLASSIC_NUMBER]) is just under 100, so the complete set of
 * possible prime divisors is 2..97. The quick rules cover the small primes and
 * the last tip lists the rest for plain division; every rule is stated in a form
 * that is equivalent to "is n divisible by p" so no composite can slip through.
 */
object TeachingTips {

    private val primeListBody: String = run {
        val primes = PrimeChecker.primesUpTo(GameRules.MAX_CLASSIC_PRIME_DIVISOR)
        val leading = primes.takeWhile { it <= 23 }
        val remaining = primes.drop(leading.size)
        "The largest number in the game is ${GameRules.MAX_CLASSIC_NUMBER}, so any " +
            "prime divisor is at most ${GameRules.MAX_CLASSIC_PRIME_DIVISOR}. Rules above " +
            "cover ${leading.joinToString(", ")}. For the rest, divide by " +
            "${remaining.joinToString(", ")} and stop at the square root."
    }

    val all: List<TeachingTip> = listOf(
        TeachingTip(
            "Divisible by 2?",
            "Look at the last digit. If it is 0, 2, 4, 6 or 8, the number is even and " +
                "therefore composite (2 itself is prime).",
        ),
        TeachingTip(
            "Divisible by 3?",
            "Add the digits. If the sum is a multiple of 3, so is the number. " +
                "For example 4 + 9 + 1 = 14, so 491 is not divisible by 3.",
        ),
        TeachingTip(
            "Divisible by 5?",
            "If the last digit is 0 or 5, the number is divisible by 5. That makes it " +
                "composite unless the number is 5 itself.",
        ),
        TeachingTip(
            "Divisible by 7?",
            "Double the last digit and subtract it from the rest. If the result is 0 or " +
                "a multiple of 7, so is the number. 91 → 9 − 2 = 7, and 91 = 7 × 13.",
        ),
        TeachingTip(
            "Divisible by 11?",
            "Add and subtract the digits alternately. 407 → 4 − 0 + 7 = 11, so " +
                "407 = 11 × 37. A result of 0 or a multiple of 11 also works.",
        ),
        TeachingTip(
            "Divisible by 13?",
            "Add four times the last digit to the rest. 91 → 9 + 4 = 13, so " +
                "91 = 7 × 13. 143 → 14 + 12 = 26, also a multiple of 13.",
        ),
        TeachingTip(
            "Divisible by 17?",
            "Subtract five times the last digit from the rest. 187 → 18 − 35 = −17, " +
                "and 187 = 11 × 17.",
        ),
        TeachingTip(
            "Divisible by 19?",
            "Add twice the last digit to the rest. 209 → 20 + 18 = 38 = 2 × 19, so " +
                "209 = 11 × 19.",
        ),
        TeachingTip(
            "Divisible by 23?",
            "Add seven times the last digit to the rest. 253 → 25 + 21 = 46 = 2 × 23, " +
                "so 253 = 11 × 23.",
        ),
        TeachingTip(
            "Primes up to 97",
            primeListBody,
        ),
        TeachingTip(
            "Why stop at the square root?",
            "If a number n = a × b with a ≤ b, then a ≤ √n. So if no prime up to √n " +
                "divides n, then n is prime. For this game √n is always below 100.",
        ),
        TeachingTip(
            "The number 1",
            "1 has only one positive divisor, itself. It is neither prime nor composite, " +
                "which is why 1 is never prime and never composite.",
        ),
    )

    fun rotatingTip(index: Int): TeachingTip = all[Math.floorMod(index, all.size)]

    /** A tip that explains the missed card, or a general rule for large factors. */
    fun forNumber(value: Int): TeachingTip {
        if (value < 2) return all.last()
        if (PrimeChecker.isPrime(value)) {
            return TeachingTip(
                title = "$value is prime",
                body = "No prime up to √$value divides it (the largest you ever need is " +
                    "${GameRules.MAX_CLASSIC_PRIME_DIVISOR}), so its only positive divisors " +
                    "are 1 and itself.",
            )
        }

        return when (PrimeChecker.smallestPrimeFactor(value)) {
            2 -> all[0]
            3 -> all[1]
            5 -> all[2]
            7 -> all[3]
            11 -> all[4]
            13 -> all[5]
            17 -> all[6]
            19 -> all[7]
            23 -> all[8]
            else -> all[9]
        }
    }
}

/** Reusable, non-blocking teaching card used in How to Play, Practice and Classic. */
@Composable
fun TeachingTipCard(
    tip: TeachingTip,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("teachingTip"),
        shape = RoundedCornerShape(18.dp),
        color = PrimeSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.28f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TIP",
                    color = PrimeAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tip.title,
                    color = PrimeTextPrimary,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = tip.body,
                color = PrimeTextMuted,
                fontSize = if (compact) 11.sp else 12.sp,
                lineHeight = if (compact) 15.sp else 17.sp,
                maxLines = if (compact) 2 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Small, line-height-matched helper so screens do not repeat the style. */
@Composable
fun TeachingTipHeadline(tip: TeachingTip, modifier: Modifier = Modifier) {
    Text(
        text = tip.title,
        style = MaterialTheme.typography.titleLarge,
        color = PrimeTextPrimary,
        modifier = modifier,
    )
}
