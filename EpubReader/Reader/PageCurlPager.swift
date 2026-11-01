import SwiftUI
import UIKit

/// The reading surface: the system's page curl, with a back page of our own.
///
/// `UIPageViewController` curls beautifully, but on its own it paints the back of
/// the sheet with its own light paper — a white band on a night page. A
/// double-sided page view lets the book hand UIKit the back of each sheet, so the
/// paper that shows while a page turns is the theme's paper, printed with the
/// same page it came from.
struct PageCurlPager: UIViewControllerRepresentable {

    let pages: [PageContent]
    let safeTop: CGFloat
    let palette: ReaderPalette
    let margin: CGFloat
    let revision: Int
    @Binding var index: Int

    var highlights: [PageHighlightSpan] = []
    var ttsHighlight: NSRange?
    var ttsChapter: Int?
    var selection: NSRange?
    var selectionEditable = true
    var onTap: (PageTap) -> Void = { _ in }
    var onSelectionBegan: () -> Void = {}
    var onSelectionChanged: (SelectionReport) -> Void = { _ in }
    var onSelectionCancel: () -> Void = {}
    var onHighlightLongPress: (SelectionReport) -> Void = { _ in }
    var onHandleMoved: (Bool, Int) -> Void = { _, _ in }
    var wordRange: (Int, Int) -> NSRange? = { _, _ in nil }
    var draggedWordRange: (Int, Int, Int) -> NSRange? = { _, _, _ in nil }
    /// A turn asked for by a tap rather than by a finger on the paper. The token
    /// makes the same page reachable again, and the curl is played by the pager.
    var turn: PageTurnRequest?
    var demoTapPoint: CGPoint?
    var demoFold: DemoFold?
    var demoDrag: DemoDrag?
    var demoLongPress: DemoLongPress?
    var demoLongPressAgain: DemoLongPress?

    func makeUIViewController(context: Context) -> PageCurlBook {
        let book = PageCurlBook()
        book.onIndexChanged = { page in
            // UIKit calls this while a page is being curled, which is inside a
            // SwiftUI update: writing the state straight away is dropped on the
            // floor, and the binding then puts the old page back.
            DispatchQueue.main.async { index = page }
        }
        book.apply(
            pages: pages, safeTop: safeTop, palette: palette, margin: margin,
            revision: revision, highlights: highlights,
            ttsHighlight: ttsHighlight, ttsChapter: ttsChapter,
            selection: selection, selectionEditable: selectionEditable,
            onTap: onTap, onSelectionBegan: onSelectionBegan,
            onSelectionChanged: onSelectionChanged, onSelectionCancel: onSelectionCancel,
            onHighlightLongPress: onHighlightLongPress, onHandleMoved: onHandleMoved,
            wordRange: wordRange, draggedWordRange: draggedWordRange,
            demoTapPoint: demoTapPoint, demoFold: demoFold,
            demoLongPress: demoLongPress, demoLongPressAgain: demoLongPressAgain
        )
        book.show(page: index)
        return book
    }

    func updateUIViewController(_ book: PageCurlBook, context: Context) {
        book.onIndexChanged = { page in
            DispatchQueue.main.async { index = page }
        }
        book.apply(
            pages: pages, safeTop: safeTop, palette: palette, margin: margin,
            revision: revision, highlights: highlights,
            ttsHighlight: ttsHighlight, ttsChapter: ttsChapter,
            selection: selection, selectionEditable: selectionEditable,
            onTap: onTap, onSelectionBegan: onSelectionBegan,
            onSelectionChanged: onSelectionChanged, onSelectionCancel: onSelectionCancel,
            onHighlightLongPress: onHighlightLongPress, onHandleMoved: onHandleMoved,
            wordRange: wordRange, draggedWordRange: draggedWordRange,
            demoTapPoint: demoTapPoint, demoFold: demoFold,
            demoLongPress: demoLongPress, demoLongPressAgain: demoLongPressAgain
        )
        // The turn goes first: it is the one that animates, and the still page
        // that follows it is ignored while the sheet is in the air.
        book.play(turn)
        book.show(page: index)
    }
}

/// One side of a sheet: the reading page, or the back of it.
protocol SheetSide: UIViewController {
    var pageIndex: Int { get }
    var isBack: Bool { get }
}

final class PageCurlBook: UIViewController {

    var onIndexChanged: ((Int) -> Void)?

    private let pager = UIPageViewController(
        transitionStyle: .pageCurl,
        navigationOrientation: .horizontal,
        options: [
            .spineLocation: NSNumber(value: UIPageViewController.SpineLocation.min.rawValue)
        ]
    )

    private var pages: [PageContent] = []
    private var fronts: [Int: ReaderPageViewController] = [:]
    private var backs: [Int: SheetBackViewController] = [:]
    private var safeTop: CGFloat = 0
    private var palette: ReaderPalette = ReaderTheme.defaultPalette
    private var margin: CGFloat = 0
    private var revision = -1
    private var highlights: [PageHighlightSpan] = []
    private var ttsHighlight: NSRange?
    private var ttsChapter: Int?
    private var selection: NSRange?
    private var selectionEditable = true
    private var onTap: (PageTap) -> Void = { _ in }
    private var onSelectionBegan: () -> Void = {}
    private var onSelectionChanged: (SelectionReport) -> Void = { _ in }
    private var onSelectionCancel: () -> Void = {}
    private var onHighlightLongPress: (SelectionReport) -> Void = { _ in }
    private var onHandleMoved: (Bool, Int) -> Void = { _, _ in }
    private var wordRange: (Int, Int) -> NSRange? = { _, _ in nil }
    private var draggedWordRange: (Int, Int, Int) -> NSRange? = { _, _, _ in nil }
    private var demoLongPress: DemoLongPress?
    private var demoLongPressAgain: DemoLongPress?
    private var demoLongPressDelivered = false
    private var demoLongPressAgainDelivered = false
    /// Set while a curl we asked for is in the air, so the binding catching up
    /// does not cut it short with a still page.
    private var animating = false
    /// Set while a finger is turning a page. A page the voice asks for during
    /// that time has to wait, or it puts the page down under the finger.
    private var turning = false
    private var waitingPage: Int?
    /// A page asked for by something other than the voice — a search result, the
    /// contents — arriving while a curl we started is still in the air.
    private var pendingJump: Int?
    /// Counts the curls we have started, so a curl interrupted by a newer one
    /// cannot land the book on its own older page.
    private var turnID = 0
    private var playedTurn = -1
    private var demoTapPoint: CGPoint?
    private var demoTapDelivered = false
    private var demoFold: DemoFold?
    private var demoFoldPlayed = false

    private(set) var current = 0
    private var currentSide = 0

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = palette.paper
        pager.view.backgroundColor = palette.paper
        pager.isDoubleSided = true
        pager.dataSource = self
        pager.delegate = self
        pager.view.frame = view.bounds
        pager.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        addChild(pager)
        view.addSubview(pager.view)
        pager.didMove(toParent: self)
    }

    func apply(
        pages: [PageContent], safeTop: CGFloat, palette: ReaderPalette, margin: CGFloat,
        revision: Int, highlights: [PageHighlightSpan],
        ttsHighlight: NSRange?, ttsChapter: Int?,
        selection: NSRange?, selectionEditable: Bool,
        onTap: @escaping (PageTap) -> Void,
        onSelectionBegan: @escaping () -> Void,
        onSelectionChanged: @escaping (SelectionReport) -> Void,
        onSelectionCancel: @escaping () -> Void,
        onHighlightLongPress: @escaping (SelectionReport) -> Void,
        onHandleMoved: @escaping (Bool, Int) -> Void,
        wordRange: @escaping (Int, Int) -> NSRange?,
        draggedWordRange: @escaping (Int, Int, Int) -> NSRange?,
        demoTapPoint: CGPoint?, demoFold: DemoFold?,
        demoLongPress: DemoLongPress?, demoLongPressAgain: DemoLongPress?
    ) {
        let reset = revision != self.revision
            || palette != self.palette
            || pages.count != self.pages.count
            || safeTop != self.safeTop
            || margin != self.margin

        self.pages = pages
        self.safeTop = safeTop
        self.palette = palette
        self.margin = margin
        self.revision = revision
        self.highlights = highlights
        self.ttsHighlight = ttsHighlight
        self.ttsChapter = ttsChapter
        self.selection = selection
        self.selectionEditable = selectionEditable
        self.onTap = onTap
        self.onSelectionBegan = onSelectionBegan
        self.onSelectionChanged = onSelectionChanged
        self.onSelectionCancel = onSelectionCancel
        self.onHighlightLongPress = onHighlightLongPress
        self.onHandleMoved = onHandleMoved
        self.wordRange = wordRange
        self.draggedWordRange = draggedWordRange
        self.demoTapPoint = demoTapPoint
        self.demoFold = demoFold
        self.demoLongPress = demoLongPress
        self.demoLongPressAgain = demoLongPressAgain

        view.backgroundColor = palette.paper
        pager.view.backgroundColor = palette.paper

        if reset {
            fronts.removeAll()
            backs.removeAll()
            demoTapDelivered = false
            demoFoldPlayed = false
            demoLongPressDelivered = false
            demoLongPressAgainDelivered = false
            show(page: current, animated: false)
        } else {
            for front in fronts.values {
                configure(front)
            }
        }

        // A chosen passage owns the glass: the curl's own gestures would turn the
        // page under the finger, so they step aside until the passage is let go.
        let locked = selection != nil
        for recognizer in pager.gestureRecognizers {
            recognizer.isEnabled = !locked
        }

        deliverDemoTapIfArmed()
        deliverDemoLongPressesIfArmed()
        playDemoFoldIfNeeded()
        if UserDefaults.standard.bool(forKey: "demoDebug") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.5) { [weak self] in self?.probe() }
        }
    }

    /// Hands a page everything it needs: the callbacks, the saved passages and the
    /// passage currently being chosen.
    private func configure(_ front: ReaderPageViewController) {
        front.onTap = { [weak self] tap in self?.onTap(tap) }
        front.onSelectionBegan = { [weak self] in self?.onSelectionBegan() }
        front.onSelectionChanged = { [weak self] report in self?.onSelectionChanged(report) }
        front.onSelectionCancel = { [weak self] in self?.onSelectionCancel() }
        front.onHighlightLongPress = { [weak self] report in self?.onHighlightLongPress(report) }
        front.onHandleMoved = { [weak self] isStart, offset in
            self?.onHandleMoved(isStart, offset)
        }
        front.wordRange = wordRange
        front.draggedWordRange = draggedWordRange
        front.updateOverlays(
            highlights: highlights,
            tts: ttsHighlight,
            ttsChapter: ttsChapter,
            selection: selection,
            editable: selectionEditable
        )
    }

    func show(page: Int, animated: Bool = false) {
        guard pages.indices.contains(page), isViewLoaded else {
            current = page
            return
        }
        if turning {
            // The voice has read on to another page while a finger is halfway
            // through a curl. Turn the page when the finger has finished.
            //
            // A page equal to the one the book is standing on is not a request at
            // all, though: it is SwiftUI catching up with some other change — a
            // control fading, the reading place being written down — and replaying
            // it after the finger's own turn landed would curl the page straight
            // back to where it started.
            if page != current {
                waitingPage = page
            } else if UserDefaults.standard.bool(forKey: "demoDebug") {
                NSLog("[curl] ignored a rebind of page \(page) while turning")
            }
            return
        }
        let front = front(at: page)
        if !animated, animating {
            // A still page asked for while the sheet is still in the air would
            // land it flat, halfway through the curl. A page that is somewhere
            // else waits for the curl to be done; the same one is only the
            // binding catching up and needs nothing at all.
            if page != current { pendingJump = page }
            return
        }
        if !animated, pager.viewControllers?.first === front {
            // Already the page this book is showing. Nothing is done to the page
            // view here: a rebind can arrive while a finger is halfway through a
            // curl, and putting the page down under it would cut the curl short.
            current = page
            return
        }
        let direction: UIPageViewController.NavigationDirection = page >= current ? .forward : .reverse
        current = page
        currentSide = 0
        // A double-sided page view is a stack of sheets. Standing still it shows one
        // side and takes one controller; a curl is a transition and takes the sheet
        // whole — the page and the back it shows while it is in the air.
        let sides: [UIViewController] = animated
            ? [front, back(at: page)]
            : [front]
        if animated {
            turnID &+= 1
            let id = turnID
            animating = true
            pager.setViewControllers(sides, direction: direction, animated: true) { [weak self] _ in
                guard let self else { return }
                // A curl that was interrupted by a newer one leaves the landing to
                // that one. Settling on this older page would put the book back
                // where it came from — the page that turns itself back.
                guard id == self.turnID else { return }
                self.animating = false
                self.settle(on: front)
                self.playPendingJump()
            }
            onIndexChanged?(page)
        } else {
            pager.setViewControllers(sides, direction: direction, animated: false)
        }
    }

    /// Stands the book back up on a single side once a curl has landed.
    ///
    /// A curl is a transition, so UIKit is handed the whole sheet — the page and
    /// its back — and it goes on believing it is showing both. A page view in that
    /// state behaves like an open spread, and its own swipe no longer has a page
    /// to turn, which is why the edges and the last pages stopped answering to a
    /// finger. Putting the single page back, still, is invisible and gives the
    /// gesture something to turn again.
    private func settle(on front: ReaderPageViewController) {
        guard isViewLoaded, pager.viewControllers?.first !== front || pager.viewControllers?.count != 1
        else { return }
        pager.setViewControllers([front], direction: .forward, animated: false)
    }

    // MARK: - Sides

    private func front(at page: Int) -> ReaderPageViewController {
        if let existing = fronts[page] { return existing }
        let controller = ReaderPageViewController(
            pageIndex: page, content: pages[page], safeTop: safeTop,
            palette: palette, margin: margin
        )
        fronts[page] = controller
        configure(controller)
        return controller
    }

    private func back(at page: Int) -> SheetBackViewController {
        if let existing = backs[page] { return existing }
        let controller = SheetBackViewController(
            pageIndex: page,
            source: front(at: page),
            palette: palette
        )
        backs[page] = controller
        return controller
    }

    /// Plays a turn the reader asked for by tapping the edge of the page.
    func play(_ request: PageTurnRequest?) {
        guard let request, request.id != playedTurn else { return }
        playedTurn = request.id
        show(page: request.target, animated: true)
    }

    // MARK: - Harness

    private func deliverDemoTapIfArmed() {
        guard !demoTapDelivered, let point = demoTapPoint else { return }
        demoTapDelivered = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            guard let self, self.pages.indices.contains(self.current) else { return }
            self.front(at: self.current).tap(at: point)
        }
    }

    /// Plays a long press from the harness, so the selection path can be run on a
    /// machine whose simulator cannot be touched.
    private func deliverDemoLongPressesIfArmed() {
        deliver(demoLongPress, delivered: &demoLongPressDelivered)
        deliver(demoLongPressAgain, delivered: &demoLongPressAgainDelivered)
    }

    private func deliver(_ request: DemoLongPress?, delivered: inout Bool) {
        guard !delivered, let request, pages.indices.contains(current) else { return }
        delivered = true
        DispatchQueue.main.asyncAfter(deadline: .now() + request.after) { [weak self] in
            guard let self, self.pages.indices.contains(self.current) else { return }
            let page = self.front(at: self.current)
            page.performDemoLongPress(at: request.point)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                page.performDemoReleaseSelection()
            }
        }
    }

    /// Prints what a finger would find at the page the book is resting on.
    func probe() {
        guard isViewLoaded else { return }
        let vcs = pager.viewControllers ?? []
        let sides = vcs.compactMap { $0 as? SheetSide }.map { "\($0.pageIndex)\($0.isBack ? "b" : "f")" }
        NSLog("[probe] page \(current) vcs \(vcs.count) [\(sides.joined(separator: " "))] double \(pager.isDoubleSided)")
        for recognizer in pager.gestureRecognizers {
            NSLog("[probe] gesture \(type(of: recognizer)) enabled \(recognizer.isEnabled) state \(recognizer.state.rawValue)")
        }
        for subview in pager.view.subviews {
            NSLog("[probe] subview \(type(of: subview)) gestures \(subview.gestureRecognizers?.count ?? 0)")
        }
    }

    private func playDemoFoldIfNeeded() {
        guard !demoFoldPlayed, let fold = demoFold, !pages.isEmpty else { return }
        demoFoldPlayed = true
        for step in 0..<8 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8 + 1.1 * Double(step)) { [weak self] in
                guard let self else { return }
                let target = fold.back ? self.current - 1 : self.current + 1
                guard self.pages.indices.contains(target) else { return }
                self.show(page: target, animated: true)
            }
        }
    }
}

// MARK: - Data source

extension PageCurlBook: UIPageViewControllerDataSource, UIPageViewControllerDelegate {

    /// The sides either side of this one, sheet by sheet.
    ///
    /// A double-sided page view walks *sides*, not pages: front of a sheet, its
    /// back, then the front of the next sheet. So a turn is answered in two
    /// steps — the back of the sheet being lifted, then the page it uncovers —
    /// and each step must be a different side of a different sheet.
    ///
    /// Answering with the next *front* on both steps, as this did, made every
    /// swipe take two pages at once; worse, at the first and the last page the
    /// second answer fell off the end of the book, and UIKit quietly refused the
    /// whole curl — the pages near the ends that would not answer to a finger.
    private func precedingSide(of side: SheetSide) -> UIViewController? {
        if side.isBack {
            // The back of a sheet is the page on its front.
            return front(at: side.pageIndex)
        }
        let previous = side.pageIndex - 1
        guard previous >= 0 else { return nil }
        // Lifting the sheet before this page shows the back of that sheet first.
        return back(at: previous)
    }

    private func followingSide(of side: SheetSide) -> UIViewController? {
        if side.isBack {
            let next = side.pageIndex + 1
            guard pages.indices.contains(next) else { return nil }
            return front(at: next)
        }
        // The sheet being turned shows its back before the next page appears.
        return back(at: side.pageIndex)
    }

    func pageViewController(
        _ pageViewController: UIPageViewController,
        viewControllerBefore viewController: UIViewController
    ) -> UIViewController? {
        guard let from = viewController as? SheetSide else { return nil }
        let answer = precedingSide(of: from)
        if UserDefaults.standard.bool(forKey: "demoDebug") {
            NSLog("[curl] before \(from.pageIndex)\(from.isBack ? "b" : "f") -> \(label(answer))")
        }
        return answer
    }

    func pageViewController(
        _ pageViewController: UIPageViewController,
        viewControllerAfter viewController: UIViewController
    ) -> UIViewController? {
        guard let from = viewController as? SheetSide else { return nil }
        let answer = followingSide(of: from)
        if UserDefaults.standard.bool(forKey: "demoDebug") {
            NSLog("[curl] after \(from.pageIndex)\(from.isBack ? "b" : "f") -> \(label(answer))")
        }
        return answer
    }

    func pageViewController(
        _ pageViewController: UIPageViewController,
        willTransitionTo pendingViewControllers: [UIViewController]
    ) {
        turning = true
        // A hook for the harness: what a SwiftUI rebind does to the book when it
        // arrives while a finger is still dragging the page — the settings panel
        // fading, the reading place being written down, the chrome going. Played
        // on demand, `-demoRebindOnTurn 1`, so the answer to it can be checked
        // without waiting for the real race to fall the right way.
        if UserDefaults.standard.bool(forKey: "demoRebindOnTurn") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
                guard let self, self.turning else { return }
                NSLog("[curl] harness rebinding page \(self.current) mid-turn")
                self.show(page: self.current)
            }
        }
    }

    func pageViewController(
        _ pageViewController: UIPageViewController,
        spineLocationFor orientation: UIInterfaceOrientation
    ) -> UIPageViewController.SpineLocation {
        .min
    }

    func pageViewController(
        _ pageViewController: UIPageViewController,
        didFinishAnimating finished: Bool,
        previousViewControllers: [UIViewController],
        transitionCompleted completed: Bool
    ) {
        turning = false
        let waiting = waitingPage
        waitingPage = nil
        if UserDefaults.standard.bool(forKey: "demoDebug") {
            NSLog("[curl] finish completed \(completed) holding \(label(pageViewController.viewControllers?.first))")
        }
        guard completed, let side = pageViewController.viewControllers?.first as? SheetSide else {
            if let waiting { show(page: waiting) } else { playPendingJump() }
            return
        }
        if UserDefaults.standard.bool(forKey: "demoDebug") {
            NSLog("[curl] landed page \(side.pageIndex) back \(side.isBack)")
        }
        currentSide = side.isBack ? 1 : 0
        current = side.pageIndex
        settle(on: front(at: side.pageIndex))
        onIndexChanged?(side.pageIndex)
        if let waiting, waiting != side.pageIndex {
            show(page: waiting, animated: true)
        } else {
            playPendingJump()
        }
    }

    /// Plays a page that arrived in the middle of a curl: a search result, a jump
    /// from the contents. It is played still, so it reads as a jump and not as a
    /// turn nobody asked for.
    private func playPendingJump() {
        guard let jump = pendingJump else { return }
        pendingJump = nil
        guard jump != current else { return }
        show(page: jump)
    }
}

/// "\(page)f" / "\(page)b" — read only in a debug run's log.
private func label(_ controller: UIViewController?) -> String {
    guard let side = controller as? SheetSide else { return "none" }
    return "\(side.pageIndex)\(side.isBack ? "b" : "f")"
}

// MARK: - The back of a sheet

/// The back of one sheet: the theme's paper, with the page printed on the other
/// side showing through it, faintly and the wrong way round.
final class SheetBackViewController: UIViewController, SheetSide {

    let pageIndex: Int
    var isBack: Bool { true }

    private let source: ReaderPageViewController
    private let palette: ReaderPalette

    init(pageIndex: Int, source: ReaderPageViewController, palette: ReaderPalette) {
        self.pageIndex = pageIndex
        self.source = source
        self.palette = palette
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = palette.paper
        view.isOpaque = true

        let print = UIImageView(image: source.snapshot())
        print.contentMode = .scaleToFill
        print.frame = view.bounds
        print.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        print.alpha = 0.09
        print.transform = CGAffineTransform(scaleX: -1, y: 1)
        view.addSubview(print)
    }
}

/// A turn asked for with a tap: which page to reach, and a token so the same page
/// can be asked for twice.
struct PageTurnRequest: Equatable {
    let id: Int
    let target: Int
}

/// A long press the harness plays from a point: where the finger went down, and
/// how long after the pages were ready.
struct DemoLongPress {
    let point: CGPoint
    let after: Double
}
