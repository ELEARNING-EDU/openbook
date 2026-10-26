package com.example.epubreader.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentencesTest {

    private fun parts(source: String): List<String> =
        Sentences.split(source).map { Sentences.readable(source, it) }

    @Test
    fun splitsOnTerminatorsFollowedByWhitespace() {
        assertEquals(
            listOf("Xin chào.", "Tôi là An!", "Bạn khỏe không?"),
            parts("Xin chào. Tôi là An! Bạn khỏe không?"),
        )
    }

    @Test
    fun keepsDecimalsAndThousandsSeparatorsTogether() {
        assertEquals(
            listOf("Giá là 1.000 đồng và cao 3.5 mét.", "Hết."),
            parts("Giá là 1.000 đồng và cao 3.5 mét. Hết."),
        )
    }

    @Test
    fun doesNotSplitWhenTerminatorHasNoFollowingSpace() {
        assertEquals(listOf("A.B"), parts("A.B"))
    }

    @Test
    fun keepsClosingQuotesWithTheSentence() {
        assertEquals(
            listOf("Anh ấy nói: \"Được.\"", "Rồi đi."),
            parts("Anh ấy nói: \"Được.\" Rồi đi."),
        )
    }

    @Test
    fun keepsClosingBracketsWithTheSentence() {
        assertEquals(
            listOf("Anh ấy nói (thật vậy.)", "Rồi đi."),
            parts("Anh ấy nói (thật vậy.) Rồi đi."),
        )
    }

    @Test
    fun hardLineBreakEndsASentenceWithoutPunctuation() {
        assertEquals(
            listOf("Dòng một", "Dòng hai", "Dòng ba"),
            parts("Dòng một\nDòng hai\n\nDòng ba"),
        )
    }

    @Test
    fun ellipsisIsATerminator() {
        assertEquals(
            listOf("Chờ đã…", "Rồi sao?!", "Được."),
            parts("Chờ đã… Rồi sao?! Được."),
        )
    }

    @Test
    fun textWithoutTerminatorIsOneSentence() {
        val source = "Một câu không có dấu kết thúc"
        assertEquals(listOf(source), parts(source))
    }

    @Test
    fun emptyAndBlankTextHaveNoSentences() {
        assertTrue(Sentences.split("").isEmpty())
        assertTrue(Sentences.split("  \n\t\r\n ").isEmpty())
    }

    @Test
    fun trailingNewlineDoesNotCreateAnEmptySentence() {
        assertEquals(listOf("Câu một."), parts("Câu một.\n"))
    }

    @Test
    fun readableCollapsesWhitespaceAndNonBreakingSpaces() {
        val source = "  Một\u00A0hai   ba   bốn  "
        val range = Sentences.split(source).single()
        assertEquals("Một hai ba bốn", Sentences.readable(source, range))
        assertEquals("a b", Sentences.readable("a\nb", 0..2))
    }
}
