import Foundation
import SwiftUI

/// Owns the on-device library: a `Books/` folder in the app's Documents directory.
///
/// Only files inside that folder are ever removed. An EPUB the reader imported is
/// copied in, so the original the file picker handed over stays wherever it was.
@MainActor
final class LibraryStore: ObservableObject {

    @Published var books: [BookItem] = []
    @Published var importError: String?
    @Published var deleteError: String?

    private let directory: URL
    private let defaults: UserDefaults
    private let seedSample: Bool

    private static let metadataKey = "libraryMetadata"
    private static let seededKey = "librarySeededSample"

    private var metadata: [String: LibraryMetadata] = [:]

    init(
        directory: URL? = nil,
        defaults: UserDefaults = .standard,
        seedSample: Bool = true
    ) {
        if let directory {
            self.directory = directory
        } else {
            let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            self.directory = documents.appendingPathComponent("Books", isDirectory: true)
        }
        self.defaults = defaults
        self.seedSample = seedSample

        try? FileManager.default.createDirectory(at: self.directory, withIntermediateDirectories: true)
        loadMetadata()
        if seedSample { seedBundledSample() }
        reload()
    }

    // MARK: - Reading the shelf

    func reload() {
        let contents = (try? FileManager.default.contentsOfDirectory(
            at: directory,
            includingPropertiesForKeys: [.creationDateKey, .contentModificationDateKey],
            options: [.skipsHiddenFiles]
        )) ?? []

        let epubs = contents
            .filter { $0.pathExtension.lowercased() == "epub" }
            .sorted { $0.lastPathComponent < $1.lastPathComponent }

        var changed = false
        books = epubs.map { url in
            let id = url.lastPathComponent
            var entry = metadata[id]
            if entry == nil {
                entry = LibraryMetadata(
                    title: nil,
                    author: nil,
                    importedAt: Self.fileDate(url) ?? Date()
                )
                metadata[id] = entry
                changed = true
            }
            let parsed = try? EpubArchiveReader.readMetadata(url: url)
            return BookItem(
                id: id,
                url: url,
                title: entry?.title ?? parsed?.title ?? url.deletingPathExtension().lastPathComponent,
                author: entry?.author
                    ?? (parsed?.author.isEmpty == false ? parsed?.author : nil)
                    ?? L10n.string("library.unknownAuthor")
            )
        }

        // Drop metadata left behind by a file the reader removed outside the app.
        let live = Set(books.map(\.id))
        for id in metadata.keys where !live.contains(id) {
            metadata[id] = nil
            changed = true
        }
        if changed { persistMetadata() }
    }

    func sortedBooks(by sort: LibrarySort, locale: Locale = L10n.locale) -> [BookItem] {
        LibraryOrder.sorted(books: books, metadata: metadata, by: sort, locale: locale)
    }

    func metadata(for id: String) -> LibraryMetadata? { metadata[id] }

    /// Read-only copy for the screens that rank books themselves (Home's rows).
    var allMetadata: [String: LibraryMetadata] { metadata }

    // MARK: - Importing

    func importBook(from source: URL) {
        let scoped = source.startAccessingSecurityScopedResource()
        defer { if scoped { source.stopAccessingSecurityScopedResource() } }

        do {
            let destination = directory.appendingPathComponent(source.lastPathComponent)
            let copied = source.standardizedFileURL != destination.standardizedFileURL
            if copied {
                if FileManager.default.fileExists(atPath: destination.path) {
                    try FileManager.default.removeItem(at: destination)
                }
                try FileManager.default.copyItem(at: source, to: destination)
            }
            do {
                try EpubArchiveReader.validate(url: destination)
            } catch {
                // A DRM wrapper or a broken file is refused at the door rather
                // than sitting on the shelf and failing when it is opened.
                if copied { try? FileManager.default.removeItem(at: destination) }
                throw error
            }
            metadata[destination.lastPathComponent] = LibraryMetadata(
                title: nil,
                author: nil,
                importedAt: Date()
            )
            persistMetadata()
            importError = nil
            reload()
        } catch {
            importError = error.localizedDescription
        }
    }

    // MARK: - Editing display metadata

    /// Changes only what the shelf and the reader call the book. The file name,
    /// the saved position and the EPUB itself are untouched.
    @discardableResult
    func updateMetadata(id: String, title: String, author: String) -> Bool {
        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanTitle.isEmpty, metadata[id] != nil else { return false }
        let cleanAuthor = author.trimmingCharacters(in: .whitespacesAndNewlines)

        metadata[id]?.title = cleanTitle
        metadata[id]?.author = cleanAuthor.isEmpty ? nil : cleanAuthor
        persistMetadata()
        reload()
        return true
    }

    // MARK: - Finished

    var finishedIDs: Set<String> {
        Set(metadata.compactMap { $0.value.finishedAt == nil ? nil : $0.key })
    }

    /// Manual "I finished this": keeps the reading place and the file, and is
    /// reversible from the same book menu.
    @discardableResult
    func markFinished(id: String, finished: Bool) -> Bool {
        guard metadata[id] != nil else { return false }
        metadata[id]?.finishedAt = finished ? Date() : nil
        persistMetadata()
        reload()
        return true
    }

    /// A finished book with the day it was marked, for the sheet's month line.
    struct FinishedBook: Identifiable {
        let book: BookItem
        let finishedAt: Date
        var id: String { book.id }
    }

    func finishedBooks(in year: Int, calendar: Calendar = .current) -> [FinishedBook] {
        books
            .compactMap { book in
                guard let date = metadata[book.id]?.finishedAt,
                      calendar.component(.year, from: date) == year
                else { return nil }
                return FinishedBook(book: book, finishedAt: date)
            }
            .sorted { left, right in
                if left.finishedAt != right.finishedAt { return left.finishedAt > right.finishedAt }
                return left.id < right.id
            }
    }

    // MARK: - Deleting

    /// Removes the app's own copies. Returns the ids that are actually gone, so
    /// the caller can clear their saved places too. A failure on one file leaves
    /// the others alone and is reported.
    @discardableResult
    func deleteBooks(ids: Set<String>) -> Set<String> {
        var removed: Set<String> = []
        var failed: [String] = []

        for book in books where ids.contains(book.id) {
            // The shelf only ever holds copies inside its own folder. The check
            // is here so a bug elsewhere cannot point this at an original file.
            guard book.url.deletingLastPathComponent().standardizedFileURL
                == directory.standardizedFileURL
            else {
                failed.append(book.title)
                continue
            }

            do {
                if FileManager.default.fileExists(atPath: book.url.path) {
                    try FileManager.default.removeItem(at: book.url)
                }
                metadata[book.id] = nil
                removed.insert(book.id)
            } catch {
                failed.append(book.title)
            }
        }

        persistMetadata()
        reload()
        deleteError = failed.isEmpty
            ? nil
            : L10n.format("library.delete.failed.message", failed.joined(separator: ", "))
        return removed
    }

    func clearErrors() {
        importError = nil
        deleteError = nil
    }

    // MARK: - Storage

    private func loadMetadata() {
        guard let data = defaults.data(forKey: Self.metadataKey),
              let decoded = try? JSONDecoder().decode([String: LibraryMetadata].self, from: data)
        else { return }
        metadata = decoded
    }

    private func persistMetadata() {
        guard let data = try? JSONEncoder().encode(metadata) else { return }
        defaults.set(data, forKey: Self.metadataKey)
    }

    /// Copies the EPUB shipped inside the app bundle on first launch, so the
    /// library is never empty on a fresh install. A sample the reader deletes is
    /// not put back on the next launch.
    private func seedBundledSample() {
        guard !defaults.bool(forKey: Self.seededKey) else { return }
        let target = directory.appendingPathComponent("ngon-hai-dang-cuoi-cung.epub")
        if FileManager.default.fileExists(atPath: target.path) {
            defaults.set(true, forKey: Self.seededKey)
            return
        }
        guard let source = Bundle.main.url(forResource: "sample", withExtension: "epub") else { return }
        do {
            try FileManager.default.copyItem(at: source, to: target)
            defaults.set(true, forKey: Self.seededKey)
        } catch {
            // A failed first seed may be tried again next launch.
        }
    }

    private static func fileDate(_ url: URL) -> Date? {
        let values = try? url.resourceValues(forKeys: [.creationDateKey, .contentModificationDateKey])
        return values?.creationDate ?? values?.contentModificationDate
    }
}
