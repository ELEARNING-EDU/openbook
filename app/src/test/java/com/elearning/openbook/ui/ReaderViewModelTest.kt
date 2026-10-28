package com.elearning.openbook.ui

import com.elearning.openbook.model.EpubProblem
import com.elearning.openbook.reader.ReadingPosition
import com.elearning.openbook.reader.SavedPlaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderViewModelTest {

    private class FakePlaces(private val saved: Map<String, ReadingPosition>) : SavedPlaces {
        override fun of(bookId: String): ReadingPosition? = saved[bookId]
    }

    private val saved = ReadingPosition(page = 41, pageCount = 100, chapter = 3, offset = 120)

    @Test
    fun beginLoadsTheSavedPlaceAndStartsAtTheFirstPage() {
        val viewModel = ReaderViewModel(FakePlaces(mapOf("b" to saved)))
        viewModel.begin("b")

        assertEquals(0, viewModel.state.value.pageIndex)
        assertEquals(saved, viewModel.state.value.savedPosition)
        assertNull(viewModel.state.value.failure)
    }

    @Test
    fun beginWithoutASavedPlaceLeavesItNull() {
        val viewModel = ReaderViewModel(FakePlaces(emptyMap()))
        viewModel.begin("new")
        assertNull(viewModel.state.value.savedPosition)
    }

    @Test
    fun setPagePublishesANewImmutableState() {
        val viewModel = ReaderViewModel(FakePlaces(emptyMap()))
        val before = viewModel.state.value

        viewModel.setPage(7)

        assertEquals(7, viewModel.state.value.pageIndex)
        assertEquals(0, before.pageIndex)
    }

    @Test
    fun failureCanBeSetAndCleared() {
        val viewModel = ReaderViewModel(FakePlaces(emptyMap()))
        viewModel.fail(EpubProblem.DRM)
        assertEquals(EpubProblem.DRM, viewModel.state.value.failure)

        viewModel.fail(null)
        assertNull(viewModel.state.value.failure)
    }
}
