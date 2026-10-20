package com.example.epubreader.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.epubreader.library.LibraryStore
import com.example.epubreader.model.BookItem
import com.example.epubreader.reader.ReaderSettings
import com.example.epubreader.reader.ReaderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EpubReaderApp(
    autoOpenFirstBook: Boolean = false,
    demo: DemoOptions = DemoOptions.None,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LibraryStore(context) }

    // One appearance for the whole app: the reader's moon repaints the library too,
    // because the palette every screen reads lives here.
    val settings = remember { ReaderSettings(context) }

    var books by remember { mutableStateOf<List<BookItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var opened by remember { mutableStateOf<BookItem?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            store.ensureSeeded()
            store.books()
        }
        books = loaded
        loading = false
    }

    // Automation hook, mirroring the iOS demo build: open the first book at launch.
    // Guarded, so importing a book later does not yank the shelf away.
    var autoOpenApplied by remember { mutableStateOf(false) }
    LaunchedEffect(books, autoOpenFirstBook) {
        if (autoOpenFirstBook && !autoOpenApplied && books.isNotEmpty()) {
            autoOpenApplied = true
            opened = books.first()
        }
    }

    val onPicked: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { store.import(uri) } }
                if (result.isSuccess) {
                    books = withContext(Dispatchers.IO) { store.books() }
                } else {
                    importError = result.exceptionOrNull()?.message ?: "Không nhập được sách."
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderTheme.Paper),
    ) {
        val book = opened
        if (book == null) {
            LibraryScreen(
                books = books,
                loading = loading,
                onPick = onPicked,
                onOpen = { opened = it },
            )
        } else {
            ReaderScreen(book = book, settings = settings, onBack = { opened = null }, demo = demo)
        }
    }

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            confirmButton = {
                TextButton(onClick = { importError = null }) { Text("OK") }
            },
            title = { Text("Không nhập được sách") },
            text = { Text(message) },
            containerColor = ReaderTheme.Paper,
            titleContentColor = ReaderTheme.Ink,
            textContentColor = ReaderTheme.InkSoft,
        )
    }
}
