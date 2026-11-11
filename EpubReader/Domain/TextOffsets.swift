import Foundation

/// The reader stores and draws every place as a UTF-16 offset — `NSRange`,
/// CoreText and the speech engine all count code units — while Swift's
/// `String` counts characters. An emoji or a combining Vietnamese vowel makes
/// the two numbers differ, so a match found with `String` has to be converted
/// rather than counted.
enum TextOffsets {

    static func location(of index: String.Index, in text: String) -> Int {
        index.utf16Offset(in: text)
    }

    /// A stored offset turned back into an index, clamped so a bookmark or a
    /// stale range can never index past either end.
    static func index(at location: Int, in text: String) -> String.Index {
        String.Index(utf16Offset: min(max(location, 0), text.utf16.count), in: text)
    }
}
