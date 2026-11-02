import XCTest
import UIKit
@testable import EpubReader

final class BookCoverImageTests: XCTestCase {

    private func png(
        background: UIColor,
        mark: CGRect? = nil,
        markColor: UIColor = .black
    ) -> Data {
        let size = CGSize(width: 60, height: 90)
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { context in
            background.setFill()
            context.fill(CGRect(origin: .zero, size: size))
            if let mark {
                markColor.setFill()
                context.fill(mark)
            }
        }
        return image.pngData()!
    }

    func testAWhiteCoverIsRejected() {
        XCTAssertNil(BookCoverImage.usableImage(from: png(background: .white)))
    }

    /// A white title page with a small line of type is still a blank cover.
    func testAWhiteTitlePageWithASmallMarkIsRejected() {
        let data = png(
            background: .white,
            mark: CGRect(x: 28, y: 42, width: 4, height: 6)
        )
        XCTAssertNil(BookCoverImage.usableImage(from: data))
    }

    func testAColouredCoverIsKept() {
        XCTAssertNotNil(BookCoverImage.usableImage(from: png(background: .systemBlue)))
    }

    /// A real cover may have a white ground; a large picture on it must survive
    /// the blank-page check.
    func testAWhiteCoverWithALargePictureIsKept() {
        let data = png(
            background: .white,
            mark: CGRect(x: 0, y: 0, width: 30, height: 90),
            markColor: .systemBlue
        )
        XCTAssertNotNil(BookCoverImage.usableImage(from: data))
    }

    /// A small, coloured mark is a logo, not a blank title page.
    func testAWhiteCoverWithASmallColouredLogoIsKept() {
        let data = png(
            background: .white,
            mark: CGRect(x: 28, y: 42, width: 6, height: 6),
            markColor: .systemBlue
        )
        XCTAssertNotNil(BookCoverImage.usableImage(from: data))
    }

    func testBrokenDataIsRejected() {
        XCTAssertNil(BookCoverImage.usableImage(from: Data("not an image".utf8)))
    }
}
