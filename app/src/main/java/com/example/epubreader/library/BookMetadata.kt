package com.example.epubreader.library

/**
 * What the library knows about a book besides the file itself.
 *
 * [title] and [author] are the reader's own edits; `null` means "use the EPUB's own
 * metadata". [importedAt] is written once, the first time the file is seen, so a title
 * edit can never reorder "recently imported".
 */
data class BookMetadata(
    val title: String? = null,
    val author: String? = null,
    val importedAt: Long = 0L,
    /** Local "finished" stamp; 0 means not marked, and old records decode as 0. */
    val finishedAt: Long = 0L,
)

/**
 * A line-safe encoding for one book's [BookMetadata] in `SharedPreferences`.
 *
 * `|` separates the fields and `~` marks an absent value, so a title may contain
 * anything a real title contains — including newlines and the separators themselves.
 */
object BookMetadataCodec {

    fun encode(metadata: BookMetadata): String =
        "${metadata.importedAt}|${metadata.finishedAt}|${escape(metadata.title)}|${escape(metadata.author)}"

    fun decode(raw: String?): BookMetadata? {
        if (raw.isNullOrEmpty()) return null
        val parts = split(raw)
        // Older records hold three fields (no finished stamp); keep them readable.
        if (parts.size == 3) {
            return BookMetadata(
                importedAt = parts[0].toLongOrNull() ?: return null,
                title = unescape(parts[1]),
                author = unescape(parts[2]),
            )
        }
        if (parts.size != 4) return null
        return BookMetadata(
            importedAt = parts[0].toLongOrNull() ?: return null,
            finishedAt = parts[1].toLongOrNull() ?: 0L,
            title = unescape(parts[2]),
            author = unescape(parts[3]),
        )
    }

    private fun escape(value: String?): String {
        if (value == null) return "~"
        return buildString(value.length + 4) {
            for (char in value) {
                when (char) {
                    '\\' -> append("\\\\")
                    '|' -> append("\\p")
                    '\n' -> append("\\n")
                    '~' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }

    private fun unescape(value: String): String? {
        if (value == "~") return null
        return buildString(value.length) {
            var index = 0
            while (index < value.length) {
                val char = value[index]
                if (char == '\\' && index + 1 < value.length) {
                    when (value[index + 1]) {
                        'p' -> append('|')
                        'n' -> append('\n')
                        't' -> append('~')
                        '\\' -> append('\\')
                        else -> append(value[index + 1])
                    }
                    index += 2
                } else {
                    append(char)
                    index++
                }
            }
        }
    }

    /** Split on unescaped `|`, so a separator inside a title is not a field break. */
    private fun split(raw: String): List<String> {
        val fields = ArrayList<String>(4)
        val current = StringBuilder()
        var index = 0
        while (index < raw.length) {
            val char = raw[index]
            when {
                char == '\\' && index + 1 < raw.length -> {
                    current.append(char).append(raw[index + 1])
                    index += 2
                }
                char == '|' -> {
                    fields += current.toString()
                    current.setLength(0)
                    index++
                }
                else -> {
                    current.append(char)
                    index++
                }
            }
        }
        fields += current.toString()
        return fields
    }
}
