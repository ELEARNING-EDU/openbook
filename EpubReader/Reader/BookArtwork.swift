import UIKit

/// The cover art used by the lock screen and Control Centre.
///
/// The app draws its covers from the title rather than shipping image assets, so
/// the now-playing artwork is the same gradient the shelf shows — recognisable
/// at a glance without a second design.
enum BookArtwork {

    static func image(for title: String, size: CGSize = CGSize(width: 512, height: 512)) -> UIImage {
        let colors = gradient(for: title)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { context in
            let cg = context.cgContext
            let space = CGColorSpaceCreateDeviceRGB()
            let cgColors = colors.map(\.cgColor) as CFArray

            if let gradient = CGGradient(colorsSpace: space, colors: cgColors, locations: [0, 1]) {
                cg.drawLinearGradient(
                    gradient,
                    start: .zero,
                    end: CGPoint(x: size.width, y: size.height),
                    options: []
                )
            }

            // The same darker spine the shelf's cover wears.
            cg.setFillColor(UIColor.black.withAlphaComponent(0.22).cgColor)
            cg.fill(CGRect(x: 0, y: 0, width: size.width * 0.045, height: size.height))
        }
    }

    /// The same five pairs `BookCover` uses, chosen by the title so a book keeps
    /// one colour everywhere.
    static func gradient(for title: String) -> [UIColor] {
        let palettes: [(UInt32, UInt32)] = [
            (0x38578C, 0x17294D),
            (0x944D38, 0x4A221C),
            (0x297066, 0x0F3B36),
            (0x8C6B2B, 0x453012),
            (0x664080, 0x301C45)
        ]
        let sum = title.unicodeScalars.reduce(0) { $0 + Int($1.value) }
        let pair = palettes[sum % palettes.count]
        return [UIColor(rgb: Int(pair.0)), UIColor(rgb: Int(pair.1))]
    }
}
