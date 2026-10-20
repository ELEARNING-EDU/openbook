package com.example.epubreader.reader

import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * The six reader themes, named as the iOS reference names them. A theme is a look,
 * not just a colour: each one also carries its own default typography (see
 * [TypeConfig.defaults]).
 */
enum class ThemeId(val key: String, val label: String) {
    ORIGINAL("original", "Original"),
    QUIET("quiet", "Quiet"),
    PAPER("paper", "Paper"),
    BOLD("bold", "Bold"),
    CALM("calm", "Calm"),
    FOCUS("focus", "Focus"),
    ;

    companion object {
        /** The iOS build opens on `paper`. */
        val default = PAPER

        fun of(key: String?): ThemeId = entries.firstOrNull { it.key == key } ?: default
    }
}

/**
 * One theme's colours, in ARGB. Kept as plain ints because the reader's paints want
 * ints; the Compose side goes through [ReaderTheme].
 *
 * [dark] is *derived from the paper itself*, exactly as the iOS `ReaderPalette.isDark`
 * is: a light variant of a dark-looking theme (Quiet's) is still a dark surface, and
 * every raised control has to know that.
 */
class Palette(
    val paper: Int,
    val paperEdge: Int,
    val ink: Int,
    val inkSoft: Int,
    val accent: Int,
) {
    val dark: Boolean = perceivedBrightness(paper) < 0.5f

    /**
     * The fill of a control that must read as *raised* off the panel: lighter than the
     * paper on a dark theme, a shade deeper on a light one.
     */
    val raised: Int =
        if (dark) blend(paper, Color.WHITE, 0.12f) else blend(paper, Color.BLACK, 0.05f)

    /** A hairline for dividers and card edges. */
    val hairline: Int = blend(ink, paper, 0.82f)

    /** The amber wash over the sentence being read, as on iOS. */
    val highlight: Int = HIGHLIGHT

    override fun toString(): String =
        "#%06X/%06X".format(paper and 0xFFFFFF, ink and 0xFFFFFF)

    companion object {
        /** `ReaderTheme.highlight` on iOS: 0.98, 0.78, 0.33 at 42% alpha. */
        const val HIGHLIGHT: Int = 0x6BFAC954.toInt()

        /** The tables below are written as RGB; a colour needs its alpha to be seen. */
        fun rgb(value: Int): Int = value or ALPHA

        private const val ALPHA: Int = 0xFF000000.toInt()

        fun blend(base: Int, over: Int, amount: Float): Int {
            val t = amount.coerceIn(0f, 1f)
            fun channel(shift: Int): Int {
                val a = (base shr shift) and 0xFF
                val b = (over shr shift) and 0xFF
                return (a + (b - a) * t).toInt().coerceIn(0, 255)
            }
            return Color.argb(channel(24), channel(16), channel(8), channel(0))
        }

        fun perceivedBrightness(color: Int): Float =
            (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f
    }
}

/**
 * The palette in force, for every screen.
 *
 * It is Compose state, so flipping the moon and sun repaints the library, the reader
 * chrome and the sheets without any wiring of their own. The engine, which renders on
 * a worker thread, is handed a [Palette] directly rather than reading this object.
 */
object ReaderTheme {

    /** A palette from RGB literals, with the alpha byte the tables leave implicit. */
    private fun palette(paper: Int, paperEdge: Int, ink: Int, inkSoft: Int, accent: Int): Palette =
        Palette(
            paper = Palette.rgb(paper),
            paperEdge = Palette.rgb(paperEdge),
            ink = Palette.rgb(ink),
            inkSoft = Palette.rgb(inkSoft),
            accent = Palette.rgb(accent),
        )

    private val light: Map<ThemeId, Palette> = mapOf(
        // White paper, black ink.
        ThemeId.ORIGINAL to palette(0xFFFFFF, 0xF0F0F2, 0x111111, 0x6E6E73, 0xB4632C),
        // Grey on a near-black page: the dim variant, both in the light and dark sets.
        ThemeId.QUIET to palette(0x1B1B1D, 0x2A2A2E, 0xA9A9B0, 0x77777E, 0xC08A5A),
        ThemeId.PAPER to palette(0xFFFFFF, 0xF2EFE9, 0x171717, 0x6B6459, 0x9E5932),
        ThemeId.BOLD to palette(0xFFFFFF, 0xECECEC, 0x000000, 0x5A5A5A, 0x9E5932),
        // Cream paper, brown ink: the iOS `paper` palette.
        ThemeId.CALM to palette(0xF7F2E7, 0xECE6D9, 0x29241F, 0x736A60, 0x9E5932),
        ThemeId.FOCUS to palette(0xFFFFFF, 0xF0F0F2, 0x111111, 0x6E6E73, 0xB4632C),
    )

    private val dark: Map<ThemeId, Palette> = mapOf(
        ThemeId.ORIGINAL to palette(0x121214, 0x242428, 0xE9E9EC, 0x8A8A90, 0xC98A52),
        ThemeId.QUIET to palette(0x0E0E10, 0x1E1E22, 0xB4B4BB, 0x7C7C83, 0xC08A5A),
        ThemeId.PAPER to palette(0x101010, 0x202020, 0xF0F0F0, 0x9A9A9A, 0xC08A5A),
        // The iOS `bold` palette: black paper, bright ink.
        ThemeId.BOLD to palette(0x101010, 0x1F1F22, 0xF2F2F2, 0x9E9EA4, 0xD08A4A),
        // The iOS `calm` palette: brown paper, cream ink.
        ThemeId.CALM to palette(0x6B5844, 0x7C6852, 0xF5EDDD, 0xD6C7AF, 0xF0C98A),
        // The iOS `focus` palette: dark brown paper, yellow ink.
        ThemeId.FOCUS to palette(0x2A2114, 0x3A2D1C, 0xD9B471, 0xA98F5E, 0xE8C073),
    )

    /** A theme's colours for one mode, whether or not it is the one in force. */
    fun paletteOf(id: ThemeId, darkMode: Boolean): Palette =
        (if (darkMode) dark else light).getValue(id)

    private var active by mutableStateOf(paletteOf(ThemeId.default, darkMode = false))

    /** Called by [ReaderSettings] whenever the theme or the mode changes. */
    fun activate(id: ThemeId, darkMode: Boolean) {
        active = paletteOf(id, darkMode)
    }

    val current: Palette get() = active

    /** The raw ARGB forms, for the canvas code that cannot take a Compose colour. */
    val paper: Int get() = active.paper
    val paperEdge: Int get() = active.paperEdge
    val ink: Int get() = active.ink
    val inkSoft: Int get() = active.inkSoft
    val accent: Int get() = active.accent

    val Paper: ComposeColor get() = ComposeColor(active.paper)
    val PaperEdge: ComposeColor get() = ComposeColor(active.paperEdge)
    val Ink: ComposeColor get() = ComposeColor(active.ink)
    val InkSoft: ComposeColor get() = ComposeColor(active.inkSoft)
    val Accent: ComposeColor get() = ComposeColor(active.accent)
    val Raised: ComposeColor get() = ComposeColor(active.raised)

    /** True when the paper in force is a dark one, so chrome can pick its own weight. */
    val isDark: Boolean get() = active.dark

    /** Palette used for the generated covers, mirroring the iOS build. */
    val coverPalettes: List<Pair<ComposeColor, ComposeColor>> = listOf(
        ComposeColor(0xFF38578C) to ComposeColor(0xFF17294D),
        ComposeColor(0xFF944D38) to ComposeColor(0xFF4A221C),
        ComposeColor(0xFF297061) to ComposeColor(0xFF0F3B36),
        ComposeColor(0xFF8C6B2B) to ComposeColor(0xFF453112),
        ComposeColor(0xFF66407A) to ComposeColor(0xFF301C45),
    )

    fun coverPalette(title: String): Pair<ComposeColor, ComposeColor> {
        val sum = title.codePoints().sum()
        return coverPalettes[(sum % coverPalettes.size + coverPalettes.size) % coverPalettes.size]
    }
}

/** True when the paper in force is dark, for code outside a composition. */
val readerPaperIsDark: Boolean get() = ReaderTheme.current.dark

/**
 * Every measurement the paginator, the page renderer and the floating chrome must
 * agree on, in pixels. Mirrors the iOS `ReaderMetrics` constant for constant.
 *
 * `statusBarTop` is supplied by the caller, because the pages deliberately stretch
 * past the system bars. `hInsetPx` is the reader's margin — a setting now, so it comes
 * from the reader's own configuration rather than being fixed here.
 *
 * There is no band of its own for the running head: it rides the toolbar's line, in
 * the gap the two round buttons leave free, so the text can start one gap below the
 * buttons (`textTop`).
 */
class PageMetrics(
    val width: Int,
    val height: Int,
    val density: Float,
    statusBarTop: Int,
    hInsetPx: Float,
) {
    val hInset: Float = hInsetPx
    val headerHeight: Float = 14f * density
    val footerHeight: Float = 14f * density
    val gap: Float = 14f * density

    /** The gutter between two columns, when the page is set in two. */
    val columnGap: Float = 20f * density

    /** Every button in the reader — toolbar, audio, settings, player — is this size. */
    val buttonSize: Float = 44f * density

    /** Gap between the safe-area top and the floating toolbar. */
    val topButtonGap: Float = 6f * density

    /** Distance from the screen's bottom edge to the footer's bottom. */
    val bottomInset: Float = 18f * density

    /** Room kept clear for the floating controls, tall enough for the unfurled player. */
    val controlBand: Float = 48f * density

    /** Breathing room between the footer and the floating controls. */
    val footerToControls: Float = 10f * density

    /** Side margin shared by the toolbar and the bottom controls. */
    val hMargin: Float = 16f * density

    /** Distance from the screen's bottom edge to the floating controls' bottom. */
    val controlsBottomInset: Float = bottomInset + footerHeight + footerToControls

    /** The band the floating bottom controls occupy, in page coordinates. */
    val controlsTop: Float = height - controlsBottomInset - controlBand

    /**
     * The running head sits between the two toolbar buttons, clear of both, and is
     * centred on the page: `hMargin + buttonSize + 10` on each side.
     */
    val headerLeft: Float = hMargin + buttonSize + 10f * density
    val headerWidth: Float = maxOf(40f, width - headerLeft * 2f)
    val headerCentreX: Float = headerLeft + headerWidth / 2f

    /** Centred on the toolbar's own line, not in a band below it. */
    val headerTop: Float = statusBarTop + topButtonGap + (buttonSize - headerHeight) / 2f

    /** Where text may go: one gap below the toolbar, one gap above the controls. */
    val textTop: Float = statusBarTop + topButtonGap + buttonSize + gap
    val textBottom: Float = controlsTop - gap

    /** The page number's baseline box, below the controls and above the home indicator. */
    val footerTop: Float = height - bottomInset - footerHeight

    /** A hair of slack, so a rounding difference never pushes a line off the page. */
    val textWidth: Float = maxOf(40f, width - hInset * 2f - 1f)
    val textHeight: Float = maxOf(40f, textBottom - textTop - 1f)

    /** The width of one column, once [columns] of them share the text box. */
    fun columnWidth(columns: Int): Float {
        val count = columns.coerceAtLeast(1)
        return maxOf(40f, (textWidth - columnGap * (count - 1)) / count)
    }

    /** Left edge of column [index] on the paper. */
    fun columnLeft(index: Int, columns: Int): Float =
        hInset + index * (columnWidth(columns) + columnGap)
}
