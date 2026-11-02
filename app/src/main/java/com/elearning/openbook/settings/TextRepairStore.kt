package com.elearning.openbook.settings

import android.content.Context

/**
 * Whether the importer repairs the text it copies in. On by default; a reader who wants
 * the book exactly as published can turn it off in app settings.
 */
class TextRepairStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    private companion object {
        const val PREFS = "textRepair"
        const val KEY_ENABLED = "enabled"
    }
}
