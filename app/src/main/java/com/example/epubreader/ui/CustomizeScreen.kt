package com.example.epubreader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import android.text.SpannableStringBuilder
import com.example.epubreader.R
import com.example.epubreader.reader.FontCatalog
import com.example.epubreader.reader.Palette
import com.example.epubreader.reader.ReaderSettings
import com.example.epubreader.reader.ReaderTheme
import com.example.epubreader.reader.TypeConfig
import com.example.epubreader.reader.TypesetSpec
import com.example.epubreader.reader.Typesetting
import kotlin.math.roundToInt

/**
 * "Tuỳ chỉnh giao diện": a screen of its own, with the theme's whole typography on it.
 *
 * Every change is applied as it is made, so the reader behind re-typesets and the page
 * count moves while the sliders are dragged; the X puts back the configuration the
 * screen was opened with, and the tick keeps what is there.
 */
@Composable
fun CustomizeScreen(
    settings: ReaderSettings,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    /** The font list is the first thing on screen, for the screenshot harness. */
    openFontList: Boolean = false,
) {
    val themeId = settings.themeId
    val original = remember(themeId) { settings.configOf(themeId) }
    var draft by remember(themeId) { mutableStateOf(original) }
    var fontsOpen by remember { mutableStateOf(openFontList) }

    fun apply(next: TypeConfig) {
        draft = next
        settings.update(themeId) { next }
    }

    fun cancel() {
        // The screen's own undo: back to how it was when it opened.
        settings.update(themeId) { original }
        onCancel()
    }

    BackHandler(onBack = ::cancel)

    val ink = ReaderTheme.Ink
    val inkSoft = ReaderTheme.InkSoft
    val columnOptions = listOf(
        TypeConfig.COLUMNS_AUTO to stringResource(R.string.customize_columns_auto),
        1 to "1",
        2 to "2",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderTheme.Paper),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleButton(
                    onClick = { cancel() },
                    contentDescription = stringResource(R.string.action_close),
                ) { CloseGlyph() }
                Text(
                    text = stringResource(R.string.customize_title),
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ink,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CircleButton(
                    onClick = onApply,
                    active = true,
                    contentDescription = stringResource(R.string.action_save),
                ) { CheckGlyph(color = ReaderTheme.Paper) }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
                PreviewBlock(draft = draft, palette = settings.palette)

                SectionLabel(text = stringResource(R.string.customize_text_section))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fontsOpen = true }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(34.dp)) {
                        Text(
                            text = "Aa",
                            fontFamily = faceOf(draft),
                            fontWeight = if (draft.bold) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 20.sp,
                            color = ink,
                        )
                    }
                    Text(
                        text = stringResource(R.string.customize_font),
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        color = ink,
                    )
                    Text(
                        text = FontCatalog.labelOf(draft.fontKey),
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = inkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.rotate(180f).size(12.dp)) { ChevronGlyph(color = inkSoft) }
                }
                SwitchRow(
                    label = stringResource(R.string.customize_bold),
                    leading = {
                        Text(
                            text = "B",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ink,
                        )
                    },
                    checked = draft.bold,
                    onChange = { apply(draft.copy(bold = it)) },
                )

                SectionLabel(text = stringResource(R.string.customize_section))
                SwitchRow(
                    label = stringResource(R.string.customize_section),
                    checked = draft.custom,
                    onChange = { apply(draft.copy(custom = it)) },
                )
                SliderRow(
                    icon = { LineSpacingGlyph(color = ink) },
                    value = draft.lineSpacing,
                    range = LINE_RANGE,
                    step = 0.02f,
                    label = ReaderSettings.trimNumber(draft.lineSpacing),
                    enabled = draft.custom,
                    onChange = { apply(draft.copy(lineSpacing = it)) },
                )
                SliderRow(
                    icon = { LetterSpacingGlyph(color = ink) },
                    value = draft.letterSpacing,
                    range = LETTER_RANGE,
                    step = 0.01f,
                    label = ReaderSettings.twoDecimals(draft.letterSpacing),
                    enabled = draft.custom,
                    onChange = { apply(draft.copy(letterSpacing = it)) },
                )
                SliderRow(
                    icon = { WordSpacingGlyph(color = ink) },
                    value = draft.wordSpacing,
                    range = WORD_RANGE,
                    step = 0.02f,
                    label = ReaderSettings.twoDecimals(draft.wordSpacing),
                    enabled = draft.custom,
                    onChange = { apply(draft.copy(wordSpacing = it)) },
                )
                SliderRow(
                    icon = { MarginGlyph(color = ink) },
                    value = draft.marginDp,
                    range = MARGIN_RANGE,
                    step = 2f,
                    label = draft.marginDp.roundToInt().toString(),
                    enabled = draft.custom,
                    onChange = { apply(draft.copy(marginDp = it)) },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(34.dp)) { ColumnsGlyph(color = ink) }
                    Text(
                        text = stringResource(R.string.customize_columns),
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        color = ink,
                    )
                    Segmented(
                        options = columnOptions,
                        selected = draft.columns,
                        onSelect = { apply(draft.copy(columns = it)) },
                    )
                }

                SwitchRow(
                    label = stringResource(R.string.customize_justify),
                    leading = { JustifyGlyph(color = ink) },
                    checked = draft.justified,
                    onChange = { apply(draft.copy(justified = it)) },
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(RESET_RED)
                        .clickable { apply(TypeConfig.defaults(themeId)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.customize_reset),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = ComposeColor.White,
                    )
                }
            }
        }

        if (fontsOpen) {
            FontList(
                selected = draft.fontKey,
                onPick = { key ->
                    apply(draft.copy(fontKey = key))
                    fontsOpen = false
                },
                onDismiss = { fontsOpen = false },
            )
        }
    }
}

/** The page as it will be set, in the type that is being chosen. */
@Composable
private fun PreviewBlock(draft: TypeConfig, palette: Palette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ReaderTheme.PaperEdge)
            .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column {
            Text(
                text = "Aa",
                fontFamily = faceOf(draft),
                fontWeight = if (draft.bold) FontWeight.Bold else FontWeight.Normal,
                fontSize = (draft.sizeSp * 1.7f).sp,
                letterSpacing = draft.letterSpacing.em,
                color = ReaderTheme.Ink,
            )
            Spacer(modifier = Modifier.height(10.dp))
            PreviewParagraph(draft = draft, palette = palette)
        }
    }
}

/**
 * The paragraph under the "Aa", set by the page's own typesetter.
 *
 * A Compose `Text` could show the face, the size and two of the sliders, but it has no
 * word spacing and no justification mode, so the switch and the "giãn từ" slider would
 * have moved nothing. This builds a real `StaticLayout` through [Typesetting] — the same
 * paint, the same spans, the same justification mode the paginator uses — and draws it at
 * the preview's own width, inset by the page margin. Whatever the page does, this does.
 */
@Composable
private fun PreviewParagraph(draft: TypeConfig, palette: Palette) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val marginPx = with(density) { draft.marginDp.dp.toPx() }
        val textWidthPx = (with(density) { maxWidth.toPx() } - marginPx * 2f).coerceAtLeast(72f)
        val spec = remember(draft, density.density, marginPx, textWidthPx) {
            TypesetSpec(
                sizePx = draft.sizeSp * density.density,
                lineSpacing = draft.lineSpacing,
                letterSpacingEm = draft.letterSpacing,
                wordSpacingEm = draft.wordSpacing,
                marginPx = marginPx,
                columns = 1,
                justified = draft.justified,
                fontKey = draft.fontKey,
                bold = draft.bold,
            )
        }
        val layout = remember(spec, textWidthPx, palette) {
            val text = SpannableStringBuilder(PREVIEW_TEXT)
            Typesetting.prepare(text, spec, palette)
            Typesetting.layout(
                text = text,
                paint = Typesetting.bodyPaint(spec, palette.ink),
                widthPx = textWidthPx.toInt(),
                spec = spec,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { layout.height.toDp() }),
        ) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(marginPx, 0f)
                layout.draw(canvas.nativeCanvas)
                canvas.nativeCanvas.restore()
            }
        }
    }
}

/** The device's real faces, each row drawn in the face it offers. */
@Composable
private fun FontList(
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderTheme.Paper),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleButton(
                    onClick = onDismiss,
                    contentDescription = stringResource(R.string.reader_back),
                ) { ChevronGlyph() }
                Text(
                    text = stringResource(R.string.customize_font_list),
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ReaderTheme.Ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(44.dp))
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(FontCatalog.available().size) { index ->
                    val font = FontCatalog.available()[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(font.key) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = font.label,
                            modifier = Modifier.weight(1f),
                            fontFamily = remember(font.key) { FontFamily(FontCatalog.typeface(font.key, false)) },
                            fontSize = 17.sp,
                            color = ReaderTheme.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (font.key == selected) {
                            Box(modifier = Modifier.size(16.dp)) {
                                CheckGlyph(color = ReaderTheme.Accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 2.dp),
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = ReaderTheme.InkSoft,
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.CenterStart) { leading() }
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = ReaderTheme.Ink,
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ReaderTheme.Paper,
                checkedTrackColor = ReaderTheme.Accent,
                uncheckedThumbColor = ReaderTheme.Paper,
                uncheckedTrackColor = ReaderTheme.Ink.copy(alpha = 0.22f),
                uncheckedBorderColor = ReaderTheme.Ink.copy(alpha = 0.30f),
            ),
        )
    }
}

/** One icon, one slider, one value — the four controls that really move the page. */
@Composable
private fun SliderRow(
    icon: @Composable () -> Unit,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    label: String,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.CenterStart) { icon() }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = { raw ->
                val snapped = range.start + ((raw - range.start) / step).roundToInt() * step
                onChange(snapped.coerceIn(range.start, range.endInclusive))
            },
            valueRange = range,
            steps = (((range.endInclusive - range.start) / step).roundToInt() - 1).coerceAtLeast(0),
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = ReaderTheme.Accent,
                activeTrackColor = ReaderTheme.Accent,
                inactiveTrackColor = ReaderTheme.Ink.copy(alpha = 0.18f),
                disabledThumbColor = ReaderTheme.Ink.copy(alpha = 0.3f),
                disabledActiveTrackColor = ReaderTheme.Ink.copy(alpha = 0.3f),
                disabledInactiveTrackColor = ReaderTheme.Ink.copy(alpha = 0.12f),
            ),
        )
        Text(
            text = label,
            modifier = Modifier.width(54.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = ReaderTheme.Ink.copy(alpha = if (enabled) 1f else 0.4f),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

/** Two or three small choices in one capsule. */
@Composable
private fun Segmented(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ReaderTheme.Raised)
            .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((value, label) in options) {
            val active = value == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) ReaderTheme.Accent else ComposeColor.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (active) ReaderTheme.Paper else ReaderTheme.Ink,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The face a draft is set in: the real font, or the nearest one the device has. */
@Composable
private fun faceOf(draft: TypeConfig): FontFamily =
    remember(draft.fontKey, draft.bold) { FontFamily(FontCatalog.typeface(draft.fontKey, draft.bold)) }

private val LINE_RANGE = 1.0f..2.0f
private val LETTER_RANGE = -0.02f..0.20f
private val WORD_RANGE = 0f..0.60f
private val MARGIN_RANGE = 12f..56f

/** The same red the platform uses for a destructive action. */
private val RESET_RED = ComposeColor(0xFFB3261E)

private const val PREVIEW_TEXT =
    "Trăng đã lên, và con thuyền nhỏ trôi giữa những vì sao. " +
        "Người lái đò hát khe khẽ một câu hò cũ, như sợ đánh thức dòng sông."
