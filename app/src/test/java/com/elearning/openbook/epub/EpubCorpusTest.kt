package com.elearning.openbook.epub

import com.elearning.openbook.model.EpubException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/**
 * The shared iOS/Android corpus, parsed for real: every file is held to the promise in
 * the corpus `manifest.json` (mirrored in [CASES] below, so the suite needs no JSON
 * reader on a bare JVM).
 *
 * The corpus lives in the iOS build tree (gitignored there), so the suite reads it in
 * place and skips when the machine does not have it:
 * `-DepubCorpus=/path/to/epub-corpus`.
 */
@RunWith(Parameterized::class)
class EpubCorpusTest(
    private val name: String,
    private val expect: Expect,
    private val chapters: Int,
    private val title: String?,
    private val author: String?,
) {

    enum class Expect { OPENS, REFUSED }

    companion object {
        private val corpus: File by lazy {
            File(
                System.getProperty("epubCorpus")
                    ?: "/Users/cuong/Workspace/Orca/OpenBook/ios/build/qa/epub-corpus",
            )
        }

        private data class Case(
            val file: String,
            val expect: Expect = Expect.OPENS,
            val chapters: Int = -1,
            val title: String? = null,
            val author: String? = null,
        )

        // Mirrors ios/build/qa/epub-corpus/manifest.json, in its order.
        private val CASES = listOf(
            Case("01-epub2-ncx.epub", chapters = 3),
            Case("02-epub3-nav.epub", chapters = 4),
            Case("03-opf-root.epub", chapters = 3),
            Case("04-streamed-zip.epub", chapters = 3),
            Case("05-case-mismatch.epub", chapters = 4),
            Case("06-percent-unicode.epub", chapters = 3),
            Case("07-absolute-href.epub", chapters = 3),
            Case("08-linear-no.epub", chapters = 3),
            Case("09-obfuscated-font.epub", chapters = 3),
            Case("10-drm-aes.epub", expect = Expect.REFUSED),
            Case("11-lcp.epub", expect = Expect.REFUSED),
            Case("12-no-container.epub", expect = Expect.REFUSED),
            Case("13-mimetype-last.epub", chapters = 3),
            Case("14-fixed-layout.epub", chapters = 8),
            Case("15-image-heavy.epub", chapters = 5),
            Case("16-nested-toc.epub", chapters = 3),
            Case("17-rtl-arabic.epub", chapters = 2),
            Case("18-cjk.epub", chapters = 2),
            Case(
                "19-bom-entities.epub",
                chapters = 1,
                title = "Tom & Jerry — Nhà <3",
                author = "A & B",
            ),
            Case("20-zip64.epub", chapters = 2),
            Case("21-sloppy-html.epub", chapters = 1),
            Case("22-spine-missing-ref.epub", chapters = 1),
            Case("23-empty-spine.epub", expect = Expect.REFUSED),
            Case("24-renamed-pdf.epub", expect = Expect.REFUSED),
            Case("25-big-250-chapters.epub", chapters = 250),
            Case("26-1000-chapters.epub", chapters = 1000),
            Case("27-single-huge-chapter.epub", chapters = 1),
            Case("28-dot-slash.epub", chapters = 1),
            Case("29-nav-invalid-xml.epub", chapters = 1),
            Case("30-nfd-filename.epub", chapters = 1),
            Case("31-no-mimetype.epub", chapters = 2),
            Case("90-real-gutenberg.epub"),
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun books(): List<Array<Any>> = CASES.map {
            arrayOf<Any>(it.file, it.expect, it.chapters, it.title.orEmpty(), it.author.orEmpty())
        }
    }

    private val file: File get() = File(File(corpus, "books"), name)

    @Test
    fun matchesTheManifest() {
        assumeTrue("corpus not on this machine: $corpus", File(corpus, "manifest.json").isFile)
        assumeTrue("$name missing", file.isFile)

        if (expect == Expect.REFUSED) {
            var thrown: Throwable? = null
            try {
                EpubArchive.read(file)
            } catch (t: Throwable) {
                thrown = t
            }
            assertTrue("$name should be refused but opened", thrown is EpubException)
            assertTrue(
                "$name should be refused by readMetadata too",
                runCatching { EpubArchive.readMetadata(file) }.isFailure,
            )
            return
        }

        val parsed = EpubArchive.read(file)
        if (chapters >= 0) assertEquals("$name chapter count", chapters, parsed.chapters.size)
        assertTrue("$name has no text", parsed.chapters.any { it.html.isNotBlank() })
        title?.takeIf { it.isNotEmpty() }?.let { assertEquals("$name title", it, parsed.title) }
        author?.takeIf { it.isNotEmpty() }?.let { assertEquals("$name author", it, parsed.author) }
    }
}
