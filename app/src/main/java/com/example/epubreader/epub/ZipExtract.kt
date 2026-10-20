package com.example.epubreader.epub

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

/**
 * Pulls a whole EPUB into memory as a path -> bytes map.
 *
 * `ZipInputStream` transparently handles both storage methods used by EPUB
 * containers: 0 (stored) and 8 (deflated). Directory entries are skipped, and
 * entry names are normalised to forward slashes without a leading `./`.
 */
object ZipExtract {

    private const val BUFFER = 16 * 1024

    fun readAll(file: File): LinkedHashMap<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        FileInputStream(file).use { raw ->
            BufferedInputStream(raw).use { buffered ->
                ZipInputStream(buffered).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val name = normalise(entry.name)
                        if (name.isEmpty()) continue
                        val out = ByteArrayOutputStream(
                            if (entry.size in 1..Int.MAX_VALUE.toLong()) entry.size.toInt() else 4096
                        )
                        val buffer = ByteArray(BUFFER)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                        }
                        entries[name] = out.toByteArray()
                    }
                }
            }
        }
        return entries
    }

    fun normalise(name: String): String {
        var n = name.replace('\\', '/')
        while (n.startsWith("./")) n = n.substring(2)
        return n.trimStart('/')
    }
}
