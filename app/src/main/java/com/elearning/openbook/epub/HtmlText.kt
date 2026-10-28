package com.elearning.openbook.epub

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Html
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.StyleSpan
import com.elearning.openbook.model.RawChapter

/**
 * XHTML -> `SpannableStringBuilder`, via the platform HTML importer.
 *
 * `Html.fromHtml` touches `Spannable`/`TextPaint` internals and must run on the
 * main thread, which is why this sits apart from `EpubArchive` (pure ZIP + XML,
 * safe on a background thread).
 *
 * Three things the platform importer does not do for us:
 *
 *  - it renders `<head>` content, so the head is stripped before conversion;
 *  - in `FROM_HTML_MODE_COMPACT` a block boundary becomes a single newline, so
 *    paragraphs end up glued together. Every block-closing tag therefore gets a
 *    [BOUNDARY] marker appended; the paginator turns those markers into real
 *    paragraph spacing and removes them before the text is laid out;
 *  - it drops `<img>` without a word, because it is handed bytes rather than a file
 *    system. Each tag is lifted out first ([ChapterImages]) and put back below as a
 *    text attachment, cut to the column the page will be set in.
 */
object HtmlText {

    /**
     * A private-use sentinel, kept out of the laid-out text. NUL would be dropped
     * (or rejected) by the HTML parser, so a private-use code point is used.
     */
    const val BOUNDARY = '\uE000'

    /** The one character a picture occupies once its token has been replaced. */
    private const val PICTURE = '\uFFFC'

    /**
     * [chapter] is imported whole: its markup, and the pictures the archive resolved for
     * it. [columnWidthPx] and [maxHeightPx] are the column the page will be set in, so
     * a picture is cut once, to the size it will actually be drawn at — never enlarged,
     * never taller than the page allows. An unknown geometry (0) leaves the pictures for
     * the next typeset, which always follows once the window has been measured.
     */
    fun toSpannable(
        chapter: RawChapter,
        columnWidthPx: Float,
        maxHeightPx: Float,
    ): SpannableStringBuilder {
        val rewritten = ChapterImages.rewrite(chapter.html)
        val cleaned = preprocess(rewritten.html)
        val spanned = Html.fromHtml(cleaned, Html.FROM_HTML_MODE_COMPACT, null, null)
        val builder = SpannableStringBuilder(spanned)
        // Trailing blank lines (and their markers) would become an extra, empty page.
        trimTrailing(builder)
        placePictures(builder, rewritten.references, chapter.images, columnWidthPx, maxHeightPx)
        return builder
    }

    private fun trimTrailing(builder: SpannableStringBuilder) {
        while (builder.isNotEmpty()) {
            val last = builder[builder.length - 1]
            if (last == '\n' || last == ' ' || last == '\u00A0' || last == '\t' || last == BOUNDARY) {
                builder.delete(builder.length - 1, builder.length)
            } else {
                break
            }
        }
    }

    // --------------------------------------------------------------- the pictures

    /**
     * Puts every token back as either the picture or a line of alt text.
     *
     * Replacing in place keeps the text either side of the picture exactly where the
     * importer left it; only the token itself changes length. A picture stands alone:
     * its paragraph is centred, the way a plate is in a printed book.
     */
    private fun placePictures(
        text: SpannableStringBuilder,
        references: List<ChapterImageReference>,
        images: Map<String, ByteArray>,
        columnWidthPx: Float,
        maxHeightPx: Float,
    ) {
        if (references.isEmpty()) return
        for ((index, reference) in references.withIndex()) {
            val token = ChapterImages.token(index)
            val at = text.toString().indexOf(token)
            if (at < 0) continue
            val end = at + token.length

            // Width is not known before the page geometry is; an empty column here only
            // means "not yet", because the reader typesets again once it is.
            val picture = if (columnWidthPx > 40f) {
                ChapterImages.picture(images[reference.src], columnWidthPx, maxHeightPx)
            } else {
                null
            }

            if (picture != null) {
                text.replace(at, end, PICTURE.toString())
                text.setSpan(
                    PictureSpan(picture),
                    at,
                    at + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            } else {
                val label = if (reference.alt.isBlank()) "[Ảnh]" else "[Ảnh: ${reference.alt}]"
                text.replace(at, end, label)
                text.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    at,
                    at + label.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                text.setSpan(
                    RelativeSizeSpan(0.85f),
                    at,
                    at + label.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            centreParagraph(text, at)
        }
    }

    /** Centres the whole paragraph the picture (or its alt text) sits in. */
    private fun centreParagraph(text: SpannableStringBuilder, at: Int) {
        if (at >= text.length) return
        var start = at
        while (start > 0 && text[start - 1] != '\n') start--
        var end = at
        while (end < text.length && text[end] != '\n') end++
        if (start >= end) return
        text.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }

    /**
     * One picture as a text run: the layout measures it as its own height and width, so
     * the paginator believes what the renderer then draws. Its bottom edge sits on the
     * baseline, as the iOS attachment's does; the paragraph's own gap carries the air
     * above and below.
     */
    private class PictureSpan(private val picture: Bitmap) : ReplacementSpan() {
        override fun getSize(
            paint: Paint,
            text: CharSequence?,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?,
        ): Int {
            if (fm != null) {
                fm.ascent = -picture.height
                fm.descent = 0
                fm.top = fm.ascent
                fm.bottom = 0
            }
            return picture.width
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence?,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint,
        ) {
            canvas.drawBitmap(picture, x, y - picture.height.toFloat(), null)
        }
    }

    // ------------------------------------------------------------------ preprocess

    private fun preprocess(html: String): String {
        var text = html
        text = text.replace(Regex("(?s)<\\?xml.*?\\?>"), " ")
        text = text.replace(Regex("(?is)<!DOCTYPE[^\\[>]*(\\[[^\\]]*])?[^>]*>"), " ")
        text = text.replace(Regex("(?is)<head\\b[^>]*>.*?</head>"), " ")
        text = text.replace(Regex("(?is)<script\\b[^>]*>.*?</script>"), " ")
        text = text.replace(Regex("(?is)<style\\b[^>]*>.*?</style>"), " ")
        val body = Regex("(?is)<body\\b[^>]*>(.*?)</body>").find(text)
        if (body != null) text = body.groupValues[1]

        // Mark where a paragraph really ends, so a `<br/>` inside a paragraph is
        // not mistaken for one. `<hr/>` is a separator, so it counts as one.
        text = text.replace(BLOCK_END) { match -> match.value + BOUNDARY }
        text = text.replace(VOID_BLOCK) { match -> match.value + BOUNDARY }
        return text
    }

    private val BLOCK_END = Regex(
        "(?i)</(p|div|h[1-6]|blockquote|li|ul|ol|dl|dd|dt|section|article|aside|figure|figcaption|table|tr|td|th)\\s*>",
    )

    private val VOID_BLOCK = Regex("(?is)<hr\\b[^>]*?/?>")
}
