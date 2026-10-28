package com.elearning.openbook.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elearning.openbook.R
import com.elearning.openbook.library.HomeContent
import com.elearning.openbook.library.HomeSections
import com.elearning.openbook.library.InProgressBook
import com.elearning.openbook.model.BookItem
import com.elearning.openbook.reader.ReadingPosition
import com.elearning.openbook.reader.ReadingSummary
import com.elearning.openbook.reader.WeekPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import kotlin.math.roundToInt

/**
 * Home: the reading achievement card, then the books that matter now.
 *
 * The section order, the five-book caps and the author rules live in [HomeSections];
 * this file only draws them. Under six books the extra rows and the week chart are not
 * drawn at all, so a new reader is not shown five empty frames.
 */
@Composable
fun HomeScreen(
    books: List<BookItem>,
    reading: Map<String, ReadingPosition>,
    summary: ReadingSummary,
    week: List<WeekPoint>,
    loading: Boolean,
    onOpen: (BookItem) -> Unit,
    onImport: () -> Unit,
    onBrowse: () -> Unit,
    onAppSettings: () -> Unit,
) {
    val colors = shellColors()
    val content = remember(books, reading) {
        HomeSections.content(books, reading, System.currentTimeMillis())
    }
    val scroll = rememberScrollState()
    val atTop by remember { derivedStateOf { scroll.value == 0 } }
    val gearAlpha by animateFloatAsState(if (atTop) 1f else 0f, tween(200), label = "gear")

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 18.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 30.sp,
                    color = colors.ink,
                )
                Box(modifier = Modifier.alphaCompat(gearAlpha)) {
                    ShellIconButton(
                        onClick = { if (atTop) onAppSettings() },
                        description = stringResource(R.string.app_settings),
                        size = 44.dp,
                    ) {
                        GearGlyph(color = colors.ink)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                loading -> CenteredNote(stringResource(R.string.loading_library))
                books.isEmpty() -> EmptyHome(onImport = onImport)
                else -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionLabel(stringResource(R.string.home_stats_achievements))
                        Spacer(modifier = Modifier.height(10.dp))
                        AchievementsCard(summary = summary)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionLabel(stringResource(R.string.home_reading_now))
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (content.readingNow.isEmpty()) {
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            NothingInProgress(onBrowse = onBrowse)
                        }
                    } else {
                        ReadingPager(items = content.readingNow, onOpen = onOpen)
                    }

                    if (content.expanded) {
                        if (content.added.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            ShelfRow(
                                title = stringResource(R.string.home_new_title),
                                books = content.added,
                                subtitle = { book ->
                                    stringResource(
                                        R.string.home_added_month,
                                        monthName(book.importedAt),
                                    )
                                },
                                onOpen = onOpen,
                            )
                        }
                        if (content.suggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            ShelfRow(
                                title = stringResource(R.string.home_suggestions_title),
                                books = content.suggestions,
                                subtitle = { stringResource(R.string.home_suggestions_reason) },
                                onOpen = onOpen,
                            )
                        }
                        if (content.finished.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            ShelfRow(
                                title = stringResource(R.string.home_finished_title),
                                books = content.finished,
                                subtitle = { book ->
                                    stringResource(
                                        R.string.home_finished_month,
                                        monthName(book.finishedAt ?: 0L),
                                    )
                                },
                                onOpen = onOpen,
                            )
                        }
                        if (week.any { it.minutes > 0 }) {
                            Spacer(modifier = Modifier.height(24.dp))
                            WeekChart(week = week, summary = summary)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun Modifier.alphaCompat(value: Float): Modifier = this.alpha(value)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 1.0.sp,
        color = shellColors().inkSoft,
    )
}

/** The day's ring, the goal in words, the running totals and the streak. */
@Composable
private fun AchievementsCard(summary: ReadingSummary) {
    val colors = shellColors()
    val fraction = (summary.todaySeconds.toFloat() / summary.goalSeconds.coerceAtLeast(1))
        .coerceIn(0f, 1f)
    val reached = stringResource(R.string.home_stats_reached)
    val left = stringResource(R.string.home_stats_left, summary.minutesLeft)
    val streakText = if (summary.streak == 0) {
        stringResource(R.string.home_stats_no_streak)
    } else {
        stringResource(R.string.home_stats_streak, summary.streak)
    }
    val span = stringResource(
        R.string.home_stats_span,
        durationText(summary.weekSeconds),
        durationText(summary.monthSeconds),
        durationText(summary.yearSeconds),
    )
    val goalLine = if (summary.achieved) {
        stringResource(R.string.home_stats_goal_done, summary.goalMinutes)
    } else {
        stringResource(R.string.home_stats_goal_left, summary.minutesLeft)
    }
    val description = stringResource(
        R.string.home_stats_a11y,
        summary.todayMinutes,
        summary.goalMinutes,
        if (summary.achieved) reached else left,
        streakText,
        span,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.raised)
            .border(1.dp, colors.hairline, RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalRing(fraction = fraction, achieved = summary.achieved)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.home_stats_today,
                    durationText(summary.todaySeconds),
                ),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = colors.ink,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = goalLine,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = colors.inkSoft,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = span,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.inkSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (summary.streak == 0) streakText else "🔥 $streakText",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = colors.inkSoft,
            maxLines = 2,
        )
    }
}

/** A 44dp progress ring; the percentage (or ✓) sits in the hole, not on the arc. */
@Composable
private fun GoalRing(fraction: Float, achieved: Boolean) {
    val colors = shellColors()
    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            drawCircle(
                color = colors.ink.copy(alpha = 0.14f),
                style = Stroke(width = stroke),
            )
            if (fraction > 0f) {
                drawArc(
                    color = colors.accent,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = if (achieved) "✓" else "${(fraction * 100).roundToInt()}%",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = if (achieved) colors.accent else colors.inkSoft,
        )
    }
}

/** One swipeable reading card, the same card Home has always had. */
@Composable
private fun ReadingPager(items: List<InProgressBook>, onOpen: (BookItem) -> Unit) {
    val colors = shellColors()
    val pager = rememberPagerState(pageCount = { items.size })
    Box(modifier = Modifier.height(152.dp)) {
        HorizontalPager(
            state = pager,
            pageSize = androidx.compose.foundation.pager.PageSize.Fixed(300.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ContinueCard(item = items[page], onOpen = { onOpen(items[page].book) })
        }
    }
    if (items.size > 1) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            items.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == pager.currentPage) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pager.currentPage) colors.accent
                            else colors.ink.copy(alpha = 0.18f),
                        ),
                )
            }
        }
    }
}

@Composable
private fun ContinueCard(item: InProgressBook, onOpen: () -> Unit) {
    val colors = shellColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.raised)
            .border(1.dp, colors.ink.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book = item.book, modifier = Modifier.width(72.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.book.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.book.author,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = colors.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(
                    R.string.page_of_pages,
                    item.position.page + 1,
                    item.position.pageCount,
                ) + " · " + stringResource(
                    R.string.percent_read,
                    HomeSections.percent(item.position.fraction),
                ),
                fontFamily = FontFamily.Serif,
                fontSize = 11.sp,
                color = colors.inkSoft,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.continue_reading),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = colors.accent,
            )
        }
    }
}

/**
 * A shared frame for one row: left inset 20dp, right edge deliberately off-screen.
 * Every book sits inside this single card — no per-book background.
 */
@Composable
private fun ShelfRow(
    title: String,
    books: List<BookItem>,
    subtitle: @Composable (BookItem) -> String,
    onOpen: (BookItem) -> Unit,
) {
    val colors = shellColors()
    Column {
        Text(
            text = title,
            modifier = Modifier.padding(start = 20.dp, bottom = 10.dp),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 1.0.sp,
            color = colors.inkSoft,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val frameWidth = maxWidth + 20.dp
            Row(
                modifier = Modifier
                    .offset(x = 20.dp)
                    .width(frameWidth)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.raised)
                    .border(1.dp, colors.ink.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                for (book in books) {
                    ShelfBook(
                        book = book,
                        subtitle = subtitle(book),
                        onOpen = { onOpen(book) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfBook(book: BookItem, subtitle: String, onOpen: () -> Unit) {
    val colors = shellColors()
    val description = stringResource(R.string.home_book_a11y, book.title, book.author)
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        BookCover(book = book, modifier = Modifier.width(96.dp))
        // A book that carries its own cover already shows its name there; a second,
        // scrolling title under it only made the shelf noisy.
        if (book.coverPath == null && book.author.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = book.author,
                fontFamily = FontFamily.Serif,
                fontSize = 11.sp,
                color = colors.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.sp,
            color = colors.inkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Seven bars, Monday first, with the goal as a thin line behind them. */
@Composable
private fun WeekChart(week: List<WeekPoint>, summary: ReadingSummary) {
    val colors = shellColors()
    val locale = LocalConfiguration.current.locales[0]
    val dayLabels = androidx.compose.ui.res.stringArrayResource(R.array.home_weekdays)
    val top = maxOf(summary.goalMinutes, week.maxOf { it.minutes }, 1)
    // Sum the bars the reader can see, so the header and the chart never disagree by a minute.
    val total = durationText(week.sumOf { it.minutes } * 60)

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_weekly_title),
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 1.0.sp,
                color = colors.inkSoft,
            )
            Text(
                text = total,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                color = colors.inkSoft,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().height(88.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * (1f - summary.goalMinutes.toFloat() / top)
                drawLine(
                    color = colors.accent.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (point in week) {
                    val height = (point.minutes.toFloat() / top).coerceIn(0f, 1f) * 88f
                    val barColor = when {
                        point.isToday -> colors.accent
                        else -> colors.inkSoft.copy(alpha = 0.45f)
                    }
                    val description = stringResource(
                        R.string.home_weekly_a11y,
                        point.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                        point.minutes,
                    )
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight()
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(maxOf(height, 2f).dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (point in week) {
                Text(
                    text = dayLabels.getOrElse(point.dayOfWeek.value - 1) {
                        point.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                    },
                    modifier = Modifier.width(18.dp),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (point.isToday) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (point.isToday) colors.accent else colors.inkSoft,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun NothingInProgress(onBrowse: () -> Unit) {
    val colors = shellColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.raised)
            .border(1.dp, colors.hairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
    ) {
        Text(
            text = stringResource(R.string.home_nothing_in_progress_title),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = colors.ink,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.home_nothing_in_progress_body),
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = colors.inkSoft,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(label = stringResource(R.string.open_library), onClick = onBrowse)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    val colors = shellColors()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (colors.dark) colors.paper else androidx.compose.ui.graphics.Color.White,
        )
    }
}

@Composable
private fun EmptyHome(onImport: () -> Unit) {
    val colors = shellColors()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = colors.ink,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_empty_body),
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            color = colors.inkSoft,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        PrimaryButton(label = stringResource(R.string.import_epub), onClick = onImport)
    }
}

@Composable
private fun CenteredNote(message: String) {
    val colors = shellColors()
    Text(
        text = message,
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp,
        color = colors.inkSoft,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

/** "N phút" / "N giờ" / "N giờ M phút", one helper for every surface. */
@Composable
fun durationText(seconds: Int): String {
    val minutes = seconds / 60
    return when {
        minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
        minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
        else -> stringResource(
            R.string.duration_hours_minutes,
            minutes / 60,
            minutes % 60,
        )
    }
}

@Composable
private fun monthName(stamp: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val date = Instant.ofEpochMilli(stamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.month.getDisplayName(TextStyle.SHORT, locale)
}

private fun HomeSections.finishedAt(book: BookItem): LocalDate? =
    book.finishedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
