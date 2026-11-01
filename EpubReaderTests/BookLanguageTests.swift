import XCTest
@testable import EpubReader

final class BookLanguageTests: XCTestCase {

    func testVietnameseContentBeatsAWrongEnglishTag() {
        let sample = "Ngọn hải đăng vẫn đứng đó, quét ánh sáng qua mặt biển đêm."
        XCTAssertEqual(BookLanguage.resolve(tag: "en", sample: sample), "vi")
    }

    func testKanaMakesABookJapaneseWhateverTheTag() {
        let sample = "こんにちは世界。これは日本語の文章です。"
        XCTAssertEqual(BookLanguage.resolve(tag: "en", sample: sample), "ja")
    }

    func testHanWithoutKanaIsChinese() {
        let sample = "这是一个中文句子，包含很多汉字。"
        XCTAssertEqual(BookLanguage.resolve(tag: "en", sample: sample), "zh")
    }

    func testAmbiguousTextKeepsTheTag() {
        let sample = "The lighthouse still stood there, sweeping its light."
        XCTAssertEqual(BookLanguage.resolve(tag: "vi", sample: sample), "vi")
    }

    func testWithoutATagTheTextIsAsked() {
        let sample = "The lighthouse still stood there, sweeping its light across the sea."
        XCTAssertEqual(BookLanguage.resolve(tag: nil, sample: sample), "en")
    }

    func testALanguageTagIsReducedToItsCode() {
        XCTAssertEqual(BookLanguage.code("vi-VN"), "vi")
        XCTAssertEqual(BookLanguage.code("zh-Hant"), "zh")
        XCTAssertEqual(BookLanguage.resolve(tag: "en-GB", sample: "hello"), "en")
    }

    func testNothingKnownFallsBackToTheTag() {
        XCTAssertEqual(BookLanguage.resolve(tag: "fr", sample: ""), "fr")
        XCTAssertNil(BookLanguage.resolve(tag: nil, sample: ""))
    }

    func testVietnameseInNFDIsStillRecognised() {
        let sample = "Nguye\u{0302}\u{0303}n ha\u{0309}i đa\u{0306}ng".precomposedStringWithCanonicalMapping
        XCTAssertEqual(BookLanguage.resolve(tag: "en", sample: sample), "vi")
    }
}
