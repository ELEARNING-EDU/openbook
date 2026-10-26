package com.example.epubreader.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.epubreader.R
import com.example.epubreader.model.BookItem
import com.example.epubreader.reader.ReadingPosition
import kotlin.math.roundToInt

/** The three places the app starts from. */
enum class MainTab(val labelRes: Int) {
    HOME(R.string.tab_home),
    LIBRARY(R.string.tab_library),
    STORE(R.string.tab_store),
}

private val TAB_BAR_HEIGHT = 56.dp
private val TAB_BAR_MARGIN = 18.dp
private val TAB_BAR_GAP = 4.dp

/** Room the floating bar keeps free at the foot of every scrolling screen. */
private val TAB_CONTENT_RESERVE = 88.dp

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
    reading: Map<String, ReadingPosition>,
    loading: Boolean,
    /** Owned above this screen, so backing out of a book returns to the tab it opened from. */
    tab: MainTab,
    onTab: (MainTab) -> Unit,
    onPick: (Uri?) -> Unit,
    onOpen: (BookItem) -> Unit,
    onAppSettings: () -> Unit,
    onEditBook: (BookItem, String, String) -> Unit,
    onMarkFinished: (BookItem, Boolean) -> Unit,
    onDeleteBooks: (List<String>) -> Unit,
    summary: com.example.epubreader.reader.ReadingSummary,
    week: List<com.example.epubreader.reader.WeekPoint>,
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

    val colors = shellColors()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = TAB_CONTENT_RESERVE + navBottom),
        ) {
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    books = books,
                    reading = reading,
                    summary = summary,
                    week = week,
                    loading = loading,
                    onOpen = onOpen,
                    onImport = import,
                    onBrowse = { onTab(MainTab.LIBRARY) },
                    onAppSettings = onAppSettings,
                )

                MainTab.LIBRARY -> LibraryScreen(
                    books = books,
                    progress = progress,
                    reading = reading,
                    loading = loading,
                    onImport = import,
                    onOpen = onOpen,
                    onEditBook = onEditBook,
                    onMarkFinished = onMarkFinished,
                    onDeleteBooks = onDeleteBooks,
                )

                MainTab.STORE -> StoreScreen(onImport = import)
            }
        }

        FloatingTabBar(
            selected = tab,
            onSelect = onTab,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = TAB_BAR_MARGIN,
                    end = TAB_BAR_MARGIN,
                    bottom = navBottom + TAB_BAR_GAP,
                ),
        )
    }
}

/**
 * The floating tab bar: a compact capsule lifted off the bottom edge, leading-aligned so
 * the room beside it stays free for a listening control later. The chosen tab keeps its
 * name; the other two are icons.
 */
@Composable
private fun FloatingTabBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = shellColors()
    Row(
        modifier = modifier
            .height(TAB_BAR_HEIGHT)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.raised)
            .border(1.dp, colors.hairline, CircleShape)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (item in MainTab.entries) {
            val active = item == selected
            val label = stringResource(item.labelRes)
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) colors.accent.copy(alpha = if (colors.dark) 0.20f else 0.11f)
                        else ComposeColor.Transparent,
                    )
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(item) })
                    .semantics { contentDescription = label }
                    .padding(horizontal = if (active) 12.dp else 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                val tint = if (active) colors.accent else colors.inkSoft
                when (item) {
                    MainTab.HOME -> HomeGlyph(color = tint)
                    MainTab.LIBRARY -> LibraryGlyph(color = tint)
                    MainTab.STORE -> StoreGlyph(color = tint)
                }
                if (active) {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    val colors = shellColors()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (colors.dark) colors.paper else ComposeColor.White,
        )
    }
}

@Composable
private fun EmptyHome(onImport: () -> Unit) {
    val colors = shellColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = colors.ink,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_empty_body),
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        PrimaryButton(label = stringResource(R.string.import_epub), onClick = onImport)
    }
}

/**
 * Store, honestly and clearly labelled: this build has no network permission, so there
 * is no catalogue to show. It says so, and offers the one thing a store would have given
 * the reader — a way to put a book on the shelf.
 */
@Composable
private fun StoreScreen(onImport: () -> Unit) {
    val colors = shellColors()
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.store_demo_badge),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accent.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = colors.accent,
            )
        }
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
                    .background(colors.raised),
                contentAlignment = Alignment.Center,
            ) {
                StoreGlyph(color = colors.accent)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.store_demo_title),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.store_demo_body),
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = colors.inkSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(label = stringResource(R.string.import_epub), onClick = onImport)
        }
    }
}

@Composable
private fun CenteredNote(message: String) {
    val colors = shellColors()
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp,
        color = colors.inkSoft,
        textAlign = TextAlign.Center,
    )
}
