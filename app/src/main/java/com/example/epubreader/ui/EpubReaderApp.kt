package com.example.epubreader.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.epubreader.R
import com.example.epubreader.library.ReadingLogStore
import com.example.epubreader.reader.ReaderTheme
import com.example.epubreader.settings.AppLanguage
import com.example.epubreader.ui.MainTab

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
    val pendingBookId = com.example.epubreader.playback.PendingOpen.bookId
    LaunchedEffect(pendingBookId, state.books) {
        val pending = pendingBookId
        if (pending != null) {
            val target = state.books.firstOrNull { it.id == pending }
            if (target != null) {
                app.open(target)
                com.example.epubreader.playback.PendingOpen.bookId = null
            }
        }
    }

    // The demo's staged states are one shot per launch.
    val demoPending = demo.isActive && state.demoPending

    // The reader saves the ledger on dispose; refresh Home after that save, not before.
    LaunchedEffect(state.opened) {
        if (state.opened == null) app.refreshAfterReader()
    }

    val systemDark = isSystemInDarkTheme()
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

        val now = com.example.epubreader.playback.NowPlaying.book
        if (now != null && state.opened == null && state.tab == MainTab.HOME) {
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
                        spinning = com.example.epubreader.playback.NowPlaying.playing,
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
                    text = (failedImport as? com.example.epubreader.model.EpubException)
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
            onDismiss = { showAppSettings = false },
            onSelect = { next ->
                app.setLanguage(next)
                showAppSettings = false
                // Resources are fixed at attach time; rebuild the activity, not the phone.
                contextActivity?.recreate()
            },
            onGoal = app::setGoal,
        )
    }
}

/** App language and the daily reading goal: both belong to the reader, not the phone. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AppSettingsDialog(
    language: AppLanguage,
    goalMinutes: Int,
    onDismiss: () -> Unit,
    onSelect: (AppLanguage) -> Unit,
    onGoal: (Int) -> Unit,
) {
    val colors = shellColors()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.raised,
        contentColor = colors.ink,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = colors.inkSoft) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.app_settings),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_language),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.inkSoft,
            )
            Spacer(modifier = Modifier.height(4.dp))
            for (option in AppLanguage.entries) {
                val selected = option == language
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, onClick = { onSelect(option) })
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.accent,
                            unselectedColor = colors.inkSoft,
                        ),
                    )
                    Text(
                        text = stringResource(
                            when (option) {
                                AppLanguage.SYSTEM -> R.string.language_system
                                AppLanguage.VIETNAMESE -> R.string.language_vietnamese
                                AppLanguage.ENGLISH -> R.string.language_english
                            },
                        ),
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = colors.ink,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.settings_reading_goal),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.inkSoft,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (minutes in ReadingLogStore.GOAL_CHOICES) {
                    val selected = minutes == goalMinutes
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) colors.accent.copy(alpha = 0.16f)
                                else colors.raised,
                            )
                            .border(
                                1.dp,
                                if (selected) colors.accent else colors.hairline,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { onGoal(minutes) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_reading_goal_minutes, minutes),
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            color = if (selected) colors.accent else colors.ink,
                        )
                    }
                }
            }
        }
    }
}
