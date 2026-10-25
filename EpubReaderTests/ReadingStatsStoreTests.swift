import XCTest
@testable import EpubReader

@MainActor
final class ReadingStatsStoreTests: XCTestCase {

    private var defaults: UserDefaults!
    private var suiteName: String!
    private var calendar: Calendar!
    private var store: ReadingStatsStore!

    override func setUpWithError() throws {
        suiteName = "ReadingStatsStoreTests.\(UUID().uuidString)"
        defaults = UserDefaults(suiteName: suiteName)
        defaults.removePersistentDomain(forName: suiteName)

        calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        calendar.firstWeekday = 2

        store = ReadingStatsStore(defaults: defaults, calendar: calendar)
    }

    override func tearDownWithError() throws {
        defaults.removePersistentDomain(forName: suiteName)
        defaults = nil
        store = nil
        calendar = nil
        suiteName = nil
    }

    private func date(_ year: Int, _ month: Int, _ day: Int,
                      _ hour: Int = 0, _ minute: Int = 0, _ second: Int = 0) -> Date {
        calendar.date(from: DateComponents(
            year: year, month: month, day: day,
            hour: hour, minute: minute, second: second
        ))!
    }

    func testSecondsLandOnTheEndingDay() {
        store.add(seconds: 600, listening: false, endingAt: date(2026, 10, 25, 10, 0))

        XCTAssertEqual(store.dayKey(for: date(2026, 10, 25)), "2026-10-25")
        XCTAssertEqual(store.minutes(on: date(2026, 10, 25)), 10, accuracy: 0.001)
    }

    func testASliceAcrossMidnightIsSplit() {
        store.add(seconds: 600, listening: false, endingAt: date(2026, 10, 26, 0, 5))

        XCTAssertEqual(store.minutes(on: date(2026, 10, 25)), 5, accuracy: 0.001)
        XCTAssertEqual(store.minutes(on: date(2026, 10, 26)), 5, accuracy: 0.001)
    }

    func testReadingAndListeningAreKeptApart() {
        let day = date(2026, 10, 25, 12, 0)
        store.add(seconds: 300, listening: false, endingAt: day)
        store.add(seconds: 120, listening: true, endingAt: day)

        let recorded = store.days["2026-10-25"]
        XCTAssertEqual(recorded?.seconds ?? 0, 300, accuracy: 0.001)
        XCTAssertEqual(recorded?.listeningSeconds ?? 0, 120, accuracy: 0.001)
        XCTAssertEqual(recorded?.total ?? 0, 420, accuracy: 0.001)
    }

    func testTotalsCoverOnlyTheRequestedRange() {
        store.add(seconds: 600, listening: false, endingAt: date(2026, 10, 25, 10, 0))
        store.add(seconds: 600, listening: false, endingAt: date(2026, 10, 27, 10, 0))

        let firstDay = store.total(
            from: date(2026, 10, 25),
            to: date(2026, 10, 26)
        )
        XCTAssertEqual(firstDay, 600, accuracy: 0.001)
    }

    func testDaysSurviveANewStore() {
        store.add(seconds: 420, listening: false, endingAt: date(2026, 10, 25, 9, 30))

        let reopened = ReadingStatsStore(defaults: defaults, calendar: calendar)
        XCTAssertEqual(reopened.minutes(on: date(2026, 10, 25)), 7, accuracy: 0.001)
    }
}
