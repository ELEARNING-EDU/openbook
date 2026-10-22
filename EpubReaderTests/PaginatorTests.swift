import XCTest
import UIKit
@testable import EpubReader

final class PaginatorTests: XCTestCase {

    private let page = CGSize(width: 160, height: 642)

    private func body(_ repeats: Int) -> NSAttributedString {
        let paragraph = NSMutableParagraphStyle()
        paragraph.lineSpacing = 8
        let sentence = "Ngọn hải đăng đứng im trên mũi đá, đèn quay chậm như một cái kim. "
        return NSAttributedString(
            string: String(repeating: sentence, count: repeats),
            attributes: [.font: UIFont.systemFont(ofSize: 19), .paragraphStyle: paragraph]
        )
    }

    func testEmptyTextIsOneEmptyPage() {
        XCTAssertEqual(Paginator.paginate(NSAttributedString(string: ""), in: page),
                       [NSRange(location: 0, length: 0)])
    }

    func testFrameTooSmallKeepsEverythingOnOnePage() {
        let text = body(40)
        XCTAssertEqual(Paginator.paginate(text, in: CGSize(width: 10, height: 10)),
                       [NSRange(location: 0, length: text.length)])
    }

    func testPagesCoverTheChapterInOrderWithoutGaps() {
        let text = body(120)
        let ranges = Paginator.paginate(text, in: page)

        XCTAssertGreaterThan(ranges.count, 1)
        var cursor = 0
        for range in ranges {
            XCTAssertEqual(range.location, cursor)
            XCTAssertGreaterThan(range.length, 0)
            cursor += range.length
        }
        XCTAssertEqual(cursor, text.length)
    }

    func testSameTextAndGeometryPaginateTheSameWay() {
        let text = body(30)
        XCTAssertEqual(Paginator.paginate(text, in: page), Paginator.paginate(text, in: page))
    }
}
