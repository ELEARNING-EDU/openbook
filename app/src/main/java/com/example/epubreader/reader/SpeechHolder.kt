package com.example.epubreader.reader

import android.content.Context

/**
 * The one narration engine for the process. It outlives the reader screen so Back does
 * not cut the voice off: the notification (and the reader, when reopened) keep driving
 * the same engine.
 */
object SpeechHolder {
    @Volatile
    private var instance: SpeechReader? = null

    fun get(context: Context): SpeechReader =
        instance ?: synchronized(this) {
            instance ?: SpeechReader(context.applicationContext).also { instance = it }
        }
}
