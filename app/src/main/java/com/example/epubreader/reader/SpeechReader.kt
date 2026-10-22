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

/** One voice the device's engine offers, with the short label the picker shows. */
data class VoiceOption(val name: String, val label: String)

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

    /**
     * Counts the times the engine has been restarted. It is part of every utterance id,
     * so a callback from a sentence that was flushed away (a seek, a page turn, a rate
     * change) can be told apart from the sentence now being spoken and dropped. The
     * engine is allowed to deliver those late; the reader must not act on them.
     */
    private var epoch = 0

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

    /**
     * The voices the device offers for Vietnamese, and the one the reader picked.
     *
     * `null` means the engine's own default for the language, which is what the reader
     * has been hearing until now — so a book opened before this feature is unaffected.
     * The choice is persisted, because a reader who picked a voice wants it next time.
     */
    var voices: List<VoiceOption> by mutableStateOf(emptyList())
        private set

    var voiceName: String? by mutableStateOf(prefs.getString(KEY_VOICE, null))
        private set

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
        loadVoices(engine)
        // A voice picked in an earlier session is applied again: the engine forgets it.
        voiceName?.let { saved ->
            val savedVoice = engine.voices?.firstOrNull { it.name == saved }
            if (savedVoice == null) {
                Log.w(TAG, "saved voice $saved is gone; using the language default")
                voiceName = null
                prefs.edit().remove(KEY_VOICE).apply()
            } else {
                Log.i(TAG, "saved voice $saved -> ${engine.setVoice(savedVoice)}")
            }
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
     * Moves the voice's place without starting or stopping it.
     *
     * This is what a page turn or a table-of-contents jump does to a reading voice: the
     * reader's hand outranks the narrator's place, so the voice continues from the page
     * that is now on screen instead of dragging it back. A playing voice restarts at
     * [fromSentence] (a new sentence needs a fresh utterance anyway); a paused one only
     * moves its place, and stays paused.
     */
    fun seek(fromSentence: Int) {
        val src = source ?: return
        if (fromSentence < 0 || fromSentence >= src.sentenceCount) return
        when (stateInternal) {
            SpeechState.IDLE -> Unit
            SpeechState.PLAYING -> {
                // Move the highlight at once, before the engine has been restarted: the
                // reader is looking at the new page now, and a highlight left on the old
                // one for the length of an engine start-up is a page that fights back.
                if (!disposed) onHighlight(fromSentence)
                beginSpeaking(fromSentence)
            }
            SpeechState.PAUSED -> {
                speakStart = fromSentence
                pausedAt = fromSentence
                lastStarted = -1
                nextToEnqueue = fromSentence
                inFlight = 0
                // The highlight stays on the sentence the voice was paused on: a page
                // turned under a paused narrator must not print the band on the page
                // just turned to. The voice still resumes from the new page.
            }
        }
    }

    /**
     * An explicit skip from the full player's back or forward button.
     *
     * It is [seek] with one difference: a paused reader sees the highlight move to the
     * sentence the skip landed on. A turned page deliberately keeps the old band (the
     * reader did not ask to read somewhere else), but a hand on "next" did.
     */
    fun skip(fromSentence: Int) {
        val paused = stateInternal == SpeechState.PAUSED
        seek(fromSentence)
        if (paused && !disposed) onHighlight(fromSentence)
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
        restartFromCurrentPlace()
    }

    fun slower() = setRate(stepRate(rateFactor, -1))

    fun faster() = setRate(stepRate(rateFactor, 1))

    /**
     * Picks a voice, or the engine's default for Vietnamese when [name] is null.
     *
     * Both are engine-wide settings, so a sentence in flight restarts at the new voice
     * — the only way it can be heard without waiting for the next page.
     */
    fun setVoice(name: String?) {
        val engine = tts ?: return
        if (name == voiceName) return
        val voice = name?.let { wanted -> engine.voices?.firstOrNull { it.name == wanted } }
        if (name != null && voice == null) return
        val code = if (voice == null) engine.setLanguage(VI_LOCALE) else engine.setVoice(voice)
        if (name != null && code != TextToSpeech.SUCCESS) {
            Log.w(TAG, "setVoice($name) refused: $code")
            onStatus("Không đặt được giọng đọc (mã $code)")
            return
        }
        voiceName = name
        prefs.edit().apply {
            if (name == null) remove(KEY_VOICE) else putString(KEY_VOICE, name)
        }.apply()
        Log.i(TAG, "voice -> ${name ?: "language default"} (code=$code)")
        restartFromCurrentPlace()
    }

    /** The engine-wide change restart shared by [setRate] and [setVoice]. */
    private fun restartFromCurrentPlace() {
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

    private fun loadVoices(engine: TextToSpeech) {
        val all = runCatching { engine.voices }.getOrNull() ?: return
        val found = all
            .filter { it.locale.language == "vi" || it.locale.language == "vie" }
            .map { VoiceOption(it.name, voiceLabel(it.name)) }
            .distinctBy { it.name }
            .sortedBy { it.label }
        voices = found
        Log.i(TAG, "voices: ${found.size} vi-VN (${found.joinToString { it.name }})")
    }

    /** `vi-vn-x-gft-network` reads as `x gft network` in the picker. */
    private fun voiceLabel(name: String): String =
        name.removePrefix("vi-vn-")
            .removePrefix("vi-VN-")
            .replace('-', ' ')
            .trim()
            .ifBlank { name }

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
        epoch++
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
            val at = epochOf(utteranceId) ?: return
            main.post {
                // The epoch is compared *inside* the post: a callback that was already
                // on its way to this thread when the engine was restarted is stale by
                // the time it runs, and acting on it would hand the reader a sentence
                // from the page it just left.
                if (at != epoch) return@post
                if (!isPlayingNow() || fallbackMode) return@post
                hearingFromEngine = true
                lastStarted = index
                cancelWatchdog()
                onHighlight(index)
            }
        }

        override fun onDone(utteranceId: String?) {
            val index = indexOf(utteranceId) ?: return
            val at = epochOf(utteranceId) ?: return
            main.post {
                if (at != epoch) return@post
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
            val at = epochOf(utteranceId) ?: return
            Log.w(TAG, "onError($errorCode) for sentence $index")
            main.post {
                if (at != epoch) return@post
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

    /** `prefix<epoch>:<sentence>` — the epoch is what makes a stale callback visible. */
    private fun utteranceId(index: Int): String = "$ID_PREFIX$epoch:$index"

    private fun indexOf(utteranceId: String?): Int? =
        utteranceId?.removePrefix(ID_PREFIX)?.substringAfter(':')?.toIntOrNull()

    private fun epochOf(utteranceId: String?): Int? =
        utteranceId?.removePrefix(ID_PREFIX)?.substringBefore(':')?.toIntOrNull()

    private fun viVoices(engine: TextToSpeech): String = try {
        engine.voices
            ?.filter { it.locale.language == "vi" || it.locale.language == "vie" }
            ?.joinToString { "${it.locale}(${it.name})" }
            ?.ifEmpty { "none" } ?: "null"
    } catch (t: Throwable) {
        "err:${t.message}"
    }

    companion object {

        /** The language the reader is read in, everywhere it is named. */
        private val VI_LOCALE: Locale = Locale("vi", "VN")

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
        private const val KEY_VOICE = "narrationVoice"
    }
}
