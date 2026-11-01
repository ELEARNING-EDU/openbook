import SwiftUI
import UIKit

/// One screenful of text. The header and footer are drawn inside the page so
/// they curl along with the paper.
final class ReaderPageViewController: UIViewController {

    let pageIndex: Int

    private let content: PageContent
    private let safeTop: CGFloat
    private let palette: ReaderPalette
    private let margin: CGFloat
    /// One text view per column: a single column on a phone in portrait, two side
    /// by side when the reader asked for two. Each carries the chapter offset of
    /// its first character in its `tag`, so a tap and the read-aloud wash both
    /// map back to the book rather than to the view.
    private var textViews: [UITextView] = []
    /// The unwashed slices, so the highlight can be repainted from clean text
    /// every time it moves.
    private var cleanText: [NSAttributedString] = []
    private let headerLabel = UILabel()
    private let footerLabel = UILabel()

    /// Reports a tap, with where it landed and which character (if any) it hit.
    ///
    /// The page only measures a tap; what it means is the reader's to decide —
    /// the sides turn the page, the middle shows the controls.
    var onTap: ((PageTap) -> Void)?

    /// What the page is asked to paint: the voice's sentence, the passages the
    /// reader has marked, and the selection being dragged.
    private var ttsHighlight: NSRange?
    private var ttsChapter: Int?
    private var savedHighlights: [PageHighlightSpan] = []
    private var activeSelection: NSRange?
    private var selectionEditable = true
    private var selectionLayer = SelectionLayerView()

    /// A long press reports the word it found; dragging it grows the passage.
    var onSelectionBegan: (() -> Void)?
    var onSelectionChanged: ((SelectionReport) -> Void)?
    var onSelectionCancel: (() -> Void)?
    var onHighlightLongPress: ((SelectionReport) -> Void)?
    /// A handle was dragged: which end, and the chapter offset it reached.
    var onHandleMoved: ((Bool, Int) -> Void)?
    /// The chapter's own text, used to snap a selection to whole words.
    var wordRange: ((Int, Int) -> NSRange?)?
    var draggedWordRange: ((Int, Int, Int) -> NSRange?)?

    private var longPressAnchor: Int?
    private var longPressedSavedHighlight = false
    private var draggingStartHandle = false
    private var handlePan: UIPanGestureRecognizer?

    init(
        pageIndex: Int,
        content: PageContent,
        safeTop: CGFloat,
        palette: ReaderPalette,
        margin: CGFloat
    ) {
        self.pageIndex = pageIndex
        self.content = content
        self.safeTop = safeTop
        self.palette = palette
        self.margin = margin
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = palette.paper

        headerLabel.font = .systemFont(ofSize: 10, weight: .semibold)
        headerLabel.textColor = palette.inkSoft
        headerLabel.textAlignment = .center
        headerLabel.text = content.chapterTitle.isEmpty
            ? " "
            : content.chapterTitle.uppercased()

        footerLabel.font = .systemFont(ofSize: 11)
        footerLabel.textColor = palette.inkSoft
        footerLabel.textAlignment = .center
        footerLabel.text = L10n.format("reader.pageFooter", content.pageNumber, content.pageCount)

        cleanText = content.columns.map { column in
            let slice = NSMutableAttributedString(
                attributedString: content.text.attributedSubstring(from: column)
            )
            // The ink belongs to the theme, not to the bitset pages. Going from
            // day to night does not re-typeset the book, so the colour is put on
            // here: otherwise the pages keep the ink they were measured with and
            // the reader is left with black on black.
            if slice.length > 0 {
                slice.addAttribute(
                    .foregroundColor,
                    value: palette.ink,
                    range: NSRange(location: 0, length: slice.length)
                )
            }
            return slice
        }

        textViews = zip(content.columns, cleanText).map { column, text in
            let view = UITextView()
            view.isEditable = false
            view.isScrollEnabled = false
            view.isSelectable = false
            view.backgroundColor = .clear
            view.textContainerInset = .zero
            view.textContainer.lineFragmentPadding = 0
            view.attributedText = text
            view.tag = content.range.location + column.location
            return view
        }

        let tap = UITapGestureRecognizer(target: self, action: #selector(handleTap(_:)))
        // On the whole page rather than just the text, so a tap on the margins,
        // header or footer can be told apart from a tap on a sentence.
        view.addGestureRecognizer(tap)

        // A long press chooses the word under the finger; dragging it grows the
        // passage. The page draws the selection itself so the band and its two
        // bars sit exactly on the lines the text was laid out on, in either
        // column, and so the popup can be told where the passage is.
        let longPress = UILongPressGestureRecognizer(
            target: self,
            action: #selector(handleLongPress(_:))
        )
        longPress.minimumPressDuration = 0.35
        longPress.allowableMovement = 600
        view.addGestureRecognizer(longPress)
        tap.require(toFail: longPress)

        let pan = UIPanGestureRecognizer(target: self, action: #selector(handleHandlePan(_:)))
        pan.isEnabled = false
        pan.delegate = self
        view.addGestureRecognizer(pan)
        handlePan = pan

        view.addSubview(headerLabel)
        view.addSubview(footerLabel)
        textViews.forEach(view.addSubview)
        selectionLayer.frame = view.bounds
        selectionLayer.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(selectionLayer)

        // A page can be handed its saved passages before its views exist (the
        // pager configures a controller as soon as it builds it). Painting here
        // as well means a book opened with marks already in it shows them on the
        // first frame, not only after the next change.
        paintMarks()
        updateSelectionLayer()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        // Every rect comes from ReaderMetrics, which the paginator measures with,
        // so the page can only ever draw inside the area that was paginated.
        let bounds = view.bounds.size

        headerLabel.frame = ReaderMetrics.headerRect(in: bounds, safeTop: safeTop)
        footerLabel.frame = ReaderMetrics.footerRect(in: bounds)

        let rects = ReaderMetrics.columnRects(
            in: bounds,
            safeTop: safeTop,
            margin: margin,
            columns: max(1, textViews.count)
        )
        for (index, textView) in textViews.enumerated() where index < rects.count {
            textView.frame = rects[index]
        }

        selectionLayer.frame = view.bounds
        if activeSelection != nil { updateSelectionLayer() }
    }

    // MARK: - Taps

    /// Drives the tap path from a coordinate instead of a finger. The simulator
    /// cannot be tapped programmatically, so this is how the harness checks that
    /// hit-testing maps a point to the right sentence.
    func performDemoTap(at point: CGPoint) {
        view.layoutIfNeeded()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            self?.emitTap(at: point)
        }
    }

    @objc private func handleTap(_ gesture: UITapGestureRecognizer) {
        emitTap(at: gesture.location(in: view))
    }

    private func emitTap(at point: CGPoint) {
        // While a passage is chosen, the page is not a book to be turned: a tap
        // anywhere outside the popup merely lets the passage go.
        if activeSelection != nil {
            onSelectionCancel?()
            return
        }

        let width = view.bounds.width
        for textView in textViews {
            let local = view.convert(point, to: textView)
            guard let chapterOffset = characterOffset(at: local, in: textView) else { continue }
            if UserDefaults.standard.bool(forKey: "demoDebug") {
                NSLog("[speech] tap \(point.x),\(point.y) column \(textView.tag) chapter \(chapterOffset)")
            }
            onTap?(PageTap(point: point, width: width, offset: chapterOffset))
            return
        }
        if UserDefaults.standard.bool(forKey: "demoDebug") {
            NSLog("[speech] tap \(point.x),\(point.y) blank")
        }
        onTap?(PageTap(point: point, width: width, offset: nil))
    }

    /// The character under the finger, in chapter coordinates — or nil when the
    /// finger is on blank paper.
    ///
    /// `closestPosition` alone cannot tell a sentence from the paper beside it:
    /// it snaps to the nearest character, so a tap on the empty space under the
    /// last line came back as a tap on the last sentence of the book. The point
    /// has to land on a character of its own.
    private func characterOffset(at point: CGPoint, in textView: UITextView) -> Int? {
        guard textView.bounds.contains(point),
              let position = textView.closestPosition(to: point) else { return nil }
        let index = textView.offset(from: textView.beginningOfDocument, to: position)
        // A finger lands between two characters as often as on one, so the two
        // neighbours are asked as well.
        for candidate in [index - 1, index, index + 1] where candidate >= 0 {
            guard let start = textView.position(from: textView.beginningOfDocument, offset: candidate),
                  let end = textView.position(from: start, offset: 1),
                  let range = textView.textRange(from: start, to: end) else { continue }
            if textView.selectionRects(for: range).contains(where: { $0.rect.contains(point) }) {
                return textView.tag + candidate
            }
        }
        return nil
    }

    // MARK: - Marked passages and selection

    /// `chapterRange` arrives in chapter coordinates; only the part that falls on
    /// a column of this page is painted. The chapter has to be named as well,
    /// because every chapter numbers its characters from zero: a range from
    /// another chapter overlaps this page's numbers and used to wash the wrong
    /// words — which is what showed up when a paused page was turned.
    func updateOverlays(
        highlights: [PageHighlightSpan],
        tts: NSRange?,
        ttsChapter: Int?,
        selection: NSRange?,
        editable: Bool
    ) {
        let selectionChanged = selection != activeSelection || editable != selectionEditable
        let paintChanged = highlights != savedHighlights
            || tts != self.ttsHighlight
            || ttsChapter != self.ttsChapter

        savedHighlights = highlights
        self.ttsHighlight = tts
        self.ttsChapter = ttsChapter
        activeSelection = selection
        selectionEditable = editable

        if paintChanged { paintMarks() }
        if selectionChanged || paintChanged { updateSelectionLayer() }
        handlePan?.isEnabled = editable && selection != nil
        if selectionChanged, selection != nil {
            // This runs while SwiftUI is applying the pager, so the report is
            // handed back on the next turn rather than mutating state mid-update.
            DispatchQueue.main.async { [weak self] in self?.reportSelection() }
        }
    }

    private func paintMarks() {
        for (index, textView) in textViews.enumerated() where index < cleanText.count {
            let attributed = NSMutableAttributedString(attributedString: cleanText[index])
            let base = textView.tag
            let length = attributed.length
            guard length > 0 else {
                textView.attributedText = attributed
                continue
            }
            let local = NSRange(location: base, length: length)

            // The passages the reader marked, in their own colours, underneath
            // everything the voice or the finger is showing.
            for span in savedHighlights where span.chapterIndex == content.chapterIndex {
                let overlap = NSIntersectionRange(span.range, local)
                guard overlap.length > 0 else { continue }
                attributed.addAttribute(
                    .backgroundColor,
                    value: span.color,
                    range: NSRange(location: overlap.location - base, length: overlap.length)
                )
            }

            if ttsChapter == content.chapterIndex, let tts = ttsHighlight {
                let overlap = NSIntersectionRange(tts, local)
                if overlap.length > 0 {
                    attributed.addAttribute(
                        .backgroundColor,
                        value: palette.highlight,
                        range: NSRange(location: overlap.location - base, length: overlap.length)
                    )
                }
            }

            textView.attributedText = attributed
        }
    }

    // MARK: - Choosing a passage

    @objc private func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        let point = gesture.location(in: view)
        switch gesture.state {
        case .began: beginSelection(at: point)
        case .changed: extendSelection(to: point)
        default: finishSelectionGesture()
        }
    }

    private func beginSelection(at point: CGPoint) {
        guard let offset = selectionOffset(at: point) else { return }
        if let span = savedHighlight(containing: offset) {
            longPressedSavedHighlight = true
            longPressAnchor = nil
            activeSelection = span.range
            selectionEditable = false
            onSelectionBegan?()
            updateSelectionLayer()
            // The saved highlight brings its own band; the popup only needs the
            // rectangle, and reporting the range as a new selection first would
            // flash the editable wash over the saved colour.
            if let report = currentReport() {
                onHighlightLongPress?(report)
            }
            return
        }
        guard let word = wordRange?(content.chapterIndex, offset), word.length > 0 else {
            return
        }
        longPressedSavedHighlight = false
        longPressAnchor = offset
        activeSelection = word
        selectionEditable = true
        onSelectionBegan?()
        updateSelectionLayer()
        reportSelection()
    }

    private func extendSelection(to point: CGPoint) {
        guard !longPressedSavedHighlight, let anchor = longPressAnchor,
              let current = selectionOffset(at: point, clamp: true)
        else { return }
        guard let range = draggedWordRange?(content.chapterIndex, anchor, current),
              range != activeSelection
        else { return }
        activeSelection = range
        updateSelectionLayer()
        reportSelection()
    }

    private func finishSelectionGesture() {
        longPressAnchor = nil
        longPressedSavedHighlight = false
        if activeSelection != nil {
            handlePan?.isEnabled = selectionEditable
            reportSelection()
        }
    }

    /// Drives the real selection path from a point, for the screenshot harness:
    /// the simulator on this machine cannot be touched.
    func performDemoLongPress(at point: CGPoint) {
        view.layoutIfNeeded()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            self?.beginSelection(at: point)
        }
    }

    func performDemoDragSelection(to point: CGPoint) {
        extendSelection(to: point)
    }

    func performDemoReleaseSelection() {
        finishSelectionGesture()
    }

    @objc private func handleHandlePan(_ gesture: UIPanGestureRecognizer) {
        guard selectionEditable, let range = activeSelection else { return }
        let point = gesture.location(in: view)

        switch gesture.state {
        case .began:
            guard let bars = selectionBars(for: range) else { return }
            let start = handleRect(at: CGPoint(x: bars.start.minX, y: bars.start.midY))
            let end = handleRect(at: CGPoint(x: bars.end.maxX, y: bars.end.midY))
            if start.insetBy(dx: -22, dy: -14).contains(point) {
                draggingStartHandle = true
            } else if end.insetBy(dx: -22, dy: -14).contains(point) {
                draggingStartHandle = false
            }
        case .changed:
            guard let offset = selectionOffset(at: point, clamp: true) else { return }
            onHandleMoved?(draggingStartHandle, offset)
        default:
            break
        }
    }

    /// Where the text under a point sits, in chapter coordinates.
    private func selectionOffset(at point: CGPoint, clamp: Bool = false) -> Int? {
        for textView in textViews where textView.bounds.contains(view.convert(point, to: textView)) {
            return textView.tag + characterIndex(at: view.convert(point, to: textView), in: textView)
        }
        guard clamp, let nearest = nearestTextView(to: point) else { return nil }
        let local = view.convert(point, to: nearest)
        let length = (nearest.text as NSString).length
        let index = local.x < nearest.bounds.midX ? 0 : length
        return nearest.tag + index
    }

    private func characterIndex(at point: CGPoint, in textView: UITextView) -> Int {
        let clamped = CGPoint(
            x: min(max(point.x, 0), textView.bounds.width),
            y: min(max(point.y, 0), textView.bounds.height)
        )
        guard let position = textView.closestPosition(to: clamped) else { return 0 }
        return textView.offset(from: textView.beginningOfDocument, to: position)
    }

    private func nearestTextView(to point: CGPoint) -> UITextView? {
        var best: UITextView?
        var bestDistance = CGFloat.greatestFiniteMagnitude
        for textView in textViews {
            let local = view.convert(point, to: textView)
            let dx = max(max(textView.bounds.minX - local.x, 0), local.x - textView.bounds.maxX)
            let dy = max(max(textView.bounds.minY - local.y, 0), local.y - textView.bounds.maxY)
            let distance = dx * dx + dy * dy
            if distance < bestDistance {
                bestDistance = distance
                best = textView
            }
        }
        return best
    }

    private func savedHighlight(containing offset: Int) -> PageHighlightSpan? {
        savedHighlights.first {
            $0.chapterIndex == content.chapterIndex && $0.range.contains(offset)
        }
    }

    /// The band and the two bars are drawn from the text's own line fragments, so
    /// they follow the type size, the leading and either column.
    private func updateSelectionLayer() {
        guard let range = activeSelection, range.length > 0 else {
            selectionLayer.clear()
            return
        }
        view.layoutIfNeeded()
        let rects = selectionRects(for: range)
        let bars = selectionBars(for: range)
        selectionLayer.set(
            rects: rects,
            startRect: bars?.start,
            endRect: bars?.end,
            color: palette.accent,
            showsHandles: selectionEditable
        )
    }

    /// The two bars, measured from the characters at the ends of the passage.
    ///
    /// Taking the first and last rectangle in reading order is enough for one
    /// column, but a passage that crosses the column break would put its end bar
    /// on the wrong column. The character offsets cannot be wrong.
    private func selectionBars(for chapterRange: NSRange) -> (start: CGRect, end: CGRect)? {
        guard chapterRange.length > 0 else { return nil }
        let lastOffset = chapterRange.location + chapterRange.length - 1
        var startRect: CGRect?
        var endRect: CGRect?

        for textView in textViews {
            let tvStart = textView.tag
            let tvLength = (textView.text as NSString).length
            guard tvLength > 0 else { continue }
            let local = NSIntersectionRange(
                chapterRange,
                NSRange(location: tvStart, length: tvLength)
            )
            guard local.length > 0 else { continue }

            let converted = NSRange(location: local.location - tvStart, length: local.length)
            let layout = textView.layoutManager
            layout.ensureLayout(for: textView.textContainer)
            let glyphs = layout.glyphRange(forCharacterRange: converted, actualCharacterRange: nil)
            guard glyphs.length > 0 else { continue }

            var lines: [(range: NSRange, rect: CGRect)] = []
            layout.enumerateLineFragments(forGlyphRange: glyphs) { _, _, container, lineGlyphs, _ in
                let lineSelection = NSIntersectionRange(glyphs, lineGlyphs)
                guard lineSelection.length > 0 else { return }
                let characters = layout.characterRange(
                    forGlyphRange: lineSelection,
                    actualGlyphRange: nil
                )
                let box = layout.boundingRect(forGlyphRange: lineSelection, in: container)
                lines.append((
                    range: NSRange(location: tvStart + characters.location, length: characters.length),
                    rect: CGRect(
                        x: textView.frame.minX + box.minX,
                        y: textView.frame.minY + box.minY,
                        width: max(1, box.width),
                        height: box.height
                    )
                ))
            }

            if startRect == nil,
               let line = lines.first(where: { NSLocationInRange(chapterRange.location, $0.range) }) {
                startRect = line.rect
            }
            if let line = lines.first(where: { NSLocationInRange(lastOffset, $0.range) }) {
                endRect = line.rect
            }
        }

        guard let startRect, let endRect else { return nil }
        return (startRect, endRect)
    }

    private func selectionRects(for chapterRange: NSRange) -> [CGRect] {
        guard chapterRange.length > 0 else { return [] }
        var rects: [CGRect] = []
        for textView in textViews {
            let local = NSIntersectionRange(
                chapterRange,
                NSRange(location: textView.tag, length: (textView.text as NSString).length)
            )
            guard local.length > 0 else { continue }
            let converted = NSRange(location: local.location - textView.tag, length: local.length)
            let layout = textView.layoutManager
            layout.ensureLayout(for: textView.textContainer)
            let glyphs = layout.glyphRange(forCharacterRange: converted, actualCharacterRange: nil)
            guard glyphs.length > 0 else { continue }
            layout.enumerateEnclosingRects(
                forGlyphRange: glyphs,
                withinSelectedGlyphRange: NSRange(location: NSNotFound, length: 0),
                in: textView.textContainer
            ) { rect, _ in
                guard rect.height > 0.5 else { return }
                rects.append(
                    CGRect(
                        x: textView.frame.minX + rect.minX,
                        y: textView.frame.minY + rect.minY,
                        width: max(1, rect.width),
                        height: rect.height
                    )
                )
            }
        }
        return rects.sorted { ($0.minY, $0.minX) < ($1.minY, $1.minX) }
    }

    private func handleRect(at point: CGPoint) -> CGRect {
        CGRect(x: point.x - 6, y: point.y - 14, width: 12, height: 28)
    }

    private func currentReport() -> SelectionReport? {
        guard let range = activeSelection, range.length > 0 else { return nil }
        let rects = selectionRects(for: range)
        guard let first = rects.first else { return nil }
        let union = rects.dropFirst().reduce(first) { $0.union($1) }
        return SelectionReport(
            chapter: content.chapterIndex,
            range: range,
            rect: union,
            editable: selectionEditable
        )
    }

    private func reportSelection() {
        guard let report = currentReport() else { return }
        onSelectionChanged?(report)
    }

    /// Kept for the harness: the popup is anchored on the passage.
    func selectionAnchor(for range: NSRange) -> CGRect? {
        currentReport()?.rect
    }

    // MARK: - The curl's own needs

    /// The page as an image.
    ///
    /// The curl folds this bitmap, so the sheet that lifts is made of the page's
    /// own paper — white, cream or black, whichever theme it is printed on.
    func snapshot() -> UIImage {
        view.layoutIfNeeded()
        let renderer = UIGraphicsImageRenderer(bounds: view.bounds)
        return renderer.image { context in
            view.layer.render(in: context.cgContext)
        }
    }

    // MARK: - The harness

    /// Maps a point in the page to a character offset, as a finger would.
    func tap(at point: CGPoint) { emitTap(at: point) }
}

/// A tap as the page measured it: where on the glass it landed, how wide the page
/// is, and — when the finger found a character — that character's offset in the
/// chapter.
struct PageTap {
    let point: CGPoint
    let width: CGFloat
    let offset: Int?
}

/// A fold the screenshot harness holds open, instead of needing a hand on the
/// glass: how far the sheet has been pulled (0…1 of the page) and whether it is
/// a turn back.
struct DemoFold {
    var pull: Double
    var back: Bool = false
}

/// A pull the harness plays back through the drag code, so a fold can be watched
/// moving: how far across it goes, how far down, over how many seconds, where the
/// hand went down, and how much of the pull to stop at when a still is wanted.
struct DemoDrag {
    var dx: CGFloat
    var dy: CGFloat
    var seconds: Double
    /// Where the hand went down, in page points. One tenth in from the middle of
    /// the right edge when nil.
    var from: CGPoint?
    /// Stop at this fraction of the pull and leave the fold standing there, for a
    /// photograph. The pull is not let go, so nothing snaps back.
    var hold: Double?
}

/// One saved passage, in chapter coordinates, ready for a page to paint.
struct PageHighlightSpan: Equatable {
    let chapterIndex: Int
    let range: NSRange
    let color: UIColor
}

/// Where a chosen passage sits on the glass, so the popup can be anchored above
/// it (or below, when there is no room above).
struct SelectionReport: Equatable {
    let chapter: Int
    let range: NSRange
    /// The whole passage in page coordinates.
    let rect: CGRect
    /// False while an already-saved highlight is being edited: it has no handles.
    let editable: Bool
}

extension ReaderPageViewController: UIGestureRecognizerDelegate {

    /// The handle drag only starts on a bar; a pan anywhere else belongs to the
    /// book (turning the page), not to the selection.
    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        guard gestureRecognizer === handlePan,
              selectionEditable,
              let range = activeSelection
        else { return false }
        guard let bars = selectionBars(for: range) else { return false }
        let point = gestureRecognizer.location(in: view)
        let start = handleRect(at: CGPoint(x: bars.start.minX, y: bars.start.midY))
        let end = handleRect(at: CGPoint(x: bars.end.maxX, y: bars.end.midY))
        return start.insetBy(dx: -22, dy: -14).contains(point)
            || end.insetBy(dx: -22, dy: -14).contains(point)
    }
}

/// The wash behind a chosen passage and the two bars that say where it starts
/// and ends. It sits above the text views but takes no touches: the page's own
/// long press and pan recognizers do the work.
private final class SelectionLayerView: UIView {

    private var rects: [CGRect] = []
    private var startRect: CGRect?
    private var endRect: CGRect?
    private var color: UIColor = .tintColor
    private var showsHandles = false

    override init(frame: CGRect) {
        super.init(frame: frame)
        isOpaque = false
        backgroundColor = .clear
        isUserInteractionEnabled = false
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    func set(
        rects: [CGRect],
        startRect: CGRect?,
        endRect: CGRect?,
        color: UIColor,
        showsHandles: Bool
    ) {
        self.rects = rects
        self.startRect = startRect
        self.endRect = endRect
        self.color = color
        self.showsHandles = showsHandles
        setNeedsDisplay()
    }

    func clear() {
        guard !rects.isEmpty else { return }
        rects = []
        startRect = nil
        endRect = nil
        setNeedsDisplay()
    }

    override func draw(_ rect: CGRect) {
        // A saved highlight being edited keeps the colour it was marked in; the
        // selection wash is only ever drawn for a passage that is being chosen.
        guard showsHandles, !rects.isEmpty, let context = UIGraphicsGetCurrentContext()
        else { return }

        context.setFillColor(color.withAlphaComponent(0.22).cgColor)
        for line in rects {
            context.fill(line)
        }

        guard let startRect, let endRect else { return }
        let bar: CGFloat = 3
        let start = CGRect(
            x: startRect.minX - bar / 2,
            y: startRect.minY - 2,
            width: bar,
            height: startRect.height + 4
        )
        let end = CGRect(
            x: endRect.maxX - bar / 2,
            y: endRect.minY - 2,
            width: bar,
            height: endRect.height + 4
        )
        color.withAlphaComponent(0.9).setFill()
        UIBezierPath(roundedRect: start, cornerRadius: bar / 2).fill()
        UIBezierPath(roundedRect: end, cornerRadius: bar / 2).fill()
    }
}

extension ReaderPageViewController: SheetSide {
    var isBack: Bool { false }
}
