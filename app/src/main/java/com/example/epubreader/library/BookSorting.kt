package com.example.epubreader.library

import com.example.epubreader.model.BookItem
import java.text.Collator
import java.util.Locale

/** How the shelf draws a book: a cover grid or a row per book. */
enum class LibraryView(val key: String) {
    GRID("grid"),
    LIST("list"),
    ;

    companion object {
        fun of(raw: String?): LibraryView = entries.firstOrNull { it.key == raw } ?: GRID
    }
}

/** The three orders the shelf offers. */
enum class LibrarySort(val key: String) {
    RECENT("recent"),
    TITLE("title"),
    AUTHOR("author"),
    ;

    companion object {
        fun of(raw: String?): LibrarySort = entries.firstOrNull { it.key == raw } ?: RECENT
    }
}

/**
 * The shelf's one ordering rule, shared by every layout.
 *
 * Text is compared with a [Collator] for the reader's locale, so Vietnamese sorts
 * `Đ` after `D` and `Á` next to `A` rather than by code point. Every order ends on the
 * book id, so two books with the same title and author never swap places between
 * recompositions. "Recently imported" is keyed on the original import stamp, never on
 * the title, which is what keeps an edit from reshuffling the shelf.
 */
object BookSorting {

    fun sorted(
        books: List<BookItem>,
        sort: LibrarySort,
        locale: Locale = Locale.getDefault(),
    ): List<BookItem> {
        val text = textOrder(locale)
        return when (sort) {
            LibrarySort.RECENT -> books.sortedWith(
                compareByDescending<BookItem> { it.importedAt }
                    .thenBy { it.id },
            )

            LibrarySort.TITLE -> books.sortedWith(
                text
                    .thenByDescending { it.importedAt }
                    .thenBy { it.id },
            )

            LibrarySort.AUTHOR -> books.sortedWith(
                authorOrder(locale)
                    .thenByDescending { it.importedAt }
                    .thenBy { it.id },
            )
        }
    }

    /** Title first, then author, both by the locale's own collation. */
    private fun textOrder(locale: Locale): Comparator<BookItem> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        return Comparator { a, b ->
            val title = collator.compare(a.title, b.title)
            if (title != 0) title else collator.compare(a.author, b.author)
        }
    }

    /** Author first, then title, both by the locale's own collation. */
    private fun authorOrder(locale: Locale): Comparator<BookItem> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        return Comparator { a, b ->
            val author = collator.compare(a.author, b.author)
            if (author != 0) author else collator.compare(a.title, b.title)
        }
    }
}
