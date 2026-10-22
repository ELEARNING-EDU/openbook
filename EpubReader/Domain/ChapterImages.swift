import Foundation

/// One picture a chapter's XHTML asks for, kept exactly as the markup wrote it.
struct ChapterImageReference {
    /// The `src` attribute: still relative to the chapter, still percent-encoded.
    let src: String
    /// The `alt` text, shown when the picture itself cannot be.
    let alt: String
}

/// Finds `<img>` tags in a chapter and swaps them for a token the HTML importer
/// will carry through as plain text.
///
/// The importer drops `<img>` without a word: it is handed bytes rather than a
/// file system, so it has no base URL to fetch `images/pic.png` from, and the tag
/// leaves neither a picture nor a placeholder behind. Lifting the tags out first
/// gives the reader a position to put the picture back into once the string has
/// been imported and styled (see `AttributedBuilder`).
enum ChapterImages {

    /// The marker left in the HTML. Plain ASCII on purpose — private-use
    /// characters are not safe through the importer — and padded so one token can
    /// never be a prefix of another.
    static func token(for index: Int) -> String { "@@ngimg\(index)@@" }

    private static let tagPattern = "<img\\b[^>]*>"
    private static let srcPattern = "\\bsrc\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')"
    private static let altPattern = "\\balt\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')"

    private static let tag = try? NSRegularExpression(pattern: tagPattern, options: [.caseInsensitive])
    private static let src = try? NSRegularExpression(pattern: srcPattern, options: [.caseInsensitive])
    private static let alt = try? NSRegularExpression(pattern: altPattern, options: [.caseInsensitive])

    /// Every `<img>` in document order, without touching the markup.
    static func references(in html: String) -> [ChapterImageReference] {
        let text = html as NSString
        guard let tag else { return [] }
        return matches(of: tag, in: html).map { reference(in: text.substring(with: $0.range)) }
    }

    /// The chapter with every `<img>` replaced by its token, plus the references in
    /// the same order, so token N always belongs to reference N.
    static func replacingTags(in html: String) -> (html: String, references: [ChapterImageReference]) {
        guard let tag else { return (html, []) }
        let text = NSMutableString(string: html)
        let found = matches(of: tag, in: html)
        guard !found.isEmpty else { return (html, []) }

        let references = found.map { reference(in: text.substring(with: $0.range)) }
        // Backwards, so replacing one tag cannot move the ones still to come.
        for (index, match) in found.enumerated().reversed() {
            text.replaceCharacters(in: match.range, with: token(for: index))
        }
        return (text as String, references)
    }

    // MARK: - Reading the tag

    private static func matches(of regex: NSRegularExpression, in html: String) -> [NSTextCheckingResult] {
        regex.matches(in: html, range: NSRange(location: 0, length: (html as NSString).length))
    }

    private static func reference(in tag: String) -> ChapterImageReference {
        ChapterImageReference(src: value(of: src, in: tag), alt: value(of: alt, in: tag))
    }

    /// The first capture group of `pattern` inside `tag`, entities decoded enough to
    /// name a file (`&amp;` is the one EPUB actually uses in `src`).
    private static func value(of regex: NSRegularExpression?, in tag: String) -> String {
        guard let regex else { return "" }
        let text = tag as NSString
        guard let match = regex.firstMatch(in: tag, range: NSRange(location: 0, length: text.length)) else {
            return ""
        }
        for group in 1..<match.numberOfRanges where match.range(at: group).location != NSNotFound {
            return text.substring(with: match.range(at: group))
                .replacingOccurrences(of: "&amp;", with: "&")
        }
        return ""
    }
}
