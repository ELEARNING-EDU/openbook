package com.example.epubreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import com.example.epubreader.model.BookItem
import com.example.epubreader.reader.ReaderTheme
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(
    books: List<BookItem>,
    /** How far through each book the reader has got, by book id. */
    progress: Map<String, Float>,
    loading: Boolean,
    /** The activity-result picker lives in the main screen, which every tab shares. */
    onImport: () -> Unit,
    onOpen: (BookItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderTheme.Paper),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Thư viện",
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp,
                    color = ReaderTheme.Ink,
                )
                CircleButton(
                    onClick = onImport,
                ) { PlusGlyph() }
            }

            when {
                loading -> CenteredMessage("Đang tải thư viện…")
                books.isEmpty() -> EmptyLibrary()
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 118.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(books.size, key = { books[it].id }) { index ->
                        val book = books[index]
                        BookTile(
                            book = book,
                            progress = progress[book.id],
                            modifier = Modifier.clickable { onOpen(book) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            fontFamily = FontFamily.Serif,
            fontSize = 15.sp,
            color = ReaderTheme.InkSoft,
        )
    }
}

@Composable
private fun EmptyLibrary() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp),
        ) {
            Text(
                text = "Chưa có sách nào",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = ReaderTheme.Ink,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Nhấn nút ＋ ở góc trên để nhập một tệp EPUB.",
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = ReaderTheme.InkSoft,
            )
        }
    }
}

/** A cover, and under it how far through the book the reader has got. */
@Composable
private fun BookTile(book: BookItem, progress: Float?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        BookCover(book = book, modifier = Modifier.fillMaxWidth())
        if (progress != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReadBar(fraction = progress, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(progress * 100f).roundToInt()}%",
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    color = ReaderTheme.InkSoft,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The shelf's progress bar: how much of the book has been read. It is the reading
 * position, not the voice's place — the reader's own page, out of the book's pages.
 */
@Composable
fun ReadBar(fraction: Float, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(pill)
            .background(ReaderTheme.Ink.copy(alpha = 0.10f)),
    ) {
        val read = fraction.coerceIn(0f, 1f)
        if (read > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(read)
                    .fillMaxHeight()
                    .clip(pill)
                    .background(ReaderTheme.Accent),
            )
        }
    }
}
