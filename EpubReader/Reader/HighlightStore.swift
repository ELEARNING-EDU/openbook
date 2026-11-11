import Foundation
import SwiftUI
import UIKit

/// The four colours a reader can mark a passage with.
///
/// Stored as ARGB so a highlight keeps its exact tone whichever theme or
/// appearance is in force when it is drawn again.
enum HighlightColor: UInt32, CaseIterable, Codable {
    case yellow = 0xB3FFD54A
    case green = 0xB3A5D66A
    case blue = 0xB36FA8DC
    case pink = 0xB3F28DB2

    init(argb: UInt32) {
        self = HighlightColor(rawValue: argb) ?? .yellow
    }

    var uiColor: UIColor { UIColor(argb: rawValue) }
    var swiftColor: Color { Color(uiColor) }

    var labelKey: String {
        switch self {
        case .yellow: return "reader.highlight.color.yellow"
        case .green: return "reader.highlight.color.green"
        case .blue: return "reader.highlight.color.blue"
        case .pink: return "reader.highlight.color.pink"
        }
    }
}

/// One passage the reader has marked.
///
/// Kept as a chapter plus a character range rather than as a page number: pages
/// are a product of the type size and the screen, so a page number stops meaning
/// anything after a re-typeset. The offsets are UTF-16, the same coordinates the
/// paginator, the bookmarks and the voice already use.
struct Highlight: Codable, Identifiable, Hashable {
    let chapterIndex: Int
    let startOffset: Int
    let endOffset: Int
    let text: String
    /// ARGB. Kept as a number so a new palette entry cannot break saved marks.
    let color: UInt32
    let createdAt: Date
    /// What the reader wrote beside the passage. Optional so a mark saved
    /// before notes existed still decodes — a missing key is simply no note.
    let note: String?

    /// Stable across a re-typeset: it names the text, not the page.
    var id: String { "\(chapterIndex)-\(startOffset)-\(endOffset)" }

    var range: NSRange {
        NSRange(location: startOffset, length: max(0, endOffset - startOffset))
    }

    var highlightColor: HighlightColor { HighlightColor(argb: color) }

    func contains(chapter index: Int, offset: Int) -> Bool {
        chapterIndex == index && offset >= startOffset && offset < endOffset
    }

    func isSamePassage(as other: Highlight) -> Bool {
        chapterIndex == other.chapterIndex
            && startOffset == other.startOffset
            && endOffset == other.endOffset
    }
}

/// Remembers every passage the reader has marked, per book.
///
/// One small dictionary in `UserDefaults`, beside the reading place and the
/// bookmarks; the library does not read it, so it stays with the reader.
@MainActor
final class HighlightStore: ObservableObject {

    static let shared = HighlightStore()

    /// The marks of the book currently open, oldest position first.
    @Published private(set) var highlights: [Highlight] = []

    private static let storageKey = "readerHighlights"

    private let defaults: UserDefaults
    private var all: [String: [Highlight]] = [:]
    private var current = ""

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        load()
    }

    /// Switches to a book. Marks belong to the book, not the reader.
    func open(bookID: String) {
        current = bookID
        highlights = sorted(all[bookID] ?? [])
    }

    /// The marks that fall in one chapter, for the page that is drawing them.
    func highlights(inChapter chapterIndex: Int) -> [Highlight] {
        highlights.filter { $0.chapterIndex == chapterIndex }
    }

    /// Marks a passage, or recolours it when the same passage is already marked.
    @discardableResult
    func add(
        chapterIndex: Int,
        startOffset: Int,
        endOffset: Int,
        text: String,
        color: HighlightColor
    ) -> Highlight? {
        guard startOffset >= 0, endOffset > startOffset, !current.isEmpty else { return nil }

        let candidate = Highlight(
            chapterIndex: chapterIndex,
            startOffset: startOffset,
            endOffset: endOffset,
            text: text,
            color: color.rawValue,
            createdAt: Date(),
            note: nil
        )

        if let index = highlights.firstIndex(where: { $0.isSamePassage(as: candidate) }) {
            let existing = highlights[index]
            let recoloured = Highlight(
                chapterIndex: existing.chapterIndex,
                startOffset: existing.startOffset,
                endOffset: existing.endOffset,
                text: existing.text,
                color: color.rawValue,
                createdAt: existing.createdAt,
                note: existing.note
            )
            highlights[index] = recoloured
            persist()
            return recoloured
        }

        highlights.append(candidate)
        highlights = sorted(highlights)
        persist()
        return candidate
    }

    /// The mark under a finger, so a long press on it can offer to recolour or
    /// remove it.
    func highlight(chapter index: Int, offset: Int) -> Highlight? {
        highlights.first { $0.contains(chapter: index, offset: offset) }
    }

    func updateColor(of highlight: Highlight, to color: HighlightColor) {
        guard let index = highlights.firstIndex(where: { $0.id == highlight.id }) else { return }
        let existing = highlights[index]
        highlights[index] = Highlight(
            chapterIndex: existing.chapterIndex,
            startOffset: existing.startOffset,
            endOffset: existing.endOffset,
            text: existing.text,
            color: color.rawValue,
            createdAt: existing.createdAt,
            note: existing.note
        )
        persist()
    }

    /// Writes or clears the note beside a mark. A blank note is no note.
    func setNote(_ note: String?, of highlight: Highlight) {
        guard let index = highlights.firstIndex(where: { $0.id == highlight.id }) else { return }
        let cleaned = note?.trimmingCharacters(in: .whitespacesAndNewlines)
        let existing = highlights[index]
        highlights[index] = Highlight(
            chapterIndex: existing.chapterIndex,
            startOffset: existing.startOffset,
            endOffset: existing.endOffset,
            text: existing.text,
            color: existing.color,
            createdAt: existing.createdAt,
            note: cleaned?.isEmpty == false ? cleaned : nil
        )
        persist()
    }

    func remove(_ highlight: Highlight) {
        highlights.removeAll { $0.id == highlight.id }
        persist()
    }

    /// Deletes by index in the sorted list, which is the order the list shows.
    func remove(at sortedOffsets: IndexSet) {
        for index in sortedOffsets.sorted(by: >) where highlights.indices.contains(index) {
            highlights.remove(at: index)
        }
        persist()
    }

    /// Forgets every mark in a book that has left the shelf.
    func erase(bookID: String) {
        guard all[bookID] != nil else { return }
        all[bookID] = nil
        if current == bookID { highlights = [] }
        persist()
    }

    // MARK: - Storage

    private func sorted(_ marks: [Highlight]) -> [Highlight] {
        marks.sorted {
            ($0.chapterIndex, $0.startOffset, $0.endOffset)
                < ($1.chapterIndex, $1.startOffset, $1.endOffset)
        }
    }

    private func load() {
        guard let data = defaults.data(forKey: Self.storageKey),
              let decoded = try? JSONDecoder().decode([String: [Highlight]].self, from: data)
        else { return }
        all = decoded
    }

    private func persist() {
        guard !current.isEmpty else { return }
        all[current] = highlights
        if let data = try? JSONEncoder().encode(all) {
            defaults.set(data, forKey: Self.storageKey)
        }
    }
}

extension UIColor {

    /// `0xAARRGGBB`, the form a highlight's colour is stored in.
    convenience init(argb: UInt32) {
        self.init(
            red: CGFloat((argb >> 16) & 0xFF) / 255,
            green: CGFloat((argb >> 8) & 0xFF) / 255,
            blue: CGFloat(argb & 0xFF) / 255,
            alpha: CGFloat((argb >> 24) & 0xFF) / 255
        )
    }
}
