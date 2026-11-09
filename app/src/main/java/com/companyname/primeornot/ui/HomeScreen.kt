package com.companyname.primeornot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companyname.primeornot.R
import com.companyname.primeornot.data.ActiveClassicSession
import com.companyname.primeornot.data.CalendarDay
import com.companyname.primeornot.data.ClassicRecords
import com.companyname.primeornot.ui.theme.PrimeAccent
import com.companyname.primeornot.ui.theme.PrimeBackground
import com.companyname.primeornot.ui.theme.PrimeBackgroundTop
import com.companyname.primeornot.ui.theme.PrimePositive
import com.companyname.primeornot.ui.theme.PrimeScoreStyle
import com.companyname.primeornot.ui.theme.PrimeSurface
import com.companyname.primeornot.ui.theme.PrimeTextMuted
import com.companyname.primeornot.ui.theme.PrimeTextPrimary
import com.companyname.primeornot.ui.theme.PrimeTrack

@Composable
fun HomeScreen(
    records: ClassicRecords,
    activeSession: ActiveClassicSession?,
    today: CalendarDay,
    onPlay: () -> Unit,
    onResume: (ActiveClassicSession) -> Unit,
    onDiscard: () -> Unit,
    onPractice: () -> Unit,
    onHowToPlay: () -> Unit,
    onRecords: () -> Unit,
    modifier: Modifier = Modifier,
    onPrivacyOptions: (() -> Unit)? = null,
) {
    val streak = records.currentStreakOn(today)

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
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimeLogo(modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimeTextPrimary,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimeTextMuted,
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            if (activeSession != null) {
                ResumeCard(
                    session = activeSession,
                    onResume = { onResume(activeSession) },
                    onDiscard = onDiscard,
                )
                Spacer(Modifier.height(14.dp))
            }

            PlayClassicCard(
                bestScore = records.bestScore,
                streak = streak,
                onClick = onPlay,
            )

            Spacer(Modifier.height(16.dp))

            HomeTile(
                title = stringResource(R.string.home_practice),
                subtitle = stringResource(R.string.home_practice_hint),
                tag = "homePractice",
                accent = PrimePositive,
                onClick = onPractice,
            )
            Spacer(Modifier.height(10.dp))
            HomeTile(
                title = stringResource(R.string.home_how_to_play),
                subtitle = stringResource(R.string.how_intro),
                tag = "homeHowToPlay",
                accent = PrimeAccent,
                onClick = onHowToPlay,
            )
            Spacer(Modifier.height(10.dp))
            HomeTile(
                title = stringResource(R.string.home_my_records),
                subtitle = stringResource(R.string.records_subtitle),
                tag = "homeRecords",
                accent = PrimeTextPrimary,
                onClick = onRecords,
            )

            Spacer(Modifier.height(28.dp))

            if (onPrivacyOptions != null) {
                TextButton(
                    onClick = onPrivacyOptions,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("privacyOptions"),
                ) {
                    Text(
                        text = stringResource(R.string.privacy_options),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimeTextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayClassicCard(
    bestScore: Int,
    streak: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("homePlay")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = PrimeAccent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.55f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            Text(
                text = stringResource(R.string.home_classic),
                color = PrimeAccent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_classic_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimeTextMuted,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                LabeledNumber(
                    label = stringResource(R.string.best_label),
                    value = bestScore.toString(),
                    tag = "homeBest",
                )
                LabeledNumber(
                    label = stringResource(R.string.progress_current_streak),
                    value = pluralStringResource(R.plurals.streak_days, streak, streak),
                    tag = "homeStreak",
                    accent = streak > 0,
                )
            }
        }
    }
}

@Composable
private fun ResumeCard(
    session: ActiveClassicSession,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("homeResumeCard"),
        shape = RoundedCornerShape(22.dp),
        color = PrimeSurface,
        border = BorderStroke(1.dp, PrimePositive.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.home_resume_title),
                style = MaterialTheme.typography.labelSmall,
                color = PrimePositive,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.home_resume_score,
                    session.score,
                    session.cardsSeen,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimeTextPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("homeResume")
                        .clickable(onClick = onResume),
                    shape = RoundedCornerShape(14.dp),
                    color = PrimePositive,
                ) {
                    Text(
                        text = stringResource(R.string.resume),
                        color = PrimeBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("homeDiscard")
                        .clickable(onClick = onDiscard),
                    shape = RoundedCornerShape(14.dp),
                    color = PrimeTrack,
                ) {
                    Text(
                        text = stringResource(R.string.discard),
                        color = PrimeTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTile(
    title: String,
    subtitle: String,
    tag: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = PrimeSurface,
        border = BorderStroke(1.dp, PrimeTrack),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = PrimeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimeTextMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun LabeledNumber(
    label: String,
    value: String,
    tag: String,
    accent: Boolean = false,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PrimeTextMuted,
        )
        Text(
            text = value,
            style = PrimeScoreStyle,
            color = if (accent) PrimeAccent else PrimeTextPrimary,
            modifier = Modifier.testTag(tag),
        )
    }
}
