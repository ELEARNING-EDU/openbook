import Foundation

/// One saved place in a book.
///
/// Stored as a chapter plus a character offset rather than as a page number,
/// because page numbers do not survive a change of type size or theme.
struct Bookmark: Codable, Identifiable, Hashable {
    let chapterIndex: Int
    let offset: Int

    var id: String { "\(chapterIndex)-\(offset)" }
}

/// The bookmarks of every book the reader has opened, kept in `UserDefaults`.
final class BookmarkStore: ObservableObject {

    /// The marks of the book currently open, newest order irrelevant: they are
    /// always kept sorted by position.
    @Published private(set) var marks: [Bookmark] = []

    private static let storageKey = "readerBookmarks"

    private var all: [String: [Bookmark]] = [:]
    private var current = ""

    init() {
        if let data = UserDefaults.standard.data(forKey: Self.storageKey),
           let decoded = try? JSONDecoder().decode([String: [Bookmark]].self, from: data) {
            all = decoded
        }
    }

    /// Switches to a book. Bookmarks belong to the book, not the reader.
    func open(book url: URL) {
        current = url.lastPathComponent
        marks = all[current] ?? []
    }

    /// A page counts as marked when a mark falls inside it, so the state follows
    /// the page even after the book has been re-typeset.
    func isMarked(chapterIndex: Int, range: NSRange) -> Bool {
        marks.contains { $0.chapterIndex == chapterIndex && range.contains($0.offset) }
    }

    /// Marks the page, or clears the mark it already carries.
    func toggle(chapterIndex: Int, range: NSRange, offset: Int) {
        if let index = marks.firstIndex(where: {
            $0.chapterIndex == chapterIndex && range.contains($0.offset)
        }) {
            marks.remove(at: index)
        } else {
            marks.append(Bookmark(chapterIndex: chapterIndex, offset: offset))
            marks.sort { ($0.chapterIndex, $0.offset) < ($1.chapterIndex, $1.offset) }
        }
        persist()
    }

    func remove(at offsets: IndexSet) {
        marks.remove(atOffsets: offsets)
        persist()
    }

    private func persist() {
        guard !current.isEmpty else { return }
        all[current] = marks
        if let data = try? JSONEncoder().encode(all) {
            UserDefaults.standard.set(data, forKey: Self.storageKey)
        }
    }
}
