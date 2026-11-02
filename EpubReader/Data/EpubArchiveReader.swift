import Foundation

/// Stage 1 of loading a book: pure ZIP + XML work, safe to run off the main thread.
/// HTML conversion happens later because `NSAttributedString`'s HTML importer is
/// main-thread only.
enum EpubArchiveReader {

    struct Metadata {
        let title: String
        let author: String
    }

    static func readMetadata(url: URL) throws -> Metadata {
        let archive = try ZipArchive(url: url)
        let index = entryIndex(archive)
        let info = try packageInfo(in: archive, index: index)
        try ensureUnprotected(archive, index: index)
        return Metadata(
            title: info.title.isEmpty ? fallbackTitle(from: url) : info.title,
            author: info.author
        )
    }

    /// The checks an import can afford: structure and protection, without
    /// decompressing every chapter of a 1500-page book.
    static func validate(url: URL) throws {
        let archive = try ZipArchive(url: url)
        let index = entryIndex(archive)
        let info = try packageInfo(in: archive, index: index)
        try ensureUnprotected(archive, index: index)
        let hasChapter = info.spine.contains { idref in
            guard let item = info.manifest[idref], isText(item) else { return false }
            return data(
                for: resolve(item.href, relativeTo: info.opfDirectory),
                in: archive,
                index: index
            ) != nil
        }
        guard hasChapter else { throw EpubError.emptySpine }
    }

    static func read(url: URL) throws -> RawBook {
        let archive = try ZipArchive(url: url)
        let index = entryIndex(archive)
        let info = try packageInfo(in: archive, index: index)
        try ensureUnprotected(archive, index: index)

        // Chapter titles, keyed by the fully resolved archive path.
        var titles: [String: String] = [:]
        if let navHref = info.navHref {
            let navPath = resolve(navHref, relativeTo: info.opfDirectory)
            if let data = data(for: navPath, in: archive, index: index) {
                titles = chapterTitles(from: data, at: navPath)
            }
        }
        if titles.isEmpty, let ncxHref = info.ncxHref {
            let ncxPath = resolve(ncxHref, relativeTo: info.opfDirectory)
            if let data = data(for: ncxPath, in: archive, index: index) {
                titles = chapterTitles(from: data, at: ncxPath)
            }
        }

        var chapters: [RawChapter] = []
        for idref in info.spine {
            guard let item = info.manifest[idref], isText(item) else { continue }

            let path = resolve(item.href, relativeTo: info.opfDirectory)
            guard let html = data(for: path, in: archive, index: index) else { continue }
            chapters.append(
                RawChapter(
                    title: titles[path] ?? "",
                    href: item.href,
                    html: html,
                    images: images(in: html, chapterPath: path, archive: archive, index: index)
                )
            )
        }

        guard !chapters.isEmpty else { throw EpubError.emptySpine }

        return RawBook(
            title: info.title.isEmpty ? fallbackTitle(from: url) : info.title,
            author: info.author,
            language: info.language.isEmpty ? nil : info.language,
            chapters: chapters
        )
    }

    // MARK: - Package

    struct ManifestItem {
        let href: String
        let mediaType: String
        let properties: String
    }

    struct PackageInfo {
        var title = ""
        var author = ""
        var language = ""
        var manifest: [String: ManifestItem] = [:]
        var spine: [String] = []
        var navHref: String?
        var ncxHref: String?
        /// `<meta name="cover" content="…">` names a manifest item, resolved
        /// after parsing; EPUB 3 can also mark the image itself.
        var coverID: String?
        var coverHref: String?
        /// The `toc` attribute on <spine> is a manifest id, resolved after parsing.
        var ncxIdOverride: String?
        var opfDirectory = ""
    }

    /// The cover image a package points at, read straight from the zip. A book
    /// with no declared cover, or one whose declared file is missing, simply has
    /// no image and falls back to the generated cover.
    static func coverData(url: URL) throws -> Data? {
        let archive = try ZipArchive(url: url)
        let index = entryIndex(archive)
        let info = try packageInfo(in: archive, index: index)
        guard let href = info.coverHref else { return nil }
        let path = resolve(href, relativeTo: info.opfDirectory)
        guard let data = data(for: path, in: archive, index: index),
              data.count <= maxImageBytes
        else { return nil }
        return data
    }

    private static func packageInfo(in archive: ZipArchive, index: [String: String]) throws -> PackageInfo {
        guard let container = data(for: "META-INF/container.xml", in: archive, index: index) else {
            throw EpubError.missingContainer
        }
        guard let opfPath = ContainerParser().parse(container) else {
            throw EpubError.missingContainer
        }
        guard let opfData = data(for: opfPath, in: archive, index: index) else {
            throw EpubError.missingPackage
        }

        var info = PackageParser().parse(opfData, path: opfPath)
        // `toc` on <spine> is a manifest id, not a path.
        if let ncxId = info.ncxIdOverride, let item = info.manifest[ncxId] {
            info.ncxHref = item.href
        }
        // `<meta name="cover">` points at a manifest id rather than a path.
        if info.coverHref == nil, let coverID = info.coverID,
           let item = info.manifest[coverID], item.mediaType.hasPrefix("image/") {
            info.coverHref = item.href
        }
        // EPUB 2 books sometimes only mark the image by its id or name.
        if info.coverHref == nil,
           let item = info.manifest.values.first(where: {
               $0.mediaType.hasPrefix("image/") && $0.href.lowercased().contains("cover")
           }) {
            info.coverHref = item.href
        }
        return info
    }

    // MARK: - Helpers

    private static func chapterTitles(from data: Data, at path: String) -> [String: String] {
        let directory = (path as NSString).deletingLastPathComponent
        var map: [String: String] = [:]
        for (href, label) in TocParser().parse(data) {
            map[resolve(href, relativeTo: directory == path ? "" : directory)] = label
        }
        return map
    }

    private static func stripFragment(_ href: String) -> String {
        guard let hash = href.firstIndex(of: "#") else { return href }
        return String(href[href.startIndex..<hash])
    }

    /// Resolves an archive-relative href against a directory inside the archive.
    static func resolve(_ href: String, relativeTo directory: String) -> String {
        let cleaned = stripFragment(href).removingPercentEncoding ?? stripFragment(href)
        if cleaned.hasPrefix("/") { return String(cleaned.dropFirst()) }

        var parts = directory.isEmpty ? [] : directory.split(separator: "/").map(String.init)
        for component in cleaned.split(separator: "/") {
            switch component {
            case ".": continue
            case "..": if !parts.isEmpty { parts.removeLast() }
            default: parts.append(String(component))
            }
        }
        return parts.joined(separator: "/")
    }

    /// Archive names folded for lookup. The archive itself is case-sensitive but
    /// EPUB writers disagree about case, and some file everything under
    /// "./OEBPS/…", so a chapter must be found by its folded name when the exact
    /// one is absent. The first name wins, so a duplicate only loses itself.
    private static func entryIndex(_ archive: ZipArchive) -> [String: String] {
        var index: [String: String] = [:]
        for name in archive.allEntryNames() {
            let clean = normalize(name)
            if index[clean.lowercased()] == nil { index[clean.lowercased()] = name }
        }
        return index
    }

    private static func normalize(_ path: String) -> String {
        var clean = path
        while clean.hasPrefix("./") { clean.removeFirst(2) }
        return clean
    }

    private static func data(
        for path: String,
        in archive: ZipArchive,
        index: [String: String]
    ) -> Data? {
        let clean = normalize(path)
        if let exact = archive.data(forEntryNamed: clean) { return exact }
        guard let name = index[clean.lowercased()] else { return nil }
        return archive.data(forEntryNamed: name)
    }

    private static func isText(_ item: ManifestItem) -> Bool {
        item.mediaType.contains("xhtml")
            || item.mediaType.contains("html")
            || item.href.hasSuffix(".xhtml")
            || item.href.hasSuffix(".html")
    }

    /// `encryption.xml` appears in two very different books: a shop's DRM wrapper
    /// (chapters encrypted — nothing can be shown) and a publisher's obfuscated
    /// fonts (text plain). Only encrypted text is a refusal.
    private static func ensureUnprotected(_ archive: ZipArchive, index: [String: String]) throws {
        guard let xml = data(for: "META-INF/encryption.xml", in: archive, index: index) else {
            return
        }
        let textExtensions: Set<String> = ["xhtml", "html", "htm"]
        for uri in EncryptionParser().encryptedPaths(in: xml)
        where textExtensions.contains((uri as NSString).pathExtension.lowercased()) {
            throw EpubError.protected
        }
    }

    /// The pictures one chapter's `<img>` tags point at, keyed by the raw `src`, so
    /// the reader can put them back without knowing anything about the archive.
    ///
    /// Pictures that are not in the archive (a broken `src`, an online URL, an SVG
    /// the reader cannot decode) are simply left out: the page then shows the alt
    /// text instead of a hole.
    private static func images(
        in html: Data,
        chapterPath: String,
        archive: ZipArchive,
        index: [String: String]
    ) -> [String: Data] {
        let source = String(decoding: html, as: UTF8.self)
        let directory = (chapterPath as NSString).deletingLastPathComponent
        let base = directory == chapterPath ? "" : directory

        var found: [String: Data] = [:]
        var carried = 0
        for reference in ChapterImages.references(in: source) {
            let src = reference.src.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !src.isEmpty, found[src] == nil, !src.hasPrefix("data:") else { continue }
            guard !src.contains("://"), !src.hasPrefix("//") else { continue }

            // `pic.png?v=2` still names the file `pic.png`.
            let path = resolve(String(src.split(separator: "?", maxSplits: 1).first ?? ""), relativeTo: base)
            guard let data = data(for: path, in: archive, index: index),
                  data.count <= maxImageBytes,
                  carried + data.count <= maxChapterImageBytes
            else { continue }
            carried += data.count
            found[src] = data
        }
        return found
    }

    /// One picture big enough to hurt the reader is not worth carrying.
    private static let maxImageBytes = 8_000_000
    /// Nor is a chapter that is nothing but photographs: the raw bytes stay in
    /// memory for as long as the book is open.
    private static let maxChapterImageBytes = 24_000_000

    private static func fallbackTitle(from url: URL) -> String {
        url.deletingPathExtension().lastPathComponent
    }
}

// MARK: - XML delegates

/// Reads `META-INF/container.xml` and returns the OPF path.
final class ContainerParser: NSObject, XMLParserDelegate {
    private var path: String?

    func parse(_ data: Data) -> String? {
        let parser = XMLParser(data: data)
        parser.delegate = self
        parser.shouldProcessNamespaces = false
        parser.parse()
        return path
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName: String?,
        attributes attributeDict: [String: String]
    ) {
        if elementName == "rootfile", path == nil {
            path = attributeDict["full-path"]
        }
    }
}

/// Reads the OPF package document: metadata, manifest, spine.
final class PackageParser: NSObject, XMLParserDelegate {
    private var info = EpubArchiveReader.PackageInfo()
    private var buffer = ""
    private var capture: String?

    func parse(_ data: Data, path: String) -> EpubArchiveReader.PackageInfo {
        info.opfDirectory = (path as NSString).deletingLastPathComponent
        if info.opfDirectory == path { info.opfDirectory = "" }

        let parser = XMLParser(data: data)
        parser.delegate = self
        parser.shouldProcessNamespaces = false
        parser.parse()
        return info
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName: String?,
        attributes attributeDict: [String: String]
    ) {
        buffer = ""
        switch elementName {
        case "dc:title", "title":
            capture = "title"
        case "dc:creator", "creator":
            capture = "creator"
        case "dc:language", "language":
            capture = "language"
        case "meta":
            if attributeDict["name"] == "cover", let id = attributeDict["content"] {
                info.coverID = id
            }
        case "item":
            if let id = attributeDict["id"], let href = attributeDict["href"] {
                let item = EpubArchiveReader.ManifestItem(
                    href: href,
                    mediaType: attributeDict["media-type"] ?? "",
                    properties: attributeDict["properties"] ?? ""
                )
                info.manifest[id] = item
                if item.properties.contains("nav") { info.navHref = href }
                if item.properties.contains("cover-image"), info.coverHref == nil {
                    info.coverHref = href
                }
            }
        case "spine":
            if let toc = attributeDict["toc"] { info.ncxIdOverride = toc }
        case "itemref":
            if let idref = attributeDict["idref"] { info.spine.append(idref) }
        default:
            break
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        buffer += string
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName: String?
    ) {
        let text = buffer.trimmingCharacters(in: .whitespacesAndNewlines)
        switch elementName {
        case "dc:title", "title":
            if capture == "title", info.title.isEmpty { info.title = text }
            capture = nil
        case "dc:creator", "creator":
            if capture == "creator", info.author.isEmpty { info.author = text }
            capture = nil
        case "dc:language", "language":
            if capture == "language", info.language.isEmpty { info.language = text }
            capture = nil
        default:
            break
        }
        buffer = ""
    }
}

/// Reads both EPUB 3 (`nav.xhtml`) and EPUB 2 (`toc.ncx`) navigation,
/// returning `(href, label)` pairs.
final class TocParser: NSObject, XMLParserDelegate {
    private var items: [(String, String)] = []
    private var buffer = ""
    private var captureLabel = false
    private var anchorHref: String?
    private var pendingLabel: String?

    func parse(_ data: Data) -> [(String, String)] {
        let parser = XMLParser(data: data)
        parser.delegate = self
        parser.shouldProcessNamespaces = false
        parser.parse()
        return items
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName: String?,
        attributes attributeDict: [String: String]
    ) {
        buffer = ""
        switch elementName {
        case "text":
            captureLabel = true
        case "a":
            captureLabel = true
            anchorHref = attributeDict["href"]
        case "content":
            if let src = attributeDict["src"], let label = pendingLabel {
                items.append((src, label))
                pendingLabel = nil
            }
        default:
            break
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        if captureLabel { buffer += string }
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName: String?
    ) {
        let label = buffer.trimmingCharacters(in: .whitespacesAndNewlines)
        switch elementName {
        case "text":
            captureLabel = false
            if !label.isEmpty { pendingLabel = label }
        case "a":
            captureLabel = false
            if let href = anchorHref, !label.isEmpty { items.append((href, label)) }
            anchorHref = nil
        default:
            break
        }
        buffer = ""
    }
}

/// Collects the resource paths `META-INF/encryption.xml` says are encrypted.
/// Font obfuscation and DRM use the same element, so the caller decides which
/// kinds of resource mean a book that cannot be read.
final class EncryptionParser: NSObject, XMLParserDelegate {
    private var paths: [String] = []

    func encryptedPaths(in data: Data) -> [String] {
        let parser = XMLParser(data: data)
        parser.delegate = self
        parser.shouldProcessNamespaces = false
        parser.parse()
        return paths
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName: String?,
        attributes attributeDict: [String: String]
    ) {
        // The prefix is optional in real files: `CipherReference` and
        // `enc:CipherReference` both occur.
        if elementName.hasSuffix("CipherReference"), let uri = attributeDict["URI"] {
            paths.append(uri)
        }
    }
}
