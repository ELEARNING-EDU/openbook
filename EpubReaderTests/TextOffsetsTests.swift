import XCTest
@testable import EpubReader

final class TextOffsetsTests: XCTestCase {

    func testAnEmojiBeforeTheMatchCountsTwoUTF16Units() throws {
        let text = String(repeating: "😀", count: 100) + "mục tiêu"
        let range = try XCTUnwrap(text.range(of: "mục"))

        XCTAssertEqual(text.distance(from: text.startIndex, to: range.lowerBound), 100)
        XCTAssertEqual(TextOffsets.location(of: range.lowerBound, in: text), 200)
    }

    func testAVietnameseCombiningFormCountsCodeUnitsNotGraphemes() {
        let text = "e\u{0302}\u{0301}x"
        let afterOneGrapheme = text.index(text.startIndex, offsetBy: 1)

        XCTAssertEqual(text.distance(from: text.startIndex, to: afterOneGrapheme), 1)
        XCTAssertEqual(TextOffsets.location(of: afterOneGrapheme, in: text), 3)
    }

    func testAnOffsetBecomesAnIndexAndStopsAtTheEnd() {
        let text = "😀ab"

        XCTAssertEqual(TextOffsets.index(at: 2, in: text).utf16Offset(in: text), 2)
        XCTAssertEqual(TextOffsets.index(at: 999, in: text).utf16Offset(in: text), text.utf16.count)
        XCTAssertEqual(TextOffsets.index(at: -5, in: text).utf16Offset(in: text), 0)
    }
}
