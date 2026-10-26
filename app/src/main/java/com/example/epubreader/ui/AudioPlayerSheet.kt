package com.example.epubreader.ui

import android.content.Intent
import android.provider.Settings
import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.epubreader.R
import com.example.epubreader.model.BookItem
import com.example.epubreader.reader.ReaderTheme
import com.example.epubreader.reader.VoiceCatalog
import com.example.epubreader.reader.VoiceOption
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The full player, opened by tapping the book's title in the reading bar.
 *
 * Layout follows the iOS player: cover and chapter, the voice in use, a speed capsule,
 * the book-wide scrubber, then one transport row — sleep, previous chapter, play,
 * next chapter, voice picker. The small bar stays the at-a-glance control; this is the
 * one that is comfortable to work with a thumb.
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
    page: Int,
    pageCount: Int,
    rateLabel: String,
    canSlowDown: Boolean,
    canSpeedUp: Boolean,
    sleepMinutes: Int,
    sleepLeftSeconds: Int,
    voices: List<VoiceOption>,
    voiceName: String?,
    voiceLabel: String,
    previewing: String?,
    canPreviousChapter: Boolean,
    canNextChapter: Boolean,
    bookLanguage: Locale,
    onSeek: (Float) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onPlayPause: () -> Unit,
    onSlower: () -> Unit,
    onFaster: () -> Unit,
    onSleep: (Int) -> Unit,
    onVoicePickerOpen: () -> Unit,
    onPreview: (VoiceOption, String) -> Unit,
    onPreviewStop: () -> Unit,
    onVoice: (String) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var draft by remember { mutableFloatStateOf(progress) }
    var voiceOpen by remember { mutableStateOf(false) }
    LaunchedEffect(progress, dragging) {
        if (!dragging) draft = progress
    }
    val shown = if (dragging) draft else progress
    val playLabel = stringResource(R.string.play)
    val pauseLabel = stringResource(R.string.pause)
    val speedLabel = stringResource(R.string.player_speed)
    val sleepLabel = stringResource(R.string.player_sleep)
    val voiceButtonLabel = stringResource(R.string.player_voice)
    val sample = stringResource(R.string.voice_preview_sample)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
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

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ReaderTheme.Ink.copy(alpha = 0.12f)),
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.player_voice_line, voiceLabel),
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = ReaderTheme.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(ReaderTheme.Raised)
                        .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.12f), RoundedCornerShape(17.dp))
                        .semantics { contentDescription = speedLabel },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RateStep(
                        enabled = canSlowDown,
                        description = stringResource(R.string.player_slower),
                        onClick = onSlower,
                    ) {
                        MinusGlyph(color = if (canSlowDown) ReaderTheme.Ink else ReaderTheme.InkSoft)
                    }
                    Text(
                        text = rateLabel,
                        modifier = Modifier.width(46.dp),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ReaderTheme.Ink,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    RateStep(
                        enabled = canSpeedUp,
                        description = stringResource(R.string.player_faster),
                        onClick = onFaster,
                    ) {
                        PlusGlyph(color = if (canSpeedUp) ReaderTheme.Ink else ReaderTheme.InkSoft)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.page_of_pages, page, pageCount),
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

        var sleepOpen by remember { mutableStateOf(false) }
        val sleepTint = if (sleepMinutes > 0) ReaderTheme.Accent else ReaderTheme.Ink

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TransportButton(
                    description = sleepLabel,
                    onClick = { sleepOpen = true },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ClockGlyph(color = sleepTint)
                        if (sleepMinutes > 0) {
                            Text(
                                text = sleepCountdown(sleepLeftSeconds),
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                color = ReaderTheme.Accent,
                                maxLines = 1,
                            )
                        }
                    }
                }
                DropdownMenu(
                    expanded = sleepOpen,
                    onDismissRequest = { sleepOpen = false },
                    containerColor = ReaderTheme.Paper,
                ) {
                    for (minutes in SLEEP_CHOICES) {
                        val label = if (minutes == 0) {
                            stringResource(R.string.sleep_off)
                        } else {
                            pluralStringResource(R.plurals.minutes, minutes, minutes)
                        }
                        val selected = sleepMinutes == minutes
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 14.sp,
                                    color = if (selected) ReaderTheme.Accent else ReaderTheme.Ink,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                sleepOpen = false
                                onSleep(minutes)
                            },
                            leadingIcon = {
                                if (selected) CheckGlyph(color = ReaderTheme.Accent)
                                else Spacer(modifier = Modifier.width(15.dp))
                            },
                        )
                    }
                }
            }

            TransportButton(
                description = stringResource(R.string.player_previous_chapter),
                enabled = canPreviousChapter,
                onClick = onPreviousChapter,
            ) {
                SkipPreviousGlyph(
                    color = if (canPreviousChapter) ReaderTheme.Ink
                    else ReaderTheme.Ink.copy(alpha = 0.30f),
                    glyphSize = 18.dp,
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(ReaderTheme.Accent)
                    .semantics {
                        contentDescription = if (playing) pauseLabel else playLabel
                        role = Role.Button
                    }
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                if (playing) {
                    PauseGlyph(color = ReaderTheme.Paper, glyphSize = 24.dp)
                } else {
                    PlayGlyph(color = ReaderTheme.Paper, glyphSize = 24.dp)
                }
            }

            TransportButton(
                description = stringResource(R.string.player_next_chapter),
                enabled = canNextChapter,
                onClick = onNextChapter,
            ) {
                SkipNextGlyph(
                    color = if (canNextChapter) ReaderTheme.Ink
                    else ReaderTheme.Ink.copy(alpha = 0.30f),
                    glyphSize = 18.dp,
                )
            }

            TransportButton(
                description = voiceButtonLabel,
                onClick = {
                    onVoicePickerOpen()
                    voiceOpen = true
                },
            ) { PersonGlyph() }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (voiceOpen) {
        VoicePicker(
            voices = voices,
            voiceName = voiceName,
            previewing = previewing,
            bookLanguage = bookLanguage,
            sample = sample,
            onPreview = onPreview,
            onStopPreview = onPreviewStop,
            onClose = {
                voiceOpen = false
                onPreviewStop()
            },
            onUse = { name ->
                voiceOpen = false
                onPreviewStop()
                onVoice(name)
            },
        )
    }
}

/** One transport icon: sleep, chapter skip or the voice picker. */
@Composable
private fun TransportButton(
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** One side of the speed capsule: a 32 × 34dp target with a minus or a plus. */
@Composable
private fun RateStep(
    enabled: Boolean,
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .height(34.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The voice picker: a bottom card with a search field, languages as groups and one row
 * per installed voice. Preview runs on the speech engine's own second instance, so it
 * never moves the book.
 */
@Composable
private fun VoicePicker(
    voices: List<VoiceOption>,
    voiceName: String?,
    previewing: String?,
    bookLanguage: Locale,
    sample: String,
    onPreview: (VoiceOption, String) -> Unit,
    onStopPreview: () -> Unit,
    onClose: () -> Unit,
    onUse: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val uiLocale = LocalConfiguration.current.locales[0]
    val groups = VoiceCatalog.groups(voices, bookLanguage, uiLocale, query)
    val context = LocalContext.current
    val closeLabel = stringResource(R.string.action_close)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ReaderTheme.Ink.copy(alpha = 0.25f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(ReaderTheme.Paper)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.player_voice),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ReaderTheme.Ink,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pluralStringResource(
                                R.plurals.voice_count_for_book,
                                voices.size,
                                voices.size,
                            ),
                            fontFamily = FontFamily.Serif,
                            fontSize = 12.sp,
                            color = ReaderTheme.InkSoft,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .semantics {
                                contentDescription = closeLabel
                                role = Role.Button
                            }
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) { CloseGlyph() }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ReaderTheme.Raised)
                        .border(
                            1.dp,
                            ReaderTheme.Ink.copy(alpha = 0.12f),
                            RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            color = ReaderTheme.Ink,
                        ),
                        cursorBrush = SolidColor(ReaderTheme.Accent),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.voice_search),
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 13.sp,
                                    color = ReaderTheme.InkSoft,
                                )
                            }
                            inner()
                        },
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                when {
                    voices.isEmpty() -> PickerNote(stringResource(R.string.voice_none))
                    groups.isEmpty() -> PickerNote(stringResource(R.string.voice_no_match))
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    ) {
                        groups.forEach { group ->
                            item(key = "group-${group.locale.toLanguageTag()}") {
                                Text(
                                    text = group.title,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = ReaderTheme.InkSoft,
                                )
                            }
                            items(group.voices, key = { it.name }) { voice ->
                                VoiceRow(
                                    voice = voice,
                                    selected = voice.name == voiceName,
                                    previewing = previewing == voice.name,
                                    onPreview = { onPreview(voice, sample) },
                                    onUse = { onUse(voice.name) },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ReaderTheme.Ink.copy(alpha = 0.12f)),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.voice_more),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onStopPreview()
                            val tts = Intent("com.android.settings.TTS_SETTINGS")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(tts) }.onFailure {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = ReaderTheme.Accent,
                )
                Text(
                    text = stringResource(R.string.voice_more_hint),
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    color = ReaderTheme.InkSoft,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/** One row: preview · name / quality and locale · Use or the tick of the current voice. */
@Composable
private fun VoiceRow(
    voice: VoiceOption,
    selected: Boolean,
    previewing: Boolean,
    onPreview: () -> Unit,
    onUse: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    1.dp,
                    ReaderTheme.Ink.copy(alpha = 0.16f),
                    RoundedCornerShape(18.dp),
                )
                .clickable(onClick = onPreview),
            contentAlignment = Alignment.Center,
        ) {
            if (previewing) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ReaderTheme.Accent),
                )
            } else {
                PlayGlyph(glyphSize = 13.dp)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = voice.label,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (selected) ReaderTheme.Accent else ReaderTheme.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = qualityLabel(voice.quality) + " · " + voice.locale.toLanguageTag(),
                fontFamily = FontFamily.Serif,
                fontSize = 11.sp,
                color = ReaderTheme.InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (selected) {
            CheckGlyph(color = ReaderTheme.Accent)
        } else {
            Text(
                text = stringResource(R.string.voice_use),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onUse)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = ReaderTheme.Accent,
            )
        }
    }
}

@Composable
private fun PickerNote(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        fontFamily = FontFamily.Serif,
        fontSize = 13.sp,
        color = ReaderTheme.InkSoft,
        textAlign = TextAlign.Center,
    )
}

/** `14:59` for the countdown beside the sleep timer. */
private fun sleepCountdown(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}

/** The engine's quality step, in the app's language. */
@Composable
private fun qualityLabel(quality: Int): String = stringResource(
    when {
        quality >= Voice.QUALITY_VERY_HIGH -> R.string.voice_quality_very_high
        quality >= Voice.QUALITY_HIGH -> R.string.voice_quality_high
        quality >= Voice.QUALITY_NORMAL -> R.string.voice_quality_medium
        quality >= Voice.QUALITY_LOW -> R.string.voice_quality_low
        else -> R.string.voice_quality_very_low
    },
)

/** Off, then the usual podcast choices. Labels are resolved by the sheet. */
private val SLEEP_CHOICES = listOf(0, 5, 15, 30, 60)
