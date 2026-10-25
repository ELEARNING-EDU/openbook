import Foundation

/// Turns stored seconds into the one way the app writes a duration: minutes
/// under an hour, hours and minutes above it. Kept out of the views so the card,
/// the sheet and the tests all read the same sentence.
enum ReadingStatsText {

    static func duration(minutes: Int, language: AppLanguage = .selected) -> String {
        let total = max(0, minutes)
        let hours = total / 60
        let remainder = total % 60

        if hours == 0 {
            return text("duration.minutes", language: language, [total])
        }
        if remainder == 0 {
            return text("duration.hours", language: language, [hours])
        }
        return text("duration.hoursMinutes", language: language, [hours, remainder])
    }

    static func days(_ count: Int, language: AppLanguage = .selected) -> String {
        text(count == 1 ? "stats.day" : "stats.days", language: language, [count])
    }

    private static func text(_ key: String, language: AppLanguage, _ arguments: [CVarArg]) -> String {
        String(
            format: L10n.string(key, language: language),
            locale: language.locale,
            arguments: arguments
        )
    }
}
