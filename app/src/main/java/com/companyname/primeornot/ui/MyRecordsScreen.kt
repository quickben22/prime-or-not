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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import com.companyname.primeornot.data.CalendarDay
import com.companyname.primeornot.data.ClassicRecords
import com.companyname.primeornot.data.CompletedGame
import com.companyname.primeornot.ui.theme.PrimeAccent
import com.companyname.primeornot.ui.theme.PrimeBackground
import com.companyname.primeornot.ui.theme.PrimeBackgroundTop
import com.companyname.primeornot.ui.theme.PrimePositive
import com.companyname.primeornot.ui.theme.PrimeScoreStyle
import com.companyname.primeornot.ui.theme.PrimeSurface
import com.companyname.primeornot.ui.theme.PrimeTextMuted
import com.companyname.primeornot.ui.theme.PrimeTextPrimary
import com.companyname.primeornot.ui.theme.PrimeTrack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyRecordsScreen(
    records: ClassicRecords,
    today: CalendarDay,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.testTag("recordsBack")) {
                    Text(
                        text = "← " + stringResource(R.string.records_back),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimeTextMuted,
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = stringResource(R.string.records_title),
                color = PrimeTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Text(
                text = stringResource(R.string.records_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimeTextMuted,
            )

            Spacer(Modifier.height(20.dp))

            SectionCard(title = stringResource(R.string.section_personal_records)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigStat(
                        label = stringResource(R.string.record_best_score),
                        value = records.bestScore.toString(),
                        tag = "recordBestScore",
                        modifier = Modifier.weight(1f),
                    )
                    BigStat(
                        label = stringResource(R.string.record_longest_streak),
                        value = records.longestCorrectStreak.toString(),
                        tag = "recordLongestStreak",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = stringResource(R.string.section_overall_stats)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallStat(
                        label = stringResource(R.string.stat_games_completed),
                        value = records.totalGames.toString(),
                        tag = "statGames",
                        modifier = Modifier.weight(1f),
                    )
                    SmallStat(
                        label = stringResource(R.string.stat_accuracy),
                        value = formatAccuracy(records.accuracyPercent),
                        tag = "statAccuracy",
                        modifier = Modifier.weight(1f),
                        accent = true,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallStat(
                        label = stringResource(R.string.stat_correct),
                        value = records.totalCorrect.toString(),
                        tag = "statCorrect",
                        modifier = Modifier.weight(1f),
                    )
                    SmallStat(
                        label = stringResource(R.string.stat_incorrect),
                        value = records.totalIncorrect.toString(),
                        tag = "statIncorrect",
                        modifier = Modifier.weight(1f),
                    )
                    SmallStat(
                        label = stringResource(R.string.stat_answered),
                        value = records.totalAnswered.toString(),
                        tag = "statAnswered",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = stringResource(R.string.section_daily_progress)) {
                val current = records.currentStreakOn(today)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigStat(
                        label = stringResource(R.string.progress_current_streak),
                        value = pluralStringResource(R.plurals.streak_days, current, current),
                        tag = "currentStreak",
                        modifier = Modifier.weight(1f),
                        accent = current > 0,
                    )
                    BigStat(
                        label = stringResource(R.string.progress_longest_streak),
                        value = pluralStringResource(
                            R.plurals.streak_days,
                            records.longestDailyStreak,
                            records.longestDailyStreak,
                        ),
                        tag = "longestStreak",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = stringResource(R.string.section_recent_games)) {
                if (records.recentGames.isEmpty()) {
                    Text(
                        text = stringResource(R.string.recent_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimeTextMuted,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .testTag("recentEmpty"),
                    )
                } else {
                    records.recentGames.forEachIndexed { index, game ->
                        if (index > 0) {
                            HorizontalDivider(color = PrimeTextPrimary.copy(alpha = 0.08f))
                        }
                        RecentGameRow(game, modifier = Modifier.testTag("recentGame"))
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PrimeSurface,
        border = BorderStroke(1.dp, PrimeTrack),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = PrimeTextMuted,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BigStat(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PrimeTextMuted,
            fontSize = 9.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = PrimeScoreStyle,
            color = if (accent) PrimeAccent else PrimeTextPrimary,
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun SmallStat(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = PrimeTrack.copy(alpha = 0.55f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                text = label,
                color = PrimeTextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                maxLines = 2,
                lineHeight = 10.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = if (accent) PrimeAccent else PrimeTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.testTag(tag),
            )
        }
    }
}

@Composable
private fun RecentGameRow(game: CompletedGame, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.recent_score, game.score),
                color = PrimeTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.recent_answers, game.correct, game.incorrect),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimeTextMuted,
            )
            Text(
                text = formatDate(game.completedAtMillis),
                color = PrimeTextMuted,
                fontSize = 11.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatAccuracy(game.accuracyPercent),
                color = PrimePositive,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.stat_accuracy),
                color = PrimeTextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
        }
    }
}

/** "—" until at least one card has been answered, then one decimal place. */
internal fun formatAccuracy(percent: Double?): String =
    if (percent == null) "\u2014" else String.format(Locale.getDefault(), "%.1f%%", percent)

internal fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault()).format(Date(millis))
