import Compression
import Foundation

/// Minimal ZIP reader — just enough for EPUB containers.
///
/// Only the two methods EPUB uses in practice are supported: stored (0) and
/// deflate (8). ZIP64 is not handled; a text-only book is nowhere near 4 GB.
/// Written by hand so the project stays free of third-party dependencies.
struct ZipArchive {

    /// One archive can be present in a folder, so the central directory is read
    /// once at init rather than on every lookup.
    private let bytes: [UInt8]
    private let allEntries: [Entry]
    private let entriesByName: [String: Entry]
    private let names: [String]

    /// A book is text and a few pictures. These are ceilings on what a crafted
    /// header may make the reader allocate before it has seen a byte of text.
    static let maxEntryBytes = 64 * 1024 * 1024
    static let maxTotalBytes = 256 * 1024 * 1024
    static let maxEntryCount = 10_000

    init(url: URL) throws {
        let data = try Data(contentsOf: url)
        self.init(bytes: [UInt8](data))
    }

    init(data: Data) {
        self.init(bytes: [UInt8](data))
    }

    private init(bytes: [UInt8]) {
        self.bytes = bytes
        let parsed = Self.readEntries(bytes)
        allEntries = parsed
        names = parsed.map(\.name)
        var map: [String: Entry] = [:]
        for entry in parsed where map[entry.name] == nil { map[entry.name] = entry }
        entriesByName = map
    }

    /// Refuses an archive whose central directory promises more memory than the
    /// reader is willing to give it, before any entry is inflated.
    func checkResourceLimits() throws {
        guard allEntries.count <= Self.maxEntryCount else { throw ZipError.tooLarge }
        var total = 0
        for entry in allEntries {
            guard entry.uncompressedSize <= Self.maxEntryBytes else { throw ZipError.tooLarge }
            total += entry.uncompressedSize
        }
        guard total <= Self.maxTotalBytes else { throw ZipError.tooLarge }
    }

    /// Exact-path lookup. EPUB hrefs are usually relative to the OPF folder,
    /// so callers resolve the full path before asking.
    func data(forEntryNamed name: String) -> Data? {
        guard let entry = entriesByName[name] else { return nil }
        return try? extract(entry)
    }

    func allEntryNames() -> [String] {
        names
    }

    // MARK: - Central directory

    private struct Entry {
        let name: String
        let method: UInt16
        let compressedSize: Int
        let uncompressedSize: Int
        let localHeaderOffset: Int
    }

    private static func u16(_ bytes: [UInt8], _ offset: Int) -> Int {
        Int(bytes[offset]) | (Int(bytes[offset + 1]) << 8)
    }

    private static func u32(_ bytes: [UInt8], _ offset: Int) -> Int {
        Int(bytes[offset])
            | (Int(bytes[offset + 1]) << 8)
            | (Int(bytes[offset + 2]) << 16)
            | (Int(bytes[offset + 3]) << 24)
    }

    private static func readEntries(_ bytes: [UInt8]) -> [Entry] {
        guard bytes.count > 22 else { return [] }
        guard let eocd = findEndOfCentralDirectory(bytes) else { return [] }

        let count = u16(bytes, eocd + 10)
        var cursor = u32(bytes, eocd + 16)
        var result: [Entry] = []

        for _ in 0..<count {
            guard cursor + 46 <= bytes.count else { break }
            guard u32(bytes, cursor) == 0x0201_4B50 else { break }

            let method = UInt16(u16(bytes, cursor + 10))
            let compressedSize = u32(bytes, cursor + 20)
            let uncompressedSize = u32(bytes, cursor + 24)
            let nameLength = u16(bytes, cursor + 28)
            let extraLength = u16(bytes, cursor + 30)
            let commentLength = u16(bytes, cursor + 32)
            let localOffset = u32(bytes, cursor + 42)

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
    private static func findEndOfCentralDirectory(_ bytes: [UInt8]) -> Int? {
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
        guard Self.u32(bytes, local) == 0x0403_4B50 else { throw ZipError.corrupt }

        // The local header carries its own extra-field length, which can differ
        // from the central directory's. Always trust the local one.
        let nameLength = Self.u16(bytes, local + 26)
        let extraLength = Self.u16(bytes, local + 28)
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
        guard expectedSize <= Self.maxEntryBytes else { throw ZipError.tooLarge }
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

enum ZipError: LocalizedError, Equatable {
    case corrupt
    case unsupportedMethod(UInt16)
    case inflateFailed
    /// The central directory promises more than the reader will allocate.
    case tooLarge

    var errorDescription: String? {
        switch self {
        case .corrupt: return L10n.string("error.zip.corrupt")
        case .unsupportedMethod(let m): return L10n.format("error.zip.unsupported", Int(m))
        case .inflateFailed: return L10n.string("error.zip.inflate")
        case .tooLarge: return L10n.string("error.zip.tooLarge")
        }
    }
}
