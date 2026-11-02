package com.elearning.openbook.epub

import android.content.Context

/**
 * The Vietnamese words the importer is allowed to lean on, read once from
 * `assets/vi-words.txt` — one lowercase word per line.
 *
 * The list is deliberately small and hand-checked rather than a full dictionary: it holds
 * the `đ` words worth repairing and the vowel-initial words that must never be repaired.
 * A word missing from the list only means a repair is skipped; it never invents text.
 */
object VietnameseWords {

    private var cached: Set<String>? = null

    fun load(context: Context): Set<String> = cached ?: runCatching {
        val words = HashSet<String>(10_000)
        for (asset in ASSETS) {
            context.assets.open(asset).bufferedReader().useLines { lines ->
                for (line in lines) {
                    val word = line.trim().lowercase()
                    if (word.isNotEmpty() && !word.startsWith("#")) words += word
                }
            }
        }
        words
    }.getOrDefault(emptySet()).also { cached = it }

    private const val ASSET = "vi-words.txt"

    /** The hand-written repair list, then the broader lexicon used to judge a word. */
    private val ASSETS = listOf(ASSET, "vi-lexicon.txt")
}
