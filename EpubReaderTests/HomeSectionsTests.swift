import XCTest
@testable import EpubReader

final class HomeSectionsTests: XCTestCase {

    private let now = Date(timeIntervalSince1970: 1_790_000_000)

    private func book(_ id: String) -> BookItem {
        BookItem(
            id: id,
            url: URL(fileURLWithPath: "/books/\(id).epub"),
            title: "Sách \(id)",
            author: "Tác giả \(id)"
        )
    }

    private func book(_ id: String, author: String) -> BookItem {
        BookItem(
            id: id,
            url: URL(fileURLWithPath: "/books/\(id).epub"),
            title: "Sách \(id)",
            author: author
        )
    }

    private func meta(_ daysAgo: Double, finishedDaysAgo: Double? = nil) -> LibraryMetadata {
        LibraryMetadata(
            title: nil,
            author: nil,
            importedAt: now.addingTimeInterval(-daysAgo * 86_400),
            finishedAt: finishedDaysAgo.map { now.addingTimeInterval(-$0 * 86_400) }
        )
    }

    private func progress(_ fraction: Double) -> BookProgress {
        BookProgress(
            chapter: 1,
            offset: 10,
            fraction: fraction,
            page: 2,
            pageCount: 10,
            updated: now
        )
    }

    func testExtraRowsNeedSixBooks() {
        XCTAssertFalse(HomeSections.showsExtraRows(bookCount: 5))
        XCTAssertTrue(HomeSections.showsExtraRows(bookCount: 6))
    }

    func testNewlyAddedKeepsUnreadUnfinishedBooksNewestFirst() {
        let books = [book("old"), book("reading"), book("finished"), book("new")]
        let metadata = [
            "old": meta(10),
            "reading": meta(1),
            "finished": meta(2, finishedDaysAgo: 1),
            "new": meta(3)
        ]
        let positions = ["reading": progress(0.4), "finished": progress(1)]

        let result = HomeSections.newlyAdded(
            books: books,
            positions: positions,
            metadata: metadata,
            finished: ["finished"]
        )

        XCTAssertEqual(result.map(\.id), ["new", "old"])
    }

    func testNewlyAddedStopsAtFive() {
        let books = (1...7).map { book("b\($0)") }
        let metadata = Dictionary(uniqueKeysWithValues: books.map { ($0.id, meta(Double(7 - Int($0.id.dropFirst())!))) })

        let result = HomeSections.newlyAdded(
            books: books,
            positions: [:],
            metadata: metadata,
            finished: []
        )

        XCTAssertEqual(result.count, 5)
        XCTAssertEqual(result.first?.id, "b7")
    }

    func testRecentlyFinishedUsesAYearAndNewestFirst() {
        let books = [book("fresh"), book("old"), book("ancient")]
        let metadata = [
            "fresh": meta(100, finishedDaysAgo: 2),
            "old": meta(400, finishedDaysAgo: 300),
            "ancient": meta(800, finishedDaysAgo: 400)
        ]

        let result = HomeSections.recentlyFinished(
            books: books,
            metadata: metadata,
            now: now,
            calendar: Calendar(identifier: .gregorian)
        )

        XCTAssertEqual(result.map(\.id), ["fresh", "old"])
    }

    func testSuggestionsMatchRecentAuthorsAndExcludeLists() {
        let books = [
            book("recent1", author: "An"),
            book("recent2", author: "Bình"),
            book("candidate1", author: "An"),
            book("candidate2", author: "Bình"),
            book("alreadyNew", author: "An"),
            book("finishedBook", author: "An"),
            book("unrelated", author: "Chi")
        ]
        let metadata = [
            "recent1": meta(1, finishedDaysAgo: 1),
            "recent2": meta(2, finishedDaysAgo: 2),
            "candidate1": meta(20),
            "candidate2": meta(21),
            "alreadyNew": meta(1),
            "finishedBook": meta(50, finishedDaysAgo: 3),
            "unrelated": meta(5)
        ]
        let positions = ["recent2": progress(0.5), "finishedBook": progress(1)]

        let result = HomeSections.suggestions(
            books: books,
            positions: positions,
            metadata: metadata,
            finished: ["recent1", "finishedBook"],
            excluding: ["alreadyNew"],
            now: now,
            calendar: Calendar(identifier: .gregorian)
        )

        XCTAssertEqual(result.map(\.id), ["candidate1", "candidate2"])
    }

    func testSuggestionsNeedTwoCandidates() {
        let books = [book("recent", author: "An"), book("only", author: "An")]
        let metadata = ["recent": meta(1, finishedDaysAgo: 1), "only": meta(5)]

        let result = HomeSections.suggestions(
            books: books,
            positions: [:],
            metadata: metadata,
            finished: ["recent"],
            excluding: [],
            now: now,
            calendar: Calendar(identifier: .gregorian)
        )

        XCTAssertTrue(result.isEmpty)
    }
}
