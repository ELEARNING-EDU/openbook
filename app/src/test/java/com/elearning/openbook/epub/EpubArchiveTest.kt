package com.elearning.openbook.epub

import com.elearning.openbook.model.EpubException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubArchiveTest {

    // Gradle runs unit tests with the :app module as the working directory.
    private val sample = File("src/main/assets/sample.epub")

    @Test
    fun readsMetadataFromTheRealSampleBook() {
        assertTrue("sample.epub missing at ${sample.absolutePath}", sample.isFile)
        val metadata = EpubArchive.readMetadata(sample)
        assertEquals("Ngọn Hải Đăng Cuối Cùng", metadata.title)
        assertEquals("Nguyễn Minh An", metadata.author)
    }

    @Test
    fun readsEverySpineChapterAndTheNavigationDocument() {
        val book = EpubArchive.read(sample)

        assertEquals(6, book.chapters.size)
        assertEquals(
            listOf(
                "1. Ngọn đèn trên mũi đá",
                "2. Người giữ đèn",
                "3. Con tàu không tên",
                "4. Bão tháng Chín",
                "5. Bức thư trong chai",
                "6. Ngày đèn tắt",
            ),
            book.toc.map { it.title },
        )
        assertEquals((0..5).toList(), book.toc.map { it.chapterIndex })
        assertTrue(book.chapters.all { it.html.isNotBlank() })
        assertEquals("vi", book.language)
    }

    @Test
    fun resolvesHrefsAgainstTheChapterDirectory() {
        assertEquals(
            "OEBPS/images/cover.jpg",
            EpubArchive.resolve("../images/cover.jpg", "OEBPS/text"),
        )
        assertEquals("OEBPS/ch01.xhtml", EpubArchive.resolve("./text/../ch01.xhtml", "OEBPS"))
        assertEquals("OEBPS/ch01.xhtml", EpubArchive.resolve("ch01.xhtml#s1", "OEBPS"))
        assertEquals("OEBPS/my cover.xhtml", EpubArchive.resolve("my%20cover.xhtml", "OEBPS"))
        assertEquals("OEBPS/ch01.xhtml", EpubArchive.resolve("/OEBPS/ch01.xhtml", ""))
    }

    @Test
    fun parentTraversalCannotEscapeTheArchiveRoot() {
        assertEquals("ch01.xhtml", EpubArchive.resolve("../../../../ch01.xhtml", "OEBPS/text"))
    }

    @Test
    fun rejectsAFileWithoutAContainer() {
        val notAnEpub = File.createTempFile("not-an-epub", ".epub").apply {
            writeBytes("this is not a zip".toByteArray())
            deleteOnExit()
        }

        assertThrows(EpubException::class.java) {
            EpubArchive.readMetadata(notAnEpub)
        }
    }

    @Test
    fun rejectsAContainerThatPointsAtAMissingPackage() {
        val containerOnly = File.createTempFile("container-only", ".epub").apply {
            deleteOnExit()
            ZipOutputStream(outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("META-INF/container.xml"))
                zip.write(
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <container xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                      <rootfiles>
                        <rootfile full-path="OEBPS/missing.opf"
                                  media-type="application/oebps-package+xml"/>
                      </rootfiles>
                    </container>
                    """.trimIndent().toByteArray(),
                )
                zip.closeEntry()
            }
        }

        val error = assertThrows(EpubException::class.java) {
            EpubArchive.readMetadata(containerOnly)
        }
        assertTrue(error.message.orEmpty().contains("missing.opf"))
    }

    @Test
    fun imageHeavyBookCarriesThePicturesTheMarkupAsksFor() {
        val corpus = File(
            System.getProperty("epubCorpus")
                ?: "/Users/cuong/Workspace/Orca/OpenBook/ios/build/qa/epub-corpus",
        )
        val book = File(File(corpus, "books"), "15-image-heavy.epub")
        org.junit.Assume.assumeTrue("corpus not on this machine", book.isFile)

        val parsed = EpubArchive.read(book)
        val withPictures = parsed.chapters.filter { it.images.isNotEmpty() }
        assertTrue("no chapter kept its pictures", withPictures.isNotEmpty())
        assertTrue(
            "a picture's bytes are empty",
            withPictures.all { chapter -> chapter.images.values.all { it.isNotEmpty() } },
        )
    }
}
