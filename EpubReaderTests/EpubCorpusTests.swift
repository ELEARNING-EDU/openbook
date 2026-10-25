import XCTest
import UIKit
@testable import EpubReader

/// Drives the real loading pipeline over the generated corpus in
/// `build/qa/epub-corpus`. Generate it first:
///
///     python3 build/qa/epub-corpus/make_corpus.py
///
/// The corpus is deliberately outside the test bundle: it is QA material, not a
/// fixture the app ships. Without it these tests skip instead of failing.
@MainActor
final class EpubCorpusTests: XCTestCase {

    private struct Corpus: Decodable {
        let books: [Case]
        let performance: [String]
    }

    private struct Case: Decodable {
        let file: String
        let expect: String
        let chapters: Int?
        let notes: String
        let title: String?
        let author: String?
        let imagesPerChapter: Int?
    }

    private var corpusRoot: URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()   // EpubReaderTests
            .deletingLastPathComponent()   // repo root
            .appendingPathComponent("build/qa/epub-corpus")
    }

    private func corpus() throws -> Corpus {
        let manifest = corpusRoot.appendingPathComponent("manifest.json")
        try XCTSkipUnless(
            FileManager.default.fileExists(atPath: manifest.path),
            "corpus missing — run python3 build/qa/epub-corpus/make_corpus.py"
        )
        return try JSONDecoder().decode(Corpus.self, from: Data(contentsOf: manifest))
    }

    private func url(_ file: String) -> URL {
        corpusRoot.appendingPathComponent("books").appendingPathComponent(file)
    }

    // MARK: - Compatibility

    func testEveryCorpusBookOpensOrIsRefusedWithoutCrashing() throws {
        let books = try corpus().books
        var failures: [String] = []

        for item in books {
            let started = CFAbsoluteTimeGetCurrent()
            switch item.expect {
            case "opens":
                do {
                    let book = try EpubArchiveReader.read(url: url(item.file))
                    let chars = book.chapters.reduce(0) { $0 + $1.html.count }
                    if let expected = item.chapters, book.chapters.count != expected {
                        failures.append(
                            "\(item.file): \(book.chapters.count) chapters, expected \(expected)"
                        )
                    }
                    if book.title.isEmpty {
                        failures.append("\(item.file): empty title")
                    }
                    if let expected = item.title, book.title != expected {
                        failures.append("\(item.file): title \"\(book.title)\", expected \"\(expected)\"")
                    }
                    if let expected = item.author, book.author != expected {
                        failures.append("\(item.file): author \"\(book.author)\", expected \"\(expected)\"")
                    }
                    if let perChapter = item.imagesPerChapter {
                        for (index, chapter) in book.chapters.enumerated() where chapter.images.count != perChapter {
                            failures.append(
                                "\(item.file) chapter \(index + 1): \(chapter.images.count) pictures, expected \(perChapter)"
                            )
                        }
                    }
                    if book.chapters.contains(where: { $0.html.isEmpty }) {
                        failures.append("\(item.file): a chapter has no bytes")
                    }
                    print("CORPUS|\(item.file)|opens|chapters=\(book.chapters.count)|htmlBytes=\(chars)|title=\(book.title)|author=\(book.author)|ms=\(Int((CFAbsoluteTimeGetCurrent() - started) * 1000))|\(item.notes)")
                } catch {
                    failures.append("\(item.file): refused — \(error.localizedDescription)")
                    print("CORPUS|\(item.file)|THREW|\(error.localizedDescription)|\(item.notes)")
                }
            default:
                do {
                    _ = try EpubArchiveReader.read(url: url(item.file))
                    failures.append("\(item.file): opened, expected a refusal")
                    print("CORPUS|\(item.file)|OPENED-UNEXPECTED|\(item.notes)")
                } catch {
                    print("CORPUS|\(item.file)|throws|\(type(of: error))|\(item.notes)")
                }
            }
        }

        XCTAssertTrue(failures.isEmpty, failures.joined(separator: "\n"))
    }

    func testEveryReadableChapterTypesetsAndPaginates() throws {
        let books = try corpus().books
        let column = CGSize(width: 345, height: 520)
        let palette = ReaderPalette.shell(dark: false)
        var failures: [String] = []

        for item in books where item.expect == "opens" {
            guard let book = try? EpubArchiveReader.read(url: url(item.file)) else { continue }
            let started = CFAbsoluteTimeGetCurrent()
            var pages = 0
            for (index, chapter) in book.chapters.enumerated() {
                let attributed = AttributedBuilder.build(
                    html: chapter.html,
                    palette: palette,
                    images: chapter.images,
                    columnWidth: column.width,
                    maxImageHeight: column.height * 0.55
                )
                let ranges = Paginator.paginate(attributed, in: column)
                pages += ranges.count
                if attributed.length == 0 {
                    failures.append("\(item.file) chapter \(index + 1): typeset to nothing")
                }
                if ranges.contains(where: { $0.length == 0 }) {
                    failures.append("\(item.file) chapter \(index + 1): empty page range")
                }
            }
            print("TYPE|\(item.file)|chapters=\(book.chapters.count)|pages=\(pages)|ms=\(Int((CFAbsoluteTimeGetCurrent() - started) * 1000))")
        }

        XCTAssertTrue(failures.isEmpty, failures.joined(separator: "\n"))
    }

    // MARK: - Performance

    func testLargeBooksLoadAndPaginate() async throws {
        let corpus = try corpus()
        let profile = CGSize(width: 402, height: 780)
        let style = ReaderCustom()
        let palette = ReaderPalette.shell(dark: false)

        for file in corpus.performance {
            let model = ReaderViewModel()
            let started = CFAbsoluteTimeGetCurrent()
            let before = Self.footprintMB()

            model.load(url: url(file), style: style, palette: palette)
            let deadline = Date().addingTimeInterval(180)
            while model.isLoading, Date() < deadline {
                try await Task.sleep(nanoseconds: 20_000_000)
            }
            let loaded = CFAbsoluteTimeGetCurrent()

            XCTAssertFalse(model.isLoading, "\(file): still loading after 180 s")
            XCTAssertNil(model.errorMessage, "\(file): \(model.errorMessage ?? "")")

            model.updatePagination(for: profile, safeTop: 62, style: style)
            let paginated = CFAbsoluteTimeGetCurrent()
            let after = Self.footprintMB()

            let readMS = Int((loaded - started) * 1000)
            let paginateMS = Int((paginated - loaded) * 1000)
            let totalMS = Int((paginated - started) * 1000)
            print("PERF|\(file)|chapters=\(model.allChapters().count)|pages=\(model.pages.count)|readMs=\(readMS)|paginateMs=\(paginateMS)|totalMs=\(totalMS)|footprintMB=\(Int(before))→\(Int(after))|toc=\(model.toc.count)")

            XCTAssertGreaterThan(model.pages.count, 500, "\(file): too few pages")
            XCTAssertEqual(model.toc.count, model.allChapters().count, "\(file): TOC lost chapters")
            // The reader freezes while it paginates the whole book; 30 s on a
            // simulator would already be a broken open on a phone.
            XCTAssertLessThan(totalMS, 30_000, "\(file): open took \(totalMS) ms")
        }
    }

    // MARK: - Library import

    func testImportAcceptsEveryFileAndLeavesReadingToTheReader() throws {
        _ = try corpus()
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("corpus-import-\(UUID().uuidString)")
        defer { try? FileManager.default.removeItem(at: directory) }
        let defaults = UserDefaults(suiteName: "corpus-import")!
        let store = LibraryStore(directory: directory, defaults: defaults, seedSample: false)

        for file in ["01-epub2-ncx.epub", "10-drm-aes.epub", "24-renamed-pdf.epub"] {
            store.importBook(from: url(file))
            XCTAssertNil(store.importError, "\(file): import error \(store.importError ?? "")")
        }

        XCTAssertEqual(store.books.count, 3, "the shelf should hold every copied file")
        print("IMPORT|books=\(store.books.count)|errors=\(store.importError ?? "none")")
    }

    /// Resident memory, so the 1000-page runs can be compared with the small ones.
    private static func footprintMB() -> Int {
        var info = task_vm_info_data_t()
        var count = mach_msg_type_number_t(
            MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<integer_t>.size
        )
        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), $0, &count)
            }
        }
        guard result == KERN_SUCCESS else { return 0 }
        return Int(info.phys_footprint / 1_048_576)
    }
}
