import XCTest
@testable import EpubReader

/// Reader guide and review prompt follow the same rules as Android.
final class ParityPromptTests: XCTestCase {
    private var defaults: UserDefaults!

    override func setUp() {
        defaults = UserDefaults(suiteName: "ParityPromptTests")!
        defaults.removePersistentDomain(forName: "ParityPromptTests")
    }

    func testReviewIsAskedOnceOnTheTenthLaunch() {
        let asks = (1...12).map { _ in ReviewPrompt.countLaunch(defaults, arguments: []) }
        XCTAssertEqual(asks.firstIndex(of: true), 9)
        XCTAssertEqual(asks.filter { $0 }.count, 1)
    }

    func testDemoLaunchesNeitherCountNorAsk() {
        for _ in 1...12 { XCTAssertFalse(ReviewPrompt.countLaunch(defaults, arguments: ["-demoTab", "home"])) }
        XCTAssertEqual(defaults.integer(forKey: ReviewPrompt.launchesKey), 0)
    }

    func testGuideShowsOnceAndNeverOnDemoLaunches() {
        XCTAssertTrue(ReaderGuide.pending(defaults, arguments: []))
        XCTAssertFalse(ReaderGuide.pending(defaults, arguments: ["-demoCurl", "0.4"]))
        ReaderGuide.done(defaults)
        XCTAssertFalse(ReaderGuide.pending(defaults, arguments: []))
    }

    func testAutoColumnsSwitchAt560LikeAndroid() {
        let auto = ColumnSetting.auto.rawValue
        XCTAssertEqual(ReaderMetrics.columnCount(setting: auto, in: CGSize(width: 559, height: 800)), 1)
        XCTAssertEqual(ReaderMetrics.columnCount(setting: auto, in: CGSize(width: 561, height: 800)), 2)
    }

    func testReaderFollowsThePhoneUntilAChoiceIsSaved() {
        XCTAssertTrue(ReaderSettings.resolvedDark(saved: nil, systemDark: true))
        XCTAssertFalse(ReaderSettings.resolvedDark(saved: false, systemDark: true))
        XCTAssertTrue(ReaderSettings.resolvedDark(saved: true, systemDark: false))
    }

    func testDefaultSideMarginIs20LikeAndroid() {
        XCTAssertEqual(ReaderMetrics.hInset, 20)
    }
}
