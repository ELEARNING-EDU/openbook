package com.example.epubreader.epub

import com.example.epubreader.model.EpubException
import com.example.epubreader.model.EpubProblem
import com.example.epubreader.model.RawBook
import com.example.epubreader.model.RawChapter
import com.example.epubreader.model.TocEntry
import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Stage 1 of loading a book: pure ZIP + XML work, safe to run off the main thread.
 *
 * HTML conversion happens later (see `HtmlText`) because it has to happen on the
 * main thread.
 */
object EpubArchive {

    data class Metadata(val title: String, val author: String, val cover: ByteArray? = null)

    private const val CONTAINER = "META-INF/container.xml"
    private const val ENCRYPTION = "META-INF/encryption.xml"

    // ---------------------------------------------------------------- metadata

    /**
     * The whole import check, without decompressing the book: container, package,
     * spine, a chapter that actually exists, and no text resource encrypted.
     *
     * Listing a library calls this per file, so it reads only `container.xml`, the OPF
     * and `encryption.xml`, and asks the central directory whether a chapter is there.
     */
    fun readMetadata(file: File): Metadata {
        val archive = open(file)
        archive.use { zip ->
            val container = zip.bytes(CONTAINER) ?: throw EpubException(EpubProblem.NO_CONTAINER)
            val opfPath = containerPath(container) ?: throw EpubException(EpubProblem.NO_OPF)
            val opf = zip.bytes(opfPath)
                ?: throw EpubException(EpubProblem.NO_OPF, "package not found: $opfPath")
            val pkg = parsePackage(opf, opfPath)

            if (zip.bytes(ENCRYPTION)?.let { isDrmProtected(it) } == true) {
                throw EpubException(EpubProblem.DRM)
            }
            if (pkg.spine.isEmpty() || pkg.manifest.isEmpty()) {
                throw EpubException(EpubProblem.EMPTY_SPINE)
            }
            val readable = pkg.spine
                .asSequence()
                .mapNotNull { pkg.manifest[it] }
                .filter { it.isHtml }
                .map { resolve(it.href, pkg.opfDir) }
                .any { zip.find(it) != null }
            if (!readable) throw EpubException(EpubProblem.EMPTY_SPINE)

            return Metadata(
                title = pkg.title.ifBlank { file.nameWithoutExtension },
                author = pkg.author.ifBlank { "" },
                cover = coverBytes(pkg, zip),
            )
        }
    }

    // ------------------------------------------------------------------- book

    fun read(file: File): RawBook {
        val entries = ZipExtract.readAll(file)

        val container = ZipExtract.find(entries, CONTAINER)
            ?: throw EpubException(EpubProblem.NO_CONTAINER)
        val opfPath = containerPath(container) ?: throw EpubException(EpubProblem.NO_OPF)
        val opfBytes = ZipExtract.find(entries, opfPath)
            ?: throw EpubException(EpubProblem.NO_OPF, "package not found: $opfPath")

        // A book whose chapters are ciphertext is refused here, before a chapter is
        // read: the reader must never be handed the encrypted bytes as if they were text.
        if (ZipExtract.find(entries, ENCRYPTION)?.let { isDrmProtected(it) } == true) {
            throw EpubException(EpubProblem.DRM)
        }

        val pkg = parsePackage(opfBytes, opfPath)

        // Navigation: EPUB 3 `nav` document first, EPUB 2 NCX as the fallback.
        var labels: LinkedHashMap<String, String> = LinkedHashMap()
        val navHref = pkg.navHref
        if (navHref != null) {
            val navPath = resolve(navHref, pkg.opfDir)
            ZipExtract.find(entries, navPath)?.let { labels = navLabels(it, navPath) }
        }
        if (labels.isEmpty()) {
            val ncxPath = pkg.ncxHref?.let { resolve(it, pkg.opfDir) }
            if (ncxPath != null) {
                ZipExtract.find(entries, ncxPath)?.let { labels = ncxLabels(it, ncxPath) }
            }
        }

        val chapters = ArrayList<RawChapter>()
        val paths = ArrayList<String>()
        val missing = ArrayList<String>()
        for (idref in pkg.spine) {
            val item = pkg.manifest[idref] ?: continue
            if (!item.isHtml) continue
            val path = resolve(item.href, pkg.opfDir)
            val bytes = ZipExtract.find(entries, path)
            if (bytes == null) {
                missing += path
                continue
            }
            val html = decodeText(bytes)
            chapters += RawChapter(
                title = labels[path].orEmpty(),
                href = item.href,
                html = html,
                images = inlineImages(html, path, entries),
            )
            paths += path
        }
        if (missing.isNotEmpty()) {
            // Loud, but not fatal: one absent href must not take the other chapters down.
            System.err.println(
                "epub: ${file.name}: ${missing.size} chapter(s) missing from archive, " +
                    "kept ${chapters.size}: ${missing.joinToString(", ")}",
            )
        }
        if (chapters.isEmpty()) throw EpubException(EpubProblem.EMPTY_SPINE)

        // Front matter (cover, title page, contents, index) is not a chapter: keep it out
        // of the chapter list when the book has real chapters behind it.
        val keep = chapters.indices.filterNot { i -> isFrontMatter(chapters[i].href) }
        if (keep.isNotEmpty() && keep.size < chapters.size) {
            val remap = HashMap<Int, Int>()
            val keptChapters = ArrayList<RawChapter>(keep.size)
            val keptPaths = ArrayList<String>(keep.size)
            for ((newIndex, oldIndex) in keep.withIndex()) {
                remap[oldIndex] = newIndex
                keptChapters += chapters[oldIndex]
                keptPaths += paths[oldIndex]
            }
            chapters.clear()
            chapters.addAll(keptChapters)
            paths.clear()
            paths.addAll(keptPaths)
            val kept = keptPaths.toHashSet()
            val relabelled = LinkedHashMap<String, String>()
            for ((path, label) in labels) if (path in kept) relabelled[path] = label
            labels = relabelled
        }

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
            author = pkg.author.ifBlank { "" },
            chapters = chapters,
            toc = toc,
            language = pkg.language,
        )
    }

    // ----------------------------------------------------------------- archive

    /** Central-directory view: names normalised to NFC, lookup ignoring case. */
    private class Archive(val zip: ZipFile) : AutoCloseable {
        private val names: Map<String, ZipEntry> = buildMap {
            for (entry in zip.entries()) {
                if (entry.isDirectory) continue
                val name = ZipExtract.normalise(entry.name)
                if (name.isNotEmpty()) putIfAbsent(name, entry)
            }
        }

        fun find(path: String): ZipEntry? {
            val wanted = ZipExtract.normalise(path)
            names[wanted]?.let { return it }
            return names.entries.firstOrNull { it.key.equals(wanted, ignoreCase = true) }?.value
        }

        fun bytes(path: String): ByteArray? =
            find(path)?.let { entry -> zip.getInputStream(entry).use { it.readBytes() } }

        override fun close() = zip.close()
    }

    private fun open(file: File): Archive = try {
        Archive(ZipFile(file))
    } catch (t: Throwable) {
        throw EpubException(EpubProblem.NOT_ZIP)
    }

    /** True when `encryption.xml` names an HTML/XHTML resource, not just a font. */
    private fun isDrmProtected(bytes: ByteArray): Boolean {
        val uris = ArrayList<String>()
        val document = Xml.parse(bytes)
        if (document != null) {
            for (reference in Xml.descendants(document, "CipherReference")) {
                Xml.attr(reference, "URI")?.let { uris += it }
            }
        }
        if (uris.isEmpty()) {
            Regex("(?is)CipherReference[^>]*\\bURI\\s*=\\s*[\"']([^\"']+)[\"']")
                .findAll(String(bytes, Charsets.UTF_8))
                .forEach { uris += it.groupValues[1] }
        }
        return uris.any { uri ->
            val path = runCatching { URLDecoder.decode(uri.substringBefore('#'), "UTF-8") }
                .getOrDefault(uri.substringBefore('#'))
                .lowercase()
            path.endsWith(".xhtml") || path.endsWith(".html") || path.endsWith(".htm")
        }
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
        var language = ""
        var opfDir = ""
        var navHref: String? = null
        var ncxHref: String? = null
        var coverId: String? = null
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
            pkg.language = Regex("(?is)<dc:language[^>]*>(.*?)</dc:language>").find(text)
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
        pkg.language = Xml.descendants(document, "language")
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

        for (meta in Xml.descendants(document, "meta")) {
            if (Xml.attr(meta, "name")?.equals("cover", ignoreCase = true) == true) {
                pkg.coverId = Xml.attr(meta, "content")
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

    /** The OPF's cover image: `properties="cover-image"` first, then `<meta name="cover">`. */
    private fun coverBytes(pkg: Package, zip: Archive): ByteArray? {
        val item = pkg.manifest.values.firstOrNull {
            it.properties.split(' ').any { token -> token.equals("cover-image", true) }
        } ?: pkg.coverId?.let { pkg.manifest[it] } ?: return null
        val bytes = zip.bytes(resolve(item.href, pkg.opfDir)) ?: return null
        return bytes.takeIf { it.isNotEmpty() }
    }

    // ------------------------------------------------------------ inline images

    /**
     * The bytes behind every `<img>` a chapter asks for, keyed by the `src` as written.
     *
     * It happens here, while the archive's entries are in hand: the HTML importer is
     * handed a string and has no base URL, so a picture it cannot find is a picture the
     * reader can never show. A `src` that names nothing in the archive simply stays out
     * of the map, and the importer falls back to the alt text.
     */
    private fun inlineImages(
        html: String,
        chapterPath: String,
        entries: Map<String, ByteArray>,
    ): Map<String, ByteArray> {
        val references = ChapterImages.references(html)
        if (references.isEmpty()) return emptyMap()
        val directory = parentDir(chapterPath)
        val out = LinkedHashMap<String, ByteArray>()
        for (reference in references) {
            if (reference.src.isBlank() || out.containsKey(reference.src)) continue
            val path = resolve(reference.src, directory)
            val bytes = ZipExtract.find(entries, path) ?: continue
            out[reference.src] = bytes
        }
        return out
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
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace(Regex("&#x([0-9a-fA-F]{1,6});")) { entity ->
            runCatching { String(Character.toChars(entity.groupValues[1].toInt(16))) }
                .getOrDefault(entity.value)
        }
        .replace(Regex("&#([0-9]{1,7});")) { entity ->
            runCatching { String(Character.toChars(entity.groupValues[1].toInt())) }
                .getOrDefault(entity.value)
        }
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun attributeValue(attributes: String, name: String): String? =
        Regex("(?i)\\b$name\\s*=\\s*[\"']([^\"']*)[\"']").find(attributes)?.groupValues?.get(1)

    /** The chapter's own `<title>`, else its first heading. */
    private fun isFrontMatter(href: String): Boolean {
        val name = href.substringAfterLast('/').substringBefore('.').lowercase()
        return name == "cover" || name == "titlepage" || name == "title" ||
            name == "toc" || name == "contents" || name == "index" ||
            name == "copyright" || name == "nav"
    }

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

}
