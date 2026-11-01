import XCTest
@testable import EpubReader

final class TextSelectionTests: XCTestCase {

    private let sample = "Ngọn hải đăng cuối cùng" as NSString

    func testTheWordUnderAnOffsetIsJustThatWord() {
        // "hải" sits at UTF-16 5…8.
        XCTAssertEqual(TextSelection.wordRange(in: sample, around: 6), NSRange(location: 5, length: 3))
        XCTAssertEqual(TextSelection.wordRange(in: sample, around: 0), NSRange(location: 0, length: 4))
        XCTAssertEqual(TextSelection.wordRange(in: sample, around: 19), NSRange(location: 19, length: 4))
    }

    func testAWordStopsAtWhitespace() {
        let text = "one  two" as NSString
        XCTAssertEqual(TextSelection.wordRange(in: text, around: 2), NSRange(location: 0, length: 3))
        XCTAssertEqual(TextSelection.wordRange(in: text, around: 3), NSRange(location: 0, length: 3))
        XCTAssertEqual(TextSelection.wordRange(in: text, around: 5), NSRange(location: 5, length: 3))
    }

    func testAnEmptyTextHasNoWord() {
        XCTAssertEqual(TextSelection.wordRange(in: "" as NSString, around: 4), NSRange(location: 0, length: 0))
    }

    func testAStartHandleSnapsToAWordStartWithoutSwallowingThePassage() {
        let text = "một đoạn văn hay" as NSString
        // Drag the start into the middle of "văn": it snaps to its beginning.
        let start = TextSelection.adjustedStart(in: text, proposed: 9, end: 13)
        XCTAssertEqual(start, 9)
        // Pushed past the end, it still leaves a whole word.
        let clamped = TextSelection.adjustedStart(in: text, proposed: 99, end: 13)
        XCTAssertLessThan(clamped, 13)
    }

    func testAnEndHandleSnapsToAWordEndAndKeepsOneCharacter() {
        let text = "một đoạn văn hay" as NSString
        XCTAssertEqual(TextSelection.adjustedEnd(in: text, proposed: 6, start: 0), 8)
        XCTAssertEqual(TextSelection.adjustedEnd(in: text, proposed: 0, start: 3), 8)
    }

    func testDraggingFromTheFirstWordToTheLastCoversTheWholePassage() {
        let range = TextSelection.range(from: 2, to: 14, in: sample)
        XCTAssertEqual(range, NSRange(location: 0, length: 18))
    }

    func testDraggingBackwardsStillGivesAForwardRange() {
        let range = TextSelection.range(from: 14, to: 2, in: sample)
        XCTAssertEqual(range, NSRange(location: 0, length: 18))
    }

    func testDeletingAWordInAVietnameseSentenceKeepsOffsetsInUTF16() {
        let text = "😀 Ngọn hải đăng" as NSString
        let range = TextSelection.wordRange(in: text, around: 6)
        XCTAssertEqual(range, NSRange(location: 3, length: 4))
    }
}
