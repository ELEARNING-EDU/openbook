package com.elearning.openbook.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * The app's own language, independent of the device's.
 *
 * [SYSTEM] follows the phone: Vietnamese when the phone is Vietnamese, English for
 * every other locale. The two explicit choices override only this app's resources;
 * nothing here changes the device's language.
 */
enum class AppLanguage(val key: String) {
    SYSTEM("system"),
    VIETNAMESE("vi"),
    ENGLISH("en"),
    ;

    fun locale(system: Locale = Locale.getDefault()): Locale = when (this) {
        SYSTEM -> if (system.language == "vi") VIETNAMESE.locale() else ENGLISH.locale()
        VIETNAMESE -> Locale("vi", "VN")
        ENGLISH -> Locale.ENGLISH
    }

    /** Wraps an activity's base context so its resources load this language. */
    fun wrap(base: Context): Context {
        if (this == SYSTEM) return base
        val locale = locale()
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }

    companion object {
        fun of(raw: String?): AppLanguage = entries.firstOrNull { it.key == raw } ?: SYSTEM
    }
}

/** Where the reader's language choice lives; read before the activity is attached. */
class AppLanguageStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var value: AppLanguage = AppLanguage.of(prefs.getString(KEY_LANGUAGE, null))
        private set

    /**
     * Persists the choice. The caller recreates the activity (or leaves it to the next
     * launch), because resources are resolved from the configuration at attach time.
     */
    fun set(next: AppLanguage) {
        if (next == value) return
        value = next
        prefs.edit().putString(KEY_LANGUAGE, next.key).apply()
    }

    companion object {
        private const val PREFS = "appLanguage"
        private const val KEY_LANGUAGE = "language"

        fun read(context: Context): AppLanguage =
            AppLanguage.of(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_LANGUAGE, null),
            )
    }
}
