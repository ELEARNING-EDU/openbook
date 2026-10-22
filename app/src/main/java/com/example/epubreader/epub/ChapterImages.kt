package com.example.epubreader.epub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.LinkedHashMap
import kotlin.math.min

/** One picture a chapter's XHTML asks for, kept exactly as the markup wrote it. */
data class ChapterImageReference(
    /** The `src` attribute: still relative to the chapter, still percent-encoded. */
    val src: String,
    /** The `alt` text, shown when the picture itself cannot be. */
    val alt: String,
)

/**
 * Finds the `<img>` tags of a chapter and swaps them for tokens the HTML importer
 * carries through as plain text — the Android counterpart of the iOS `ChapterImages`.
 *
 * `Html.fromHtml` is handed a string, not a file system, so an `<img>` would leave
 * neither a picture nor a placeholder behind. Lifting the tags out first gives the
 * reader a position to put the picture back into once the string has been imported
 * and styled (see `HtmlText`).
 *
 * The archive is consulted while the ZIP's entries are still in hand
 * ([EpubArchive.read]); the chapter then carries the bytes under the very `src` the
 * markup wrote, so an import never has to open the book again. The `picture` decoder
 * is the one piece that touches pixels, and it only ever runs during an import, on the
 * main thread.
 */
object ChapterImages {

    /**
     * The marker left in the HTML. Plain ASCII on purpose — private-use characters are
     * not safe through the importer — and padded so one token cannot prefix another.
     */
    fun token(index: Int): String = "@@ngimg$index@@"

    /** The chapter with every `<img>` replaced by its token, plus the same-order refs. */
    data class Rewritten(val html: String, val references: List<ChapterImageReference>)

    private val tagPattern = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val srcPattern =
        Regex("\\bsrc\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)
    private val altPattern =
        Regex("\\balt\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)

    /** Every `<img>` in document order, without touching the markup. */
    fun references(html: String): List<ChapterImageReference> =
        tagPattern.findAll(html).map { reference(it.value) }.toList()

    /** The chapter with each tag replaced by its token, in the order the refs come. */
    fun rewrite(html: String): Rewritten {
        val found = tagPattern.findAll(html).toList()
        if (found.isEmpty()) return Rewritten(html, emptyList())
        val out = StringBuilder(html.length)
        var last = 0
        for ((index, match) in found.withIndex()) {
            out.append(html, last, match.range.first)
            out.append(token(index))
            last = match.range.last + 1
        }
        out.append(html, last, html.length)
        return Rewritten(out.toString(), found.map { reference(it.value) })
    }

    // --------------------------------------------------------------- the picture

    /**
     * Pictures already cut to a column, so a re-typeset does not decode them all over
     * again. Keyed by the bytes and the size they were cut to.
     */
    private const val CACHE_LIMIT = 12

    private val cache = object : LinkedHashMap<String, Bitmap>(CACHE_LIMIT + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > CACHE_LIMIT
    }

    /**
     * The picture cut down to the column, or null when it cannot be read — an SVG, a
     * corrupt file, a `src` that was not in the archive. The caller then shows the alt
     * text, which is better than a hole.
     *
     * Never enlarged: a small picture keeps its own pixels. The height is capped at
     * [maxHeightPx] so a tall plate cannot swallow the page.
     */
    fun picture(bytes: ByteArray?, columnWidthPx: Float, maxHeightPx: Float): Bitmap? {
        if (bytes == null || bytes.isEmpty() || columnWidthPx < 40f) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        val limit = if (maxHeightPx > 1f) maxHeightPx else columnWidthPx * 1.6f
        var scale = min(1f, columnWidthPx / width)
        if (height * scale > limit) scale = limit / height
        val targetWidth = maxOf(1, (width * scale).toInt())
        val targetHeight = maxOf(1, (height * scale).toInt())

        val key = "${bytes.contentHashCode()}-${targetWidth}x$targetHeight"
        synchronized(cache) { cache[key]?.let { if (!it.isRecycled) return it } }

        var sample = 1
        while (width / (sample * 2) >= targetWidth && height / (sample * 2) >= targetHeight) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null
        val picture = if (decoded.width == targetWidth && decoded.height == targetHeight) {
            decoded
        } else {
            Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
        }
        synchronized(cache) { cache[key] = picture }
        return picture
    }

    // ------------------------------------------------------------- reading a tag

    private fun reference(tag: String): ChapterImageReference =
        ChapterImageReference(src = value(srcPattern, tag), alt = value(altPattern, tag))

    /**
     * The first capture group of [pattern] inside [tag], entities decoded enough to name
     * a file (`&amp;` is the one EPUB actually uses in `src`).
     */
    private fun value(pattern: Regex, tag: String): String {
        val match = pattern.find(tag) ?: return ""
        for (group in 1 until match.groupValues.size) {
            val value = match.groupValues[group]
            if (value.isNotEmpty()) return value.replace("&amp;", "&")
        }
        return ""
    }
}
