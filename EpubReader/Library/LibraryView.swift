import SwiftUI
import UIKit
import UniformTypeIdentifiers

/// How the shelf lists its books, kept between launches.
enum LibraryLayout: String {
    case grid
    case list
}

struct LibraryView: View {

    /// The books opened from the shelf, owned by the app shell so the floating
    /// bar can step aside while one is being read.
    @Binding var path: [BookItem]

    @EnvironmentObject private var library: LibraryStore
    /// How far each book has been read, shown under its cover.
    @ObservedObject private var progress = ReadingProgressStore.shared
    /// The reader's own night/day, needed here so the window's colour scheme is
    /// released the moment the book closes — keeping it on the reader itself
    /// made the shelf repaint dark for the length of the pop animation.
    @ObservedObject private var readerSettings = ReaderSettings.shared
    @AppStorage("libraryLayout") private var layout: LibraryLayout = .grid
    @AppStorage("librarySort") private var sort: LibrarySort = .recent
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.playerInset) private var playerInset

    @State private var showImporter = false
    @State private var selecting = false
    @State private var selected: Set<String> = []
    @State private var editing: BookItem?
    @State private var deleteRequest: DeleteRequest?
    /// The harness opens the demo book once, not every time the shelf reappears.
    private static var demoOpenedBook = false

    private var palette: ReaderPalette { .shell(dark: colorScheme == .dark) }

    /// The reader's look while a book is open; the shelf's own appearance as soon
    /// as it closes, so the pop animation cannot paint the shelf in the wrong one.
    private var readerScheme: ColorScheme? {
        guard !path.isEmpty else { return nil }
        return readerSettings.palette.isDark ? .dark : .light
    }

    private var books: [BookItem] { library.sortedBooks(by: sort) }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text("library.title")
                        .font(.system(size: 30, weight: .semibold, design: .serif))
                        .foregroundStyle(palette.swiftInk)
                        .padding(.horizontal, 20)

                    if library.books.isEmpty {
                        emptyState
                    } else {
                        shelf
                            .padding(.horizontal, 20)
                            .padding(.top, 4)
                    }
                }
                .padding(.top, 8)
                .padding(.bottom, MainTabBarMetrics.contentReserve + playerInset)
            }
            .background(palette.swiftPaper.ignoresSafeArea())
            .scrollIndicators(.hidden)
            .navigationDestination(for: BookItem.self) { book in
                ReaderView(book: book)
            }
            .preferredColorScheme(readerScheme)
            .toolbar { toolbar }
            // The bar only exists for selection. Otherwise the pair is drawn on
            // the paper itself, so it takes the page's tint rather than the
            // system's white glass.
            .toolbar(selecting ? .visible : .hidden, for: .navigationBar)
            .overlay(alignment: .topTrailing) {
                if !selecting {
                    shelfControls
                        .padding(.trailing, 20)
                        .padding(.top, 4)
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
                "library.import.failed.title",
                isPresented: errorBinding(\.importError)
            ) {
                Button("common.ok", role: .cancel) {}
            } message: {
                Text(verbatim: library.importError ?? "")
            }
            .alert(
                "library.delete.failed.title",
                isPresented: errorBinding(\.deleteError)
            ) {
                Button("common.ok", role: .cancel) {}
            } message: {
                Text(verbatim: library.deleteError ?? "")
            }
            .confirmationDialog(
                Text(deleteTitle),
                isPresented: Binding(
                    get: { deleteRequest != nil },
                    set: { if !$0 { deleteRequest = nil } }
                ),
                titleVisibility: .visible,
                presenting: deleteRequest
            ) { request in
                Button("library.delete.confirm", role: .destructive) {
                    delete(request)
                }
                Button("common.cancel") {}
            } message: { request in
                deleteMessage(request)
            }
            .sheet(item: $editing) { book in
                BookEditSheet(book: book) { title, author in
                    library.updateMetadata(id: book.id, title: title, author: author)
                }
            }
            // Demo / automation hook: `xcrun simctl launch <udid> <bundle> -demoOpenBook 1`
            // opens the first book without needing a touch event.
            .onAppear {
                applyLibraryDemoHooks()
                guard !Self.demoOpenedBook else { return }
                let index = UserDefaults.standard.integer(forKey: "demoOpenBook")
                guard index > 0, library.books.count >= index else { return }
                Self.demoOpenedBook = true
                path = [library.books[index - 1]]
            }
        }
        .tint(palette.swiftAccent)
    }

    // MARK: - Toolbar

    /// The navigation bar is shown only while selecting. The rest of the time
    /// the pair is drawn over the page by `shelfControls`.
    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        if selecting {
            ToolbarItem(placement: .topBarLeading) {
                Button(role: .destructive) {
                    deleteRequest = DeleteRequest(books: books.filter { selected.contains($0.id) })
                } label: {
                    Image(systemName: "trash")
                }
                .disabled(selected.isEmpty)
                .accessibilityLabel(Text("library.delete"))
            }
            ToolbarItem(placement: .principal) {
                Text(verbatim: L10n.format("library.selectedCount", selected.count))
                    .font(.system(size: 15, weight: .semibold, design: .rounded))
                    .foregroundStyle(palette.swiftInk)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("common.cancel") { endSelection() }
            }
        }
    }

    /// One capsule holding the shelf's two buttons: the menu on the outside,
    /// import just inside it — the same shape as the reader's toolbar pairs.
    private var shelfControls: some View {
        HStack(spacing: 0) {
            Menu {
                Button {
                    selecting = true
                } label: {
                    Label("library.select", systemImage: "checkmark.circle")
                }

                Menu {
                    ForEach(LibrarySort.allCases) { option in
                        Button {
                            sort = option
                        } label: {
                            if sort == option {
                                Label(L10n.string(option.labelKey), systemImage: "checkmark")
                            } else {
                                Text(verbatim: L10n.string(option.labelKey))
                            }
                        }
                    }
                } label: {
                    Label("library.sort", systemImage: "arrow.up.arrow.down")
                }

                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        layout = layout == .grid ? .list : .grid
                    }
                } label: {
                    Label(
                        layout == .grid ? "library.layout.toList" : "library.layout.toGrid",
                        systemImage: layout == .grid ? "list.bullet" : "square.grid.2x2"
                    )
                }
            } label: {
                shelfToolbarIcon("ellipsis")
            }
            .accessibilityLabel(Text("library.more"))

            Rectangle()
                .fill(palette.swiftInk.opacity(0.12))
                .frame(width: 1, height: 22)

            Button {
                showImporter = true
            } label: {
                shelfToolbarIcon("plus")
            }
            .accessibilityLabel(Text("library.import"))
        }
        // A faint film of the paper rather than a white chip, so the pair sits
        // on the page instead of floating above it.
        .background(Capsule().fill(palette.cardFill))
        .overlay(Capsule().strokeBorder(palette.swiftInk.opacity(0.10)))
        .shadow(color: .black.opacity(0.08), radius: 6, y: 2)
    }

    /// Same metrics as the reader's toolbar buttons: 44pt target, 15pt icon,
    /// 22pt divider — so the shelf's pair matches the book's pair.
    private func shelfToolbarIcon(_ symbol: String) -> some View {
        Image(systemName: symbol)
            .font(.system(size: ReaderMetrics.buttonIconSize, weight: .semibold))
            .foregroundStyle(palette.swiftInk)
            .frame(width: ReaderMetrics.buttonSize, height: ReaderMetrics.buttonSize)
            .contentShape(Rectangle())
    }

    // MARK: - Shelf

    /// The same books in the user's chosen shape. Both reuse the cover and the
    /// percentage, so switching layouts changes the arrangement and nothing else.
    @ViewBuilder
    private var shelf: some View {
        switch layout {
        case .grid:
            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: 96), spacing: 16)],
                spacing: 18
            ) {
                ForEach(books) { book in
                    ZStack(alignment: .topTrailing) {
                        if selecting {
                            Button {
                                toggleSelection(book.id)
                            } label: {
                                gridTile(book)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(bookLabel(book))
                            .accessibilityAddTraits(selected.contains(book.id) ? .isSelected : [])
                        } else {
                            NavigationLink(value: book) {
                                gridTile(book)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(bookLabel(book))
                        }

                        if selecting {
                            selectionMark(book.id)
                                .padding(6)
                                .allowsHitTesting(false)
                        }
                    }
                }
            }
        case .list:
            LazyVStack(spacing: 0) {
                ForEach(books) { book in
                    HStack(spacing: 8) {
                        if selecting {
                            Button {
                                toggleSelection(book.id)
                            } label: {
                                listRow(book)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(bookLabel(book))
                            .accessibilityAddTraits(selected.contains(book.id) ? .isSelected : [])
                        } else {
                            NavigationLink(value: book) {
                                listRow(book)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(bookLabel(book))
                        }

                        if selecting {
                            selectionMark(book.id)
                                .allowsHitTesting(false)
                        } else {
                            bookMenu(book)
                        }
                    }

                    if book.id != books.last?.id {
                        Rectangle()
                            .fill(palette.swiftInk.opacity(0.12))
                            .frame(height: 1)
                    }
                }
            }
        }
    }

    /// Grid tile: the cover, then one short line under it — how far the book is
    /// read, and its own menu. The menu sits on that line rather than on the
    /// cover, so it never covers the art.
    private func gridTile(_ book: BookItem) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            BookCover(book: book)
            HStack(spacing: 6) {
                readState(book)
                Spacer(minLength: 0)
                if !selecting {
                    bookMenu(book)
                }
            }
        }
        .contentShape(Rectangle())
    }

    private func listRow(_ book: BookItem) -> some View {
        HStack(alignment: .center, spacing: 14) {
            BookCover(book: book)
                .frame(width: 64)
            VStack(alignment: .leading, spacing: 6) {
                Text(verbatim: book.title)
                    .font(.system(size: 16, weight: .semibold, design: .serif))
                    .foregroundStyle(palette.swiftInk)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Text(verbatim: book.author)
                    .font(.system(size: 12))
                    .foregroundStyle(palette.swiftInkSoft)
                    .lineLimit(1)
                readState(book)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }

    /// One book's own menu. Only the things that belong to a single book: finish
    /// it, edit it, or remove it.
    private func bookMenu(_ book: BookItem) -> some View {
        Menu {
            let finished = isFinished(book)
            Button {
                library.markFinished(id: book.id, finished: !finished)
            } label: {
                Label(
                    finished ? "library.unmarkFinished" : "library.markFinished",
                    systemImage: finished ? "arrow.uturn.backward" : "checkmark.circle"
                )
            }
            Button {
                editing = book
            } label: {
                Label("library.edit", systemImage: "pencil")
            }
            Button(role: .destructive) {
                deleteRequest = DeleteRequest(books: [book])
            } label: {
                Label("library.delete", systemImage: "trash")
            }
        } label: {
            Image(systemName: "ellipsis")
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(palette.swiftInkSoft)
                .frame(width: 30, height: 30)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(Text("library.bookMenu"))
    }

    private func selectionMark(_ id: String) -> some View {
        Image(systemName: selected.contains(id) ? "checkmark.circle.fill" : "circle")
            .font(.system(size: 22, weight: .semibold))
            .foregroundStyle(selected.contains(id) ? palette.swiftAccent : palette.swiftInkSoft)
            .background(Circle().fill(palette.swiftPaper).padding(2))
    }

    private func bookLabel(_ book: BookItem) -> Text {
        if isFinished(book) {
            return Text(verbatim: L10n.format("library.book.finished.a11y", book.title, book.author))
        }
        // The cover prints the title for a generated cover; the item reads the
        // title and author once, not once per place they are drawn.
        return Text(verbatim: L10n.format("home.book.a11y", book.title, book.author))
    }

    private func isFinished(_ book: BookItem) -> Bool {
        library.metadata(for: book.id)?.finishedAt != nil
    }

    /// The shelf line under a cover: how far it is read, or that it is done.
    @ViewBuilder
    private func readState(_ book: BookItem) -> some View {
        if isFinished(book) {
            Text("library.finished")
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftAccent)
        } else {
            ReadPercentLabel(fraction: progress.fraction(for: book.id), palette: palette)
        }
    }

    // MARK: - Selection and deletion

    private func toggleSelection(_ id: String) {
        if selected.contains(id) {
            selected.remove(id)
        } else {
            selected.insert(id)
        }
    }

    private func endSelection() {
        selecting = false
        selected = []
    }

    private var deleteTitle: LocalizedStringKey {
        if deleteRequest?.books.count == 1 { return "library.delete.single.title" }
        return "library.delete.multiple.title"
    }

    private func deleteMessage(_ request: DeleteRequest) -> Text {
        if request.books.count == 1 {
            return Text(verbatim: L10n.format("library.delete.single.message", request.books[0].title))
        }
        return Text(verbatim: L10n.format("library.delete.multiple.message", request.books.count))
    }

    private func delete(_ request: DeleteRequest) {
        let ids = Set(request.books.map(\.id))
        let removed = library.deleteBooks(ids: ids)
        for id in removed {
            progress.remove(bookID: id)
        }
        deleteRequest = nil
        endSelection()
    }

    private func errorBinding(_ keyPath: ReferenceWritableKeyPath<LibraryStore, String?>) -> Binding<Bool> {
        Binding(
            get: { library[keyPath: keyPath] != nil },
            set: { if !$0 { library[keyPath: keyPath] = nil } }
        )
    }

    // MARK: - Empty state

    private var emptyState: some View {
        VStack(spacing: 14) {
            Image(systemName: "books.vertical")
                .font(.system(size: 44))
                .foregroundStyle(palette.swiftInkSoft)
            Text("library.empty.title")
                .font(.headline)
                .foregroundStyle(palette.swiftInk)
            Text("library.empty.body")
                .font(.footnote)
                .foregroundStyle(palette.swiftInkSoft)
                .multilineTextAlignment(.center)
        }
        .padding(40)
        .frame(maxWidth: .infinity)
    }

    // MARK: - Demo hooks

    /// Drives the shelf's palette from launch arguments, so a capture run can
    /// photograph the library before and after the look changes — on the shelf
    /// itself, and without a relaunch:
    /// `-demoLibraryTheme focus -demoLibraryDark 1`.
    private func applyLibraryDemoHooks() {
        let defaults = UserDefaults.standard
        if let layoutID = defaults.string(forKey: "demoLibraryLayout"),
           let requested = LibraryLayout(rawValue: layoutID) {
            layout = requested
        }
        if let sortID = defaults.string(forKey: "demoLibrarySort"),
           let requested = LibrarySort(rawValue: sortID) {
            sort = requested
        }
        if defaults.bool(forKey: "demoLibrarySelectAll") {
            selecting = true
            selected = Set(library.books.map(\.id))
        }
        // Capture hook for the title-on-cover check: rename one book and watch
        // its generated gradient and printed title follow the metadata.
        let rename = defaults.integer(forKey: "demoRenameBook")
        if rename > 0,
           library.books.count >= rename,
           let title = defaults.string(forKey: "demoRenameTitle"),
           !title.isEmpty {
            let book = library.books[rename - 1]
            library.updateMetadata(id: book.id, title: title, author: book.author)
        }
    }

    private static var epubType: UTType {
        UTType("org.idpf.epub-container")
            ?? UTType(filenameExtension: "epub")
            ?? .data
    }
}

/// A deletion waiting for the reader to confirm it: one book, or the selection.
private struct DeleteRequest: Identifiable {
    let id = UUID()
    let books: [BookItem]
}

/// How far this book has been read. The bar is gone: the shelf shows the figure
/// itself, which is the part the reader asked to keep.
struct ReadPercentLabel: View {

    let fraction: Double
    let palette: ReaderPalette

    private var percent: Int { Int((min(max(fraction, 0), 1) * 100).rounded()) }

    var body: some View {
        Text(verbatim: "\(percent)%")
            .font(.system(size: 11, weight: .semibold, design: .rounded))
            .monospacedDigit()
            .foregroundStyle(palette.swiftInkSoft)
            .fixedSize()
            .accessibilityLabel(Text(verbatim: L10n.format("library.percent.a11y", percent)))
    }
}

/// The one cover every screen shares: the book's own image when it has a usable
/// one, otherwise the title-driven gradient with the title and author printed
/// inside it.
struct BookCover: View {

    let book: BookItem

    @ObservedObject private var covers = BookCoverStore.shared
    @State private var image: UIImage?

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
        ZStack {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                LinearGradient(
                    colors: palette,
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                generatedText
            }
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
        // The item around the cover carries the title and author for VoiceOver;
        // reading the printed cover as well would say them twice.
        .accessibilityHidden(true)
        .task(id: book.coverKey) {
            image = nil
            image = await covers.image(for: book)
        }
    }

    /// The title and author printed on a generated cover. Their size follows the
    /// cover so a 58 pt shelf thumbnail and a 96 pt shelf cover both read.
    private var generatedText: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let titleSize = max(9, min(14, width * 0.13))
            let authorSize = max(7, min(10, width * 0.095))

            VStack(spacing: 4) {
                Text(verbatim: book.title)
                    .font(.system(size: titleSize, weight: .bold))
                    .lineLimit(4)
                    .truncationMode(.tail)

                if !book.author.isEmpty {
                    Text(verbatim: book.author)
                        .font(.system(size: authorSize))
                        .opacity(0.78)
                        .lineLimit(2)
                        .truncationMode(.tail)
                }
            }
            .foregroundStyle(.white)
            .multilineTextAlignment(.center)
            // Clear of the spine strip on the left and the hairline all round.
            .padding(.leading, 10)
            .padding(.trailing, 8)
            .padding(.vertical, 10)
            .frame(
                width: geometry.size.width,
                height: geometry.size.height,
                alignment: .center
            )
        }
    }

    private var palette: [Color] { Self.palette(for: book.title) }
}
