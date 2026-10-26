package com.example.epubreader

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.epubreader.settings.AppLanguageStore
import com.example.epubreader.ui.DemoOptions
import com.example.epubreader.ui.EpubReaderApp

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * The app's own language is applied here, before any resource is resolved, so the
     * phone's language is only the default: choosing Vietnamese or English overrides the
     * app's resources and leaves the device's setting untouched.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageStore.read(newBase).wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent bars; their icon colour is set from what is on screen — the phone's
        // setting across the shell, the reader's paper inside a book.
        enableEdgeToEdge()

        val autoOpenFirstBook = intent?.getBooleanExtra(EXTRA_DEMO_OPEN_BOOK, false) ?: false
        val demo = DemoOptions(
            page = intent?.getIntExtra(EXTRA_DEMO_PAGE, -1) ?: -1,
            tapX = intent?.getIntExtra(EXTRA_DEMO_TAP_X, -1) ?: -1,
            tapY = intent?.getIntExtra(EXTRA_DEMO_TAP_Y, -1) ?: -1,
            reading = intent?.getBooleanExtra(EXTRA_DEMO_READING, false) ?: false,
            player = intent?.getBooleanExtra(EXTRA_DEMO_PLAYER, false) ?: false,
            settings = intent?.getBooleanExtra(EXTRA_DEMO_SETTINGS, false) ?: false,
            sizeStep = intent?.getIntExtra(EXTRA_DEMO_SIZE_STEP, 0) ?: 0,
            theme = intent?.getStringExtra(EXTRA_DEMO_THEME) ?: "",
            dark = intent?.getIntExtra(EXTRA_DEMO_DARK, -1) ?: -1,
            config = intent?.getStringExtra(EXTRA_DEMO_CONFIG) ?: "",
            customize = intent?.getBooleanExtra(EXTRA_DEMO_CUSTOMIZE, false) ?: false,
            fontList = intent?.getBooleanExtra(EXTRA_DEMO_FONT_LIST, false) ?: false,
            search = intent?.getBooleanExtra(EXTRA_DEMO_SEARCH, false) ?: false,
            searchQuery = intent?.getStringExtra(EXTRA_DEMO_SEARCH_QUERY) ?: "",
            bookmark = intent?.getBooleanExtra(EXTRA_DEMO_BOOKMARK, false) ?: false,
            marks = intent?.getBooleanExtra(EXTRA_DEMO_MARKS, false) ?: false,
            lateCursor = intent?.getIntExtra(EXTRA_DEMO_LATE_CURSOR, -1) ?: -1,
            curl = intent?.getFloatExtra(EXTRA_DEMO_CURL, -1f) ?: -1f,
            curlAngle = intent?.getFloatExtra(EXTRA_DEMO_CURL_ANGLE, 0f) ?: 0f,
            curlBack = intent?.getBooleanExtra(EXTRA_DEMO_CURL_BACK, false) ?: false,
            dragX = intent?.getFloatExtra(EXTRA_DEMO_DRAG_X, 0f) ?: 0f,
            dragY = intent?.getFloatExtra(EXTRA_DEMO_DRAG_Y, 0f) ?: 0f,
            dragSeconds = intent?.getFloatExtra(EXTRA_DEMO_DRAG_SECONDS, 0f) ?: 0f,
            jumpChapter = intent?.getIntExtra(EXTRA_DEMO_JUMP_CHAPTER, -1) ?: -1,
            sleepTimer = intent?.getIntExtra(EXTRA_DEMO_SLEEP, 0) ?: 0,
        )

        setContent {
            EpubReaderApp(autoOpenFirstBook = autoOpenFirstBook, demo = demo)
        }
    }

    private companion object {
        /**
         * Screenshot harness hooks, mirroring the iOS demo build. All inert unless the
         * launching intent carries them:
         *
         * ```
         * adb shell am start -n com.example.epubreader/.MainActivity \
         *   --ez demoOpenBook true --ei demoPage 4 --ez demoPlayer true
         * ```
         */
        const val EXTRA_DEMO_OPEN_BOOK = "demoOpenBook"
        const val EXTRA_DEMO_PAGE = "demoPage"
        const val EXTRA_DEMO_TAP_X = "demoTapX"
        const val EXTRA_DEMO_TAP_Y = "demoTapY"
        const val EXTRA_DEMO_READING = "demoReading"
        const val EXTRA_DEMO_PLAYER = "demoPlayer"
        const val EXTRA_DEMO_SETTINGS = "demoSettings"
        const val EXTRA_DEMO_SIZE_STEP = "demoSizeStep"
        const val EXTRA_DEMO_THEME = "demoTheme"
        const val EXTRA_DEMO_DARK = "demoDark"
        const val EXTRA_DEMO_CONFIG = "demoConfig"
        const val EXTRA_DEMO_CUSTOMIZE = "demoCustomize"
        const val EXTRA_DEMO_FONT_LIST = "demoFontList"
        const val EXTRA_DEMO_SEARCH = "demoSearch"
        const val EXTRA_DEMO_SEARCH_QUERY = "demoSearchQuery"
        const val EXTRA_DEMO_BOOKMARK = "demoBookmark"
        const val EXTRA_DEMO_MARKS = "demoMarks"
        const val EXTRA_DEMO_LATE_CURSOR = "demoLateCursor"
        const val EXTRA_DEMO_CURL = "demoCurl"
        const val EXTRA_DEMO_CURL_ANGLE = "demoCurlAngle"
        const val EXTRA_DEMO_CURL_BACK = "demoCurlBack"
        const val EXTRA_DEMO_DRAG_X = "demoDragX"
        const val EXTRA_DEMO_DRAG_Y = "demoDragY"
        const val EXTRA_DEMO_DRAG_SECONDS = "demoDragSeconds"
        const val EXTRA_DEMO_JUMP_CHAPTER = "demoJumpChapter"
        const val EXTRA_DEMO_SLEEP = "demoSleep"
    }
}
