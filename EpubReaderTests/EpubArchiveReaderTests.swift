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

    /// The central directory is all a reader has to go on before inflating, and a
    /// crafted one can promise gigabytes for a few compressed kilobytes.
    func testAnArchiveThatPromisesMoreThanTheCapIsRefusedBeforeAllocation() throws {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("huge-book-\(UUID().uuidString).epub")
        try archiveDeclaringEntry(name: "OEBPS/ch1.xhtml", uncompressedSize: 900_000_000)
            .write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }

        XCTAssertThrowsError(try EpubArchiveReader.validate(url: url)) { error in
            XCTAssertEqual(error as? ZipError, .tooLarge)
        }
        XCTAssertThrowsError(try EpubArchiveReader.read(url: url)) { error in
            XCTAssertEqual(error as? ZipError, .tooLarge)
        }
        XCTAssertThrowsError(try EpubArchiveReader.readMetadata(url: url)) { error in
            XCTAssertEqual(error as? ZipError, .tooLarge)
        }
    }

    /// A zip with a central directory only: enough for the pre-flight check, with
    /// no payload to decompress.
    private func archiveDeclaringEntry(name: String, uncompressedSize: Int) -> Data {
        var bytes: [UInt8] = []
        func put16(_ value: Int) {
            bytes.append(UInt8(value & 0xFF))
            bytes.append(UInt8((value >> 8) & 0xFF))
        }
        func put32(_ value: Int) {
            for shift in stride(from: 0, to: 32, by: 8) {
                bytes.append(UInt8((value >> shift) & 0xFF))
            }
        }

        let nameBytes = Array(name.utf8)
        put32(0x0201_4B50)   // central directory header
        put16(20)            // version made by
        put16(20)            // version needed
        put16(0)             // flags
        put16(8)             // method: deflate
        put16(0)             // time
        put16(0)             // date
        put32(0)             // crc
        put32(0)             // compressed size
        put32(uncompressedSize)
        put16(nameBytes.count)
        put16(0)             // extra length
        put16(0)             // comment length
        put16(0)             // disk
        put16(0)             // internal attributes
        put32(0)             // external attributes
        put32(0)             // local header offset
        bytes.append(contentsOf: nameBytes)
        let directorySize = bytes.count

        put32(0x0605_4B50)    // end of central directory
        put16(0)
        put16(0)
        put16(1)
        put16(1)
        put32(directorySize)
        put32(0)
        put16(0)
        return Data(bytes)
    }
}
