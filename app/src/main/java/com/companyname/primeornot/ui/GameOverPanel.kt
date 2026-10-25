package com.companyname.primeornot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companyname.primeornot.R
import com.companyname.primeornot.game.PrimeChecker
import com.companyname.primeornot.ui.theme.PrimeAccent
import com.companyname.primeornot.ui.theme.PrimeBackground
import com.companyname.primeornot.ui.theme.PrimeScoreStyle
import com.companyname.primeornot.ui.theme.PrimeSurface
import com.companyname.primeornot.ui.theme.PrimeTextMuted
import com.companyname.primeornot.ui.theme.PrimeTextPrimary

private val PanelHorizontalPadding = 22.dp
private val BottomContentPadding = 10.dp

/**
 * Full-screen retry screen: scrim, the run summary, and an optional banner slot
 * pinned to the bottom.
 *
 * [bottomContent] is null in previews and tests, so the panel never depends on
 * an ad. When present it sits outside the panel surface — a banner directly
 * under the restart button invites accidental clicks.
 */
@Composable
fun GameOverPanel(
    state: GameUiState,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier) {
        // Scrim and tap blocker live behind the content: taps outside the panel
        // still do nothing, while the banner slot below stays tappable.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .pointerInput(Unit) { detectTapGestures { } },
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = PanelHorizontalPadding),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gameOverPanel"),
                    shape = RoundedCornerShape(28.dp),
                    color = PrimeSurface,
                    tonalElevation = 6.dp,
                ) {
                    GameOverContent(state = state, onRestart = onRestart)
                }
            }

            if (bottomContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = BottomContentPadding),
                ) {
                    bottomContent()
                }
            }
        }
    }
}

@Composable
private fun GameOverContent(
    state: GameUiState,
    onRestart: () -> Unit,
) {
    val missed = state.missedCard
    val title = when (state.gameOverReason) {
        GameOverReason.TimeUp -> stringResource(R.string.game_over_timeout_title)
        else -> stringResource(R.string.game_over_wrong_title)
    }
    val answer = when {
        missed == null -> ""
        missed.isPrime -> stringResource(R.string.answer_prime, missed.value)
        else -> stringResource(
            R.string.answer_composite,
            missed.value,
            PrimeChecker.factorizationText(missed.value),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isNewBest) {
            Surface(
                shape = RoundedCornerShape(50),
                color = PrimeAccent,
            ) {
                Text(
                    text = stringResource(R.string.new_best_label),
                    color = PrimeBackground,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .testTag("panelNewBest")
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = PrimeTextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = answer,
            style = MaterialTheme.typography.headlineMedium,
            color = PrimeTextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(22.dp))
        HorizontalDivider(color = PrimeTextPrimary.copy(alpha = 0.10f))
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ResultStat(
                label = stringResource(R.string.score_label),
                value = state.score.toString(),
                tag = "summaryScore",
            )
            ResultStat(
                label = stringResource(R.string.best_label),
                value = state.bestScore.toString(),
                tag = "summaryBest",
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ResultStat(
                label = stringResource(R.string.best_streak_label),
                value = state.bestStreak.toString(),
                tag = "summaryStreak",
                compact = true,
            )
            ResultStat(
                label = stringResource(R.string.accuracy_label),
                value = "${state.accuracyPercent}%",
                tag = "summaryAccuracy",
                compact = true,
            )
            ResultStat(
                label = stringResource(R.string.fastest_label),
                value = formatFastest(state.fastestAnswerMs),
                tag = "summaryFastest",
                compact = true,
            )
        }

        val toBeat = state.cardsToBeatBest
        if (toBeat != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.cards_to_beat_best,
                    toBeat,
                    toBeat,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimeTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("toBeatBest"),
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimeAccent,
                contentColor = PrimeBackground,
            ),
        ) {
            Text(
                text = stringResource(R.string.try_again),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun ResultStat(
    label: String,
    value: String,
    tag: String,
    compact: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PrimeTextMuted,
            fontSize = if (compact) 9.sp else 11.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            style = PrimeScoreStyle,
            fontSize = if (compact) 20.sp else 34.sp,
            color = PrimeTextPrimary,
            modifier = Modifier.testTag(tag),
        )
    }
}

/** Compact, locale-free formatting: "840 ms" under a second, "1.2s" above. */
private fun formatFastest(millis: Long?): String {
    if (millis == null) return "\u2014"
    if (millis < 1_000L) return "$millis ms"
    val tenths = (millis + 50L) / 100L
    return "${tenths / 10}.${tenths % 10}s"
}
