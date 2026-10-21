package com.example.epubreader.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * One theme's typography, as the reader has it *now*. Every reader theme keeps its own
 * copy, so switching themes does not throw away the way you set the other one up — and
 * a theme whose copy no longer matches its own defaults is the one that wears the `*`.
 *
 * [custom] is the "Tuỳ chỉnh" switch in the appearance screen: while it is off the four
 * spacing fields are held at the theme's own defaults (and the sliders are inert).
 */
data class TypeConfig(
    val sizeSp: Float,
    val lineSpacing: Float,
    /** Tracking, in em. 0 is the font's own spacing. */
    val letterSpacing: Float,
    /** Extra space between words, in em. */
    val wordSpacing: Float,
    /** The page's side margin, in dp. */
    val marginDp: Float,
    /** [COLUMNS_AUTO], 1 or 2. */
    val columns: Int,
    val justified: Boolean,
    val fontKey: String,
    val bold: Boolean,
    val custom: Boolean,
) {
    /**
     * The fields the `*` on a theme card is about, in a form that compares cleanly.
     *
     * The type size is deliberately not one of them: it has its own dial on the panel
     * (the A/A pill), and stepping it is not a change to the theme's *look* — only what
     * the "Tuỳ chỉnh" screen changes marks a card.
     */
    fun signature(): List<Comparable<*>> = listOf(
        lineSpacing,
        letterSpacing,
        wordSpacing,
        marginDp,
        columns,
        justified,
        fontKey,
        bold,
    )

    fun differsFrom(id: ThemeId): Boolean = signature() != defaults(id).signature()

    companion object {

        /** "Tự động": one column, which is what a page this narrow wants. */
        const val COLUMNS_AUTO: Int = 0

        const val FONT_SERIF: String = "serif"
        const val FONT_SANS: String = "sans-serif"

        /**
         * Each theme's own look — where its `*` baseline comes from. The colours are in
         * [ReaderTheme]; these are the typographic half of a theme, and the iOS app's
         * one serif/sans switch and 19pt/1.14 body are the anchor they were built from.
         */
        fun defaults(id: ThemeId): TypeConfig = when (id) {
            ThemeId.ORIGINAL -> base()
            ThemeId.QUIET -> base(size = 17f, leading = 1.4f, margin = 36f, font = FONT_SANS)
            ThemeId.PAPER -> base(leading = 1.2f, margin = 32f)
            ThemeId.BOLD -> base(leading = 1.3f, margin = 26f, font = FONT_SANS, bold = true)
            ThemeId.CALM -> base(size = 21f, leading = 1.5f, margin = 40f)
            ThemeId.FOCUS -> base(leading = 1.35f, margin = 22f, font = FONT_SANS)
        }

        private fun base(
            size: Float = DEFAULT_BODY_SIZE,
            leading: Float = DEFAULT_LINE_SPACING,
            margin: Float = DEFAULT_MARGIN_DP,
            font: String = FONT_SERIF,
            bold: Boolean = false,
        ) = TypeConfig(
            sizeSp = size,
            lineSpacing = leading,
            letterSpacing = 0f,
            wordSpacing = 0f,
            marginDp = margin,
            columns = COLUMNS_AUTO,
            justified = true,
            fontKey = font,
            bold = bold,
            custom = false,
        )

        /** Exactly the sizes the A/A pill steps through, tightest control first. */
        val sizeOptions: List<Float> = listOf(16f, 17f, 19f, 21f, 23f)

        /** Matches `AttributedBuilder.defaultBodySize` / `defaultLineSpacing` on iOS. */
        const val DEFAULT_BODY_SIZE: Float = 19f
        const val DEFAULT_LINE_SPACING: Float = 1.14f
        const val DEFAULT_MARGIN_DP: Float = 30f
    }
}

/**
 * Reading appearance for the whole app: which theme, light or dark, and each theme's
 * own [TypeConfig].
 *
 * A `ReaderSettings` exists once per process (the app creates it), because the mode and
 * the theme are app-wide: the moon in the reader must repaint the library too. Every
 * mutation re-publishes the palette to [ReaderTheme] and persists itself, so a cold
 * launch opens the way the reader left it.
 */
class ReaderSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var themeId: ThemeId by mutableStateOf(ThemeId.of(prefs.getString(KEY_THEME, null)))
        private set

    var dark: Boolean by mutableStateOf(prefs.getBoolean(KEY_DARK, false))
        private set

    private var configs: Map<String, TypeConfig> by mutableStateOf(loadConfigs())
        private set

    init {
        // The appearance model replaced the old global size/leading pair; leaving those
        // keys behind would only confuse the next reading of the file.
        prefs.edit().remove(KEY_LEGACY_BODY_SIZE).remove(KEY_LEGACY_LINE_SPACING).apply()
        ReaderTheme.activate(themeId, dark)
    }

    val palette: Palette get() = ReaderTheme.paletteOf(themeId, dark)

    /** A theme's colours in the mode in force, for the cards and the customizer. */
    fun paletteOf(id: ThemeId): Palette = ReaderTheme.paletteOf(id, dark)

    /** The theme's configuration: what the reader was last told to use. */
    fun configOf(id: ThemeId): TypeConfig = configs[id.key] ?: TypeConfig.defaults(id)

    /** True when [id] has been changed away from its defaults: the `*` on its card. */
    fun isModified(id: ThemeId): Boolean = configOf(id).differsFrom(id)

    /** True when any theme at all has been changed, so the panel can say so. */
    val anyModified: Boolean get() = ThemeId.entries.any { isModified(it) }

    fun update(id: ThemeId, transform: (TypeConfig) -> TypeConfig) {
        val defaults = TypeConfig.defaults(id)
        val next = transform(configOf(id)).let { candidate ->
            // With "Tuỳ chỉnh" off the four spacing controls follow the theme itself.
            if (candidate.custom) {
                candidate
            } else {
                candidate.copy(
                    lineSpacing = defaults.lineSpacing,
                    letterSpacing = defaults.letterSpacing,
                    wordSpacing = defaults.wordSpacing,
                    marginDp = defaults.marginDp,
                )
            }
        }
        configs = configs + (id.key to next)
        persist(id, next)
        ReaderTheme.activate(themeId, dark)
    }

    fun resetToDefaults(id: ThemeId) {
        configs = configs - id.key
        clear(id)
    }

    fun setTheme(id: ThemeId) {
        themeId = id
        prefs.edit().putString(KEY_THEME, id.key).apply()
        ReaderTheme.activate(themeId, dark)
    }

    /** The moon and the sun: the same theme, its other face. */
    fun toggleDark() {
        dark = !dark
        prefs.edit().putBoolean(KEY_DARK, dark).apply()
        ReaderTheme.activate(themeId, dark)
    }

    // ------------------------------------------------------------- size control

    val canGrowText: Boolean get() = configOf(themeId).sizeSp < TypeConfig.sizeOptions.last()
    val canShrinkText: Boolean get() = configOf(themeId).sizeSp > TypeConfig.sizeOptions.first()
    val sizeLabel: String get() = configOf(themeId).sizeSp.toInt().toString()

    fun growText() = stepSize(1)
    fun shrinkText() = stepSize(-1)

    private fun stepSize(delta: Int) {
        update(themeId) { config ->
            val index = TypeConfig.sizeOptions.indexOf(config.sizeSp).takeIf { it >= 0 } ?: 0
            val stepped = TypeConfig.sizeOptions[(index + delta).coerceIn(0, TypeConfig.sizeOptions.size - 1)]
            config.copy(sizeSp = stepped)
        }
    }

    // ---------------------------------------------------------------- typesetting

    /** Everything the paginator and the renderer need, in pixels, from one source. */
    fun typesetSpec(density: Float): TypesetSpec {
        val config = configOf(themeId)
        return TypesetSpec(
            sizePx = config.sizeSp * density,
            lineSpacing = config.lineSpacing,
            letterSpacingEm = config.letterSpacing,
            wordSpacingEm = config.wordSpacing,
            marginPx = config.marginDp * density,
            columns = if (config.columns == 2) 2 else 1,
            justified = config.justified,
            fontKey = config.fontKey,
            bold = config.bold,
        )
    }

    /** A one-line description of the typesetting, for the log and for evidence. */
    fun describe(density: Float): String {
        val config = configOf(themeId)
        return buildString {
            append("${themeId.key}/${if (dark) "dark" else "light"}")
            append(" ${config.sizeSp}sp/${trimNumber(config.lineSpacing)}")
            append(" font=${config.fontKey}${if (config.bold) "-bold" else ""}")
            append(" letter=${trimNumber(config.letterSpacing)} word=${trimNumber(config.wordSpacing)}")
            append(" margin=${config.marginDp.toInt()}dp")
            append(" cols=${if (config.columns == 2) 2 else 1}")
            append(if (config.justified) " justified" else " ragged")
        }
    }

    // ------------------------------------------------------------------ harness

    /**
     * Applies a `key:value` list to the active theme, for the screenshot harness.
     * Unknown keys are ignored, so the launching intent stays a safe place to poke.
     */
    fun applyDemoConfig(spec: String) {
        for (pair in spec.split(',')) {
            val (rawKey, rawValue) = pair.split(':', limit = 2).let {
                if (it.size < 2) return@let "" to "" else it[0].trim() to it[1].trim()
            }
            if (rawKey.isEmpty()) continue
            val number = rawValue.toFloatOrNull()
            val flag = rawValue == "1" || rawValue == "true"
            update(themeId) { config ->
                when (rawKey) {
                    "size" -> number?.let { config.copy(sizeSp = it) } ?: config
                    "line" -> number?.let { config.copy(lineSpacing = it, custom = true) } ?: config
                    "letter" -> number?.let { config.copy(letterSpacing = it, custom = true) } ?: config
                    "word" -> number?.let { config.copy(wordSpacing = it, custom = true) } ?: config
                    "margin" -> number?.let { config.copy(marginDp = it, custom = true) } ?: config
                    "custom" -> config.copy(custom = flag)
                    "columns" -> number?.let { config.copy(columns = it.toInt()) } ?: config
                    "justified" -> config.copy(justified = flag)
                    "bold" -> config.copy(bold = flag)
                    "font" -> config.copy(fontKey = rawValue)
                    else -> config
                }
            }
        }
    }

    // ---------------------------------------------------------------- persistence

    private fun loadConfigs(): Map<String, TypeConfig> {
        val out = HashMap<String, TypeConfig>(ThemeId.entries.size)
        for (id in ThemeId.entries) {
            val defaults = TypeConfig.defaults(id)
            if (!prefs.contains("$KEY_PREFIX${id.key}.size")) continue
            out[id.key] = defaults.copy(
                sizeSp = prefs.getFloat("$KEY_PREFIX${id.key}.size", defaults.sizeSp),
                lineSpacing = prefs.getFloat("$KEY_PREFIX${id.key}.leading", defaults.lineSpacing),
                letterSpacing = prefs.getFloat("$KEY_PREFIX${id.key}.letter", defaults.letterSpacing),
                wordSpacing = prefs.getFloat("$KEY_PREFIX${id.key}.word", defaults.wordSpacing),
                marginDp = prefs.getFloat("$KEY_PREFIX${id.key}.margin", defaults.marginDp),
                columns = prefs.getInt("$KEY_PREFIX${id.key}.columns", defaults.columns),
                justified = prefs.getBoolean("$KEY_PREFIX${id.key}.justified", defaults.justified),
                fontKey = prefs.getString("$KEY_PREFIX${id.key}.font", defaults.fontKey) ?: defaults.fontKey,
                bold = prefs.getBoolean("$KEY_PREFIX${id.key}.bold", defaults.bold),
                custom = prefs.getBoolean("$KEY_PREFIX${id.key}.custom", defaults.custom),
            )
        }
        return out
    }

    private fun persist(id: ThemeId, config: TypeConfig) {
        prefs.edit()
            .putFloat("$KEY_PREFIX${id.key}.size", config.sizeSp)
            .putFloat("$KEY_PREFIX${id.key}.leading", config.lineSpacing)
            .putFloat("$KEY_PREFIX${id.key}.letter", config.letterSpacing)
            .putFloat("$KEY_PREFIX${id.key}.word", config.wordSpacing)
            .putFloat("$KEY_PREFIX${id.key}.margin", config.marginDp)
            .putInt("$KEY_PREFIX${id.key}.columns", config.columns)
            .putBoolean("$KEY_PREFIX${id.key}.justified", config.justified)
            .putString("$KEY_PREFIX${id.key}.font", config.fontKey)
            .putBoolean("$KEY_PREFIX${id.key}.bold", config.bold)
            .putBoolean("$KEY_PREFIX${id.key}.custom", config.custom)
            .apply()
    }

    private fun clear(id: ThemeId) {
        val edit = prefs.edit()
        for (field in FIELDS) edit.remove("$KEY_PREFIX${id.key}.$field")
        edit.apply()
    }

    companion object {

        private const val PREFS = "readerSettings"
        private const val KEY_THEME = "readerThemeID"
        private const val KEY_DARK = "readerDarkMode"
        private const val KEY_PREFIX = "cfg."

        /** The old global pair, retired when the per-theme model arrived. */
        private const val KEY_LEGACY_BODY_SIZE = "readerBodySize"
        private const val KEY_LEGACY_LINE_SPACING = "readerLineSpacing"

        private val FIELDS = listOf(
            "size", "leading", "letter", "word", "margin", "columns", "justified", "font", "bold", "custom",
        )

        /** `%g` of the iOS build: `1.14` not `1.140000`, `1` not `1.0`. */
        fun trimNumber(value: Float): String =
            if (value == value.toInt().toFloat()) {
                value.toInt().toString()
            } else {
                value.toString().trimEnd('0').trimEnd('.')
            }

        /** Two decimals, always, for the spacing sliders: `0.10`, not `0.1`. */
        fun twoDecimals(value: Float): String {
            val rounded = Math.round(value * 100f) / 100f
            return String.format(java.util.Locale.US, "%.2f", rounded)
        }
    }
}
