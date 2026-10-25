import XCTest
@testable import EpubReader

final class HomeFeedTests: XCTestCase {

    private func book(_ id: String) -> BookItem {
        BookItem(
            id: id,
            url: URL(fileURLWithPath: "/books/\(id).epub"),
            title: "Sách \(id)",
            author: "Tác giả \(id)"
        )
    }

    private func progress(
        _ fraction: Double,
        minutesAgo: Double = 0,
        page: Int = 3
    ) -> BookProgress {
        BookProgress(
            chapter: 1,
            offset: 120,
            fraction: fraction,
            page: page,
            pageCount: 12,
            updated: Date(timeIntervalSince1970: 1_000_000 - minutesAgo * 60)
        )
    }

    func testOnlyBooksOpenedAndNotFinishedAreContinued() {
        let books = [book("a"), book("b"), book("c"), book("d")]
        let positions = [
            "a": progress(0.4, minutesAgo: 10),
            "b": progress(0),
            "c": progress(0.999, minutesAgo: 1),
            "d": progress(1)
        ]

        let items = HomeFeed.continueReading(books: books, positions: positions)

        XCTAssertEqual(items.map(\.book.id), ["c", "a"])
        XCTAssertEqual(items.first?.progress.fraction ?? 0, 0.999, accuracy: 0.0001)
    }

    func testMostRecentlyReadComesFirst() {
        let books = [book("a"), book("b"), book("c")]
        let positions = [
            "a": progress(0.2, minutesAgo: 30),
            "b": progress(0.8, minutesAgo: 2),
            "c": progress(0.5, minutesAgo: 60)
        ]

        let items = HomeFeed.continueReading(books: books, positions: positions)

        XCTAssertEqual(items.map(\.book.id), ["b", "a", "c"])
    }

    func testBooksReadAtTheSameTimeFallBackToAStableOrder() {
        let books = [book("b"), book("a")]
        let stamp = Date(timeIntervalSince1970: 1_000_000)
        let same = BookProgress(
            chapter: 1,
            offset: 10,
            fraction: 0.3,
            page: 2,
            pageCount: 10,
            updated: stamp
        )

        let items = HomeFeed.continueReading(books: books, positions: ["a": same, "b": same])

        XCTAssertEqual(items.map(\.book.id), ["a", "b"])
    }

    func testProgressForABookNoLongerOnTheShelfIsIgnored() {
        let positions = ["removed": progress(0.5)]

        let items = HomeFeed.continueReading(books: [book("a")], positions: positions)

        XCTAssertTrue(items.isEmpty)
    }

    func testFinishedBooksLeaveTheContinueCarousel() {
        let books = [book("a"), book("b")]
        let positions = [
            "a": progress(0.4, minutesAgo: 10),
            "b": progress(0.5, minutesAgo: 2)
        ]

        let items = HomeFeed.continueReading(books: books, positions: positions, finished: ["b"])

        XCTAssertEqual(items.map(\.book.id), ["a"])
    }

    func testNoSavedPositionsMeansNothingToContinue() {
        let items = HomeFeed.continueReading(books: [book("a")], positions: [:])

        XCTAssertTrue(items.isEmpty)
    }
}
