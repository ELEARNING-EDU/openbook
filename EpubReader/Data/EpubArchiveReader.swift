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
        let info = try packageInfo(in: archive)
        return Metadata(
            title: info.title.isEmpty ? fallbackTitle(from: url) : info.title,
            author: info.author
        )
    }

    static func read(url: URL) throws -> RawBook {
        let archive = try ZipArchive(url: url)
        let info = try packageInfo(in: archive)

        // Chapter titles, keyed by the fully resolved archive path.
        var titles: [String: String] = [:]
        if let navHref = info.navHref {
            let navPath = resolve(navHref, relativeTo: info.opfDirectory)
            if let data = archive.data(forEntryNamed: navPath) {
                titles = chapterTitles(from: data, at: navPath)
            }
        }
        if titles.isEmpty, let ncxHref = info.ncxHref {
            let ncxPath = resolve(ncxHref, relativeTo: info.opfDirectory)
            if let data = archive.data(forEntryNamed: ncxPath) {
                titles = chapterTitles(from: data, at: ncxPath)
            }
        }

        // The archive stores names case-sensitively; EPUBs disagree about case in
        // the hrefs they write, so pictures are looked up through a folded index.
        var entryNames: [String: String] = [:]
        for name in archive.allEntryNames() where entryNames[name.lowercased()] == nil {
            entryNames[name.lowercased()] = name
        }

        var chapters: [RawChapter] = []
        for idref in info.spine {
            guard let item = info.manifest[idref] else { continue }
            guard item.mediaType.contains("xhtml")
                    || item.mediaType.contains("html")
                    || item.href.hasSuffix(".xhtml")
                    || item.href.hasSuffix(".html") else { continue }

            let path = resolve(item.href, relativeTo: info.opfDirectory)
            guard let html = archive.data(forEntryNamed: path) else { continue }
            chapters.append(
                RawChapter(
                    title: titles[path] ?? "",
                    href: item.href,
                    html: html,
                    images: images(in: html, chapterPath: path, archive: archive, entryNames: entryNames)
                )
            )
        }

        guard !chapters.isEmpty else { throw EpubError.emptySpine }

        return RawBook(
            title: info.title.isEmpty ? fallbackTitle(from: url) : info.title,
            author: info.author,
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
        var manifest: [String: ManifestItem] = [:]
        var spine: [String] = []
        var navHref: String?
        var ncxHref: String?
        /// The `toc` attribute on <spine> is a manifest id, resolved after parsing.
        var ncxIdOverride: String?
        var opfDirectory = ""
    }

    private static func packageInfo(in archive: ZipArchive) throws -> PackageInfo {
        guard let container = archive.data(forEntryNamed: "META-INF/container.xml") else {
            throw EpubError.missingContainer
        }
        guard let opfPath = ContainerParser().parse(container) else {
            throw EpubError.missingContainer
        }
        guard let opfData = archive.data(forEntryNamed: opfPath) else {
            throw EpubError.missingPackage
        }

        var info = PackageParser().parse(opfData, path: opfPath)
        // `toc` on <spine> is a manifest id, not a path.
        if let ncxId = info.ncxIdOverride, let item = info.manifest[ncxId] {
            info.ncxHref = item.href
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
        entryNames: [String: String]
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
            guard let name = entryNames[path.lowercased()],
                  let data = archive.data(forEntryNamed: name),
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
        case "item":
            if let id = attributeDict["id"], let href = attributeDict["href"] {
                let item = EpubArchiveReader.ManifestItem(
                    href: href,
                    mediaType: attributeDict["media-type"] ?? "",
                    properties: attributeDict["properties"] ?? ""
                )
                info.manifest[id] = item
                if item.properties.contains("nav") { info.navHref = href }
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
