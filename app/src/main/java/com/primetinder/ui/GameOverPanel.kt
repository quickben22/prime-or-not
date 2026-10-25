package com.primetinder.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primetinder.R
import com.primetinder.game.PrimeChecker
import com.primetinder.ui.theme.PrimeAccent
import com.primetinder.ui.theme.PrimeBackground
import com.primetinder.ui.theme.PrimeSurface
import com.primetinder.ui.theme.PrimeTextMuted
import com.primetinder.ui.theme.PrimeTextPrimary
import androidx.compose.ui.res.stringResource

@Composable
fun GameOverPanel(
    state: GameUiState,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
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

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.58f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .testTag("gameOverPanel"),
            shape = RoundedCornerShape(28.dp),
            color = PrimeSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ResultStat(
                        label = stringResource(R.string.score_label),
                        value = state.score,
                    )
                    ResultStat(
                        label = stringResource(R.string.best_label),
                        value = state.bestScore,
                    )
                }

                Spacer(Modifier.height(26.dp))
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
    }
}

@Composable
private fun ResultStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PrimeTextMuted,
        )
        Text(
            text = value.toString(),
            color = PrimeTextPrimary,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
