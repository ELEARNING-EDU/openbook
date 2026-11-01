package com.elearning.openbook.playback

import android.content.Context
import android.util.Log
import com.elearning.openbook.model.BookItem
import com.elearning.openbook.reader.ReaderEngine
import com.elearning.openbook.reader.SpeechReader
import com.elearning.openbook.reader.SpeechState

/**
 * The narration's process-lifetime controls.
 *
 * The reader screen owns the page and the pagination, but Back disposes that
 * composition while the voice keeps reading. The notification's buttons and the sleep
 * timer therefore reach the engine from here, and the reader attaches only the part
 * that needs a page on screen.
 */
object Narration {
    private const val TAG = "Narration"

    @Volatile private var speech: SpeechReader? = null
    @Volatile private var engine: ReaderEngine? = null
    @Volatile private var book: BookItem? = null
    @Volatile private var appContext: Context? = null

    /** Set by the reader while it is on screen, so a skip moves the leaf too. */
    @Volatile var onVoiceMoved: ((Int) -> Unit)? = null

    /** Wires the notification's buttons once, when the process-wide engine is created. */
    fun install(context: Context, reader: SpeechReader) {
        appContext = context.applicationContext
        speech = reader
        PlaybackBridge.onToggle = { toggle() }
        PlaybackBridge.onStop = { stop() }
        PlaybackBridge.onNext = { skipChapter(1) }
        PlaybackBridge.onPrevious = { skipChapter(-1) }
    }

    /** The book and its pagination, attached when the reader has them ready. */
    fun attach(book: BookItem, engine: ReaderEngine?) {
        this.book = book
        this.engine = engine
    }

    fun detachReader() {
        onVoiceMoved = null
    }

    fun toggle() {
        speech?.togglePlayPause()
    }

    fun stop() {
        speech?.stop()
    }

    /**
     * A chapter skip from the notification, with no reader composition to consult:
     * the engine and the voice's own sentence are the whole state.
     */
    fun skipChapter(step: Int) {
        val source = engine ?: return
        val reader = speech ?: return
        val anchor = reader.currentSentence
        if (anchor < 0) return
        var target = source.chapterOfSentence(anchor) + step
        var from = -1
        while (target in 0 until source.chapterCount) {
            from = source.firstSentenceOfChapter(target)
            if (from >= 0) break
            target += step
        }
        if (from < 0) return
        reader.skip(from)
        Log.i(TAG, "notification chapter skip $step -> sentence #$from")
        onVoiceMoved?.invoke(from)
        notify(appContext ?: return)
    }

    /** Called by the engine's process-level hook on every state change. */
    fun onSpeechState(state: SpeechState) {
        val context = appContext ?: return
        val active = state != SpeechState.IDLE
        NowPlaying.book = if (active) book else null
        NowPlaying.playing = state == SpeechState.PLAYING
        if (active) {
            notify(context)
        } else {
            // A finished or stopped voice ends its timer too, whether or not the reader
            // is on screen to see the state change.
            SleepTimer.cancel()
            PlaybackService.stop(context)
        }
    }

    private fun notify(context: Context) {
        val current = book ?: return
        val source = engine
        val anchor = speech?.currentSentence ?: -1
        val chapter = if (source == null || anchor < 0) {
            ""
        } else {
            source.chapterTitle(source.chapterOfSentence(anchor))
        }
        PlaybackService.update(
            context,
            speech?.state == SpeechState.PLAYING,
            current.id,
            current.title,
            chapter,
        )
    }
}
