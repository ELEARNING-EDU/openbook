package com.elearning.openbook.library

import android.content.Context
import com.elearning.openbook.reader.ReadingLog

/** Local storage for the reading ledger and today's goal. */
class ReadingLogStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("readingLog", Context.MODE_PRIVATE)

    fun load(): ReadingLog = ReadingLog.decode(prefs.getString(KEY_DAYS, null))

    fun save(log: ReadingLog) {
        prefs.edit().putString(KEY_DAYS, log.encode()).apply()
    }

    var goalMinutes: Int
        get() = prefs.getInt(KEY_GOAL, DEFAULT_GOAL)
        set(value) {
            prefs.edit().putInt(KEY_GOAL, value).apply()
        }

    companion object {
        const val DEFAULT_GOAL = 5
        val GOAL_CHOICES = listOf(1, 5, 10, 15, 30, 60, 120)
        private const val KEY_DAYS = "days"
        private const val KEY_GOAL = "goalMinutes"
    }
}
