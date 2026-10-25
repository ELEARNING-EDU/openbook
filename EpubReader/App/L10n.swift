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

    /// The interface language this phone starts in: Vietnamese only when it is
    /// the phone's first language, English for every other phone. The app ships
    /// both, so this is a deliberate choice rather than the bundle's own
    /// fallback, which would pick Vietnamese from a second-place entry.
    static func resolved(for preferred: [String]) -> AppLanguage {
        preferred.first?.hasPrefix("vi") == true ? .vietnamese : .english
    }

    static var device: AppLanguage { resolved(for: Locale.preferredLanguages) }
}

/// The app's UI strings. Book text, chapter titles and narration never come
/// through here: only the interface around the book is translated.
enum L10n {

    /// The resolved language's own locale, so `system` formats numbers and
    /// percentages in the language the interface is actually drawn in.
    static var locale: Locale { (AppLanguage.selected.code ?? AppLanguage.device.code).map(Locale.init(identifier:)) ?? .autoupdatingCurrent }

    /// The language's own bundle; `system` resolves to vi or en by the phone's
    /// first language, never to the bundle iOS would have picked for us.
    static func bundle(for language: AppLanguage) -> Bundle {
        let code = language.code ?? AppLanguage.device.code
        guard let code,
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
