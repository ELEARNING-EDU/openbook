package com.example.epubreader.library

/**
 * The shelf's multi-select state: which books are ticked.
 *
 * Keeping it as a value makes the two destructive rules testable without a device:
 * cancel restores nothing but an empty set, and the delete list is exactly what the
 * header counted.
 */
data class LibrarySelection(val ids: Set<String> = emptySet()) {

    val count: Int get() = ids.size

    fun contains(id: String): Boolean = id in ids

    fun toggle(id: String): LibrarySelection =
        if (id in ids) copy(ids = ids - id) else copy(ids = ids + id)

    fun clear(): LibrarySelection = LibrarySelection()

    /** Only ids still on the shelf are deletable, so a stale tick cannot hit a file. */
    fun forExisting(books: Collection<String>): List<String> = ids.filter { it in books }
}
