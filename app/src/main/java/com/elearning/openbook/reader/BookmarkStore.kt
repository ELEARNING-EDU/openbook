package com.elearning.openbook.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * One saved place in a book: a chapter and a character offset, never a page number,
 * because page numbers do not survive a change of type size or theme.
 */
data class Bookmark(
    val chapterIndex: Int,
    val offset: Int,
) {
    /** Stable identity, so a list does not re-key when the book is re-typeset. */
    val id: String get() = "$chapterIndex-$offset"
}

/**
 * The marks of every book the reader has opened, kept in `SharedPreferences`.
 *
 * Marks belong to the book rather than to the reader: opening another book swaps the
 * list, and coming back restores it. A page counts as marked when a mark falls inside
 * it, so the state follows the page through a re-typeset instead of pointing at a page
 * number that has since moved.
 */
class BookmarkStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The open book's marks, always sorted by position. */
    var marks: List<Bookmark> by mutableStateOf(emptyList())
        private set

    private var currentKey: String = ""

    /** Switches to a book. Marks are per book, so this is a load, not a merge. */
    fun open(bookKey: String) {
        if (bookKey == currentKey) return
        currentKey = bookKey
        marks = read(bookKey)
    }

    /** A page is marked when one of its marks falls inside it. */
    fun isMarked(chapter: Int, start: Int, end: Int): Boolean =
        marks.any { it.chapterIndex == chapter && it.offset in start until maxOf(start + 1, end) }

    /**
     * Marks the page, or clears the mark it already carries. The offset stored is the
     * page's first character, which is what a re-typeset can be anchored on.
     */
    fun toggle(chapter: Int, start: Int, end: Int, offset: Int) {
        val existing = marks.firstOrNull {
            it.chapterIndex == chapter && it.offset in start until maxOf(start + 1, end)
        }
        marks = if (existing != null) {
            marks - existing
        } else {
            (marks + Bookmark(chapter, offset)).sortedWith(compareBy({ it.chapterIndex }, { it.offset }))
        }
        persist()
    }

    /** Removes one mark, for the list's own swipe or its remove button. */
    fun remove(mark: Bookmark) {
        marks = marks - mark
        persist()
    }

    /** Drops a deleted book's marks; they cannot belong to any book on the shelf. */
    fun erase(bookKey: String) {
        prefs.edit().remove(storageKey(bookKey)).apply()
        if (bookKey == currentKey) marks = emptyList()
    }

    private fun read(bookKey: String): List<Bookmark> {
        val raw = prefs.getString(storageKey(bookKey), null) ?: return emptyList()
        return runCatching {
            val array = org.json.JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                Bookmark(
                    chapterIndex = item.optInt("chapter", -1),
                    offset = item.optInt("offset", -1),
                ).takeIf { it.chapterIndex >= 0 && it.offset >= 0 }
            }.sortedWith(compareBy({ it.chapterIndex }, { it.offset }))
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        if (currentKey.isEmpty()) return
        val array = org.json.JSONArray()
        for (mark in marks) {
            array.put(
                JSONObject()
                    .put("chapter", mark.chapterIndex)
                    .put("offset", mark.offset),
            )
        }
        prefs.edit().putString(storageKey(currentKey), array.toString()).apply()
    }

    private fun storageKey(bookKey: String): String = "$KEY_PREFIX$bookKey"

    private companion object {
        const val PREFS = "readerBookmarks"
        const val KEY_PREFIX = "marks."
    }
}
