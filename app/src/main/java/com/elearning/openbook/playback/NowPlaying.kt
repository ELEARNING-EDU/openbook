package com.elearning.openbook.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.elearning.openbook.model.BookItem

/** What the voice is reading right now, for the Home player button. */
object NowPlaying {
    var book: BookItem? by mutableStateOf(null)
    var playing: Boolean by mutableStateOf(false)
}

/** A notification tap asks the app to open this book when it next comes to the front. */
object PendingOpen {
    var bookId: String? by mutableStateOf(null)
}
