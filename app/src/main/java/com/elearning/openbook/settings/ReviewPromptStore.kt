package com.elearning.openbook.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Counts app launches so the review sheet is asked for once, on the tenth.
 *
 * Launches are counted where a process starts, not where a screen resumes, so turning the
 * phone or coming back from a book does not inflate the count.
 */
class ReviewPromptStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
    )

    /**
     * Counts one launch; true when a review is due. The flag is only set by [markAsked],
     * when the sheet is really requested, so a screenshot launch cannot use up the ask.
     */
    fun countLaunch(): Boolean {
        val launches = prefs.getInt(KEY_LAUNCHES, 0) + 1
        prefs.edit().putInt(KEY_LAUNCHES, launches).apply()
        return shouldAsk(launches, prefs.getBoolean(KEY_ASKED, false))
    }

    fun markAsked() {
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
    }

    companion object {
        const val LAUNCHES_BEFORE_ASK = 10

        /**
         * The threshold as a plain rule, so it can be checked without a phone. Asking is
         * once in a lifetime: Play throttles the sheet itself, but a flag keeps the app
         * from calling it again on every later release.
         */
        fun shouldAsk(launches: Int, asked: Boolean): Boolean =
            launches >= LAUNCHES_BEFORE_ASK && !asked

        private const val PREFS = "reviewPrompt"
        private const val KEY_LAUNCHES = "launches"
        private const val KEY_ASKED = "asked"
    }
}
