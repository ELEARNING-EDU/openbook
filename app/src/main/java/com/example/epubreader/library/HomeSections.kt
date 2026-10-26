package com.example.epubreader.library

import com.example.epubreader.model.BookItem
import com.example.epubreader.reader.ReadingPosition
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/** Everything Home draws under the achievement card, already filtered and cut to five. */
data class HomeContent(
    val readingNow: List<InProgressBook>,
    /** False under six books: Home then shows only the card and the reading row. */
    val expanded: Boolean,
    val added: List<BookItem>,
    val suggestions: List<BookItem>,
    val finished: List<BookItem>,
)

data class InProgressBook(val book: BookItem, val position: ReadingPosition)

/**
 * Home's five rows, as plain data.
 *
 * The rules are the spec's, not the screen's: five at most, empty rows stay empty (the
 * UI hides them), suggestions only between the same author and only when there are at
 * least two, and a finished book never appears under "reading now".
 */
object HomeSections {

    const val ROW_LIMIT = 5
    private const val SUGGESTION_MIN = 2
    private const val FINISHED_WINDOW_MONTHS = 12L

    fun content(
        books: List<BookItem>,
        positions: Map<String, ReadingPosition>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): HomeContent {
        val expanded = books.size >= 6
        val readingNow = readingNow(books, positions)
        if (!expanded) {
            return HomeContent(readingNow, expanded = false, emptyList(), emptyList(), emptyList())
        }

        val finished = finished(books, nowMillis, zone)
        val added = added(books, positions)
        val suggestions = suggestions(
            books = books,
            positions = positions,
            readingNow = readingNow,
            finished = finished,
            added = added,
        )
        return HomeContent(readingNow, expanded = true, added, suggestions, finished)
    }

    fun readingNow(
        books: List<BookItem>,
        positions: Map<String, ReadingPosition>,
    ): List<InProgressBook> = books.mapNotNull { book ->
        if (book.finishedAt != null) return@mapNotNull null
        val position = positions[book.id] ?: return@mapNotNull null
        if (position.fraction <= 0f || position.fraction >= 1f) return@mapNotNull null
        InProgressBook(book, position)
    }.sortedWith(
        compareByDescending<InProgressBook> { it.position.updatedAt }
            .thenBy { it.book.id },
    ).take(ROW_LIMIT)

    /** Unread (no place, or still at the cover), not finished, newest import first. */
    fun added(
        books: List<BookItem>,
        positions: Map<String, ReadingPosition>,
    ): List<BookItem> = books.filter { book ->
        book.finishedAt == null && (positions[book.id]?.fraction ?: 0f) <= 0f
    }.sortedWith(
        compareByDescending<BookItem> { it.importedAt }.thenBy { it.id },
    ).take(ROW_LIMIT)

    fun finished(
        books: List<BookItem>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<BookItem> {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val oldest = now.minusMonths(FINISHED_WINDOW_MONTHS).atStartOfDay(zone).toInstant().toEpochMilli()
        return books.filter { book ->
            val stamp = book.finishedAt ?: return@filter false
            stamp >= oldest && stamp <= nowMillis
        }.sortedByDescending { it.finishedAt }.take(ROW_LIMIT)
    }

    /**
     * Unread books by an author the reader has just been reading: the three most recent
     * "reading now" and finished books decide which authors count. Books already on the
     * added or finished rows are not repeated, and fewer than two matches hides the row.
     */
    fun suggestions(
        books: List<BookItem>,
        positions: Map<String, ReadingPosition>,
        readingNow: List<InProgressBook> = readingNow(books, positions),
        finished: List<BookItem> = finished(books),
        added: List<BookItem> = added(books, positions),
    ): List<BookItem> {
        val seeds = (readingNow.map { it.book to it.position.updatedAt } +
            finished.map { it to (it.finishedAt ?: 0L) })
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.id }
            .take(3)
        if (seeds.isEmpty()) return emptyList()

        val authors = seeds.map { authorKey(it.author) }.filter { it.isNotEmpty() }.toSet()
        if (authors.isEmpty()) return emptyList()

        val excluded = (added.map { it.id } + finished.map { it.id }).toSet()
        val candidates = books.filter { book ->
            book.id !in excluded &&
                book.finishedAt == null &&
                (positions[book.id]?.fraction ?: 0f) <= 0f &&
                authorKey(book.author) in authors
        }.sortedWith(
            compareByDescending<BookItem> { it.importedAt }.thenBy { it.id },
        ).take(ROW_LIMIT)

        return if (candidates.size >= SUGGESTION_MIN) candidates else emptyList()
    }

    /**
     * "Nguyễn Minh An" == "Nguyen Minh An" == "nguyen  minh an": diacritics dropped,
     * case folded, whitespace collapsed, so a Vietnamese name matches its ASCII typing.
     */
    fun authorKey(author: String): String {
        val folded = Normalizer.normalize(author, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase(Locale.ROOT)
        return folded.trim().replace(Regex("\\s+"), " ")
    }

    /** Days since the finished mark, for the 12-month window in tests. */
    fun finishedThisYear(books: List<BookItem>, today: LocalDate, zone: ZoneId): Int =
        books.count { book ->
            val stamp = book.finishedAt ?: return@count false
            Instant.ofEpochMilli(stamp).atZone(zone).toLocalDate().year == today.year
        }

    /** Percentage for the shelf, 0…100. */
    fun percent(fraction: Float): Int = (fraction * 100f).roundToInt()
}
