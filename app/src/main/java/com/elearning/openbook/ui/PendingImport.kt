package com.elearning.openbook.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A document another app asked us to open. [android.app.Activity] parks the URI here and
 * the shell imports it through the normal library path, then clears it, so a second
 * delivery while the app is running is handled the same way as a cold start.
 */
object PendingImport {
    var uri: Uri? by mutableStateOf(null)
}
