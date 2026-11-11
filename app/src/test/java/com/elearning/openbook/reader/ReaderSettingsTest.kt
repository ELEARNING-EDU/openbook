package com.elearning.openbook.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two owner decisions that must not drift: the auto-column rule and the dark rule. */
class ReaderSettingsTest {

    private fun config(columns: Int) =
        TypeConfig.defaults(ThemeId.ORIGINAL).copy(columns = columns)

    @Test
    fun `an auto page is one column just under 560dp`() {
        assertEquals(1, config(TypeConfig.COLUMNS_AUTO).columnsFor(559f))
    }

    @Test
    fun `an auto page is two columns just over 560dp`() {
        assertEquals(2, config(TypeConfig.COLUMNS_AUTO).columnsFor(561f))
    }

    @Test
    fun `an explicit column count ignores the page width`() {
        assertEquals(2, config(2).columnsFor(320f))
        assertEquals(1, config(1).columnsFor(900f))
    }

    @Test
    fun `no saved choice follows the phone`() {
        assertTrue(readerDark(saved = null, systemDark = true))
        assertFalse(readerDark(saved = null, systemDark = false))
    }

    @Test
    fun `a saved choice beats the phone`() {
        assertFalse(readerDark(saved = false, systemDark = true))
        assertTrue(readerDark(saved = true, systemDark = false))
    }

    @Test
    fun `the default side margin is 20dp`() {
        assertEquals(20f, TypeConfig.DEFAULT_MARGIN_DP)
        assertEquals(20f, TypeConfig.defaults(ThemeId.ORIGINAL).marginDp)
    }
}
