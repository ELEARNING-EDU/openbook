package com.elearning.openbook.ui

import android.os.SystemClock
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elearning.openbook.library.ReadingLogStore
import java.time.LocalDate
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.elearning.openbook.R
import com.elearning.openbook.epub.EpubArchive
import com.elearning.openbook.epub.HtmlText
import com.elearning.openbook.model.BookItem
import com.elearning.openbook.model.RawBook
import com.elearning.openbook.reader.Bookmark
import com.elearning.openbook.reader.BookmarkStore
import com.elearning.openbook.reader.CurlView
import com.elearning.openbook.reader.PageMetrics
import com.elearning.openbook.reader.ReaderEngine
import com.elearning.openbook.reader.ReaderSettings
import com.elearning.openbook.reader.ReaderTheme
import com.elearning.openbook.reader.ReadingPosition
import com.elearning.openbook.reader.ReadingPositions
import com.elearning.openbook.reader.SpeechReader
import com.elearning.openbook.reader.SpeechState
import com.elearning.openbook.reader.ThemeId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

/**
 * Everything the reader's chrome and the paginator must agree on comes from one place:
 * `PageMetrics`, built here from the measured window and handed to the engine.
 *
 * The running head is *not* chrome — the page renderer draws it, so it curls with the
 * paper — but it rides the toolbar's own line, which is why the toolbar has no band of
 * its own and the text starts one gap below the two buttons.
 */

/** Escape hatch for the reader's imperative page jumps. */
private class CurlHolder {
    var view: CurlView? = null
}

/**
 * How much of the reader's chrome is on screen.
 *
 * The middle level is the one that matters: while the book is being read aloud the page
 * is not the reader's only subject any more, but the paper must still be readable, so
 * the toolbar drops to the way out and the player and keeps nothing else. Without it,
 * auto-hide would take the back button with it and leave the reader in a book with no
 * exit and a voice that will not stop.
 */
private enum class ChromeMode {
    /** Toolbar pairs, progress and controls, all of it. */
    FULL,

    /** The voice's turn: the way out and the player, nothing else. */
    PLAYER_ONLY,

    /** Just the page. */
    HIDDEN,
}

private val ChromeMode.visible: Boolean get() = this != ChromeMode.HIDDEN

/**
 * Optional states the screenshot harness can ask for, mirroring the iOS demo build.
 * All of them are inert unless the launching intent carries the extras.
 */
data class DemoOptions(
    val page: Int = -1,
    val tapX: Int = -1,
    val tapY: Int = -1,
    val reading: Boolean = false,
    val player: Boolean = false,
    val settings: Boolean = false,
    val sizeStep: Int = 0,
    val theme: String = "",
    /** -1 leaves the mode alone, 0 is light, 1 is dark. */
    val dark: Int = -1,
    /** A `key:value,key:value` list applied to the theme before anything is shown. */
    val config: String = "",
    val customize: Boolean = false,
    /** Open the appearance screen with the font list already showing. */
    val fontList: Boolean = false,
    /** Open the search screen, optionally with a query already typed in. */
    val search: Boolean = false,
    val searchQuery: String = "",
    /** Mark the page the harness lands on, so the toolbar shows a kept mark. */
    val bookmark: Boolean = false,
    /** Open the contents sheet on its marks tab. */
    val marks: Boolean = false,
    /**
     * Harness only, and only for the one thing a screenshot cannot stage by hand: the
     * page a *late* speech cursor claims after the voice has been paused. The demo
     * starts reading, pauses it, and only then hands the reader the first sentence of
     * this page — the shape of a speech callback that arrives after the pause — so a
     * screenshot can show the page staying where the reader's thumb left it. -1 is off.
     */
    val lateCursor: Int = -1,
    /**
     * Harness only, and only for what a still screenshot cannot stage: a curl held open
     * where the camera can see it. `curl` is the pull as a fraction of the page (0…1,
     * -1 is off), `curlAngle` is how far the crease leans off straight across in degrees
     * (negative leans the other way), and `curlBack` turns the page backwards instead.
     */
    val curl: Float = -1f,
    val curlAngle: Float = 0f,
    val curlBack: Boolean = false,
    /** A pull played back through the drag code, so the curl can be watched moving. */
    val dragX: Float = 0f,
    val dragY: Float = 0f,
    val dragSeconds: Float = 0f,
    /**
     * Harness only: after the voice has started, jump to this chapter (1-based) through
     * the same call the contents sheet's rows make, so what is verified is the reader's
     * own behaviour rather than a parallel code path. -1 is off.
     */
    val jumpChapter: Int = -1,
    /**
     * Harness only: arm the sleep timer for this many minutes once the voice has
     * started. The sheet's shortest real choice is 5 minutes, so a screenshot run uses
     * 1 to watch it expire. 0 is off.
     */
    val sleepTimer: Int = 0,
) {
    val isActive: Boolean
        get() = page >= 0 || (tapX >= 0 && tapY >= 0) || reading || player ||
            settings || sizeStep != 0 || theme.isNotBlank() || dark >= 0 ||
            config.isNotBlank() || customize || fontList || search ||
            searchQuery.isNotBlank() || bookmark || marks || lateCursor >= 0 ||
            curl >= 0f || dragSeconds > 0f || jumpChapter > 0 || sleepTimer > 0

    companion object {
        val None = DemoOptions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: BookItem,
    onBack: () -> Unit,
    demo: DemoOptions = DemoOptions.None,
    /**
     * Whether the harness's staged states are still waiting to be played. They are one
     * shot per launch: replaying them every time a book is opened in the same process
     * made a demo launch that starts the voice start it again, unasked, on the reader's
     * next visit.
     */
    demoPending: Boolean = true,
    onDemoUsed: () -> Unit = {},
) {
    val stores = androidx.lifecycle.viewmodel.compose.viewModel<ReaderStoresViewModel>()
    val reader = androidx.lifecycle.viewmodel.compose.viewModel<ReaderViewModel>(key = book.id)
    val readerState by reader.state.collectAsStateWithLifecycle()
    val settings = stores.settings
    val positions = stores.positions
    val bookmarks = stores.bookmarks
    val readingLogStore = stores.readingLog
    val highlightStore = stores.highlights

    val staged = if (demoPending) demo else DemoOptions.None
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val context = LocalContext.current

    var size by remember { mutableStateOf(IntSize.Zero) }
    var raw by remember { mutableStateOf<RawBook?>(null) }
    var engine by remember { mutableStateOf<ReaderEngine?>(null) }
    val failure = readerState.failure
    val pageIndex = readerState.pageIndex

    /**
     * The page a contents/search jump is on its way to, or -1.
     *
     * A jump is played as a run of single-page curls, so the view walks across the pages
     * in between; while it does, the voice must not chase the page it is speaking (its own
     * sentence is still on the page it started from) or it would cancel the jump and drag
     * the reader back. The voice is moved once, when the jump arrives.
     */
    var jumpTarget by remember { mutableIntStateOf(-1) }
    var showToc by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showCustomize by remember { mutableStateOf(false) }
    var showPlayer by remember { mutableStateOf(false) }
    /** The full player, opened by tapping the title in the reading bar. */
    var showPlayerSheet by remember { mutableStateOf(false) }
    /** The live word selection: chapter and character range, plus its bands. */
    var selection by remember { mutableStateOf<TextSelection?>(null) }
    /** The selection popover's colour row is open. */
    var colourOpen by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    /** A saved highlight opened for edit/delete by a long-press. */
    var editing by remember { mutableStateOf<com.elearning.openbook.reader.Highlight?>(null) }
    var editColourOpen by remember { mutableStateOf(false) }
    // The sleep timer lives with the playback, not with this composition: Back disposes
    // the screen but must not cancel a deadline the reading voice still owes.
    val sleepMinutes = com.elearning.openbook.playback.SleepTimer.minutes
    val sleepLeft = com.elearning.openbook.playback.SleepTimer.leftSeconds
    var chrome by remember { mutableStateOf(ChromeMode.FULL) }
    var autoHideToken by remember { mutableIntStateOf(0) }
    var currentSentence by remember { mutableIntStateOf(-1) }
    var speechNote by remember { mutableStateOf<String?>(null) }
    val holder = remember { CurlHolder() }
    LaunchedEffect(book.id) {
        reader.begin(book.id)
        bookmarks.open(book.id)
        highlightStore.open(book.id)
    }

    // Where this book was left, read once per visit: the reader is put back there after
    // the first typeset, and every page they settle on afterwards writes the new place.
    val savedPosition = readerState.savedPosition

    // Short status lines, localized here so the callbacks below need no context.
    val sleepDoneNote = stringResource(R.string.speech_timer_done)
    val noSentencesNote = stringResource(R.string.speech_no_sentences)
    val noSentenceAtTapNote = stringResource(R.string.speech_no_sentence_at_tap)
    val highlightModeNote = stringResource(R.string.highlight_mode)

    // The active theme's own typography, and the palette it paints with. Both are
    // Compose state, so any edit in the appearance screens re-typesets from here.
    val config = settings.configOf(settings.themeId)

    // One object, built from the measured window: the paginator, the renderer and the
    // chrome all read their geometry from it, so they cannot drift apart. The margin
    // is the reader's own setting now, which is why it is part of the key.
    val metrics = remember(size, statusBarTop, density, config.marginDp) {
        if (size.width <= 0 || size.height <= 0) {
            null
        } else {
            PageMetrics(
                width = size.width,
                height = size.height,
                density = density.density,
                statusBarTop = statusBarTop,
                hInsetPx = config.marginDp * density.density,
            )
        }
    }

    // The engine is process-wide: leaving the reader (Back) must not silence the book.
    val speech = remember { com.elearning.openbook.reader.SpeechHolder.get(context) }
    DisposableEffect(speech) {
        speech.onHighlight = { currentSentence = it }
        speech.onStateChanged = { state -> Log.i(TAG, "speech state: $state") }
        speech.onStatus = { note ->
            speechNote = note
            Log.i(TAG, "speech: $note")
        }
        onDispose {
            // Detach this composition without touching the voice that is still reading.
            speech.onHighlight = {}
            speech.onStateChanged = {}
            speech.onStatus = {}
        }
    }
    // A new book must not keep reading the old one, but reopening the book that is
    // already being narrated must let it carry on from where it is.
    LaunchedEffect(book.path) {
        val playingBook = com.elearning.openbook.playback.NowPlaying.book
        val sameBook = playingBook?.id == book.id && speech.state != SpeechState.IDLE
        if (!sameBook) speech.stop()
    }

    BackHandler(onBack = onBack)

    // When the reader asked for this book, for the open-time measurements in the log.
    val loadStart = remember(book.path) { SystemClock.elapsedRealtime() }

    // The archive is read once per book: re-typesetting must not touch the file again.
    LaunchedEffect(book.path) {
        raw = null
        engine = null
        reader.setPage(0)
        reader.fail(null)
        val readStart = SystemClock.elapsedRealtime()
        try {
            raw = withContext(Dispatchers.IO) { EpubArchive.read(File(book.path)) }
            Log.i(
                TAG,
                "read ${File(book.path).name}: ${SystemClock.elapsedRealtime() - readStart} ms, " +
                    "${raw?.chapters?.size} chapters",
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            // The reader sees a localized line; the detail stays in the log.
            Log.w(TAG, "could not read ${book.path}", t)
            reader.fail(
                (t as? com.elearning.openbook.model.EpubException)?.problem
                    ?: com.elearning.openbook.model.EpubProblem.GENERIC,
            )
        }
    }

    LaunchedEffect(raw, metrics, config, settings.dark, settings.themeId) {
        val archive = raw ?: return@LaunchedEffect
        val pageMetrics = metrics ?: return@LaunchedEffect
        val spec = settings.typesetSpec(density.density)
        val palette = settings.palette
        // A picture is cut to the column it will be drawn in, and never taller than a
        // little over half the column: the same box the paginator measures the text in.
        val columnWidth = pageMetrics.columnWidth(spec.columnCount)
        val maxImageHeight = pageMetrics.textHeight * MAX_PICTURE_HEIGHT_FRACTION
        val previous = engine
        // Holding a size button must not re-typeset the book per press.
        if (previous != null) delay(RESTYLE_DEBOUNCE_MS)
        if (previous != null) {
            // Where the reader is, in terms of the text rather than a page number:
            // a re-typeset changes what page N is, but not what the reader was reading.
            val page = holder.view?.pageIndex ?: pageIndex
            val anchor = if (page in 0 until previous.pageCount) {
                previous.chapterOfPage(page) to previous.offsetOfPage(page)
            } else {
                null
            }
            speech.stop()
            showPlayer = false
            // The platform HTML importer must run on the main thread, and the spans are
            // mutated by the paginator, so every restyle gets a fresh set.
            val spanned = toSpannableInChunks(archive.chapters, columnWidth, maxImageHeight)
            val built = withContext(Dispatchers.Default) {
                ReaderEngine.build(archive, spanned, pageMetrics, spec, palette)
            }
            val target = anchor?.let { built.pageForChapterOffset(it.first, it.second) } ?: 0
            withContext(Dispatchers.Default) { built.preload(listOf(target, target + 1)) }
            Log.i(
                TAG,
                "typeset ${settings.describe(density.density)}: ${built.pageCount} pages (${built.typesetNote})" +
                    (anchor?.let { "; anchor ${built.chapterTitle(it.first)}+${it.second} -> page ${target + 1}" } ?: ""),
            )
            reader.setPage(target)
            engine = built
        } else {
            reader.fail(null)
            try {
                val typesetStart = SystemClock.elapsedRealtime()
                val spanned = toSpannableInChunks(archive.chapters, columnWidth, maxImageHeight)
                val built = withContext(Dispatchers.Default) {
                    ReaderEngine.build(archive, spanned, pageMetrics, spec, palette)
                }
                Log.i(
                    TAG,
                    "typeset ${archive.chapters.size} chapters -> ${built.pageCount} pages in " +
                        "${SystemClock.elapsedRealtime() - typesetStart} ms",
                )
                // The place the reader left the book, if it has been open before. The
                // anchor is the text, not the old page number: coming back after a
                // re-typeset must land on the words, not on whatever page 7 has become.
                val target = savedPosition
                    ?.let { built.pageForChapterOffset(it.chapter, it.offset) }
                    ?.coerceIn(0, built.pageCount - 1)
                    ?: 0
                withContext(Dispatchers.Default) { built.preload(listOf(target, target + 1)) }
                Log.i(
                    TAG,
                    "typeset ${settings.describe(density.density)}: ${built.pageCount} pages (${built.typesetNote})" +
                        (
                            savedPosition?.let { saved ->
                                "; resumed at page ${target + 1} (left on page " +
                                    "${saved.page + 1}/${saved.pageCount}, anchor chapter " +
                                    "${saved.chapter}+${saved.offset})"
                            } ?: "; first read, page 1"
                            ),
                )
                reader.setPage(target)
                engine = built
                Log.i(
                    TAG,
                    "first page ready for ${book.title}: ${SystemClock.elapsedRealtime() - loadStart} ms total",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.w(TAG, "could not typeset ${book.path}", t)
                reader.fail(
                    (t as? com.elearning.openbook.model.EpubException)?.problem
                        ?: com.elearning.openbook.model.EpubProblem.GENERIC,
                )
            }
        }
    }

    val ready = engine
    val tocSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(ready) {
        speech.source = ready
        ready?.let { speech.useLanguage(it.bookLanguage) }
    }
    // The notification's chapter skips read the engine from the process-wide controller,
    // which must have the current book even after this screen goes away.
    LaunchedEffect(ready, book) {
        com.elearning.openbook.playback.Narration.attach(book, ready)
    }

    val speechState = speech.state
    val chromeVisible = chrome.visible

    // Reading time: the reader is on screen and the app is in front. The ledger itself is
    // pure JVM (`ReadingLog`); this block only decides which seconds are "alive".
    val logStore = readingLogStore
    val readingLog = remember { logStore.load() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            foreground = event == Lifecycle.Event.ON_RESUME
            if (event == Lifecycle.Event.ON_PAUSE) logStore.save(readingLog)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            logStore.save(readingLog)
        }
    }
    var lastPageTurnAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(pageIndex) { lastPageTurnAt = SystemClock.elapsedRealtime() }
    LaunchedEffect(book.path) {
        var sessionSeconds = 0
        var lastAccrual = 0L
        var lastSave = SystemClock.elapsedRealtime()
        while (true) {
            delay(1000)
            val now = SystemClock.elapsedRealtime()
            if (!foreground) continue
            if (reader.state.value.failure != null) continue
            val listening = speech.state == SpeechState.PLAYING
            // Five quiet minutes with neither a page turn nor the voice stop the count.
            if (!listening && now - lastPageTurnAt > IDLE_CUTOFF_MS) continue
            if (lastAccrual != 0L && now - lastAccrual > SESSION_GAP_MS) sessionSeconds = 0
            sessionSeconds++
            readingLog.addSecond(System.currentTimeMillis(), listening = listening)
            readingLog.noteSession(
                LocalDate.now(),
                sessionSeconds,
                newSession = sessionSeconds == 1,
            )
            lastAccrual = now
            if (now - lastSave >= SAVE_INTERVAL_MS) {
                logStore.save(readingLog)
                lastSave = now
            }
        }
    }
    // While the voice has the book the player is not optional: it is the only control
    // level (b) leaves on the page, so it opens itself even when narration began from a
    // tap on a sentence rather than from the headset.
    val playerOpen =
        (showPlayer || chrome == ChromeMode.PLAYER_ONLY) && speechState != SpeechState.IDLE

    // How far the voice has read, through the whole book's text rather than the page on
    // screen: the cursor is the last character of the sentence being spoken, summed over
    // every chapter before it. The player draws it, and it is in the log because a
    // screenshot alone could not say how much of the book it is.
    val readCursor = if (ready == null) 0 else ready.readCursor(currentSentence)
    val bookChars = ready?.bookCharCount ?: 0
    val readProgress = if (bookChars > 0) readCursor.toFloat() / bookChars else 0f
    LaunchedEffect(readCursor, bookChars, speechState, playerOpen) {
        if (playerOpen) {
            Log.i(
                TAG,
                "read cursor $readCursor/$bookChars chars " +
                    "(${String.format(java.util.Locale.US, "%.1f", readProgress * 100f)}% of the book), " +
                    "sentence #$currentSentence speech=$speechState",
            )
        }
    }

    // The chapter the voice is in, for the full player's caption and chapter skips.
    val voiceChapterIndex = ready?.let { source ->
        if (currentSentence >= 0) {
            source.chapterOfSentence(currentSentence)
        } else {
            source.chapterOfPage(holder.view?.pageIndex ?: pageIndex)
        }
    } ?: 0
    val voiceChapter = ready?.chapterTitle(voiceChapterIndex).orEmpty()

    /**
     * The full player's back and forward: to the head of the previous/next chapter,
     * read from its first sentence. The buttons are off at the first and last chapter.
     */
    fun skipChapter(step: Int) {
        val source = ready ?: return
        var target = voiceChapterIndex + step
        var from = -1
        // Image-only chapters (a cover, a plate) have no sentence: walk past them rather
        // than leaving the button enabled and doing nothing.
        while (target in 0 until source.chapterCount) {
            from = source.firstSentenceOfChapter(target)
            if (from >= 0) break
            target += step
        }
        if (target !in 0 until source.chapterCount || from < 0) return
        if (speech.state == SpeechState.PAUSED) {
            val page = source.sentencePage(from)
            if (page >= 0 && page != (holder.view?.pageIndex ?: pageIndex)) {
                reader.setPage(page)
                jumpTarget = -1
                holder.view?.setPageIndex(page)
            }
        }
        speech.skip(from)
        Log.i(
            TAG,
            "chapter $step -> #$target, sentence #$from " +
                "(${source.sentenceText(from).take(40)}) speech=${speech.state}",
        )
    }

    // The notification's controls belong to the process-wide engine (see Narration), so
    // they keep working after Back. While this screen is on screen a chapter skip also
    // moves the leaf, through the same page sync the sheet's buttons use.
    val voiceMoved = androidx.compose.runtime.rememberUpdatedState<(Int) -> Unit> { from ->
        val source = ready
        if (source != null) {
            val page = source.sentencePage(from)
            if (page >= 0 && page != (holder.view?.pageIndex ?: pageIndex)) {
                reader.setPage(page)
                jumpTarget = -1
                holder.view?.setPageIndex(page)
            }
        }
    }
    DisposableEffect(Unit) {
        com.elearning.openbook.playback.Narration.onVoiceMoved = { from -> voiceMoved.value(from) }
        onDispose { com.elearning.openbook.playback.Narration.detachReader() }
    }
    LaunchedEffect(speechState, voiceChapter, book.title) {
        if (speechState == SpeechState.IDLE) {
            com.elearning.openbook.playback.PlaybackService.stop(context)
        } else {
            com.elearning.openbook.playback.PlaybackService.update(
                context,
                speechState == SpeechState.PLAYING,
                book.id,
                book.title,
                voiceChapter,
            )
        }
    }

    // The deadline itself lives in SleepTimer, outside this composition: its only job is
    // to stop the voice, and Back must not cancel a timer the narration still owes.
    LaunchedEffect(com.elearning.openbook.playback.SleepTimer.justFinished) {
        if (com.elearning.openbook.playback.SleepTimer.justFinished) {
            speechNote = sleepDoneNote
            com.elearning.openbook.playback.SleepTimer.acknowledge()
        }
    }

    // Whether the voice was running a moment ago, so stopping it can be told apart from
    // never having started.
    var wasReading by remember { mutableStateOf(false) }

    // The chrome's level is the evidence for level (b): it has to be in the log, because
    // a screenshot cannot tell "toolbar hidden" from "toolbar back, one button".
    LaunchedEffect(chrome) {
        Log.i(TAG, "chrome=${chrome.name.lowercase()} speech=$speechState playerOpen=$playerOpen")
    }

    // Reading a sentence that lives on another page drags that page in behind it,
    // with the same curl animation a manual turn uses — but only while the voice is
    // actually running. A callback that lands after a pause (the engine posts its own
    // callbacks to this thread, so one can arrive late) must leave the book where the
    // reader's thumb left it: turning a page under a paused narrator is the reader
    // losing their place to a voice that is not even speaking.
    LaunchedEffect(currentSentence, jumpTarget) {
        val source = ready ?: return@LaunchedEffect
        if (currentSentence < 0) return@LaunchedEffect
        if (jumpTarget >= 0) {
            // Self-healing: if the jump has already arrived without a notification (it
            // was to the page already on screen, or the view refused it), stop standing
            // aside rather than leaving the voice unable to follow for good.
            if ((holder.view?.pageIndex ?: pageIndex) == jumpTarget) jumpTarget = -1
            return@LaunchedEffect
        }
        val page = source.sentencePage(currentSentence)
        val current = holder.view?.pageIndex ?: pageIndex
        if (speech.state != SpeechState.PLAYING) {
            Log.i(
                TAG,
                "page turn held while ${speech.state.name.lowercase()}: cursor on " +
                    "#$currentSentence (page ${page + 1}), reader on page ${current + 1}",
            )
            return@LaunchedEffect
        }
        Log.i(TAG, "reading #$currentSentence on page ${page + 1}: ${source.sentenceText(currentSentence)}")
        // Only forward. A cursor whose page lies *behind* the page on screen belongs to
        // a sentence that began on an earlier page and is still being spoken across this
        // one; turning the page back to its beginning would take the book out of the
        // reader's hand. Backward moves come from the reader, through `pageShown`.
        if (page >= 0 && page > current) holder.view?.animateToPage(page)
    }

    fun currentPage(): Int = holder.view?.pageIndex ?: pageIndex

    /** A jump the reader asked for (contents, search, a mark): the page *and* the voice. */
    fun jumpToPage(page: Int) {
        if (page == currentPage()) return
        jumpTarget = page
        holder.view?.animateToPage(page)
    }

    /**
     * Every page change lands here, whoever caused it: the curl settling under the
     * reader's hand, a table-of-contents jump, a search result, the harness — or the
     * voice itself asking for the page the sentence it is reading lives on.
     *
     * A reading voice follows the reader's hand. Turning the page (or jumping a chapter)
     * while the book is being read moves the voice to the first sentence of the page now
     * on screen, playing or paused; it is the reader who is turning the leaves. The one
     * case that must not seek is the voice's own move: when the sentence being read is
     * already on this page, the page change *was* the follow, and seeking again would
     * restart the sentence it is speaking.
     */
    fun pageShown(page: Int) {
        reader.setPage(page)
        // A page notification means whatever jump was in flight has landed (a run reports
        // once, on arrival) or has been taken over by a hand — either way the voice may
        // follow again. Guarding this on `page == jumpTarget` would leave the voice frozen
        // for good if a hand turned a page in the middle of a jump.
        jumpTarget = -1
        val source = ready ?: return
        if (speech.state == SpeechState.IDLE) return
        var first = source.firstSentenceOnPage(page)
        if (first < 0) return
        // `firstSentenceOnPage` falls back to the sentence that crosses the top edge when
        // nothing *begins* on this page. Its page is then the one before, and the voice
        // keeps reading it here — see the forward-only rule in the follow effect.
        val cursorPage = if (currentSentence < 0) -1 else source.sentencePage(currentSentence)
        if (cursorPage == page) return
        speech.seek(first)
        Log.i(
            TAG,
            "audio follows the page: page ${page + 1} now on screen -> " +
                "voice moves to sentence #$first " +
                "(was on ${if (cursorPage < 0) "nothing" else "page ${cursorPage + 1}"}); " +
                "${source.sentenceText(first).take(60)}",
        )
    }

    /**
     * Every page the reader settles on is the new place in the book, whichever way they
     * got there: a curl, a contents jump, a search hit, or the voice. It is written here
     * rather than in [pageShown] so a jump is written too, and so the page the first
     * typeset puts the reader back on is itself recorded.
     */
    LaunchedEffect(ready, pageIndex) {
        val source = ready ?: return@LaunchedEffect
        if (pageIndex !in 0 until source.pageCount) return@LaunchedEffect
        positions.save(
            book.id,
            ReadingPosition(
                page = pageIndex,
                pageCount = source.pageCount,
                chapter = source.chapterOfPage(pageIndex),
                offset = source.offsetOfPage(pageIndex),
            ),
        )
    }

    // Saved highlight bands are resolved per page inside the view, so a curl never draws
    // the previous page's bands.
    LaunchedEffect(ready, highlightStore.highlights) {
        val source = ready ?: return@LaunchedEffect
        holder.view?.savedRectsForPage = { page ->
            highlightStore.highlights.flatMap { highlight ->
                source.rangeRects(
                    page,
                    highlight.chapterIndex,
                    highlight.startOffset,
                    highlight.endOffset,
                ).map { it to highlight.color }
            }
        }
        holder.view?.invalidate()
    }
    LaunchedEffect(ready, pageIndex) {
        selection = null
        colourOpen = false
        menuExpanded = false
        editing = null
        editColourOpen = false
    }
    LaunchedEffect(selection) {
        holder.view?.selectionRects = selection?.rects.orEmpty()
        holder.view?.selecting = selection != null
        holder.view?.invalidate()
    }

    /** The toolbar, all of it. */
    fun revealChrome() {
        chrome = ChromeMode.FULL
        autoHideToken++
    }

    /**
     * The level the chrome falls to when left alone: the voice's own level while the
     * book is being read, the bare page otherwise.
     *
     * It reads `speech.state` rather than the composition's `speechState`, because the
     * caller is a coroutine that may outlive the composition it was launched in: the
     * timer can be armed while the book is quiet and fire after the reader starts the
     * voice, and a captured value would then hide the toolbar — and the way out with it
     * — at exactly the moment level (b) exists to prevent.
     */
    fun restingChrome(): ChromeMode =
        if (speech.state == SpeechState.IDLE) ChromeMode.HIDDEN else ChromeMode.PLAYER_ONLY

    fun hideChrome() {
        autoHideToken++
        chrome = restingChrome()
    }

    fun scheduleAutoHide() {
        autoHideToken++
    }

    // Stopping the voice is the one move that gives the reader the whole toolbar back:
    // the player's ✕ and the last spoken sentence both end here, and the toolbar then
    // hides on its own timer as usual.
    LaunchedEffect(speechState) {
        if (speechState == SpeechState.IDLE) {
            showPlayer = false
            showPlayerSheet = false
            // A sleep timer belongs to the voice that was running; one set before the
            // voice started again must not stop the next one at a stale hour.
            com.elearning.openbook.playback.SleepTimer.cancel()
            if (wasReading) {
                wasReading = false
                revealChrome()
            }
        } else {
            wasReading = true
        }
    }

    /**
     * The bottom-left button: a headset when the book is silent, the book's own cover
     * spinning like a record while it is being read.
     */
    fun audioButtonTapped() {
        if (speechState != SpeechState.IDLE) {
            showPlayer = !showPlayer
            scheduleAutoHide()
            return
        }
        val source = ready ?: return
        val first = source.firstSentenceOnPage(currentPage())
        if (first >= 0) {
            speech.start(first)
            showPlayer = true
            // The voice's own auto-hide starts when the voice does, not whenever the
            // toolbar last moved.
            scheduleAutoHide()
        } else {
            speechNote = noSentencesNote
        }
    }

    /** True when the page on screen already carries a mark. */
    fun pageMarked(): Boolean {
        val source = ready ?: return false
        val range = source.pageRange(pageIndex)
        return bookmarks.isMarked(source.chapterOfPage(pageIndex), range.first, range.last + 1)
    }

    /**
     * Marks the page on screen, or clears the mark it carries. The anchor is the page's
     * first character, so the mark stays on the same words when the book is re-typeset.
     */
    fun toggleBookmark() {
        val source = ready ?: return
        val page = currentPage()
        val range = source.pageRange(page)
        bookmarks.toggle(
            chapter = source.chapterOfPage(page),
            start = range.first,
            end = range.last + 1,
            offset = range.first,
        )
        Log.i(
            TAG,
            "bookmark page ${page + 1} chapter ${source.chapterOfPage(page)} " +
                "offset ${range.first} -> ${bookmarks.marks.size} mark(s)",
        )
        scheduleAutoHide()
    }

    /**
     * One tap on the page.
     *
     * With a quiet book the reader turns the leaf by hand: a tap on the left third goes
     * back, a tap on the right third goes on, and a tap in the middle works the chrome
     * — shown, hidden. A third with no leaf behind it (the first page, the last page)
     * falls through to the chrome instead, so a hidden toolbar is never stranded.
     *
     * While the voice is running the text is its navigation surface — a tap on a
     * sentence reads from there — and the blank paper is the reader's, which brings
     * the whole toolbar back.
     */
    fun pageTapped(x: Float, y: Float) {
        val source = ready
        val reading = speechState != SpeechState.IDLE
        if (!reading) {
            val width = holder.view?.width?.toFloat() ?: 0f
            if (width > 0f) {
                val third = width / 3f
                val step = when {
                    x < third -> -1
                    x > width - third -> 1
                    else -> 0
                }
                val last = (source?.pageCount ?: 0) - 1
                // Where the reader is heading, not just where the sheet has got to: a
                // second tap while the first curl is still folding must put them two
                // leaves on, the way a hand on paper would.
                val from = if (jumpTarget >= 0) jumpTarget else currentPage()
                if (step != 0 && from + step in 0..last) {
                    jumpToPage(from + step)
                    scheduleAutoHide()
                    return
                }
            }
        }
        if (chrome == ChromeMode.HIDDEN) {
            revealChrome()
            return
        }
        if (source != null && reading && source.isOnText(x, y)) {
            val sentence = source.sentenceAt(currentPage(), x, y)
            if (sentence != null) {
                speech.start(sentence)
                scheduleAutoHide()
                return
            }
        }
        if (chrome == ChromeMode.FULL) hideChrome() else revealChrome()
    }

    // The chrome hides itself so the page is just paper, but never while the reader is
    // in a sheet or the appearance screen. While the voice is running it steps down to
    // the player's own level rather than out of sight.
    LaunchedEffect(autoHideToken, showToc, showSearch, showSettings, showCustomize, showPlayerSheet) {
        if (showToc || showSearch || showSettings || showCustomize || showPlayerSheet) return@LaunchedEffect
        if (!chrome.visible) return@LaunchedEffect
        delay(AUTO_HIDE_MS)
        chrome = restingChrome()
    }

    // Screenshot harness: pick a theme, a mode and a typography first; then, once that
    // has settled into a fresh typeset, land on a page, open a surface, start reading.
    // The appearance pass is its own effect because a re-typeset replaces `ready`, and
    // an effect keyed on `ready` would otherwise be cancelled halfway through.
    var appearanceApplied by remember { mutableStateOf(false) }
    var demoSizeStepped by remember { mutableStateOf(false) }
    var demoApplied by remember { mutableStateOf(false) }

    LaunchedEffect(staged) {
        if (!staged.isActive) return@LaunchedEffect
        delay(DEMO_SETTLE_MS)
        if (staged.theme.isNotBlank()) settings.setTheme(ThemeId.of(staged.theme))
        if (staged.dark >= 0 && (staged.dark == 1) != settings.dark) settings.toggleDark()
        if (staged.config.isNotBlank()) settings.applyDemoConfig(staged.config)
        appearanceApplied = true
    }

    LaunchedEffect(ready, staged, appearanceApplied) {
        val source = ready ?: return@LaunchedEffect
        if (!staged.isActive || demoApplied || !appearanceApplied) return@LaunchedEffect
        // Let the appearance pass finish its re-typeset before touching the page.
        delay(RESTYLE_DEBOUNCE_MS + DEMO_SETTLE_MS)
        if (staged.page >= 0) {
            // The view is the only imperative piece here, so the state has to
            // follow it: the AndroidView update pushes the state back otherwise.
            val target = (staged.page - 1).coerceIn(0, source.pageCount - 1)
            reader.setPage(target)
            holder.view?.setPageIndex(target)
        }
        if (staged.sizeStep != 0 && !demoSizeStepped) {
            demoSizeStepped = true
            repeat(kotlin.math.abs(staged.sizeStep)) {
                if (staged.sizeStep > 0) settings.growText() else settings.shrinkText()
            }
            delay(RESTYLE_DEBOUNCE_MS + DEMO_SETTLE_MS)
        }
        if (staged.curl >= 0f) {
            // A fold held open for the camera: the same geometry the drawing uses, so
            // the screenshot and the log line describe one fold, not two.
            holder.view?.holdFold(staged.curl, staged.curlAngle, staged.curlBack)
        }
        if (staged.dragSeconds > 0f) {
            // A pull played back through the touch path, so the fold is judged moving.
            holder.view?.playDrag(staged.dragX, staged.dragY, staged.dragSeconds)
        }
        if (staged.tapX >= 0 && staged.tapY >= 0) {
            val sentence = source.sentenceAt(currentPage(), staged.tapX.toFloat(), staged.tapY.toFloat())
            if (sentence != null) {
                speech.start(sentence)
            } else {
                speechNote = noSentenceAtTapNote
                Log.w(TAG, "demo tap found no sentence at ${staged.tapX},${staged.tapY}")
            }
        }
        if (staged.reading || staged.player) {
            val first = source.firstSentenceOnPage(currentPage())
            if (first >= 0) speech.start(first)
        }
        if (staged.jumpChapter > 0) {
            // The contents sheet's own jump, a moment after the voice has started: the
            // reader opens a chapter and the voice has to come with it.
            delay(VOICE_SETTLE_MS)
            val row = source.toc.getOrNull(staged.jumpChapter - 1)
            if (row == null) {
                Log.w(TAG, "harness: no chapter ${staged.jumpChapter} (${source.toc.size} chapters)")
            } else {
                Log.i(
                    TAG,
                    "harness: contents jump to chapter ${staged.jumpChapter} " +
                        "(\"${row.title}\") -> page ${row.pageIndex + 1}, voice=${speech.state}",
                )
                jumpToPage(row.pageIndex)
                delay(VOICE_SETTLE_MS)
            }
        }
        if (staged.player) showPlayer = true
        if (staged.sleepTimer > 0) {
            com.elearning.openbook.playback.SleepTimer.set(staged.sleepTimer)
        }
        if (staged.settings) showSettings = true
        if (staged.customize || staged.fontList) showCustomize = true
        if (staged.bookmark) toggleBookmark()
        if (staged.marks) showToc = true
        if (staged.search) showSearch = true
        if (staged.lateCursor >= 0) {
            // The voice runs for a moment, the reader pauses it, and only then does the
            // cursor move: the shape of a speech callback that arrives late. The page
            // must not follow it — a paused narrator is not the reader's hand.
            val first = source.firstSentenceOnPage(currentPage())
            if (first >= 0) speech.start(first)
            delay(DEMO_PAUSE_MS)
            speech.pause()
            val claimed = source.firstSentenceOnPage(
                (staged.lateCursor - 1).coerceIn(0, source.pageCount - 1),
            )
            Log.i(
                TAG,
                "harness: paused at speech=${speech.state}; a late cursor claims the " +
                    "first sentence of page ${staged.lateCursor} (#$claimed)",
            )
            if (claimed >= 0) currentSentence = claimed
        }
        scheduleAutoHide()
        demoApplied = true
        // One shot, spent: a later visit to this book in the same process is a reader's
        // visit, not the harness's.
        onDemoUsed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderTheme.Paper)
            .onSizeChanged { size = it },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                CurlView(viewContext).apply {
                    onPageChanged = { page -> pageShown(page) }
                }
            },
            update = { view ->
                holder.view = view
                val source = ready
                if (source != null) {
                    view.pageSource = source
                    view.onPageTap = { x, y -> pageTapped(x, y) }
                    view.onLongPress = long@{ x, y ->
                        val source = ready ?: return@long
                        val page = currentPage()
                        // A long-press on an existing highlight edits it instead of starting
                        // a new selection; this avoids fighting the tap-to-seek gesture.
                        highlightStore.highlights.firstOrNull { highlight ->
                            highlight.chapterIndex >= 0 &&
                                source.rangeRects(
                                    page,
                                    highlight.chapterIndex,
                                    highlight.startOffset,
                                    highlight.endOffset,
                                ).any { it.left <= x && x <= it.right && it.top <= y && y <= it.bottom }
                        }?.let { found ->
                            selection = null
                            colourOpen = false
                            editColourOpen = false
                            editing = found
                            return@long
                        }
                        val hit = source.offsetAt(page, x, y) ?: return@long
                        val words = source.wordBounds(hit.first, hit.second) ?: return@long
                        if (speech.state == SpeechState.PLAYING) speech.pause()
                        // Close the frame gap before Compose applies `selecting`.
                        holder.view?.selecting = true
                        holder.view?.cancelTurn()
                        selection = TextSelection(
                            page = page,
                            chapter = hit.first,
                            start = words.first,
                            end = words.last + 1,
                            rects = source.rangeRects(page, hit.first, words.first, words.last + 1),
                        )
                        colourOpen = false
                        scheduleAutoHide()
                    }
                    view.onSelectionExtend = extend@{ x, y ->
                        val current = selection ?: return@extend
                        val source = ready ?: return@extend
                        val hit = source.offsetAt(current.page, x, y) ?: return@extend
                        val words = source.wordBounds(hit.first, hit.second) ?: return@extend
                        if (hit.first != current.chapter) return@extend
                        val start = minOf(current.start, words.first)
                        val end = maxOf(current.end, words.last + 1)
                        selection = current.copy(
                            start = start,
                            end = end,
                            rects = source.rangeRects(current.page, current.chapter, start, end),
                        )
                    }
                    view.onSelectionHandleDrag = handle@{ handle, x, y ->
                        val current = selection ?: return@handle
                        val source = ready ?: return@handle
                        val hit = source.offsetAt(current.page, x, y) ?: return@handle
                        val words = source.wordBounds(hit.first, hit.second)
                            ?: return@handle
                        if (hit.first != current.chapter) return@handle
                        val start = if (handle == 0) minOf(words.first, current.end - 1) else current.start
                        val end = if (handle == 1) maxOf(words.last + 1, current.start + 1) else current.end
                        selection = current.copy(
                            start = start,
                            end = end,
                            rects = source.rangeRects(current.page, current.chapter, start, end),
                        )
                    }
                    view.onSelectionCancel = {
                        selection = null
                        colourOpen = false
                        menuExpanded = false
                    }
                    // Re-assigned here rather than only in `factory`, so the callback
                    // reads this composition's state (the book, the cursor, the voice)
                    // instead of the one that happened to build the view.
                    view.onPageChanged = { page -> pageShown(page) }
                    view.setPageIndex(pageIndex)
                    view.highlightSentence = currentSentence
                } else {
                    view.onPageTap = null
                    view.highlightSentence = -1
                }
            },
        )

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(tween(CHROME_FADE_MS)),
            exit = fadeOut(tween(CHROME_FADE_MS)),
        ) {
            ProgressLine(
                fraction = when {
                    ready == null || ready.pageCount == 0 -> 0f
                    else -> (pageIndex + 1f) / ready.pageCount
                },
            )
        }

        // Only surfaces when the speech engine has something to confess, and only for a
        // moment: a status is not part of the page.
        LaunchedEffect(speechNote) {
            val note = speechNote ?: return@LaunchedEffect
            delay(SPEECH_NOTE_MS)
            if (speechNote == note) speechNote = null
        }

        speechNote?.let { note ->
            Text(
                text = note,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = with(density) { statusBarTop.toDp() } + 56.dp,
                        start = 64.dp,
                        end = 64.dp,
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(ReaderTheme.Paper.copy(alpha = 0.95f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = ReaderTheme.InkSoft,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (ready == null && failure == null) {
            LoadingOverlay()
        }
        failure?.let { FailureOverlay(it) }

        val pageMetrics = metrics
        if (pageMetrics != null) {
            // The running head rides this row, drawn inside the page; the toolbar only
            // holds the two buttons at its ends.
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(tween(CHROME_FADE_MS)),
                exit = fadeOut(tween(CHROME_FADE_MS)),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = with(density) { statusBarTop.toDp() } +
                                with(density) { pageMetrics.topButtonGap.toDp() },
                            start = with(density) { pageMetrics.hMargin.toDp() },
                            end = with(density) { pageMetrics.hMargin.toDp() },
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The way out and the contents, at the full level only. While the
                    // voice has the page the toolbar is gone altogether: a tap on the
                    // page brings it back, so a lone dimmed chevron in the corner was
                    // only a button that had lost its row.
                    if (ready != null && chrome == ChromeMode.FULL) {
                        ButtonPair(modifier = Modifier.animateContentSize()) {
                            PairButton(
                                onClick = onBack,
                                contentDescription = stringResource(R.string.reader_back),
                            ) { ChevronGlyph() }
                            PairDivider()
                            PairButton(
                                onClick = { showToc = true },
                                contentDescription = stringResource(R.string.reader_contents),
                            ) { ListGlyph() }
                        }
                    }
                    // Right pair: find something, and mark the place. It is the one pair
                    // the voice's level gives up entirely.
                    if (ready != null && chrome == ChromeMode.FULL) {
                        ButtonPair {
                            PairButton(
                                onClick = { showSearch = true },
                                contentDescription = stringResource(R.string.reader_search),
                            ) { SearchGlyph() }
                            PairDivider()
                            PairButton(
                                onClick = { toggleBookmark() },
                                contentDescription = stringResource(
                                    if (pageMarked()) R.string.reader_bookmark_remove
                                    else R.string.reader_bookmark_add,
                                ),
                            ) {
                                BookmarkGlyph(filled = pageMarked())
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(tween(CHROME_FADE_MS)),
                exit = fadeOut(tween(CHROME_FADE_MS)),
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = with(density) { pageMetrics.hMargin.toDp() },
                            end = with(density) { pageMetrics.hMargin.toDp() },
                            bottom = with(density) { pageMetrics.controlsBottomInset.toDp() },
                        )
                        .height(with(density) { pageMetrics.controlBand.toDp() }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (ready != null) {
                        CircleButton(
                            onClick = { audioButtonTapped() },
                            active = speechState != SpeechState.IDLE,
                            contentDescription = stringResource(R.string.reader_read_aloud),
                        ) {
                            if (speechState == SpeechState.IDLE) {
                                EarphoneGlyph()
                            } else {
                                VinylDisc(
                                    title = book.title,
                                    spinning = speechState == SpeechState.PLAYING,
                                    discSize = 36.dp,
                                    coverPath = book.coverPath,
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(44.dp))
                    }

                    // A weighted box owns the space whether or not the player is
                    // unfurled: an invisible AnimatedVisibility measures to nothing,
                    // which would leave the gear sitting on the disc.
                    Box(modifier = Modifier.weight(1f)) {
                        PlayerSlot(visible = playerOpen) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(10.dp))
                                PlayerRow(
                                    headline = playerHeadline(book, ready, currentSentence),
                                    rateLabel = speech.rateLabel,
                                    playing = speechState == SpeechState.PLAYING,
                                    // How far the voice has read, through the whole book:
                                    // the cursor is the sentence's last character, summed
                                    // over every chapter before it, over the book's total.
                                    progress = readProgress,
                                    canSlowDown = speech.canSlowDown,
                                    canSpeedUp = speech.canSpeedUp,
                                    compact = chrome == ChromeMode.FULL,
                                    onTitle = { showPlayerSheet = true },
                                    onSlower = { speech.slower() },
                                    onPlayPause = { speech.togglePlayPause() },
                                    onFaster = { speech.faster() },
                                    onClose = {
                                        speech.stop()
                                        showPlayer = false
                                        scheduleAutoHide()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    // The gear is one of the toolbar's buttons, so it keeps the toolbar's
                    // company: drawn at the full level, gone with the rest of the row at
                    // the voice's level. Its 44dp is reserved either way, so the pill is
                    // measured against the same box at both levels and does not change
                    // width when the chrome steps down.
                    if (ready != null && chrome == ChromeMode.FULL) {
                        CircleButton(
                            onClick = { showSettings = true },
                            contentDescription = stringResource(R.string.reader_settings),
                        ) { GearGlyph() }
                    } else {
                        Spacer(modifier = Modifier.width(44.dp))
                    }
                }
            }
        }
    }

    val live = selection
    if (live != null && ready != null) {
        val anchor = live.rects.firstOrNull()
        val density = LocalDensity.current
        val screenWidth = with(density) {
            androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx()
        }
        Box(
            modifier = Modifier
                .offset {
                    val x = minOf(anchor?.left ?: 24f, screenWidth - with(density) { 330.dp.toPx() })
                        .toInt()
                        .coerceAtLeast(0)
                    val y = ((anchor?.top ?: 200f) - with(density) { 64.dp.toPx() }).toInt()
                        .coerceAtLeast(0)
                    androidx.compose.ui.unit.IntOffset(x, y)
                }
                .clip(RoundedCornerShape(12.dp))
                .background(ReaderTheme.Paper)
                .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!colourOpen) {
                if (!menuExpanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.reader_highlight),
                            modifier = Modifier
                                .clickable { colourOpen = true }
                                .padding(vertical = 4.dp),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ReaderTheme.Accent,
                        )
                        Text(
                            text = stringResource(R.string.menu_translate),
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            color = ReaderTheme.InkSoft.copy(alpha = 0.45f),
                        )
                        Text(
                            text = stringResource(R.string.menu_search),
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            color = ReaderTheme.InkSoft.copy(alpha = 0.45f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.menu_more),
                            tint = ReaderTheme.Accent,
                            modifier = Modifier
                                .clickable { menuExpanded = true }
                                .padding(horizontal = 2.dp)
                                .size(20.dp),
                        )
                    }
                } else {
                    Column(modifier = Modifier.width(172.dp)) {
                        val actions = listOf(
                            Triple(R.string.reader_highlight, Icons.Filled.BorderColor) { colourOpen = true; menuExpanded = false },
                            Triple(R.string.menu_translate, Icons.Filled.Translate) {},
                            Triple(R.string.menu_search, Icons.Filled.Search) {},
                            Triple(R.string.menu_copy, Icons.Filled.ContentCopy) {},
                            Triple(R.string.menu_share, Icons.Filled.Share) {},
                        )
                        for ((label, icon, action) in actions) {
                            val enabled = label == R.string.reader_highlight
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = enabled, onClick = action)
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (enabled) ReaderTheme.Accent
                                    else ReaderTheme.InkSoft.copy(alpha = 0.45f),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = stringResource(label),
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = if (enabled) FontWeight.SemiBold
                                    else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (enabled) ReaderTheme.Accent
                                    else ReaderTheme.InkSoft.copy(alpha = 0.45f),
                                )
                            }
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (color in com.elearning.openbook.reader.Highlight.COLORS) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ComposeColor(color))
                                .clickable {
                                    val text = ready.chapterSlice(live.chapter, live.start, live.end)
                                    highlightStore.add(
                                        live.chapter,
                                        live.start,
                                        live.end,
                                        text,
                                        color,
                                    )
                                    selection = null
                                    colourOpen = false
                                },
                        )
                    }
                }
            }
        }
    }

    val edit = editing
    if (edit != null) {
        // A tap anywhere else closes the edit popover and its colour row.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    editing = null
                    editColourOpen = false
                },
        )
    }
    if (edit != null && ready != null) {
        val density = LocalDensity.current
        val rects = ready.rangeRects(
            pageIndex,
            edit.chapterIndex,
            edit.startOffset,
            edit.endOffset,
        )
        val anchor = rects.firstOrNull()
        Box(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        minOf(anchor?.left ?: 24f, size.width - with(density) { 200.dp.toPx() })
                            .coerceAtLeast(0f).toInt(),
                        ((anchor?.top ?: 200f) - with(density) { 112.dp.toPx() }).toInt()
                            .coerceAtLeast(0),
                    )
                }
                .clip(RoundedCornerShape(12.dp))
                .background(ReaderTheme.Paper)
                .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!editColourOpen) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        text = stringResource(R.string.reader_highlight),
                        modifier = Modifier
                            .clickable { editColourOpen = true }
                            .padding(vertical = 4.dp),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = ReaderTheme.Accent,
                    )
                    Text(
                        text = stringResource(R.string.action_delete),
                        modifier = Modifier
                            .clickable {
                                highlightStore.remove(edit)
                                editing = null
                            }
                            .padding(vertical = 4.dp),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = ComposeColor(0xFFC62828),
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (color in com.elearning.openbook.reader.Highlight.COLORS) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(ComposeColor(color))
                                .clickable {
                                    highlightStore.recolour(edit, color)
                                    editing = null
                                    editColourOpen = false
                                },
                        )
                    }
                }
            }
        }
    }

    if (showToc && ready != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showToc = false
                scheduleAutoHide()
            },
            sheetState = tocSheetState,
            containerColor = ReaderTheme.Paper,
            contentColor = ReaderTheme.Ink,
        ) {
            TocContent(
                titles = ready.toc.map { it.title to it.pageIndex },
                marks = bookmarks.marks.mapNotNull { mark ->
                    ready.markRow(mark.chapterIndex, mark.offset)?.let { row ->
                        MarkRow(mark, row.chapterTitle, row.pageIndex)
                    }
                },
                highlights = highlightStore.highlights.mapNotNull { highlight ->
                    ready.markRow(highlight.chapterIndex, highlight.startOffset)?.let { row ->
                        HighlightRow(highlight, row.chapterTitle, row.pageIndex)
                    }
                },
                initialTab = if (demo.marks) 1 else 0,
                onJump = { page ->
                    showToc = false
                    // The reader view is the only imperative piece, so drive it directly.
                    jumpToPage(page)
                },
                onRemoveMark = { bookmarks.remove(it) },
                onRemoveHighlight = { highlightStore.remove(it) },
            )
        }
    }

    if (showSearch && ready != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showSearch = false
                scheduleAutoHide()
            },
            sheetState = searchSheetState,
            containerColor = ReaderTheme.Paper,
            contentColor = ReaderTheme.Ink,
        ) {
            SearchSheet(
                engine = ready,
                initialQuery = demo.searchQuery,
                onPick = { page ->
                    showSearch = false
                    jumpToPage(page)
                    scheduleAutoHide()
                },
                onClose = {
                    showSearch = false
                    scheduleAutoHide()
                },
            )
        }
    }

    if (showPlayerSheet && playerOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                showPlayerSheet = false
                scheduleAutoHide()
            },
            sheetState = playerSheetState,
            containerColor = ReaderTheme.Paper,
            contentColor = ReaderTheme.Ink,
        ) {
            AudioPlayerSheet(
                book = book,
                chapter = voiceChapter,
                playing = speechState == SpeechState.PLAYING,
                progress = readProgress,
                page = pageIndex + 1,
                pageCount = ready?.pageCount ?: 0,
                rateLabel = speech.rateLabel,
                canSlowDown = speech.canSlowDown,
                canSpeedUp = speech.canSpeedUp,
                sleepMinutes = sleepMinutes,
                sleepLeftSeconds = sleepLeft,
                voices = speech.voices,
                voiceName = speech.voiceName,
                voiceLabel = speech.voices.firstOrNull { it.name == speech.voiceName }?.label
                    ?: stringResource(R.string.voice_default_name),
                previewing = speech.previewing,
                canPreviousChapter = ready != null && voiceChapterIndex > 0,
                canNextChapter = ready != null &&
                    voiceChapterIndex < (ready?.chapterCount ?: 0) - 1,
                bookLanguage = ready?.bookLanguage
                    ?: LocalConfiguration.current.locales[0],
                onSeek = { fraction ->
                    val source = ready
                    val sentence = source?.sentenceAtBookFraction(fraction) ?: -1
                    if (source != null && sentence >= 0) {
                        // A slider is a hand on the book, like a page turn: the page must
                        // show where it landed even while the voice is paused. It is set
                        // silently, so it does not rewind the voice to the page's first
                        // sentence the way a reported page change would.
                        val page = source.sentencePage(sentence)
                        if (page >= 0) {
                            reader.setPage(page)
                            jumpTarget = -1
                            holder.view?.setPageIndex(page)
                        }
                        speech.skip(sentence)
                        Log.i(TAG, "slider ${(fraction * 100).toInt()}% -> sentence #$sentence (page ${page + 1})")
                    }
                },
                onPreviousChapter = { skipChapter(-1) },
                onNextChapter = { skipChapter(1) },
                onPlayPause = { speech.togglePlayPause() },
                onSlower = { speech.slower() },
                onFaster = { speech.faster() },
                onSleep = { minutes ->
                    com.elearning.openbook.playback.SleepTimer.set(minutes)
                    Log.i(TAG, "sleep timer: ${if (minutes == 0) "off" else "$minutes min"}")
                },
                onVoicePickerOpen = { speech.refreshVoices() },
                onPreview = { voice, sample -> speech.preview(voice, sample) },
                onPreviewStop = { speech.stopPreview() },
                onVoice = { name -> speech.setVoice(name) },
            )
        }
    }

    if (showSettings && !showCustomize) {
        ModalBottomSheet(
            onDismissRequest = {
                showSettings = false
                scheduleAutoHide()
            },
            sheetState = settingsSheetState,
            containerColor = ReaderTheme.Paper,
            contentColor = ReaderTheme.Ink,
        ) {
            AppearanceSheet(
                settings = settings,
                onCustomize = {
                    showSettings = false
                    showCustomize = true
                },
            )
        }
    }

    if (showCustomize) {
        CustomizeScreen(
            settings = settings,
            openFontList = demo.fontList,
            onCancel = {
                showCustomize = false
                scheduleAutoHide()
            },
            onApply = {
                showCustomize = false
                scheduleAutoHide()
            },
        )
    }

    // The guide waits for the first page: shown over a blank spinner it would explain
    // buttons that are not there yet. It is marked seen on open, so a rotation or a
    // second book does not bring it back.
    var guideOpen by remember { mutableStateOf(false) }
    LaunchedEffect(ready != null) {
        if (ready != null && staged == DemoOptions.None && ReaderGuide.pending(context)) {
            ReaderGuide.done(context)
            guideOpen = true
        }
    }
    if (guideOpen) {
        ReaderGuideOverlay(onDone = {
            guideOpen = false
            scheduleAutoHide()
        })
    }
}

/** `Sách · Chương`, as the player's headline, with the chapter the voice is in. */
private fun playerHeadline(
    book: BookItem,
    engine: ReaderEngine?,
    sentence: Int,
): String {
    val chapter = engine?.let { source ->
        if (sentence >= 0) source.chapterTitle(source.chapterOfSentence(sentence)) else ""
    }.orEmpty()
    return if (chapter.isBlank()) book.title else "${book.title} · $chapter"
}

@Composable
private fun ProgressLine(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.001f, 1f))
                .fillMaxHeight()
                .background(ReaderTheme.Accent.copy(alpha = 0.9f)),
        )
    }
}

/**
 * The player, unfurled to the right of the disc: one row in a 48dp capsule, with the
 * headline scrolling if the book and chapter name are too long for it, and the voice's
 * progress drawn as a hairline along the capsule's floor.
 *
 * The four controls are narrower here (32dp) than the toolbar's 44dp, which returns
 * 48dp of the row to the headline; the line stops short of the close button so it runs
 * from the title to the way out rather than under it.
 */
/**
 * The player's slot in the bottom bar, with its unfurl.
 *
 * The pill grows out of the disc rather than fading in whole: it is the *width* that
 * animates, its contents are measured at full size and revealed by the expanding clip,
 * so the headline never reflows while the player opens. It closes back into the disc the
 * same way. (It is a function of its own because `AnimatedVisibility` inside the bar's
 * `Row` would be resolved through `RowScope`, which cannot be reached from the `Box`
 * that owns the weight.)
 */
@Composable
private fun PlayerSlot(visible: Boolean, content: @Composable () -> Unit) {
    // The pill unfurls from the disc: scale from 0.55 pinned to its left edge, the
    // spring the spec asks for (320 ms, damping 0.78). Folding back shrinks to 0.75
    // rather than vanishing.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(CHROME_FADE_MS)) +
            scaleIn(
                animationSpec = PLAYER_SPRING,
                initialScale = 0.55f,
                transformOrigin = TransformOrigin(0f, 0.5f),
            ),
        exit = fadeOut(tween(CHROME_FADE_MS)) +
            scaleOut(
                animationSpec = PLAYER_SPRING,
                targetScale = 0.75f,
                transformOrigin = TransformOrigin(0f, 0.5f),
            ),
    ) {
        content()
    }
}

@Composable
private fun PlayerRow(
    headline: String,
    rateLabel: String,
    playing: Boolean,
    progress: Float,
    canSlowDown: Boolean,
    canSpeedUp: Boolean,
    compact: Boolean,
    onTitle: () -> Unit,
    onSlower: () -> Unit,
    onPlayPause: () -> Unit,
    onFaster: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(PLAYER_ROW_HEIGHT)
            .drawBehind { softRoundShadow(24.dp.toPx()) }
            .clip(RoundedCornerShape(24.dp))
            .background(ReaderTheme.Paper.copy(alpha = 0.92f))
            .border(1.dp, ReaderTheme.Ink.copy(alpha = 0.14f), RoundedCornerShape(24.dp)),
    ) {
        val openPlayer = stringResource(R.string.player_open)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = PLAYER_ROW_INSET, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The title and the speed beside it are one target: tapping anywhere on that
            // part of the capsule opens the full player, and the speed sits right after
            // the title rather than at the far end of the bar.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClickLabel = openPlayer, onClick = onTitle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarqueeText(
                    text = headline,
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = 13.sp,
                    color = ReaderTheme.Ink,
                    scrolling = playing,
                )
                if (!compact) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rateLabel,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = ReaderTheme.InkSoft,
                        maxLines = 1,
                    )
                }
            }
            // Compact (toolbar showing) keeps only play/pause and stop, so the pill
            // sits quietly beside the toolbar; the full set returns with the chrome.
            if (!compact) {
                PanelButton(
                    onClick = onSlower,
                    enabled = canSlowDown,
                    width = PLAYER_BUTTON_WIDTH,
                    height = PLAYER_ROW_HEIGHT,
                    description = stringResource(R.string.player_slower),
                ) {
                    SeekBackGlyph()
                }
            }
            PanelButton(
                onClick = onPlayPause,
                width = PLAYER_BUTTON_WIDTH,
                height = PLAYER_ROW_HEIGHT,
                description = stringResource(if (playing) R.string.pause else R.string.play),
            ) {
                if (playing) PauseGlyph() else PlayGlyph()
            }
            if (!compact) {
                PanelButton(
                    onClick = onFaster,
                    enabled = canSpeedUp,
                    width = PLAYER_BUTTON_WIDTH,
                    height = PLAYER_ROW_HEIGHT,
                    description = stringResource(R.string.player_faster),
                ) {
                    SeekForwardGlyph()
                }
            }
            PanelButton(
                onClick = onClose,
                width = PLAYER_BUTTON_WIDTH,
                height = PLAYER_ROW_HEIGHT,
                description = stringResource(R.string.stop_reading),
            ) { CloseGlyph() }
        }

        // The voice's own progress: how much of the book's text it has read, not how
        // many pages have gone by, so the line creeps along at reading pace instead of
        // jumping a whole width at every turn. It runs the pill's whole width and sits
        // on the pill's own bottom edge: no inset any more, and no spill either — the
        // capsule's 24dp clip rounds its ends off inside the corners, so the line reads
        // as the pill's bottom edge from one end to the other.
        AudioProgressLine(
            fraction = progress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(AUDIO_LINE_HEIGHT),
        )
    }
}

/**
 * The reading progress inside the player: a 2.5dp groove with the part already read in
 * the accent, clipped to a pill at both ends so it reads as one piece of the player.
 */
@Composable
private fun AudioProgressLine(fraction: Float, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(AUDIO_LINE_HEIGHT / 2)
    Box(modifier = modifier.clip(pill).background(ReaderTheme.Ink.copy(alpha = 0.10f))) {
        val read = fraction.coerceIn(0f, 1f)
        if (read > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(read)
                    .fillMaxHeight()
                    .clip(pill)
                    .background(ReaderTheme.Accent.copy(alpha = 0.9f)),
            )
        }
    }
}

/** Reading appearance: a preview of the page as it will be set, and two steppers. */
@Composable
private fun LoadingOverlay() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ReaderTheme.Accent, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.reader_opening),
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = ReaderTheme.InkSoft,
            )
        }
    }
}

@Composable
private fun FailureOverlay(problem: com.elearning.openbook.model.EpubProblem) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_open_failed_title),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ReaderTheme.Ink,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = epubProblemText(problem),
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = ReaderTheme.InkSoft,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** One saved place, resolved for the contents sheet's second tab. */
private data class MarkRow(
    val mark: Bookmark,
    val title: String,
    val page: Int,
)

/**
 * The contents sheet: the book's own table of contents and the reader's marks, in one
 * sheet with two tabs, because both answer the same question — "take me somewhere".
 */
@Composable
private fun TocContent(
    titles: List<Pair<String, Int>>,
    marks: List<MarkRow>,
    highlights: List<HighlightRow>,
    initialTab: Int,
    onJump: (Int) -> Unit,
    onRemoveMark: (Bookmark) -> Unit,
    onRemoveHighlight: (com.elearning.openbook.reader.Highlight) -> Unit,
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    TabRow(
        selectedTabIndex = tab,
        containerColor = ReaderTheme.Paper,
        contentColor = ReaderTheme.Accent,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Tab(selected = tab == 0, onClick = { tab = 0 }) {
            Text(
                text = stringResource(R.string.toc_tab),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        Tab(selected = tab == 1, onClick = { tab = 1 }) {
            Text(
                text = stringResource(R.string.marks_tab, marks.size),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        Tab(selected = tab == 2, onClick = { tab = 2 }) {
            Text(
                text = stringResource(R.string.toc_highlights, highlights.size),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }

    if (tab == 0) {
        if (titles.isEmpty()) {
            SheetNote(stringResource(R.string.toc_empty))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(titles.size) { index ->
                    val (title, page) = titles[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onJump(page) }
                            .padding(horizontal = 24.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp,
                            color = ReaderTheme.Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.page_short, page + 1),
                            fontFamily = FontFamily.Serif,
                            fontSize = 12.sp,
                            color = ReaderTheme.InkSoft,
                        )
                    }
                }
            }
        }
    } else if (tab == 1) {
        if (marks.isEmpty()) {
            SheetNote(stringResource(R.string.marks_empty))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(
                    count = marks.size,
                    key = { index -> "${marks[index].mark.chapterIndex}-${marks[index].mark.offset}" },
                ) { index ->
                    val row = marks[index]
                    // A mark is removed the way a list row is removed everywhere else: by
                    // swiping it away. There is no ✕ any more — the row itself is the
                    // control, and a mis-swipe can simply be swiped back before release.
                    val dismissState = rememberSwipeToDismissBoxState()
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        onDismiss = { direction ->
                            if (direction == SwipeToDismissBoxValue.EndToStart) {
                                onRemoveMark(row.mark)
                            }
                        },
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ReaderTheme.Paper)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = ComposeColor(0xFFC62828),
                                )
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ReaderTheme.Paper)
                                .clickable { onJump(row.page) }
                                .padding(start = 24.dp, end = 24.dp, top = 13.dp, bottom = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.title,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = ReaderTheme.Accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "tr. ${row.page + 1}",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 12.sp,
                                    color = ReaderTheme.InkSoft,
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        if (highlights.isEmpty()) {
            SheetNote(stringResource(R.string.highlights_empty))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(
                    count = highlights.size,
                    key = { index -> highlights[index].highlight.id },
                ) { index ->
                    val row = highlights[index]
                    val dismissState = rememberSwipeToDismissBoxState()
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        onDismiss = { direction ->
                            if (direction == SwipeToDismissBoxValue.EndToStart) {
                                onRemoveHighlight(row.highlight)
                            }
                        },
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ReaderTheme.Paper)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = ComposeColor(0xFFC62828),
                                )
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ReaderTheme.Paper)
                                .clickable { onJump(row.page) }
                                .padding(start = 24.dp, end = 24.dp, top = 13.dp, bottom = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.title,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = ReaderTheme.Accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = row.highlight.text,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 13.sp,
                                    color = ReaderTheme.Ink,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = stringResource(R.string.page_short, row.page + 1),
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 11.sp,
                                    color = ReaderTheme.InkSoft,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

/** A live text selection: page, chapter, character range and its bands. */
private data class TextSelection(
    val page: Int,
    val chapter: Int,
    val start: Int,
    val end: Int,
    val rects: List<android.graphics.RectF>,
)

/** One saved highlight, resolved for the sheet. */
private data class HighlightRow(
    val highlight: com.elearning.openbook.reader.Highlight,
    val title: String,
    val page: Int,
)

/** One tab of the contents sheet. */
@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ReaderTheme.Accent else ReaderTheme.Ink.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Serif,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp,
            color = if (selected) ComposeColor.White else ReaderTheme.Ink,
        )
    }
}

@Composable
private fun SheetNote(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp,
        color = ReaderTheme.InkSoft,
    )
}

/** Quiet time after the last page turn or spoken word that stops the clock. */
private const val IDLE_CUTOFF_MS = 5 * 60 * 1000L

/** A pause this long starts a new reading session in the ledger. */
private const val SESSION_GAP_MS = 2 * 60 * 1000L

private const val SAVE_INTERVAL_MS = 15_000L

/** Chrome hides itself after this, so the page is just paper. */
private const val AUTO_HIDE_MS = 4500L

private const val CHROME_FADE_MS = 250

/** The pill's unfurl: 320 ms with enough damping to settle without overshooting. */
private val PLAYER_SPRING = spring<Float>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

/** The player's frame: the 48dp touch target the accessibility rules ask for. */
private val PLAYER_BUTTON_WIDTH = 36.dp

private val PLAYER_ROW_HEIGHT = 48.dp

/** The capsule's own inset, which is where the headline starts. */
private val PLAYER_ROW_INSET = 14.dp

private val AUDIO_LINE_HEIGHT = 3.dp

/**
 * `Html.fromHtml` has to run on the main thread, so a 1,000-chapter book is converted
 * in small batches with a yield between them: the spinner keeps drawing and Back still
 * works while the pages are being built.
 */
private suspend fun toSpannableInChunks(
    chapters: List<com.elearning.openbook.model.RawChapter>,
    columnWidth: Float,
    maxImageHeight: Float,
): List<android.text.SpannableStringBuilder> {
    val out = ArrayList<android.text.SpannableStringBuilder>(chapters.size)
    for ((index, chapter) in chapters.withIndex()) {
        out += HtmlText.toSpannable(chapter, columnWidth, maxImageHeight)
        if (index % 8 == 7) yield()
    }
    return out
}

/**
 * A picture may take at most this much of the column's height. iOS cuts a plate to the
 * column's width and caps it the same way, so one illustration cannot own the page.
 */
private const val MAX_PICTURE_HEIGHT_FRACTION = 0.55f

/** Time for the harness to reach the page before it starts poking at it. */
private const val DEMO_SETTLE_MS = 1200L

/** Time the harness gives a started voice before it does the next thing (a jump, a pause). */
private const val VOICE_SETTLE_MS = 1600L

/** Matches the reader's own debounce, so a harness size step lands on one typeset. */
private const val RESTYLE_DEBOUNCE_MS = 350L

/** How long the harness lets the voice run before pausing it for the late-cursor test. */
private const val DEMO_PAUSE_MS = 2000L

/** How long a one-line speech status stays over the page before it clears itself. */
private const val SPEECH_NOTE_MS = 4000L

private const val TAG = "ReaderScreen"
