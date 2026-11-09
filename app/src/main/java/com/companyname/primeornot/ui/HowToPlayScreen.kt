package com.companyname.primeornot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companyname.primeornot.R
import com.companyname.primeornot.ui.theme.PrimeAccent
import com.companyname.primeornot.ui.theme.PrimeBackground
import com.companyname.primeornot.ui.theme.PrimeBackgroundTop
import com.companyname.primeornot.ui.theme.PrimeNegative
import com.companyname.primeornot.ui.theme.PrimePositive
import com.companyname.primeornot.ui.theme.PrimeSurface
import com.companyname.primeornot.ui.theme.PrimeTextMuted
import com.companyname.primeornot.ui.theme.PrimeTextPrimary
import com.companyname.primeornot.ui.theme.PrimeTrack

@Composable
fun HowToPlayScreen(
    onboarding: Boolean,
    onBack: () -> Unit,
    onStartPractice: () -> Unit,
    modifier: Modifier = Modifier,
    onPrivacyOptions: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimeBackgroundTop, PrimeBackground)))
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.testTag("howBack")) {
                    Text(
                        text = "← " + stringResource(R.string.how_back),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimeTextMuted,
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            Text(
                text = stringResource(R.string.how_title),
                color = PrimeTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.how_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimeTextMuted,
            )

            Spacer(Modifier.height(20.dp))

            ConceptCard(
                title = stringResource(R.string.how_prime_title),
                body = stringResource(R.string.how_prime_body),
                examples = stringResource(R.string.how_prime_examples),
                accent = PrimePositive,
            )
            Spacer(Modifier.height(12.dp))
            ConceptCard(
                title = stringResource(R.string.how_composite_title),
                body = stringResource(R.string.how_composite_body),
                examples = stringResource(R.string.how_composite_examples),
                accent = PrimeNegative,
            )
            Spacer(Modifier.height(12.dp))
            ConceptCard(
                title = stringResource(R.string.how_neither_title),
                body = stringResource(R.string.how_neither_body),
                examples = null,
                accent = PrimeAccent,
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = PrimeSurface,
                border = BorderStroke(1.dp, PrimeTrack),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.how_play_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimeTextMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.how_play_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimeTextPrimary,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.how_practice_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimeTextMuted,
                        lineHeight = 20.sp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            TeachingTipCard(tip = TeachingTips.all.last())

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = if (onboarding) onStartPractice else onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("howPrimary"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimeAccent,
                    contentColor = PrimeBackground,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (onboarding) R.string.how_start_practice else R.string.how_got_it,
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            if (!onboarding) {
                TextButton(
                    onClick = onStartPractice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("howPractice"),
                ) {
                    Text(
                        text = stringResource(R.string.home_practice),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimeAccent,
                    )
                }
            }
            if (onPrivacyOptions != null) {
                TextButton(
                    onClick = onPrivacyOptions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("howPrivacy"),
                ) {
                    Text(
                        text = stringResource(R.string.privacy_options),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimeTextMuted,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConceptCard(
    title: String,
    body: String,
    examples: String?,
    accent: androidx.compose.ui.graphics.Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PrimeSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = PrimeTextPrimary,
                lineHeight = 20.sp,
            )
            if (examples != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = examples,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimeTextMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
