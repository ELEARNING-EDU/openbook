package com.elearning.openbook.library

import android.content.Context

/** The shelf's remembered shape: which layout, and which order. */
class LibraryPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var view: LibraryView = LibraryView.of(prefs.getString(KEY_VIEW, null))
        private set

    var sort: LibrarySort = LibrarySort.of(prefs.getString(KEY_SORT, null))
        private set

    fun setView(next: LibraryView) {
        if (next == view) return
        view = next
        prefs.edit().putString(KEY_VIEW, next.key).apply()
    }

    fun setSort(next: LibrarySort) {
        if (next == sort) return
        sort = next
        prefs.edit().putString(KEY_SORT, next.key).apply()
    }

    private companion object {
        const val PREFS = "libraryPreferences"
        const val KEY_VIEW = "view"
        const val KEY_SORT = "sort"
    }
}
