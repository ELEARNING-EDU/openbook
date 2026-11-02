package com.elearning.openbook.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.openbook.library.LibraryStore
import com.elearning.openbook.library.ReadingLogStore
import com.elearning.openbook.model.BookItem
import com.elearning.openbook.reader.BookmarkStore
import com.elearning.openbook.reader.HighlightStore
import com.elearning.openbook.reader.ReadingPosition
import com.elearning.openbook.reader.ReadingPositions
import com.elearning.openbook.reader.ReadingSummary
import com.elearning.openbook.reader.WeekPoint
import com.elearning.openbook.settings.AppLanguage
import com.elearning.openbook.settings.AppLanguageStore
import com.elearning.openbook.settings.TextRepairStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Everything the shell needs to draw Home, Library, Store and the open book. */
data class AppUiState(
    val books: List<BookItem> = emptyList(),
    val reading: Map<String, ReadingPosition> = emptyMap(),
    val loading: Boolean = true,
    val opened: BookItem? = null,
    val tab: MainTab = MainTab.HOME,
    val summary: ReadingSummary = ReadingSummary(
        todaySeconds = 0,
        goalMinutes = ReadingLogStore.DEFAULT_GOAL,
        weekSeconds = 0,
        monthSeconds = 0,
        yearSeconds = 0,
        streak = 0,
        recordStreak = 0,
        readingDays = 0,
        longestSessionSeconds = 0,
        bestDaySeconds = 0,
    ),
    val week: List<WeekPoint> = emptyList(),
    val goalMinutes: Int = ReadingLogStore.DEFAULT_GOAL,
    val language: AppLanguage = AppLanguage.SYSTEM,
    /** Repair the text of an imported copy (on by default). */
    val repairText: Boolean = true,
    val importError: Throwable? = null,
    val deleteFailure: Int = 0,
    /** True until the harness's staged states have been played once. */
    val demoPending: Boolean = true,
) {
    val progress: Map<String, Float> get() = reading.mapValues { it.value.fraction }
}

/**
 * The shell's state holder: books, the open book, the tab and the reading summary are
 * immutable [AppUiState] values published on a [StateFlow]; the stores do the I/O.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val library: LibraryStore,
    private val positions: ReadingPositions,
    private val bookmarks: BookmarkStore,
    private val highlights: HighlightStore,
    private val languageStore: AppLanguageStore,
    private val repairStore: TextRepairStore,
    private val readingLogStore: ReadingLogStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AppUiState(
            goalMinutes = readingLogStore.goalMinutes,
            language = languageStore.value,
            repairText = repairStore.enabled,
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var statsDay: LocalDate = LocalDate.now()

    init {
        viewModelScope.launch {
            val loaded = withContextIo {
                library.ensureSeeded()
                // Books copied in before the importer cleaned text still need one pass.
                library.cleanLibrary()
                library.books()
            }
            _state.update {
                it.copy(
                    books = loaded,
                    // A first run with an empty shelf opens the Library, where importing lives.
                    tab = if (loaded.isEmpty()) MainTab.LIBRARY else it.tab,
                    loading = false,
                )
            }
            refreshReading()
            refreshStats()
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (LocalDate.now() != statsDay) {
                    statsDay = LocalDate.now()
                    refreshStats()
                }
            }
        }
    }

    fun selectTab(tab: MainTab) = _state.update { it.copy(tab = tab) }

    fun open(book: BookItem) = _state.update { it.copy(opened = book) }

    /** Closing a book re-reads the place it was left at and the day's reading time. */
    fun closeBook() {
        _state.update { it.copy(opened = null) }
        viewModelScope.launch {
            refreshReading()
            refreshStats()
        }
    }

    fun useDemo() = _state.update { it.copy(demoPending = false) }

    /** Re-read the ledger after the reader leaves composition (its last save lands on dispose). */
    fun refreshAfterReader() = refreshStats()

    fun import(uri: Uri) {
        viewModelScope.launch {
            val result = withContextIo { runCatching { library.import(uri) } }
            result.exceptionOrNull()?.let { error ->
                _state.update { it.copy(importError = error) }
                return@launch
            }
            val books = withContextIo { library.books() }
            _state.update { it.copy(books = books) }
        }
    }

    fun dismissImportError() = _state.update { it.copy(importError = null) }

    fun dismissDeleteFailure() = _state.update { it.copy(deleteFailure = 0) }

    fun markFinished(book: BookItem, finished: Boolean) {
        viewModelScope.launch {
            withContextIo { library.markFinished(book.id, finished) }
            val books = withContextIo { library.books() }
            _state.update { it.copy(books = books) }
        }
    }

    fun updateMetadata(book: BookItem, title: String, author: String) {
        viewModelScope.launch {
            withContextIo { library.updateMetadata(book.id, title, author) }
            val books = withContextIo { library.books() }
            _state.update { it.copy(books = books) }
        }
    }

    fun deleteBooks(ids: List<String>) {
        viewModelScope.launch {
            val report = withContextIo { library.delete(ids) }
            for (id in report.deleted) {
                positions.remove(id)
                bookmarks.erase(id)
                // A same-name reimport must start without the old book's highlights.
                highlights.erase(id)
            }
            val books = withContextIo { library.books() }
            _state.update {
                it.copy(
                    books = books,
                    // An empty shelf always lands on Library, where importing lives.
                    tab = if (books.isEmpty()) MainTab.LIBRARY else it.tab,
                    deleteFailure = if (report.failed.isNotEmpty()) report.failed.size
                    else it.deleteFailure,
                )
            }
            refreshReading()
        }
    }

    fun setRepairText(enabled: Boolean) {
        repairStore.enabled = enabled
        _state.update { it.copy(repairText = enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        languageStore.set(language)
        _state.update { it.copy(language = language) }
    }

    fun setGoal(minutes: Int) {
        readingLogStore.goalMinutes = minutes
        _state.update { it.copy(goalMinutes = minutes) }
        refreshStats()
    }

    /** Re-reads the day's ledger; called after the reader closes and at midnight. */
    fun refreshStats() {
        viewModelScope.launch {
            val log = withContextIo { readingLogStore.load() }
            val today = LocalDate.now()
            val summary = log.summary(today, readingLogStore.goalMinutes)
            val week = log.week(today)
            _state.update { it.copy(summary = summary, week = week) }
        }
    }

    private suspend fun refreshReading() {
        val reading = withContextIo { positions.allPositions() }
        _state.update { it.copy(reading = reading) }
    }

    private suspend fun <T> withContextIo(block: suspend () -> T): T =
        kotlinx.coroutines.withContext(Dispatchers.IO) { block() }
}
