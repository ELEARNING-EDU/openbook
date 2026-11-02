package com.elearning.openbook.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.elearning.openbook.R
import com.elearning.openbook.epub.EpubArchive
import com.elearning.openbook.epub.TextCleanup
import com.elearning.openbook.epub.TextCleanupReport
import com.elearning.openbook.epub.VietnameseWords
import com.elearning.openbook.model.BookItem
import com.elearning.openbook.model.EpubException
import com.elearning.openbook.model.EpubProblem
import com.elearning.openbook.settings.TextRepairStore
import java.io.File

/** A book that landed on the shelf, plus what the import's text pass changed in it. */
data class ImportedBook(val book: BookItem, val cleanup: TextCleanupReport)

/**
 * Owns the on-device library: a `Books/` folder inside the app's private files.
 *
 * The folder is the library; `SharedPreferences` only carries what the reader changed
 * about a file — its title, its author, and when it arrived. An imported copy is cleaned
 * once (see [TextCleanup]) and never rewritten after that, and the document the reader
 * picked from outside the app is only ever read.
 */
class LibraryStore(context: Context) {

    private val appContext = context.applicationContext

    private val metadata = appContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)

    /** Read once: the words the import may lean on when it puts a lost `đ` back. */
    private val words by lazy { VietnameseWords.load(appContext) }

    /** The reader's choice: repair the text of a copy, or keep the book as published. */
    private val repair = TextRepairStore(appContext)

    private val directory: File
        get() = File(appContext.filesDir, "Books").also { if (!it.exists()) it.mkdirs() }

    /**
     * Copies the EPUB shipped inside the app the first time, so the shelf is never bare.
     *
     * Once the reader has deleted that sample, it stays deleted: the flag is what keeps
     * a cold launch from quietly putting a book back on the shelf.
     */
    fun ensureSeeded() {
        val target = File(directory, SEED_FILE_NAME)
        if (target.exists()) return
        if (metadata.getBoolean(KEY_SAMPLE_DELETED, false)) return
        runCatching {
            appContext.assets.open(SEED_ASSET).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    fun books(): List<BookItem> =
        directory.listFiles { file -> file.isFile && file.name.endsWith(".epub", true) }
            ?.map { file -> describe(file) }
            ?: emptyList()

    /**
     * Cleans copies that arrived before the importer could do it: a book dropped into
     * the folder by hand, or imported by an older build. Each file is done once and the
     * flag keeps later launches from repeating the pass.
     */
    fun cleanLibrary() {
        if (!repair.enabled) return
        val edit = metadata.edit()
        var touched = false
        for (file in directory.listFiles().orEmpty()) {
            if (!file.isFile || !file.name.endsWith(".epub", ignoreCase = true)) continue
            if (metadata.getBoolean(cleanedKey(file.name), false)) continue
            val report = TextCleanup.clean(file, words)
            if (report.files > 0 || file.length() > 0) {
                edit.putBoolean(cleanedKey(file.name), true)
                touched = true
            }
        }
        if (touched) edit.apply()
    }

    private fun describe(file: File): BookItem {
        val stored = BookMetadataCodec.decode(metadata.getString(metaKey(file.name), null))
        val info = runCatching { EpubArchive.readMetadata(file) }.getOrNull()
        val importedAt = stored?.importedAt?.takeIf { it > 0L }
            ?: file.lastModified().takeIf { it > 0L }
            ?: System.currentTimeMillis()
        if (stored?.importedAt != importedAt) {
            // First sight of this file: freeze its arrival time now, so a later edit or
            // copy cannot move it up or down the "recently imported" order.
            persist(file.name, stored?.copy(importedAt = importedAt) ?: BookMetadata(importedAt = importedAt))
        }
        return BookItem(
            id = file.name,
            path = file.absolutePath,
            coverPath = info?.cover?.let { cacheCover(file.name, it) },
            title = tidyTitle(
                stored?.title?.takeIf { it.isNotBlank() }
                    ?: info?.title?.takeIf { it.isNotBlank() }
                    ?: file.nameWithoutExtension,
            ),
            author = tidyAuthor(
                stored?.author?.takeIf { it.isNotBlank() }
                    ?: info?.author?.takeIf { it.isNotBlank() }
                    ?: "",
            ),
            importedAt = importedAt,
            finishedAt = stored?.finishedAt?.takeIf { it > 0L },
        )
    }

    /**
     * Replaces the reader's title and author for one book. The id, the file, the reading
     * position and the marks are untouched: this is a label, not a new book.
     */
    fun updateMetadata(id: String, title: String, author: String): BookItem? {
        val file = directory.listFiles { candidate -> candidate.isFile && candidate.name == id }
            ?.firstOrNull() ?: return null
        val stored = BookMetadataCodec.decode(metadata.getString(metaKey(id), null))
        val next = BookMetadata(
            title = title.trim().takeIf { it.isNotBlank() },
            author = author.trim().takeIf { it.isNotBlank() },
            importedAt = stored?.importedAt?.takeIf { it > 0L } ?: file.lastModified(),
            finishedAt = stored?.finishedAt ?: 0L,
        )
        persist(id, next)
        return describe(file)
    }

    /** Marks a book finished (now) or clears the mark; reading position is untouched. */
    fun markFinished(id: String, finished: Boolean): BookItem? {
        val file = directory.listFiles { candidate -> candidate.isFile && candidate.name == id }
            ?.firstOrNull() ?: return null
        val stored = BookMetadataCodec.decode(metadata.getString(metaKey(id), null))
        val next = (stored ?: BookMetadata(importedAt = file.lastModified())).copy(
            finishedAt = if (finished) System.currentTimeMillis() else 0L,
        )
        persist(id, next)
        return describe(file)
    }

    /**
     * Removes app-managed copies only. The reader's original document, wherever the
     * system picker found it, is not ours to delete and is never referenced here.
     */
    fun delete(ids: Collection<String>): LibraryFiles.Report {
        val report = LibraryFiles.delete(directory, ids)
        if (report.deleted.isNotEmpty()) {
            val edit = metadata.edit()
            val covers = File(appContext.filesDir, "covers")
            for (id in report.deleted) {
                edit.remove(metaKey(id))
                edit.remove(cleanedKey(id))
                // A same-name reimport must not inherit the old book's cover.
                File(covers, id).delete()
            }
            if (SEED_FILE_NAME in report.deleted) edit.putBoolean(KEY_SAMPLE_DELETED, true)
            edit.apply()
        }
        return report
    }

    /** Copies a document chosen through the Storage Access Framework into the library. */
    fun import(uri: Uri): ImportedBook {
        val name = sanitise(displayName(uri) ?: "sach-${System.currentTimeMillis()}.epub")
        val unique = uniqueName(name)
        val target = File(directory, unique)
        val started = android.os.SystemClock.elapsedRealtime()
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw java.io.IOException("the picked document could not be opened")

            // Refuse anything that is not a readable, unencrypted EPUB. The archive
            // decides which problem it is (not a ZIP, no container, no OPF, empty
            // spine, DRM); the UI turns that into the matching localized line.
            val meta = try {
                EpubArchive.readMetadata(target)
            } catch (problem: EpubException) {
                throw problem
            } catch (t: Throwable) {
                throw EpubException(EpubProblem.NOT_ZIP, t.message ?: "not an EPUB")
            }

            android.util.Log.i(
                "LibraryStore",
                "import ${target.name}: ${android.os.SystemClock.elapsedRealtime() - started} ms, " +
                    "${target.length()} bytes, lang ${meta.language.ifBlank { "unknown" }}",
            )
            persist(
                unique,
                BookMetadata(importedAt = System.currentTimeMillis().coerceAtLeast(target.lastModified())),
            )
            // Composed Unicode and no invisible characters, so the same text searches,
            // highlights and speaks the same way on every device. The original pick is
            // never touched; only this copy is.
            val cleanup = if (repair.enabled) {
                TextCleanup.clean(target, words).also {
                    metadata.edit().putBoolean(cleanedKey(unique), true).apply()
                }
            } else {
                TextCleanupReport()
            }
            return ImportedBook(describe(target), cleanup)
        } catch (t: Throwable) {
            target.delete()
            android.util.Log.w(
                "LibraryStore",
                "refused ${target.name}: " +
                    ((t as? EpubException)?.problem?.name ?: t.javaClass.simpleName),
            )
            throw t
        }
    }

    /** Some shop EPUBs carry their own download slug as `dc:title`; unwrap those. */
    private fun tidyTitle(raw: String): String {
        val looksLikeSlug = raw.contains("thuviensach", ignoreCase = true) ||
            raw.matches(Regex("^\\d+[-_].*"))
        val cleaned = if (looksLikeSlug) cleanFileTitle(raw) else raw.trim()
        return cleaned.ifBlank { "Untitled" }
    }

    /** Engine metadata often says only "Unknown"; show nothing rather than a lie. */
    private fun tidyAuthor(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.equals("unknown", ignoreCase = true) ||
            trimmed.equals("unknown author", ignoreCase = true)
        ) "" else trimmed
    }

    /** A download slug such as `965-tinh-hoa-giao-duc-thuviensach.vn` as a title. */
    private fun cleanFileTitle(stem: String): String = stem
        .substringBefore("thuviensach", stem)
        .replace(Regex("^\\d+[-_]"), "")
        .replace('-', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .replaceFirstChar { it.uppercase() }
        .ifBlank { stem }

    /** Writes the EPUB's cover once, then reuses it on every later listing. */
    private fun cacheCover(id: String, bytes: ByteArray): String? = runCatching {
        val dir = File(appContext.filesDir, "covers").also { if (!it.exists()) it.mkdirs() }
        val target = File(dir, id)
        if (!target.exists() || target.length() != bytes.size.toLong()) {
            target.writeBytes(bytes)
        }
        target.absolutePath
    }.getOrNull()

    private fun persist(id: String, value: BookMetadata) {
        metadata.edit().putString(metaKey(id), BookMetadataCodec.encode(value)).apply()
    }

    /** Never let a second import silently replace a book already on the shelf. */
    private fun uniqueName(name: String): String {
        if (!File(directory, name).exists()) return name
        val stem = name.removeSuffix(".epub")
        var counter = 2
        while (true) {
            val candidate = "$stem ($counter).epub"
            if (!File(directory, candidate).exists()) return candidate
            counter++
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

    private fun metaKey(id: String): String = "$KEY_META_PREFIX$id"

    private fun cleanedKey(id: String): String = "$KEY_CLEANED_PREFIX$id"

    private companion object {
        const val SEED_ASSET = "sample.epub"
        const val SEED_FILE_NAME = "ngon-hai-dang-cuoi-cung.epub"
        const val META_PREFS = "libraryMetadata"
        const val KEY_META_PREFIX = "meta."
        const val KEY_SAMPLE_DELETED = "sampleDeleted"
        const val KEY_CLEANED_PREFIX = "clean."
    }
}
