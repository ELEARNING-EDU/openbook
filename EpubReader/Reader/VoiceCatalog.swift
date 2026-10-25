import AVFoundation
import Foundation

/// One installed system voice, cut down to what the picker needs.
struct VoiceEntry: Identifiable, Equatable {

    let id: String
    let name: String
    let language: String
    /// `AVSpeechSynthesisVoiceQuality` raw value: 1 default, 2 enhanced, 3 premium.
    let quality: Int

    var isVietnamese: Bool { language.hasPrefix("vi") }

    static func entries(from voices: [AVSpeechSynthesisVoice]) -> [VoiceEntry] {
        voices.map {
            VoiceEntry(
                id: $0.identifier,
                name: $0.name,
                language: $0.language,
                quality: $0.quality.rawValue
            )
        }
    }
}

/// One language/region heading in the picker, with the voices installed for it.
struct VoiceGroup: Identifiable, Equatable {
    let id: String
    let title: String
    let entries: [VoiceEntry]
}

/// Groups the installed voices by language and region, the phone's own language
/// first, then Vietnamese — iOS exposes one locale per language, so the region
/// in the heading is the voice's own (`vi-VN`, `en-GB`, …), not a made-up dialect.
enum VoiceCatalog {

    static func groups(
        from entries: [VoiceEntry],
        preferredLanguage: String? = Locale.preferredLanguages.first,
        locale: Locale = L10n.locale
    ) -> [VoiceGroup] {
        let preferred = preferredLanguage?
            .split(separator: "-")
            .first
            .map(String.init)

        func isPreferred(_ group: VoiceGroup) -> Bool {
            guard let preferred else { return false }
            return group.id.split(separator: "-").first.map(String.init) == preferred
        }

        return Dictionary(grouping: entries, by: \.language)
            .map { language, entries in
                VoiceGroup(
                    id: language,
                    title: locale.localizedString(forIdentifier: language) ?? language,
                    entries: entries.sorted {
                        $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
                    }
                )
            }
            .sorted { first, second in
                if isPreferred(first) != isPreferred(second) { return isPreferred(first) }
                if first.id.hasPrefix("vi") != second.id.hasPrefix("vi") {
                    return first.id.hasPrefix("vi")
                }
                return first.title.localizedCaseInsensitiveCompare(second.title) == .orderedAscending
            }
    }

    /// Keeps only voices that speak the book's language — a foreign voice reads
    /// Vietnamese badly. A book in a language the phone has no voice for keeps
    /// every voice, so the sheet can never be empty.
    static func voices(_ entries: [VoiceEntry], speaking language: String?) -> [VoiceEntry] {
        guard let language, !language.isEmpty else { return entries }
        let short = language.split(separator: "-").first.map(String.init)?.lowercased() ?? language
        let matching = entries.filter {
            $0.language.split(separator: "-").first.map(String.init)?.lowercased() == short
        }
        return matching.isEmpty ? entries : matching
    }

    /// Narrows the list to languages or voice names containing the query. A
    /// language hit keeps the whole group; a name hit keeps only those rows.
    static func groups(_ groups: [VoiceGroup], matching query: String) -> [VoiceGroup] {
        let needle = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !needle.isEmpty else { return groups }

        return groups.compactMap { group in
            let languageHit = group.title.localizedCaseInsensitiveContains(needle)
                || group.id.localizedCaseInsensitiveContains(needle)
            if languageHit { return group }

            let entries = group.entries.filter {
                $0.name.localizedCaseInsensitiveContains(needle)
            }
            return entries.isEmpty
                ? nil
                : VoiceGroup(id: group.id, title: group.title, entries: entries)
        }
    }
}
