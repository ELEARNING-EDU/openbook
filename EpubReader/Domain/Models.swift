import Foundation
import UIKit

/// A book sitting in the library (one .epub file on disk).
struct BookItem: Identifiable, Hashable {
    let id: String
    let url: URL
    let title: String
    let author: String
}

/// A chapter after HTML conversion.
struct Chapter {
    let title: String
    let href: String
    let text: NSAttributedString
    /// Sentence ranges inside `text.string`, used by narration and highlighting.
    let sentences: [NSRange]
}

/// Chapter as read straight out of the ZIP, before HTML conversion.
/// Kept separate because `NSAttributedString` HTML import must run on the main thread.
struct RawChapter {
    let title: String
    let href: String
    let html: Data
    /// The pictures this chapter points at, keyed by the raw `src` its markup
    /// wrote. Read off the zip while the file is open, so the reader never has to
    /// go back to the archive while it lays a page out.
    var images: [String: Data] = [:]
}

struct RawBook {
    let title: String
    let author: String
    let chapters: [RawChapter]
}

/// One screenful of text, ready to be handed to a page view controller.
struct PageContent {
    let text: NSAttributedString
    let chapterTitle: String
    /// Index of the chapter this page came from.
    let chapterIndex: Int
    /// Where this page sits inside the chapter string, so a character offset can be
    /// translated between page coordinates and chapter coordinates. With two
    /// columns this is the span that holds both of them, so every containment test
    /// still works.
    let range: NSRange
    /// The columns this page is made of, in chapter coordinates: one range usually,
    /// two when the reader asked for two columns.
    let columns: [NSRange]
    let pageNumber: Int
    let pageCount: Int
}

struct TocEntry: Identifiable {
    let id: Int
    let title: String
    let pageIndex: Int
}

enum EpubError: LocalizedError {
    case notReadable
    case missingContainer
    case missingPackage
    case emptySpine

    var errorDescription: String? {
        switch self {
        case .notReadable: return L10n.string("error.epub.notReadable")
        case .missingContainer: return L10n.string("error.epub.missingContainer")
        case .missingPackage: return L10n.string("error.epub.missingPackage")
        case .emptySpine: return L10n.string("error.epub.emptySpine")
        }
    }
}
