package com.example.epubreader.ui

import androidx.lifecycle.ViewModel
import com.example.epubreader.library.ReadingLogStore
import com.example.epubreader.reader.BookmarkStore
import com.example.epubreader.reader.ReaderSettings
import com.example.epubreader.reader.ReadingPositions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The reader's stores, injected instead of rebuilt per composition. */
@HiltViewModel
class ReaderStoresViewModel @Inject constructor(
    val settings: ReaderSettings,
    val positions: ReadingPositions,
    val bookmarks: BookmarkStore,
    val readingLog: ReadingLogStore,
) : ViewModel()
