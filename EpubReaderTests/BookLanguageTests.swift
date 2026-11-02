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

    /// French accents are not Vietnamese tonemarks. A French book must not be
    /// read by the Vietnamese voice just because it has `é`, `ê`, `à` or `ô`.
    func testFrenchContentKeepsItsTag() {
        let sample = "La lettre arriva un mardi, dans une enveloppe adoucie par le voyage. "
            + "Camille la lut deux fois sur le seuil, puis une troisième fois dans la cuisine, "
            + "où la bouilloire chantait."
        XCTAssertEqual(BookLanguage.resolve(tag: "fr", sample: sample), "fr")
        XCTAssertNil(BookLanguage.strongLanguage(in: sample))
    }

    func testSpanishContentKeepsItsTag() {
        let sample = "El puerto despertaba despacio. Las barcas de pesca se acomodaban contra "
            + "el muelle de madera, las gaviotas discutían sobre los tejados y el faro seguía "
            + "girando."
        XCTAssertEqual(BookLanguage.resolve(tag: "es", sample: sample), "es")
        XCTAssertNil(BookLanguage.strongLanguage(in: sample))
    }

    func testPortugueseContentKeepsItsTag() {
        let sample = "O porto não acordava devagar. As barcas de pesca encostavam ao cais de "
            + "madeira, as gaivotas discutiam sobre os telhados e o farol continuava a girar."
        XCTAssertEqual(BookLanguage.resolve(tag: "pt", sample: sample), "pt")
        XCTAssertNil(BookLanguage.strongLanguage(in: sample))
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
