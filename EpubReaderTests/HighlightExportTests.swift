import XCTest
@testable import EpubReader

final class HighlightExportTests: XCTestCase {

    /// The shared fixture: Android writes the same file from the same input.
    func testTheSharedFixtureProducesTheSameBytes() {
        let document = HighlightMarkdown.document(
            title: "Ngọn Hải Đăng",
            author: "Minh An",
            highlights: [
                HighlightExportEntry(
                    chapterIndex: 1,
                    startOffset: 50,
                    chapterTitle: "Mở đầu",
                    text: "Biển đêm",
                    note: "Hay"
                ),
                HighlightExportEntry(
                    chapterIndex: 0,
                    startOffset: 10,
                    chapterTitle: "Lời tựa",
                    text: "Ngọn đèn",
                    note: nil
                ),
            ]
        )

        XCTAssertEqual(document, """
        # Ngọn Hải Đăng
        Minh An

        ## Lời tựa

        > Ngọn đèn

        ## Mở đầu

        > Biển đêm

        Hay

        """)
        XCTAssertTrue(document.hasSuffix("\n"))
        XCTAssertFalse(document.hasSuffix("\n\n"))
    }

    func testAnUnknownAuthorIsOmittedAndEveryTextLineIsQuoted() {
        let document = HighlightMarkdown.document(
            title: "Sách",
            author: "  ",
            highlights: [
                HighlightExportEntry(
                    chapterIndex: 4,
                    startOffset: 0,
                    chapterTitle: "",
                    text: "dòng một\ndòng hai",
                    note: nil
                ),
            ]
        )

        let heading = L10n.format("reader.highlight.export.chapter", 5)
        XCTAssertEqual(document, """
        # Sách

        ## \(heading)

        > dòng một
        > dòng hai

        """)
    }
}
