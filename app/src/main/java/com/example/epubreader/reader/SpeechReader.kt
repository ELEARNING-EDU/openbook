package com.example.epubreader.reader

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** What the voice is doing. Drives the disc, the play/pause button and the highlight. */
enum class SpeechState { IDLE, PLAYING, PAUSED }

/**
 * Reads a paginated book aloud, one sentence at a time.
 *
 * The engine is fed sentence by sentence through `QUEUE_ADD`, each utterance tagged
 * with an id that carries its global sentence index. `onStart` is the highlight
 * signal — every engine reports it, unlike `onRangeStart`, which is optional. When a
 * sentence finishes, the next one is pulled into the queue, so playback runs on
 * across page and chapter boundaries.
 *
 * The Android engine has no pause of its own, so [pause] stops it and remembers the
 * sentence: resuming restarts that sentence, and the highlight is left exactly where
 * it was, because the reader is paused mid-sentence rather than finished with it.
 *
 * If the engine never reports `onStart` (for example an emulator with no usable
 * audio device), the reader says so and falls back to walking the highlight on a
 * timer, so the pagination/highlight logic can still be demonstrated.
 *
 * Created and destroyed with the reader screen; [shutdown] must be called on the way
 * out so the engine's service connection is released.
 */
class SpeechReader(private val context: Context) {

    /** The book to read. Set once pagination is ready. */
    var source: SentenceSource? = null

    /** Global sentence index that should be highlighted, or -1. */
    var onHighlight: (Int) -> Unit = {}

    /** Fires whenever the voice changes between idle, playing and paused. */
    var onStateChanged: (SpeechState) -> Unit = {}

    /** Honest one-line status for the UI log / report. */
    var onStatus: (String) -> Unit = {}

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var tts: TextToSpeech? = TextToSpeech(context, TextToSpeech.OnInitListener { handleInit(it) })
    private var ready = false
    private var disposed = false

    /**
     * Compose state, so the disc, the play/pause glyph and the settings sheet follow
     * the voice without any callback wiring of their own. Every write happens on the
     * main thread.
     */
    private var stateInternal by mutableStateOf(SpeechState.IDLE)
    private var hearingFromEngine = false
    private var fallbackMode = false

    private var speakStart = 0
    private var nextToEnqueue = 0
    private var inFlight = 0
    private var lastStarted = -1

    /** Where a pause left off, so resuming restarts that sentence, not the page. */
    private var pausedAt = -1

    /**
     * Reading speed, as a multiple of normal (the iOS build's rate factor). Persisted,
     * because a reader who slows the voice down wants it slow next time too.
     */
    var rateFactor: Float by mutableFloatStateOf(
        prefs.getFloat(KEY_RATE, DEFAULT_RATE).takeIf { rateOptions.contains(it) } ?: DEFAULT_RATE
    )
        private set

    val state: SpeechState get() = stateInternal

    /** True while reading or paused — the disc is on screen for both. */
    val isActive: Boolean get() = stateInternal != SpeechState.IDLE

    val isPlaying: Boolean get() = stateInternal == SpeechState.PLAYING

    val rateLabel: String get() = "${ReaderSettings.trimNumber(rateFactor)}×"
    val canSlowDown: Boolean get() = rateFactor > rateOptions.first()
    val canSpeedUp: Boolean get() = rateFactor < rateOptions.last()

    private val main = Handler(Looper.getMainLooper())
    private var watchdog: Runnable? = null
    private var fallbackWalk: Runnable? = null

    // --------------------------------------------------------------- lifecycle

    private fun handleInit(status: Int) {
        if (disposed) return
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech init failed: $status")
            onStatus("Không khởi tạo được TextToSpeech (mã $status)")
            return
        }
        val engine = tts ?: return
        val result = engine.setLanguage(Locale("vi", "VN"))
        val voiceOk = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        Log.i(
            TAG,
            "setLanguage(vi-VN) -> $result (voiceOk=$voiceOk); vi voices: ${viVoices(engine)}",
        )
        if (!voiceOk) {
            onStatus("Thiếu dữ liệu giọng tiếng Việt (mã $result) — đọc bằng giọng mặc định")
        }
        engine.setSpeechRate(rateFactor)
        engine.setOnUtteranceProgressListener(progress)
        ready = true
        // A tap can land before the engine is up; honour it now.
        pendingStart?.let { pendingStart = null; beginSpeaking(it) }
    }

    /** Call from `onDispose` of the reader screen. */
    fun shutdown() {
        disposed = true
        cancelWatchdog()
        stopFallback()
        main.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        stateInternal = SpeechState.IDLE
    }

    // ------------------------------------------------------------------ control

    private var pendingStart: Int? = null

    /** Starts reading at [fromSentence] (global index), or switches to it if already reading. */
    fun start(fromSentence: Int) {
        val src = source ?: return
        if (fromSentence < 0 || fromSentence >= src.sentenceCount) return
        if (!ready || tts == null) {
            pendingStart = fromSentence
            onStatus("Đang khởi tạo TextToSpeech…")
            return
        }
        beginSpeaking(fromSentence)
    }

    /** Stops reading and clears the highlight. */
    fun stop() {
        tts?.stop()
        pausedAt = -1
        finishSpeaking()
    }

    /**
     * Stops the engine and holds the place. The highlight is left on the sentence
     * being spoken, so a paused page looks paused rather than finished.
     */
    fun pause() {
        if (stateInternal != SpeechState.PLAYING) return
        pausedAt = if (lastStarted >= 0) lastStarted else speakStart
        tts?.stop()
        cancelWatchdog()
        stopFallback()
        fallbackMode = false
        inFlight = 0
        setState(SpeechState.PAUSED)
    }

    fun resume() {
        if (stateInternal != SpeechState.PAUSED) return
        beginSpeaking(if (pausedAt >= 0) pausedAt else speakStart)
    }

    fun togglePlayPause() {
        when (stateInternal) {
            SpeechState.PLAYING -> pause()
            SpeechState.PAUSED -> resume()
            SpeechState.IDLE -> Unit
        }
    }

    /**
     * Rate can only be applied to a fresh utterance, so the sentence being read
     * restarts at the new speed; a paused reader stays paused.
     */
    fun setRate(factor: Float) {
        val clamped = factor.coerceIn(rateOptions.first(), rateOptions.last())
        if (clamped == rateFactor) return
        rateFactor = clamped
        prefs.edit().putFloat(KEY_RATE, clamped).apply()
        if (stateInternal == SpeechState.IDLE) return
        val wasPaused = stateInternal == SpeechState.PAUSED
        val from = when {
            stateInternal == SpeechState.PLAYING && lastStarted >= 0 -> lastStarted
            pausedAt >= 0 -> pausedAt
            else -> speakStart
        }
        beginSpeaking(from)
        if (wasPaused) pause()
    }

    fun slower() = setRate(stepRate(rateFactor, -1))

    fun faster() = setRate(stepRate(rateFactor, 1))

    private fun stepRate(current: Float, delta: Int): Float {
        val index = rateOptions.indexOf(current).takeIf { it >= 0 } ?: 0
        return rateOptions[(index + delta).coerceIn(0, rateOptions.size - 1)]
    }

    private fun setState(next: SpeechState) {
        if (stateInternal == next) return
        stateInternal = next
        if (!disposed) onStateChanged(next)
    }

    private fun beginSpeaking(from: Int) {
        val engine = tts ?: return
        pendingStart = null
        fallbackMode = false
        speakStart = from
        pausedAt = from
        lastStarted = -1
        nextToEnqueue = from
        inFlight = 0
        engine.setSpeechRate(rateFactor)
        setState(SpeechState.PLAYING)
        engine.stop()
        pump(flushFirst = true)
        if (inFlight == 0) {
            // Everything from here on is blank; nothing to say.
            finishSpeaking()
            return
        }
        armWatchdog()
    }

    /** Keeps up to [LOOKAHEAD] sentences queued so playback does not stutter. */
    private fun pump(flushFirst: Boolean) {
        val src = source ?: return
        val engine = tts ?: return
        var first = flushFirst
        while (inFlight < LOOKAHEAD && nextToEnqueue < src.sentenceCount) {
            val index = nextToEnqueue++
            val text = src.sentenceText(index)
            if (text.isBlank()) continue
            val mode = if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            first = false
            val code = engine.speak(text, mode, null, utteranceId(index))
            if (code == TextToSpeech.ERROR) {
                Log.w(TAG, "speak() refused sentence $index")
                onStatus("Engine từ chối câu $index")
                continue
            }
            inFlight++
        }
    }

    private fun onSentenceSpoken() {
        inFlight = (inFlight - 1).coerceAtLeast(0)
        pump(flushFirst = false)
        val src = source
        if (inFlight == 0 && (src == null || nextToEnqueue >= src.sentenceCount)) {
            finishSpeaking()
        }
    }

    private fun finishSpeaking() {
        if (stateInternal == SpeechState.IDLE && lastStarted < 0 && !fallbackMode) return
        cancelWatchdog()
        stopFallback()
        fallbackMode = false
        inFlight = 0
        lastStarted = -1
        pausedAt = -1
        setState(SpeechState.IDLE)
        if (!disposed) onHighlight(-1)
    }

    // --------------------------------------------------------------- listeners

    private fun isPlayingNow(): Boolean = !disposed && stateInternal == SpeechState.PLAYING

    private val progress = object : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            val index = indexOf(utteranceId) ?: return
            main.post {
                if (!isPlayingNow() || fallbackMode) return@post
                hearingFromEngine = true
                lastStarted = index
                cancelWatchdog()
                onHighlight(index)
            }
        }

        override fun onDone(utteranceId: String?) {
            val index = indexOf(utteranceId) ?: return
            main.post {
                if (!isPlayingNow() || fallbackMode) return@post
                if (index < lastStarted) return@post
                onSentenceSpoken()
            }
        }

        /**
         * `stop()` reports through here, and the framework's default would call
         * `onDone` — which would look like the sentence ended and let the queue run
         * on. A stop is ours to interpret, so this stays empty.
         */
        override fun onStop(utteranceId: String?, interrupted: Boolean) = Unit

        @Deprecated("Kept for engines that still call the older signature.")
        override fun onError(utteranceId: String?) {
            onError(utteranceId, TextToSpeech.ERROR)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            val index = indexOf(utteranceId) ?: return
            Log.w(TAG, "onError($errorCode) for sentence $index")
            main.post {
                if (!isPlayingNow() || fallbackMode) return@post
                onStatus("Lỗi engine ở câu $index (mã $errorCode)")
                onSentenceSpoken()
            }
        }
    }

    // ------------------------------------------------------ watchdog / fallback

    /**
     * Some engines (and emulators with no audio device) accept `speak()` and then
     * never call back. After [WATCHDOG_MS] with no `onStart`, demonstrate the
     * highlight by walking it on a timer instead.
     */
    private fun armWatchdog() {
        cancelWatchdog()
        if (hearingFromEngine) return
        val runnable = Runnable {
            if (disposed || stateInternal != SpeechState.PLAYING || hearingFromEngine) return@Runnable
            Log.w(TAG, "no onStart within ${WATCHDOG_MS}ms — falling back to timed highlight walk")
            onStatus("Engine không phát tín hiệu — tô sáng mô phỏng (chưa kiểm chứng được âm thanh)")
            fallbackMode = true
            startFallback()
        }
        watchdog = runnable
        main.postDelayed(runnable, WATCHDOG_MS)
    }

    private fun cancelWatchdog() {
        watchdog?.let { main.removeCallbacks(it) }
        watchdog = null
    }

    private fun startFallback() {
        val src = source ?: return
        var index = if (lastStarted >= 0) lastStarted else speakStart
        if (index < 0 || index >= src.sentenceCount) {
            finishSpeaking()
            return
        }
        onHighlight(index)
        val runnable = object : Runnable {
            override fun run() {
                if (disposed || stateInternal != SpeechState.PLAYING || !fallbackMode) return
                index++
                if (index >= src.sentenceCount) {
                    finishSpeaking()
                    return
                }
                onHighlight(index)
                main.postDelayed(this, FALLBACK_MS)
            }
        }
        fallbackWalk = runnable
        main.postDelayed(runnable, FALLBACK_MS)
    }

    private fun stopFallback() {
        fallbackWalk?.let { main.removeCallbacks(it) }
        fallbackWalk = null
    }

    // ------------------------------------------------------------------ helpers

    private fun utteranceId(index: Int): String = "$ID_PREFIX$index"

    private fun indexOf(utteranceId: String?): Int? =
        utteranceId?.removePrefix(ID_PREFIX)?.toIntOrNull()

    private fun viVoices(engine: TextToSpeech): String = try {
        engine.voices
            ?.filter { it.locale.language == "vi" || it.locale.language == "vie" }
            ?.joinToString { "${it.locale}(${it.name})" }
            ?.ifEmpty { "none" } ?: "null"
    } catch (t: Throwable) {
        "err:${t.message}"
    }

    companion object {

        /** The speeds the player steps through, matching the iOS reader. */
        val rateOptions: List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)

        const val DEFAULT_RATE: Float = 1.0f

        private const val TAG = "SpeechReader"

        /** Two sentences in flight: enough to bridge the gap between utterances. */
        private const val LOOKAHEAD = 2

        private const val WATCHDOG_MS = 1500L

        /** Timed-walk pace, roughly one short sentence. */
        private const val FALLBACK_MS = 1400L

        private const val ID_PREFIX = "s"

        private const val PREFS = "readerSettings"
        private const val KEY_RATE = "narrationRateFactor"
    }
}
