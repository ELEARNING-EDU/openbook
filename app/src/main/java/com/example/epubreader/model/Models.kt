package com.example.epubreader.model

/** A book sitting in the library: one `.epub` file on disk. */
data class BookItem(
    val id: String,
    val path: String,
    val title: String,
    val author: String,
    /** When the copy entered the library. Written once; editing a title never moves it. */
    val importedAt: Long = 0L,
    /** When the reader marked the book finished, or `null` while it is not. */
    val finishedAt: Long? = null,
)

/** A chapter as read straight out of the ZIP, before HTML conversion. */
data class RawChapter(
    val title: String,
    val href: String,
    val html: String,
    /**
     * The pictures the markup asks for, keyed by the `src` exactly as it was written.
     *
     * Resolved while the ZIP's entries are still in hand, because the importer has no
     * base URL to fetch `images/pic.png` from — see `ChapterImages`.
     */
    val images: Map<String, ByteArray> = emptyMap(),
)

data class RawBook(
    val title: String,
    val author: String,
    val chapters: List<RawChapter>,
    val toc: List<TocEntry>,
    /** `dc:language` as written in the OPF, or blank when the book does not say. */
    val language: String = "",
)

/** A table-of-contents entry resolved to a chapter index. */
data class TocEntry(
    val title: String,
    val chapterIndex: Int,
)

/** What is wrong with a file that will not open; the UI turns this into a local string. */
enum class EpubProblem { NOT_ZIP, NO_CONTAINER, NO_OPF, EMPTY_SPINE, DRM, GENERIC }

class EpubException(
    val problem: EpubProblem,
    message: String = problem.name,
) : Exception(message)
