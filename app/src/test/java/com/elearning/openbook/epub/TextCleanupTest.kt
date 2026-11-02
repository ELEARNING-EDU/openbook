package com.elearning.openbook.epub

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TextCleanupTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun epub(body: String, style: String = "body { color: #111; }"): File {
        val file = folder.newFile("book.epub")
        ZipOutputStream(file.outputStream()).use { out ->
            val type = "application/epub+zip".encodeToByteArray()
            out.putNextEntry(ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = type.size.toLong()
                compressedSize = type.size.toLong()
                crc = java.util.zip.CRC32().apply { update(type) }.value
            })
            out.write(type)
            out.closeEntry()
            for ((name, text) in listOf("OEBPS/chapter.xhtml" to body, "OEBPS/style.css" to style)) {
                out.putNextEntry(ZipEntry(name))
                out.write(text.encodeToByteArray())
                out.closeEntry()
            }
        }
        return file
    }

    private fun entry(file: File, name: String): String =
        ZipFile(file).use { zip -> zip.getInputStream(zip.getEntry(name)).readBytes().decodeToString() }

    @Test
    fun composesDecomposedTextAndDropsInvisibleCharacters() {
        val file = epub(
            "<p>tie\u0302\u0301ng Vie\u0323\u0302t\u200B va\u0300 mo\u0323\u0302\u00ADt</p>",
        )

        val report = TextCleanup.clean(file)

        assertEquals(1, report.files)
        assertTrue(report.changed)
        val body = entry(file, "OEBPS/chapter.xhtml")
        assertEquals("<p>tiếng Việt và một</p>", body)
        assertFalse(body.contains('\u200B'))
        assertFalse(body.contains('\u00AD'))
    }

    @Test
    fun leavesMarkupAndNonDocumentEntriesAlone() {
        val css = "/* tie\u0302\u0301ng */"
        val file = epub("<p class=\"a\">to\u0302\u0301t</p>", style = css)

        TextCleanup.clean(file)

        assertEquals("<p class=\"a\">tốt</p>", entry(file, "OEBPS/chapter.xhtml"))
        assertEquals(css, entry(file, "OEBPS/style.css"))
    }

    @Test
    fun keepsTheMediaTypeEntryFirstAndUncompressed() {
        val file = epub("<p>tie\u0302\u0301ng</p>")

        TextCleanup.clean(file)

        ZipFile(file).use { zip ->
            val first = zip.entries().nextElement()
            assertEquals("mimetype", first.name)
            assertEquals(ZipEntry.STORED, first.method)
            assertEquals("application/epub+zip", zip.getInputStream(first).readBytes().decodeToString())
        }
    }

    @Test
    fun reportsNothingOnASecondPass() {
        val file = epub("<p>tie\u0302\u0301ng</p>")

        assertTrue(TextCleanup.clean(file).changed)
        assertFalse(TextCleanup.clean(file).changed)
    }

    @Test
    fun restoresALostDWhenTheResultIsAKnownWord() {
        val file = epub(
            "<p>Người ta bảo nhau rằng biển rất đẹp và ất ã ó ời là những chữ đã mất.</p>",
        )

        val report = TextCleanup.clean(file, setOf("đất", "đã", "đó", "đời", "ăn", "em"))

        assertEquals(
            "<p>Người ta bảo nhau rằng biển rất đẹp và đất đã đó đời là những chữ đã mất.</p>",
            entry(file, "OEBPS/chapter.xhtml"),
        )
        assertEquals(4, report.words)
        assertEquals(1, report.files)
    }

    @Test
    fun keepsKnownWordsAndLettersThatAreNotVietnamese() {
        val file = epub(
            "<p>Người ta bảo nhau rằng em ăn ở nhà và i a vẫn đúng như vậy.</p>",
        )

        val report = TextCleanup.clean(
            file,
            setOf("em", "ăn", "ở", "đem", "đăn", "đở", "đi", "đa"),
        )

        assertEquals(
            "<p>Người ta bảo nhau rằng em ăn ở nhà và i a vẫn đúng như vậy.</p>",
            entry(file, "OEBPS/chapter.xhtml"),
        )
        assertEquals(0, report.words)
        assertFalse(report.changed)
    }

    @Test
    fun keepsTheCaseOfARestoredLetter() {
        val file = epub(
            "<p>Người ta bảo nhau rằng biển rất đẹp và Ã là chữ mất đầu câu.</p>",
        )

        TextCleanup.clean(file, setOf("đã"))

        assertEquals(
            "<p>Người ta bảo nhau rằng biển rất đẹp và ĐÃ là chữ mất đầu câu.</p>",
            entry(file, "OEBPS/chapter.xhtml"),
        )
    }

    @Test
    fun recognisesVietnameseFromTheTextItself() {
        assertTrue(
            TextCleanup.looksVietnamese(
                "Người ta bảo nhau rằng một người giữ đèn thì không được ngủ.",
            ),
        )
        assertFalse(
            TextCleanup.looksVietnamese(
                "The quick brown fox jumps over the lazy dog and runs away.",
            ),
        )
        // Too short to judge either way.
        assertFalse(TextCleanup.looksVietnamese("Tiếng Việt"))
    }

    @Test
    fun leavesAChapterInAnotherLanguageAloneEvenWithAWordList() {
        val file = epub(
            "<p>The quick brown fox jumps over the lazy dog and runs away before the hunter comes.</p>",
        )

        val report = TextCleanup.clean(file, setOf("đất", "đó", "được"))

        assertEquals(0, report.words)
        assertTrue(
            entry(file, "OEBPS/chapter.xhtml").startsWith("<p>The quick brown fox"),
        )
    }

    @Test
    fun leavesANonUtf8DocumentUntouched() {
        val file = folder.newFile("binary.epub")
        ZipOutputStream(file.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("mimetype"))
            out.write("application/epub+zip".encodeToByteArray())
            out.closeEntry()
            out.putNextEntry(ZipEntry("OEBPS/chapter.xhtml"))
            out.write(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00))
            out.closeEntry()
        }

        val report = TextCleanup.clean(file)

        assertFalse(report.changed)
        assertEquals(3, ZipFile(file).use { zip -> zip.getEntry("OEBPS/chapter.xhtml").size.toInt() })
    }
}
