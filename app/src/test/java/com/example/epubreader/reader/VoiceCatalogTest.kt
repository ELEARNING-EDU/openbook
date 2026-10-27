package com.example.epubreader.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class VoiceCatalogTest {

    private val vi = Locale("vi", "VN")
    private val en = Locale.US

    private fun voice(name: String, locale: Locale, quality: Int = 300) =
        VoiceOption(name = name, label = VoiceCatalog.label(name), locale = locale, quality = quality)

    @Test
    fun declaredLanguageWinsOverTheSample() {
        assertEquals(
            "en",
            VoiceCatalog.resolveLanguage("en-GB", "Xin chào, đây là tiếng Việt.", vi).language,
        )
    }

    @Test
    fun guessesVietnameseFromItsDiacritics() {
        assertEquals("vi", VoiceCatalog.resolveLanguage("", "Ngọn hải đăng cuối cùng", en).language)
    }

    @Test
    fun guessesJapaneseAndChineseScripts() {
        assertEquals("ja", VoiceCatalog.resolveLanguage("", "これは本です", en).language)
        assertEquals("zh", VoiceCatalog.resolveLanguage("", "这是一本书", en).language)
    }

    @Test
    fun fallsBackToThePhoneWhenNothingIsRecognisable() {
        assertEquals(en, VoiceCatalog.resolveLanguage("", "The last lighthouse", en))
    }

    @Test
    fun offersOnlyTheBooksLanguageWhenTheDeviceHasOne() {
        val voices = listOf(
            voice("vi-vn-x-a", vi),
            voice("en-us-x-b", en),
        )
        assertEquals(listOf("vi-vn-x-a"), VoiceCatalog.forBook(voices, vi).map { it.name })
    }

    @Test
    fun showsEveryVoiceWhenTheBookLanguageHasNone() {
        val voices = listOf(voice("en-us-x-a", en), voice("ja-jp-x-b", Locale.JAPAN))
        assertEquals(2, VoiceCatalog.forBook(voices, Locale.KOREAN).size)
    }

    @Test
    fun aSavedVoiceInAnotherLanguageIsIgnored() {
        val voices = listOf(voice("vi-vn-x-a", vi), voice("vi-vn-x-b", vi))
        val chosen = VoiceCatalog.pickDefault(voices, vi, en, saved = "en-us-x-other")
        assertEquals("vi-vn-x-a", chosen?.name)
    }

    @Test
    fun aSavedVoiceInTheBooksLanguageWins() {
        val voices = listOf(voice("vi-vn-x-a", vi), voice("vi-vn-x-b", vi))
        assertEquals(
            "vi-vn-x-b",
            VoiceCatalog.pickDefault(voices, vi, en, saved = "vi-vn-x-b")?.name,
        )
    }

    @Test
    fun groupsPutTheBookLanguageFirstThenVietnamese() {
        val voices = listOf(
            voice("en-us-x-a", en),
            voice("vi-vn-x-a", vi),
            voice("ja-jp-x-a", Locale.JAPAN),
        )
        val groups = VoiceCatalog.groups(voices, Locale.JAPAN, Locale.US, "")
        assertEquals(Locale.JAPAN, groups.first().locale)
        assertEquals("vi", groups[1].locale.language)
        assertEquals("en", groups.last().locale.language)
    }

    @Test
    fun aLanguageQueryKeepsTheGroupAndANameQueryKeepsTheRow() {
        val voices = listOf(
            voice("vi-vn-x-gft-local", vi),
            voice("vi-vn-x-vic-local", vi),
            voice("en-us-x-iom-local", en),
        )
        val byLanguage = VoiceCatalog.groups(voices, vi, Locale.US, "english")
        assertEquals(listOf("en"), byLanguage.map { it.locale.language })
        assertEquals(1, byLanguage.single().voices.size)

        val byName = VoiceCatalog.groups(voices, vi, Locale.US, "gft")
        assertEquals(1, byName.size)
        assertEquals(listOf("vi-vn-x-gft-local"), byName.single().voices.map { it.name })

        assertTrue(VoiceCatalog.groups(voices, vi, Locale.US, "klingon").isEmpty())
    }

    @Test
    fun labelsDropTheLocalePrefixButKeepTheName() {
        assertEquals("Gft · Network", VoiceCatalog.label("vi-vn-x-gft-network"))
        assertEquals("Iom · Local", VoiceCatalog.label("en-US-x-iom-local"))
        assertFalse(VoiceCatalog.label("vi-vn-x-gft-network").contains("vi-vn"))
        assertEquals("vi-vn", VoiceCatalog.label("vi-vn"))
    }
}
