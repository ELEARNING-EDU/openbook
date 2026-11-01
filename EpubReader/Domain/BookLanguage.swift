import Foundation
import NaturalLanguage

/// Works out which voice should read a book.
///
/// The package's `dc:language` is only a hint. Many EPUBs sold online carry
/// `en` on a Vietnamese book, so the text itself is checked for a strong signal
/// first; when the text is ambiguous the tag wins, because a tag is still better
/// evidence than a guess.
enum BookLanguage {

    /// The language code (`vi`, `ja`, `zh`, `en`, …) the voice should use, or
    /// nil when neither the tag nor the text says anything useful.
    static func resolve(tag: String?, sample: String) -> String? {
        if let strong = strongLanguage(in: sample) { return strong }
        if let tag, !tag.isEmpty { return code(tag) }
        if !sample.isEmpty,
           let detected = NLLanguageRecognizer.dominantLanguage(for: String(sample.prefix(1200))) {
            return code(detected.rawValue)
        }
        return nil
    }

    /// The language the text itself insists on, if any.
    ///
    /// Only scripts that cannot be mistaken for another language count: kana is
    /// Japanese, Vietnamese tone marks and letters are Vietnamese, and Han
    /// characters without kana are Chinese. Everything else is left to the tag.
    static func strongLanguage(in sample: String) -> String? {
        guard !sample.isEmpty else { return nil }
        let text = sample.precomposedStringWithCanonicalMapping

        var kana = 0
        var han = 0
        var vietnamese = 0

        for scalar in text.unicodeScalars {
            let value = scalar.value
            if (0x3040...0x30FF).contains(value) {
                kana += 1
            } else if (0x4E00...0x9FFF).contains(value) || (0x3400...0x4DBF).contains(value) {
                han += 1
            } else if vietnameseSignals.contains(scalar) {
                vietnamese += 1
            }
        }

        if kana > 0 { return "ja" }
        if vietnamese >= 2 { return "vi" }
        if han >= 2 { return "zh" }
        return nil
    }

    /// `vi-VN` → `vi`; a language code is what the synthesizer matches on.
    static func code(_ tag: String) -> String {
        String(tag.split(separator: "-").first ?? Substring(tag)).lowercased()
    }

    /// Letters and tone marks that only Vietnamese uses.
    private static let vietnameseSignals: Set<Unicode.Scalar> = {
        let letters =
            "ăâđêôơưĂÂĐÊÔƠƯ"
            + "àáảãạằắẳẵặầấẩẫậèéẻẽẹềếểễệìíỉĩị"
            + "òóỏõọồốổỗộờớởỡợùúủũụừứửữựỳýỷỹỵ"
            + "ÀÁẢÃẠẰẮẲẴẶẦẤẨẪẬÈÉẺẼẸỀẾỂỄỆÌÍỈĨỊ"
            + "ÒÓỎÕỌỒỐỔỖỘỜỚỞỠỢÙÚỦŨỤỪỨỬỮỰỲÝỶỸỴ"
        return Set(letters.unicodeScalars)
    }()
}
