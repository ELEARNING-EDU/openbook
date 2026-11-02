package com.elearning.openbook.epub

import java.io.File
import java.text.Normalizer
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** What a text pass over an imported copy changed; nothing changed when both are zero. */
data class TextCleanupReport(val passages: Int = 0, val files: Int = 0) {
    val changed: Boolean get() = passages > 0
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
    private val junk = charArrayOf(
        '\u00AD', // soft hyphen
        '\u200B', '\u200C', '\u200D', '\u200E', '\u200F',
        '\uFEFF',
    )

    /** Rewrites [file], or leaves it alone when there is nothing to fix or the pass fails. */
    fun clean(file: File): TextCleanupReport {
        val temp = File(file.parentFile, file.name + ".clean")
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
                        if (bytes != null && document.containsMatchIn(entry.name)) {
                            val text = runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }
                                .getOrNull()
                            if (text != null) {
                                val (cleaned, count) = cleanMarkup(text)
                                if (count > 0) {
                                    payload = cleaned.encodeToByteArray()
                                    changed = count
                                }
                            }
                        }
                        out.putNextEntry(entryFor(entry, payload))
                        if (payload != null && !entry.isDirectory) out.write(payload)
                        out.closeEntry()
                        if (changed > 0) report = report.copy(
                            passages = report.passages + changed,
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
    private fun cleanMarkup(html: String): Pair<String, Int> {
        val out = StringBuilder(html.length)
        var changed = 0
        var at = 0
        for (match in tag.findAll(html)) {
            val text = html.substring(at, match.range.first)
            val cleaned = cleanText(text)
            if (cleaned != text) changed++
            out.append(cleaned).append(match.value)
            at = match.range.last + 1
        }
        val tail = html.substring(at)
        val cleaned = cleanText(tail)
        if (cleaned != tail) changed++
        out.append(cleaned)
        return out.toString() to changed
    }

    private fun cleanText(value: String): String {
        if (value.isEmpty()) return value
        var text = Normalizer.normalize(value, Normalizer.Form.NFC)
        if (text.any { it in junk }) text = text.filterNot { it in junk }
        return text
    }
}
