package com.example.epubreader.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingPositionTest {

    @Test
    fun firstPageReportsItsShareOfTheBook() {
        // The bar counts the page on screen as read, so page 0 of 10 is 10%.
        assertEquals(0.1f, ReadingPosition(0, 10, 0, 0).fraction, 0.0001f)
    }

    @Test
    fun middlePageIsHalfwayThrough() {
        assertEquals(0.5f, ReadingPosition(4, 10, 0, 0).fraction, 0.0001f)
    }

    @Test
    fun lastPageIsComplete() {
        assertEquals(1f, ReadingPosition(9, 10, 1, 0).fraction, 0.0001f)
    }

    @Test
    fun unknownPageCountReportsNoProgress() {
        assertEquals(0f, ReadingPosition(0, 0, 0, 0).fraction, 0f)
    }

    @Test
    fun pageBeyondTheEndClampsToComplete() {
        assertEquals(1f, ReadingPosition(12, 10, 0, 0).fraction, 0f)
    }

    @Test
    fun negativePageClampsToZero() {
        assertEquals(0f, ReadingPosition(-1, 10, 0, 0).fraction, 0f)
    }
}
