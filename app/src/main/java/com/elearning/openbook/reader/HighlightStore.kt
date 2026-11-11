package com.elearning.openbook.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** One saved highlight: where it starts, and the words it covers. */
data class Highlight(
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    /** ARGB band colour. Old records decode to the default amber. */
    val color: Int = DEFAULT_COLOR,
    /** The reader's own note, or blank. Old records decode to no note. */
    val note: String = "",
) {
    companion object {
        // Same hues as iOS `HighlightColor` (yellow, green, blue, pink). The alpha is lower
        // because Android draws the band over the text, iOS behind it.
        const val DEFAULT_COLOR = 0x66FFD54A
        val COLORS = listOf(
            0x66FFD54A,
            0x66A5D66A,
            0x666FA8DC,
            0x66F28DB2,
        )
    }

    val id: String get() = "$chapterIndex-$startOffset-$endOffset"
}

/** Highlights per book, kept in `SharedPreferences` and anchored on text, not pages. */
class HighlightStore(context: Context) {

    private val prefs = context.getSharedPreferences("highlights", Context.MODE_PRIVATE)

    var highlights: List<Highlight> by mutableStateOf(emptyList())
        private set

    private var currentKey: String = ""

    fun open(bookKey: String) {
        if (bookKey == currentKey) return
        currentKey = bookKey
        highlights = read(bookKey)
    }

    fun add(
        chapter: Int,
        start: Int,
        end: Int,
        text: String,
        color: Int = Highlight.DEFAULT_COLOR,
    ) {
        val clean = text.trim()
        if (chapter < 0 || start < 0 || end <= start || clean.isEmpty()) return
        if (highlights.any {
                it.chapterIndex == chapter && it.startOffset == start && it.endOffset == end
            }
        ) return
        highlights = (highlights + Highlight(chapter, start, end, clean, color))
            .sortedWith(compareBy({ it.chapterIndex }, { it.startOffset }))
        persist()
    }

    /** Saves the note; a blank note removes it. */
    fun setNote(highlight: Highlight, note: String) {
        val clean = note.trim()
        highlights = highlights.map { if (it.id == highlight.id) it.copy(note = clean) else it }
        persist()
    }

    fun recolour(highlight: Highlight, color: Int) {
        highlights = highlights.map { if (it.id == highlight.id) it.copy(color = color) else it }
        persist()
    }

    fun remove(highlight: Highlight) {
        highlights = highlights - highlight
        persist()
    }

    fun erase(bookKey: String) {
        prefs.edit().remove(storageKey(bookKey)).apply()
        if (bookKey == currentKey) highlights = emptyList()
    }

    private fun read(bookKey: String): List<Highlight> {
        val raw = prefs.getString(storageKey(bookKey), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val text = item.optString("text").orEmpty()
                val start = item.optInt("start", item.optInt("offset", -1))
                val end = item.optInt("end", if (start >= 0) start + text.length else -1)
                Highlight(
                    chapterIndex = item.optInt("chapter", -1),
                    startOffset = start,
                    endOffset = end,
                    text = text,
                    color = item.optInt("color", Highlight.DEFAULT_COLOR),
                    note = item.optString("note").orEmpty(),
                ).takeIf { it.chapterIndex >= 0 && it.startOffset >= 0 && it.endOffset > it.startOffset }
            }.sortedWith(compareBy({ it.chapterIndex }, { it.startOffset }))
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        if (currentKey.isEmpty()) return
        val array = JSONArray()
        for (highlight in highlights) {
            array.put(
                JSONObject()
                    .put("chapter", highlight.chapterIndex)
                    .put("start", highlight.startOffset)
                    .put("end", highlight.endOffset)
                    .put("text", highlight.text)
                    .put("color", highlight.color)
                    .put("note", highlight.note),
            )
        }
        prefs.edit().putString(storageKey(currentKey), array.toString()).apply()
    }

    private fun storageKey(bookKey: String) = "book.$bookKey"
}
