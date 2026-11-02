import Foundation
import SwiftUI
import UIKit

/// Whether a book has a real cover image or the generated one.
enum BookCoverState: Equatable {
    case unknown
    case generated
    case image
}

/// Reads a book's cover out of its EPUB once, then keeps it for the session.
///
/// The cover is not persisted: the EPUB is the source of truth, and a cache key
/// tied to the file's modification date makes a re-import read the new cover.
/// Only covers that come into view are read, so a large shelf is not parsed on
/// launch.
@MainActor
final class BookCoverStore: ObservableObject {

    static let shared = BookCoverStore()

    @Published private(set) var states: [String: BookCoverState] = [:]

    private var images: [String: UIImage] = [:]
    private var generated: Set<String> = []
    private var tasks: [String: Task<UIImage?, Never>] = [:]

    func state(for book: BookItem) -> BookCoverState {
        states[book.id] ?? .unknown
    }

    func image(for book: BookItem) async -> UIImage? {
        let key = book.coverKey
        if let image = images[key] { return image }
        if generated.contains(key) { return nil }
        if let task = tasks[key] { return await task.value }

        let url = book.url
        let task = Task<UIImage?, Never> {
            await Task.detached(priority: .utility) {
                guard let data = try? EpubArchiveReader.coverData(url: url) else {
                    return nil
                }
                return BookCoverImage.usableImage(from: data)
            }.value
        }
        tasks[key] = task

        let image = await task.value
        tasks[key] = nil
        if let image {
            images[key] = image
            states[book.id] = .image
        } else {
            generated.insert(key)
            states[book.id] = .generated
        }
        return image
    }
}

extension BookItem {

    /// Changes when the EPUB file is replaced, but not when only the display
    /// title is edited, so a corrected title never throws its image away.
    var coverKey: String {
        let values = try? url.resourceValues(forKeys: [.contentModificationDateKey])
        let date = values?.contentModificationDate ?? .distantPast
        return "\(id)|\(date.timeIntervalSince1970)"
    }
}
