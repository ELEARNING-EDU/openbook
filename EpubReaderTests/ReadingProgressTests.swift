import XCTest
@testable import EpubReader

final class ReadingProgressTests: XCTestCase {

    private let suiteName = "EpubReaderTests.ReadingProgress"
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
    func testABookNeverOpenedHasNoProgress() {
        let store = ReadingProgressStore(defaults: defaults)
        XCTAssertEqual(store.fraction(for: "book"), 0)
        XCTAssertNil(store.position(for: "book"))
    }

    @MainActor
    func testFractionIsClampedToZeroAndOne() {
        let store = ReadingProgressStore(defaults: defaults)

        store.mark(bookID: "a", chapter: 0, offset: 0, fraction: -3, page: 1, pageCount: 13)
        XCTAssertEqual(store.fraction(for: "a"), 0)

        store.mark(bookID: "b", chapter: 6, offset: 900, fraction: 4.2, page: 13, pageCount: 13)
        XCTAssertEqual(store.fraction(for: "b"), 1)
    }

    @MainActor
    func testThePlaceSurvivesANewStore() {
        let first = ReadingProgressStore(defaults: defaults)
        first.mark(bookID: "a", chapter: 2, offset: 789, fraction: 0.36, page: 5, pageCount: 13)

        let second = ReadingProgressStore(defaults: defaults)
        let saved = second.position(for: "a")
        XCTAssertEqual(saved?.chapter, 2)
        XCTAssertEqual(saved?.offset, 789)
        XCTAssertEqual(saved?.page, 5)
        XCTAssertEqual(saved?.pageCount, 13)
        XCTAssertEqual(saved?.fraction ?? 0, 0.36, accuracy: 0.0005)
    }

    @MainActor
    func testMarkingTheSamePlaceAgainKeepsTheFirstStamp() {
        let store = ReadingProgressStore(defaults: defaults)
        store.mark(bookID: "a", chapter: 2, offset: 789, fraction: 0.36, page: 5, pageCount: 13)
        let first = store.position(for: "a")?.updated

        store.mark(bookID: "a", chapter: 2, offset: 789, fraction: 0.36, page: 5, pageCount: 13)
        XCTAssertEqual(store.position(for: "a")?.updated, first)
    }

    @MainActor
    func testABookWithItsPagesRetypesetIsStillAChange() {
        let store = ReadingProgressStore(defaults: defaults)
        store.mark(bookID: "a", chapter: 2, offset: 789, fraction: 0.36, page: 5, pageCount: 13)
        store.mark(bookID: "a", chapter: 2, offset: 789, fraction: 0.36, page: 8, pageCount: 22)
        XCTAssertEqual(store.position(for: "a")?.pageCount, 22)
        XCTAssertEqual(store.position(for: "a")?.page, 8)
    }
}
