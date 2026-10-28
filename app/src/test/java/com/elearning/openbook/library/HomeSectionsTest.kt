package com.elearning.openbook.library

import com.elearning.openbook.model.BookItem
import com.elearning.openbook.reader.ReadingPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeSectionsTest {

    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 10, 26, 12, 0, 0, 0, zone)
        .toInstant().toEpochMilli()

    private fun book(
        id: String,
        author: String = "Nguyễn Minh An",
        importedAt: Long = 0L,
        finishedAt: Long? = null,
    ) = BookItem(id, "/books/$id", "Sách $id", author, importedAt, finishedAt)

    private fun at(fraction: Float, updatedAt: Long = 0L) = ReadingPosition(
        page = (fraction * 10).toInt(),
        pageCount = 10,
        chapter = 0,
        offset = 0,
        updatedAt = updatedAt,
    )

    @Test
    fun readingNowCapsAtFiveNewestFirstAndExcludesFinishedAndEnds() {
        val books = (1..7).map { book("b$it") } + book("done", finishedAt = now)
        val positions = (1..7).associate { "b$it" to at(0.3f, updatedAt = it.toLong()) } +
            ("done" to at(0.5f, updatedAt = 99)) +
            ("b1" to at(1f, updatedAt = 100))

        val now = HomeSections.readingNow(books, positions)

        assertEquals(5, now.size)
        assertEquals(listOf("b7", "b6", "b5", "b4", "b3"), now.map { it.book.id })
    }

    @Test
    fun addedIsUnreadNewestImportFiveAtMost() {
        val books = (1..7).map { book("b$it", importedAt = it * 10L) }
        val positions = mapOf("b7" to at(0.2f))

        val added = HomeSections.added(books, positions)

        assertEquals(listOf("b6", "b5", "b4", "b3", "b2"), added.map { it.id })
    }

    @Test
    fun suggestionsNeedTwoUnreadBooksByTheSameAuthorAndSkipOtherRows() {
        val seed = book("seed")
        val positions = mapOf("seed" to at(0.4f))
        val sameAuthor = listOf(
            book("s1", importedAt = 20),
            book("s2", importedAt = 10),
        )
        val otherAuthor = book("o1", author = "Ai Khác", importedAt = 30)
        val done = book("done", finishedAt = now)
        val added = book("a1", importedAt = 40)

        val suggestions = HomeSections.suggestions(
            books = listOf(seed, done, added) + sameAuthor + otherAuthor,
            positions = positions,
            readingNow = HomeSections.readingNow(listOf(seed, done, added) + sameAuthor + otherAuthor, positions),
            finished = HomeSections.finished(listOf(done), now, zone),
            added = HomeSections.added(listOf(added, seed), positions),
        )

        assertEquals(listOf("s1", "s2"), suggestions.map { it.id })
    }

    @Test
    fun oneSuggestionIsNotARow() {
        val seed = book("seed")
        val positions = mapOf("seed" to at(0.4f))
        val only = book("s1")
        val content = HomeSections.content(listOf(seed, only) + (1..4).map { book("x$it", author = "Khác") }, positions, now, zone)
        assertTrue(content.suggestions.isEmpty())
    }

    @Test
    fun authorMatchingIgnoresDiacriticsCaseAndSpacing() {
        assertEquals(
            HomeSections.authorKey("Nguyễn Minh An"),
            HomeSections.authorKey("  nguyen   MINH an "),
        )
        assertEquals(HomeSections.authorKey("Đặng Thuỳ Trâm"), HomeSections.authorKey("Dang Thuy Tram"))
    }

    @Test
    fun underSixBooksOnlyKeepsTheCardAndReadingRow() {
        val books = listOf(book("a", importedAt = 1), book("b"))
        val content = HomeSections.content(books, emptyMap(), now, zone)

        assertFalse(content.expanded)
        assertTrue(content.added.isEmpty())
        assertTrue(content.suggestions.isEmpty())
        assertTrue(content.finished.isEmpty())
    }

    @Test
    fun finishedCoversTwelveMonthsNewestFirst() {
        val day: Long = 24 * 60 * 60 * 1000L
        val books = listOf(
            book("this-month", finishedAt = now - 2 * day),
            book("eleven-months", finishedAt = now - 330 * day),
            book("thirteen-months", finishedAt = now - 400 * day),
            book("unfinished"),
        )

        val finished = HomeSections.finished(books, now, zone)

        assertEquals(listOf("this-month", "eleven-months"), finished.map { it.id })
    }

    @Test
    fun finishedBooksLeaveReadingNow() {
        val finished = book("done", finishedAt = now)
        val positions = mapOf("done" to at(0.5f))
        assertTrue(HomeSections.readingNow(listOf(finished), positions).isEmpty())
    }

    @Test
    fun weekIsMondayToSundayWithTodayFlagged() {
        val log = com.elearning.openbook.reader.ReadingLog()
        val monday = LocalDate.of(2026, 10, 26)
        repeat(22 * 60) { log.addSecond(now, listening = false, zone = zone) }

        val week = log.week(monday)

        assertEquals(7, week.size)
        assertEquals(java.time.DayOfWeek.MONDAY, week.first().dayOfWeek)
        assertEquals(java.time.DayOfWeek.SUNDAY, week.last().dayOfWeek)
        assertEquals(22, week.first().minutes)
        assertTrue(week.first().isToday)
        assertFalse(week[1].isToday)
        assertEquals(0, week[1].minutes)
    }

    @Test
    fun finishedThisYearCountsTheCalendarYear() {
        val january = ZonedDateTime.of(2026, 1, 3, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val books = listOf(book("a", finishedAt = january), book("b", finishedAt = now))
        assertEquals(2, HomeSections.finishedThisYear(books, LocalDate.of(2026, 10, 26), zone))
    }
}
