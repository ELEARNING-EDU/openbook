import XCTest
@testable import EpubReader

final class ZipArchiveTests: XCTestCase {

    func testADuplicateNameResolvesToItsFirstEntry() throws {
        let archive = ZipArchive(data: storedArchive([
            ("a.txt", "first"),
            ("a.txt", "second"),
            ("b.txt", "third"),
        ]))

        let first = try XCTUnwrap(archive.data(forEntryNamed: "a.txt"))
        XCTAssertEqual(String(decoding: first, as: UTF8.self), "first")
        XCTAssertEqual(archive.allEntryNames(), ["a.txt", "a.txt", "b.txt"])
        XCTAssertNil(archive.data(forEntryNamed: "missing.txt"))
        XCTAssertNoThrow(try archive.checkResourceLimits())
    }

    /// A zip of stored entries, written by hand: enough for the reader to find
    /// the central directory and hand back the bytes.
    private func storedArchive(_ entries: [(name: String, text: String)]) -> Data {
        var bytes: [UInt8] = []
        var directory: [UInt8] = []

        func put16(_ value: Int, into buffer: inout [UInt8]) {
            buffer.append(UInt8(value & 0xFF))
            buffer.append(UInt8((value >> 8) & 0xFF))
        }
        func put32(_ value: Int, into buffer: inout [UInt8]) {
            for shift in stride(from: 0, to: 32, by: 8) {
                buffer.append(UInt8((value >> shift) & 0xFF))
            }
        }

        for entry in entries {
            let name = Array(entry.name.utf8)
            let payload = Array(entry.text.utf8)
            let offset = bytes.count

            put32(0x0403_4B50, into: &bytes)
            put16(20, into: &bytes)
            put16(0, into: &bytes)
            put16(0, into: &bytes)          // stored
            put16(0, into: &bytes)
            put16(0, into: &bytes)
            put32(0, into: &bytes)          // crc, never checked by the reader
            put32(payload.count, into: &bytes)
            put32(payload.count, into: &bytes)
            put16(name.count, into: &bytes)
            put16(0, into: &bytes)
            bytes.append(contentsOf: name)
            bytes.append(contentsOf: payload)

            put32(0x0201_4B50, into: &directory)
            put16(20, into: &directory)
            put16(20, into: &directory)
            put16(0, into: &directory)
            put16(0, into: &directory)      // stored
            put16(0, into: &directory)
            put16(0, into: &directory)
            put32(0, into: &directory)
            put32(payload.count, into: &directory)
            put32(payload.count, into: &directory)
            put16(name.count, into: &directory)
            put16(0, into: &directory)
            put16(0, into: &directory)
            put16(0, into: &directory)
            put16(0, into: &directory)
            put32(0, into: &directory)
            put32(offset, into: &directory)
            directory.append(contentsOf: name)
        }

        let directoryOffset = bytes.count
        bytes.append(contentsOf: directory)

        put32(0x0605_4B50, into: &bytes)
        put16(0, into: &bytes)
        put16(0, into: &bytes)
        put16(entries.count, into: &bytes)
        put16(entries.count, into: &bytes)
        put32(directory.count, into: &bytes)
        put32(directoryOffset, into: &bytes)
        put16(0, into: &bytes)
        return Data(bytes)
    }
}
