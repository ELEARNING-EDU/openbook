package com.example.epubreader.epub

import android.text.Html
import android.text.SpannableStringBuilder

/**
 * XHTML -> `SpannableStringBuilder`, via the platform HTML importer.
 *
 * `Html.fromHtml` touches `Spannable`/`TextPaint` internals and must run on the
 * main thread, which is why this sits apart from `EpubArchive` (pure ZIP + XML,
 * safe on a background thread).
 *
 * Two things the platform importer does not do for us:
 *
 *  - it renders `<head>` content, so the head is stripped before conversion;
 *  - in `FROM_HTML_MODE_COMPACT` a block boundary becomes a single newline, so
 *    paragraphs end up glued together. Every block-closing tag therefore gets a
 *    [BOUNDARY] marker appended; the paginator turns those markers into real
 *    paragraph spacing and removes them before the text is laid out.
 */
object HtmlText {

    /**
     * A private-use sentinel, kept out of the laid-out text. NUL would be dropped
     * (or rejected) by the HTML parser, so a private-use code point is used.
     */
    const val BOUNDARY = '\uE000'

    fun toSpannable(html: String): SpannableStringBuilder {
        val cleaned = preprocess(html)
        val spanned = Html.fromHtml(cleaned, Html.FROM_HTML_MODE_COMPACT, null, null)
        val builder = SpannableStringBuilder(spanned)
        // Trailing blank lines (and their markers) would become an extra, empty page.
        while (builder.isNotEmpty()) {
            val last = builder[builder.length - 1]
            if (last == '\n' || last == ' ' || last == '\u00A0' || last == '\t' || last == BOUNDARY) {
                builder.delete(builder.length - 1, builder.length)
            } else {
                break
            }
        }
        return builder
    }

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
