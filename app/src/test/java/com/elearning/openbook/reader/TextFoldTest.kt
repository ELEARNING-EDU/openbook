package com.elearning.openbook.reader

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TextFoldTest {

    @Test
    fun foldsCaseAndDiacritics() {
        assertEquals("doc", foldText("Đọc").text)
        assertEquals("hai dang", foldText("Hải Đăng").text)
    }

    @Test
    fun foldsDecomposedVietnameseAndMapsOffsetsBack() {
        val source = "tie\u0302\u0301ng Vie\u0323\u0302t"
        val folded = foldText(source)

        assertEquals("tieng viet", folded.text)
        val at = folded.text.indexOf("tieng")
        assertEquals(0, at)
        // 't','i','e','n','g' come from source offsets 0,1,2,5,6.
        assertArrayEquals(intArrayOf(0, 1, 2, 5, 6), folded.offsets.copyOfRange(at, at + 5))
        assertEquals(source.length - 1, folded.offsets[folded.offsets.size - 1])
    }

    @Test
    fun keepsOneOffsetPerKeptCharacter() {
        val folded = foldText("a\u0301b")
        assertEquals("ab", folded.text)
        assertArrayEquals(intArrayOf(0, 2), folded.offsets)
    }
}
