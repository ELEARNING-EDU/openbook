import XCTest
@testable import EpubReader

final class EpubArchiveReaderTests: XCTestCase {

    private var sampleURL: URL {
        get throws {
            try XCTUnwrap(
                Bundle.main.url(forResource: "sample", withExtension: "epub"),
                "the bundled sample book is missing from the app"
            )
        }
    }

    func testSampleMetadata() throws {
        let metadata = try EpubArchiveReader.readMetadata(url: sampleURL)
        XCTAssertEqual(metadata.title, "Ngọn Hải Đăng Cuối Cùng")
        XCTAssertEqual(metadata.author, "Nguyễn Minh An")
    }

    func testSampleSpineIsSevenChaptersWithNavTitles() throws {
        let book = try EpubArchiveReader.read(url: sampleURL)

        XCTAssertEqual(book.title, "Ngọn Hải Đăng Cuối Cùng")
        XCTAssertEqual(book.chapters.map(\.title), [
            "Bìa",
            "1. Ngọn đèn trên mũi đá",
            "2. Người giữ đèn",
            "3. Con tàu không tên",
            "4. Bão tháng Chín",
            "5. Bức thư trong chai",
            "6. Ngày đèn tắt",
        ])
        XCTAssertEqual(book.chapters.map(\.href), [
            "cover.xhtml", "ch01.xhtml", "ch02.xhtml", "ch03.xhtml",
            "ch04.xhtml", "ch05.xhtml", "ch06.xhtml",
        ])
        XCTAssertTrue(book.chapters.allSatisfy { !$0.html.isEmpty })
    }

    func testResolveAgainstArchiveDirectory() {
        let resolve = EpubArchiveReader.resolve(_:relativeTo:)

        XCTAssertEqual(resolve("ch01.xhtml", "OEBPS"), "OEBPS/ch01.xhtml")
        XCTAssertEqual(resolve("../images/a.png", "OEBPS/text"), "OEBPS/images/a.png")
        XCTAssertEqual(resolve("./a.png", "OEBPS"), "OEBPS/a.png")
        XCTAssertEqual(resolve("một ảnh.png", "OEBPS"), "OEBPS/một ảnh.png")
        XCTAssertEqual(resolve("a%20b.png", "OEBPS"), "OEBPS/a b.png")
        XCTAssertEqual(resolve("/root.png", "OEBPS"), "root.png")
        XCTAssertEqual(resolve("ch01.xhtml#s2", "OEBPS"), "OEBPS/ch01.xhtml")
        // A leading `..` above the root stops at the root instead of escaping it.
        XCTAssertEqual(resolve("../../x.png", "OEBPS"), "x.png")
        XCTAssertEqual(resolve("x.png", ""), "x.png")
    }

    func testAFileThatIsNotAZipIsRejected() throws {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("not-a-book-\(UUID().uuidString).epub")
        try Data("not an archive".utf8).write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }

        XCTAssertThrowsError(try EpubArchiveReader.read(url: url))
        XCTAssertThrowsError(try EpubArchiveReader.readMetadata(url: url))
    }
}
