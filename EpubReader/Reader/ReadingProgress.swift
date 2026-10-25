import Foundation

/// Where the reader had got to in one book, and how far through it that is.
///
/// The place is kept as a chapter and a character offset rather than as a page
/// number: pages are a product of the type size, the margins and the screen, so a
/// page number means nothing after the reader grows the type. A chapter plus an
/// offset survives all of that, and can be turned back into a page once the book
/// has been paginated again.
struct BookProgress: Codable, Equatable {
    var chapter: Int
    var offset: Int
    /// How much of the book is behind the reader, counted in characters, so the
    /// library's figure does not jump when the type size changes.
    var fraction: Double
    /// The page it was on. Only a fallback, for the day the chapter anchor no
    /// longer fits — a re-imported file, say.
    var page: Int
    var pageCount: Int
    var updated: Date
}

/// Remembers the place in every book.
///
/// Two things read it: the reader, to open a book where the reading stopped, and
/// the library, to say how far each book has been read. It is one small file's
/// worth of state, so it lives in `UserDefaults` beside the reader's settings and
/// is shared by the two screens.
@MainActor
final class ReadingProgressStore: ObservableObject {

    static let shared = ReadingProgressStore()

    @Published private(set) var positions: [String: BookProgress] = [:]

    private static let key = "readerPositions"

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        load()
    }

    func position(for bookID: String) -> BookProgress? {
        positions[bookID]
    }

    /// 0…1, as the library shows it. A book never opened is 0.
    func fraction(for bookID: String) -> Double {
        positions[bookID]?.fraction ?? 0
    }

    /// Records the place the reader is at. Called on every page turn, so it keeps
    /// quiet when nothing has actually moved.
    func mark(
        bookID: String,
        chapter: Int,
        offset: Int,
        fraction: Double,
        page: Int,
        pageCount: Int
    ) {
        let clamped = min(1, max(0, fraction))
        if let old = positions[bookID],
           old.chapter == chapter,
           old.offset == offset,
           old.pageCount == pageCount,
           abs(old.fraction - clamped) < 0.0005 {
            return
        }

        positions[bookID] = BookProgress(
            chapter: chapter,
            offset: offset,
            fraction: clamped,
            page: page,
            pageCount: pageCount,
            updated: Date()
        )
        persist()
    }

    /// Forgets the place in a book that has left the shelf.
    func remove(bookID: String) {
        guard positions[bookID] != nil else { return }
        positions[bookID] = nil
        persist()
    }

    // MARK: - Storage

    private func load() {
        guard let data = defaults.data(forKey: Self.key),
              let decoded = try? JSONDecoder().decode([String: BookProgress].self, from: data)
        else { return }
        positions = decoded
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(positions) else { return }
        defaults.set(data, forKey: Self.key)
    }
}
