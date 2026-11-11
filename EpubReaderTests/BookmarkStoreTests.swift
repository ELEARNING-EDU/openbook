import XCTest
@testable import EpubReader

final class BookmarkStoreTests: XCTestCase {

    private var suiteName: String!
    private var defaults: UserDefaults!

    override func setUpWithError() throws {
        suiteName = "EpubReaderTests.BookmarkStore.\(UUID().uuidString)"
        defaults = UserDefaults(suiteName: suiteName)
        defaults.removePersistentDomain(forName: suiteName)
    }

    override func tearDownWithError() throws {
        defaults.removePersistentDomain(forName: suiteName)
        defaults = nil
        suiteName = nil
    }

    private func book(_ name: String) -> URL {
        URL(fileURLWithPath: "/books/\(name)")
    }

    private func mark(_ store: BookmarkStore, chapter: Int, offset: Int) {
        store.toggle(
            chapterIndex: chapter,
            range: NSRange(location: offset, length: 1),
            offset: offset
        )
    }

    func testAMarkSurvivesANewStore() {
        let first = BookmarkStore(defaults: defaults)
        first.open(book: book("a.epub"))
        mark(first, chapter: 2, offset: 40)

        let second = BookmarkStore(defaults: defaults)
        second.open(book: book("a.epub"))

        XCTAssertEqual(second.marks.map(\.offset), [40])
    }

    func testErasingABookForgetsItsMarksAndKeepsTheOthers() {
        let store = BookmarkStore(defaults: defaults)
        store.open(book: book("a.epub"))
        mark(store, chapter: 1, offset: 10)
        store.open(book: book("b.epub"))
        mark(store, chapter: 1, offset: 20)

        store.erase(bookID: "a.epub")

        XCTAssertEqual(store.marks.map(\.offset), [20], "the open book keeps its mark")

        let reopened = BookmarkStore(defaults: defaults)
        reopened.open(book: book("a.epub"))
        XCTAssertTrue(reopened.marks.isEmpty)
        reopened.open(book: book("b.epub"))
        XCTAssertEqual(reopened.marks.map(\.offset), [20])
    }

    func testErasingTheOpenBookClearsItsMarksAtOnce() {
        let store = BookmarkStore(defaults: defaults)
        store.open(book: book("a.epub"))
        mark(store, chapter: 1, offset: 10)

        store.erase(bookID: "a.epub")

        XCTAssertTrue(store.marks.isEmpty)
    }
}
