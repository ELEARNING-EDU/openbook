import Foundation
import XCTest
@testable import EpubReader

final class MiniPlayerVisibilityTests: XCTestCase {

    private let book = BookItem(
        id: "a.epub",
        url: URL(fileURLWithPath: "/tmp/a.epub"),
        title: "A",
        author: "B"
    )

    private func visible(
        state: NarrationState = .playing,
        book: BookItem? = nil,
        readerOpen: Bool = false,
        bookInLibrary: Bool = true
    ) -> Bool {
        MiniPlayerVisibility.isVisible(
            state: state,
            book: book ?? self.book,
            readerOpen: readerOpen,
            bookInLibrary: bookInLibrary
        )
    }

    func testAnIdleVoiceHidesTheButton() {
        XCTAssertFalse(visible(state: .idle))
    }

    func testNoBookHidesTheButton() {
        XCTAssertFalse(
            MiniPlayerVisibility.isVisible(
                state: .playing,
                book: nil,
                readerOpen: false,
                bookInLibrary: true
            )
        )
    }

    func testPlayingAndPausedBothShowTheButton() {
        XCTAssertTrue(visible(state: .playing))
        XCTAssertTrue(visible(state: .paused))
    }

    func testTheReaderHidesTheButton() {
        XCTAssertFalse(visible(readerOpen: true))
        XCTAssertFalse(visible(state: .paused, readerOpen: true))
    }

    func testADeletedBookHidesTheButton() {
        XCTAssertFalse(visible(bookInLibrary: false))
    }
}
