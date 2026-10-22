import CoreText
import Foundation

/// Splits a chapter's attributed text into page-sized string ranges using CoreText.
///
/// The same string range is later handed to a text view for display, so the page
/// renderer and the paginator must use identical width, fonts and paragraph styles.
enum Paginator {

    static func paginate(_ attributed: NSAttributedString, in size: CGSize) -> [NSRange] {
        let total = attributed.length
        guard total > 0 else { return [NSRange(location: 0, length: 0)] }
        guard size.width > 20, size.height > 20 else { return [NSRange(location: 0, length: total)] }

        let framesetter = CTFramesetterCreateWithAttributedString(attributed)
        let path = CGPath(rect: CGRect(origin: .zero, size: size), transform: nil)

        var ranges: [NSRange] = []
        var location = 0
        var safety = 0

        while location < total, safety < 4000 {
            safety += 1
            let frame = CTFramesetterCreateFrame(
                framesetter,
                CFRange(location: location, length: 0),
                path,
                nil
            )
            let visible = CTFrameGetVisibleStringRange(frame)
            guard visible.length > 0 else { break }
            ranges.append(NSRange(location: location, length: visible.length))
            location += visible.length
        }

        if ranges.isEmpty {
            ranges = [NSRange(location: 0, length: total)]
        }
        return ranges
    }
}
