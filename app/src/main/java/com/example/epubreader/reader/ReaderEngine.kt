package com.example.epubreader.reader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.example.epubreader.model.RawBook
import java.util.Locale
import kotlin.math.ceil

/** A table-of-contents entry resolved to a page of the whole book. */
data class TocItem(
    val title: String,
    val pageIndex: Int,
)

/**
 * Everything the page's typesetting depends on, in pixels. Built from the reader's
 * [TypeConfig] and handed to [ReaderEngine.build] as one object, so the paginator can
 * never be told something the renderer was not.
 */
data class TypesetSpec(
    val sizePx: Float,
    val lineSpacing: Float,
    /** Tracking in em; 0 leaves the font's own spacing alone. */
    val letterSpacingEm: Float,
    /** Extra word spacing in em. Ignored below API 29, where `Paint` has no such knob. */
    val wordSpacingEm: Float,
    val marginPx: Float,
    /** 1 or 2. Two really sets the page in two columns, side by side. */
    val columns: Int,
    val justified: Boolean,
    val fontKey: String,
    val bold: Boolean,
) {
    val columnCount: Int = columns.coerceIn(1, 2)
}

/**
 * Paginates a book once and then renders individual pages to bitmaps on demand.
 *
 * `StaticLayout` is built once per chapter; a page is a list of line ranges inside it —
 * one per column, so a two-column page is two runs of the same layout, side by side.
 * Page bitmaps are expensive (a full screen of ARGB_8888), so only a couple are kept
 * around in a small LRU and re-rendered when needed.
 */
class ReaderEngine private constructor(
    private val book: RawBook,
    private val layouts: List<StaticLayout>,
    private val slots: List<PageSlot>,
    private val metrics: PageMetrics,
    private val spec: TypesetSpec,
    private val palette: Palette,
    private val headerPaint: TextPaint,
    private val footerPaint: TextPaint,
    private val chapterText: List<CharSequence>,
    private val sentenceChapter: IntArray,
    private val sentenceStart: IntArray,
    private val sentenceStop: IntArray,
    private val sentencePageIndex: IntArray,
) : CurlView.PageSource, SentenceSource {

    /** One column's worth of lines of one chapter's layout. */
    data class ColumnRun(val chapterIndex: Int, val startLine: Int, val endLine: Int)

    /** One page: a single column, or two of them side by side. */
    data class PageSlot(val columns: List<ColumnRun>) {
        val chapterIndex: Int get() = columns.first().chapterIndex
    }

    private val cache = object : LinkedHashMap<Int, Bitmap>(CACHE_LIMIT + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Bitmap>): Boolean =
            size > CACHE_LIMIT
    }

    val toc: List<TocItem> = book.toc
        .mapNotNull { entry ->
            slots.indexOfFirst { slot -> slot.columns.any { it.chapterIndex == entry.chapterIndex } }
                .takeIf { it >= 0 }
                ?.let { TocItem(entry.title, it) }
        }
        .ifEmpty {
            book.chapters.mapIndexedNotNull { index, chapter ->
                slots.indexOfFirst { slot -> slot.columns.any { it.chapterIndex == index } }
                    .takeIf { it >= 0 && chapter.title.isNotBlank() }
                    ?.let { TocItem(chapter.title, it) }
            }
        }

    private val headerBaseline: Float = run {
        val fm = headerPaint.fontMetrics
        metrics.headerTop + (metrics.headerHeight - (fm.descent - fm.ascent)) / 2f - fm.ascent
    }

    private val footerBaseline: Float = run {
        val fm = footerPaint.fontMetrics
        metrics.footerTop + (metrics.footerHeight - (fm.descent - fm.ascent)) / 2f - fm.ascent
    }

    override val pageCount: Int
        get() = slots.size

    val width: Int
        get() = metrics.width

    val height: Int
        get() = metrics.height

    val chapterCount: Int
        get() = book.chapters.size

    /**
     * The language the book is read in: `dc:language` when the OPF carries one, a guess
     * from the first chapter otherwise. The voice picker offers voices for this language.
     */
    val bookLanguage: Locale = VoiceCatalog.resolveLanguage(
        declared = book.language,
        sample = chapterText.firstOrNull { it.isNotBlank() }?.take(4000)?.toString().orEmpty(),
        fallback = Locale.getDefault(),
    )

    /** The width of one column, which is the layout's own width in either mode. */
    private val columnWidth: Float = metrics.columnWidth(spec.columnCount)

    /**
     * How the text fell out — the line count the paginator produced and the width it
     * produced it at. The page count alone cannot show a change of tracking or of
     * margin, because those move the line breaks without moving the page boundaries;
     * these two numbers can, and they are in the log for that reason.
     */
    val typesetNote: String
        get() = "lines=${layouts.sumOf { it.lineCount }} colW=${columnWidth.toInt()}px"


    override fun bitmapFor(index: Int): Bitmap? {
        if (index < 0 || index >= slots.size) return null
        synchronized(cache) {
            cache[index]?.let { if (!it.isRecycled) return it }
        }
        val rendered = render(index)
        synchronized(cache) { cache[index] = rendered }
        return rendered
    }

    /** Warms the LRU off the main thread, so the first frames draw without a hitch. */
    fun preload(indices: List<Int>) {
        for (index in indices) {
            if (index in slots.indices) bitmapFor(index)
        }
    }

    // ------------------------------------------------------------- sentences

    override val sentenceCount: Int
        get() = sentenceChapter.size

    override fun sentenceText(index: Int): String {
        if (index < 0 || index >= sentenceCount) return ""
        return Sentences.readable(chapterText[sentenceChapter[index]], sentenceStart[index]..(sentenceStop[index] - 1))
    }

    override fun sentencePage(index: Int): Int =
        if (index in sentencePageIndex.indices) sentencePageIndex[index] else -1

    /** First sentence that *begins* on [page], or the one crossing its top edge. */
    fun firstSentenceOnPage(page: Int): Int {
        for (index in sentencePageIndex.indices) {
            if (sentencePageIndex[index] == page) return index
        }
        val run = slots.getOrNull(page)?.columns?.firstOrNull() ?: return -1
        val offset = layouts[run.chapterIndex].getLineStart(run.startLine)
        return sentenceAtOffset(run.chapterIndex, offset)
    }

    /** The chapter a page belongs to. */
    fun chapterOfPage(page: Int): Int = slots.getOrNull(page)?.chapterIndex ?: 0

    /** The chapter [sentence] is spoken from, for the player's headline. */
    fun chapterOfSentence(sentence: Int): Int = chapterOfPage(sentencePage(sentence))

    /** The chapter's first spoken sentence, or -1 when the chapter has no text. */
    fun firstSentenceOfChapter(chapter: Int): Int =
        sentenceChapter.indexOfFirst { it == chapter }

    /**
     * Characters of the book's text that precede each chapter, with the book's total as
     * the last entry: `chapterCharStart[c]` is where chapter `c` begins once every
     * chapter is read end to end. Every chapter numbers its own characters from zero,
     * which is why a reader-wide cursor has to be summed here rather than read off a
     * page.
     */
    private val chapterCharStart: IntArray = IntArray(chapterText.size + 1).also { starts ->
        for (chapter in chapterText.indices) {
            starts[chapter + 1] = starts[chapter] + chapterText[chapter].length
        }
    }

    /** The whole book in characters — the denominator of the player's progress line. */
    val bookCharCount: Int get() = chapterCharStart.last()

    /**
     * How far into the book the voice has got while it is speaking [sentence]: the
     * character its last word ends at, counted across the whole book rather than inside
     * the page on screen. 0..[bookCharCount], and 0 when there is no sentence at all.
     */
    fun readCursor(sentence: Int): Int {
        if (sentence !in sentenceChapter.indices) return 0
        val chapter = sentenceChapter[sentence]
        val start = chapterCharStart.getOrElse(chapter) { 0 }
        return start + sentenceStop[sentence].coerceIn(0, chapterText[chapter].length)
    }

    /**
     * The sentence a drag on the player's slider lands on: [fraction] of the book's
     * text, mapped back to the sentence that is being spoken at that point.
     *
     * [readCursor] rises with the sentence index (a chapter starts where the previous
     * one ended), so a binary search is enough. -1 when there is nothing to read.
     */
    fun sentenceAtBookFraction(fraction: Float): Int {
        if (sentenceCount == 0) return -1
        val target = fraction.coerceIn(0f, 1f) * bookCharCount
        var low = 0
        var high = sentenceCount - 1
        while (low < high) {
            val mid = (low + high) / 2
            if (readCursor(mid) < target) low = mid + 1 else high = mid
        }
        return low
    }

    fun chapterTitle(chapter: Int): String =
        book.chapters.getOrNull(chapter)?.title?.replace(Regex("\\s+"), " ")?.trim().orEmpty()

    /**
     * The character offset a page starts at, in its chapter's text. Together with
     * [chapterOfPage] this is the anchor a re-typeset is held by: page numbers do
     * not survive a change of type size, but a chapter and an offset do.
     */
    fun offsetOfPage(page: Int): Int {
        val run = slots.getOrNull(page)?.columns?.firstOrNull() ?: return 0
        return layouts[run.chapterIndex].getLineStart(run.startLine)
    }

    /** The page that shows [offset] inside [chapter], for the anchor above. */
    fun pageForChapterOffset(chapter: Int, offset: Int): Int {
        val layout = layouts.getOrNull(chapter) ?: return 0
        val line = layout.getLineForOffset(offset.coerceIn(0, layout.text.length))
        var lastOfChapter = -1
        for (index in slots.indices) {
            val slot = slots[index]
            val run = slot.columns.firstOrNull { it.chapterIndex == chapter } ?: continue
            if (line in run.startLine..run.endLine) return index
            lastOfChapter = index
        }
        return if (lastOfChapter >= 0) lastOfChapter else 0
    }

    // ------------------------------------------------------------- finding things

    /**
     * The character range [page] covers inside its chapter. A mark is stored against
     * this range, so it stays on the same words when the book is re-typeset and the
     * page boundaries move.
     */
    fun pageRange(page: Int): IntRange {
        val slot = slots.getOrNull(page) ?: return 0 until 0
        val first = slot.columns.firstOrNull() ?: return 0 until 0
        val last = slot.columns.last()
        val start = layouts[first.chapterIndex].getLineStart(first.startLine)
        val end = layouts[last.chapterIndex].getLineEnd(last.endLine)
        return start until maxOf(start + 1, end)
    }

    /**
     * A stored mark as a list row, so a bookmark reads like a search result: the same
     * chapter line, the same snippet shape, and the page it currently falls on.
     */
    fun markRow(chapter: Int, offset: Int): Hit? {
        val text = chapterText.getOrNull(chapter) ?: return null
        if (offset !in 0..text.length) return null
        return Hit(
            chapterIndex = chapter,
            offset = offset,
            chapterTitle = chapterTitle(chapter),
            snippet = snippetAround(text, offset, 0),
            pageIndex = pageForChapterOffset(chapter, offset),
        )
    }

    /**
     * One search hit: where it is in the book, what it says, and the page it lives on.
     *
     * Vietnamese is read both with and without its diacritics, so the scan is
     * case- and accent-insensitive ("hai dang" finds "Hải Đăng"); the page is resolved
     * from the character offset, so a hit survives a re-typeset.
     */
    data class Hit(
        val chapterIndex: Int,
        val offset: Int,
        val chapterTitle: String,
        val snippet: String,
        val pageIndex: Int,
    ) {
        /** Stable identity, for a list that must not re-key on every keystroke. */
        val id: String get() = "$chapterIndex-$offset"
    }

    /**
     * Scans the whole book for [query]. Two characters is the minimum: one letter
     * matches almost every page and tells the reader nothing.
     */
    fun search(query: String, limit: Int = 60): List<Hit> {
        val needle = query.trim()
        if (needle.length < 2) return emptyList()
        val folded = fold(needle)
        val found = ArrayList<Hit>()

        for (chapterIndex in chapterText.indices) {
            val text = chapterText[chapterIndex]
            // The text is folded once, and the match's offsets are mapped back through
            // it: `fold` is length-preserving by construction.
            val haystack = fold(text.toString())
            var from = 0
            while (found.size < limit) {
                val at = haystack.indexOf(folded, from)
                if (at < 0) break
                found += Hit(
                    chapterIndex = chapterIndex,
                    offset = at,
                    chapterTitle = chapterTitle(chapterIndex),
                    snippet = snippetAround(text, at, needle.length),
                    pageIndex = pageForChapterOffset(chapterIndex, at),
                )
                from = at + maxOf(1, folded.length)
            }
            if (found.size >= limit) break
        }
        return found
    }

    /** The line a hit reads as in a result list: the match in its own sentence. */
    private fun snippetAround(text: CharSequence, offset: Int, length: Int): String {
        val start = maxOf(0, offset - SNIPPET_PAD)
        val end = minOf(text.length, offset + length + SNIPPET_PAD)
        val body = text.substring(start, end)
            .replace(Regex("\\s+"), " ")
            .trim()
        return when {
            start == 0 && end == text.length -> body
            start == 0 -> "$body…"
            end == text.length -> "…$body"
            else -> "…$body…"
        }
    }

    /**
     * Lower-cases and strips Vietnamese diacritics, one character for one character, so
     * an offset in the folded string is the same offset in the real one.
     *
     * Each character is decomposed on its own and only its base is kept, which is what
     * makes the mapping length-preserving: a letter with a mark becomes `base + marks`
     * and the marks are dropped, leaving one character where there was one. `đ` has no
     * decomposition, so it is mapped by hand.
     */
    private fun fold(value: String): String {
        val out = StringBuilder(value.length)
        for (character in value) {
            val base = java.text.Normalizer
                .normalize(character.toString(), java.text.Normalizer.Form.NFD)
                .firstOrNull { !isMark(it) }
                ?: character
            out.append(
                when (base) {
                    'đ' -> 'd'
                    'Đ' -> 'D'
                    else -> base.lowercaseChar()
                },
            )
        }
        return out.toString()
    }

    private fun isMark(character: Char): Boolean =
        Character.getType(character) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(character) == Character.COMBINING_SPACING_MARK.toInt()

    /** True when a touch landed on the text itself rather than on the paper around it. */
    fun isOnText(x: Float, y: Float): Boolean {
        if (y < metrics.textTop || y > metrics.textBottom) return false
        for (column in 0 until spec.columnCount) {
            val left = metrics.columnLeft(column, spec.columnCount)
            if (x >= left && x <= left + columnWidth) return true
        }
        return false
    }

    /** Sentence under a touch, given the page it landed on and view-space pixels. */
    fun sentenceAt(page: Int, viewX: Float, viewY: Float): Int? {
        val slot = slots.getOrNull(page) ?: return null
        val column = columnIndexAt(viewX, slot)
        val run = slot.columns.getOrNull(column) ?: return null
        val layout = layouts[run.chapterIndex]
        if (layout.text.isEmpty() || layout.lineCount == 0) return null
        val layoutX = viewX - metrics.columnLeft(column, spec.columnCount)
        val layoutY = viewY - metrics.textTop + layout.getLineTop(run.startLine)
        // `Layout.getOffsetForPosition` is a hidden API, so do the two public steps.
        val clampedY = layoutY.coerceIn(0f, (layout.height - 1).toFloat())
        val line = layout.getLineForVertical(clampedY.toInt()).coerceIn(0, layout.lineCount - 1)
        val offset = layout.getOffsetForHorizontal(line, layoutX)
        return sentenceAtOffset(run.chapterIndex, offset).takeIf { it >= 0 }
    }

    /** Chapter + character offset under a touch, for text selection. */
    fun offsetAt(page: Int, viewX: Float, viewY: Float): Pair<Int, Int>? {
        val slot = slots.getOrNull(page) ?: return null
        val column = columnIndexAt(viewX, slot)
        val run = slot.columns.getOrNull(column) ?: return null
        val layout = layouts[run.chapterIndex]
        if (layout.text.isEmpty() || layout.lineCount == 0) return null
        if (viewY < metrics.textTop || viewY > metrics.textBottom) return null
        val layoutX = viewX - metrics.columnLeft(column, spec.columnCount)
        val layoutY = viewY - metrics.textTop + layout.getLineTop(run.startLine)
        val clampedY = layoutY.coerceIn(0f, (layout.height - 1).toFloat())
        val line = layout.getLineForVertical(clampedY.toInt())
            .coerceIn(run.startLine, run.endLine)
        val offset = layout.getOffsetForHorizontal(line, layoutX)
        return run.chapterIndex to offset
    }

    /** The chapter text inside [start, end), for a saved highlight's label. */
    fun chapterSlice(chapter: Int, start: Int, end: Int): String {
        val text = chapterText.getOrNull(chapter) ?: return ""
        if (start !in 0..text.length || end !in 0..text.length || end <= start) return ""
        return text.subSequence(start, end).toString()
    }

    /** Whole words around an offset; null when the offset is on whitespace only. */
    fun wordBounds(chapter: Int, offset: Int): IntRange? {
        val text = chapterText.getOrNull(chapter) ?: return null
        if (offset !in 0..text.length) return null
        var start = offset.coerceAtMost(text.length - 1).coerceAtLeast(0)
        var end = start
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        while (end < text.length && !text[end].isWhitespace()) end++
        if (start >= end) return null
        return start until end
    }

    /** Bands for an arbitrary character range of one chapter, in view space. */
    @Suppress("DEPRECATION")
    fun rangeRects(page: Int, chapter: Int, start: Int, end: Int): List<RectF> {
        val slot = slots.getOrNull(page) ?: return emptyList()
        val layout = layouts.getOrNull(chapter) ?: return emptyList()
        if (layout.text.isEmpty() || start < 0 || end <= start) return emptyList()
        val from = start.coerceIn(0, layout.text.length)
        val to = (end - 1).coerceIn(0, layout.text.length - 1)
        val startLine = layout.getLineForOffset(from)
        val endLine = layout.getLineForOffset(to)
        val out = ArrayList<RectF>(4)
        for ((column, run) in slot.columns.withIndex()) {
            if (run.chapterIndex != chapter) continue
            val firstLine = startLine.coerceAtLeast(run.startLine)
            val lastLine = endLine.coerceAtMost(run.endLine)
            if (lastLine < firstLine) continue
            val startsEarlier = startLine < run.startLine
            val continuesLater = endLine > run.endLine
            val topShift = metrics.textTop - layout.getLineTop(run.startLine)
            val left = metrics.columnLeft(column, spec.columnCount)
            for (line in firstLine..lastLine) {
                val startX = if (line == firstLine && !startsEarlier) {
                    layout.getPrimaryHorizontal(from)
                } else {
                    layout.getLineLeft(line).coerceAtLeast(0f)
                }
                val after = minOf(to + 1, layout.text.length)
                val endX = if (line == lastLine && !continuesLater) {
                    layout.getPrimaryHorizontal(after)
                } else {
                    layout.getLineRight(line)
                }
                if (endX <= startX) continue
                val top = layout.getLineTop(line) + topShift
                out += RectF(left + startX, top, left + endX, layout.getLineBottom(line) + topShift)
            }
        }
        return out
    }

    /** Which of the page's columns a touch is on; a touch in the gutter takes the nearer. */
    private fun columnIndexAt(x: Float, slot: PageSlot): Int {
        if (slot.columns.size == 1) return 0
        val mid = metrics.columnLeft(0, spec.columnCount) + columnWidth + metrics.columnGap / 2f
        return if (x < mid) 0 else 1
    }

    /**
     * The sentence's lines, as rectangles in view space, but only when it belongs to
     * [page] — the caller is drawing one page at a time.
     *
     * The first and last lines are trimmed to the sentence's own extent, so a band
     * never spills over the neighbouring sentence sharing a line. A sentence running
     * into the next column is drawn in both, each band inside its own column.
     */
    @Suppress("DEPRECATION")
    override fun highlightRects(page: Int, sentence: Int): List<RectF> {
        if (sentence < 0 || sentence >= sentenceCount) return emptyList()
        if (sentencePageIndex[sentence] != page) return emptyList()
        val slot = slots.getOrNull(page) ?: return emptyList()
        val from = sentenceStart[sentence]
        val to = sentenceStop[sentence] - 1
        if (to < from) return emptyList()

        val out = ArrayList<RectF>(4)
        val pad = 2f * metrics.density

        for ((column, run) in slot.columns.withIndex()) {
            if (run.chapterIndex != sentenceChapter[sentence]) continue
            val layout = layouts[run.chapterIndex]
            val startLine = layout.getLineForOffset(from)
            val endLine = layout.getLineForOffset(to)
            val firstLine = startLine.coerceAtLeast(run.startLine)
            val lastLine = endLine.coerceAtMost(run.endLine)
            if (lastLine < firstLine) continue

            // A sentence may run past either edge of this column; then its band must
            // reach the edge of the ink box rather than stop at its own extent.
            val startsEarlier = startLine < run.startLine
            val continuesLater = endLine > run.endLine

            // `getPrimaryHorizontal(offset)` (single-argument) is the only public
            // spelling of "x of this offset" — the clamped overload is a hidden API.
            val afterSentence = minOf(to + 1, layout.text.length)
            val topShift = metrics.textTop - layout.getLineTop(run.startLine)
            val left = metrics.columnLeft(column, spec.columnCount)

            for (line in firstLine..lastLine) {
                val startX = if (line == firstLine && !startsEarlier) {
                    layout.getPrimaryHorizontal(from)
                } else {
                    layout.getLineLeft(line)
                }
                var endX = if (line == lastLine && !continuesLater) {
                    layout.getPrimaryHorizontal(afterSentence)
                } else {
                    layout.getLineRight(line)
                }
                if (endX <= startX) endX = layout.getLineRight(line)
                if (endX <= startX) continue
                out += RectF(
                    left + startX - pad,
                    topShift + layout.getLineTop(line),
                    left + endX + pad,
                    topShift + layout.getLineBottom(line),
                )
            }
        }
        return out
    }

    /** The sentence a saved highlight anchored at (chapter, offset) starts. */
    fun sentenceAtAnchor(chapter: Int, offset: Int): Int = sentenceAtOffset(chapter, offset)

    /** (chapter, offset) for a sentence, the anchor a highlight is stored under. */
    fun anchorOfSentence(sentence: Int): Pair<Int, Int>? {
        if (sentence !in sentenceChapter.indices) return null
        val chapter = sentenceChapter[sentence]
        val start = sentenceStart[sentence] - chapterCharStart.getOrElse(chapter) { 0 }
        return chapter to start.coerceAtLeast(0)
    }

    /** Binary search for the sentence containing [offset] within [chapter]. */
    private fun sentenceAtOffset(chapter: Int, offset: Int): Int {
        if (sentenceChapter.isEmpty()) return -1
        var lo = 0
        var hi = sentenceChapter.size - 1
        var candidate = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val c = sentenceChapter[mid]
            val before = c < chapter || (c == chapter && sentenceStart[mid] <= offset)
            if (before) {
                candidate = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        if (candidate < 0) return -1
        if (sentenceChapter[candidate] != chapter) return -1
        if (offset >= sentenceStop[candidate]) {
            // Landed in the whitespace between two sentences: take the next one.
            val next = candidate + 1
            return if (next < sentenceChapter.size && sentenceChapter[next] == chapter) next else -1
        }
        return candidate
    }

    // -------------------------------------------------------------- rendering

    private fun render(index: Int): Bitmap {
        val slot = slots[index]
        val bitmap = Bitmap.createBitmap(metrics.width, metrics.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.paper)

        val chapterTitle = book.chapters[slot.chapterIndex].title
            .uppercase(Locale.getDefault())
            .replace(Regex("\\s+"), " ")
            .trim()
        if (chapterTitle.isNotEmpty()) {
            // One line between the two toolbar buttons: a longer title is cut with an
            // ellipsis rather than being allowed to run under a button.
            val fitted = TextUtils.ellipsize(
                chapterTitle,
                headerPaint,
                metrics.headerWidth,
                TextUtils.TruncateAt.END,
            )
            canvas.drawText(
                fitted,
                0,
                fitted.length,
                metrics.headerCentreX,
                headerBaseline,
                headerPaint,
            )
        }

        for ((column, run) in slot.columns.withIndex()) {
            val layout = layouts[run.chapterIndex]
            val left = metrics.columnLeft(column, spec.columnCount)
            val lineTopShift = metrics.textTop - layout.getLineTop(run.startLine).toFloat()
            canvas.save()
            // Clip to the last line this column actually owns, not to the raw text box:
            // a paragraph-final line carries the paragraph gap in its box, so the looser
            // box would let the next column's first line bleed in at the bottom.
            canvas.clipRect(
                left,
                metrics.textTop,
                left + columnWidth,
                minOf(
                    metrics.textTop + metrics.textHeight,
                    lineTopShift + layout.getLineBottom(run.endLine),
                ),
            )
            canvas.translate(left, lineTopShift)
            layout.draw(canvas)
            canvas.restore()
        }

        val footer = "Trang ${index + 1} / ${slots.size}"
        canvas.drawText(footer, metrics.width / 2f, footerBaseline, footerPaint)

        return bitmap
    }

    companion object {

        private const val CACHE_LIMIT = 3

        /** Characters of context a search snippet keeps either side of the match. */
        private const val SNIPPET_PAD = 46

        /** Paragraph gap, as a fraction of the body text size (iOS uses 11pt at 19pt). */
        private const val PARAGRAPH_GAP_EM = 0.55f

        /**
         * Builds one `StaticLayout` per chapter and slices each into pages.
         *
         * `spanned` must already exist (it is produced on the main thread by
         * `HtmlText`); this method itself is safe to run on a worker thread.
         */
        fun build(
            book: RawBook,
            spanned: List<SpannableStringBuilder>,
            metrics: PageMetrics,
            spec: TypesetSpec,
            palette: Palette,
        ): ReaderEngine {
            val bodyPaint = Typesetting.bodyPaint(spec, palette.ink)
            val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textSize = spec.sizePx * 11f / 19f
                color = palette.inkSoft
                textAlign = Paint.Align.CENTER
            }
            val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                textSize = spec.sizePx * 12f / 19f
                color = palette.inkSoft
                textAlign = Paint.Align.CENTER
            }

            val columnCount = spec.columnCount
            val layoutWidth = maxOf(40, ceil(metrics.columnWidth(columnCount)).toInt())
            val layouts = ArrayList<StaticLayout>(spanned.size)
            val slots = ArrayList<PageSlot>()

            for ((chapterIndex, text) in spanned.withIndex()) {
                Typesetting.prepare(text, spec, palette)
                val layout = Typesetting.layout(text, bodyPaint, layoutWidth, spec)
                layouts += layout

                val lineCount = layout.lineCount
                val runs = ArrayList<ColumnRun>()
                var line = 0
                while (line < lineCount) {
                    val top = layout.getLineTop(line)
                    var end = line
                    while (end + 1 < lineCount &&
                        layout.getLineBottom(end + 1) - top <= metrics.textHeight
                    ) {
                        end++
                    }
                    runs += ColumnRun(chapterIndex, line, end)
                    line = end + 1
                }
                if (lineCount == 0) {
                    // An empty chapter still deserves a page, so the counter and the
                    // page-of-book arithmetic stay honest.
                    runs += ColumnRun(chapterIndex, 0, 0)
                }
                // A chapter always starts a page of its own, and the columns then fill
                // its pages left to right.
                for (page in runs.chunked(columnCount)) {
                    slots += PageSlot(page)
                }
            }

            // Sentences are computed on the same (already paragraph-spaced) text the
            // layouts were built from, so their offsets line up with layout offsets.
            val chapterText = ArrayList<CharSequence>(layouts.size)
            val sChapter = ArrayList<Int>()
            val sStart = ArrayList<Int>()
            val sStop = ArrayList<Int>()
            val sPage = ArrayList<Int>()
            for (chapterIndex in layouts.indices) {
                val text = spanned[chapterIndex]
                chapterText += text
                if (text.isEmpty()) continue
                val layout = layouts[chapterIndex]
                val lines = maxOf(1, layout.lineCount)
                val lineToPage = IntArray(lines) { -1 }
                for ((pageIndex, slot) in slots.withIndex()) {
                    for (run in slot.columns) {
                        if (run.chapterIndex != chapterIndex) continue
                        var line = run.startLine
                        while (line <= run.endLine && line < lines) {
                            lineToPage[line] = pageIndex
                            line++
                        }
                    }
                }
                val fallbackPage = slots.indexOfFirst { slot ->
                    slot.columns.any { it.chapterIndex == chapterIndex }
                }
                for (range in Sentences.split(text)) {
                    val line = layout.getLineForOffset(range.first).coerceIn(0, lines - 1)
                    val page = if (lineToPage[line] >= 0) lineToPage[line] else fallbackPage
                    if (page < 0) continue
                    sChapter += chapterIndex
                    sStart += range.first
                    sStop += range.last + 1
                    sPage += page
                }
            }

            return ReaderEngine(
                book = book,
                layouts = layouts,
                slots = slots,
                metrics = metrics,
                spec = spec,
                palette = palette,
                headerPaint = headerPaint,
                footerPaint = footerPaint,
                chapterText = chapterText,
                sentenceChapter = sChapter.toIntArray(),
                sentenceStart = sStart.toIntArray(),
                sentenceStop = sStop.toIntArray(),
                sentencePageIndex = sPage.toIntArray(),
            )
        }
    }
}
