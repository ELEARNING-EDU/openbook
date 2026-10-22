package com.example.epubreader.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.epubreader.model.BookItem
import com.example.epubreader.reader.ReaderTheme
import kotlin.math.roundToInt

/** The three places the app starts from. */
enum class MainTab(val label: String) {
    HOME("Home"),
    LIBRARY("Library"),
    STORE("Store"),
}

private val TAB_BAR_HEIGHT = 62.dp

/**
 * The app's front door: three tabs over one shared import picker.
 *
 * The picker lives here rather than in [LibraryScreen] because both Home and Store offer
 * a way in for a new book; one activity-result launcher for the whole screen keeps them
 * from registering three.
 */
@Composable
fun MainScreen(
    books: List<BookItem>,
    progress: Map<String, Float>,
    loading: Boolean,
    /** The book the reader was last in this process, so Home can offer to continue it. */
    lastOpenedId: String?,
    /** Owned above this screen, so backing out of a book returns to the tab it opened from. */
    tab: MainTab,
    onTab: (MainTab) -> Unit,
    onPick: (Uri?) -> Unit,
    onOpen: (BookItem) -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> onPick(uri) }

    val import: () -> Unit = {
        runCatching {
            picker.launch(
                arrayOf(
                    "application/epub+zip",
                    "application/octet-stream",
                    "*/*",
                ),
            )
        }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(ReaderTheme.Paper)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = TAB_BAR_HEIGHT + navBottom),
        ) {
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    books = books,
                    progress = progress,
                    loading = loading,
                    continuing = books.firstOrNull { it.id == lastOpenedId }
                        ?: books.maxByOrNull { progress[it.id] ?: 0f },
                    onOpen = onOpen,
                    onImport = import,
                )

                MainTab.LIBRARY -> LibraryScreen(
                    books = books,
                    progress = progress,
                    loading = loading,
                    onImport = import,
                    onOpen = onOpen,
                )

                MainTab.STORE -> StoreScreen(onImport = import)
            }
        }

        MainTabBar(
            selected = tab,
            onSelect = onTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The bar itself: three hand-drawn icons, the same language as the reader's chrome. */
@Composable
private fun MainTabBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ReaderTheme.Paper)
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ReaderTheme.Ink.copy(alpha = 0.12f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TAB_BAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in MainTab.entries) {
                val active = item == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSelect(item) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val tint = if (active) ReaderTheme.Accent else ReaderTheme.InkSoft
                    when (item) {
                        MainTab.HOME -> HomeGlyph(color = tint)
                        MainTab.LIBRARY -> LibraryGlyph(color = tint)
                        MainTab.STORE -> StoreGlyph(color = tint)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 11.sp,
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Home: what to carry on with, then the shelf.
 *
 * There is no feed to build and nothing to fetch, so the useful answer to "what now?"
 * is the one book already under way and the rest of the shelf one tap away.
 */
@Composable
private fun HomeScreen(
    books: List<BookItem>,
    progress: Map<String, Float>,
    loading: Boolean,
    continuing: BookItem?,
    onOpen: (BookItem) -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Text(
            text = "Trang chủ",
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 14.dp),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 27.sp,
            color = ReaderTheme.Ink,
        )

        when {
            loading -> CenteredNote("Đang tải thư viện…")
            books.isEmpty() -> EmptyHome(onImport = onImport)
            else -> {
                if (continuing != null) {
                    Text(
                        text = "ĐANG ĐỌC",
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = ReaderTheme.InkSoft,
                    )
                    ContinueCard(
                        book = continuing,
                        progress = progress[continuing.id],
                        onOpen = { onOpen(continuing) },
                    )
                    Spacer(modifier = Modifier.height(26.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Thư viện của bạn",
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ReaderTheme.Ink,
                    )
                    Text(
                        text = "${books.size} cuốn",
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = ReaderTheme.InkSoft,
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(books.size) { index ->
                        val book = books[index]
                        Column(
                            modifier = Modifier
                                .width(112.dp)
                                .clickable { onOpen(book) },
                        ) {
                            BookCover(book = book, modifier = Modifier.fillMaxWidth())
                            val read = progress[book.id]
                            if (read != null) {
                                Spacer(modifier = Modifier.height(7.dp))
                                ReadBar(fraction = read)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${(read * 100f).roundToInt()}%",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 11.sp,
                                    color = ReaderTheme.InkSoft,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/** The book under way: its cover, how far in it is, and one way back into it. */
@Composable
private fun ContinueCard(
    book: BookItem,
    progress: Float?,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book = book, modifier = Modifier.width(96.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                color = ReaderTheme.Ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = book.author,
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = ReaderTheme.InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (progress != null) {
                ReadBar(fraction = progress)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Đã đọc ${(progress * 100f).roundToInt()}%",
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = ReaderTheme.InkSoft,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            PrimaryButton(label = "Đọc tiếp", onClick = onOpen)
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ReaderTheme.Accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = ComposeColor.White,
        )
    }
}

@Composable
private fun EmptyHome(onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
            text = "Nhập một tệp EPUB để bắt đầu đọc.",
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            color = ReaderTheme.InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        PrimaryButton(label = "Nhập EPUB", onClick = onImport)
    }
}

/**
 * Store, honestly: this build has no network permission, so there is no catalogue to
 * show. It says so, and offers the one thing a store would have given the reader — a
 * way to put a book on the shelf.
 */
@Composable
private fun StoreScreen(onImport: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Text(
            text = "Cửa hàng",
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 14.dp),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 27.sp,
            color = ReaderTheme.Ink,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ReaderTheme.Raised),
                contentAlignment = Alignment.Center,
            ) {
                StoreGlyph(color = ReaderTheme.Accent)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Cửa hàng chưa kết nối",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = ReaderTheme.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Bản này không dùng mạng, nên chưa có sách để tải về. " +
                    "Sách EPUB trong máy vẫn nhập được vào thư viện.",
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = ReaderTheme.InkSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(label = "Nhập EPUB", onClick = onImport)
        }
    }
}

@Composable
private fun CenteredNote(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp,
        color = ReaderTheme.InkSoft,
        textAlign = TextAlign.Center,
    )
}
