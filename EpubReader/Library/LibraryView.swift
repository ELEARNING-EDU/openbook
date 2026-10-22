import SwiftUI
import UniformTypeIdentifiers

struct LibraryView: View {

    @EnvironmentObject private var library: LibraryStore
    /// How far each book has been read, shown under its cover.
    @ObservedObject private var progress = ReadingProgressStore.shared
    /// The look in force. The shelf is printed on the reader's own paper, so it
    /// reads the same settings object the reader writes to and repaints as soon as
    /// a theme or the day/night face changes.
    @ObservedObject private var settings = ReaderSettings.shared
    @State private var showImporter = false
    @State private var path: [BookItem] = []

    /// Paper, ink, accent and the hairlines that belong to the reader's palette:
    /// the library has no paper colour of its own.
    private var palette: ReaderPalette { settings.palette }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                if library.books.isEmpty {
                    emptyState
                } else {
                    LazyVGrid(
                        columns: [GridItem(.adaptive(minimum: 132), spacing: 20)],
                        spacing: 24
                    ) {
                        ForEach(library.books) { book in
                            NavigationLink(value: book) {
                                VStack(alignment: .leading, spacing: 8) {
                                    BookCover(book: book)
                                    ReadProgressRow(
                                        fraction: progress.fraction(for: book.id),
                                        palette: palette
                                    )
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(20)
                }
            }
            .background(palette.swiftPaper.ignoresSafeArea())
            .navigationTitle("Thư viện")
            .navigationDestination(for: BookItem.self) { book in
                ReaderView(book: book)
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showImporter = true
                    } label: {
                        Label("Nhập EPUB", systemImage: "plus")
                    }
                }
            }
            .fileImporter(
                isPresented: $showImporter,
                allowedContentTypes: [Self.epubType],
                allowsMultipleSelection: false
            ) { result in
                if case .success(let urls) = result, let url = urls.first {
                    library.importBook(from: url)
                }
            }
            .alert(
                "Không nhập được sách",
                isPresented: Binding(
                    get: { library.importError != nil },
                    set: { if !$0 { library.importError = nil } }
                )
            ) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(library.importError ?? "")
            }
            // Demo / automation hook: `xcrun simctl launch <udid> <bundle> -demoOpenBook 1`
            // opens the first book without needing a touch event.
            .onAppear {
                applyLibraryDemoHooks()
                let index = UserDefaults.standard.integer(forKey: "demoOpenBook")
                guard index > 0, library.books.count >= index else { return }
                path = [library.books[index - 1]]
            }
        }
        // The shelf's controls and system chrome are the reader's accent and
        // paper, so the two screens never drift apart.
        .tint(palette.swiftAccent)
        .preferredColorScheme(palette.isDark ? .dark : .light)
    }

    /// Drives the shelf's palette from launch arguments, so a capture run can
    /// photograph the library before and after the look changes — on the shelf
    /// itself, and without a relaunch:
    /// `-demoLibraryTheme focus -demoLibraryDark 1`.
    private func applyLibraryDemoHooks() {
        let defaults = UserDefaults.standard
        if let themeID = defaults.string(forKey: "demoLibraryTheme"),
           ReaderTheme.themes.contains(where: { $0.id == themeID }) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                settings.setTheme(themeID)
            }
        }
        if defaults.object(forKey: "demoLibraryDark") != nil {
            let dark = defaults.bool(forKey: "demoLibraryDark")
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                settings.setAppearance(dark: dark)
            }
        }
    }

    private static var epubType: UTType {
        UTType("org.idpf.epub-container")
            ?? UTType(filenameExtension: "epub")
            ?? .data
    }

    private var emptyState: some View {
        VStack(spacing: 14) {
            Image(systemName: "books.vertical")
                .font(.system(size: 44))
                .foregroundStyle(palette.swiftInkSoft)
            Text("Chưa có sách nào")
                .font(.headline)
                .foregroundStyle(palette.swiftInk)
            Text("Nhấn nút ＋ ở góc trên để nhập một tệp EPUB.")
                .font(.footnote)
                .foregroundStyle(palette.swiftInkSoft)
                .multilineTextAlignment(.center)
        }
        .padding(40)
        .frame(maxWidth: .infinity)
    }
}

/// How far this book has been read: a thin line of the reader's accent under the
/// cover, and the figure itself, which is what the reader asked to be able to see.
private struct ReadProgressRow: View {

    let fraction: Double
    /// The reader's palette: its edge draws the empty track, its accent the read
    /// portion, its soft ink the figure.
    let palette: ReaderPalette

    private var percent: Int { Int((min(max(fraction, 0), 1) * 100).rounded()) }

    var body: some View {
        HStack(spacing: 8) {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(palette.swiftPaperEdge)
                    if percent > 0 {
                        Capsule()
                            .fill(palette.swiftAccent)
                            .frame(width: max(geo.size.width * min(max(fraction, 0), 1), 4))
                    }
                }
            }
            .frame(height: 3)

            Text("\(percent)%")
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(palette.swiftInkSoft)
                .fixedSize()
        }
        .padding(.horizontal, 2)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Đã đọc \(percent) phần trăm")
    }
}

/// Covers are generated from the title, so the demo ships no image assets.
struct BookCover: View {

    let book: BookItem

    /// The same colours the mini vinyl uses in the reader, so the spinning disc
    /// reads as "this book".
    static func palette(for title: String) -> [Color] {
        let palettes: [[Color]] = [
            [Color(red: 0.22, green: 0.34, blue: 0.55), Color(red: 0.09, green: 0.16, blue: 0.30)],
            [Color(red: 0.58, green: 0.30, blue: 0.22), Color(red: 0.29, green: 0.13, blue: 0.11)],
            [Color(red: 0.16, green: 0.44, blue: 0.38), Color(red: 0.06, green: 0.23, blue: 0.21)],
            [Color(red: 0.55, green: 0.42, blue: 0.17), Color(red: 0.27, green: 0.19, blue: 0.07)],
            [Color(red: 0.40, green: 0.25, blue: 0.48), Color(red: 0.19, green: 0.11, blue: 0.27)]
        ]
        let sum = title.unicodeScalars.reduce(0) { $0 + Int($1.value) }
        return palettes[sum % palettes.count]
    }

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(colors: palette, startPoint: .topLeading, endPoint: .bottomTrailing)

            VStack(alignment: .leading, spacing: 6) {
                Spacer(minLength: 0)
                Text(book.title)
                    .font(.system(size: 16, weight: .semibold, design: .serif))
                    .foregroundStyle(.white)
                    .lineLimit(4)
                    .multilineTextAlignment(.leading)
                Text(book.author)
                    .font(.system(size: 11))
                    .foregroundStyle(.white.opacity(0.82))
                    .lineLimit(2)
            }
            .padding(14)
        }
        .aspectRatio(0.68, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(.white.opacity(0.22), lineWidth: 1)
        )
        .overlay(alignment: .leading) {
            // A darker strip down the spine edge, so it reads as a book.
            Rectangle()
                .fill(.black.opacity(0.22))
                .frame(width: 5)
                .padding(.vertical, 1)
        }
        .shadow(color: .black.opacity(0.28), radius: 9, x: 0, y: 5)
    }

    private var palette: [Color] { Self.palette(for: book.title) }
}
