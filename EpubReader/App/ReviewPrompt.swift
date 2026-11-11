import Foundation

/// Counts app launches so the App Store review sheet is asked for once, on the
/// tenth — the same rule as Android's `ReviewPromptStore`. Counted where the
/// process starts, so returning from the background does not inflate it.
enum ReviewPrompt {
    static let launchesBeforeAsk = 10
    static let launchesKey = "reviewPrompt.launches"
    static let askedKey = "reviewPrompt.asked"

    /// Set by `countLaunch()` at app start; read once by the shell.
    static var due = false

    static func shouldAsk(launches: Int, asked: Bool) -> Bool {
        launches >= launchesBeforeAsk && !asked
    }

    /// Counts one launch; true when this launch is the one that should ask.
    /// Screenshot/test launches (`-demo…` arguments) neither count nor ask.
    @discardableResult
    static func countLaunch(_ defaults: UserDefaults = .standard,
                            arguments: [String] = ProcessInfo.processInfo.arguments) -> Bool {
        guard !arguments.contains(where: { $0.hasPrefix("-demo") }) else { return false }
        let launches = defaults.integer(forKey: launchesKey) + 1
        defaults.set(launches, forKey: launchesKey)
        guard shouldAsk(launches: launches, asked: defaults.bool(forKey: askedKey)) else { return false }
        defaults.set(true, forKey: askedKey)
        return true
    }
}
