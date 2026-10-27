package com.example.epubreader.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** One saved highlight: where it starts, and the words it covers. */
data class Highlight(
    val chapterIndex: Int,
    val offset: Int,
    val text: String,
) {
    val id: String get() = "$chapterIndex-$offset"
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

    fun add(chapter: Int, offset: Int, text: String) {
        val clean = text.trim()
        if (chapter < 0 || offset < 0 || clean.isEmpty()) return
        if (highlights.any { it.chapterIndex == chapter && it.offset == offset }) return
        highlights = (highlights + Highlight(chapter, offset, clean))
            .sortedWith(compareBy({ it.chapterIndex }, { it.offset }))
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
                Highlight(
                    chapterIndex = item.optInt("chapter", -1),
                    offset = item.optInt("offset", -1),
                    text = item.optString("text").orEmpty(),
                ).takeIf { it.chapterIndex >= 0 && it.offset >= 0 && it.text.isNotBlank() }
            }.sortedWith(compareBy({ it.chapterIndex }, { it.offset }))
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        if (currentKey.isEmpty()) return
        val array = JSONArray()
        for (highlight in highlights) {
            array.put(
                JSONObject()
                    .put("chapter", highlight.chapterIndex)
                    .put("offset", highlight.offset)
                    .put("text", highlight.text),
            )
        }
        prefs.edit().putString(storageKey(currentKey), array.toString()).apply()
    }

    private fun storageKey(bookKey: String) = "book.$bookKey"
}
