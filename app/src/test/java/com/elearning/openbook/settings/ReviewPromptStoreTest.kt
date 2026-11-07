package com.elearning.openbook.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptStoreTest {

    @Test
    fun `the first nine launches stay quiet`() {
        for (launches in 1..9) {
            assertFalse("launch $launches", ReviewPromptStore.shouldAsk(launches, asked = false))
        }
    }

    @Test
    fun `the tenth launch asks`() {
        assertEquals(10, ReviewPromptStore.LAUNCHES_BEFORE_ASK)
        assertTrue(ReviewPromptStore.shouldAsk(10, asked = false))
    }

    @Test
    fun `nothing asks once the sheet was shown`() {
        assertFalse(ReviewPromptStore.shouldAsk(10, asked = true))
        assertFalse(ReviewPromptStore.shouldAsk(40, asked = true))
    }
}
