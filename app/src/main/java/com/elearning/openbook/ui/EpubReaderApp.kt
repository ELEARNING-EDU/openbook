package com.elearning.openbook.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elearning.openbook.R
import com.elearning.openbook.library.ReadingLogStore
import com.elearning.openbook.reader.ReaderTheme
import com.elearning.openbook.settings.AppLanguage

@Composable
fun EpubReaderApp(
    autoOpenFirstBook: Boolean = false,
    demo: DemoOptions = DemoOptions.None,
) {
    val context = LocalContext.current
    val app = viewModel<AppViewModel>()
    val state by app.state.collectAsStateWithLifecycle()
    val contextActivity = context as? Activity

    // The harness opens the first book once; importing a book later must not yank the
    // shelf away.
    var autoOpenApplied by remember { mutableStateOf(false) }
    LaunchedEffect(state.books, autoOpenFirstBook) {
        if (autoOpenFirstBook && !autoOpenApplied && state.books.isNotEmpty()) {
            autoOpenApplied = true
            app.open(state.books.first())
        }
    }

    // A notification tap asks for its book, at the place the voice has reached.
    val pendingBookId = com.elearning.openbook.playback.PendingOpen.bookId
    LaunchedEffect(pendingBookId, state.books) {
        val pending = pendingBookId
        if (pending != null) {
            val target = state.books.firstOrNull { it.id == pending }
            if (target != null) {
                app.open(target)
                com.elearning.openbook.playback.PendingOpen.bookId = null
            }
        }
    }

    // A document another app opened or shared is imported through the library, then read.
    val pendingImport = PendingImport.uri
    LaunchedEffect(pendingImport) {
        val uri = pendingImport ?: return@LaunchedEffect
        PendingImport.uri = null
        app.import(uri, open = true)
    }

    // The demo's staged states are one shot per launch.
    val demoPending = demo.isActive && state.demoPending

    // The reader saves the ledger on dispose; refresh Home after that save, not before.
    LaunchedEffect(state.opened) {
        if (state.opened == null) app.refreshAfterReader()
    }

    val systemDark = isSystemInDarkTheme()
    // The reader follows the phone until it picks a face of its own.
    LaunchedEffect(systemDark) { app.followSystemDark(systemDark) }
    val darkSurface = if (state.opened != null) ReaderTheme.isDark else systemDark
    val view = LocalView.current
    SideEffect {
        val window = contextActivity?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkSurface
            isAppearanceLightNavigationBars = !darkSurface
        }
    }

    var showAppSettings by remember { mutableStateOf(false) }
    val colors = shellColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        val book = state.opened
        if (book == null) {
            MainScreen(
                books = state.books,
                progress = state.progress,
                reading = state.reading,
                loading = state.loading,
                tab = state.tab,
                onTab = app::selectTab,
                onPick = { uri -> uri?.let(app::import) },
                onOpen = app::open,
                onAppSettings = { showAppSettings = true },
                onMarkFinished = app::markFinished,
                summary = state.summary,
                week = state.week,
                onEditBook = app::updateMetadata,
                onDeleteBooks = app::deleteBooks,
            )
        } else {
            ReaderScreen(
                book = book,
                onBack = app::closeBook,
                demo = demo,
                demoPending = demoPending,
                onDemoUsed = app::useDemo,
            )
        }

        val now = com.elearning.openbook.playback.NowPlaying.book
        // The listening disc follows the reader across Home, Library and Store.
        if (now != null && state.opened == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 104.dp),
            ) {
                CircleButton(
                    onClick = { app.open(now) },
                    size = 56.dp,
                    contentDescription = stringResource(R.string.player_open),
                ) {
                    VinylDisc(
                        title = now.title,
                        spinning = com.elearning.openbook.playback.NowPlaying.playing,
                        discSize = 48.dp,
                        coverPath = now.coverPath,
                    )
                }
            }
        }

    }

    val failedImport = state.importError
    if (failedImport != null) {
        AlertDialog(
            onDismissRequest = app::dismissImportError,
            confirmButton = {
                TextButton(onClick = app::dismissImportError) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.import_failed_title),
                    fontFamily = FontFamily.Serif,
                )
            },
            text = {
                Text(
                    text = (failedImport as? com.elearning.openbook.model.EpubException)
                        ?.let { epubProblemText(it.problem) }
                        ?: stringResource(R.string.import_failed_body),
                    fontFamily = FontFamily.Serif,
                )
            },
            containerColor = colors.raised,
            titleContentColor = colors.ink,
            textContentColor = colors.inkSoft,
        )
    }

    if (state.deleteFailure > 0) {
        AlertDialog(
            onDismissRequest = app::dismissDeleteFailure,
            confirmButton = {
                TextButton(onClick = app::dismissDeleteFailure) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_failed_title),
                    fontFamily = FontFamily.Serif,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_failed_body, state.deleteFailure),
                    fontFamily = FontFamily.Serif,
                )
            },
            containerColor = colors.raised,
            titleContentColor = colors.ink,
            textContentColor = colors.inkSoft,
        )
    }

    if (showAppSettings) {
        AppSettingsDialog(
            language = state.language,
            goalMinutes = state.goalMinutes,
            repairText = state.repairText,
            onDismiss = { showAppSettings = false },
            onSelect = { next ->
                app.setLanguage(next)
                showAppSettings = false
                // Resources are fixed at attach time; rebuild the activity, not the phone.
                contextActivity?.recreate()
            },
            onGoal = app::setGoal,
            onRepair = app::setRepairText,
        )
    }
}

/** The reader's own settings: language, reading, and the sleep timer. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AppSettingsDialog(
    language: AppLanguage,
    goalMinutes: Int,
    repairText: Boolean,
    onDismiss: () -> Unit,
    onSelect: (AppLanguage) -> Unit,
    onGoal: (Int) -> Unit,
    onRepair: (Boolean) -> Unit,
) {
    val colors = shellColors()
    val sleepMinutes = com.elearning.openbook.playback.SleepTimer.minutes
    val sleepLeft = com.elearning.openbook.playback.SleepTimer.leftSeconds
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.raised,
        contentColor = colors.ink,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = colors.inkSoft) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.app_settings),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = colors.ink,
            )

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroupLabel(stringResource(R.string.settings_group_language))
            SettingsPick(
                label = stringResource(R.string.app_language),
                value = stringResource(language.labelRes()),
                options = AppLanguage.entries.map { option ->
                    stringResource(option.labelRes()) to { onSelect(option) }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroupLabel(stringResource(R.string.settings_group_reading))
            SettingsPick(
                label = stringResource(R.string.settings_reading_goal),
                value = stringResource(R.string.settings_reading_goal_minutes, goalMinutes),
                options = ReadingLogStore.GOAL_CHOICES.map { minutes ->
                    stringResource(R.string.settings_reading_goal_minutes, minutes) to { onGoal(minutes) }
                },
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_repair_text),
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = colors.ink,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_repair_text_hint),
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = colors.inkSoft.copy(alpha = 0.8f),
                    )
                }
                androidx.compose.material3.Switch(checked = repairText, onCheckedChange = onRepair)
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroupLabel(stringResource(R.string.settings_group_audio))
            SettingsPick(
                label = stringResource(R.string.settings_sleep_timer),
                value = when {
                    sleepMinutes == 0 -> stringResource(R.string.settings_sleep_off)
                    sleepLeft > 0 -> stringResource(
                        R.string.settings_sleep_left,
                        (sleepLeft + 59) / 60,
                    )
                    else -> stringResource(R.string.settings_reading_goal_minutes, sleepMinutes)
                },
                options = SLEEP_CHOICES.map { minutes ->
                    val label = if (minutes == 0) {
                        stringResource(R.string.settings_sleep_off)
                    } else {
                        stringResource(R.string.settings_reading_goal_minutes, minutes)
                    }
                    label to { com.elearning.openbook.playback.SleepTimer.set(minutes) }
                },
            )
        }
    }
}

/** One setting row: the current value on the right, the choices in a popover. */
@Composable
private fun SettingsPick(
    label: String,
    value: String,
    options: List<Pair<String, () -> Unit>>,
) {
    val colors = shellColors()
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { open = true }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                color = colors.ink,
            )
            Text(
                text = value,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = colors.accent,
            )
            Text(
                text = "⌄",
                modifier = Modifier.padding(start = 8.dp),
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = colors.inkSoft,
            )
        }
        if (open) {
            androidx.compose.ui.window.Popup(
                popupPositionProvider = AnchorBelow,
                onDismissRequest = { open = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .shadow(10.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.raised)
                        .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
                        .padding(vertical = 6.dp),
                ) {
                    for ((text, action) in options) {
                        Text(
                            text = text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    open = false
                                    action()
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp,
                            color = colors.ink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(bottom = 2.dp),
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = shellColors().inkSoft,
    )
}

/**
 * Drops a menu just under the row it belongs to. `DropdownMenu` inside a bottom sheet is
 * anchored to the sheet's own window in Compose, which put it far from the row; this
 * provider uses the popup's real anchor bounds instead.
 */
private object AnchorBelow : androidx.compose.ui.window.PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ): androidx.compose.ui.unit.IntOffset {
        val margin = 8
        val x = anchorBounds.left.coerceIn(
            margin,
            (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin),
        )
        val below = anchorBounds.bottom + margin
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - popupContentSize.height - margin).coerceAtLeast(margin)
        }
        return androidx.compose.ui.unit.IntOffset(x, y)
    }
}

private val SLEEP_CHOICES = listOf(0, 5, 15, 30, 45, 60)

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.VIETNAMESE -> R.string.language_vietnamese
    AppLanguage.ENGLISH -> R.string.language_english
}
