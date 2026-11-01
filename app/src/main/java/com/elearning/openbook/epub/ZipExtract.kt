package com.elearning.openbook.epub

import com.elearning.openbook.model.EpubException
import com.elearning.openbook.model.EpubProblem
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.Normalizer
import java.util.zip.ZipFile

/**
 * Pulls a whole EPUB into memory as a path -> bytes map.
 *
 * The central directory is the source of truth here (`ZipFile`, not `ZipInputStream`):
 * real EPUBs are produced by stream writers that put sizes in a data descriptor, store
 * entries with bit 3 set, or add ZIP64 fields — all of which a forward-only stream can
 * misread. Directory entries are skipped, and entry names are normalised to forward
 * slashes without a leading `./` and to Unicode NFC (macOS writes NFD).
 *
 * Decompression is bounded: a tiny archive can declare (or simply hide) an enormous
 * payload, and reading it whole would exhaust the heap before any validation ran. Both
 * the declared size and the bytes actually streamed are checked.
 */
object ZipExtract {

    /** No single entry may expand past this. */
    const val MAX_ENTRY_BYTES: Long = 64L * 1024 * 1024

    /** The whole archive may not expand past this. */
    const val MAX_TOTAL_BYTES: Long = 256L * 1024 * 1024

    private const val CHUNK = 64 * 1024

    fun readAll(
        file: File,
        maxEntryBytes: Long = MAX_ENTRY_BYTES,
        maxTotalBytes: Long = MAX_TOTAL_BYTES,
    ): LinkedHashMap<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        try {
            ZipFile(file).use { zip ->
                var total = 0L
                for (entry in zip.entries()) {
                    if (entry.isDirectory) continue
                    val name = normalise(entry.name)
                    if (name.isEmpty() || entries.containsKey(name)) continue
                    // The central directory's size is a claim; refuse it before allocating.
                    if (entry.size > maxEntryBytes) throw tooLarge(name, entry.size, maxEntryBytes)
                    val bytes = zip.getInputStream(entry).use { readLimited(it, maxEntryBytes, name) }
                    total += bytes.size
                    if (total > maxTotalBytes) throw tooLarge(name, total, maxTotalBytes)
                    entries[name] = bytes
                }
            }
        } catch (e: EpubException) {
            throw e
        } catch (t: Throwable) {
            throw EpubException(EpubProblem.NOT_ZIP)
        }
        return entries
    }

    /**
     * Reads one entry, refusing to buffer more than [limit] bytes.
     *
     * The declared entry size can lie, so the stream itself is counted as well.
     */
    fun readLimited(input: InputStream, limit: Long, name: String): ByteArray {
        val out = ByteArrayOutputStream(minOf(limit, CHUNK.toLong()).toInt().coerceAtLeast(32))
        val buffer = ByteArray(CHUNK)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw tooLarge(name, total, limit)
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun tooLarge(name: String, size: Long, limit: Long) = EpubException(
        EpubProblem.TOO_LARGE,
        "$name expands to $size bytes, over the $limit-byte limit",
    )

    /**
     * Finds an entry whose name may differ in case or Unicode form from the path the
     * OPF writes (CH02.XHTML vs ch02.xhtml, NFD on disk vs NFC in the href).
     */
    fun <T> find(entries: Map<String, T>, path: String): T? {
        val wanted = normalise(path)
        entries[wanted]?.let { return it }
        return entries.entries.firstOrNull { it.key.equals(wanted, ignoreCase = true) }?.value
    }

    fun normalise(name: String): String {
        var n = name.replace('\\', '/')
        while (n.startsWith("./")) n = n.substring(2)
        return Normalizer.normalize(n.trimStart('/'), Normalizer.Form.NFC)
    }
}
