package com.example.epubreader.library

import com.example.epubreader.model.BookItem
import com.example.epubreader.reader.ReadingPosition
import com.example.epubreader.settings.AppLanguage
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private fun book(
    id: String,
    title: String = id,
    author: String = "Author",
    importedAt: Long = 0L,
) = BookItem(id = id, path = "/books/$id", title = title, author = author, importedAt = importedAt)

private val VI = Locale("vi", "VN")

class BookSortingTest {

    @Test
    fun recentIsNewestImportFirstAndKeepsThatOrderAfterATitleEdit() {
        val old = book("old", title = "Zeta", importedAt = 100)
        val newest = book("new", title = "Alpha", importedAt = 300)
        val middle = book("mid", title = "Mu", importedAt = 200)

        val once = BookSorting.sorted(listOf(old, newest, middle), LibrarySort.RECENT, VI)
        val afterEdit = BookSorting.sorted(
            listOf(old.copy(title = "Aardvark"), newest, middle),
            LibrarySort.RECENT,
            VI,
        )

        assertEquals(listOf("new", "mid", "old"), once.map { it.id })
        // An edit may change the title; it must not change the import order.
        assertEquals(listOf("new", "mid", "old"), afterEdit.map { it.id })
    }

    @Test
    fun recentTiesBreakOnIdSoARenameCannotReshuffleTheShelf() {
        val sameStampA = book("a", title = "Zeta", importedAt = 100)
        val sameStampB = book("b", title = "Alpha", importedAt = 100)

        assertEquals(
            listOf("a", "b"),
            BookSorting.sorted(listOf(sameStampB, sameStampA), LibrarySort.RECENT, VI).map { it.id },
        )
        assertEquals(
            listOf("a", "b"),
            BookSorting.sorted(
                listOf(sameStampB.copy(title = "Zulu"), sameStampA.copy(title = "Aaa")),
                LibrarySort.RECENT,
                VI,
            ).map { it.id },
        )
    }

    @Test
    fun titleOrderIsLocaleCollationAndNotCodePointOrder() {
        // "à" and "a" share a primary weight, so they tie and the import stamp decides:
        // a code-point sort would put "à" after every plain "a" title instead.
        val accented = book("accented", title = "à", importedAt = 200)
        val plainNew = book("plain-new", title = "a", importedAt = 100)
        val b = book("b", title = "b", importedAt = 0)

        val order = BookSorting.sorted(listOf(accented, plainNew, b), LibrarySort.TITLE, VI)

        assertEquals(listOf("accented", "plain-new", "b"), order.map { it.id })
    }

    @Test
    fun titleTiesFallToImportThenId() {
        val a = book("a", title = "Same", author = "One", importedAt = 10)
        val b = book("b", title = "Same", author = "Two", importedAt = 30)
        val c = book("c", title = "Same", author = "One", importedAt = 10)

        val order = BookSorting.sorted(listOf(a, b, c), LibrarySort.TITLE, VI)

        // Same title, so the author decides; only the equal-author pair reaches id/import.
        assertEquals(listOf("a", "c", "b"), order.map { it.id })
        // Reversed input gives the identical order: nothing is left to the sort's stability.
        assertEquals(order.map { it.id }, BookSorting.sorted(listOf(c, b, a), LibrarySort.TITLE, VI).map { it.id })
    }

    @Test
    fun authorOrderPutsTheAuthorFirstAndTheTitleSecond() {
        val b = book("b", title = "First", author = "Beta")
        val a2 = book("a2", title = "Second", author = "Alpha")
        val a1 = book("a1", title = "First", author = "Alpha")

        val order = BookSorting.sorted(listOf(b, a2, a1), LibrarySort.AUTHOR, VI)

        assertEquals(listOf("a1", "a2", "b"), order.map { it.id })
    }

    @Test
    fun unknownStoredValuesFallBackToTheDefaults() {
        assertEquals(LibrarySort.RECENT, LibrarySort.of(null))
        assertEquals(LibrarySort.RECENT, LibrarySort.of("nonsense"))
        assertEquals(LibrarySort.AUTHOR, LibrarySort.of("author"))
        assertEquals(LibraryView.GRID, LibraryView.of(null))
        assertEquals(LibraryView.LIST, LibraryView.of("list"))
    }
}

class BookMetadataCodecTest {

    @Test
    fun roundTripsTitlesThatContainTheSeperators() {
        val metadata = BookMetadata(
            title = "A|B~C\\D\nE",
            author = "Nguyễn ~ Văn | A",
            importedAt = 1_700_000_000_000L,
        )

        assertEquals(metadata, BookMetadataCodec.decode(BookMetadataCodec.encode(metadata)))
    }

    @Test
    fun absentFieldsStayAbsent() {
        val metadata = BookMetadata(importedAt = 5)
        val decoded = BookMetadataCodec.decode(BookMetadataCodec.encode(metadata))

        assertNotNull(decoded)
        assertEquals(5L, decoded!!.importedAt)
        assertNull(decoded.title)
        assertNull(decoded.author)
    }

    @Test
    fun malformedOrMissingValuesDecodeToNull() {
        assertNull(BookMetadataCodec.decode(null))
        assertNull(BookMetadataCodec.decode(""))
        assertNull(BookMetadataCodec.decode("not-a-time|title|author"))
        assertNull(BookMetadataCodec.decode("1|only-two"))
    }
}

class LibrarySelectionTest {

    @Test
    fun toggleCountsAndClearReturnsToAnEmptySelection() {
        val selection = LibrarySelection().toggle("a").toggle("b")
        assertEquals(2, selection.count)
        assertTrue(selection.contains("b"))

        val unticked = selection.toggle("a")
        assertEquals(setOf("b"), unticked.ids)
        assertEquals(0, unticked.clear().count)
    }

    @Test
    fun staleIdsAreNeverHandedToDelete() {
        val selection = LibrarySelection(setOf("a", "gone"))
        assertEquals(listOf("a"), selection.forExisting(listOf("a", "b")))
    }
}

class LibraryFilesTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun deletesOnlyFilesDirectlyInsideTheLibraryAndReportsFailures() {
        val library = folder.newFolder("Books")
        val outside = folder.newFile("original.epub").apply { writeText("original") }
        val copy = File(library, "copy.epub").apply { writeText("copy") }

        val report = LibraryFiles.delete(library, listOf("copy.epub", "missing.epub", "../original.epub"))

        assertEquals(listOf("copy.epub"), report.deleted)
        assertEquals(listOf("missing.epub", "../original.epub"), report.failed)
        assertFalse(copy.exists())
        assertTrue("the reader's own file must survive", outside.exists())
        assertEquals("original", outside.readText())
    }
}

class HomeShelfTest {

    private fun position(page: Int, pageCount: Int, updatedAt: Long) =
        ReadingPosition(page = page, pageCount = pageCount, chapter = 0, offset = 0, updatedAt = updatedAt)

    @Test
    fun showsOnlyBooksGenuinelyPartWayThrough() {
        val books = listOf(book("fresh"), book("reading"), book("done"), book("unknown"))
        val positions = mapOf(
            "reading" to position(4, 10, 20),
            "done" to position(9, 10, 30),
            "unknown" to position(0, 0, 40),
        )

        val shelf = HomeSections.readingNow(books, positions)

        assertEquals(listOf("reading"), shelf.map { it.book.id })
    }

    @Test
    fun mostRecentlyReadComesFirstAndTiesUseTheId() {
        val books = listOf(book("b"), book("a"), book("old"))
        val positions = mapOf(
            "a" to position(1, 10, 50),
            "b" to position(1, 10, 50),
            "old" to position(1, 10, 10),
        )

        assertEquals(listOf("a", "b", "old"), HomeSections.readingNow(books, positions).map { it.book.id })
    }
}

class AppLanguageTest {

    @Test
    fun systemFollowsVietnameseAndTreatsEveryOtherPhoneLanguageAsEnglish() {
        assertEquals("vi", AppLanguage.SYSTEM.locale(Locale("vi", "VN")).language)
        assertEquals("en", AppLanguage.SYSTEM.locale(Locale.US).language)
        assertEquals("en", AppLanguage.SYSTEM.locale(Locale.FRANCE).language)
    }

    @Test
    fun explicitChoicesDoNotFollowThePhone() {
        assertEquals("vi", AppLanguage.VIETNAMESE.locale(Locale.US).language)
        assertEquals("en", AppLanguage.ENGLISH.locale(VI).language)
    }

    @Test
    fun storedKeysMapToTheRightChoice() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.of(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.of("nonsense"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.of("vi"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.of("en"))
    }
}
