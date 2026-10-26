package com.example.epubreader.ui

import androidx.lifecycle.ViewModel
import com.example.epubreader.model.EpubProblem
import com.example.epubreader.reader.ReadingPosition
import com.example.epubreader.reader.SavedPlaces
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** The reader's durable state: where the reader is, and why a book would not open. */
data class ReaderUiState(
    val pageIndex: Int = 0,
    val failure: EpubProblem? = null,
    val savedPosition: ReadingPosition? = null,
)

/**
 * Owns the page the reader is on and the failure that stops it, so a configuration
 * change does not lose the place. Chrome, sheets and speech remain view-local.
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val positions: SavedPlaces,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    /** Called once per book: read the place it was left at and start at page one. */
    fun begin(bookId: String) {
        _state.value = ReaderUiState(savedPosition = positions.of(bookId))
    }

    fun setPage(index: Int) {
        _state.update { it.copy(pageIndex = index) }
    }

    fun fail(problem: EpubProblem?) {
        _state.update { it.copy(failure = problem) }
    }
}
