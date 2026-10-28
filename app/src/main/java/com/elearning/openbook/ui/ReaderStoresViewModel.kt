package com.elearning.openbook.ui

import androidx.lifecycle.ViewModel
import com.elearning.openbook.library.ReadingLogStore
import com.elearning.openbook.reader.BookmarkStore
import com.elearning.openbook.reader.HighlightStore
import com.elearning.openbook.reader.ReaderSettings
import com.elearning.openbook.reader.ReadingPositions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The reader's stores, injected instead of rebuilt per composition. */
@HiltViewModel
class ReaderStoresViewModel @Inject constructor(
    val settings: ReaderSettings,
    val positions: ReadingPositions,
    val bookmarks: BookmarkStore,
    val highlights: HighlightStore,
    val readingLog: ReadingLogStore,
) : ViewModel()
