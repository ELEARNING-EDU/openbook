import Foundation

/// Word-granular selection arithmetic, kept apart from the views so it can be
/// tested without a screen.
///
/// Offsets are UTF-16 — the same coordinates the paginator, the bookmarks and
/// the voice use, so a saved highlight always points at the same characters.
enum TextSelection {

    /// The word under an offset: the run of characters around it that the page
    /// breaks on. Vietnamese separates words with spaces, so a word is a run of
    /// non-whitespace characters.
    static func wordRange(in text: NSString, around offset: Int) -> NSRange {
        guard text.length > 0 else { return NSRange(location: 0, length: 0) }
        let clamped = min(max(offset, 0), text.length - 1)
        let start = wordStart(in: text, at: clamped)
        let end = wordEnd(in: text, at: clamped)
        return NSRange(location: start, length: max(0, end - start))
    }

    /// The beginning of the word that contains `offset`, or `offset` when it sits
    /// on the break between two words.
    static func wordStart(in text: NSString, at offset: Int) -> Int {
        var index = min(max(offset, 0), text.length)
        guard index > 0 else { return 0 }
        if isBreak(text.character(at: index - 1)) { return index }
        while index > 0, !isBreak(text.character(at: index - 1)) {
            index -= 1
        }
        return index
    }

    /// One past the end of the word that contains `offset`, or `offset` when it
    /// sits on the break between two words.
    static func wordEnd(in text: NSString, at offset: Int) -> Int {
        var index = min(max(offset, 0), text.length)
        while index < text.length, !isBreak(text.character(at: index)) {
            index += 1
        }
        return index
    }

    /// A dragged start handle: snap to a word start, but never past the end of the
    /// chosen passage, and never leave it empty.
    static func adjustedStart(
        in text: NSString,
        proposed offset: Int,
        end: Int
    ) -> Int {
        guard text.length > 0, end > 0 else { return 0 }
        let limit = min(end, text.length)
        let snapped = wordStart(in: text, at: min(max(offset, 0), limit - 1))
        if snapped < limit { return snapped }
        // Past the end: take the last whole word before it.
        return wordStart(in: text, at: max(0, limit - 1))
    }

    /// A dragged end handle: snap to a word end, but keep at least one character
    /// after the start.
    static func adjustedEnd(
        in text: NSString,
        proposed offset: Int,
        start: Int
    ) -> Int {
        guard text.length > 0 else { return 0 }
        let clamped = min(max(offset, 0), text.length)
        let snapped = wordEnd(in: text, at: min(clamped, text.length - 1))
        if snapped > start { return snapped }
        // Pushed behind the start: walk forward to the end of the next word so
        // the passage never becomes empty or ends on a space.
        var index = min(start + 1, text.length)
        while index < text.length, isBreak(text.character(at: index)) {
            index += 1
        }
        return wordEnd(in: text, at: min(index, text.length - 1))
    }

    /// A long-press that drags as well: the passage runs from the word first
    /// touched to the word under the finger.
    static func range(from anchor: Int, to current: Int, in text: NSString) -> NSRange {
        guard text.length > 0 else { return NSRange(location: 0, length: 0) }
        let a = min(max(anchor, 0), text.length - 1)
        let b = min(max(current, 0), text.length - 1)
        let lower = min(a, b)
        let upper = max(a, b)
        let start = wordStart(in: text, at: lower)
        let end = max(start + 1, wordEnd(in: text, at: upper))
        return NSRange(location: start, length: min(end, text.length) - start)
    }

    static func isBreak(_ character: unichar) -> Bool {
        guard let scalar = Unicode.Scalar(character) else { return true }
        return CharacterSet.whitespacesAndNewlines.contains(scalar)
    }
}
