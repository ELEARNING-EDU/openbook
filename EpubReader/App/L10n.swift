import Foundation

/// The language the app's own interface is drawn in.
///
/// `system` is the phone's language; the two explicit cases override it for this
/// app only (the device language is never changed). The override is kept in the
/// app's own defaults, so a relaunch opens in the chosen language as well.
enum AppLanguage: String, CaseIterable, Identifiable {
    case system
    case vietnamese = "vi"
    case english = "en"

    static let storageKey = "appLanguage"

    var id: String { rawValue }

    /// The resource language, or nil to let the bundle follow the system.
    var code: String? {
        switch self {
        case .system: return nil
        case .vietnamese: return "vi"
        case .english: return "en"
        }
    }

    var locale: Locale {
        code.map(Locale.init(identifier:)) ?? .autoupdatingCurrent
    }

    var titleKey: String { "settings.language.\(rawValue)" }

    /// Makes the choice survive a relaunch. `AppleLanguages` is an app-scoped
    /// preference; the phone's own language list is untouched.
    func applyOverride(in defaults: UserDefaults = .standard) {
        if let code {
            defaults.set([code], forKey: "AppleLanguages")
        } else {
            defaults.removeObject(forKey: "AppleLanguages")
        }
    }

    static var selected: AppLanguage {
        AppLanguage(rawValue: UserDefaults.standard.string(forKey: storageKey) ?? "") ?? .system
    }
}

/// The app's UI strings. Book text, chapter titles and narration never come
/// through here: only the interface around the book is translated.
enum L10n {

    static var locale: Locale { AppLanguage.selected.locale }

    static func bundle(for language: AppLanguage) -> Bundle {
        guard let code = language.code,
              let path = Bundle.main.path(forResource: code, ofType: "lproj"),
              let bundle = Bundle(path: path)
        else { return .main }
        return bundle
    }

    static func string(_ key: String, language: AppLanguage = .selected) -> String {
        NSLocalizedString(key, tableName: nil, bundle: bundle(for: language), value: key, comment: "")
    }

    /// A formatted string (page numbers, percentages) in the reader's locale.
    static func format(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: string(key), locale: locale, arguments: arguments)
    }
}
