package com.example.epubreader.library

import java.io.File

/**
 * The one place a library file may be deleted from.
 *
 * The original EPUB the reader picked through the system picker is never touched: only
 * the copy inside the app's own `Books/` folder can resolve, and a resolved file must be
 * a direct child of that folder. A failure on one file is reported rather than aborting
 * the rest, so the other books stay intact.
 */
object LibraryFiles {

    data class Report(val deleted: List<String>, val failed: List<String>)

    fun resolve(directory: File, id: String): File? {
        val root = runCatching { directory.canonicalFile }.getOrNull() ?: return null
        val candidate = runCatching { File(root, id).canonicalFile }.getOrNull() ?: return null
        return candidate.takeIf { it.parentFile == root && it.isFile }
    }

    fun delete(directory: File, ids: Collection<String>): Report {
        val deleted = ArrayList<String>(ids.size)
        val failed = ArrayList<String>(0)
        for (id in ids) {
            val file = resolve(directory, id)
            val removed = runCatching { file?.delete() == true }.getOrDefault(false)
            if (removed) deleted += id else failed += id
        }
        return Report(deleted = deleted, failed = failed)
    }
}
