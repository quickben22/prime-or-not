package com.primetinder.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.primetinder.R
import com.primetinder.game.GameCard
import com.primetinder.game.Guess
import com.primetinder.ui.theme.PrimeCard
import com.primetinder.ui.theme.PrimeInk
import com.primetinder.ui.theme.PrimeNegativeInk
import com.primetinder.ui.theme.PrimePositiveInk
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SWIPE_THRESHOLD_RATIO = 0.32f
private const val MAX_ROTATION_DEGREES = 12f

@Composable
fun SwipeCard(
    card: GameCard,
    widthPx: Float,
    enabled: Boolean,
    onGuess: (Guess) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val currentOnGuess by rememberUpdatedState(onGuess)

    var offsetX by remember(card.value) { mutableFloatStateOf(0f) }
    var settleJob by remember(card.value) { mutableStateOf<Job?>(null) }
    var committed by remember(card.value) { mutableStateOf(false) }
    var thresholdCrossed by remember(card.value) { mutableStateOf(false) }
    val entryScale = remember(card.value) { Animatable(0.96f) }

    LaunchedEffect(card.value) {
        entryScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing),
        )
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
                translationX = offsetX
                rotationZ = rotation
                scaleX = entryScale.value
                scaleY = entryScale.value
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
        shadowElevation = 14.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6E8EC)),
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

            Text(
                text = card.value.toString(),
                style = MaterialTheme.typography.displayLarge,
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
                .rotate(rotationDegrees)
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
