package com.elearning.openbook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elearning.openbook.R
import com.elearning.openbook.model.BookItem
import com.elearning.openbook.reader.ReadingSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * The detail behind Home's achievement card, the same sections as iOS
 * `ReadingStatsSheet`: four totals, the runs, the records and the books finished this year.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingStatsSheet(
    summary: ReadingSummary,
    finished: List<BookItem>,
    onChangeGoal: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = shellColors()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.paper,
        contentColor = colors.ink,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.inkSoft) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.stats_title),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = colors.ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.stats_goal, summary.goalMinutes),
                        fontSize = 12.sp,
                        color = colors.inkSoft,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.stats_change_goal),
                        modifier = Modifier.clickable(onClick = onChangeGoal).padding(vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(stringResource(R.string.stats_today), summary.todaySeconds, Modifier.weight(1f))
                    Tile(stringResource(R.string.stats_week), summary.weekSeconds, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(stringResource(R.string.stats_month), summary.monthSeconds, Modifier.weight(1f))
                    Tile(stringResource(R.string.stats_year), summary.yearSeconds, Modifier.weight(1f))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption(stringResource(R.string.stats_consistency))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DayCount(stringResource(R.string.stats_streak_current), summary.streak, Modifier.weight(1f))
                    DayCount(stringResource(R.string.stats_streak_longest), summary.recordStreak, Modifier.weight(1f))
                    DayCount(stringResource(R.string.stats_days_read), summary.readingDays, Modifier.weight(1f))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption(stringResource(R.string.stats_records))
                RecordRow(stringResource(R.string.stats_longest_session), durationText(summary.longestSessionSeconds))
                summary.bestDayDate?.let { date ->
                    val locale = LocalConfiguration.current.locales[0]
                    val day = date.format(DateTimeFormatter.ofPattern("d MMM", locale))
                    RecordRow(
                        stringResource(R.string.stats_best_day),
                        stringResource(R.string.stats_best_day_value, day, durationText(summary.bestDaySeconds)),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption(stringResource(R.string.stats_finished))
                if (finished.isEmpty()) {
                    Text(stringResource(R.string.stats_finished_empty), fontSize = 13.sp, color = colors.inkSoft)
                } else {
                    val locale = LocalConfiguration.current.locales[0]
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        for (book in finished) {
                            Column(
                                modifier = Modifier.width(92.dp).semantics(mergeDescendants = true) {},
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                BookCover(book = book, modifier = Modifier.width(58.dp))
                                Text(
                                    text = book.title,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = colors.ink,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = Instant.ofEpochMilli(book.finishedAt ?: 0L)
                                        .atZone(ZoneId.systemDefault()).month
                                        .getDisplayName(TextStyle.FULL_STANDALONE, locale),
                                    fontSize = 10.sp,
                                    color = colors.inkSoft,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Books marked finished in the current calendar year, newest first. */
fun finishedThisYear(books: List<BookItem>, zone: ZoneId = ZoneId.systemDefault()): List<BookItem> {
    val year = java.time.LocalDate.now(zone).year
    return books
        .filter { book -> book.finishedAt?.let { Instant.ofEpochMilli(it).atZone(zone).year == year } == true }
        .sortedByDescending { it.finishedAt }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = shellColors().inkSoft,
    )
}

@Composable
private fun Tile(title: String, seconds: Int, modifier: Modifier) {
    val colors = shellColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.raised)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Caption(title)
        Text(
            text = durationText(seconds),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            color = colors.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun DayCount(title: String, days: Int, modifier: Modifier) {
    val colors = shellColors()
    Column(modifier = modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontSize = 10.sp, color = colors.inkSoft, maxLines = 1)
        Text(
            text = pluralStringResource(R.plurals.stats_days, days, days),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.ink,
        )
    }
}

@Composable
private fun RecordRow(title: String, value: String) {
    val colors = shellColors()
    Row(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.inkSoft)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.ink)
    }
}
