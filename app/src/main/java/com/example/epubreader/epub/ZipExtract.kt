package com.example.epubreader.epub

import com.example.epubreader.model.EpubException
import com.example.epubreader.model.EpubProblem
import java.io.File
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
 */
object ZipExtract {

    fun readAll(file: File): LinkedHashMap<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        try {
            ZipFile(file).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory) continue
                    val name = normalise(entry.name)
                    if (name.isEmpty() || entries.containsKey(name)) continue
                    entries[name] = zip.getInputStream(entry).use { it.readBytes() }
                }
            }
        } catch (t: Throwable) {
            throw EpubException(EpubProblem.NOT_ZIP)
        }
        return entries
    }

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
