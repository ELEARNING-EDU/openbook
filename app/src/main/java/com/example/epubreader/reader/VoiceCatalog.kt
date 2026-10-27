package com.example.epubreader.reader

import java.util.Locale

/** One installed system voice, with what the picker prints beside it. */
data class VoiceOption(
    val name: String,
    val label: String,
    val locale: Locale,
    val quality: Int,
)

/** A language/region group in the voice picker. */
data class VoiceGroup(
    val locale: Locale,
    val title: String,
    val voices: List<VoiceOption>,
)

/**
 * The rules the voice picker and the engine share: which language a book is in, which
 * voices are offered for it, and which installed voice speaks as the default.
 *
 * Kept free of Android classes so the matching rules can be unit-tested on the JVM.
 */
object VoiceCatalog {

    /** Diacritics that only Vietnamese uses; a sample carrying one is Vietnamese. */
    private val VIETNAMESE = "ăâđêôơưàảãáạằẳẵắặầẩẫấậèẻẽéẹềểễếệìỉĩíịòỏõóọồổỗốộờởỡớợùủũúụừửữứựỳỷỹýỵ".toSet()

    /** `dc:language` first; a script guess when the book does not say; the phone last. */
    fun resolveLanguage(declared: String, sample: String, fallback: Locale): Locale {
        val tag = declared.trim().substringBefore(',').substringBefore(';').trim()
        if (tag.isNotEmpty()) {
            val parsed = runCatching { Locale.forLanguageTag(tag.replace('_', '-')) }.getOrNull()
            if (parsed != null && parsed.language.isNotEmpty()) return parsed
        }
        return guessLanguage(sample, fallback)
    }

    fun guessLanguage(sample: String, fallback: Locale): Locale {
        if (sample.any { it.lowercaseChar() in VIETNAMESE }) return Locale("vi", "VN")
        if (sample.any { it in '\u3040'..'\u30FF' }) return Locale.JAPANESE
        if (sample.any { it in '\u4E00'..'\u9FFF' }) return Locale.CHINESE
        return fallback
    }

    /** `vi` and `vi-VN` are the same language; a region only refines it. */
    fun speaks(voice: Locale, book: Locale): Boolean {
        val a = voice.language.lowercase()
        val b = book.language.lowercase()
        return a == b || (a == "vi" && b == "vie") || (a == "vie" && b == "vi")
    }

    /**
     * The voices the picker may show: the book's language when the device has any,
     * everything when it has none (an empty picker would be a dead end).
     */
    fun forBook(all: List<VoiceOption>, book: Locale): List<VoiceOption> =
        all.filter { speaks(it.locale, book) }.ifEmpty { all }

    /**
     * The book's saved voice, then its own language, the phone's language, Vietnamese,
     * then anything. [voices] is already limited to the book's language when possible.
     */
    fun pickDefault(
        voices: List<VoiceOption>,
        book: Locale,
        device: Locale,
        saved: String?,
    ): VoiceOption? =
        voices.firstOrNull { it.name == saved }
            ?: voices.firstOrNull { speaks(it.locale, book) && it.locale.country == book.country }
            ?: voices.firstOrNull { speaks(it.locale, book) }
            ?: voices.firstOrNull { it.locale.language == device.language }
            ?: voices.firstOrNull { it.locale.language == "vi" && it.locale.country == "VN" }
            ?: voices.firstOrNull { it.locale.language == "vi" }
            ?: voices.firstOrNull()

    /**
     * The picker's rows: grouped by language and region, the book's language first,
     * Vietnamese next, then the rest A→Z; voices A→Z inside a group.
     *
     * A query matching the group's language or locale code keeps the whole group; a
     * query matching a voice keeps just that row.
     */
    fun groups(
        voices: List<VoiceOption>,
        book: Locale,
        ui: Locale,
        query: String,
    ): List<VoiceGroup> {
        val q = query.trim().lowercase()
        fun groupMatches(locale: Locale): Boolean {
            if (q.isEmpty()) return true
            return locale.toLanguageTag().lowercase().contains(q) ||
                locale.getDisplayName(ui).lowercase().contains(q) ||
                locale.getDisplayLanguage(ui).lowercase().contains(q)
        }
        return voices
            .groupBy { "${it.locale.language}-${it.locale.country}" }
            .map { (_, list) ->
                val locale = list.first().locale
                val shown = if (q.isEmpty() || groupMatches(locale)) {
                    list
                } else {
                    list.filter {
                        it.label.lowercase().contains(q) || it.name.lowercase().contains(q)
                    }
                }
                VoiceGroup(
                    locale = locale,
                    title = locale.getDisplayName(ui).ifBlank { locale.toLanguageTag() },
                    voices = shown.sortedBy { it.label.lowercase() },
                )
            }
            .filter { it.voices.isNotEmpty() }
            .sortedWith(
                compareBy(
                    { !speaks(it.locale, book) },
                    { it.locale.language != "vi" },
                    { it.title.lowercase() },
                ),
            )
    }

    /**
     * A readable name for a voice id. Android exposes only ids such as
     * `vi-vn-x-gft-network`, so the locale prefix and the `x` marker are dropped and the
     * remaining tokens are title-cased: `Gft · Network`, `Vic · Local`, `Ling`.
     */
    fun label(name: String): String {
        val stripped = name.replaceFirst(LOCALE_PREFIX, "")
        val tokens = stripped.split('-', '_', ' ')
            .filter { it.isNotBlank() && !it.equals("x", ignoreCase = true) }
        if (tokens.isEmpty()) return name
        val pretty = tokens.map { token ->
            val lower = token.lowercase()
            when (lower) {
                "local" -> "Local"
                "network" -> "Network"
                "language" -> "Default"
                else -> token.replaceFirstChar { it.uppercase() }
            }
        }
        val quality = pretty.lastOrNull()
        return if (quality == "Local" || quality == "Network" || quality == "Default") {
            (pretty.dropLast(1).joinToString(" ") + " · " + quality).trim()
        } else {
            pretty.joinToString(" ")
        }
    }

    private val LOCALE_PREFIX = Regex("^[a-z]{2,3}[-_][a-z]{2,4}(?:[-_]|$)", RegexOption.IGNORE_CASE)
}
