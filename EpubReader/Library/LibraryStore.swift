import Foundation
import SwiftUI

/// Owns the on-device library: a `Books/` folder in the app's Documents directory.
@MainActor
final class LibraryStore: ObservableObject {

    @Published var books: [BookItem] = []
    @Published var importError: String?

    private let directory: URL

    init() {
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        directory = documents.appendingPathComponent("Books", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        seedBundledSample()
        reload()
    }

    func reload() {
        let contents = (try? FileManager.default.contentsOfDirectory(
            at: directory,
            includingPropertiesForKeys: nil
        )) ?? []

        let epubs = contents
            .filter { $0.pathExtension.lowercased() == "epub" }
            .sorted { $0.lastPathComponent < $1.lastPathComponent }

        books = epubs.map { url in
            let metadata = try? EpubArchiveReader.readMetadata(url: url)
            return BookItem(
                id: url.lastPathComponent,
                url: url,
                title: metadata?.title ?? url.deletingPathExtension().lastPathComponent,
                author: metadata?.author ?? "Không rõ tác giả"
            )
        }
    }

    func importBook(from source: URL) {
        let scoped = source.startAccessingSecurityScopedResource()
        defer { if scoped { source.stopAccessingSecurityScopedResource() } }

        do {
            let destination = directory.appendingPathComponent(source.lastPathComponent)
            if FileManager.default.fileExists(atPath: destination.path) {
                try FileManager.default.removeItem(at: destination)
            }
            try FileManager.default.copyItem(at: source, to: destination)
            reload()
        } catch {
            importError = error.localizedDescription
        }
    }

    /// Copies the EPUB shipped inside the app bundle on first launch, so the
    /// library is never empty on a fresh install.
    private func seedBundledSample() {
        let target = directory.appendingPathComponent("ngon-hai-dang-cuoi-cung.epub")
        guard !FileManager.default.fileExists(atPath: target.path) else { return }
        guard let source = Bundle.main.url(forResource: "sample", withExtension: "epub") else { return }
        try? FileManager.default.copyItem(at: source, to: target)
    }
}
