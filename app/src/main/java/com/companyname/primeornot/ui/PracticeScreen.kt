package com.companyname.primeornot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companyname.primeornot.R
import com.companyname.primeornot.game.PracticeAnswer
import com.companyname.primeornot.game.PracticeDeck
import com.companyname.primeornot.game.PrimeChecker
import com.companyname.primeornot.ui.theme.PrimeAccent
import com.companyname.primeornot.ui.theme.PrimeBackground
import com.companyname.primeornot.ui.theme.PrimeBackgroundTop
import com.companyname.primeornot.ui.theme.PrimeCard
import com.companyname.primeornot.ui.theme.PrimeCardBottom
import com.companyname.primeornot.ui.theme.PrimeCardNumberStyle
import com.companyname.primeornot.ui.theme.PrimeInk
import com.companyname.primeornot.ui.theme.PrimeNegative
import com.companyname.primeornot.ui.theme.PrimeNegativeInk
import com.companyname.primeornot.ui.theme.PrimePositive
import com.companyname.primeornot.ui.theme.PrimePositiveInk
import com.companyname.primeornot.ui.theme.PrimeSurface
import com.companyname.primeornot.ui.theme.PrimeTextMuted
import com.companyname.primeornot.ui.theme.PrimeTextPrimary
import com.companyname.primeornot.ui.theme.PrimeTrack

@Composable
fun PracticeScreen(
    state: PracticeUiState,
    onAnswer: (PracticeAnswer) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onPlayClassic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimeBackgroundTop, PrimeBackground)))
            .safeDrawingPadding(),
    ) {
        if (state.finished) {
            PracticeComplete(
                onPlayClassic = onPlayClassic,
                onRestart = onRestart,
                onExit = onExit,
            )
        } else {
            PracticeRound(
                state = state,
                onAnswer = onAnswer,
                onSkip = onSkip,
                onNext = onNext,
                onExit = onExit,
            )
        }
    }
}

@Composable
private fun PracticeRound(
    state: PracticeUiState,
    onAnswer: (PracticeAnswer) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
) {
    val card = state.currentCard ?: return
    val answered = state.answered
    val correctAnswer = PracticeDeck.correctAnswerFor(card)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit, modifier = Modifier.testTag("practiceExit")) {
                Text(
                    text = "✕ " + stringResource(R.string.practice_exit),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimeTextMuted,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.practice_card_progress,
                    state.cardNumber,
                    PracticeDeck.SIZE,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = PrimeAccent,
                modifier = Modifier.testTag("practiceProgress"),
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.practice_title),
            color = PrimeTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        Text(
            text = stringResource(R.string.practice_no_pressure),
            style = MaterialTheme.typography.bodyMedium,
            color = PrimeTextMuted,
        )

        Spacer(Modifier.height(18.dp))
        ProgressDots(index = state.index, answered = answered != null || state.skipped)

        Spacer(Modifier.height(18.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 210.dp)
                .testTag("practiceCard"),
            shape = RoundedCornerShape(28.dp),
            color = PrimeCard,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Color(0xFFE6E8EC)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(PrimeCard, PrimeCardBottom))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.card_prompt),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimeInk.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = card.value.toString(),
                        style = PrimeCardNumberStyle,
                        color = PrimeInk,
                        modifier = Modifier.testTag("practiceNumber"),
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        if (answered == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnswerButton(
                    label = stringResource(R.string.practice_prime),
                    color = PrimePositiveInk,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("answerPrime"),
                    onClick = { onAnswer(PracticeAnswer.Prime) },
                )
                AnswerButton(
                    label = stringResource(R.string.practice_composite),
                    color = PrimeNegativeInk,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("answerComposite"),
                    onClick = { onAnswer(PracticeAnswer.Composite) },
                )
                AnswerButton(
                    label = stringResource(R.string.practice_neither),
                    color = PrimeAccent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("answerNeither"),
                    onClick = { onAnswer(PracticeAnswer.Neither) },
                )
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onSkip, modifier = Modifier.testTag("practiceSkip")) {
                Text(
                    text = stringResource(R.string.practice_skip),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimeTextMuted,
                )
            }
        } else {
            val isCorrect = state.isCorrect == true
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("practiceFeedback"),
                shape = RoundedCornerShape(20.dp),
                color = if (isCorrect) {
                    PrimePositive.copy(alpha = 0.12f)
                } else {
                    PrimeNegative.copy(alpha = 0.12f)
                },
                border = BorderStroke(
                    1.dp,
                    (if (isCorrect) PrimePositive else PrimeNegative).copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(
                            if (isCorrect) R.string.practice_correct else R.string.practice_incorrect,
                        ),
                        color = if (isCorrect) PrimePositive else PrimeNegative,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = explanationFor(card.value, correctAnswer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimeTextPrimary,
                        lineHeight = 20.sp,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!isCorrect) {
                TeachingTipCard(tip = TeachingTips.forNumber(card.value))
                Spacer(Modifier.height(10.dp))
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("practiceNext"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimeAccent,
                    contentColor = PrimeBackground,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (state.isLastCard) R.string.practice_finish else R.string.practice_next,
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun explanationFor(value: Int, correctAnswer: PracticeAnswer): String = when (correctAnswer) {
    PracticeAnswer.Neither -> stringResource(R.string.practice_feedback_neither)
    PracticeAnswer.Prime -> stringResource(R.string.practice_feedback_prime, value)
    PracticeAnswer.Composite -> stringResource(
        R.string.practice_feedback_composite,
        value,
        PrimeChecker.factorizationText(value),
    )
}

@Composable
private fun ProgressDots(index: Int, answered: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(PracticeDeck.SIZE) { position ->
            val active = position < index || (position == index && answered)
            val current = position == index && !answered
            Box(
                modifier = Modifier
                    .size(if (current) 11.dp else 8.dp)
                    .background(
                        color = when {
                            active -> PrimeAccent
                            current -> PrimeTextPrimary
                            else -> PrimeTrack
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun AnswerButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PracticeComplete(
    onPlayClassic: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.practice_complete_title),
            color = PrimeTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.practice_complete_body),
            style = MaterialTheme.typography.bodyMedium,
            color = PrimeTextMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onPlayClassic,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("practicePlayClassic"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimeAccent,
                contentColor = PrimeBackground,
            ),
        ) {
            Text(
                text = stringResource(R.string.practice_play_classic),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("practiceAgain"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimeSurface,
                contentColor = PrimeTextPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.practice_again),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
        TextButton(onClick = onExit, modifier = Modifier.testTag("practiceMenu")) {
            Text(
                text = stringResource(R.string.practice_back_home),
                style = MaterialTheme.typography.labelSmall,
                color = PrimeTextMuted,
            )
        }
    }
}
