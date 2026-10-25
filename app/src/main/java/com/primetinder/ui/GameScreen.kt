package com.primetinder.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.primetinder.ui.theme.PrimePositive
import com.primetinder.ui.theme.PrimePositiveInk
import com.primetinder.ui.theme.PrimeScoreStyle
import com.primetinder.ui.theme.PrimeSurface
import com.primetinder.ui.theme.PrimeTextMuted
import com.primetinder.ui.theme.PrimeTextPrimary
import com.primetinder.ui.theme.PrimeTinderTheme
import com.primetinder.ui.theme.PrimeTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlin.random.Random

private const val LOW_TIME_RATIO = 0.3f
private const val STREAK_CHIP_MIN = 3
private const val BANNER_VISIBLE_MS = 1_500L

@Composable
fun GameScreen(
    state: GameUiState,
    onSwipe: (Guess) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    onStart: () -> Unit = {},
    effects: Flow<GameEffect> = emptyFlow(),
    gameOverAd: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current

    var shakeTrigger by remember { mutableIntStateOf(0) }
    var plusOneTrigger by remember { mutableIntStateOf(0) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var bannerEffect by remember { mutableStateOf<GameEffect?>(null) }
    var bannerVisible by remember { mutableStateOf(false) }
    var bannerSerial by remember { mutableIntStateOf(0) }

    fun showBanner(effect: GameEffect) {
        bannerEffect = effect
        bannerSerial += 1
    }

    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                GameEffect.Swipe -> Unit

                is GameEffect.Correct -> {
                    plusOneTrigger += 1
                    view.performHapticFeedback(hapticConfirm())
                }

                GameEffect.Wrong -> {
                    shakeTrigger += 1
                    view.performHapticFeedback(hapticReject())
                }

                GameEffect.NewBest -> showBanner(effect)

                is GameEffect.TierUp -> showBanner(effect)

                is GameEffect.Milestone -> {
                    showBanner(effect)
                    confettiTrigger += 1
                }

                GameEffect.Tick -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    // Resolved during composition so the strings follow configuration changes.
    val bannerText = when (val effect = bannerEffect) {
        is GameEffect.TierUp -> stringResource(R.string.banner_level_up) + " \u00b7 " +
            stringResource(tierNameRes(effect.tier))
        is GameEffect.Milestone -> stringResource(R.string.banner_milestone, effect.score)
        GameEffect.NewBest -> stringResource(R.string.new_best_label)
        else -> ""
    }

    LaunchedEffect(bannerSerial) {
        if (bannerSerial == 0) return@LaunchedEffect
        bannerVisible = true
        delay(BANNER_VISIBLE_MS)
        bannerVisible = false
    }

    LaunchedEffect(state.phase) {
        if (state.phase == GamePhase.Ready) bannerVisible = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimeBackgroundTop, PrimeBackground)))
            .safeDrawingPadding(),
    ) {
        LivingBackground(state = state, modifier = Modifier.matchParentSize())

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
                        // Third card: a blank surface so the stack reads as endless.
                        StackCard(
                            scale = 0.92f,
                            alpha = 0.75f,
                            modifier = Modifier
                                .matchParentSize()
                                .offset(y = (-34).dp),
                        )
                        state.nextCard?.let { upcoming ->
                            CardFace(
                                card = upcoming,
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(y = (-18).dp)
                                    .scale(0.96f)
                                    .alpha(0.95f),
                            )
                        }
                        state.card?.let { card ->
                            SwipeCard(
                                card = card,
                                widthPx = cardWidthPx,
                                enabled = state.phase == GamePhase.Playing,
                                onGuess = onSwipe,
                                shakeTrigger = shakeTrigger,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                        PlusOne(trigger = plusOneTrigger, modifier = Modifier.matchParentSize())
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

        ConfettiBurst(trigger = confettiTrigger, modifier = Modifier.matchParentSize())

        AnimatedVisibility(
            visible = bannerVisible,
            enter = fadeIn(animationSpec = tween(120)) +
                scaleIn(
                    initialScale = 0.82f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
                ),
            exit = fadeOut(animationSpec = tween(240)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 118.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PrimeAccent,
                shadowElevation = 10.dp,
            ) {
                Text(
                    text = bannerText,
                    color = PrimeBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .testTag("banner")
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = state.phase == GamePhase.GameOver,
            enter = fadeIn(animationSpec = tween(200)) +
                scaleIn(
                    initialScale = 0.86f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 380f,
                    ),
                ),
            exit = fadeOut(animationSpec = tween(140)),
            modifier = Modifier.fillMaxSize(),
        ) {
            GameOverPanel(
                state = state,
                onRestart = onRestart,
                modifier = Modifier.fillMaxSize(),
                bottomContent = gameOverAd,
            )
        }
    }
}

/**
 * A slow drifting accent glow plus a red vignette that creeps in as the clock
 * runs down. Drawn on a Canvas so it costs no layout.
 */
@Composable
private fun LivingBackground(state: GameUiState, modifier: Modifier = Modifier) {
    val drift = rememberInfiniteTransition(label = "backgroundDrift")
    val phase by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "backgroundPhase",
    )
    val lowTime = state.timeFraction <= LOW_TIME_RATIO
    val urgency = if (lowTime) {
        (1f - state.timeFraction / LOW_TIME_RATIO).coerceIn(0f, 1f)
    } else {
        0f
    }

    Canvas(modifier) {
        val glowRadius = size.minDimension * 0.95f
        val centerX = size.width * (0.12f + 0.76f * phase)
        val centerY = size.height * (0.24f + 0.10f * (1f - phase))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PrimeAccent.copy(alpha = 0.17f),
                    PrimeAccent.copy(alpha = 0.04f),
                    Color.Transparent,
                ),
                center = Offset(centerX, centerY),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(centerX, centerY),
        )

        val secondX = size.width * (0.85f - 0.6f * phase)
        val secondY = size.height * 0.78f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PrimePositive.copy(alpha = 0.07f),
                    Color.Transparent,
                ),
                center = Offset(secondX, secondY),
                radius = glowRadius * 0.55f,
            ),
            radius = glowRadius * 0.55f,
            center = Offset(secondX, secondY),
        )

        if (urgency > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        PrimeNegative.copy(alpha = 0.22f * urgency),
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.72f,
                ),
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
        shadowElevation = 18.dp,
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
private fun StackCard(scale: Float, alpha: Float, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .scale(scale)
            .alpha(alpha),
        shape = RoundedCornerShape(28.dp),
        color = PrimeCard,
        shadowElevation = 8.dp,
    ) {}
}

@Composable
private fun Hud(state: GameUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = PrimeSurface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = stringResource(R.string.score_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimeTextMuted,
                    )
                    Text(
                        text = state.score.toString(),
                        style = PrimeScoreStyle,
                        color = PrimeTextPrimary,
                        modifier = Modifier.testTag("scoreValue"),
                    )
                }

                if (state.streak >= STREAK_CHIP_MIN) {
                    Spacer(Modifier.width(14.dp))
                    StreakChip(state.streak)
                }

                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isNewBest) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = PrimeAccent.copy(alpha = 0.18f),
                            ) {
                                Text(
                                    text = stringResource(R.string.new_best_label),
                                    color = PrimeAccent,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier
                                        .testTag("newBestBadge")
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(
                            text = stringResource(R.string.best_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimeTextMuted,
                        )
                    }
                    Text(
                        text = state.bestScore.toString(),
                        style = PrimeScoreStyle,
                        color = if (state.isNewBest) PrimeAccent else PrimeTextPrimary,
                        modifier = Modifier.testTag("bestValue"),
                    )
                    if (state.dailyStreak >= 2) {
                        Text(
                            text = stringResource(R.string.daily_streak_days, state.dailyStreak),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = PrimeTextMuted,
                            modifier = Modifier.testTag("dailyStreak"),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            TimerBar(state)
        }
    }
}

@Composable
private fun StreakChip(streak: Int) {
    Surface(
        shape = CircleShape,
        color = PrimeAccent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("streakChip"),
    ) {
        Text(
            text = stringResource(R.string.streak_chip, streak),
            color = PrimeAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
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

    var pulse by remember { mutableStateOf(1f) }
    LaunchedEffect(seconds, lowTime) {
        if (!lowTime) {
            pulse = 1f
            return@LaunchedEffect
        }
        animate(
            initialValue = 1.25f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        ) { value, _ -> pulse = value }
    }

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
            modifier = Modifier
                .width(30.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },
            textAlign = TextAlign.End,
        )
    }
}

/** Floating "+1" that rises from the card and fades after a correct guess. */
@Composable
private fun PlusOne(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return

    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.plus_one),
            color = PrimePositive,
            fontSize = 46.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .testTag("plusOne")
                .graphicsLayer {
                    alpha = (1f - progress.value).coerceIn(0f, 1f)
                    translationY = -72.dp.toPx() * progress.value
                    val scale = 1f + 0.35f * progress.value
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

private data class ConfettiParticle(
    val startX: Float,
    val driftX: Float,
    val rise: Float,
    val sizeFactor: Float,
    val colorIndex: Int,
)

/** A one-shot confetti burst; no dependency, just a Canvas and 56 rectangles. */
@Composable
private fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return

    val progress = remember(trigger) { Animatable(0f) }
    val particles = remember(trigger) {
        val random = Random(trigger * 7_919 + 17)
        List(56) {
            ConfettiParticle(
                startX = 0.18f + 0.64f * random.nextFloat(),
                driftX = (random.nextFloat() - 0.5f) * 1.1f,
                rise = 0.30f + 0.35f * random.nextFloat(),
                sizeFactor = 0.6f + 0.8f * random.nextFloat(),
                colorIndex = random.nextInt(4),
            )
        }
    }
    val colors = listOf(PrimeAccent, PrimePositive, PrimeNegative, Color(0xFFFFD166))

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1_200, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val alpha = (1f - t * t).coerceIn(0f, 1f)
        particles.forEach { particle ->
            val x = size.width * particle.startX + particle.driftX * size.width * t
            val y = size.height * 0.42f -
                particle.rise * size.height * t +
                0.55f * size.height * t * t
            val piece = 9.dp.toPx() * particle.sizeFactor
            drawRect(
                color = colors[particle.colorIndex].copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(piece * 0.6f, piece),
            )
        }
    }
}

private fun tierNameRes(tier: Int): Int = when (tier) {
    1 -> R.string.tier_factor_hunter
    2 -> R.string.tier_prime_time
    else -> R.string.tier_rookie
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
                score = 12,
                bestScore = 21,
                remainingMs = 3_400,
                timeLimitMs = 6_000,
                streak = 4,
                dailyStreak = 3,
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
                bestStreak = 5,
                cardsSeen = 13,
                correctCount = 12,
                fastestAnswerMs = 1_240,
            ),
            onSwipe = {},
            onRestart = {},
        )
    }
}
