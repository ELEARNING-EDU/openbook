package com.elearning.openbook.reader

import android.content.Context
import com.elearning.openbook.playback.Narration

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
            instance ?: SpeechReader(context.applicationContext).also { reader ->
                // The notification and the sleep timer follow the voice for the life of
                // the process, not the life of the reader's composition.
                Narration.install(context, reader)
                reader.addStateHook(Narration::onSpeechState)
                instance = reader
            }
        }
}
