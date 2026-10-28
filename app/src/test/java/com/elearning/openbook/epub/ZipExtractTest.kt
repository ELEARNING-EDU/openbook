package com.elearning.openbook.epub

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipExtractTest {

    @Test
    fun normalisesSeparatorsAndLeadingDots() {
        assertEquals("OEBPS/ch01.xhtml", ZipExtract.normalise("./OEBPS/ch01.xhtml"))
        assertEquals("OEBPS/ch01.xhtml", ZipExtract.normalise("././OEBPS/ch01.xhtml"))
        assertEquals("OEBPS/ch01.xhtml", ZipExtract.normalise("OEBPS\\ch01.xhtml"))
        assertEquals("OEBPS/ch01.xhtml", ZipExtract.normalise("/OEBPS/ch01.xhtml"))
        assertEquals("mimetype", ZipExtract.normalise("mimetype"))
    }

    @Test
    fun readsFilesAndSkipsDirectoryEntries() {
        val archive = File.createTempFile("entries", ".zip").apply { deleteOnExit() }
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("META-INF/"))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("./OEBPS/ch01.xhtml"))
            zip.write("<html/>".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("OEBPS/images/pic.bin"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
        }

        val entries = ZipExtract.readAll(archive)

        assertEquals(setOf("OEBPS/ch01.xhtml", "OEBPS/images/pic.bin"), entries.keys)
        assertArrayEquals("<html/>".toByteArray(), entries["OEBPS/ch01.xhtml"])
        assertArrayEquals(byteArrayOf(1, 2, 3), entries["OEBPS/images/pic.bin"])
    }

    @Test
    fun anEmptyFileIsNotAZip() {
        val empty = File.createTempFile("empty", ".epub").apply { deleteOnExit() }

        // A zero-byte file has no central directory: it must be refused as NOT_ZIP,
        // not reported as a book with zero entries.
        val error = assertThrows(com.elearning.openbook.model.EpubException::class.java) {
            ZipExtract.readAll(empty)
        }
        assertEquals(com.elearning.openbook.model.EpubProblem.NOT_ZIP, error.problem)
    }
}
