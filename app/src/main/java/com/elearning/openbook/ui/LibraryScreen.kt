package com.elearning.openbook.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.elearning.openbook.R
import com.elearning.openbook.library.BookSorting
import com.elearning.openbook.library.LibraryPreferences
import com.elearning.openbook.library.LibrarySelection
import com.elearning.openbook.library.LibrarySort
import com.elearning.openbook.library.LibraryView
import com.elearning.openbook.model.BookItem
import com.elearning.openbook.reader.ReaderTheme
import kotlin.math.roundToInt

/**
 * The shelf: every book as a cover or a row, ordered the way the reader asked, with the
 * one place a book can be edited or removed.
 *
 * Progress is a number here, never a bar: the percentage is the fact, and the bar was
 * only ever decoration on top of it.
 */
@Composable
fun LibraryScreen(
    books: List<BookItem>,
    /** How far through each book the reader has got, by book id. */
    progress: Map<String, Float>,
    /** Page number and page count per book, for the list's "Page n / m" line. */
    reading: Map<String, com.elearning.openbook.reader.ReadingPosition>,
    loading: Boolean,
    /** The activity-result picker lives in the main screen, which every tab shares. */
    onImport: () -> Unit,
    onOpen: (BookItem) -> Unit,
    onEditBook: (BookItem, String, String) -> Unit,
    onMarkFinished: (BookItem, Boolean) -> Unit,
    onDeleteBooks: (List<String>) -> Unit,
) {
    val colors = shellColors()
    val context = LocalContext.current
    val preferences = remember { LibraryPreferences(context) }
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    var view by remember { mutableStateOf(preferences.view) }
    var sort by remember { mutableStateOf(preferences.sort) }
    var selection by remember { mutableStateOf(LibrarySelection()) }
    var selecting by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BookItem?>(null) }
    var pendingDelete by remember { mutableStateOf<List<String>?>(null) }

    val importLabel = stringResource(R.string.import_epub)
    val menuLabel = stringResource(R.string.library_options)

    val ordered = remember(books, sort, locale) {
        BookSorting.sorted(books, sort, locale)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_title),
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                color = colors.ink,
                maxLines = 1,
            )
            if (!selecting) {
                Box {
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.raised)
                            .border(1.dp, colors.hairline, RoundedCornerShape(22.dp))
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconHit(onClick = { showMenu = true }, description = menuLabel) {
                            MoreGlyph(color = colors.ink)
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(colors.hairline),
                        )
                        IconHit(onClick = onImport, description = importLabel) {
                            PlusGlyph(color = colors.ink)
                        }
                    }
                    LibraryMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        view = view,
                        sort = sort,
                        canSelect = books.isNotEmpty(),
                        onView = {
                            view = it
                            preferences.setView(it)
                            showMenu = false
                        },
                        onSort = {
                            sort = it
                            preferences.setSort(it)
                            showMenu = false
                        },
                        onSelect = {
                            showMenu = false
                            selecting = true
                            selection = LibrarySelection()
                        },
                    )
                }
            }
        }

        if (selecting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 18.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.selected_count,
                        selection.count,
                        selection.count,
                    ),
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.ink,
                )
                TextButton(
                    onClick = {
                        selecting = false
                        selection = selection.clear()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.inkSoft),
                ) {
                    Text(stringResource(R.string.selection_cancel), fontFamily = FontFamily.Serif, fontSize = 13.sp)
                }
                TextButton(
                    onClick = { pendingDelete = selection.forExisting(books.map { it.id }) },
                    enabled = selection.count > 0,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.danger),
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        when {
            loading -> CenteredMessage(stringResource(R.string.loading_library), colors.inkSoft)
            books.isEmpty() -> EmptyLibrary(onImport = onImport)
            else -> when (view) {
                LibraryView.GRID -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(ordered.size, key = { ordered[it].id }) { index ->
                        val book = ordered[index]
                        GridBook(
                            book = book,
                            progress = progress[book.id],
                            selecting = selecting,
                            selected = selection.contains(book.id),
                            onOpen = {
                                if (selecting) selection = selection.toggle(book.id) else onOpen(book)
                            },
                            onEdit = { editing = book },
                            onMarkFinished = {
                                onMarkFinished(book, book.finishedAt == null)
                            },
                            onDelete = { pendingDelete = listOf(book.id) },
                        )
                    }
                }

                LibraryView.LIST -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 22.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(ordered.size, key = { ordered[it].id }) { index ->
                        val book = ordered[index]
                        ListBook(
                            book = book,
                            progress = progress[book.id],
                            position = reading[book.id],
                            selecting = selecting,
                            selected = selection.contains(book.id),
                            onOpen = {
                                if (selecting) selection = selection.toggle(book.id) else onOpen(book)
                            },
                            onEdit = { editing = book },
                            onMarkFinished = {
                                onMarkFinished(book, book.finishedAt == null)
                            },
                            onDelete = { pendingDelete = listOf(book.id) },
                        )
                    }
                }
            }
        }
    }

    editing?.let { book ->
        EditBookDialog(
            book = book,
            onDismiss = { editing = null },
            onSave = { title, author ->
                editing = null
                onEditBook(book, title, author)
            },
        )
    }

    pendingDelete?.let { ids ->
        DeleteDialog(
            count = ids.size,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                selecting = false
                selection = selection.clear()
                onDeleteBooks(ids)
            },
        )
    }
}

private fun LibrarySort.labelRes(): Int = when (this) {
    LibrarySort.RECENT -> R.string.sort_recent
    LibrarySort.TITLE -> R.string.sort_title
    LibrarySort.AUTHOR -> R.string.sort_author
}

/**
 * Every shelf control in one place: view mode, order, and selection. The button that
 * opens it sits beside the import button, so the shelf keeps one control cluster.
 */
@Composable
private fun LibraryMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    view: LibraryView,
    sort: LibrarySort,
    canSelect: Boolean,
    onView: (LibraryView) -> Unit,
    onSort: (LibrarySort) -> Unit,
    onSelect: () -> Unit,
) {
    val colors = shellColors()
    BubbleMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        anchorHeight = 44.dp,
        width = 220.dp,
    ) {
        for (option in LibraryView.entries) {
            BubbleRow(
                label = stringResource(
                    if (option == LibraryView.GRID) R.string.library_view_grid
                    else R.string.library_view_list,
                ),
                active = option == view,
                onClick = { onView(option) },
            ) {
                if (option == LibraryView.GRID) GridGlyph(color = colors.ink)
                else ListGlyph(color = colors.ink)
            }
        }
        BubbleDivider()
        for (option in LibrarySort.entries) {
            BubbleRow(
                label = stringResource(option.labelRes()),
                active = option == sort,
                onClick = { onSort(option) },
            ) {
                when (option) {
                    LibrarySort.RECENT -> ClockGlyph(color = colors.ink)
                    LibrarySort.TITLE -> Text(
                        text = "A",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.ink,
                    )
                    LibrarySort.AUTHOR -> PersonGlyph(color = colors.ink)
                }
            }
        }
        if (canSelect) {
            BubbleDivider()
            BubbleRow(
                label = stringResource(R.string.action_select),
                active = false,
                onClick = onSelect,
            ) {
                CheckGlyph(color = colors.ink)
            }
        }
    }
}

/** A bare icon target: no circle, no fill, just the glyph and its hit area. */
@Composable
private fun IconHit(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/**
 * A rounded popup with a tail pointing up at the button that opened it. It replaces
 * Material's square drop-down so the shelf's two menus read as one speech bubble.
 */
@Composable
private fun BubbleMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    anchorHeight: Dp,
    width: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val colors = shellColors()
    val density = LocalDensity.current
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, with(density) { (anchorHeight + 4.dp).roundToPx() }),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box {
            Column(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .width(width)
                    .shadow(10.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.raised)
                    .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
                    .padding(vertical = 6.dp),
                content = content,
            )
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp)
                    .size(16.dp, 8.dp),
            ) {
                val tail = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(tail, colors.raised)
            }
        }
    }
}

@Composable
private fun BubbleRow(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = shellColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = when {
                danger -> colors.danger
                active -> colors.accent
                else -> colors.ink
            },
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (active) {
            Spacer(modifier = Modifier.width(10.dp))
            CheckGlyph(color = colors.accent)
        }
    }
}

@Composable
private fun BubbleDivider() {
    val colors = shellColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(1.dp)
            .background(colors.hairline),
    )
}

@Composable
private fun GridBook(
    book: BookItem,
    progress: Float?,
    selecting: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onMarkFinished: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = shellColors()
    val spoken = stringResource(R.string.home_book_a11y, book.title, book.author)
    Column(
        modifier = Modifier
            .clickable(onClick = onOpen)
            .semantics { contentDescription = spoken },
    ) {
        Box {
            BookCover(book = book, modifier = Modifier.fillMaxWidth())
            if (selecting) {
                SelectionBadge(
                    selected = selected,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    book.finishedAt != null -> stringResource(R.string.library_finished)
                    progress != null -> stringResource(
                        R.string.percent_read,
                        (progress * 100f).roundToInt(),
                    )
                    else -> ""
                },
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Serif,
                fontWeight = if (book.finishedAt != null) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp,
                color = if (book.finishedAt != null) colors.accent else colors.inkSoft,
                maxLines = 1,
            )
            if (!selecting) {
                BookActions(
                    finished = book.finishedAt != null,
                    onToggleFinished = onMarkFinished,
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun ListBook(
    book: BookItem,
    progress: Float?,
    position: com.elearning.openbook.reader.ReadingPosition?,
    selecting: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onMarkFinished: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = shellColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book = book, modifier = Modifier.width(64.dp), showText = false)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = book.author,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = colors.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val progressLine = when {
                book.finishedAt != null -> stringResource(R.string.library_finished)
                position != null && position.pageCount > 0 -> stringResource(
                    R.string.page_of_pages,
                    position.page + 1,
                    position.pageCount,
                ) + " · " + stringResource(
                    R.string.percent_read,
                    (position.fraction * 100f).roundToInt(),
                )
                progress != null -> stringResource(
                    R.string.percent_read,
                    (progress * 100f).roundToInt(),
                )
                else -> null
            }
            if (progressLine != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = progressLine,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (book.finishedAt != null) FontWeight.SemiBold
                    else FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (book.finishedAt != null) colors.accent else colors.inkSoft,
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        if (selecting) {
            SelectionBadge(selected = selected)
        } else {
            BookActions(
                finished = book.finishedAt != null,
                onToggleFinished = onMarkFinished,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }
    }
}

/** The tick beside a book while the shelf is in selection mode. */
@Composable
private fun SelectionBadge(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = shellColors()
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accent else colors.paper.copy(alpha = 0.85f))
            .border(1.dp, if (selected) colors.accent else colors.hairline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) CheckGlyph(color = if (colors.dark) colors.paper else ComposeColor.White)
    }
}

@Composable
private fun BookActions(
    finished: Boolean,
    onToggleFinished: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = shellColors()
    var open by remember { mutableStateOf(false) }
    val label = stringResource(R.string.library_menu)
    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .semantics { contentDescription = label }
                .clickable(role = Role.Button) { open = true },
            contentAlignment = Alignment.Center,
        ) {
            MoreGlyph(color = colors.ink)
        }
        BubbleMenu(
            expanded = open,
            onDismiss = { open = false },
            anchorHeight = 40.dp,
            width = 170.dp,
        ) {
            BubbleRow(
                label = stringResource(
                    if (finished) R.string.library_unmark_finished
                    else R.string.library_mark_finished,
                ),
                active = false,
                onClick = {
                    open = false
                    onToggleFinished()
                },
            ) {
                CheckGlyph(color = colors.ink)
            }
            BubbleRow(
                label = stringResource(R.string.action_edit),
                active = false,
                onClick = {
                    open = false
                    onEdit()
                },
            ) {
                EditGlyph(color = colors.ink)
            }
            BubbleRow(
                label = stringResource(R.string.action_delete),
                active = false,
                danger = true,
                onClick = {
                    open = false
                    onDelete()
                },
            ) {
                TrashGlyph(color = if (colors.dark) colors.accent else colors.ink)
            }
        }
    }
}

@Composable
private fun EditBookDialog(
    book: BookItem,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val colors = shellColors()
    var title by remember(book.id) { mutableStateOf(book.title) }
    var author by remember(book.id) { mutableStateOf(book.author) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.ink,
        unfocusedTextColor = colors.ink,
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.hairline,
        cursorColor = colors.accent,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.inkSoft,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.raised,
        titleContentColor = colors.ink,
        textContentColor = colors.inkSoft,
        title = { Text(stringResource(R.string.edit_book_title), fontFamily = FontFamily.Serif) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.field_author)) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, author) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.accent),
            ) {
                Text(stringResource(R.string.action_save), fontFamily = FontFamily.Serif)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.inkSoft),
            ) {
                Text(stringResource(R.string.action_cancel), fontFamily = FontFamily.Serif)
            }
        },
    )
}

@Composable
private fun DeleteDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = shellColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.raised,
        titleContentColor = colors.ink,
        textContentColor = colors.inkSoft,
        title = {
            Text(
                text = if (count == 1) {
                    stringResource(R.string.delete_book_title)
                } else {
                    pluralStringResource(R.plurals.delete_books_title, count, count)
                },
                fontFamily = FontFamily.Serif,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_confirm_body),
                fontFamily = FontFamily.Serif,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.danger),
            ) {
                Text(
                    text = stringResource(R.string.action_delete),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.inkSoft),
            ) {
                Text(stringResource(R.string.action_cancel), fontFamily = FontFamily.Serif)
            }
        },
    )
}

@Composable
private fun CenteredMessage(message: String, color: ComposeColor) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            fontFamily = FontFamily.Serif,
            fontSize = 15.sp,
            color = color,
        )
    }
}

@Composable
private fun EmptyLibrary(onImport: () -> Unit) {
    val colors = shellColors()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp),
        ) {
            Text(
                text = stringResource(R.string.library_empty_title),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.library_empty_body),
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = colors.inkSoft,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.accent)
                    .clickable(onClick = onImport)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.import_epub),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (colors.dark) colors.paper else ComposeColor.White,
                )
            }
        }
    }
}

/**
 * The shelf's progress bar: how much of the book has been read. It is the reading
 * position, not the voice's place — the reader's own page, out of the book's pages.
 *
 * It lives on Home's "carry on" cards. The shelf itself shows only the percentage, so
 * the bar's colours are parameters rather than the reader's palette.
 */
@Composable
fun ReadBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    track: ComposeColor = ReaderTheme.Ink.copy(alpha = 0.10f),
    fill: ComposeColor = ReaderTheme.Accent,
) {
    val pill = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(pill)
            .background(track),
    ) {
        val read = fraction.coerceIn(0f, 1f)
        if (read > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(read)
                    .fillMaxHeight()
                    .clip(pill)
                    .background(fill),
            )
        }
    }
}
