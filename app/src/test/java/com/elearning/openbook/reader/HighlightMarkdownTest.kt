package com.elearning.openbook.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class HighlightMarkdownTest {

    private val entries = listOf(
        // Given out of order on purpose: the export sorts by chapter, then offset.
        MarkdownHighlight(
            chapterIndex = 1,
            chapterTitle = "Mở đầu",
            startOffset = 50,
            text = "Biển đêm",
            note = "Hay",
        ),
        MarkdownHighlight(
            chapterIndex = 0,
            chapterTitle = "Lời tựa",
            startOffset = 10,
            text = "Ngọn đèn",
        ),
    )

    @Test
    fun `the fixture exports byte for byte`() {
        val expected = "# Ngọn Hải Đăng\n" +
            "Minh An\n" +
            "\n" +
            "## Lời tựa\n" +
            "\n" +
            "> Ngọn đèn\n" +
            "\n" +
            "## Mở đầu\n" +
            "\n" +
            "> Biển đêm\n" +
            "\n" +
            "Hay\n"
        assertEquals(expected, highlightsMarkdown("Ngọn Hải Đăng", "Minh An", entries))
    }

    @Test
    fun `an unknown author leaves no line behind`() {
        val expected = "# Sách\n\n## Chương 1\n\n> Một câu\n"
        assertEquals(
            expected,
            highlightsMarkdown(
                "Sách",
                "",
                listOf(MarkdownHighlight(0, "Chương 1", 0, "Một câu")),
            ),
        )
    }

    @Test
    fun `every line of a multi-line highlight is quoted`() {
        val expected = "# Sách\n\n## Chương 1\n\n> Một\n> Hai\n"
        assertEquals(
            expected,
            highlightsMarkdown(
                "Sách",
                "",
                listOf(MarkdownHighlight(0, "Chương 1", 0, "Một\nHai")),
            ),
        )
    }
}
