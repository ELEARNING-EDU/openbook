package com.elearning.openbook

import android.app.Application
import com.elearning.openbook.settings.ReviewPromptStore
import dagger.hilt.android.HiltAndroidApp

/** The Hilt entry point: one graph for the whole app. */
@HiltAndroidApp
class ReaderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        reviewDue = ReviewPromptStore(this).countLaunch()
    }

    companion object {
        /**
         * True on the launch that has to ask for a review: counted here because this is
         * where a launch happens, and read once by [MainActivity] before its content is set.
         */
        var reviewDue: Boolean = false
            private set

        /** Reads and clears the flag, so a recreated activity does not ask again. */
        fun consumeReviewDue(): Boolean {
            val due = reviewDue
            reviewDue = false
            return due
        }
    }
}
