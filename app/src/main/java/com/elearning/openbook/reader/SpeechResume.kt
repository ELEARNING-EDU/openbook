package com.elearning.openbook.reader

/** Where Play restarts after a pause. */
internal enum class ResumePlace {
    /** The paused sentence: the reader is still looking at its page. */
    IN_PLACE,

    /** The first sentence of the page on screen: the reader moved away. */
    VISIBLE_PAGE,
}

/**
 * The resume rule, in one place and with no Android in it, because both apps must answer
 * "press Play after turning away" the same way.
 *
 * A paused sentence on another page is abandoned for the page the reader can see — the
 * same start "read aloud from this page" gives. With no page on screen (the reader is
 * closed, so [visiblePage] is -1) the voice simply resumes where it was paused.
 */
internal fun resumePlace(pausedSentencePage: Int, visiblePage: Int): ResumePlace = when {
    visiblePage < 0 -> ResumePlace.IN_PLACE
    pausedSentencePage < 0 -> ResumePlace.VISIBLE_PAGE
    pausedSentencePage == visiblePage -> ResumePlace.IN_PLACE
    else -> ResumePlace.VISIBLE_PAGE
}
