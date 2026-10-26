package com.example.epubreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.epubreader.R
import com.example.epubreader.reader.Palette
import com.example.epubreader.reader.ReaderSettings
import com.example.epubreader.reader.ReaderTheme
import com.example.epubreader.reader.ThemeId
import com.example.epubreader.reader.TypeConfig

/**
 * The reading settings panel: the theme cards, the A/A size pill, the moon and sun, and
 * the way through to the full appearance screen.
 *
 * Every colour here comes from the palette in force, so the panel turns over with the
 * app — and on a dark paper the pill and the sun sit on a *raised* fill, one shade
 * lighter than the sheet they are on.
 */
@Composable
fun AppearanceSheet(
    settings: ReaderSettings,
    onCustomize: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.appearance_title),
            modifier = Modifier.weight(1f),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = ReaderTheme.Ink,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SizePill(settings = settings, modifier = Modifier.weight(1f))
        ModeButton(dark = settings.dark, modifier = Modifier.width(84.dp)) { settings.toggleDark() }
    }

    ThemeGrid(settings = settings)

    Spacer(modifier = Modifier.height(18.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(56.dp)
            .clip(CircleShape)
            .background(ReaderTheme.Raised)
            .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onCustomize),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearGlyph()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.appearance_customize),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = ReaderTheme.Ink,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.rotate(180f).size(12.dp)) {
            ChevronGlyph(color = ReaderTheme.Ink.copy(alpha = 0.6f))
        }
    }

    Spacer(modifier = Modifier.height(28.dp))
}

/** `A` small, a rule, `A` large: the reader's type size in one control. */
@Composable
private fun SizePill(settings: ReaderSettings, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(66.dp)
            .drawBehind { softRoundShadow(20.dp.toPx()) }
            .clip(RoundedCornerShape(20.dp))
            .background(ReaderTheme.Raised)
            .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(66.dp)
                .clickable(enabled = settings.canShrinkText) { settings.shrinkText() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "A",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = ReaderTheme.Ink.copy(alpha = if (settings.canShrinkText) 1f else 0.3f),
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(34.dp)
                .background(ReaderTheme.Ink.copy(alpha = 0.18f)),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(66.dp)
                .clickable(enabled = settings.canGrowText) { settings.growText() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "A",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = ReaderTheme.Ink.copy(alpha = if (settings.canGrowText) 1f else 0.3f),
            )
        }
    }
}

/** The moon and the sun: the same theme, its other face, for the whole app. */
@Composable
private fun ModeButton(
    dark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(66.dp)
            .drawBehind { softRoundShadow(20.dp.toPx()) }
            .clip(RoundedCornerShape(20.dp))
            .background(ReaderTheme.Raised)
            .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(22.dp)) {
            if (dark) SunGlyph() else MoonGlyph()
        }
    }
}

/** Three cards to a row: six themes, each drawn in its own colours, `*` when altered. */
@Composable
private fun ThemeGrid(settings: ReaderSettings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (row in ThemeId.entries.chunked(3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (id in row) {
                    ThemeCard(
                        id = id,
                        palette = settings.paletteOf(id),
                        selected = id == settings.themeId,
                        modified = settings.isModified(id),
                        bold = settings.configOf(id).bold,
                        onClick = { settings.setTheme(id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    id: ThemeId,
    palette: Palette,
    selected: Boolean,
    modified: Boolean,
    bold: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val paper = ComposeColor(palette.paper)
    val ink = ComposeColor(palette.ink)
    Box(
        modifier = modifier
            .height(104.dp)
            .drawBehind { softRoundShadow(16.dp.toPx(), alpha = 0.08f) }
            .clip(shape)
            .background(paper)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) ComposeColor(palette.accent) else ink.copy(alpha = 0.14f),
                shape = shape,
            )
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Aa",
                fontFamily = FontFamily.Serif,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 30.sp,
                color = ink,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = id.label,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = ink,
            )
        }
        if (modified) {
            // The theme has been changed away from its own defaults.
            Text(
                text = "*",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 10.dp),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ink,
            )
        }
    }
}
