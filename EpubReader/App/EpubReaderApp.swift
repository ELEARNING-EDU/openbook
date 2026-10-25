import SwiftUI

@main
struct EpubReaderApp: App {
    @StateObject private var library = LibraryStore()

    init() {
        // "Theo máy" must keep following the phone: drop an override left by an
        // earlier choice, which the system would otherwise keep for this app.
        if AppLanguage.selected == .system {
            UserDefaults.standard.removeObject(forKey: "AppleLanguages")
        }
    }

    var body: some Scene {
        WindowGroup {
            MainView()
                .environmentObject(library)
                .tint(Color(red: 0.62, green: 0.35, blue: 0.20))
        }
    }
}
