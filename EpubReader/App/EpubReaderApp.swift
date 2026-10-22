import SwiftUI

@main
struct EpubReaderApp: App {
    @StateObject private var library = LibraryStore()

    var body: some Scene {
        WindowGroup {
            LibraryView()
                .environmentObject(library)
                .tint(Color(red: 0.62, green: 0.35, blue: 0.20))
        }
    }
}
