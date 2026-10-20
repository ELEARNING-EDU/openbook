package com.example.epubreader.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.epubreader.epub.EpubArchive
import com.example.epubreader.model.BookItem
import com.example.epubreader.model.EpubException
import java.io.File

/**
 * Owns the on-device library: a `Books/` folder inside the app's private files.
 */
class LibraryStore(context: Context) {

    private val appContext = context.applicationContext

    private val directory: File
        get() = File(appContext.filesDir, "Books").also { if (!it.exists()) it.mkdirs() }

    /** Copies the EPUB shipped inside the app the first time, so the shelf is never bare. */
    fun ensureSeeded() {
        val target = File(directory, SEED_FILE_NAME)
        if (target.exists()) return
        runCatching {
            appContext.assets.open(SEED_ASSET).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    fun books(): List<BookItem> =
        directory.listFiles { file -> file.isFile && file.name.endsWith(".epub", true) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { file -> describe(file) }
            ?: emptyList()

    private fun describe(file: File): BookItem {
        val metadata = runCatching { EpubArchive.readMetadata(file) }.getOrNull()
        return BookItem(
            id = file.name,
            path = file.absolutePath,
            title = metadata?.title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension,
            author = metadata?.author?.takeIf { it.isNotBlank() } ?: "Không rõ tác giả",
        )
    }

    /** Copies a document chosen through the Storage Access Framework into the library. */
    fun import(uri: Uri): BookItem {
        val name = sanitise(displayName(uri) ?: "sach-${System.currentTimeMillis()}.epub")
        val target = File(directory, name)
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw EpubException("Không mở được tệp đã chọn.")

            // Refuse anything that is not actually an EPUB.
            runCatching { EpubArchive.readMetadata(target) }
                .getOrElse { throw EpubException("Tệp không đọc được như một EPUB hợp lệ.") }

            return describe(target)
        } catch (t: Throwable) {
            target.delete()
            throw t
        }
    }

    private fun displayName(uri: Uri): String? {
        runCatching {
            appContext.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) return cursor.getString(index)
                    }
                }
        }
        return uri.lastPathSegment
    }

    private fun sanitise(name: String): String {
        var base = name.substringAfterLast('/').substringAfterLast('\\')
        if (!base.endsWith(".epub", ignoreCase = true)) base = "$base.epub"
        base = base.replace(Regex("[^\\p{L}\\p{N}._-]"), "-")
        if (base.startsWith(".")) base = "sach$base"
        return base.ifBlank { "sach-${System.currentTimeMillis()}.epub" }
    }

    private companion object {
        const val SEED_ASSET = "sample.epub"
        const val SEED_FILE_NAME = "ngon-hai-dang-cuoi-cung.epub"
    }
}
