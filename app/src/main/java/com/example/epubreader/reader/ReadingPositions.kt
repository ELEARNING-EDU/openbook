package com.example.epubreader.reader

import android.content.Context

/**
 * Where the reader left a book: the page they were on, and the text under it.
 *
 * The page number alone would be a lie after a re-typeset, so the text anchor (chapter
 * + character offset) is kept beside it. The anchor is what puts the reader back; the
 * page and the page count are what the shelf's bar is drawn from. [updatedAt] is when
 * the place was last written, so Home can put the most recently read book first.
 */
data class ReadingPosition(
    val page: Int,
    val pageCount: Int,
    val chapter: Int,
    val offset: Int,
    val updatedAt: Long = 0L,
) {
    /** The page on screen over all of them, 0…1: the shelf's progress. */
    val fraction: Float
        get() = if (pageCount <= 0) 0f else ((page + 1).toFloat() / pageCount).coerceIn(0f, 1f)

    fun encode(): String = "$page|$pageCount|$chapter|$offset|$updatedAt"

    companion object {
        /** Accepts the four-field form written before [updatedAt] existed. */
        fun decode(raw: String?): ReadingPosition? {
            val parts = raw?.split('|') ?: return null
            if (parts.size !in 4..5) return null
            val page = parts[0].toIntOrNull() ?: return null
            val pageCount = parts[1].toIntOrNull() ?: return null
            val chapter = parts[2].toIntOrNull() ?: return null
            val offset = parts[3].toIntOrNull() ?: return null
            val updatedAt = parts.getOrNull(4)?.toLongOrNull() ?: 0L
            if (pageCount <= 0) return null
            return ReadingPosition(
                page = page.coerceIn(0, pageCount - 1),
                pageCount = pageCount,
                chapter = chapter,
                offset = offset,
                updatedAt = updatedAt,
            )
        }
    }
}

/**
 * One reading position per book, in private preferences — the same kind of small state
 * the bookmarks live in, and for the same reason: closing the app is not losing the place.
 */
/** The one read the reader needs; an interface so its ViewModel can be tested. */
interface SavedPlaces {
    fun of(bookId: String): ReadingPosition?
}

class ReadingPositions(context: Context) : SavedPlaces {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun of(bookId: String): ReadingPosition? =
        ReadingPosition.decode(prefs.getString(bookId, null))

    fun save(bookId: String, position: ReadingPosition) {
        prefs.edit()
            .putString(bookId, position.copy(updatedAt = System.currentTimeMillis()).encode())
            .apply()
    }

    /** Every started book's progress, keyed by book id, for the shelf's own bars. */
    fun all(): Map<String, Float> = allPositions().mapValues { it.value.fraction }

    /** Every started book's place, for Home's "carry on" row and the shelf. */
    fun allPositions(): Map<String, ReadingPosition> =
        prefs.all.keys.mapNotNull { id -> of(id)?.let { id to it } }.toMap()

    /** A deleted book must not leave a place behind that a new import could inherit. */
    fun remove(bookId: String) {
        prefs.edit().remove(bookId).apply()
    }

    private companion object {
        const val PREFS = "reading_positions"
    }
}
