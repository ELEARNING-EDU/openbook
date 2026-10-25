import SwiftUI

/// The three places in the app: what is being read, what is owned, and the store
/// demo.
enum MainTab: String, CaseIterable, Identifiable {
    case home
    case library
    case store

    var id: String { rawValue }

    var title: String { L10n.string("tab.\(rawValue)") }

    var symbol: String {
        switch self {
        case .home: return "house"
        case .library: return "books.vertical"
        case .store: return "bag"
        }
    }

    var selectedSymbol: String {
        switch self {
        case .home: return "house.fill"
        case .library: return "books.vertical.fill"
        case .store: return "bag.fill"
        }
    }
}

/// Every measurement the floating tab bar and the screens that scroll under it
/// agree on. The bar floats clear of the bottom edge, so the screens keep its
/// whole height plus a gap free at the foot of their scrolling content.
enum MainTabBarMetrics {
    static let height: CGFloat = 56
    static let hMargin: CGFloat = 18
    /// Gap between the bar and the safe area's bottom edge.
    static let bottomGap: CGFloat = 12
    /// Space the scrolling screens keep free under their last row.
    static let contentReserve: CGFloat = 104
}

/// The app's shell: three screens and a compact tab bar that floats above them,
/// detached from the bottom edge. The bar stays a small group on the leading
/// side, leaving the space beside it free.
///
/// The shell follows the phone's light/dark appearance — never the reader's
/// theme. The reader is a place of its own and paints from `ReaderSettings`.
struct MainView: View {

    @State private var tab: MainTab = .home
    @State private var homePath: [BookItem] = []
    @State private var libraryPath: [BookItem] = []

    @Environment(\.colorScheme) private var colorScheme
    @AppStorage(AppLanguage.storageKey) private var languageID = AppLanguage.system.rawValue

    private var palette: ReaderPalette { .shell(dark: colorScheme == .dark) }
    private var language: AppLanguage { AppLanguage(rawValue: languageID) ?? .system }

    /// A book is a full-screen experience, so the bar steps aside while one is
    /// open; it comes back with the shelf or the home screen.
    private var showsBar: Bool {
        switch tab {
        case .home: return homePath.isEmpty
        case .library: return libraryPath.isEmpty
        case .store: return true
        }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Group {
                switch tab {
                case .home:
                    HomeView(path: $homePath) { tab = .library }
                case .library:
                    LibraryView(path: $libraryPath)
                case .store:
                    StoreView()
                }
            }

            if showsBar {
                HStack(spacing: 0) {
                    FloatingTabBar(selection: $tab, palette: palette)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, MainTabBarMetrics.hMargin)
                .padding(.bottom, MainTabBarMetrics.bottomGap)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.22), value: tab)
        .animation(.easeInOut(duration: 0.22), value: showsBar)
        .tint(palette.swiftAccent)
        .environment(\.locale, language.locale)
        .onAppear(perform: applyDemoTabHook)
    }

    /// Lets a capture run start on any tab without a touch:
    /// `-demoTab library` or `-demoTab store`. Home is the default.
    private func applyDemoTabHook() {
        guard let raw = UserDefaults.standard.string(forKey: "demoTab"),
              let requested = MainTab(rawValue: raw) else { return }
        tab = requested
    }
}

/// The floating tab bar: a compact capsule of paper lifted off the page, with
/// the chosen tab named and the others as icons. It is deliberately narrower
/// than the screen, so a second floating control can sit beside it later.
struct FloatingTabBar: View {

    @Binding var selection: MainTab
    let palette: ReaderPalette

    var body: some View {
        HStack(spacing: 2) {
            ForEach(MainTab.allCases) { tab in
                Button {
                    selection = tab
                } label: {
                    HStack(spacing: 7) {
                        Image(systemName: selection == tab ? tab.selectedSymbol : tab.symbol)
                            .font(.system(size: 17, weight: .semibold))
                        if selection == tab {
                            Text(verbatim: tab.title)
                                .font(.system(size: 12, weight: .semibold, design: .rounded))
                                .fixedSize()
                        }
                    }
                    .foregroundStyle(selection == tab ? palette.swiftAccent : palette.swiftInkSoft)
                    .frame(minWidth: 44, minHeight: 44)
                    .padding(.horizontal, 9)
                    .background {
                        if selection == tab {
                            Capsule().fill(palette.swiftAccent.opacity(palette.isDark ? 0.20 : 0.11))
                        }
                    }
                    .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(verbatim: tab.title))
                .accessibilityAddTraits(selection == tab ? .isSelected : [])
            }
        }
        .padding(.horizontal, 6)
        .frame(height: MainTabBarMetrics.height)
        .background(Capsule().fill(palette.controlFill))
        .overlay(Capsule().strokeBorder(palette.swiftInk.opacity(0.14)))
        .shadow(color: .black.opacity(0.22), radius: 14, y: 6)
    }
}
