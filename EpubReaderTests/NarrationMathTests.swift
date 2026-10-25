import XCTest
@testable import EpubReader

final class NarrationMathTests: XCTestCase {

    func testTheStartAndTheEndOfTheBookAreTheFirstAndLastCharacters() {
        let place = NarrationMath.place(fraction: 0, chapterLengths: [100, 100])
        XCTAssertEqual(place?.chapter, 0)
        XCTAssertEqual(place?.offset, 0)

        let end = NarrationMath.place(fraction: 1, chapterLengths: [100, 100])
        XCTAssertEqual(end?.chapter, 1)
        XCTAssertEqual(end?.offset, 99)
    }

    func testAFractionLandsInTheChapterThatHoldsIt() {
        let place = NarrationMath.place(fraction: 0.75, chapterLengths: [100, 100])
        XCTAssertEqual(place?.chapter, 1)
        XCTAssertEqual(place?.offset, 50)
    }

    func testAChapterOfZeroLengthIsSkipped() {
        let place = NarrationMath.place(fraction: 0, chapterLengths: [0, 10])
        XCTAssertEqual(place?.chapter, 1)
        XCTAssertEqual(place?.offset, 0)
    }

    func testAnEmptyBookHasNoPlace() {
        XCTAssertNil(NarrationMath.place(fraction: 0.5, chapterLengths: []))
        XCTAssertNil(NarrationMath.place(fraction: 0.5, chapterLengths: [0, 0]))
    }
}
