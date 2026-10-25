package com.primetinder.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primetinder.R
import com.primetinder.game.GameCard
import com.primetinder.game.Guess
import com.primetinder.ui.theme.PrimeAccent
import com.primetinder.ui.theme.PrimeBackground
import com.primetinder.ui.theme.PrimeBackgroundTop
import com.primetinder.ui.theme.PrimeCard
import com.primetinder.ui.theme.PrimeInk
import com.primetinder.ui.theme.PrimeNegative
import com.primetinder.ui.theme.PrimeNegativeInk
import com.primetinder.ui.theme.PrimePositiveInk
import com.primetinder.ui.theme.PrimeTextMuted
import com.primetinder.ui.theme.PrimeTextPrimary
import com.primetinder.ui.theme.PrimeTinderTheme
import com.primetinder.ui.theme.PrimeTrack

private const val LOW_TIME_RATIO = 0.3f

@Composable
fun GameScreen(
    state: GameUiState,
    onSwipe: (Guess) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    onStart: () -> Unit = {},
) {
    val view = LocalView.current
    var previousScore by remember { mutableIntStateOf(state.score) }
    var previousPhase by remember { mutableStateOf(state.phase) }

    LaunchedEffect(state.score, state.phase) {
        when {
            state.score > previousScore -> view.performHapticFeedback(hapticConfirm())
            state.phase == GamePhase.GameOver && previousPhase == GamePhase.Playing ->
                view.performHapticFeedback(hapticReject())
        }
        previousScore = state.score
        previousPhase = state.phase
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimeBackgroundTop, PrimeBackground)))
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Hud(state)
            Spacer(Modifier.height(18.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val cardWidth = minOf(maxWidth * 0.92f, (maxHeight - 16.dp) * 0.72f)
                val cardWidthPx = with(LocalDensity.current) { cardWidth.toPx() }

                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .aspectRatio(0.72f),
                ) {
                    if (state.phase == GamePhase.Ready) {
                        ReadyCard(
                            onStart = onStart,
                            modifier = Modifier.matchParentSize(),
                        )
                    } else {
                        state.nextCard?.let { upcoming ->
                            CardFace(
                                card = upcoming,
                                modifier = Modifier
                                    .matchParentSize()
                                    .scale(0.94f)
                                    .alpha(0.45f),
                            )
                        }
                        state.card?.let { card ->
                            SwipeCard(
                                card = card,
                                widthPx = cardWidthPx,
                                enabled = state.phase == GamePhase.Playing,
                                onGuess = onSwipe,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.swipe_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimeTextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
        }

        AnimatedVisibility(
            visible = state.phase == GamePhase.GameOver,
            enter = fadeIn(animationSpec = tween(180)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(140)),
            modifier = Modifier.fillMaxSize(),
        ) {
            GameOverPanel(
                state = state,
                onRestart = onRestart,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ReadyCard(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .testTag("readyCard")
            .clickable(onClick = onStart),
        shape = RoundedCornerShape(28.dp),
        color = PrimeCard,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, Color(0xFFE6E8EC)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 22.dp),
        ) {
            Text(
                text = stringResource(R.string.card_prompt),
                style = MaterialTheme.typography.labelSmall,
                color = PrimeInk.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.TopCenter),
            )

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(PrimeAccent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "P",
                        color = PrimeBackground,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.tap_to_start),
                    color = PrimeInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.tap_to_start_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimeInk.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.not_prime_edge_hint),
                    color = PrimeNegativeInk.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = stringResource(R.string.prime_edge_hint),
                    color = PrimePositiveInk.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun Hud(state: GameUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(R.string.score_label),
                style = MaterialTheme.typography.labelSmall,
                color = PrimeTextMuted,
            )
            Text(
                text = state.score.toString(),
                color = PrimeTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.testTag("scoreValue"),
            )
        }

        Spacer(Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.best_label),
                style = MaterialTheme.typography.labelSmall,
                color = PrimeTextMuted,
            )
            Text(
                text = state.bestScore.toString(),
                color = PrimeTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.testTag("bestValue"),
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    TimerBar(state)
}

@Composable
private fun TimerBar(state: GameUiState) {
    val animatedFraction by animateFloatAsState(
        targetValue = state.timeFraction,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "timeFraction",
    )
    val seconds = ((state.remainingMs + 999L) / 1000L).toInt()
    val lowTime = state.timeFraction <= LOW_TIME_RATIO
    val barColor = if (lowTime) PrimeNegative else PrimeAccent
    val timerDescription = pluralStringResource(R.plurals.timer_description, seconds, seconds)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(PrimeTrack)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(state.timeFraction, 0f..1f)
                    contentDescription = timerDescription
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(barColor.copy(alpha = 0.7f), barColor),
                        ),
                    ),
            )
        }

        Spacer(Modifier.width(10.dp))
        Text(
            text = "${seconds}s",
            color = if (lowTime) PrimeNegative else PrimeTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End,
        )
    }
}

private fun hapticConfirm(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.LONG_PRESS
    }

private fun hapticReject(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.REJECT
    } else {
        HapticFeedbackConstants.LONG_PRESS
    }

@Preview(showBackground = true, backgroundColor = 0xFF101820, widthDp = 380, heightDp = 820)
@Composable
private fun GameScreenPreview() {
    PrimeTinderTheme {
        GameScreen(
            state = GameUiState(
                card = GameCard(997),
                nextCard = GameCard(91),
                score = 7,
                bestScore = 21,
                remainingMs = 3_400,
                timeLimitMs = 6_000,
            ),
            onSwipe = {},
            onRestart = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101820, widthDp = 380, heightDp = 820)
@Composable
private fun GameOverPreview() {
    PrimeTinderTheme {
        GameScreen(
            state = GameUiState(
                phase = GamePhase.GameOver,
                card = GameCard(91),
                nextCard = GameCard(997),
                score = 12,
                bestScore = 21,
                remainingMs = 2_400,
                timeLimitMs = 6_000,
                gameOverReason = GameOverReason.WrongGuess,
                missedCard = GameCard(91),
            ),
            onSwipe = {},
            onRestart = {},
        )
    }
}
