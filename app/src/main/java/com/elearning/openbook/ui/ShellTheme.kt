package com.elearning.openbook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * The shell's colours: Home, Library, Store and their dialogs.
 *
 * These follow the *phone's* light/dark setting and nothing else. The reader's paper,
 * ink and its moon are a separate palette (`ReaderTheme`) that lives inside a book, so
 * choosing a dark reading theme never repaints the shelf or survives as a shell theme.
 */
data class ShellColors(
    val dark: Boolean,
    val paper: Color,
    val raised: Color,
    val ink: Color,
    val inkSoft: Color,
    val accent: Color,
    val hairline: Color,
    val danger: Color,
)

@Composable
fun shellColors(): ShellColors = if (isSystemInDarkTheme()) DARK else LIGHT

private val LIGHT = ShellColors(
    dark = false,
    paper = Color(0xFFFBF7EE),
    raised = Color(0xFFF1EBDF),
    ink = Color(0xFF2A241D),
    inkSoft = Color(0xFF6E6459),
    accent = Color(0xFF9E5932),
    hairline = Color(0x1F2A241D),
    danger = Color(0xFFB3261E),
)

private val DARK = ShellColors(
    dark = true,
    paper = Color(0xFF0F0D0B),
    raised = Color(0xFF1E1B17),
    ink = Color(0xFFEDE6DA),
    inkSoft = Color(0xFF9C9385),
    accent = Color(0xFFD9A06A),
    hairline = Color(0x24EDE6DA),
    danger = Color(0xFFFFB4AB),
)

/** The shell's round icon button, the same shape the reader uses with the shell palette. */
@Composable
fun ShellIconButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    val colors = shellColors()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.raised)
            .border(1.dp, colors.hairline, CircleShape)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
