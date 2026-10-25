import SwiftUI
import UIKit

@MainActor
final class ReaderViewModel: ObservableObject {

    @Published var pages: [PageContent] = []
    /// Bumped on every re-pagination. A theme or a type size can change what is on
    /// a page without changing how many pages there are, so the count alone is not
    /// enough for the curl view to tell that its pages are stale.
    @Published private(set) var pagesRevision = 0
    @Published var toc: [TocEntry] = []
    @Published var title = ""
    @Published var author = ""
    @Published var isLoading = true
    @Published var errorMessage: String?

    private var chapters: [Chapter] = []
    /// Characters per chapter, so the voice's place in one can be turned into a
    /// fraction of the whole book for the player's progress bar.
    private var chapterCharacters: [Int] = []
    private var chapterStartPage: [Int] = []
    private var containerSize: CGSize = .zero
    private var safeTop: CGFloat = 62
    private var paginatedSize: CGSize = .zero
    private var loadedURL: URL?

    /// The parsed chapters, kept so the styling can be re-applied when the reader
    /// changes the type size: the styled text is a pure function of these.
    private var rawChapters: [(title: String, href: String, html: Data, images: [String: Data])] = []
    private var styledTheme = ""
    private var styledStyle = ReaderCustom()
    /// The column the chapters were typeset for. A picture is cut to the column it
    /// lands in, so a change of width re-typesets rather than clips it.
    private var builtWidth: CGFloat = 0
    private var builtHeight: CGFloat = 0
    private var builtPalette: ReaderPalette?
    /// The column geometry the current pages were measured against.
    private var paginatedKey = ""
    /// The style the pages were typeset with, kept so a later re-pagination knows
    /// how wide a column is.
    private var style = ReaderCustom()

    func load(
        url: URL,
        style: ReaderCustom,
        palette: ReaderPalette
    ) {
        guard loadedURL != url else { return }
        loadedURL = url
        isLoading = true
        errorMessage = nil

        Task {
            do {
                let raw = try await Task.detached(priority: .userInitiated) {
                    try EpubArchiveReader.read(url: url)
                }.value

                rawChapters = raw.chapters.map {
                    (title: $0.title, href: $0.href, html: $0.html, images: $0.images)
                }
                // The text is typeset inside pagination, where the column width is
                // known: a picture is measured against the column it lands in, so
                // building it before the geometry exists would bake in the wrong
                // size and re-typeset straight afterwards. Nothing measured for a
                // previous book may survive into this one.
                chapters = []
                builtWidth = 0
                builtHeight = 0
                paginatedKey = ""
                self.style = style
                styledStyle = style
                styledTheme = palette.id
                builtPalette = palette

                title = raw.title
                author = raw.author
                isLoading = false
                rebuildPagesIfPossible()
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    /// Re-typesets the whole book at a new size / leading and re-paginates.
    ///
    /// Page numbers are meaningless across a re-typeset, so the caller is
    /// expected to re-anchor the reading position by chapter offset.
    func restyle(style: ReaderCustom, palette: ReaderPalette) {
        guard !rawChapters.isEmpty else { return }
        guard style != styledStyle || palette.id != styledTheme else { return }
        buildChapters(style: style, palette: palette)
        rebuildPagesIfPossible(force: true)
    }

    /// The HTML importer is main-thread only, which is why this is not in the task.
    private func buildChapters(style: ReaderCustom, palette: ReaderPalette) {
        self.style = style
        styledStyle = style
        styledTheme = palette.id
        builtPalette = palette

        let width = builtWidth
        // A tall picture may take more than half the page, never the whole of it.
        let pictureHeight = builtHeight * 0.55

        chapters = rawChapters.map { raw in
            let attributed = AttributedBuilder.build(
                html: raw.html,
                style: style,
                palette: palette,
                images: raw.images,
                columnWidth: width,
                maxImageHeight: pictureHeight
            )
            return Chapter(
                title: raw.title,
                href: raw.href,
                text: attributed,
                sentences: SentenceSplitter.ranges(in: attributed.string)
            )
        }
        chapterCharacters = chapters.map { $0.text.length }
    }

    /// How far into the book a place in a chapter sits, as a fraction, counted in
    /// characters so the bar moves evenly through chapters of different lengths.
    func narrationProgress(chapter chapterIndex: Int, offset: Int) -> Double {
        let total = chapterCharacters.reduce(0, +)
        guard total > 0, !chapterCharacters.isEmpty else { return 0 }
        let index = min(max(chapterIndex, 0), chapterCharacters.count - 1)
        let before = chapterCharacters.prefix(index).reduce(0, +)
        let within = min(max(offset, 0), chapterCharacters[index])
        return min(1, max(0, Double(before + within) / Double(total)))
    }

    /// The inverse of `narrationProgress`: the chapter and offset a fraction of
    /// the book points at. Used by the player's scrubber.
    func narrationPlace(fraction: Double) -> (chapter: Int, offset: Int)? {
        NarrationMath.place(fraction: fraction, chapterLengths: chapterCharacters)
    }

    func chapterTitle(at index: Int) -> String {
        guard chapters.indices.contains(index) else { return "" }
        let title = chapters[index].title
        return title.isEmpty ? L10n.string("reader.untitled") : title
    }

    /// Called from the view's GeometryReader, both on appear and on size change.
    func updatePagination(for bounds: CGSize, safeTop: CGFloat, style: ReaderCustom) {
        containerSize = bounds
        self.safeTop = safeTop
        // The margins and the column count are part of the measurement, so a
        // change of either has to reach the page builder even when the container
        // itself has not moved.
        if style != self.style {
            self.style = style
            rebuildPagesIfPossible(force: true)
            return
        }
        rebuildPagesIfPossible()
    }

    func pageIndex(forChapter chapterIndex: Int) -> Int {
        guard chapterStartPage.indices.contains(chapterIndex) else { return 0 }
        return chapterStartPage[chapterIndex]
    }

    func allChapters() -> [Chapter] { chapters }

    /// Where narration should start when the user asks for "this page": the first
    /// sentence that *begins* on the page, falling back to one that merely crosses it.
    func firstSentenceOffset(inChapter chapterIndex: Int, pageRange: NSRange) -> Int? {
        guard chapters.indices.contains(chapterIndex) else { return nil }
        let sentences = chapters[chapterIndex].sentences
        if let starting = sentences.first(where: { NSLocationInRange($0.location, pageRange) }) {
            return starting.location
        }
        if let crossing = sentences.first(where: { NSIntersectionRange($0, pageRange).length > 0 }) {
            return crossing.location
        }
        return sentences.first?.location
    }

    /// The page that contains a character offset, so the reader can follow the voice.
    func pageIndex(forChapterOffset offset: Int, chapter chapterIndex: Int) -> Int? {
        pages.firstIndex { page in
            page.chapterIndex == chapterIndex
                && offset >= page.range.location
                && offset < page.range.location + page.range.length
        }
    }

    // MARK: - Finding things

    /// Everything the reader needs to show one line of a result list.
    struct Hit: Identifiable {
        let id: String
        let chapterIndex: Int
        let offset: Int
        let chapterTitle: String
        let snippet: String
        let pageIndex: Int
    }

    /// Case- and accent-insensitive scan of the whole book: Vietnamese is read
    /// with and without its diacritics, so both forms have to match.
    func search(_ query: String, limit: Int = 60) -> [Hit] {
        let needle = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard needle.count >= 2 else { return [] }

        var found: [Hit] = []
        for (index, chapter) in chapters.enumerated() {
            let text = chapter.text.string
            var from = text.startIndex
            while found.count < limit, from < text.endIndex,
                  let range = text.range(
                      of: needle,
                      options: [.caseInsensitive, .diacriticInsensitive],
                      range: from..<text.endIndex
                  ) {
                let offset = text.distance(from: text.startIndex, to: range.lowerBound)
                if let page = pageIndex(forChapterOffset: offset, chapter: index) {
                    found.append(
                        Hit(
                            id: "\(index)-\(offset)",
                            chapterIndex: index,
                            offset: offset,
                            chapterTitle: chapter.title,
                            snippet: snippet(in: text, around: range),
                            pageIndex: page
                        )
                    )
                }
                from = range.upperBound
            }
            if found.count >= limit { break }
        }
        return found
    }

    /// The same shape of line for a stored position, so a bookmark reads like a
    /// search result.
    func hit(chapterIndex: Int, offset: Int) -> Hit? {
        guard chapters.indices.contains(chapterIndex),
              let page = pageIndex(forChapterOffset: offset, chapter: chapterIndex)
        else { return nil }
        let text = chapters[chapterIndex].text.string
        let start = text.index(text.startIndex, offsetBy: min(offset, text.count))
        return Hit(
            id: "\(chapterIndex)-\(offset)",
            chapterIndex: chapterIndex,
            offset: offset,
            chapterTitle: chapters[chapterIndex].title,
            snippet: snippet(in: text, around: start..<start),
            pageIndex: page
        )
    }

    /// A window of text around a match, with the whitespace the HTML importer
    /// leaves behind squeezed out of it.
    private func snippet(in text: String, around range: Range<String.Index>) -> String {
        let start = text.index(range.lowerBound, offsetBy: -30, limitedBy: text.startIndex)
            ?? text.startIndex
        let end = text.index(range.upperBound, offsetBy: 70, limitedBy: text.endIndex)
            ?? text.endIndex
        return text[start..<end]
            .split(whereSeparator: { $0.isWhitespace })
            .joined(separator: " ")
    }

    // MARK: - Pagination

    private func rebuildPagesIfPossible(force: Bool = false) {
        guard !rawChapters.isEmpty else { return }
        guard containerSize.width > 40, containerSize.height > 40 else { return }

        let columns = ReaderMetrics.columnCount(setting: style.columns, in: containerSize)
        let rects = ReaderMetrics.columnRects(
            in: containerSize,
            safeTop: safeTop,
            margin: CGFloat(style.margin),
            columns: columns
        )
        let columnSize = rects[0].size

        // A picture is cut to the column, so the text has to be rebuilt whenever the
        // column changes size — not only when the type changes.
        if abs(columnSize.width - builtWidth) > 0.5 || abs(columnSize.height - builtHeight) > 0.5 {
            builtWidth = columnSize.width
            builtHeight = columnSize.height
            if let palette = builtPalette {
                buildChapters(style: style, palette: palette)
            }
        }

        guard !chapters.isEmpty else { return }

        let key = "\(Int(columnSize.width))x\(Int(columnSize.height))/\(columns)"
        if !force {
            guard key != paginatedKey else { return }
        }
        paginatedKey = key
        paginatedSize = columnSize

        var built: [PageContent] = []
        var starts: [Int] = []
        var entries: [TocEntry] = []

        for (chapterIndex, chapter) in chapters.enumerated() {
            starts.append(built.count)
            let ranges = Paginator.paginate(chapter.text, in: columnSize)
            var index = 0
            while index < ranges.count {
                let group = Array(ranges[index..<min(index + columns, ranges.count)])
                let span = group.dropFirst().reduce(group[0]) { NSUnionRange($0, $1) }
                built.append(
                    PageContent(
                        text: chapter.text.attributedSubstring(from: span),
                        chapterTitle: chapter.title,
                        chapterIndex: chapterIndex,
                        range: span,
                        columns: group.map {
                            NSRange(location: $0.location - span.location, length: $0.length)
                        },
                        // Placeholder numbers; the whole-book totals are filled in below.
                        pageNumber: 0,
                        pageCount: 0
                    )
                )
                index += columns
            }
            if !chapter.title.isEmpty {
                entries.append(
                    TocEntry(id: chapterIndex, title: chapter.title, pageIndex: starts[chapterIndex])
                )
            }
        }

        // The footer counts through the whole book, not the chapter.
        let total = built.count
        pages = built.enumerated().map { offset, page in
            PageContent(
                text: page.text,
                chapterTitle: page.chapterTitle,
                chapterIndex: page.chapterIndex,
                range: page.range,
                columns: page.columns,
                pageNumber: offset + 1,
                pageCount: total
            )
        }

        chapterStartPage = starts
        toc = entries
        pagesRevision += 1
    }
}

/// Turns a fraction of the book back into a place in it, and back again. Kept
/// apart from the view model so the scrubber's arithmetic can be tested without
/// opening a book.
enum NarrationMath {

    /// The chapter and offset `fraction` points at, counted in characters so a
    /// long chapter takes more of the bar than a short one.
    static func place(fraction: Double, chapterLengths: [Int]) -> (chapter: Int, offset: Int)? {
        let total = chapterLengths.reduce(0, +)
        guard total > 0 else { return nil }

        var remaining = min(Int((Double(total) * min(max(fraction, 0), 1)).rounded(.down)), total - 1)
        for (index, length) in chapterLengths.enumerated() where length > 0 {
            if remaining < length { return (index, remaining) }
            remaining -= length
        }
        return nil
    }
}
