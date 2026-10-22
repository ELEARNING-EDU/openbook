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

    /// Highlight currently painted, in chapter coordinates, with the chapter it
    /// came from.
    private var appliedHighlight: NSRange?
    private var appliedHighlightChapter: Int?

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
        footerLabel.text = "Trang \(content.pageNumber) / \(content.pageCount)"

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

        view.addSubview(headerLabel)
        view.addSubview(footerLabel)
        textViews.forEach(view.addSubview)
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

    // MARK: - Highlight

    /// `chapterRange` arrives in chapter coordinates; only the part that falls on
    /// a column of this page is painted. The chapter has to be named as well,
    /// because every chapter numbers its characters from zero: a range from
    /// another chapter overlaps this page's numbers and used to wash the wrong
    /// words — which is what showed up when a paused page was turned.
    func updateHighlight(_ chapterRange: NSRange?, chapter: Int?) {
        guard chapterRange != appliedHighlight || chapter != appliedHighlightChapter else { return }
        appliedHighlight = chapterRange
        appliedHighlightChapter = chapter
        paintHighlight()
    }

    private func paintHighlight() {
        let wash = appliedHighlightChapter == content.chapterIndex ? appliedHighlight : nil
        for (index, textView) in textViews.enumerated() where index < cleanText.count {
            let attributed = NSMutableAttributedString(attributedString: cleanText[index])
            let base = textView.tag
            let length = attributed.length
            if let chapterRange = wash {
                let overlap = NSIntersectionRange(
                    chapterRange,
                    NSRange(location: base, length: length)
                )
                if overlap.length > 0 {
                    let local = NSRange(location: overlap.location - base, length: overlap.length)
                    if local.location >= 0, local.location + local.length <= length {
                        attributed.addAttribute(
                            .backgroundColor,
                            value: palette.highlight,
                            range: local
                        )
                    }
                }
            }
            textView.attributedText = attributed
        }
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

extension ReaderPageViewController: SheetSide {
    var isBack: Bool { false }
}
