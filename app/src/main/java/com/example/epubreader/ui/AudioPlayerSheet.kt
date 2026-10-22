package com.example.epubreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.epubreader.model.BookItem
import com.example.epubreader.reader.ReaderTheme
import com.example.epubreader.reader.VoiceOption
import kotlin.math.roundToInt

/**
 * The full player, opened by tapping the book's title in the reading bar.
 *
 * It is the "now playing" screen a music or podcast app would show, cut to what a book
 * read aloud actually has: the cover, where the voice is in the text, the three transport
 * controls, a sleep timer and the device's Vietnamese voices. The small bar stays the
 * at-a-glance control; this is the one that is comfortable to work with a thumb.
 *
 * The slider is a *draft* while a finger is on it, so the voice's own cursor cannot
 * yank the thumb out from under the hand; it seeks once, on release.
 */
@Composable
fun AudioPlayerSheet(
    book: BookItem,
    chapter: String,
    playing: Boolean,
    progress: Float,
    rateLabel: String,
    sleepMinutes: Int,
    sleepLeftSeconds: Int,
    voices: List<VoiceOption>,
    voiceName: String?,
    onSeek: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayPause: () -> Unit,
    onSleep: (Int) -> Unit,
    onVoice: (String?) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var draft by remember { mutableFloatStateOf(progress) }
    LaunchedEffect(progress, dragging) {
        if (!dragging) draft = progress
    }
    val shown = if (dragging) draft else progress

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BookCover(book = book, modifier = Modifier.width(96.dp))
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    color = ReaderTheme.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = book.author,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = ReaderTheme.InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (chapter.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = chapter,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ReaderTheme.Accent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (playing) "Đang đọc · $rateLabel" else "Tạm dừng · $rateLabel",
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = ReaderTheme.InkSoft,
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Tiến trình đọc",
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = ReaderTheme.InkSoft,
            )
            Text(
                text = "${(shown * 100f).roundToInt()}%",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = ReaderTheme.Ink,
            )
        }
        Slider(
            value = shown.coerceIn(0f, 1f),
            onValueChange = {
                dragging = true
                draft = it
            },
            onValueChangeFinished = {
                dragging = false
                onSeek(draft)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = ReaderTheme.Accent,
                activeTrackColor = ReaderTheme.Accent,
                inactiveTrackColor = ReaderTheme.Ink.copy(alpha = 0.18f),
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackButton(onClick = onPrevious) { SeekBackGlyph(glyphSize = 22.dp) }
            Spacer(modifier = Modifier.width(28.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(ReaderTheme.Accent)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                if (playing) {
                    PauseGlyph(color = ReaderTheme.Paper, glyphSize = 26.dp)
                } else {
                    PlayGlyph(color = ReaderTheme.Paper, glyphSize = 26.dp)
                }
            }
            Spacer(modifier = Modifier.width(28.dp))
            PlaybackButton(onClick = onNext) { SeekForwardGlyph(glyphSize = 22.dp) }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Hẹn giờ tắt",
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = ReaderTheme.Ink,
            )
            if (sleepMinutes > 0) {
                Text(
                    text = "tự dừng sau ${sleepCountdown(sleepLeftSeconds)}",
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = ReaderTheme.Accent,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SLEEP_CHOICES.size) { index ->
                val (minutes, label) = SLEEP_CHOICES[index]
                ChoiceChip(
                    label = label,
                    selected = sleepMinutes == minutes,
                    onClick = { onSleep(minutes) },
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Giọng đọc",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = ReaderTheme.Ink,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (voices.isEmpty()) {
            Text(
                text = "Máy chưa có giọng tiếng Việt nào khác.",
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = ReaderTheme.InkSoft,
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp),
            ) {
                item {
                    ChoiceChip(
                        label = "Mặc định",
                        selected = voiceName == null,
                        onClick = { onVoice(null) },
                    )
                }
                items(voices.size) { index ->
                    val voice = voices[index]
                    ChoiceChip(
                        label = voice.label,
                        selected = voiceName == voice.name,
                        onClick = { onVoice(voice.name) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/** One transport icon beside the big play button. */
@Composable
private fun PlaybackButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** A capsule choice: the sleep timer and the voices share one look, as in the sheet above. */
@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) ReaderTheme.Accent else ReaderTheme.Raised)
            .border(
                1.dp,
                if (selected) ReaderTheme.Accent else ReaderTheme.Ink.copy(alpha = 0.12f),
                RoundedCornerShape(11.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Serif,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp,
            color = if (selected) ReaderTheme.Paper else ReaderTheme.Ink,
            maxLines = 1,
        )
    }
}

/** `14:59` for the countdown beside the sleep timer. */
private fun sleepCountdown(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}

/** Off, then the usual podcast choices. */
private val SLEEP_CHOICES = listOf(
    0 to "Tắt",
    5 to "5 phút",
    15 to "15 phút",
    30 to "30 phút",
    60 to "60 phút",
)
