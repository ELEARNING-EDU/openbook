package com.elearning.openbook.reader

import org.junit.Assert.assertEquals
import org.junit.Test

/** Play after a pause follows the page on screen; with no reader open it stays put. */
class SpeechResumeTest {

    @Test
    fun `a forward page turn resumes at the page now on screen`() {
        assertEquals(ResumePlace.VISIBLE_PAGE, resumePlace(pausedSentencePage = 2, visiblePage = 7))
    }

    @Test
    fun `a backward page turn resumes at the page now on screen`() {
        assertEquals(ResumePlace.VISIBLE_PAGE, resumePlace(pausedSentencePage = 7, visiblePage = 2))
    }

    @Test
    fun `a page that still holds the paused sentence resumes it in place`() {
        assertEquals(ResumePlace.IN_PLACE, resumePlace(pausedSentencePage = 5, visiblePage = 5))
    }

    @Test
    fun `with the reader closed the voice resumes where it paused`() {
        assertEquals(ResumePlace.IN_PLACE, resumePlace(pausedSentencePage = 5, visiblePage = -1))
    }

    @Test
    fun `an unknown paused page still follows the visible page`() {
        assertEquals(ResumePlace.VISIBLE_PAGE, resumePlace(pausedSentencePage = -1, visiblePage = 4))
    }
}
