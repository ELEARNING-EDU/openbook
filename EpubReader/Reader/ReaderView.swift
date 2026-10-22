import SwiftUI

struct ReaderView: View {

    let book: BookItem

    @StateObject private var model = ReaderViewModel()
    @StateObject private var speech = SpeechController()
    /// The app-wide reading appearance: the library is painted from the same
    /// object, so the shelf and the page never disagree about the palette.
    @ObservedObject private var settings = ReaderSettings.shared
    @StateObject private var bookmarks = BookmarkStore()
    /// The place in this book, remembered across visits and shared with the
    /// library's "how far have I read" figure.
    @ObservedObject private var progress = ReadingProgressStore.shared
    @State private var pageIndex = 0
    /// The remembered place is put back once, when the first pagination lands.
    @State private var restoredPosition = false
    /// Bumped on every turn so a burst of them only writes once.
    @State private var saveToken = 0
    @State private var showTOC = false
    @State private var showSettings = false
    /// Bumped for every tap-to-turn, so the same page can be asked for twice.
    @State private var turnCount = 0
    @State private var turnRequest: PageTurnRequest?
    @State private var showSearch = false
    @State private var searchQuery = ""
    /// Which half of the contents sheet is showing: the book's own outline, or the
    /// places the reader has marked.
    @State private var tocSection = 0
    @State private var demoApplied = false
    /// The synthetic tap is armed late so it lands on the demo page, not page 1.
    @State private var demoTapArmed = false
    /// The player unfurls to the right of the bottom-left button.
    @State private var showPlayer = false
    /// How much chrome is on screen. While the voice is running the buttons step
    /// aside so the page is just paper — but the back button stays, because a
    /// reader must never be locked inside a book they can only hear.
    @State private var chrome: ChromeMode = .full
    /// Bumped on every interaction, so a stale auto-hide timer can tell it is stale.
    @State private var autoHideToken = 0
    /// Same trick for the re-typeset, which is far too slow to run per tap.
    @State private var restyleToken = 0
    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase

    private enum ChromeMode: String {
        /// Everything: toolbar, controls, progress.
        case full
        /// The voice's turn: the back button and the player, nothing else.
        case playerOnly
        /// Just the page.
        case hidden
    }

    private var chromeVisible: Bool { chrome != .hidden }

    /// How long the chrome stays put after it is shown.
    private static let autoHideDelay: TimeInterval = 4.5

    /// The look the reader is set in, threaded through every piece of chrome so the
    /// floating buttons never disagree with the paper under them.
    private var palette: ReaderPalette { settings.palette }

    /// The page on screen, when there is one.
    private var currentPage: PageContent? {
        model.pages.indices.contains(pageIndex) ? model.pages[pageIndex] : nil
    }

    private var isPageMarked: Bool {
        guard let page = currentPage else { return false }
        return bookmarks.isMarked(chapterIndex: page.chapterIndex, range: page.range)
    }

    var body: some View {
        GeometryReader { geo in
            // The pages stretch edge to edge, so pagination is computed against the
            // full screen rather than the safe area.
            let full = CGSize(
                width: geo.size.width,
                height: geo.size.height + geo.safeAreaInsets.top + geo.safeAreaInsets.bottom
            )
            let safeTop = geo.safeAreaInsets.top

            ZStack(alignment: .top) {
                palette.swiftPaper.ignoresSafeArea()

                if !model.pages.isEmpty {
                    ZStack(alignment: .bottom) {
                        PageCurlPager(
                            pages: model.pages,
                            safeTop: safeTop,
                            palette: palette,
                            margin: CGFloat(settings.applied.margin),
                            revision: model.pagesRevision,
                            index: $pageIndex,
                            highlight: speech.highlighted,
                            highlightChapter: speech.highlighted == nil ? nil : speech.currentChapter,
                            onTap: pageTapped,
                            turn: turnRequest,
                            demoTapPoint: demoTapArmed ? demoTapPoint : nil,
                            demoFold: demoFold,
                            demoDrag: demoDrag
                        )

                        // Floats above the paper, so it stays put while pages curl.
                        bottomBar
                            .padding(.horizontal, ReaderMetrics.hMargin)
                            .padding(.bottom, ReaderMetrics.controlsBottomInset)
                            .opacity(chromeVisible ? 1 : 0)
                            .allowsHitTesting(chromeVisible)
                    }
                    .ignoresSafeArea()
                }

                if model.isLoading {
                    loadingState
                }

                if let message = model.errorMessage {
                    errorState(message)
                }

                progressLine
                    .opacity(chromeVisible ? 1 : 0)

                toolbar
                    .padding(.top, ReaderMetrics.topButtonGap)
                    .padding(.horizontal, ReaderMetrics.hMargin)

                if UserDefaults.standard.bool(forKey: "demoDebug") {
                    VStack(alignment: .leading, spacing: 0) {
                        Spacer()
                        Text(
                            "size \(Int(geo.size.width))x\(Int(geo.size.height))\n"
                            + "text \(Int(ReaderMetrics.textRect(in: full, safeTop: safeTop).height)) tall\n"
                            + "safeTop \(Int(geo.safeAreaInsets.top)) safeBottom \(Int(geo.safeAreaInsets.bottom))\n"
                            + "full \(Int(full.width))x\(Int(full.height))\n"
                            + "pages \(model.pages.count) index \(pageIndex)\n"
                            + "state \(stateLabel) rate \(speech.rateLabel) player \(showPlayer)\n"
                            + "chrome \(chrome.rawValue) body \(Int(settings.custom.bodySize)) lead \(settings.custom.lineSpacingLabel)\n"
                            + "theme \(settings.themeID) dark \(settings.isDarkAppearance) "
                            + "col \(settings.custom.columns) margin \(Int(settings.custom.margin)) "
                            + "mark \(settings.isCustomised ? "yes" : "no")\n"
                            + "chapter \(speech.currentChapter)\n"
                            + "voice \(speech.voiceName)\n"
                            + "spoken "
                            + (speech.highlighted.map { "\($0.location)+\($0.length)" } ?? "nil")
                        )
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundStyle(.white)
                        .padding(6)
                        .background(.black.opacity(0.75), in: RoundedRectangle(cornerRadius: 6))
                        .padding(8)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    // Clear of the audio control that now sits in the corner.
                    .padding(.bottom, 74)
                }
            }
            .onAppear {
                bookmarks.open(book: book.url)
                model.load(
                    url: book.url,
                    style: settings.applied,
                    palette: palette
                )
                model.updatePagination(for: full, safeTop: safeTop, style: settings.applied)
                scheduleAutoHide()
            }
            // A theme or a setting is a re-typeset, not a repaint: the ink, the
            // face, the leading and the margins are all baked into the attributed
            // string, so the paper cannot simply draw over the top.
            .onChange(of: settings.applied) { _, _ in scheduleRestyle() }
            .onChange(of: settings.themeID) { _, _ in scheduleRestyle() }
            .onChange(of: full) { _, newValue in
                model.updatePagination(for: newValue, safeTop: safeTop, style: settings.applied)
            }
            .onChange(of: safeTop) { _, newValue in
                // The safe-area inset reads as 0 on the very first layout pass, so
                // the real value only shows up here a moment later.
                model.updatePagination(for: full, safeTop: newValue, style: settings.applied)
            }
            .onChange(of: model.pages.count) { _, count in
                // Chapters exist only after the book has been parsed and paginated.
                speech.configure(chapters: model.allChapters())
                resumeReading(pageCount: count)
                applyDemoHooks(pageCount: count)
                applyNarrationDemoHook()
            }
            .onDisappear {
                // A page turn waits a moment before it is written; leaving the
                // book must not lose the last of them.
                saveToken += 1
                savePosition()
            }
            .onChange(of: scenePhase) { _, phase in
                guard phase != .active else { return }
                savePosition()
            }
            .onChange(of: speech.highlighted) { _, newValue in
                // The voice drives the page — but only while it is actually
                // speaking: paused, the reader is the one holding the book, and a
                // late callback must not turn their page for them.
                guard speech.state == .playing, let spoken = newValue else { return }
                if let target = model.pageIndex(
                    forChapterOffset: spoken.location,
                    chapter: speech.currentChapter
                ), target != pageIndex {
                    pageIndex = target
                }
            }
            .onChange(of: pageIndex) { _, newIndex in
                scheduleSavePosition()
                guard speech.state == .playing, model.pages.indices.contains(newIndex) else { return }
                let page = model.pages[newIndex]
                // Filter out the page changes the voice itself just caused: those
                // land on the page that already holds the spoken sentence.
                if let spoken = speech.highlighted,
                   page.chapterIndex == speech.currentChapter,
                   spoken.location >= page.range.location,
                   spoken.location < page.range.location + page.range.length {
                    return
                }
                speak(page: page)
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showTOC) { tocSheet }
        .sheet(isPresented: $showSettings) { settingsSheet }
        .sheet(isPresented: $showSearch, onDismiss: { scheduleAutoHide() }) { searchSheet }
    }

    // MARK: - Reader chrome
    //
    // The controls fade out so the page is just paper. Tapping shows them again.
    // While the player is open — or a sheet is up — they stay put, because the
    // reader is plainly in the middle of something.

    private func revealChrome() {
        withAnimation(.easeInOut(duration: 0.25)) { chrome = .full }
        scheduleAutoHide()
    }

    /// Steps the chrome down. The voice gets its own stop: with narration running
    /// the page keeps its way out of the book and the player, and lets the rest go.
    private func hideChrome() {
        autoHideToken += 1
        withAnimation(.easeInOut(duration: 0.25)) { chrome = restingChrome }
    }

    private var restingChrome: ChromeMode {
        speech.state == .idle ? .hidden : .playerOnly
    }

    private func scheduleAutoHide() {
        autoHideToken += 1
        let token = autoHideToken
        guard !showTOC, !showSettings else { return }
        guard !UserDefaults.standard.bool(forKey: "demoNoAutoHide") else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + Self.autoHideDelay) {
            // A later interaction bumped the token, so this timer is stale.
            guard token == autoHideToken, !showTOC, !showSettings else { return }
            withAnimation(.easeInOut(duration: 0.3)) { chrome = restingChrome }
        }
    }

    /// What a tap on the paper means.
    ///
    /// While the voice is reading, the text is the narration's own navigation
    /// surface, so a tap jumps the reading to that word. Otherwise — with the
    /// voice closed — the page behaves like a book held open: the two sides turn
    /// the page, and the middle shows and hides the controls.
    private func pageTapped(_ tap: PageTap) {
        let fraction = tap.point.x / max(tap.width, 1)

        if speech.state != .idle {
            // The sides are not the book's while the voice is reading: there a tap
            // is only ever a way of asking for the controls back. The middle stays
            // the narration's own surface, where a tap moves the reading.
            if fraction < 0.3 || fraction > 0.7 {
                revealChrome()
                scheduleAutoHide()
                return
            }
            if let offset = tap.offset {
                if speech.state == .paused {
                    // A paused voice that is handed a sentence reads it: the tap
                    // starts the narration again from the word under the finger.
                    speak(fromChapterOffset: offset)
                    scheduleAutoHide()
                } else {
                    pageTappedOnText(offset)
                }
                return
            }
            // Blank paper is not the voice's: it only ever asks for the controls.
            if chrome == .full {
                hideChrome()
            } else {
                revealChrome()
            }
            return
        }

        if fraction < 0.3 {
            turnPage(-1)
        } else if fraction > 0.7 {
            turnPage(1)
        } else if chrome == .hidden {
            revealChrome()
        } else {
            hideChrome()
        }
    }

    /// Turns one page, with the curl played by the pager: a tap should not cut to
    /// the next page when the rest of the book turns under a hand.
    private func turnPage(_ delta: Int) {
        let target = pageIndex + delta
        guard model.pages.indices.contains(target) else { return }
        // The page is the reader's, not the pager's: the tap sets it here and the
        // pager is asked to play the curl towards it.
        pageIndex = target
        turnCount += 1
        turnRequest = PageTurnRequest(id: turnCount, target: target)
        scheduleAutoHide()
    }

    /// A tap on a sentence while the voice is reading: it moves the narration
    /// there, and takes the controls with it.
    private func pageTappedOnText(_ offset: Int) {
        guard chrome != .hidden else {
            revealChrome()
            return
        }
        speak(fromChapterOffset: offset)
        scheduleAutoHide()
    }

    // MARK: - Demo / automation hooks

    /// Lets the harness jump to a page and trigger an animated page turn without
    /// a touch event:
    /// `-demoPage 12` and `-demoTurn 2.0` (seconds after the pages are ready).
    private func applyDemoHooks(pageCount: Int) {
        guard pageCount > 0, !demoApplied else { return }
        demoApplied = true

        if UserDefaults.standard.bool(forKey: "demoDebug") {
            for (index, page) in model.pages.enumerated() {
                NSLog(
                    "[map] page \(index + 1) chapter \(page.chapterIndex) "
                    + "range \(page.range.location)+\(page.range.length)"
                )
            }
        }

        let defaults = UserDefaults.standard
        let target = defaults.integer(forKey: "demoPage")
        if target > 0 {
            pageIndex = min(target - 1, pageCount - 1)
        }

        if defaults.bool(forKey: "demoTOC") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { showTOC = true }
        }

        if defaults.bool(forKey: "demoSettings") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { showSettings = true }
        }

        // Closes the settings panel from the harness, which cannot tap the Xong pill.
        let closeSettings = defaults.double(forKey: "demoCloseSettings")
        if closeSettings > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5 + closeSettings) {
                showSettings = false
            }
        }

        // Walks the appearance toggle and the theme grid, so the panel's own
        // controls are proven rather than only photographed.
        let themeSteps = defaults.integer(forKey: "demoThemeStep")
        if themeSteps != 0, let start = ReaderTheme.themes.firstIndex(where: { $0.id == settings.themeID }) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.4) {
                let last = ReaderTheme.themes.count - 1
                let target = min(max(start + themeSteps, 0), last)
                settings.setTheme(ReaderTheme.themes[target].id)
            }
        }

        let themeID = defaults.string(forKey: "demoTheme")
        if let themeID, ReaderTheme.themes.contains(where: { $0.id == themeID }) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.4) { settings.setTheme(themeID) }
        }

        if defaults.bool(forKey: "demoAppearance") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.4) { settings.toggleAppearance() }
        }

        // Pins the appearance instead of toggling it, so a capture run does not
        // depend on which face the last run left the reader wearing.
        if defaults.object(forKey: "demoDark") != nil {
            let dark = defaults.bool(forKey: "demoDark")
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.4) {
                settings.setAppearance(dark: dark)
            }
        }

        let openSearch = defaults.bool(forKey: "demoSearch")
        if openSearch {
            if let text = defaults.string(forKey: "demoSearchQuery") { searchQuery = text }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { showSearch = true }
        }

        // Takes the first result, so the jump is proven and not only the list.
        let pickDelay = defaults.double(forKey: "demoSearchPick")
        if pickDelay > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5 + pickDelay) {
                if let first = model.search(searchQuery).first { jump(to: first) }
            }
        }

        // Marks the page the reader is on, then opens the marks list.
        let markAfter = defaults.double(forKey: "demoMark")
        if markAfter > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + markAfter) { toggleBookmark() }
        }

        if defaults.bool(forKey: "demoMarks") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                tocSection = 1
                showTOC = true
            }
        }

        if defaults.bool(forKey: "demoTocSection1") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { tocSection = 1 }
        }

        // Puts the reader back at an exact type size, so a captured run does not
        // inherit whatever the last one left behind.
        let exactSize = defaults.double(forKey: "demoBodySize")
        if exactSize > 0, ReaderCustom.sizeOptions.contains(exactSize) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                settings.update(inCustomise: false) { $0.bodySize = exactSize }
            }
        }

        applyCustomizeDemoHooks()

        // Exercises the settings rows, so the harness can prove they took effect.
        let sizeSteps = defaults.integer(forKey: "demoSizeStep")
        if sizeSteps != 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                for _ in 0..<abs(sizeSteps) {
                    sizeSteps > 0 ? settings.growText() : settings.shrinkText()
                }
            }
        }

        let spacingSteps = defaults.integer(forKey: "demoSpacingStep")
        if spacingSteps != 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                settings.update { custom in
                    let step = 0.06 * Double(spacingSteps)
                    custom.lineSpacing = min(max(custom.lineSpacing + step, 1.0), 2.0)
                    custom.enabled = true
                }
            }
        }

        // Hides the controls on demand, so the hidden state can be photographed.
        let hideAfter = defaults.double(forKey: "demoHideChrome")
        if hideAfter > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + hideAfter) { hideChrome() }
        }

        // Turns a page every `demoTurnEvery` seconds, `demoTurnCount` times, so a
        // screenshot loop is guaranteed to land inside a curl animation.
        let every = defaults.double(forKey: "demoTurnEvery")
        let count = defaults.integer(forKey: "demoTurnCount")
        if every > 0, count > 0 {
            for step in 1...count {
                DispatchQueue.main.asyncAfter(deadline: .now() + every * Double(step)) {
                    if pageIndex + 1 < pageCount { pageIndex += 1 }
                }
            }
        }

        let delay = defaults.double(forKey: "demoTurn")
        guard delay > 0 else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            if pageIndex + 1 < pageCount { pageIndex += 1 }
        }
    }

    /// Drives the customise screen from launch arguments, because the simulator
    /// cannot be dragged: every slider, the font list, the columns menu and the
    /// reset button have to be provable without a finger.
    private func applyCustomizeDemoHooks() {
        let defaults = UserDefaults.standard
        let after = 2.6

        if let font = defaults.string(forKey: "demoFont"),
           ReaderFonts.all.contains(where: { $0.id == font }) {
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { $0.fontID = font }
            }
        }

        let lines = defaults.double(forKey: "demoLineSpacing")
        if lines > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { custom in
                    custom.lineSpacing = min(max(lines, ReaderCustom.lineSpacingRange.lowerBound),
                                             ReaderCustom.lineSpacingRange.upperBound)
                    custom.enabled = true
                }
            }
        }

        if defaults.object(forKey: "demoCharacterSpacing") != nil {
            let value = defaults.double(forKey: "demoCharacterSpacing")
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { custom in
                    custom.characterSpacing = value
                    custom.enabled = true
                }
            }
        }

        if defaults.object(forKey: "demoWordSpacing") != nil {
            let value = defaults.double(forKey: "demoWordSpacing")
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { custom in
                    custom.wordSpacing = value
                    custom.enabled = true
                }
            }
        }

        if defaults.object(forKey: "demoMargin") != nil {
            let value = defaults.double(forKey: "demoMargin")
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { custom in
                    custom.margin = value
                    custom.enabled = true
                }
            }
        }

        if let columns = defaults.string(forKey: "demoColumns"),
           ColumnSetting(rawValue: columns) != nil {
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { $0.columns = columns }
            }
        }

        if defaults.bool(forKey: "demoJustify") {
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { custom in
                    custom.justify = true
                    custom.enabled = true
                }
            }
        }

        if defaults.bool(forKey: "demoBold") {
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { $0.bold = true }
            }
        }

        if defaults.bool(forKey: "demoCustomEnabled") {
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { $0.enabled = true }
            }
        }

        if defaults.bool(forKey: "demoCustomizeToggle") {
            // Proves the Customize switch itself enables the sliders.
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { $0.enabled.toggle() }
            }
        }

        if let columns = defaults.string(forKey: "demoColumns"),
           ColumnSetting(rawValue: columns) != nil {
            // One column is twice as wide as two: the width a picture is cut to.
            DispatchQueue.main.asyncAfter(deadline: .now() + after) {
                settings.update { custom in
                    custom.columns = columns
                    custom.enabled = true
                }
            }
        }

        if defaults.bool(forKey: "demoResetTheme") {
            // Before the hooks below, so each captured run starts from the theme as
            // designed and only the setting under test is off its default.
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                settings.resetCurrentTheme()
            }
        }

        if defaults.bool(forKey: "demoCustomize") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { showSettings = true }
        }
    }

    /// Narration needs the jump above to have settled first, so it runs on a timer
    /// and only once the pager really shows the requested page.
    private func applyNarrationDemoHook() {
        let defaults = UserDefaults.standard
        if defaults.object(forKey: "demoTapY") != nil {
            // Delayed on request, so a tap can be aimed at a moment when the
            // controls have already hidden themselves.
            let after = defaults.object(forKey: "demoTapAfter") != nil
                ? defaults.double(forKey: "demoTapAfter")
                : 1.2
            DispatchQueue.main.asyncAfter(deadline: .now() + after) { demoTapArmed = true }
        }
        guard defaults.bool(forKey: "demoSpeak") else { return }

        let rate = defaults.double(forKey: "demoRate")
        if rate > 0 { speech.setRate(rate) }

        // Late enough to start after any restyle the same run asks for, because a
        // re-typeset stops narration.
        let speakAfter = defaults.object(forKey: "demoSpeakAfter") != nil
            ? defaults.double(forKey: "demoSpeakAfter")
            : 1.2
        DispatchQueue.main.asyncAfter(deadline: .now() + speakAfter) { audioButtonTapped() }

        // Pressing the player's stop must end narration rather than jump chapters.
        let stopAfter = defaults.double(forKey: "demoStop")
        if stopAfter > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2 + stopAfter) { stopNarration() }
        }

        // Tapping the disc again unfurls / collapses the player.
        let togglePlayer = defaults.double(forKey: "demoTogglePlayer")
        if togglePlayer > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2 + togglePlayer) { audioButtonTapped() }
        }

        // Presses the player's play/pause once, to prove the control works.
        let pauseAfter = defaults.double(forKey: "demoPause")
        if pauseAfter > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2 + pauseAfter) { speech.togglePlayPause() }
        }

        // Presses the player's "+" speed button, to prove that control works.
        let fasterSteps = defaults.integer(forKey: "demoFaster")
        if fasterSteps > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2 + 3) {
                for _ in 0..<fasterSteps { speech.faster() }
            }
        }
    }

    private var stateLabel: String {
        switch speech.state {
        case .idle: return "idle"
        case .playing: return "playing"
        case .paused: return "paused"
        }
    }

    // MARK: - Narration

    /// Bottom-left control: headphones when silent, the book spinning like a record
    /// while it reads. Tapping it starts narration, or unfurls the player.
    private var bottomBar: some View {
        HStack(alignment: .center, spacing: 10) {
            Button(action: audioButtonTapped) {
                ZStack {
                    Circle().fill(palette.controlFill)
                    if speech.state == .idle {
                        Image(systemName: "headphones")
                            .font(.system(size: ReaderMetrics.buttonIconSize, weight: .semibold))
                            .foregroundStyle(palette.swiftInk)
                    } else {
                        VinylCover(
                            title: book.title,
                            spinning: speech.state == .playing,
                            size: ReaderMetrics.buttonSize * 0.82
                        )
                    }
                }
                .frame(width: ReaderMetrics.buttonSize, height: ReaderMetrics.buttonSize)
                .overlay(Circle().strokeBorder(palette.swiftInk.opacity(0.14)))
                .shadow(color: .black.opacity(0.20), radius: 7, y: 3)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(audioButtonLabel)

            if playerUnfurled {
                playerControls
                    // It unfurls from the audio button the way a scroll does, and
                    // rolls back into it.
                    .transition(
                        .asymmetric(
                            insertion: .scale(scale: 0.55, anchor: .leading)
                                .combined(with: .opacity),
                            removal: .scale(scale: 0.75, anchor: .leading)
                                .combined(with: .opacity)
                        )
                    )
            }

            Spacer(minLength: 8)

            // Bottom-right: the reader's own settings, not the book's. It steps
            // aside while the player is open, which is where its width goes.
            if chrome == .full {
                circleButton(systemName: "gearshape", label: "Cài đặt đọc") {
                    showSettings = true
                    scheduleAutoHide()
                }
                .transition(.opacity)
            }
        }
        // Pinned to the band the text was paginated against, so the controls can
        // never creep down over the page number or the home indicator.
        .frame(height: ReaderMetrics.controlBand)
        .animation(playerAnimation, value: chrome)
        .animation(playerAnimation, value: speech.state)
        .animation(playerAnimation, value: showPlayer)
    }

    /// Whether the player is out. It keeps the voice's corner to itself, and gives
    /// it up while the toolbar is showing: the buttons are what was asked for, and
    /// the player sits where the settings button goes. The moment the chrome steps
    /// back to the voice's own state, the player is out again — it was never put
    /// away, only stood aside.
    private var playerUnfurled: Bool {
        showPlayer && speech.state != .idle
    }

    /// While the whole toolbar is up, the player keeps its place in the row by
    /// taking its speed label and the two speed buttons away, so the settings
    /// button has room beside it. The full player comes back with the toolbar.
    private var playerIsCompact: Bool { chrome == .full }

    private var audioButtonLabel: String {
        switch speech.state {
        case .idle: return "Đọc to trang này"
        case .playing: return "Mở trình phát đang đọc"
        case .paused: return "Mở trình phát đang tạm dừng"
        }
    }

    /// Which chapter the player should name: the one being read, or — if that is
    /// somehow unknown — the one on screen.
    private var playingChapterTitle: String {
        if speech.state != .idle { return model.chapterTitle(at: speech.currentChapter) }
        guard model.pages.indices.contains(pageIndex) else { return "" }
        return model.chapterTitle(at: model.pages[pageIndex].chapterIndex)
    }

    /// Title and chapter, run together so the scrolling strip can carry both.
    private var playerHeadline: String {
        let chapter = playingChapterTitle
        return chapter.isEmpty ? book.title : "\(book.title) · \(chapter)"
    }

    /// The unfurled player, a single row: what is being read, how fast it is being
    /// read, then the controls. The title is legitimately long, so it scrolls
    /// instead of spending the row's width on an ellipsis.
    private var playerControls: some View {
        HStack(spacing: 8) {
            MarqueeText(
                text: playerHeadline,
                font: .systemFont(ofSize: 13, weight: .semibold),
                color: palette.swiftInk
            )
            .frame(maxWidth: .infinity)

            if !playerIsCompact {
                Text(speech.rateLabel)
                    .font(.system(size: 11, weight: .semibold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(palette.swiftInkSoft)
                    .fixedSize()
            }

            HStack(spacing: 0) {
                if !playerIsCompact {
                    panelButton("gobackward", enabled: speech.canSlowDown) { speech.slower() }
                }
                panelButton(
                    speech.state == .playing ? "pause.fill" : "play.fill"
                ) { speech.togglePlayPause() }
                if !playerIsCompact {
                    panelButton("goforward", enabled: speech.canSpeedUp) { speech.faster() }
                }
                panelButton("xmark") { stopNarration() }
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 2)
        .frame(maxWidth: .infinity)
        .background(
            palette.controlFill,
            in: RoundedRectangle(cornerRadius: ReaderMetrics.controlBand / 2, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: ReaderMetrics.controlBand / 2, style: .continuous)
                .strokeBorder(palette.swiftInk.opacity(0.14))
        )
        // The voice's progress, a thin line inside the player: it runs the width
        // of the row and moves as the narration reads. It sits on the player's
        // bottom rim and is clipped to the capsule, so its ends follow the curve
        // instead of floating a few points above the edge.
        .overlay(alignment: .bottom) { audioProgressLine }
        // The whole player is cut to its own capsule, so the progress line at the
        // bottom edge follows the rounded ends instead of standing out past them.
        .clipShape(RoundedRectangle(cornerRadius: ReaderMetrics.controlBand / 2, style: .continuous))
        .shadow(color: .black.opacity(0.18), radius: 8, y: 3)
    }

    private var narrationFraction: Double {
        guard let spoken = speech.highlighted else { return 0 }
        return model.narrationProgress(chapter: speech.currentChapter, offset: spoken.location)
    }

    private var audioProgressLine: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(palette.swiftInk.opacity(0.22))
                Capsule()
                    .fill(palette.swiftAccent)
                    .frame(width: max(geo.size.width * narrationFraction, 2))
            }
        }
        .frame(height: 3)
        .padding(.bottom, 0.5)
        .allowsHitTesting(false)
    }

    private func panelButton(
        _ systemName: String,
        enabled: Bool = true,
        width: CGFloat = ReaderMetrics.panelButtonSize,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: ReaderMetrics.buttonIconSize, weight: .semibold))
                .foregroundStyle(palette.swiftInk)
                .frame(width: width, height: ReaderMetrics.buttonSize)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.3)
    }

    private var playerAnimation: Animation {
        .spring(response: 0.32, dampingFraction: 0.78)
    }

    private func audioButtonTapped() {
        if speech.state == .idle {
            guard model.pages.indices.contains(pageIndex) else { return }
            speak(page: model.pages[pageIndex])
            withAnimation(playerAnimation) { showPlayer = true }
        } else {
            withAnimation(playerAnimation) { showPlayer.toggle() }
        }
        scheduleAutoHide()
    }

    private func stopNarration() {
        speech.stop()
        withAnimation(playerAnimation) { showPlayer = false }
        scheduleAutoHide()
    }

    // MARK: - Where the reading stopped

    /// Opens the book where it was left, once, as soon as it has pages.
    ///
    /// The remembered place is a chapter and a character offset, so it has to be
    /// turned back into a page against *this* typesetting — the reader may well
    /// have changed the type size since.
    private func resumeReading(pageCount: Int) {
        guard !restoredPosition, pageCount > 0 else { return }
        restoredPosition = true
        guard let place = progress.position(for: book.id) else { return }
        if let target = model.pageIndex(forChapterOffset: place.offset, chapter: place.chapter) {
            pageIndex = target
        } else if model.pages.indices.contains(place.page) {
            pageIndex = min(place.page, pageCount - 1)
        }
    }

    /// Writes the place after the page has stopped moving, so a quick run of turns
    /// is one write rather than five.
    private func scheduleSavePosition() {
        saveToken += 1
        let token = saveToken
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            guard token == saveToken else { return }
            savePosition()
        }
    }

    /// The place is the *top* of the page on screen — that is where the reader is —
    /// but how far they have read is measured at the foot of it, so the last page
    /// of a book reads as 100% rather than as "nearly".
    private func savePosition() {
        guard model.pages.indices.contains(pageIndex) else { return }
        let page = model.pages[pageIndex]
        let end = page.range.location + page.range.length
        progress.mark(
            bookID: book.id,
            chapter: page.chapterIndex,
            offset: page.range.location,
            fraction: model.narrationProgress(chapter: page.chapterIndex, offset: end),
            page: pageIndex,
            pageCount: model.pages.count
        )
    }

    /// Re-typesetting re-imports the HTML of every chapter, which is slow enough
    /// that holding down a size button must not run it once per press.
    private func scheduleRestyle() {
        restyleToken += 1
        let token = restyleToken
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            guard token == restyleToken else { return }
            restyleReader()
        }
    }

    /// Re-typesets at the new size / leading and keeps the reader where they were.
    ///
    /// Page numbers do not survive a re-typeset, so the position is held as a
    /// chapter plus a character offset and translated back into a page.
    private func restyleReader() {
        var anchor: (chapter: Int, offset: Int)?
        if model.pages.indices.contains(pageIndex) {
            let page = model.pages[pageIndex]
            anchor = (page.chapterIndex, page.range.location)
        }

        // The voice's sentence offsets belong to the old typesetting.
        if speech.state != .idle {
            speech.stop()
            withAnimation(playerAnimation) { showPlayer = false }
        }

        model.restyle(style: settings.applied, palette: palette)
        speech.configure(chapters: model.allChapters())

        if let anchor,
           let target = model.pageIndex(forChapterOffset: anchor.offset, chapter: anchor.chapter) {
            pageIndex = target
            if UserDefaults.standard.bool(forKey: "demoDebug") {
                NSLog("[style] \(model.pages.count) pages, page \(pageIndex) for anchor \(anchor)")
            }
        } else if pageIndex >= model.pages.count {
            pageIndex = max(0, model.pages.count - 1)
        }
        scheduleAutoHide()
    }

    /// Synthetic tap in the page's text-view coordinates. The screenshot harness
    /// uses this because the simulator cannot be tapped programmatically.
    private var demoTapPoint: CGPoint? {
        let defaults = UserDefaults.standard
        guard defaults.object(forKey: "demoTapY") != nil else { return nil }
        return CGPoint(
            x: defaults.double(forKey: "demoTapX"),
            y: defaults.double(forKey: "demoTapY")
        )
    }

    /// A fold held open for the screenshot harness, so the curl can be looked at
    /// without a hand on the glass: `-demoCurl 0.45 [-demoCurlBack 1]`.
    private var demoFold: DemoFold? {
        let arguments = ProcessInfo.processInfo.arguments
        guard let key = arguments.firstIndex(of: "-demoCurl"), key + 1 < arguments.count,
              let pull = Double(arguments[key + 1]) else { return nil }
        let back = value(after: "-demoCurlBack", in: arguments).flatMap(Double.init) ?? 0
        return DemoFold(pull: pull, back: back != 0)
    }

    private func value(after key: String, in arguments: [String]) -> String? {
        guard let index = arguments.firstIndex(of: key), index + 1 < arguments.count else { return nil }
        return arguments[index + 1]
    }

    /// A pull the harness plays back through the drag code, so the curl can be
    /// watched instead of photographed:
    /// `-demoDragX -260 -demoDragY 90 [-demoDragFromX 380 -demoDragFromY 120]
    /// [-demoDragSeconds 1.2] [-demoDragHold 0.5]`.
    /// `-demoDragHold` stops part way and leaves the fold standing, which is how a
    /// diagonal fold gets photographed without letting go of it.
    private var demoDrag: DemoDrag? {
        let arguments = ProcessInfo.processInfo.arguments
        guard let dx = value(after: "-demoDragX", in: arguments).flatMap(Double.init) else { return nil }
        let dy = value(after: "-demoDragY", in: arguments).flatMap(Double.init) ?? 0
        let seconds = value(after: "-demoDragSeconds", in: arguments).flatMap(Double.init) ?? 1.2
        let fromX = value(after: "-demoDragFromX", in: arguments).flatMap(Double.init)
        let fromY = value(after: "-demoDragFromY", in: arguments).flatMap(Double.init) ?? 0
        let from = fromX.map { CGPoint(x: $0, y: fromY) }
        let hold = value(after: "-demoDragHold", in: arguments).flatMap(Double.init)
        return DemoDrag(dx: CGFloat(dx), dy: CGFloat(dy), seconds: seconds, from: from, hold: hold)
    }

    /// The headphone button reads "this page", i.e. from its first sentence.
    private func speak(page: PageContent) {
        guard let start = model.firstSentenceOffset(
            inChapter: page.chapterIndex,
            pageRange: page.range
        ) else { return }
        speech.speak(chapter: page.chapterIndex, fromChapterOffset: start)
    }

    /// A tap on a sentence: the page measured the tap in chapter coordinates, so
    /// the offset is handed to the voice as it is.
    private func speak(fromChapterOffset offset: Int) {
        guard model.pages.indices.contains(pageIndex) else { return }
        let page = model.pages[pageIndex]
        speech.speak(chapter: page.chapterIndex, fromChapterOffset: offset)
    }

    // MARK: - Pieces

    private var fraction: Double {
        guard !model.pages.isEmpty else { return 0 }
        return Double(min(pageIndex + 1, model.pages.count)) / Double(model.pages.count)
    }

    private var progressLine: some View {
        Rectangle()
            .fill(palette.swiftAccent.opacity(0.9))
            .frame(height: 2)
            .scaleEffect(x: max(fraction, 0.001), anchor: .leading)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Left: leave the book, and its contents. Right: find something in it, and
    /// mark the place the reader is at. Each pair is one capsule, so the bar reads
    /// as two groups instead of four loose circles.
    private var toolbar: some View {
        HStack(spacing: 10) {
            buttonPair(leftGroup)
                .opacity(chrome == .full ? 1 : 0)
                .allowsHitTesting(chrome == .full)

            Spacer(minLength: 8)

            buttonPair(rightGroup)
                .opacity(chrome == .full ? 1 : 0)
                .allowsHitTesting(chrome == .full)
        }
        .animation(.easeInOut(duration: 0.25), value: chrome)
    }

    /// The way out of the book, and its contents. The pair comes and goes with the
    /// rest of the toolbar — never left standing on its own while the voice reads,
    /// but never taken away either: a tap on the page's edge is how it is called
    /// back, and it must answer that tap like every other button does.
    private var leftGroup: [ToolbarButton] {
        [
            ToolbarButton(symbol: "chevron.left", label: "Quay lại") { dismiss() },
            ToolbarButton(symbol: "list.bullet", label: "Mục lục") {
                tocSection = 0
                showTOC = true
            }
        ]
    }

    private var rightGroup: [ToolbarButton] {
        [
            ToolbarButton(symbol: "magnifyingglass", label: "Tìm trong sách") {
                showSearch = true
            },
            ToolbarButton(
                symbol: isPageMarked ? "bookmark.fill" : "bookmark",
                label: isPageMarked ? "Bỏ đánh dấu trang này" : "Đánh dấu trang này"
            ) {
                toggleBookmark()
            }
        ]
    }

    /// One toolbar control.
    private struct ToolbarButton: Identifiable {
        let id = UUID()
        let symbol: String
        let label: String
        /// Drawn back a little: the way out of the book steps into the background
        /// while the voice is the thing in hand.
        var dimmed = false
        let action: () -> Void
    }

    /// One or two icons sharing a single capsule, divided by a hairline. Every
    /// icon keeps the same 44pt target, so grouping changes the look and not the
    /// size of anything.
    private func buttonPair(_ buttons: [ToolbarButton]) -> some View {
        HStack(spacing: 0) {
            ForEach(Array(buttons.enumerated()), id: \.element.id) { index, button in
                if index > 0 {
                    Rectangle()
                        .fill(palette.swiftInk.opacity(0.14))
                        .frame(width: 1, height: 22)
                }

                Button(action: button.action) {
                    Image(systemName: button.symbol)
                        .font(.system(size: ReaderMetrics.buttonIconSize, weight: .semibold))
                        .foregroundStyle(palette.swiftInk)
                        .opacity(button.dimmed ? 0.5 : 1)
                        .frame(width: ReaderMetrics.buttonSize, height: ReaderMetrics.buttonSize)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(button.label)
            }
        }
        .background(Capsule().fill(palette.controlFill))
        .overlay(Capsule().strokeBorder(palette.swiftInk.opacity(0.14)))
        .shadow(color: .black.opacity(0.20), radius: 7, y: 3)
    }

    /// Marks the page, or clears the mark it already carries. The mark is kept as a
    /// chapter plus a character offset, so it survives a change of type size.
    private func toggleBookmark() {
        guard let page = currentPage else { return }
        bookmarks.toggle(
            chapterIndex: page.chapterIndex,
            range: page.range,
            offset: page.range.location
        )
        scheduleAutoHide()
    }

    /// The reader's one round button. Toolbar, audio and settings all come through
    /// here, so they are the same size, same fill, same ring and same shadow — a
    /// button that only *looks* a different size is still a bug.
    private func circleButton(
        systemName: String,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: ReaderMetrics.buttonIconSize, weight: .semibold))
                .foregroundStyle(palette.swiftInk)
                .frame(width: ReaderMetrics.buttonSize, height: ReaderMetrics.buttonSize)
                .background(Circle().fill(palette.controlFill))
                .overlay(Circle().strokeBorder(palette.swiftInk.opacity(0.14)))
                .shadow(color: .black.opacity(0.20), radius: 7, y: 3)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private var loadingState: some View {
        VStack(spacing: 14) {
            ProgressView()
            Text("Đang mở sách…")
                .font(.footnote)
                .foregroundStyle(palette.swiftInkSoft)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func errorState(_ message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
            Text(message)
                .font(.callout)
                .multilineTextAlignment(.center)
        }
        .padding(32)
        .foregroundStyle(palette.swiftInkSoft)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// The book's outline and the reader's marks, in one sheet: they are the two
    /// ways to navigate a book, and neither deserves a button of its own.
    private var tocSheet: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("", selection: $tocSection) {
                    Text("Mục lục").tag(0)
                    Text("Đánh dấu").tag(1)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.top, 8)

                if tocSection == 0 {
                    contentsList
                } else {
                    marksList
                }
            }
            .background(palette.swiftPaper)
            .navigationTitle(tocSection == 0 ? "Mục lục" : "Đã đánh dấu")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Xong") { showTOC = false }
                }
            }
            .preferredColorScheme(palette.isDark ? .dark : .light)
        }
    }

    private var contentsList: some View {
        List {
            if model.toc.isEmpty {
                Text("Sách này không có mục lục.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(model.toc) { entry in
                    Button {
                        pageIndex = entry.pageIndex
                        showTOC = false
                    } label: {
                        HStack {
                            Text(entry.title)
                                .foregroundStyle(.primary)
                            Spacer()
                            Text("tr. \(entry.pageIndex + 1)")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    /// Marks are stored by position, so they are resolved into pages here — a mark
    /// keeps pointing at its sentence after a change of type size.
    private var marksList: some View {
        let resolved = bookmarks.marks.compactMap {
            model.hit(chapterIndex: $0.chapterIndex, offset: $0.offset)
        }
        return List {
            if resolved.isEmpty {
                Text("Chưa đánh dấu trang nào. Nút đánh dấu nằm ở góc trên bên phải.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(resolved) { hit in
                    Button {
                        pageIndex = hit.pageIndex
                        showTOC = false
                    } label: {
                        VStack(alignment: .leading, spacing: 3) {
                            Text(hit.chapterTitle)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(palette.swiftAccent)
                            Text(hit.snippet)
                                .font(.system(size: 14))
                                .foregroundStyle(.primary)
                                .lineLimit(2)
                            Text("tr. \(hit.pageIndex + 1)")
                                .font(.system(size: 12))
                                .foregroundStyle(.secondary)
                        }
                        .padding(.vertical, 2)
                    }
                }
                .onDelete { offsets in
                    bookmarks.remove(at: IndexSet(offsets.map { resolved[$0] }.compactMap { hit in
                        bookmarks.marks.firstIndex { $0.id == hit.id }
                    }))
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    // MARK: - Settings

    /// A paragraph of the page the reader is on, for the customise screen to set.
    private var previewSample: String {
        guard let page = currentPage else {
            return "Ngọn hải đăng vẫn đứng đó, quét ánh sáng qua mặt biển đêm."
        }
        let text = page.text.string
            .replacingOccurrences(of: "\n", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return String(text.prefix(420))
    }

    private var settingsSheet: some View {
        ThemesSettingsSheet(
            settings: settings,
            sample: previewSample
        )
            // Half the screen, as the phone version of the panel: the page being
            // dressed stays in sight while the look is chosen.
            .presentationDetents([.fraction(0.56)])
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(28)
            .onDisappear { scheduleAutoHide() }
    }

    // MARK: - Search

    /// Jumps to the page a search result lives on. Tapping a result and the
    /// screenshot harness both come through here.
    private func jump(to hit: ReaderViewModel.Hit) {
        pageIndex = hit.pageIndex
        showSearch = false
        scheduleAutoHide()
    }

    private var searchSheet: some View {
        SearchSheet(
            palette: palette,
            query: $searchQuery,
            hits: model.search(searchQuery),
            onPick: { hit in jump(to: hit) },
            onDone: { showSearch = false }
        )
    }
}
