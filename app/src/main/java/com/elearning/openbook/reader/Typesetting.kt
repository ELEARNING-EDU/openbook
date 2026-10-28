package com.elearning.openbook.reader

import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.LineHeightSpan
import android.text.style.MetricAffectingSpan
import android.text.style.QuoteSpan
import com.elearning.openbook.epub.HtmlText

/**
 * The reader's typography, in one place.
 *
 * The page and the appearance screen's preview have to agree: a preview that is merely
 * *similar* to the page is how a "Căn đều hai bên" switch ends up doing nothing visible.
 * So both build their text with the functions here — the same paint, the same paragraph
 * gap, the same word-gap spans, the same `StaticLayout` configuration. The only thing
 * the preview does differently is the width it is given and the column count it asks for.
 */
object Typesetting {

    /** The paragraph gap, as a fraction of the body size. */
    const val PARAGRAPH_GAP_EM = 0.55f

    /**
     * The body paint, as the paginator and the preview both want it.
     *
     * The face comes from [FontCatalog], so a `file:` key really loads the file; the
     * colour is the theme's ink; tracking is the reader's own setting.
     */
    fun bodyPaint(spec: TypesetSpec, ink: Int): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = FontCatalog.typeface(spec.fontKey, spec.bold)
            textSize = spec.sizePx
            color = ink
            letterSpacing = spec.letterSpacingEm
            linkColor = ink
        }

    /**
     * Prepares one chapter's text for the layout: paragraph gaps in, theme colours in,
     * word gaps in. `HtmlText` has already turned the HTML into a `SpannableStringBuilder`,
     * so everything here is a span edit.
     */
    fun prepare(
        text: SpannableStringBuilder,
        spec: TypesetSpec,
        palette: Palette,
    ) {
        applyParagraphSpacing(text, spec.sizePx * PARAGRAPH_GAP_EM)
        recolourQuotes(text, palette)
        recolourForeground(text, palette)
        applyWordSpacing(text, spec.wordSpacingEm)
    }

    /**
     * The layout both the page and the preview are cut from.
     *
     * The alignment is `ALIGN_NORMAL` in both cases — justification is a *justification
     * mode*, not an alignment, so it has to go through `setJustificationMode`, which is
     * what makes the switch visible at all.
     */
    fun layout(
        text: CharSequence,
        paint: TextPaint,
        widthPx: Int,
        spec: TypesetSpec,
    ): StaticLayout =
        StaticLayout.Builder
            .obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, spec.lineSpacing)
            .setIncludePad(false)
            .setJustificationMode(
                if (spec.justified) {
                    android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
                } else {
                    android.text.Layout.JUSTIFICATION_MODE_NONE
                },
            )
            .setBreakStrategy(
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY
                } else {
                    android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY
                },
            )
            .setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NORMAL)
            .build()

    // ------------------------------------------------------------- span helpers

    /**
     * Extra air between words.
     *
     * `Paint.setWordSpacing` is the obvious knob, but it does not move a line break or
     * change a single line count — measured, it does nothing to the layout — so the gap
     * is carried by the spaces themselves: one metric-affecting span per space, which
     * the layout measures, breaks and draws like any other glyph.
     */
    fun applyWordSpacing(text: SpannableStringBuilder, extraEm: Float) {
        if (extraEm <= 0f) return
        for (index in text.indices) {
            if (text[index] != ' ') continue
            text.setSpan(
                WordGapSpan(extraEm),
                index,
                index + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    /** Widens one space, in ems, in both drawing and measurement. */
    private class WordGapSpan(private val extraEm: Float) : MetricAffectingSpan() {
        override fun updateDrawState(tp: TextPaint) {
            tp.letterSpacing += extraEm
        }

        override fun updateMeasureState(tp: TextPaint) {
            tp.letterSpacing += extraEm
        }
    }

    /**
     * `HtmlText` leaves a [HtmlText.BOUNDARY] sentinel after every block-closing tag.
     * The sentinel is removed here and its newline gets a [ParagraphGapSpan], because
     * `FROM_HTML_MODE_COMPACT` emits a single `\n` per block boundary and a plain
     * newline cannot express a visual paragraph gap.
     */
    fun applyParagraphSpacing(text: SpannableStringBuilder, gapPx: Float) {
        val boundaries = ArrayList<Int>()
        var index = text.length - 1
        while (index >= 0) {
            if (text[index] == HtmlText.BOUNDARY) {
                if (index > 0 && text[index - 1] == '\n') boundaries += index - 1
                text.delete(index, index + 1)
            }
            index--
        }
        for (boundary in boundaries) {
            if (boundary < text.length && text[boundary] == '\n') {
                text.setSpan(
                    ParagraphGapSpan(gapPx),
                    boundary,
                    boundary + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }

    /** Adds a little room below the last line of a paragraph. */
    private class ParagraphGapSpan(private val extraPx: Float) : LineHeightSpan {
        override fun chooseHeight(
            text: CharSequence,
            start: Int,
            end: Int,
            spanStart: Int,
            spanEnd: Int,
            metrics: Paint.FontMetricsInt,
        ) {
            val extra = extraPx.toInt()
            metrics.descent += extra
            metrics.bottom += extra
        }
    }

    /**
     * Imported colours are the book's, not the reader's, and a `#555` subtitle is
     * unreadable on dark paper. Every `ForegroundColorSpan` is repainted in the theme's
     * ink; a quote's own span is left to [recolourQuotes].
     */
    fun recolourForeground(text: SpannableStringBuilder, palette: Palette) {
        val spans = text.getSpans(0, text.length, ForegroundColorSpan::class.java)
        for (span in spans) {
            text.setSpan(
                ForegroundColorSpan(palette.ink),
                text.getSpanStart(span),
                text.getSpanEnd(span),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    /**
     * A quote keeps its indent and its size, but not its colour: the imported grey is
     * repainted in the theme's soft ink. `QuoteSpan`'s colour constructor only exists
     * from API 28, so below that the span is left alone rather than dropped.
     */
    fun recolourQuotes(text: SpannableStringBuilder, palette: Palette) {
        val spans = text.getSpans(0, text.length, QuoteSpan::class.java)
        if (spans.isEmpty()) return
        for (span in spans) {
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            text.removeSpan(span)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                text.setSpan(
                    QuoteSpan(palette.inkSoft),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }
}
