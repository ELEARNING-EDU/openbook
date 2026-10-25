import XCTest
@testable import EpubReader

final class LibraryOrderingTests: XCTestCase {

    private func book(_ id: String, title: String, author: String) -> BookItem {
        BookItem(
            id: id,
            url: URL(fileURLWithPath: "/books/\(id)"),
            title: title,
            author: author
        )
    }

    private func metadata(_ id: String, at seconds: TimeInterval) -> LibraryMetadata {
        LibraryMetadata(title: nil, author: nil, importedAt: Date(timeIntervalSince1970: seconds))
    }

    func testRecentOrderUsesImportDateThenIdentity() {
        let books = [book("a", title: "A", author: "A"), book("b", title: "B", author: "B")]
        let meta = ["a": metadata("a", at: 10), "b": metadata("b", at: 10)]

        let sorted = LibraryOrder.sorted(books: books, metadata: meta, by: .recent, locale: Locale(identifier: "en"))

        XCTAssertEqual(sorted.map(\.id), ["a", "b"])
    }

    func testEditingATitleDoesNotMoveABookInRecentOrder() {
        let books = [book("old", title: "Zzz", author: "A"), book("new", title: "Aaa", author: "B")]
        let meta = ["old": metadata("old", at: 10), "new": metadata("new", at: 20)]

        let sorted = LibraryOrder.sorted(books: books, metadata: meta, by: .recent, locale: Locale(identifier: "en"))

        XCTAssertEqual(sorted.map(\.id), ["new", "old"])
    }

    func testTitleOrderIsLocaleAwareAndTiesBreakOnIdentity() {
        let books = [
            book("b", title: "Zebra", author: "A"),
            book("a", title: "Ängel", author: "B"),
            book("c", title: "Zebra", author: "C")
        ]
        let meta: [String: LibraryMetadata] = [:]

        let english = LibraryOrder.sorted(books: books, metadata: meta, by: .title, locale: Locale(identifier: "en"))
        let swedish = LibraryOrder.sorted(books: books, metadata: meta, by: .title, locale: Locale(identifier: "sv"))

        XCTAssertEqual(english.map(\.id), ["a", "b", "c"])
        XCTAssertEqual(swedish.map(\.id), ["b", "c", "a"])
    }

    func testAuthorOrderThenTitleThenIdentity() {
        let books = [
            book("c", title: "Beta", author: "An"),
            book("b", title: "Alpha", author: "An"),
            book("a", title: "Alpha", author: "An"),
            book("d", title: "Gamma", author: "Bình")
        ]
        let sorted = LibraryOrder.sorted(
            books: books,
            metadata: [:],
            by: .author,
            locale: Locale(identifier: "vi")
        )

        XCTAssertEqual(sorted.map(\.id), ["a", "b", "c", "d"])
    }
}

final class LibraryStoreTests: XCTestCase {

    private var directory: URL!
    private var defaults: UserDefaults!
    private var suiteName: String!

    override func setUpWithError() throws {
        suiteName = "EpubReaderTests.LibraryStore.\(UUID().uuidString)"
        defaults = UserDefaults(suiteName: suiteName)
        defaults.removePersistentDomain(forName: suiteName)
        directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("LibraryStoreTests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    }

    override func tearDownWithError() throws {
        try? FileManager.default.removeItem(at: directory)
        defaults.removePersistentDomain(forName: suiteName)
        directory = nil
        defaults = nil
        suiteName = nil
    }

    private func writeBook(_ name: String) throws -> URL {
        let url = directory.appendingPathComponent(name)
        let sample = try XCTUnwrap(
            Bundle.main.url(forResource: "sample", withExtension: "epub"),
            "the bundled sample book is missing from the app"
        )
        try FileManager.default.copyItem(at: sample, to: url)
        return url
    }

    @MainActor
    func testEditedMetadataSurvivesANewStoreAndKeepsTheImportDate() throws {
        try writeBook("a.epub")
        let first = LibraryStore(directory: directory, defaults: defaults, seedSample: false)
        let originalDate = try XCTUnwrap(first.metadata(for: "a.epub")?.importedAt)

        XCTAssertTrue(first.updateMetadata(id: "a.epub", title: "Tên mới", author: "Tác giả mới"))
        let second = LibraryStore(directory: directory, defaults: defaults, seedSample: false)

        XCTAssertEqual(second.books.first?.title, "Tên mới")
        XCTAssertEqual(second.books.first?.author, "Tác giả mới")
        XCTAssertEqual(second.metadata(for: "a.epub")?.importedAt, originalDate)
        XCTAssertEqual(second.books.first?.id, "a.epub")
    }

    @MainActor
    func testEditingATitleDoesNotMoveTheBookInRecentOrder() throws {
        try writeBook("a.epub")
        try writeBook("b.epub")
        let store = LibraryStore(directory: directory, defaults: defaults, seedSample: false)
        let before = store.sortedBooks(by: .recent).map(\.id)

        store.updateMetadata(id: "a.epub", title: "Zzz", author: "A")

        XCTAssertEqual(store.sortedBooks(by: .recent).map(\.id), before)
        XCTAssertEqual(store.books.first { $0.id == "a.epub" }?.title, "Zzz")
    }

    @MainActor
    func testDeletingRemovesOnlyTheAppCopyAndLeavesOtherBooks() throws {
        let external = FileManager.default.temporaryDirectory
            .appendingPathComponent("external-\(UUID().uuidString).epub")
        let sample = try XCTUnwrap(Bundle.main.url(forResource: "sample", withExtension: "epub"))
        try FileManager.default.copyItem(at: sample, to: external)
        defer { try? FileManager.default.removeItem(at: external) }

        let store = LibraryStore(directory: directory, defaults: defaults, seedSample: false)
        store.importBook(from: external)
        try writeBook("other.epub")
        store.reload()

        let id = external.lastPathComponent
        let removed = store.deleteBooks(ids: [id])

        XCTAssertEqual(removed, [id])
        XCTAssertFalse(FileManager.default.fileExists(atPath: directory.appendingPathComponent(id).path))
        XCTAssertTrue(FileManager.default.fileExists(atPath: external.path))
        XCTAssertEqual(store.books.map(\.id), ["other.epub"])
        XCTAssertNil(store.metadata(for: id))
        XCTAssertNil(store.deleteError)
    }

    @MainActor
    func testAFailedRemovalKeepsTheBookAndReportsIt() throws {
        let url = try writeBook("locked.epub")
        let store = LibraryStore(directory: directory, defaults: defaults, seedSample: false)

        try FileManager.default.setAttributes([.posixPermissions: 0o500], ofItemAtPath: directory.path)
        defer {
            try? FileManager.default.setAttributes([.posixPermissions: 0o700], ofItemAtPath: directory.path)
        }

        let removed = store.deleteBooks(ids: ["locked.epub"])

        XCTAssertTrue(removed.isEmpty)
        XCTAssertTrue(FileManager.default.fileExists(atPath: url.path))
        XCTAssertEqual(store.books.map(\.id), ["locked.epub"])
        XCTAssertNotNil(store.deleteError)
    }

    @MainActor
    func testDeletedSampleIsNotSeededAgain() throws {
        let first = LibraryStore(directory: directory, defaults: defaults, seedSample: true)
        let sample = try XCTUnwrap(first.books.first { $0.id.contains("ngon-hai-dang") })
        _ = first.deleteBooks(ids: [sample.id])

        let second = LibraryStore(directory: directory, defaults: defaults, seedSample: true)

        XCTAssertFalse(second.books.contains { $0.id == sample.id })
        XCTAssertFalse(
            FileManager.default.fileExists(atPath: directory.appendingPathComponent(sample.id).path)
        )
    }
}
