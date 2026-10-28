package com.elearning.openbook.reader

import android.graphics.Typeface
import java.io.File

/** One row of the font list: what the reader stores, and what the row is called. */
data class FontInfo(
    val key: String,
    val label: String,
)

/**
 * The real fonts on this device.
 *
 * The families Android always guarantees come first; after them the device's own font
 * files — the very `.ttf`s the system renders with — are listed, each row drawn in the
 * face it offers. Picking one changes the page, because the paginator builds its paints
 * from the stored key.
 */
object FontCatalog {

    /** A stored key that names a file rather than a family. */
    private const val FILE_PREFIX = "file:"

    val generic: List<FontInfo> = listOf(
        FontInfo(TypeConfig.FONT_SERIF, "Serif"),
        FontInfo(TypeConfig.FONT_SANS, "Sans"),
        FontInfo("monospace", "Mono"),
        FontInfo("serif-monospace", "Serif Mono"),
        FontInfo("casual", "Casual"),
        FontInfo("cursive", "Cursive"),
    )

    /** Styles that are variants of a family, not families of their own. */
    private val STYLES = listOf(
        "regular", "bold", "italic", "oblique", "light", "medium", "thin", "black",
        "semibold", "extrabold", "extralight", "condensed", "variable",
    )

    /** Faces there is no point offering: they shape scripts this reader cannot set. */
    private val EXCLUDED = listOf("cjk", "symbol", "emoji", "math", "arabic", "hebrew", "thai", "devanagari")

    /**
     * `Noto Sans Adlam`, `Noto Sans Avestan` and forty more like them: one face per
     * script, which would push every readable family off the bottom of the list. The
     * plain `Noto Sans` and `Noto Serif` stay.
     */
    private val SCRIPT_COVERAGE = Regex("^Noto (Sans|Serif) .+")

    private const val MAX_SYSTEM_FONTS = 36

    private var cached: List<FontInfo>? = null

    /** The guaranteed families, then the device's own, alphabetically. */
    fun available(): List<FontInfo> {
        cached?.let { return it }
        val system = systemFonts().sortedBy { it.label.lowercase() }
        return (generic + system).also { cached = it }
    }

    private fun systemFonts(): List<FontInfo> {
        val files = runCatching { File("/system/fonts").listFiles() }.getOrNull() ?: return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<FontInfo>()
        for (file in files) {
            val name = file.name
            if (!name.endsWith(".ttf", true) && !name.endsWith(".otf", true)) continue
            if (EXCLUDED.any { name.contains(it, true) }) continue
            val label = prettify(name.substringBeforeLast('.'))
            if (SCRIPT_COVERAGE.matches(label)) continue
            if (!seen.add(label)) continue
            out += FontInfo(FILE_PREFIX + file.path, label)
            if (out.size >= MAX_SYSTEM_FONTS) break
        }
        return out
    }

    /** `RobotoCondensed-Regular` becomes `Roboto Condensed`. */
    private fun prettify(fileName: String): String {
        val head = fileName.split('-').let { parts ->
            if (parts.size > 1 && parts.last().lowercase() in STYLES) parts.dropLast(1) else parts
        }.joinToString("-")
        return head.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ').trim()
    }

    /** The label a font row shows: the reader's own name for the generic faces. */
    fun labelOf(key: String): String = generic.firstOrNull { it.key == key }?.label ?: key

    /**
     * The face a stored key names. A file key is loaded from the file itself; anything
     * else goes through the platform's family lookup, which is where the four generic
     * names and the system aliases live.
     */
    fun typeface(key: String, bold: Boolean): Typeface {
        val base = if (key.startsWith(FILE_PREFIX)) {
            runCatching { Typeface.createFromFile(key.removePrefix(FILE_PREFIX)) }
                .getOrElse { Typeface.create(TypeConfig.FONT_SERIF, Typeface.NORMAL) }
        } else {
            Typeface.create(key, Typeface.NORMAL)
        }
        return if (bold) Typeface.create(base, Typeface.BOLD) else base
    }
}
