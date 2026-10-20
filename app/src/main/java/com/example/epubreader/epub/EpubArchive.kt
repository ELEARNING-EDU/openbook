package com.example.epubreader.epub

import com.example.epubreader.model.EpubException
import com.example.epubreader.model.RawBook
import com.example.epubreader.model.RawChapter
import com.example.epubreader.model.TocEntry
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream

/**
 * Stage 1 of loading a book: pure ZIP + XML work, safe to run off the main thread.
 *
 * HTML conversion happens later (see `HtmlText`) because it has to happen on the
 * main thread.
 */
object EpubArchive {

    data class Metadata(val title: String, val author: String)

    private const val CONTAINER = "META-INF/container.xml"

    // ---------------------------------------------------------------- metadata

    /** Reads only `container.xml` and the OPF, so listing a library stays cheap. */
    fun readMetadata(file: File): Metadata {
        val container = findEntry(file) { it == CONTAINER }?.second
            ?: throw EpubException("Thiếu META-INF/container.xml — tệp không phải EPUB hợp lệ.")
        val opfPath = containerPath(container)
            ?: throw EpubException("container.xml không chỉ tới tệp .opf nào.")
        val opf = findEntry(file) { it == opfPath || it.equals(opfPath, ignoreCase = true) }?.second
            ?: throw EpubException("Không tìm thấy tệp gói (.opf): $opfPath")
        val pkg = parsePackage(opf, opfPath)
        return Metadata(
            title = pkg.title.ifBlank { file.nameWithoutExtension },
            author = pkg.author.ifBlank { "Không rõ tác giả" },
        )
    }

    // ------------------------------------------------------------------- book

    fun read(file: File): RawBook {
        val entries = ZipExtract.readAll(file)

        val container = entries[CONTAINER]
            ?: entries.entries.firstOrNull { it.key.equals(CONTAINER, ignoreCase = true) }?.value
            ?: throw EpubException("Thiếu META-INF/container.xml — tệp không phải EPUB hợp lệ.")
        val opfPath = containerPath(container)
            ?: throw EpubException("container.xml không chỉ tới tệp .opf nào.")
        val opfBytes = entries[ZipExtract.normalise(opfPath)]
            ?: entries.entries.firstOrNull { it.key.equals(opfPath, ignoreCase = true) }?.value
            ?: throw EpubException("Không tìm thấy tệp gói (.opf): $opfPath")

        val pkg = parsePackage(opfBytes, opfPath)

        // Navigation: EPUB 3 `nav` document first, EPUB 2 NCX as the fallback.
        var labels: LinkedHashMap<String, String> = LinkedHashMap()
        val navHref = pkg.navHref
        if (navHref != null) {
            val navPath = resolve(navHref, pkg.opfDir)
            entries[navPath]?.let { labels = navLabels(it, navPath) }
        }
        if (labels.isEmpty()) {
            val ncxPath = pkg.ncxHref?.let { resolve(it, pkg.opfDir) }
            if (ncxPath != null) entries[ncxPath]?.let { labels = ncxLabels(it, ncxPath) }
        }

        val chapters = ArrayList<RawChapter>()
        val paths = ArrayList<String>()
        for (idref in pkg.spine) {
            val item = pkg.manifest[idref] ?: continue
            if (!item.isHtml) continue
            val path = resolve(item.href, pkg.opfDir)
            val bytes = entries[path]
                ?: entries.entries.firstOrNull { it.key.equals(path, ignoreCase = true) }?.value
                ?: continue
            chapters += RawChapter(
                title = labels[path].orEmpty(),
                href = item.href,
                html = decodeText(bytes),
            )
            paths += path
        }
        if (chapters.isEmpty()) throw EpubException("Sách không có chương nào trong spine.")

        // A chapter the nav did not cover still needs a header label.
        for (i in chapters.indices) {
            if (chapters[i].title.isBlank()) {
                chapters[i] = chapters[i].copy(title = documentTitle(chapters[i].html))
            }
        }

        val toc = ArrayList<TocEntry>()
        val seen = HashSet<Int>()
        for ((path, label) in labels) {
            val index = paths.indexOf(path)
            if (index >= 0 && seen.add(index) && label.isNotBlank()) {
                toc += TocEntry(title = label, chapterIndex = index)
            }
        }
        if (toc.isEmpty()) {
            chapters.forEachIndexed { index, chapter ->
                if (chapter.title.isNotBlank()) {
                    toc += TocEntry(title = chapter.title, chapterIndex = index)
                }
            }
        }

        return RawBook(
            title = pkg.title.ifBlank { file.nameWithoutExtension },
            author = pkg.author.ifBlank { "Không rõ tác giả" },
            chapters = chapters,
            toc = toc,
        )
    }

    // --------------------------------------------------------------- package

    private class ManifestItem(
        val id: String,
        val href: String,
        val mediaType: String,
        val properties: String,
    ) {
        val isHtml: Boolean
            get() {
                val type = mediaType.lowercase()
                val lower = href.lowercase()
                return type.contains("xhtml") || type.contains("html") ||
                    lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")
            }
    }

    private class Package {
        var title = ""
        var author = ""
        var opfDir = ""
        var navHref: String? = null
        var ncxHref: String? = null
        val manifest = LinkedHashMap<String, ManifestItem>()
        val spine = ArrayList<String>()
    }

    private fun parsePackage(bytes: ByteArray, opfPath: String): Package {
        val pkg = Package()
        pkg.opfDir = parentDir(opfPath)
        val document = Xml.parse(bytes)

        if (document == null) {
            // Regex fallback: enough to open the book, even if the OPF is not well formed.
            val text = String(bytes, Charsets.UTF_8)
            pkg.title = Regex("(?is)<dc:title[^>]*>(.*?)</dc:title>").find(text)
                ?.groupValues?.get(1)?.let { stripTags(it) }.orEmpty()
            pkg.author = Regex("(?is)<dc:creator[^>]*>(.*?)</dc:creator>").find(text)
                ?.groupValues?.get(1)?.let { stripTags(it) }.orEmpty()
            Regex("(?is)<item\\b([^>]*)>").findAll(text).forEach { match ->
                val attrs = match.groupValues[1]
                val id = attributeValue(attrs, "id") ?: return@forEach
                val href = attributeValue(attrs, "href") ?: return@forEach
                val item = ManifestItem(
                    id = id,
                    href = href,
                    mediaType = attributeValue(attrs, "media-type").orEmpty(),
                    properties = attributeValue(attrs, "properties").orEmpty(),
                )
                pkg.manifest[id] = item
                if (item.properties.split(' ').any { it.equals("nav", true) }) pkg.navHref = href
            }
            Regex("(?is)<itemref\\b([^>]*)>").findAll(text).forEach { match ->
                attributeValue(match.groupValues[1], "idref")?.let { pkg.spine += it }
            }
            Regex("(?is)<spine\\b([^>]*)>").find(text)
                ?.let { attributeValue(it.groupValues[1], "toc") }
                ?.let { id -> pkg.manifest[id]?.let { pkg.ncxHref = it.href } }
            return pkg
        }

        pkg.title = Xml.descendants(document, "title")
            .map { stripTags(Xml.text(it)) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        pkg.author = Xml.descendants(document, "creator")
            .map { stripTags(Xml.text(it)) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        for (item in Xml.descendants(document, "item")) {
            val id = Xml.attr(item, "id") ?: continue
            val href = Xml.attr(item, "href") ?: continue
            val manifestItem = ManifestItem(
                id = id,
                href = href,
                mediaType = Xml.attr(item, "media-type").orEmpty(),
                properties = Xml.attr(item, "properties").orEmpty(),
            )
            pkg.manifest[id] = manifestItem
            if (manifestItem.properties.split(' ').any { it.equals("nav", ignoreCase = true) }) {
                pkg.navHref = href
            }
            if (pkg.ncxHref == null &&
                manifestItem.mediaType.contains("dtbncx", ignoreCase = true)
            ) {
                pkg.ncxHref = href
            }
        }

        val spine = Xml.descendants(document, "spine").firstOrNull()
        if (spine != null) {
            for (itemref in Xml.children(spine, "itemref")) {
                Xml.attr(itemref, "idref")?.let { pkg.spine += it }
            }
            Xml.attr(spine, "toc")?.let { id -> pkg.manifest[id]?.let { pkg.ncxHref = it.href } }
        }
        return pkg
    }

    private fun containerPath(bytes: ByteArray): String? {
        val document = Xml.parse(bytes)
        if (document != null) {
            Xml.descendants(document, "rootfile")
                .firstOrNull { Xml.attr(it, "full-path") != null }
                ?.let { return ZipExtract.normalise(Xml.attr(it, "full-path")!!) }
        }
        val text = String(bytes, Charsets.UTF_8)
        return Regex("(?is)full-path\\s*=\\s*[\"']([^\"']+)[\"']").find(text)
            ?.groupValues?.get(1)
            ?.let { ZipExtract.normalise(it) }
    }

    // ------------------------------------------------------------ navigation

    /** EPUB 3: the `<nav epub:type="toc">` anchor list, ordered. */
    private fun navLabels(bytes: ByteArray, navPath: String): LinkedHashMap<String, String> {
        val dir = parentDir(navPath)
        val out = LinkedHashMap<String, String>()
        val document = Xml.parse(bytes)

        if (document != null) {
            val navs = Xml.descendants(document, "nav")
            val tocNav = navs.firstOrNull {
                Xml.attr(it, "type")?.split(' ')?.any { token -> token.equals("toc", true) } == true
            } ?: document.documentElement
            collectAnchors(tocNav, dir, out)
            if (out.isEmpty()) collectAnchors(document.documentElement, dir, out)
        }

        if (out.isEmpty()) {
            val text = String(bytes, Charsets.UTF_8)
            Regex("(?is)<a\\b([^>]*)>(.*?)</a>").findAll(text).forEach { match ->
                val href = attributeValue(match.groupValues[1], "href") ?: return@forEach
                val label = stripTags(match.groupValues[2]).replace(Regex("\\s+"), " ").trim()
                if (label.isEmpty()) return@forEach
                val path = resolve(href, dir)
                if (path.isNotEmpty()) out.putIfAbsent(path, label)
            }
        }
        return out
    }

    private fun collectAnchors(root: org.w3c.dom.Node, dir: String, out: LinkedHashMap<String, String>) {
        for (anchor in Xml.descendants(root, "a")) {
            val href = Xml.attr(anchor, "href") ?: continue
            val label = Xml.text(anchor).replace(Regex("\\s+"), " ").trim()
            if (label.isEmpty()) continue
            val path = resolve(href, dir)
            if (path.isNotEmpty()) out.putIfAbsent(path, label)
        }
    }

    /** EPUB 2: `navPoint` -> `navLabel/text` + `content/src`, ordered. */
    private fun ncxLabels(bytes: ByteArray, ncxPath: String): LinkedHashMap<String, String> {
        val dir = parentDir(ncxPath)
        val out = LinkedHashMap<String, String>()
        val document = Xml.parse(bytes)

        if (document != null) {
            for (navPoint in Xml.descendants(document, "navPoint")) {
                val label = Xml.text(Xml.child(Xml.child(navPoint, "navLabel"), "text"))
                val src = Xml.attr(Xml.child(navPoint, "content"), "src")
                if (label.isBlank() || src.isNullOrBlank()) continue
                val path = resolve(src, dir)
                if (path.isNotEmpty()) out.putIfAbsent(path, label)
            }
        }

        if (out.isEmpty()) {
            val text = String(bytes, Charsets.UTF_8)
            var pending: String? = null
            Regex("(?is)<(text|content)\\b([^>]*)>(?:(.*?)</text>)?")
                .findAll(text)
                .forEach { match ->
                    if (match.groupValues[1].equals("text", true)) {
                        val label = stripTags(match.groupValues[3]).replace(Regex("\\s+"), " ").trim()
                        if (label.isNotEmpty()) pending = label
                    } else {
                        val src = attributeValue(match.groupValues[2], "src")
                        val label = pending
                        if (src != null && !label.isNullOrBlank()) {
                            val path = resolve(src, dir)
                            if (path.isNotEmpty()) out.putIfAbsent(path, label)
                            pending = null
                        }
                    }
                }
        }
        return out
    }

    // ---------------------------------------------------------------- helpers

    /** Resolves an archive-relative href against a directory inside the archive. */
    fun resolve(href: String, directory: String): String {
        val withoutFragment = href.substringBefore('#')
        val decoded = runCatching { URLDecoder.decode(withoutFragment, "UTF-8") }
            .getOrDefault(withoutFragment)
        if (decoded.startsWith("/")) return ZipExtract.normalise(decoded)

        val parts = ArrayList<String>()
        if (directory.isNotEmpty()) {
            directory.split('/').filter { it.isNotEmpty() && it != "." }.forEach { parts += it }
        }
        for (component in decoded.split('/')) {
            when (component) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.size - 1)
                else -> parts += component
            }
        }
        return parts.joinToString("/")
    }

    private fun parentDir(path: String): String {
        val index = path.lastIndexOf('/')
        return if (index <= 0) "" else path.substring(0, index)
    }

    private fun stripTags(value: String): String = value
        .replace(Regex("(?s)<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun attributeValue(attributes: String, name: String): String? =
        Regex("(?i)\\b$name\\s*=\\s*[\"']([^\"']*)[\"']").find(attributes)?.groupValues?.get(1)

    /** The chapter's own `<title>`, else its first heading. */
    private fun documentTitle(html: String): String {
        Regex("(?is)<title[^>]*>(.*?)</title>").find(html)?.let {
            val value = stripTags(it.groupValues[1])
            if (value.isNotBlank()) return value
        }
        Regex("(?is)<h[1-3][^>]*>(.*?)</h[1-3]>").find(html)?.let {
            val value = stripTags(it.groupValues[1])
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun decodeText(bytes: ByteArray): String {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        val head = String(bytes, 0, minOf(bytes.size, 256), Charsets.ISO_8859_1)
        val declared = Regex("(?i)encoding\\s*=\\s*[\"']([\\w-]+)[\"']")
            .find(head)?.groupValues?.get(1)
        val charset = declared
            ?.let { name -> runCatching { charset(name) }.getOrNull() }
            ?: Charsets.UTF_8
        return String(bytes, charset)
    }

    private fun findEntry(file: File, wanted: (String) -> Boolean): Pair<String, ByteArray>? {
        FileInputStream(file).use { raw ->
            BufferedInputStream(raw).use { buffered ->
                ZipInputStream(buffered).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val name = ZipExtract.normalise(entry.name)
                        if (!wanted(name)) continue
                        val out = ByteArrayOutputStream(8192)
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                        }
                        return name to out.toByteArray()
                    }
                }
            }
        }
        return null
    }
}
