import SwiftUI
import UniformTypeIdentifiers

/// One mark as the export sees it: where it sits, the chapter heading the
/// highlights tab shows, the passage, and the reader's note if there is one.
struct HighlightExportEntry: Equatable {
    let chapterIndex: Int
    let startOffset: Int
    let chapterTitle: String
    let text: String
    let note: String?
}

/// Builds the Markdown file of a book's highlights.
///
/// One pure function, so the same fixture can be compared byte for byte with
/// the Android export.
enum HighlightMarkdown {

    static func document(
        title: String,
        author: String?,
        highlights: [HighlightExportEntry]
    ) -> String {
        var lines = ["# \(title)"]
        let author = author?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !author.isEmpty { lines.append(author) }

        var chapter: Int?
        for entry in highlights.sorted(by: {
            ($0.chapterIndex, $0.startOffset) < ($1.chapterIndex, $1.startOffset)
        }) {
            if entry.chapterIndex != chapter {
                chapter = entry.chapterIndex
                lines.append("")
                lines.append("## \(heading(for: entry))")
            }
            lines.append("")
            for line in entry.text.split(separator: "\n", omittingEmptySubsequences: false) {
                lines.append("> \(line)")
            }
            let note = entry.note?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if !note.isEmpty {
                lines.append("")
                lines.append(note)
            }
        }
        return lines.joined(separator: "\n") + "\n"
    }

    private static func heading(for entry: HighlightExportEntry) -> String {
        let title = entry.chapterTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        return title.isEmpty
            ? L10n.format("reader.highlight.export.chapter", entry.chapterIndex + 1)
            : title
    }
}

/// The file the export writes: UTF-8 Markdown, nothing else.
struct HighlightExportFile: FileDocument {
    /// Resolved from the extension, so nothing has to be declared in Info.plist.
    static let markdownType = UTType(filenameExtension: "md", conformingTo: .plainText) ?? .plainText

    static var readableContentTypes: [UTType] { [markdownType] }

    var text: String

    init(text: String) {
        self.text = text
    }

    init(configuration: ReadConfiguration) throws {
        text = String(decoding: configuration.file.regularFileContents ?? Data(), as: UTF8.self)
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}
