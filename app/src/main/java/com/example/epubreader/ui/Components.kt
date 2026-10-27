package com.example.epubreader.ui

import android.animation.ValueAnimator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.epubreader.model.BookItem
import com.example.epubreader.reader.ReaderTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Every icon in the reader is drawn at this size, so the chrome reads as one set. */
private val GLYPH = 16.dp

/** Icon size inside the small circular buttons, matching the iOS `buttonIconSize`. */
private val CIRCLE_GLYPH = 15.dp

/**
 * A soft drop shadow, drawn by hand.
 *
 * `Modifier.shadow` asks the platform for a shadow from the node's outline, and under
 * the emulator's software renderer that outline is approximated by a polygon — which
 * turns a circular button into a heptagon. A radial gradient costs one draw call and
 * looks the same everywhere.
 */
private fun DrawScope.softCircleShadow(alpha: Float = 0.14f) {
    val halo = 5.dp.toPx()
    val centre = Offset(center.x, center.y + halo * 0.28f)
    val radius = size.minDimension / 2f + halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                ComposeColor.Black.copy(alpha = alpha),
                ComposeColor.Black.copy(alpha = 0f),
            ),
            center = centre,
            radius = radius,
        ),
        radius = radius,
        center = centre,
    )
}

/** The same idea for the player's rounded bar: stacked rectangles, fading outwards. */
fun DrawScope.softRoundShadow(cornerRadius: Float, alpha: Float = 0.05f) {
    val lift = 2.dp.toPx()
    for (step in 0..3) {
        val grow = step * 1.6f.dp.toPx()
        drawRoundRect(
            color = ComposeColor.Black.copy(alpha = alpha),
            topLeft = Offset(-grow, -grow + lift),
            size = Size(size.width + grow * 2f, size.height + grow * 2f),
            cornerRadius = CornerRadius(cornerRadius + grow, cornerRadius + grow),
        )
    }
}

/**
 * The reader's one round button. Toolbar, audio and settings all come through here,
 * so they are the same size, same fill, same ring and same shadow — a button that
 * only *looks* a different size is still a bug.
 */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind { softCircleShadow() }
            .clip(CircleShape)
            .background(if (active) ReaderTheme.Accent else ReaderTheme.Paper.copy(alpha = 0.82f))
            .border(
                1.dp,
                if (active) ReaderTheme.Accent else ReaderTheme.Ink.copy(alpha = 0.20f),
                CircleShape,
            )
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/**
 * One toolbar group: two icons in one capsule, with a hairline rule between them.
 *
 * The pair is one piece of material — same fill, same ring, same shadow as
 * [CircleButton] — so the bar reads as two groups rather than four loose circles. Each
 * icon keeps its own 44dp target: the capsule is wider than a button, but a tap is
 * still a tap on one icon, never on the seam.
 */
@Composable
fun ButtonPair(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .drawBehind { softRoundShadow(cornerRadius = 22.dp.toPx()) }
            .clip(RoundedCornerShape(22.dp))
            .background(ReaderTheme.Paper.copy(alpha = 0.82f))
            .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.20f), RoundedCornerShape(22.dp)),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** One icon inside a [ButtonPair]; the same 44dp target as a round button. */
@Composable
fun PairButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** The seam between two icons of a [ButtonPair]: a hairline, not a full-height rule. */
@Composable
fun PairDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(ReaderTheme.Ink.copy(alpha = 0.16f)),
    )
}

/**
 * A plain touch target around a bare icon, used by the player and the settings rows.
 * The icon is quiet when the action is not available, but the target stays.
 *
 * [width] is the frame's own width, 44dp by default. The player narrows it to 32dp so
 * four controls take 48dp less of the row and the headline gets that room back; the
 * height stays 44dp, so the target is still a thumb even where the frame is slim.
 */
@Composable
fun PanelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    width: Dp = 44.dp,
    height: Dp = 44.dp,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .then(
                if (description == null) Modifier
                else Modifier.semantics { contentDescription = description },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.alpha(if (enabled) 1f else 0.3f)) { content() }
    }
}

/** A back chevron, drawn by hand so the app needs no icon dependency. */
@Composable
fun ChevronGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 2f.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.72f, stroke / 2f)
            lineTo(size.width * 0.26f, size.height / 2f)
            lineTo(size.width * 0.72f, size.height - stroke / 2f)
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

/** Three stacked rules, for the table of contents. */
@Composable
fun ListGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 1.8f.dp.toPx()
        val dotX = size.width * 0.14f
        for (i in 0..2) {
            val y = size.height * (0.18f + i * 0.32f)
            drawLine(
                color = color,
                start = Offset(size.width * 0.36f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawCircle(color = color, radius = stroke * 0.64f, center = Offset(dotX, y))
        }
    }
}

/** A magnifier, for finding something in the book. */
@Composable
fun SearchGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 1.9.dp.toPx()
        val radius = size.minDimension * 0.29f
        val centre = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(
            color = color,
            radius = radius,
            center = centre,
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(centre.x + radius * 0.72f, centre.y + radius * 0.72f),
            end = Offset(size.width * 0.90f, size.height * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * A bookmark: a ribbon with a notch cut out of its foot. It is drawn hollow when the
 * page is not marked and solid when it is, so the toolbar says which state the page is
 * in without a second control.
 */
@Composable
fun BookmarkGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 1.8.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.26f, size.height * 0.12f)
            lineTo(size.width * 0.74f, size.height * 0.12f)
            lineTo(size.width * 0.74f, size.height * 0.88f)
            lineTo(size.width * 0.50f, size.height * 0.68f)
            lineTo(size.width * 0.26f, size.height * 0.88f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = if (filled) Fill else Stroke(width = stroke, join = StrokeJoin.Round),
        )
    }
}

/** A plus, for importing a book and for stepping a setting up. */
@Composable
fun PlusGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(GLYPH)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width / 2f, stroke / 2f),
            end = Offset(size.width / 2f, size.height - stroke / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(stroke / 2f, size.height / 2f),
            end = Offset(size.width - stroke / 2f, size.height / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** A minus, for stepping a setting down. */
@Composable
fun MinusGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(GLYPH)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(stroke / 2f, size.height / 2f),
            end = Offset(size.width - stroke / 2f, size.height / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** A cog: the reader's own settings. */
@Composable
fun GearGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 1.7.dp.toPx()
        val centre = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.29f
        drawCircle(color = color, radius = radius, center = centre, style = Stroke(width = stroke))
        repeat(8) { index ->
            val angle = index * 45f * PI.toFloat() / 180f
            val dx = cos(angle)
            val dy = sin(angle)
            drawLine(
                color = color,
                start = centre + Offset(dx * radius, dy * radius),
                end = centre + Offset(dx * (radius + stroke * 1.7f), dy * (radius + stroke * 1.7f)),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** A headset, for starting the read-aloud. */
@Composable
fun EarphoneGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(GLYPH)) {
        val stroke = 1.9.dp.toPx()
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.10f, size.height * 0.08f),
            size = Size(size.width * 0.80f, size.height * 0.84f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val cupW = size.width * 0.20f
        val cupH = size.height * 0.30f
        val cupY = size.height * 0.46f
        val corner = CornerRadius(cupW / 2.4f, cupW / 2.4f)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.02f, cupY),
            size = Size(cupW, cupH),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.78f, cupY),
            size = Size(cupW, cupH),
            cornerRadius = corner,
        )
    }
}

/**
 * The seek pair: a ring broken at the top with an arrowhead closing one way — the
 * media player's own "back" and "forward", so the player reads at a glance.
 */
private fun DrawScope.seekRing(color: ComposeColor, stroke: Float, forward: Boolean) {
    val radius = size.minDimension / 2f - stroke * 0.7f
    val centre = Offset(size.width / 2f, size.height / 2f)
    // The ring is open across the top, where the arrowhead sits.
    val headAngle = if (forward) -55f else -125f
    drawArc(
        color = color,
        startAngle = -55f,
        sweepAngle = 290f,
        useCenter = false,
        topLeft = Offset(centre.x - radius, centre.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    val theta = Math.toRadians(headAngle.toDouble())
    val apex = Offset(
        centre.x + (radius * cos(theta)).toFloat(),
        centre.y + (radius * sin(theta)).toFloat(),
    )
    // Travel: clockwise for forward, the other way for back. The arms point behind it.
    val travel = if (forward) {
        Offset((-sin(theta)).toFloat(), cos(theta).toFloat())
    } else {
        Offset(sin(theta).toFloat(), (-cos(theta)).toFloat())
    }
    val arm = stroke * 2.8f
    for (sign in listOf(-1f, 1f)) {
        val r = Math.toRadians((sign * 38f).toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        val rotated = Offset(travel.x * c - travel.y * s, travel.x * s + travel.y * c)
        drawLine(
            color = color,
            start = apex,
            end = Offset(apex.x - rotated.x * arm, apex.y - rotated.y * arm),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** Read it a step at a time: the player's rewind. */
@Composable
fun SeekBackGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = GLYPH,
) {
    Icon(
        imageVector = Icons.Filled.Speed,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(glyphSize),
    )
}

/** Read it a step at a time: the player's fast-forward. */
@Composable
fun SeekForwardGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = GLYPH,
) {
    Icon(
        imageVector = Icons.Filled.Speed,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(glyphSize),
    )
}

/**
 * Material 3's `skip_previous`: an upright bar with a triangle pointing back at it.
 * The player uses it for chapters, so it cannot look like the speed ring beside it.
 */
@Composable
fun SkipPreviousGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = GLYPH,
) {
    Canvas(modifier.size(glyphSize)) {
        val bar = size.width * 0.12f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.14f, size.height * 0.12f),
            size = Size(bar, size.height * 0.76f),
            cornerRadius = CornerRadius(bar / 2f, bar / 2f),
        )
        val triangle = Path().apply {
            moveTo(size.width * 0.88f, size.height * 0.10f)
            lineTo(size.width * 0.88f, size.height * 0.90f)
            lineTo(size.width * 0.32f, size.height * 0.50f)
            close()
        }
        drawPath(triangle, color)
    }
}

/** Material 3's `skip_next`: the same bar and triangle, facing forward. */
@Composable
fun SkipNextGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = GLYPH,
) {
    Canvas(modifier.size(glyphSize)) {
        val bar = size.width * 0.12f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.74f, size.height * 0.12f),
            size = Size(bar, size.height * 0.76f),
            cornerRadius = CornerRadius(bar / 2f, bar / 2f),
        )
        val triangle = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.10f)
            lineTo(size.width * 0.12f, size.height * 0.90f)
            lineTo(size.width * 0.68f, size.height * 0.50f)
            close()
        }
        drawPath(triangle, color)
    }
}

/** A pencil, for "Edit": a slanted outline with a solid tip. */
@Composable
fun EditGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 1.5.dp.toPx()
        rotate(degrees = -45f) {
            val bodyWidth = size.width * 0.24f
            val bodyHeight = size.height * 0.62f
            drawRoundRect(
                color = color,
                topLeft = Offset((size.width - bodyWidth) / 2f, size.height * 0.08f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                style = Stroke(width = stroke),
            )
            val tip = Path().apply {
                val left = (size.width - bodyWidth) / 2f
                val right = left + bodyWidth
                moveTo(left, size.height * 0.70f)
                lineTo(size.width / 2f, size.height * 0.92f)
                lineTo(right, size.height * 0.70f)
                close()
            }
            drawPath(tip, color)
        }
    }
}

/** A waste bin, for "Delete": a lid, a tapered body and two ribs. */
@Composable
fun TrashGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 1.5.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.26f),
            end = Offset(size.width * 0.82f, size.height * 0.26f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.40f, size.height * 0.16f),
            end = Offset(size.width * 0.60f, size.height * 0.16f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val body = Path().apply {
            moveTo(size.width * 0.26f, size.height * 0.30f)
            lineTo(size.width * 0.32f, size.height * 0.86f)
            lineTo(size.width * 0.68f, size.height * 0.86f)
            lineTo(size.width * 0.74f, size.height * 0.30f)
        }
        drawPath(body, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        for (x in listOf(0.43f, 0.57f)) {
            drawLine(
                color = color,
                start = Offset(size.width * x, size.height * 0.40f),
                end = Offset(size.width * x, size.height * 0.74f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** The sun: switch the whole app to its light face. */
@Composable
fun SunGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = GLYPH,
) {
    Canvas(modifier.size(glyphSize)) {
        val stroke = 1.7.dp.toPx()
        val centre = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.24f
        drawCircle(color = color, radius = radius, center = centre, style = Stroke(width = stroke))
        repeat(8) { index ->
            val angle = index * 45f * PI.toFloat() / 180f
            val dx = cos(angle)
            val dy = sin(angle)
            drawLine(
                color = color,
                start = centre + Offset(dx * radius * 1.45f, dy * radius * 1.45f),
                end = centre + Offset(dx * radius * 2.05f, dy * radius * 2.05f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** The moon: switch the whole app to its dark face. */
@Composable
fun MoonGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = GLYPH,
) {
    Canvas(modifier.size(glyphSize)) {
        val centre = Offset(size.width * 0.58f, size.height * 0.5f)
        val radius = size.minDimension * 0.40f
        val dx = radius * 0.5f
        val rho = kotlin.math.sqrt(radius * radius + dx * dx)
        val join = Math.toDegrees(kotlin.math.atan2(radius.toDouble(), (-dx).toDouble())).toFloat()
        val path = Path().apply {
            moveTo(centre.x, centre.y - radius)
            arcTo(
                Rect(centre.x - radius, centre.y - radius, centre.x + radius, centre.y + radius),
                -90f,
                -180f,
                false,
            )
            arcTo(
                Rect(centre.x + dx - rho, centre.y - rho, centre.x + dx + rho, centre.y + rho),
                join,
                2f * (180f - join),
                false,
            )
            close()
        }
        drawPath(path = path, color = color)
    }
}

/** The tick that keeps an edit. */
@Composable
fun CheckGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 2.1.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.54f)
            lineTo(size.width * 0.40f, size.height * 0.80f)
            lineTo(size.width * 0.88f, size.height * 0.22f)
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

/** A clock face: "recently imported" in the shelf menu. */
@Composable
fun ClockGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 1.6f.dp.toPx()
        drawCircle(
            color = color,
            radius = size.minDimension / 2f - stroke / 2f,
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(size.width / 2f, size.height * 0.28f),
            end = Offset(size.width / 2f, size.height / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width / 2f, size.height / 2f),
            end = Offset(size.width * 0.68f, size.height * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** Head and shoulders: "author" in the shelf menu. */
@Composable
fun PersonGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 1.6f.dp.toPx()
        drawCircle(
            color = color,
            radius = size.width * 0.18f,
            center = Offset(size.width / 2f, size.height * 0.30f),
            style = Stroke(width = stroke),
        )
        val path = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.92f)
            cubicTo(
                size.width * 0.18f,
                size.height * 0.58f,
                size.width * 0.82f,
                size.height * 0.58f,
                size.width * 0.84f,
                size.height * 0.92f,
            )
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

/** Three rules with air between them: the line-spacing control. */
@Composable
fun LineSpacingGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(GLYPH)) {
        val stroke = 1.6.dp.toPx()
        val left = size.width * 0.34f
        for (ratio in listOf(0.16f, 0.5f, 0.84f)) {
            drawLine(
                color = color,
                start = Offset(left, size.height * ratio),
                end = Offset(size.width, size.height * ratio),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        val arrowX = size.width * 0.13f
        drawLine(
            color = color,
            start = Offset(arrowX, size.height * 0.20f),
            end = Offset(arrowX, size.height * 0.80f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(arrowX, size.height * 0.20f),
            end = Offset(arrowX - stroke * 1.2f, size.height * 0.34f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(arrowX, size.height * 0.80f),
            end = Offset(arrowX - stroke * 1.2f, size.height * 0.66f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** Two stems with air between: the letter-spacing control. */
@Composable
fun LetterSpacingGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(GLYPH)) {
        val stroke = 1.6.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.10f, size.height * 0.16f),
            end = Offset(size.width * 0.10f, size.height * 0.84f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.90f, size.height * 0.16f),
            end = Offset(size.width * 0.90f, size.height * 0.84f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawDoubleArrow(color, stroke, size.width * 0.32f, size.width * 0.68f, size.height * 0.5f)
    }
}

/** Two words with air between: the word-spacing control. */
@Composable
fun WordSpacingGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(GLYPH)) {
        val stroke = 1.6.dp.toPx()
        val corner = CornerRadius(stroke * 0.8f, stroke * 0.8f)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.02f, size.height * 0.34f),
            size = Size(size.width * 0.28f, size.height * 0.32f),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.70f, size.height * 0.34f),
            size = Size(size.width * 0.28f, size.height * 0.32f),
            cornerRadius = corner,
        )
        drawDoubleArrow(color, stroke * 0.9f, size.width * 0.38f, size.width * 0.62f, size.height * 0.5f)
    }
}

/** A page with its margins marked: the margin control. */
@Composable
fun MarginGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(GLYPH)) {
        val stroke = 1.5.dp.toPx()
        drawRect(
            color = color,
            topLeft = Offset(size.width * 0.06f, size.height * 0.10f),
            size = Size(size.width * 0.88f, size.height * 0.80f),
            style = Stroke(width = stroke),
        )
        val inset = size.width * 0.26f
        for (x in listOf(inset, size.width - inset)) {
            var y = size.height * 0.22f
            while (y < size.height * 0.78f) {
                drawLine(
                    color = color,
                    start = Offset(x, y),
                    end = Offset(x, y + size.height * 0.09f),
                    strokeWidth = stroke * 0.9f,
                    cap = StrokeCap.Round,
                )
                y += size.height * 0.17f
            }
        }
    }
}

/** Two columns of rules: the columns control. */
@Composable
fun ColumnsGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(GLYPH)) {
        val stroke = 1.5.dp.toPx()
        val columnWidth = size.width * 0.38f
        for (column in 0..1) {
            val left = if (column == 0) size.width * 0.04f else size.width * 0.58f
            for (index in 0..3) {
                val y = size.height * (0.14f + index * 0.24f)
                drawLine(
                    color = color,
                    start = Offset(left, y),
                    end = Offset(left + columnWidth, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/** A two-headed arrow is not enough: three rules flush at both ends. */
@Composable
fun JustifyGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(GLYPH)) {
        val stroke = 1.6.dp.toPx()
        for (ratio in listOf(0.16f, 0.5f, 0.84f)) {
            drawLine(
                color = color,
                start = Offset(size.width * 0.04f, size.height * ratio),
                end = Offset(size.width * 0.96f, size.height * ratio),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** A two-headed arrow, drawn at [y] between [from] and [to]. */
private fun DrawScope.drawDoubleArrow(
    color: ComposeColor,
    stroke: Float,
    from: Float,
    to: Float,
    y: Float,
) {
    drawLine(
        color = color,
        start = Offset(from, y),
        end = Offset(to, y),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    for ((x, sign) in listOf(from to 1f, to to -1f)) {
        drawLine(
            color = color,
            start = Offset(x, y),
            end = Offset(x + sign * stroke * 1.6f, y - stroke * 1.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(x, y),
            end = Offset(x + sign * stroke * 1.6f, y + stroke * 1.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** Pause the voice. */
@Composable
fun PauseGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = GLYPH,
) {
    Canvas(modifier.size(glyphSize)) {
        val barW = size.width * 0.20f
        val corner = CornerRadius(barW / 2f, barW / 2f)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.22f, size.height * 0.10f),
            size = Size(barW, size.height * 0.80f),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.58f, size.height * 0.10f),
            size = Size(barW, size.height * 0.80f),
            cornerRadius = corner,
        )
    }
}

/** Start, or carry on, the voice. */
@Composable
fun PlayGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = GLYPH,
) {
    Canvas(modifier.size(glyphSize)) {
        val path = Path().apply {
            moveTo(size.width * 0.24f, size.height * 0.10f)
            lineTo(size.width * 0.86f, size.height * 0.50f)
            lineTo(size.width * 0.24f, size.height * 0.90f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

/** Close the player, which stops the voice. */
@Composable
fun CloseGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(CIRCLE_GLYPH),
    )
}

@Composable
private fun LegacyCloseGlyph(
    color: ComposeColor,
    modifier: Modifier,
) {
    Canvas(modifier.size(CIRCLE_GLYPH)) {
        val stroke = 1.9.dp.toPx()
        val pad = size.width * 0.22f
        drawLine(
            color = color,
            start = Offset(pad, pad),
            end = Offset(size.width - pad, size.height - pad),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width - pad, pad),
            end = Offset(pad, size.height - pad),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * The book's cover shrunk to a disc and spun like a record on a turntable.
 *
 * The spin comes from a timer rather than an animation, so pausing freezes the disc
 * exactly where it was. The gradient runs corner to corner and the glint sits
 * off-centre, which is what makes the rotation readable — a symmetric disc would
 * look static at any angle.
 */
@Composable
fun VinylDisc(
    title: String,
    spinning: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    var angle by remember { mutableFloatStateOf(0f) }

    // 30 degrees a second, measured, so a slow or busy frame never changes the speed.
    // Pausing simply stops updating: the disc holds the exact angle it reached.
    val motion = motionEnabled()
    LaunchedEffect(spinning, motion) {
        if (!spinning || !motion) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val seconds = (now - last) / 1_000_000_000f
                last = now
                angle = (angle + DISC_DEGREES_PER_SECOND * seconds) % 360f
            }
        }
    }

    val palette = ReaderTheme.coverPalette(title)
    val holeColor = ComposeColor(0xB8000000)

    Canvas(modifier.size(size)) {
        rotate(degrees = angle, pivot = Offset(this.size.width / 2f, this.size.height / 2f)) {
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(palette.first, palette.second),
                    start = Offset.Zero,
                    end = Offset(this.size.width, this.size.height),
                ),
            )
            for (ratio in listOf(0.74f, 0.56f, 0.40f)) {
                drawCircle(
                    color = ComposeColor.Black.copy(alpha = 0.20f),
                    radius = this.size.minDimension * ratio / 2f,
                    center = center,
                    style = Stroke(width = 1f.dp.toPx()),
                )
            }
            // A soft glint, drawn as a gradient rather than a blur so it works on
            // every API level.
            val glintCentre = Offset(center.x, center.y - this.size.height * 0.31f)
            val glintRadius = this.size.minDimension * 0.115f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ComposeColor.White.copy(alpha = 0.55f),
                        ComposeColor.White.copy(alpha = 0f),
                    ),
                    center = glintCentre,
                    radius = glintRadius * 2.1f,
                ),
                radius = glintRadius * 2.1f,
                center = glintCentre,
            )
            drawCircle(
                color = ReaderTheme.Paper.copy(alpha = 0.9f),
                radius = this.size.minDimension * 0.15f,
                center = center,
            )
            drawCircle(
                color = holeColor,
                radius = this.size.minDimension * 0.0375f,
                center = center,
            )
        }
    }
}

/**
 * A one-line label that scrolls itself when the space it is given is too narrow for
 * the whole string, and stands still when it is not.
 *
 * The string is laid out twice, so the second copy slides in as the first leaves and
 * the run loops seamlessly. Both copies are measured **unbounded**: a bounded row
 * clamps each one to the width on screen, so the sliding string would be clipped away
 * to nothing as soon as it moved (a blank label — see the report's appearance pass). The movement comes from a timer rather than a repeating
 * animation, for the same reason the record spins that way: it can be stopped
 * exactly where it is.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
    color: ComposeColor = ReaderTheme.Ink,
    scrolling: Boolean = true,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        color = color,
    )

    val textWidthPx = remember(text, style) {
        measurer.measure(AnnotatedString(text), style).size.width.toFloat()
    }
    var containerWidth by remember { mutableIntStateOf(0) }
    var offsetPx by remember { mutableFloatStateOf(0f) }

    val motionOn = motionEnabled()
    val gapPx = with(density) { MARQUEE_GAP.toPx() }
    val stepPx = with(density) { (MARQUEE_SPEED / TICKS_PER_SECOND).dp.toPx() }
    val travels = containerWidth > 0 && textWidthPx > containerWidth

    LaunchedEffect(text, travels, textWidthPx, containerWidth, scrolling) {
        // Held at the start whenever it is not scrolling, so a stopped player
        // still shows which book and chapter it is on.
        offsetPx = 0f
        if (!travels || !scrolling || !motionOn) return@LaunchedEffect
        val span = textWidthPx + gapPx
        while (true) {
            delay(TICK_MS)
            offsetPx -= stepPx
            if (offsetPx <= -span) offsetPx += span
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .offset { IntOffset(offsetPx.roundToInt(), 0) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarqueeRun(text, style)
            if (travels) {
                Spacer(modifier = Modifier.width(MARQUEE_GAP))
                MarqueeRun(text, style)
            }
        }
    }
}

@Composable
private fun MarqueeRun(text: String, style: TextStyle) {
    Text(text = text, style = style, maxLines = 1, softWrap = false)
}

/**
 * The generated cover, in the shelf and in the audio player: the same gradient and the
 * same spine, so the book is one object wherever it appears. The title and author live
 * outside the cover — printing them twice made the cover fight the label beside it.
 */
@Composable
fun BookCover(book: BookItem, modifier: Modifier = Modifier) {
    val (top, bottom) = ReaderTheme.coverPalette(book.title)
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .aspectRatio(0.68f)
            .shadow(9.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(top, bottom))),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(5.dp)
                .background(ComposeColor.Black.copy(alpha = 0.22f)),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, ComposeColor.White.copy(alpha = 0.22f), shape),
        )
    }
}

/** The home tab: a roof over a page. */
@Composable
fun HomeGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(22.dp)) {
        val stroke = 1.7f.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.46f)
            lineTo(size.width * 0.5f, size.height * 0.15f)
            lineTo(size.width * 0.88f, size.height * 0.46f)
            moveTo(size.width * 0.22f, size.height * 0.40f)
            lineTo(size.width * 0.22f, size.height * 0.86f)
            lineTo(size.width * 0.78f, size.height * 0.86f)
            lineTo(size.width * 0.78f, size.height * 0.40f)
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** The library tab: a closed book, spine on the left. */
@Composable
fun LibraryGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(22.dp)) {
        val stroke = 1.7f.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.20f, size.height * 0.13f),
            size = Size(size.width * 0.60f, size.height * 0.74f),
            cornerRadius = CornerRadius(2.5f.dp.toPx()),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.34f, size.height * 0.16f),
            end = Offset(size.width * 0.34f, size.height * 0.84f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** The store tab: a shopping bag. */
@Composable
fun StoreGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
) {    Canvas(modifier.size(22.dp)) {
        val stroke = 1.7f.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.17f, size.height * 0.36f),
            size = Size(size.width * 0.66f, size.height * 0.50f),
            cornerRadius = CornerRadius(3f.dp.toPx()),
            style = Stroke(width = stroke),
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.34f, size.height * 0.14f),
            size = Size(size.width * 0.32f, size.height * 0.44f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}


/** Four tiles: the shelf's grid layout. */
@Composable
fun GridGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = 18.dp,
) {
    Canvas(modifier.size(glyphSize)) {
        val stroke = 1.6.dp.toPx()
        val cell = size.width * 0.40f
        for (x in listOf(size.width * 0.06f, size.width * 0.54f)) {
            for (y in listOf(size.height * 0.06f, size.height * 0.54f)) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(width = stroke),
                )
            }
        }
    }
}

/** Three dots: the row's own actions. */
@Composable
fun MoreGlyph(
    color: ComposeColor = ReaderTheme.Ink,
    modifier: Modifier = Modifier,
    glyphSize: Dp = 18.dp,
) {
    Canvas(modifier.size(glyphSize)) {
        val radius = size.width * 0.075f
        for (ratio in listOf(0.20f, 0.50f, 0.80f)) {
            drawCircle(color = color, radius = radius, center = Offset(size.width * ratio, size.height / 2f))
        }
    }
}

/** Matches the record's tick, so the marquee and the disc look like one system. */
private const val TICK_MS = 33L
private const val TICKS_PER_SECOND = 30f

/** Points travelled per second. */
/** The disc's real speed, from the spec: one turn every twelve seconds. */
private const val DISC_DEGREES_PER_SECOND = 30f

/**
 * Animator duration scale 0 (Developer options / "remove animations") turns the disc and
 * the marquee into stills; Compose's own transitions already follow the same setting.
 */
@Composable
private fun motionEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()

private const val MARQUEE_SPEED = 26f

/** Blank run between the end of the string and its repeat. */
private val MARQUEE_GAP = 40.dp
