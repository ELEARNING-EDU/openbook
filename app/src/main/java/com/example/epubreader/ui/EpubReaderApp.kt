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
import com.example.epubreader.reader.ReadingPositions
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

    // One book of places for the whole app: the reader writes it, the shelf reads it.
    val positions = remember { ReadingPositions(context) }

    var books by remember { mutableStateOf<List<BookItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var opened by remember { mutableStateOf<BookItem?>(null) }
    /** Home's "carry on" card: the last book opened in this process, if any. */
    var lastOpenedId by remember { mutableStateOf<String?>(null) }
    /** Which tab the shelf was left on; the reader covers it and comes back to it. */
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var importError by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }

    // The shelf's bars are re-read whenever the reader is closed, so the page the reader
    // has just been on is on the shelf by the time they see it.
    LaunchedEffect(opened) {
        if (opened == null) progress = withContext(Dispatchers.IO) { positions.all() }
    }

    // The harness's staged states are spent once per launch. The reader screen is built
    // afresh every time a book is opened, so a flag kept there would replay the whole
    // staging — including starting the voice — on the reader's next visit.
    var demoPending by remember { mutableStateOf(demo.isActive) }

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
            MainScreen(
                books = books,
                progress = progress,
                loading = loading,
                lastOpenedId = lastOpenedId,
                tab = tab,
                onTab = { tab = it },
                onPick = onPicked,
                onOpen = { picked ->
                    lastOpenedId = picked.id
                    opened = picked
                },
            )
        } else {
            ReaderScreen(
                book = book,
                settings = settings,
                positions = positions,
                onBack = { opened = null },
                demo = demo,
                demoPending = demoPending,
                onDemoUsed = { demoPending = false },
            )
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
