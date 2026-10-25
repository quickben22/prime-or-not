package com.companyname.primeornot.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companyname.primeornot.R
import com.companyname.primeornot.game.GameCard
import com.companyname.primeornot.game.Guess
import com.companyname.primeornot.ui.theme.PrimeCard
import com.companyname.primeornot.ui.theme.PrimeCardBottom
import com.companyname.primeornot.ui.theme.PrimeCardEdge
import com.companyname.primeornot.ui.theme.PrimeCardNumberStyle
import com.companyname.primeornot.ui.theme.PrimeInk
import com.companyname.primeornot.ui.theme.PrimeNegativeInk
import com.companyname.primeornot.ui.theme.PrimePositiveInk
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SWIPE_THRESHOLD_RATIO = 0.32f
private const val MAX_ROTATION_DEGREES = 12f
private const val SHAKE_DP = 6f

/**
 * The top card of the stack.
 *
 * [shakeTrigger] is a monotonically increasing counter; every change plays a
 * short horizontal shake. GameScreen bumps it when a miss is reported so the
 * failure is felt before the game-over panel covers the screen.
 */
@Composable
fun SwipeCard(
    card: GameCard,
    widthPx: Float,
    enabled: Boolean,
    onGuess: (Guess) -> Unit,
    modifier: Modifier = Modifier,
    shakeTrigger: Int = 0,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val currentOnGuess by rememberUpdatedState(onGuess)
    val density = LocalDensity.current

    var offsetX by remember(card.value) { mutableFloatStateOf(0f) }
    var settleJob by remember(card.value) { mutableStateOf<Job?>(null) }
    var committed by remember(card.value) { mutableStateOf(false) }
    var thresholdCrossed by remember(card.value) { mutableStateOf(false) }

    // Deal animation: the card drops in slightly small and transparent, then
    // springs to full size. Keyed on the value, so every card feels dealt.
    val entryScale = remember(card.value) { Animatable(0.90f) }
    val entryAlpha = remember(card.value) { Animatable(0f) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(card.value) {
        launch { entryAlpha.animateTo(1f, tween(durationMillis = 110)) }
        entryScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        )
    }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger <= 0) return@LaunchedEffect
        val amplitude = with(density) { SHAKE_DP.dp.toPx() }
        repeat(3) { index ->
            val step = amplitude * (1f - index * 0.22f)
            shake.animateTo(step, tween(durationMillis = 44, easing = LinearEasing))
            shake.animateTo(-step, tween(durationMillis = 44, easing = LinearEasing))
        }
        shake.animateTo(0f, tween(durationMillis = 60, easing = FastOutSlowInEasing))
    }

    DisposableEffect(card.value) {
        onDispose { settleJob?.cancel() }
    }

    fun animateOffsetTo(target: Float, onFinished: () -> Unit = {}) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue = offsetX,
                targetValue = target,
                animationSpec = if (target == 0f) {
                    spring(dampingRatio = 0.72f, stiffness = 520f)
                } else {
                    tween(durationMillis = 190, easing = FastOutLinearInEasing)
                },
            ) { value, _ -> offsetX = value }
            onFinished()
        }
    }

    fun commit(guess: Guess) {
        if (!enabled || committed) return
        committed = true
        thresholdCrossed = false
        val direction = if (guess == Guess.Prime) 1f else -1f
        animateOffsetTo(direction * widthPx * 1.5f) { currentOnGuess(guess) }
    }

    val progress = (abs(offsetX) / (widthPx * SWIPE_THRESHOLD_RATIO)).coerceIn(0f, 1f)
    val rotation = (offsetX / widthPx) * MAX_ROTATION_DEGREES
    val cardDescription = stringResource(R.string.card_description, card.value)
    val primeAction = stringResource(R.string.action_prime)
    val notPrimeAction = stringResource(R.string.action_not_prime)

    Box(
        modifier = modifier
            .testTag("card")
            .graphicsLayer {
                translationX = offsetX + shake.value
                rotationZ = rotation
                scaleX = entryScale.value
                scaleY = entryScale.value
                alpha = entryAlpha.value
            }
            .semantics {
                contentDescription = cardDescription
                customActions = listOf(
                    CustomAccessibilityAction(primeAction) {
                        commit(Guess.Prime)
                        true
                    },
                    CustomAccessibilityAction(notPrimeAction) {
                        commit(Guess.NotPrime)
                        true
                    },
                )
            }
            .pointerInput(card.value, enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        settleJob?.cancel()
                        thresholdCrossed = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!committed) {
                            change.consume()
                            offsetX += dragAmount
                            val threshold = widthPx * SWIPE_THRESHOLD_RATIO
                            val crossed = abs(offsetX) >= threshold
                            if (crossed && !thresholdCrossed) {
                                thresholdCrossed = true
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            } else if (!crossed && thresholdCrossed) {
                                thresholdCrossed = false
                            }
                        }
                    },
                    onDragEnd = {
                        if (!committed) {
                            val threshold = widthPx * SWIPE_THRESHOLD_RATIO
                            when {
                                offsetX >= threshold -> commit(Guess.Prime)
                                offsetX <= -threshold -> commit(Guess.NotPrime)
                                else -> {
                                    thresholdCrossed = false
                                    animateOffsetTo(0f)
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        thresholdCrossed = false
                        animateOffsetTo(0f)
                    },
                )
            },
    ) {
        CardFace(card = card, modifier = Modifier.fillMaxSize())

        SwipeStamp(
            text = stringResource(R.string.prime_label),
            color = PrimePositiveInk,
            alpha = if (offsetX > 0f) progress else 0f,
            alignment = Alignment.CenterStart,
            rotationDegrees = -12f,
        )
        SwipeStamp(
            text = stringResource(R.string.not_prime_label),
            color = PrimeNegativeInk,
            alpha = if (offsetX < 0f) progress else 0f,
            alignment = Alignment.CenterEnd,
            rotationDegrees = 12f,
        )
    }
}

@Composable
fun CardFace(card: GameCard, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = PrimeCard,
        shadowElevation = 18.dp,
        border = BorderStroke(1.dp, PrimeCardEdge),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(PrimeCard, PrimeCardBottom)))
                .drawWithCache {
                    val highlight = Color.White.copy(alpha = 0.95f)
                    onDrawWithContent {
                        drawContent()
                        // Inner top highlight: the "light catching the edge" cue.
                        drawLine(
                            color = highlight,
                            start = Offset(0f, 1f),
                            end = Offset(size.width, 1f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
                .padding(horizontal = 22.dp, vertical = 22.dp),
        ) {
            Text(
                text = stringResource(R.string.card_prompt),
                style = MaterialTheme.typography.labelSmall,
                color = PrimeInk.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.TopCenter),
            )

            Text(
                text = card.value.toString(),
                style = PrimeCardNumberStyle,
                color = PrimeInk,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )

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
private fun SwipeStamp(
    text: String,
    color: Color,
    alpha: Float,
    alignment: Alignment,
    rotationDegrees: Float,
) {
    if (alpha <= 0.01f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = alignment,
    ) {
        Text(
            text = text,
            color = color.copy(alpha = alpha),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier
                .graphicsLayer { rotationZ = rotationDegrees }
                .border(3.dp, color.copy(alpha = alpha), RoundedCornerShape(10.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            color.copy(alpha = alpha * 0.16f),
                            color.copy(alpha = alpha * 0.06f),
                        ),
                    ),
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
