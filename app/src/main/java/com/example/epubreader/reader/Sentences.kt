package com.example.epubreader.reader

/**
 * A chapter's text split into sentences, addressed purely by character offset.
 *
 * Offsets are into exactly the same `CharSequence` the paginator lays out, so a
 * `StaticLayout` line index and a sentence range are always comparable — which is
 * what lets a tap position be turned back into "the sentence under the finger".
 */
object Sentences {

    /** Characters that may trail a terminator: `xin chào!` / `(thật vậy.)` */
    private const val CLOSERS = "\"'”’»)]}…"

    /**
     * Splits [text] into trimmed, non-overlapping ranges.
     *
     * A sentence ends at `.`, `!`, `?` or `…` — together with any closing quotes or
     * brackets that follow — when the character after that is whitespace or the end
     * of the text, or at a hard line break. Requiring the whitespace keeps decimals
     * (`3.5`) and thousands separators (`1.000`) from being cut in half.
     */
    fun split(text: CharSequence): List<IntRange> {
        val out = ArrayList<IntRange>()
        val n = text.length
        var start = 0
        var i = 0
        while (i < n) {
            val c = text[i]
            when {
                c == '\n' -> {
                    emit(out, text, start, i)
                    start = i + 1
                }

                c == '.' || c == '!' || c == '?' || c == '…' -> {
                    var j = i + 1
                    while (j < n && text[j] in CLOSERS) j++
                    if (j >= n || isSpace(text[j])) {
                        emit(out, text, start, j)
                        start = j
                    }
                    i = j - 1
                }
            }
            i++
        }
        emit(out, text, start, n)
        return out
    }

    /**
     * The sentence as something worth saying out loud: non-breaking spaces become
     * real ones, newlines and runs of whitespace collapse to a single space.
     */
    fun readable(text: CharSequence, range: IntRange): String =
        text.subSequence(range.first, range.last + 1)
            .toString()
            .replace('\u00A0', ' ')
            .replace(WHITESPACE, " ")
            .trim()

    private fun emit(out: MutableList<IntRange>, text: CharSequence, from: Int, to: Int) {
        var a = from
        var b = to - 1
        while (a <= b && isSpace(text[a])) a++
        while (b >= a && isSpace(text[b])) b--
        if (a <= b) out += a..b
    }

    private fun isSpace(c: Char): Boolean =
        c == ' ' || c == '\n' || c == '\t' || c == '\r' || c == '\u00A0'

    private val WHITESPACE = Regex("\\s+")
}

/**
 * What the speech reader needs to know about a paginated book. Kept separate from
 * [ReaderEngine] so the playback logic can be reasoned about (and tested) on its own.
 */
interface SentenceSource {
    val sentenceCount: Int
    fun sentenceText(index: Int): String
    fun sentencePage(index: Int): Int
}
