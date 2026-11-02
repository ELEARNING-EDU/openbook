package com.elearning.openbook.epub

import java.io.File
import java.text.Normalizer
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** What a text pass over an imported copy changed; nothing changed when all are zero. */
data class TextCleanupReport(
    val passages: Int = 0,
    val words: Int = 0,
    val files: Int = 0,
) {
    val changed: Boolean get() = passages > 0 || words > 0
}

/**
 * Repairs the text of an EPUB copy in place: composed Unicode, and no soft hyphen,
 * zero-width or byte-order characters. Markup is untouched — only the text between
 * tags changes — and every other entry is copied byte for byte, with `mimetype` kept
 * first and stored, which the format requires.
 *
 * Deliberately language-neutral: the shelf holds books in many languages, so there is
 * no dictionary here and no guessing at missing letters. A book that is not valid
 * UTF-8 in a document is left exactly as it was.
 */
object TextCleanup {

    private const val MIMETYPE = "mimetype"

    private val document = Regex("\\.(x?html?|htm)$", RegexOption.IGNORE_CASE)
    private val tag = Regex("<[^>]*>")
    private val word = Regex("\\p{L}+")
    private val junk = charArrayOf(
        '\u00AD', // soft hyphen
        '\u200B', '\u200C', '\u200D', '\u200E', '\u200F',
        '\uFEFF',
    )

    /**
     * Whether a document reads as Vietnamese, judged from the text itself. Shop copies
     * are often mislabelled (`dc:language` says `en` for a Vietnamese book), and a
     * mixed-language book can hold both, so the package's claim is not trusted.
     */
    internal fun looksVietnamese(text: CharSequence): Boolean {
        var letters = 0
        var vietnamese = 0
        for (character in text) {
            if (!character.isLetter()) continue
            letters++
            if (character.lowercaseChar() in vietnameseLetters) vietnamese++
        }
        return letters >= 20 && vietnamese * 50 >= letters
    }

    private val vietnameseLetters =
        "àáảãạăằắẳẵặâầấẩẫậđèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵ".toSet()

    /** Rewrites [file], or leaves it alone when there is nothing to fix or the pass fails. */
    fun clean(file: File, words: Set<String> = emptySet()): TextCleanupReport {        val temp = File(file.parentFile, file.name + ".clean")
        var report = TextCleanupReport()
        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries().toList()
                ZipOutputStream(temp.outputStream().buffered()).use { out ->
                    val ordered = entries.sortedBy { if (it.name == MIMETYPE) 0 else 1 }
                    for (entry in ordered) {
                        val bytes = if (entry.isDirectory) null
                        else zip.getInputStream(entry).use { it.readBytes() }
                        var payload = bytes
                        var changed = 0
                        var repaired = 0
                        if (bytes != null && document.containsMatchIn(entry.name)) {
                            val text = runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }
                                .getOrNull()
                            if (text != null) {
                                val dictionary = if (looksVietnamese(text)) words else emptySet()
                                val (cleaned, count, fixed) = cleanMarkup(text, dictionary)
                                if (count > 0 || fixed > 0) {
                                    payload = cleaned.encodeToByteArray()
                                    changed = count
                                    repaired = fixed
                                }
                            }
                        }
                        out.putNextEntry(entryFor(entry, payload))
                        if (payload != null && !entry.isDirectory) out.write(payload)
                        out.closeEntry()
                        if (changed > 0 || repaired > 0) report = report.copy(
                            passages = report.passages + changed,
                            words = report.words + repaired,
                            files = report.files + 1,
                        )
                    }
                }
            }
            if (!report.changed) {
                temp.delete()
                return report
            }
            if (!temp.renameTo(file)) {
                file.delete()
                if (!temp.renameTo(file)) {
                    temp.delete()
                    return TextCleanupReport()
                }
            }
            return report
        } catch (t: Throwable) {
            temp.delete()
            android.util.Log.w("TextCleanup", "left ${file.name} untouched: ${t.javaClass.simpleName}")
            return TextCleanupReport()
        }
    }

    private fun entryFor(source: ZipEntry, payload: ByteArray?): ZipEntry {
        val target = ZipEntry(source.name)
        target.time = source.time
        target.comment = source.comment
        // The first entry must be the media type, and it must not be compressed, so the
        // CRC and the size have to be known before the entry is opened.
        if (source.name == MIMETYPE) {
            val body = payload ?: ByteArray(0)
            target.method = ZipEntry.STORED
            target.size = body.size.toLong()
            target.compressedSize = body.size.toLong()
            target.crc = java.util.zip.CRC32().apply { update(body) }.value
        }
        return target
    }

    /** Cleans only the runs between tags; the tags themselves are copied verbatim. */
    private fun cleanMarkup(
        html: String,
        words: Set<String>,
    ): Triple<String, Int, Int> {
        val out = StringBuilder(html.length)
        var changed = 0
        var repaired = 0
        var at = 0
        for (match in tag.findAll(html)) {
            val text = html.substring(at, match.range.first)
            val (cleaned, count, fixed) = cleanText(text, words)
            if (count > 0) changed++
            repaired += fixed
            out.append(cleaned).append(match.value)
            at = match.range.last + 1
        }
        val tail = html.substring(at)
        val (cleaned, count, fixed) = cleanText(tail, words)
        if (count > 0) changed++
        repaired += fixed
        out.append(cleaned)
        return Triple(out.toString(), changed, repaired)
    }

    private fun cleanText(value: String, words: Set<String>): Triple<String, Int, Int> {
        if (value.isEmpty()) return Triple(value, 0, 0)
        var text = Normalizer.normalize(value, Normalizer.Form.NFC)
        if (text.any { it in junk }) text = text.filterNot { it in junk }
        val tidied = tidy(text)
        val passage = if (text != value || tidied != text) 1 else 0
        val (repaired, count) = if (words.isEmpty()) tidied to 0 else repairWords(tidied, words)
        return Triple(repaired, passage, count)
    }

    /**
     * The mechanical slips every conversion makes, all of them language-neutral:
     * runs of spaces, a space before a comma or a full stop, and a word typed twice
     * in a row. Numbers keep their separators ("1.000", "10,5"), because a space is
     * only added after `, ; : ! ?` when a letter follows.
     */
    private fun tidy(text: String): String {
        var out = spaces.replace(text, " ")
        out = spaceBeforePunctuation.replace(out, "$1")
        out = punctuationGlue.replace(out, "$1 ")
        out = doubledWord.replace(out) { match -> match.groupValues[1] }
        return out
    }

    private val spaces = Regex("[ \\t]{2,}")
    private val spaceBeforePunctuation = Regex("[ \\t]+([,.;:!?%)\\]}»)”…])")
    private val punctuationGlue = Regex("([,;:!?])(?=\\p{L})")
    private val doubledWord = Regex("(?iu)\\b(\\p{L}+)(?:[ \\t]+\\1\\b)+")

    /**
     * Repairs a word that is not in the list by the two slips conversions make: a lost
     * `đ` ("ất" for "đất") and a letter typed twice ("đượcc"). Only a replacement that
     * turns the word into one the list knows is made, so nothing is invented.
     */
    private fun repairWords(text: String, words: Set<String>): Pair<String, Int> {
        var rebuilt: StringBuilder? = null
        var last = 0
        var count = 0
        for (match in word.findAll(text)) {
            val token = match.value
            val lower = token.lowercase()
            if (lower in words) continue
            val repaired = when {
                !lower.startsWith("đ") && lower.any { it.code > 127 } && "đ$lower" in words -> "đ$lower"
                else -> collapseRepeats(lower).takeIf { it != lower && it in words }
            } ?: continue
            val out = rebuilt ?: StringBuilder(text.length + 8).also { rebuilt = it }
            out.append(text, last, match.range.first).append(withCase(token, repaired))
            last = match.range.last + 1
            count++
        }
        val out = rebuilt ?: return text to 0
        out.append(text, last, text.length)
        return out.toString() to count
    }

    /** `đượcc` → `được`: any run of one letter squeezed to a single letter. */
    private fun collapseRepeats(word: String): String = repeatRun.replace(word, "$1")

    private fun withCase(source: String, replacement: String): String = when {
        source.all { it.isUpperCase() } -> replacement.uppercase()
        source.first().isUpperCase() -> replacement.replaceFirstChar { it.uppercaseChar() }
        else -> replacement
    }

    private val repeatRun = Regex("(\\p{L})\\1+")
}
