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

    @Test
    fun anEntryOverTheBudgetIsRefusedBeforeItIsBuffered() {
        val archive = archiveWith("OEBPS/ch01.xhtml" to "<html/>".toByteArray())

        val error = assertThrows(com.elearning.openbook.model.EpubException::class.java) {
            ZipExtract.readAll(archive, maxEntryBytes = 4)
        }
        assertEquals(com.elearning.openbook.model.EpubProblem.TOO_LARGE, error.problem)
    }

    @Test
    fun entriesTogetherOverTheBudgetAreRefused() {
        val archive = archiveWith(
            "a.bin" to byteArrayOf(1, 2, 3),
            "b.bin" to byteArrayOf(4, 5, 6),
        )

        val error = assertThrows(com.elearning.openbook.model.EpubException::class.java) {
            ZipExtract.readAll(archive, maxEntryBytes = 4, maxTotalBytes = 5)
        }
        assertEquals(com.elearning.openbook.model.EpubProblem.TOO_LARGE, error.problem)
    }

    @Test
    fun aStreamThatLiesAboutItsLengthIsStillCounted() {
        val error = assertThrows(com.elearning.openbook.model.EpubException::class.java) {
            ZipExtract.readLimited(java.io.ByteArrayInputStream(ByteArray(10)), 4, "entry")
        }
        assertEquals(com.elearning.openbook.model.EpubProblem.TOO_LARGE, error.problem)
    }

    private fun archiveWith(vararg entries: Pair<String, ByteArray>): File {
        val archive = File.createTempFile("bounded", ".zip").apply { deleteOnExit() }
        ZipOutputStream(archive.outputStream()).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return archive
    }
}
