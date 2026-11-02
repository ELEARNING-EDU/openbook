import CoreGraphics
import Foundation
import UIKit

/// Decides whether the image an EPUB calls its cover is worth showing.
///
/// Some books point at their title page instead of an illustrated cover; that
/// page decodes as an almost entirely white card. Such an image is worse than
/// the generated cover, so it is rejected and the caller falls back to the
/// gradient.
enum BookCoverImage {

    /// The decoded image when it is usable, or nil for a broken, missing or
    /// near-white cover.
    static func usableImage(from data: Data) -> UIImage? {
        guard let image = UIImage(data: data),
              image.size.width > 0,
              image.size.height > 0
        else { return nil }
        return isPlain(image) ? nil : image
    }

    /// A 32 × 32 sample is enough to tell a printed cover from a blank page,
    /// and it keeps the check cheap for a 6000 px image.
    static func isPlain(_ image: UIImage) -> Bool {
        guard let source = image.cgImage else { return false }

        let side = 32
        var pixels = [UInt8](repeating: 0, count: side * side * 4)
        let sample: (nonWhite: Int, saturation: Double) = pixels.withUnsafeMutableBytes { buffer in
            guard let context = CGContext(
                data: buffer.baseAddress,
                width: side,
                height: side,
                bitsPerComponent: 8,
                bytesPerRow: side * 4,
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
                    | CGBitmapInfo.byteOrder32Big.rawValue
            ) else { return (0, 0) }

            // Transparent covers are laid on white, exactly as a paper page would
            // look; otherwise their invisible pixels would read as black.
            context.setFillColor(UIColor.white.cgColor)
            context.fill(CGRect(x: 0, y: 0, width: side, height: side))
            context.draw(source, in: CGRect(x: 0, y: 0, width: side, height: side))

            var nonWhite = 0
            var saturation = 0.0
            for index in stride(from: 0, to: side * side * 4, by: 4) {
                let red = Double(buffer[index]) / 255
                let green = Double(buffer[index + 1]) / 255
                let blue = Double(buffer[index + 2]) / 255
                guard red <= 0.96 || green <= 0.96 || blue <= 0.96 else { continue }

                nonWhite += 1
                let brightest = max(red, green, blue)
                let saturationOfPixel = brightest > 0
                    ? (brightest - min(red, green, blue)) / brightest
                    : 0
                saturation += saturationOfPixel
            }
            return (nonWhite, saturation)
        }

        let area = Double(side * side)
        let nonWhiteFraction = Double(sample.nonWhite) / area
        // A white title page has only a little text (grey, not coloured) on it.
        // A real cover with a white ground and a small coloured logo has a high
        // saturation in that little part, so it is kept.
        guard nonWhiteFraction < 0.10 else { return false }
        let averageSaturation = sample.nonWhite > 0
            ? sample.saturation / Double(sample.nonWhite)
            : 0
        return averageSaturation < 0.15
    }
}
