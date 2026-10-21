package com.example.epubreader.reader

import android.content.Context

/**
 * Where the reader left a book: the page they were on, and the text under it.
 *
 * The page number alone would be a lie after a re-typeset, so the text anchor (chapter
 * + character offset) is kept beside it. The anchor is what puts the reader back; the
 * page and the page count are what the shelf's bar is drawn from.
 */
data class ReadingPosition(
    val page: Int,
    val pageCount: Int,
    val chapter: Int,
    val offset: Int,
) {
    /** The page on screen over all of them, 0…1: the shelf's progress. */
    val fraction: Float
        get() = if (pageCount <= 0) 0f else ((page + 1).toFloat() / pageCount).coerceIn(0f, 1f)
}

/**
 * One reading position per book, in private preferences — the same kind of small state
 * the bookmarks live in, and for the same reason: closing the app is not losing the place.
 */
class ReadingPositions(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun of(bookId: String): ReadingPosition? {
        val raw = prefs.getString(bookId, null) ?: return null
        val values = raw.split('|').mapNotNull { it.toIntOrNull() }
        if (values.size != 4) return null
        val page = values[0]
        val pageCount = values[1]
        if (pageCount <= 0) return null
        return ReadingPosition(
            page = page.coerceIn(0, pageCount - 1),
            pageCount = pageCount,
            chapter = values[2],
            offset = values[3],
        )
    }

    fun save(bookId: String, position: ReadingPosition) {
        prefs.edit()
            .putString(
                bookId,
                "${position.page}|${position.pageCount}|${position.chapter}|${position.offset}",
            )
            .apply()
    }

    /** Every started book's progress, keyed by book id, for the shelf's own bars. */
    fun all(): Map<String, Float> = prefs.all.keys.mapNotNull { id ->
        of(id)?.let { id to it.fraction }
    }.toMap()

    private companion object {
        const val PREFS = "reading_positions"
    }
}
