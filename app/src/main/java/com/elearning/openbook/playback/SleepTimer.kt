package com.elearning.openbook.playback

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The sleep timer, owned by the process rather than the reader screen.
 *
 * Back disposes the reader but not the voice, so a timer that lived in the composition
 * used to vanish with it and let the narration run past its deadline. The deadline is
 * absolute ([SystemClock.elapsedRealtime]) and the countdown is exposed as Compose
 * state, so reopening the player shows the remaining time instead of starting over.
 */
object SleepTimer {

    private const val TAG = "SleepTimer"

    /** Minutes the timer was set for; 0 when it is off. */
    var minutes by mutableIntStateOf(0)
        private set

    /** Seconds left on the clock, for the sheet's countdown. */
    var leftSeconds by mutableIntStateOf(0)
        private set

    /** True once a timer has expired, until the reader has shown its note. */
    var justFinished by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    fun set(requested: Int) {
        cancel()
        if (requested <= 0) return
        minutes = requested
        leftSeconds = requested * 60
        justFinished = false
        val deadline = SystemClock.elapsedRealtime() + requested * 60_000L
        job = scope.launch {
            while (true) {
                val left = ((deadline - SystemClock.elapsedRealtime()) / 1000L).toInt()
                leftSeconds = left.coerceAtLeast(0)
                if (left <= 0) break
                delay(500)
            }
            minutes = 0
            leftSeconds = 0
            justFinished = true
            Log.i(TAG, "sleep timer: ${requested}min up -> stopping the voice")
            // Stopping the voice also clears the notification through Narration.
            Narration.stop()
        }
    }

    /** Called when the voice goes idle: a timer belongs to the voice that was running. */
    fun cancel() {
        job?.cancel()
        job = null
        minutes = 0
        leftSeconds = 0
    }

    fun acknowledge() {
        justFinished = false
    }
}
