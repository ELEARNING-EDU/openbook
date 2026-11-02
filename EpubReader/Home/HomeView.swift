import SwiftUI

/// The home screen: the books that are actually part-way read, ready to be
/// picked up again. A book appears here only after a page of it has been saved.
///
/// More than one book can be in progress, so the shelf runs sideways: each card
/// is a whole book, and the next one waits just past the edge of the screen.
struct HomeView: View {

    @Binding var path: [BookItem]
    /// Sends the reader to the shelf when there is nothing to continue.
    let openLibrary: () -> Void

    @EnvironmentObject private var library: LibraryStore
    @ObservedObject private var progress = ReadingProgressStore.shared
    @ObservedObject private var stats = ReadingStatsStore.shared
    /// See `LibraryView`: the window scheme belongs to the open book, so it is
    /// released here when the book closes rather than on the reader itself.
    @ObservedObject private var readerSettings = ReaderSettings.shared

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.playerInset) private var playerInset

    @AppStorage(ReadingGoal.storageKey) private var goalMinutes = ReadingGoal.defaultMinutes
    @State private var showSettings = false
    @State private var showStats = false
    @State private var openSettingsAfterStats = false
    @State private var seededDemoStats = false
    @State private var openedDemoSheet = false
    @State private var isAtTop = true

    private var palette: ReaderPalette { .shell(dark: colorScheme == .dark) }

    private var readerScheme: ColorScheme? {
        guard !path.isEmpty else { return nil }
        return readerSettings.palette.isDark ? .dark : .light
    }

    private var newlyAddedBooks: [BookItem] {
        HomeSections.newlyAdded(
            books: library.books,
            positions: progress.positions,
            metadata: library.allMetadata,
            finished: library.finishedIDs
        )
    }

    private var recentlyFinishedBooks: [BookItem] {
        HomeSections.recentlyFinished(
            books: library.books,
            metadata: library.allMetadata,
            now: Date()
        )
    }

    private var suggestionBooks: [BookItem] {
        HomeSections.suggestions(
            books: library.books,
            positions: progress.positions,
            metadata: library.allMetadata,
            finished: library.finishedIDs,
            excluding: Set(newlyAddedBooks.map(\.id)),
            now: Date()
        )
    }

    private var weekPoints: [ReadingWeekPoint] {
        stats.week(now: Date())
    }

    private var weekHasReading: Bool {
        weekPoints.contains { $0.minutes >= 1 }
    }

    private func monthText(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = L10n.locale
        let sameYear = Calendar.current.component(.year, from: date)
            == Calendar.current.component(.year, from: Date())
        formatter.setLocalizedDateFormatFromTemplate(sameYear ? "LLL" : "LLL yyyy")
        let name = formatter.string(from: date)
        // Vietnamese writes "thg 10" mid-sentence; English keeps "Oct".
        guard L10n.locale.identifier.hasPrefix("vi"), let first = name.first else {
            return name
        }
        return first.lowercased() + name.dropFirst()
    }

    private var items: [ContinueReadingItem] {
        HomeFeed.continueReading(
            books: library.books,
            positions: progress.positions,
            finished: library.finishedIDs
        )
    }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    Text("home.title")
                        .font(.system(size: 30, weight: .semibold, design: .serif))
                        .foregroundStyle(palette.swiftInk)
                        .padding(.horizontal, 20)
                    if !library.books.isEmpty {
                        // A minute is fine for a minute-based card: the summary
                        // crosses midnight without needing a relaunch.
                        TimelineView(.periodic(from: Date(), by: 60)) { context in
                            statsSection(now: context.date)
                        }
                    }
                    if items.isEmpty {
                        emptyState
                    } else {
                        sectionHeading
                        carousel
                    }
                    if HomeSections.showsExtraRows(bookCount: library.books.count) {
                        if !newlyAddedBooks.isEmpty {
                            HomeBookRow(
                                titleKey: "home.new.title",
                                books: newlyAddedBooks,
                                palette: palette
                            ) { book in
                                L10n.format(
                                    "home.added.month",
                                    monthText(library.metadata(for: book.id)?.importedAt ?? Date())
                                )
                            }
                        }
                        if !suggestionBooks.isEmpty {
                            HomeBookRow(
                                titleKey: "home.suggestions.title",
                                books: suggestionBooks,
                                palette: palette
                            ) { _ in
                                L10n.string("home.suggestions.reason")
                            }
                        }
                        if !recentlyFinishedBooks.isEmpty {
                            HomeBookRow(
                                titleKey: "home.finished.title",
                                books: recentlyFinishedBooks,
                                palette: palette
                            ) { book in
                                let date = library.metadata(for: book.id)?.finishedAt ?? Date()
                                return L10n.format("home.finished.month", monthText(date))
                            }
                        }
                        if weekHasReading {
                            WeeklyReadingChart(
                                points: weekPoints,
                                goalMinutes: goalMinutes,
                                palette: palette
                            )
                            .padding(.horizontal, 20)
                        }
                    }
                }
                .padding(.top, 8)
                .padding(.bottom, MainTabBarMetrics.contentReserve + playerInset)
            }
            .background(palette.swiftPaper.ignoresSafeArea())
            .scrollIndicators(.hidden)
            .onScrollGeometryChange(for: Bool.self) { geometry in
                geometry.contentOffset.y <= 1
            } action: { _, atTop in
                isAtTop = atTop
            }
            .onAppear {
                seedDemoStatsIfAsked()
                openStatsSheetIfAsked()
            }
            .navigationDestination(for: BookItem.self) { book in
                ReaderView(book: book)
            }
            .preferredColorScheme(readerScheme)
            // No navigation bar at all: the screen has no title, and the gear
            // is drawn here so it can take the paper's own tint instead of the
            // system's white chip.
            .toolbar(.hidden, for: .navigationBar)
            .overlay(alignment: .topTrailing) {
                if isAtTop {
                    Button {
                        showSettings = true
                    } label: {
                    Image(systemName: "gearshape")
                        .font(.system(size: ReaderMetrics.buttonIconSize, weight: .semibold))
                        .foregroundStyle(palette.swiftInk)
                        .frame(width: ReaderMetrics.buttonSize, height: ReaderMetrics.buttonSize)
                        .background(Circle().fill(palette.cardFill))
                        .overlay(Circle().strokeBorder(palette.swiftInk.opacity(0.10)))
                        .contentShape(Circle())
                }
                .buttonStyle(.plain)
                .padding(.trailing, 20)
                .padding(.top, 4)
                    .accessibilityLabel(Text("settings.title"))
                    .transition(.opacity)
                }
            }
            .animation(.easeInOut(duration: 0.2), value: isAtTop)
            .sheet(isPresented: $showSettings) {
                AppSettingsView()
            }
            .sheet(isPresented: $showStats, onDismiss: {
                if openSettingsAfterStats {
                    openSettingsAfterStats = false
                    showSettings = true
                }
            }) {
                ReadingStatsSheet(
                    summary: stats.summary(now: Date(), goalMinutes: goalMinutes),
                    finished: library.finishedBooks(
                        in: Calendar.current.component(.year, from: Date())
                    ),
                    palette: palette
                ) {
                    openSettingsAfterStats = true
                    showStats = false
                }
                .preferredColorScheme(palette.isDark ? .dark : .light)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
            }
        }
    }

    private var carousel: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 14) {
                ForEach(items) { item in
                    NavigationLink(value: item.book) {
                        ContinueReadingCard(item: item, palette: palette)
                            .frame(width: 300)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(
                        Text(verbatim: L10n.format("home.book.a11y", item.book.title, item.book.author))
                    )
                }
            }
            .scrollTargetLayout()
            .padding(.vertical, 2)
        }
        .scrollTargetBehavior(.viewAligned)
        .contentMargins(.horizontal, 20, for: .scrollContent)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Text("home.reading"))
    }

    private func statsSection(now: Date) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("home.stats.title")
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .tracking(1.0)
                .textCase(.uppercase)
                .foregroundStyle(palette.swiftInkSoft)
            ReadingStatsCard(
                summary: stats.summary(now: now, goalMinutes: goalMinutes),
                palette: palette
            ) {
                showStats = true
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 4)
    }

    /// Seeds a few days of reading for capture runs only:
    /// `-demoStatsSeed 1`. Never runs without the launch argument.
    private func seedDemoStatsIfAsked() {
        guard !seededDemoStats,
              UserDefaults.standard.bool(forKey: "demoStatsSeed")
        else { return }
        seededDemoStats = true

        let now = Date()
        // A real session, so the sheet's "longest session" record is not zero.
        stats.startSession(at: now.addingTimeInterval(-960))
        stats.noteActivity(at: now.addingTimeInterval(-480))
        stats.endSession(at: now)
        stats.add(seconds: 600, listening: false, endingAt: now.addingTimeInterval(-86_400))
        stats.add(seconds: 1200, listening: false, endingAt: now.addingTimeInterval(-2 * 86_400))
        stats.add(seconds: 3600, listening: false, endingAt: now.addingTimeInterval(-12 * 86_400))
        stats.add(seconds: 18000, listening: false, endingAt: now.addingTimeInterval(-100 * 86_400))
    }

    /// Opens the sheet for capture runs: `-demoStatsSheet 1`.
    private func openStatsSheetIfAsked() {
        guard !openedDemoSheet,
              UserDefaults.standard.bool(forKey: "demoStatsSheet")
        else { return }
        openedDemoSheet = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { showStats = true }
    }

    private var sectionHeading: some View {
        Text("home.reading")
            .font(.system(size: 14, weight: .semibold, design: .rounded))
            .tracking(1.0)
            .textCase(.uppercase)
            .foregroundStyle(palette.swiftInkSoft)
            .padding(.horizontal, 20)
    }

    private var emptyState: some View {
        VStack(spacing: 14) {
            Image(systemName: "book.closed")
                .font(.system(size: 42))
                .foregroundStyle(palette.swiftInkSoft)
            Text("home.empty.title")
                .font(.system(size: 19, weight: .semibold, design: .serif))
                .foregroundStyle(palette.swiftInk)
            Text("home.empty.body")
                .font(.footnote)
                .foregroundStyle(palette.swiftInkSoft)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Button(action: openLibrary) {
                Text("home.openLibrary")
                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                    .foregroundStyle(palette.swiftAccent)
                    .padding(.horizontal, 18)
                    .frame(minHeight: 44)
                    .background(Capsule().fill(palette.swiftAccent.opacity(palette.isDark ? 0.20 : 0.11)))
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 32)
        .padding(.top, 64)
    }
}

/// A book that is part-way read: its cover, where the reader stopped, and how
/// far through they are.
private struct ContinueReadingCard: View {

    let item: ContinueReadingItem
    let palette: ReaderPalette

    var body: some View {
        HStack(spacing: 14) {
            BookCover(book: item.book, showsGeneratedText: false)
                .frame(width: 72)

            VStack(alignment: .leading, spacing: 7) {
                Text(verbatim: item.book.title)
                    .font(.system(size: 17, weight: .semibold, design: .serif))
                    .foregroundStyle(palette.swiftInk)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)

                Text(verbatim: item.book.author)
                    .font(.system(size: 12))
                    .foregroundStyle(palette.swiftInkSoft)
                    .lineLimit(1)

                Spacer(minLength: 0)

                // Page and percent in one figure; the standalone percentage
                // used to sit above it and said the same thing twice.
                Text(verbatim: item.placeLabel)
                    .font(.system(size: 11, weight: .medium, design: .rounded))
                    .foregroundStyle(palette.swiftInkSoft)
                    .fixedSize()

                Text("home.continue")
                    .font(.system(size: 12, weight: .semibold, design: .rounded))
                    .foregroundStyle(palette.swiftAccent)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(palette.cardFill)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(palette.swiftInk.opacity(0.10))
        )
        .contentShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private extension ContinueReadingItem {

    var percent: Int {
        Int((min(max(progress.fraction, 0), 1) * 100).rounded())
    }

    /// Where the reader will land: the page saved with the place, against the
    /// page count of the typesetting it was saved in.
    var placeLabel: String {
        guard progress.pageCount > 0 else {
            return L10n.format("home.readPercent", percent)
        }
        let page = min(max(progress.page, 0), progress.pageCount - 1) + 1
        return L10n.format("home.place", page, progress.pageCount, percent)
    }
}
