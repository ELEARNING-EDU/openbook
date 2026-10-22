package com.example.epubreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.epubreader.reader.ReaderEngine
import com.example.epubreader.reader.ReaderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Full-text search over the open book, as the contents of a bottom sheet.
 *
 * iOS shows this as a sheet over the page, and so does the reader now: the book stays
 * visible underneath, and dismissing the sheet is one gesture back to the page.
 * Every hit already knows its page, so picking one is a jump rather than a second
 * lookup.
 */
@Composable
fun SearchSheet(
    engine: ReaderEngine,
    initialQuery: String,
    onPick: (Int) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    val focus = remember { FocusRequester() }

    // The scan is cheap on this book, but it runs off the main thread all the same: a
    // keystroke must never wait behind a chapter.
    val hits by produceState(initialValue = emptyList<ReaderEngine.Hit>(), query) {
        val trimmed = query.trim()
        value = if (trimmed.length < 2) {
            emptyList()
        } else {
            delay(120)
            withContext(Dispatchers.Default) { engine.search(trimmed) }.also { hits ->
                android.util.Log.i("SearchSheet", "search \u201c$trimmed\u201d -> ${hits.size} hit(s)")
            }
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    val ink = ReaderTheme.Ink
    val inkSoft = ReaderTheme.InkSoft

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // The sheet is the top of this screen, so it carries the keyboard's inset:
            // the field must stay above the keys while the results scroll behind them.
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tìm trong sách",
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ink,
            )
            Text(
                text = "Xong",
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = ReaderTheme.Accent,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ReaderTheme.Ink.copy(alpha = 0.06f))
                .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchGlyph(color = inkSoft)
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Tìm trong sách",
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = inkSoft.copy(alpha = 0.7f),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = ink,
                    ),
                    cursorBrush = SolidColor(ReaderTheme.Accent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { query = "" },
                    contentAlignment = Alignment.Center,
                ) {
                    CloseGlyph(color = inkSoft)
                }
            }
        }

        when {
            query.trim().length < 2 -> Hint("Nhập ít nhất hai chữ để tìm trong sách.")
            hits.isEmpty() -> Hint("Không tìm thấy “${query.trim()}”.")
            else -> {
                // Kept although iOS has no such line: a count is the one thing a sheet
                // over the page can say that a result list cannot — how much of the book
                // the phrase covers, before scrolling it.
                Text(
                    text = "${hits.size} chỗ",
                    modifier = Modifier.padding(start = 22.dp, top = 4.dp, bottom = 6.dp),
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = inkSoft,
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(hits.size) { index ->
                        val hit = hits[index]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(hit.pageIndex) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = hit.chapterTitle,
                                    modifier = Modifier.weight(1f),
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = ReaderTheme.Accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "tr. ${hit.pageIndex + 1}",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 11.sp,
                                    color = inkSoft,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = hit.snippet,
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp,
                                color = ink,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp,
        color = ReaderTheme.InkSoft,
        textAlign = TextAlign.Start,
    )
}
