package com.example.epubreader.model

/** A book sitting in the library: one `.epub` file on disk. */
data class BookItem(
    val id: String,
    val path: String,
    val title: String,
    val author: String,
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
)

/** A table-of-contents entry resolved to a chapter index. */
data class TocEntry(
    val title: String,
    val chapterIndex: Int,
)

class EpubException(message: String) : Exception(message)
