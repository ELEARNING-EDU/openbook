import Foundation

/// The extra shelves Home offers once the library has enough in it.
///
/// Pure functions over the app's own data: no new store, no network, no
/// heuristics beyond the one the owner asked for — a suggestion is a book by an
/// author the reader has just been reading.
enum HomeSections {

    static let minimumLibrarySize = 6
    static let rowLimit = 5

    static func showsExtraRows(bookCount: Int) -> Bool {
        bookCount >= minimumLibrarySize
    }

    /// Books that have never been opened and are not marked finished, newest
    /// import first.
    static func newlyAdded(
        books: [BookItem],
        positions: [String: BookProgress],
        metadata: [String: LibraryMetadata],
        finished: Set<String>,
        limit: Int = rowLimit
    ) -> [BookItem] {
        books
            .filter { book in
                !finished.contains(book.id) && (positions[book.id]?.fraction ?? 0) <= 0
            }
            .sorted { left, right in
                let leftDate = metadata[left.id]?.importedAt ?? .distantPast
                let rightDate = metadata[right.id]?.importedAt ?? .distantPast
                if leftDate != rightDate { return leftDate > rightDate }
                return left.id < right.id
            }
            .prefix(limit)
            .map { $0 }
    }

    /// Books marked finished in the last year, most recently finished first.
    static func recentlyFinished(
        books: [BookItem],
        metadata: [String: LibraryMetadata],
        now: Date,
        calendar: Calendar = .current,
        months: Int = 12,
        limit: Int = rowLimit
    ) -> [BookItem] {
        let cutoff = calendar.date(byAdding: .month, value: -months, to: now) ?? .distantPast
        return books
            .compactMap { book -> (BookItem, Date)? in
                guard let finished = metadata[book.id]?.finishedAt,
                      finished >= cutoff
                else { return nil }
                return (book, finished)
            }
            .sorted { left, right in
                if left.1 != right.1 { return left.1 > right.1 }
                return left.0.id < right.0.id
            }
            .prefix(limit)
            .map(\.0)
    }

    /// Unread books by an author of the three books the reader touched most
    /// recently. Two books are needed before the shelf is worth showing; when no
    /// author matches, the shelf stays hidden rather than guessing.
    static func suggestions(
        books: [BookItem],
        positions: [String: BookProgress],
        metadata: [String: LibraryMetadata],
        finished: Set<String>,
        excluding: Set<String>,
        now: Date,
        calendar: Calendar = .current,
        limit: Int = rowLimit
    ) -> [BookItem] {
        let recent = books
            .compactMap { book -> (BookItem, Date)? in
                let finishedAt = metadata[book.id]?.finishedAt
                let readAt = positions[book.id]?.updated
                guard let date = finishedAt ?? readAt else { return nil }
                return (book, date)
            }
            .sorted { left, right in
                if left.1 != right.1 { return left.1 > right.1 }
                return left.0.id < right.0.id
            }
            .prefix(3)

        let authors = Set(recent.map { normalizedAuthor($0.0.author) }.filter { !$0.isEmpty })
        guard !authors.isEmpty else { return [] }

        let candidates = books
            .filter { book in
                !finished.contains(book.id)
                    && !excluding.contains(book.id)
                    && (positions[book.id]?.fraction ?? 0) <= 0
                    && authors.contains(normalizedAuthor(book.author))
            }
            .sorted { left, right in
                let leftDate = metadata[left.id]?.importedAt ?? .distantPast
                let rightDate = metadata[right.id]?.importedAt ?? .distantPast
                if leftDate != rightDate { return leftDate > rightDate }
                return left.id < right.id
            }

        guard candidates.count >= 2 else { return [] }
        return Array(candidates.prefix(limit))
    }

    /// Authors are matched the way a reader reads them: accents, case and extra
    /// spaces must not split "Nguyễn Minh An" from "Nguyen Minh An".
    private static func normalizedAuthor(_ author: String) -> String {
        author
            .folding(options: [.diacriticInsensitive, .caseInsensitive, .widthInsensitive], locale: nil)
            .split(whereSeparator: \.isWhitespace)
            .joined(separator: " ")
    }
}
