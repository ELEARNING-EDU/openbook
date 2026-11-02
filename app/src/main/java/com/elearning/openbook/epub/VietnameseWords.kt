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
        context.assets.open(ASSET).bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toHashSet()
        }
    }.getOrDefault(emptySet()).also { cached = it }

    private const val ASSET = "vi-words.txt"
}
