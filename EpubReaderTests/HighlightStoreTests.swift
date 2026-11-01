import XCTest
@testable import EpubReader

final class HighlightStoreTests: XCTestCase {

    private let suiteName = "EpubReaderTests.HighlightStore"
    private var defaults: UserDefaults!

    override func setUp() {
        super.setUp()
        defaults = UserDefaults(suiteName: suiteName)
        defaults.removePersistentDomain(forName: suiteName)
    }

    override func tearDown() {
        defaults.removePersistentDomain(forName: suiteName)
        defaults = nil
        super.tearDown()
    }

    @MainActor
    private func makeStore() -> HighlightStore {
        HighlightStore(defaults: defaults)
    }

    @MainActor
    func testAMarkedPassageIsKeptAndSurvivesANewStore() throws {
        let store = makeStore()
        store.open(bookID: "a.epub")
        let mark = try XCTUnwrap(
            store.add(
                chapterIndex: 2,
                startOffset: 10,
                endOffset: 24,
                text: "một đoạn văn",
                color: .green
            )
        )

        XCTAssertEqual(mark.id, "2-10-24")
        XCTAssertEqual(mark.range, NSRange(location: 10, length: 14))
        XCTAssertEqual(mark.highlightColor, .green)

        let second = makeStore()
        second.open(bookID: "a.epub")
        XCTAssertEqual(second.highlights.count, 1)
        XCTAssertEqual(second.highlights.first?.id, "2-10-24")
        XCTAssertEqual(second.highlights.first?.text, "một đoạn văn")
    }

    @MainActor
    func testMarksBelongToTheirBook() {
        let store = makeStore()
        store.open(bookID: "a.epub")
        store.add(chapterIndex: 0, startOffset: 0, endOffset: 4, text: "Một", color: .yellow)

        store.open(bookID: "b.epub")
        XCTAssertTrue(store.highlights.isEmpty)
        store.add(chapterIndex: 1, startOffset: 2, endOffset: 8, text: "Hai đoạn", color: .blue)

        store.open(bookID: "a.epub")
        XCTAssertEqual(store.highlights.map(\.id), ["0-0-4"])
    }

    @MainActor
    func testRecolouringTheSamePassageDoesNotDuplicateIt() throws {
        let store = makeStore()
        store.open(bookID: "a.epub")
        let first = try XCTUnwrap(
            store.add(chapterIndex: 0, startOffset: 5, endOffset: 9, text: "ngọn", color: .yellow)
        )
        let second = try XCTUnwrap(
            store.add(chapterIndex: 0, startOffset: 5, endOffset: 9, text: "ngọn", color: .pink)
        )

        XCTAssertEqual(store.highlights.count, 1)
        XCTAssertEqual(first.id, second.id)
        XCTAssertEqual(store.highlights.first?.highlightColor, .pink)
    }

    @MainActor
    func testPassagesAreListedInReadingOrder() {
        let store = makeStore()
        store.open(bookID: "a.epub")
        store.add(chapterIndex: 2, startOffset: 4, endOffset: 8, text: "b", color: .yellow)
        store.add(chapterIndex: 0, startOffset: 9, endOffset: 12, text: "a", color: .yellow)
        store.add(chapterIndex: 2, startOffset: 1, endOffset: 3, text: "c", color: .yellow)

        XCTAssertEqual(store.highlights.map(\.id), ["0-9-12", "2-1-3", "2-4-8"])
    }

    @MainActor
    func testTheFingerFindsThePassageUnderIt() throws {
        let store = makeStore()
        store.open(bookID: "a.epub")
        let mark = try XCTUnwrap(
            store.add(chapterIndex: 3, startOffset: 20, endOffset: 30, text: "x", color: .blue)
        )

        XCTAssertEqual(store.highlight(chapter: 3, offset: 20)?.id, mark.id)
        XCTAssertEqual(store.highlight(chapter: 3, offset: 29)?.id, mark.id)
        XCTAssertNil(store.highlight(chapter: 3, offset: 30))
        XCTAssertNil(store.highlight(chapter: 2, offset: 25))
    }

    @MainActor
    func testRemovingAndErasingForgetTheMarks() throws {
        let store = makeStore()
        store.open(bookID: "a.epub")
        let one = try XCTUnwrap(
            store.add(chapterIndex: 0, startOffset: 0, endOffset: 3, text: "a", color: .yellow)
        )
        store.add(chapterIndex: 0, startOffset: 8, endOffset: 11, text: "b", color: .yellow)

        store.remove(one)
        XCTAssertEqual(store.highlights.count, 1)

        store.erase(bookID: "a.epub")
        XCTAssertTrue(store.highlights.isEmpty)
        let second = makeStore()
        second.open(bookID: "a.epub")
        XCTAssertTrue(second.highlights.isEmpty)
    }

    @MainActor
    func testAnEmptyOrInvertedPassageIsRefused() {
        let store = makeStore()
        store.open(bookID: "a.epub")
        XCTAssertNil(store.add(chapterIndex: 0, startOffset: 4, endOffset: 4, text: "", color: .yellow))
        XCTAssertNil(store.add(chapterIndex: 0, startOffset: 9, endOffset: 2, text: "x", color: .yellow))
        XCTAssertTrue(store.highlights.isEmpty)
    }
}
