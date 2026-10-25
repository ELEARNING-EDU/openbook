import Foundation

/// One book that is part-way read, as the home screen lists it.
struct ContinueReadingItem: Identifiable {
    let book: BookItem
    let progress: BookProgress

    var id: String { book.id }
}

enum HomeFeed {

    /// The books worth offering to continue: opened at least once and not
    /// finished, most recently read first.
    ///
    /// A saved position belongs to a file, so progress left behind by a book
    /// that has since been deleted from the shelf is ignored.
    static func continueReading(
        books: [BookItem],
        positions: [String: BookProgress]
    ) -> [ContinueReadingItem] {
        books.compactMap { book in
            guard let progress = positions[book.id],
                  progress.fraction > 0,
                  progress.fraction < 1
            else { return nil }
            return ContinueReadingItem(book: book, progress: progress)
        }
        .sorted { left, right in
            if left.progress.updated != right.progress.updated {
                return left.progress.updated > right.progress.updated
            }
            return left.book.id < right.book.id
        }
    }
}
