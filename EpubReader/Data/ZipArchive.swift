import Compression
import Foundation

/// Minimal ZIP reader — just enough for EPUB containers.
///
/// Only the two methods EPUB uses in practice are supported: stored (0) and
/// deflate (8). ZIP64 is not handled; a text-only book is nowhere near 4 GB.
/// Written by hand so the project stays free of third-party dependencies.
struct ZipArchive {

    private let bytes: [UInt8]

    init(url: URL) throws {
        let data = try Data(contentsOf: url)
        self.bytes = [UInt8](data)
    }

    init(data: Data) {
        self.bytes = [UInt8](data)
    }

    /// Exact-path lookup. EPUB hrefs are usually relative to the OPF folder,
    /// so callers resolve the full path before asking.
    func data(forEntryNamed name: String) -> Data? {
        guard let entry = entries().first(where: { $0.name == name }) else { return nil }
        return try? extract(entry)
    }

    func allEntryNames() -> [String] {
        entries().map(\.name)
    }

    // MARK: - Central directory

    private struct Entry {
        let name: String
        let method: UInt16
        let compressedSize: Int
        let uncompressedSize: Int
        let localHeaderOffset: Int
    }

    private func u16(_ offset: Int) -> Int {
        Int(bytes[offset]) | (Int(bytes[offset + 1]) << 8)
    }

    private func u32(_ offset: Int) -> Int {
        Int(bytes[offset])
            | (Int(bytes[offset + 1]) << 8)
            | (Int(bytes[offset + 2]) << 16)
            | (Int(bytes[offset + 3]) << 24)
    }

    private func entries() -> [Entry] {
        guard bytes.count > 22 else { return [] }
        guard let eocd = findEndOfCentralDirectory() else { return [] }

        let count = u16(eocd + 10)
        var cursor = u32(eocd + 16)
        var result: [Entry] = []

        for _ in 0..<count {
            guard cursor + 46 <= bytes.count else { break }
            guard u32(cursor) == 0x0201_4B50 else { break }

            let method = UInt16(u16(cursor + 10))
            let compressedSize = u32(cursor + 20)
            let uncompressedSize = u32(cursor + 24)
            let nameLength = u16(cursor + 28)
            let extraLength = u16(cursor + 30)
            let commentLength = u16(cursor + 32)
            let localOffset = u32(cursor + 42)

            let nameStart = cursor + 46
            guard nameStart + nameLength <= bytes.count else { break }
            let name = String(decoding: bytes[nameStart..<(nameStart + nameLength)], as: UTF8.self)

            result.append(
                Entry(
                    name: name,
                    method: method,
                    compressedSize: compressedSize,
                    uncompressedSize: uncompressedSize,
                    localHeaderOffset: localOffset
                )
            )
            cursor = nameStart + nameLength + extraLength + commentLength
        }
        return result
    }

    /// The EOCD record is last, followed by an optional comment of up to 64 KB.
    private func findEndOfCentralDirectory() -> Int? {
        let signature: [UInt8] = [0x50, 0x4B, 0x05, 0x06]
        let lowest = max(0, bytes.count - 65_557)
        var index = bytes.count - 22
        while index >= lowest {
            if bytes[index] == signature[0],
               bytes[index + 1] == signature[1],
               bytes[index + 2] == signature[2],
               bytes[index + 3] == signature[3] {
                return index
            }
            index -= 1
        }
        return nil
    }

    // MARK: - Extraction

    private func extract(_ entry: Entry) throws -> Data {
        let local = entry.localHeaderOffset
        guard local + 30 <= bytes.count else { throw ZipError.corrupt }
        guard u32(local) == 0x0403_4B50 else { throw ZipError.corrupt }

        // The local header carries its own extra-field length, which can differ
        // from the central directory's. Always trust the local one.
        let nameLength = u16(local + 26)
        let extraLength = u16(local + 28)
        let start = local + 30 + nameLength + extraLength
        let end = start + entry.compressedSize
        guard end <= bytes.count, start <= end else { throw ZipError.corrupt }

        let payload = Data(bytes[start..<end])

        switch entry.method {
        case 0:
            return payload
        case 8:
            return try inflate(payload, expectedSize: entry.uncompressedSize)
        default:
            throw ZipError.unsupportedMethod(entry.method)
        }
    }

    private func inflate(_ input: Data, expectedSize: Int) throws -> Data {
        guard expectedSize > 0 else { return Data() }
        guard !input.isEmpty else { throw ZipError.inflateFailed }

        var output = Data(count: expectedSize)
        let written: Int = output.withUnsafeMutableBytes { destination -> Int in
            input.withUnsafeBytes { source -> Int in
                guard let dst = destination.bindMemory(to: UInt8.self).baseAddress,
                      let src = source.bindMemory(to: UInt8.self).baseAddress else { return 0 }
                // COMPRESSION_ZLIB here is a raw DEFLATE stream, which is exactly
                // what a ZIP entry stores.
                return compression_decode_buffer(
                    dst, expectedSize, src, input.count, nil, COMPRESSION_ZLIB
                )
            }
        }

        guard written > 0 else { throw ZipError.inflateFailed }
        output.removeSubrange(written..<output.count)
        return output
    }
}

enum ZipError: LocalizedError {
    case corrupt
    case unsupportedMethod(UInt16)
    case inflateFailed

    var errorDescription: String? {
        switch self {
        case .corrupt: return "Tệp ZIP hỏng."
        case .unsupportedMethod(let m): return "Kiểu nén ZIP không hỗ trợ: \(m)."
        case .inflateFailed: return "Giải nén thất bại."
        }
    }
}
