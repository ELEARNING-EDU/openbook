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

    func testGoalMetExactlyCounts() {
        store.add(seconds: 300, listening: false, endingAt: date(2026, 10, 25, 12, 0))
        store.add(seconds: 299, listening: false, endingAt: date(2026, 10, 26, 12, 0))

        XCTAssertEqual(store.streak(now: date(2026, 10, 25, 22, 0), goalMinutes: 5).daysRead, 1)
        XCTAssertEqual(store.streak(now: date(2026, 10, 26, 22, 0), goalMinutes: 5).daysRead, 1)
    }

    func testStreakEndsTodayWhenMet() {
        for day in 23...25 {
            store.add(seconds: 600, listening: false, endingAt: date(2026, 10, day, 12, 0))
        }

        let streak = store.streak(now: date(2026, 10, 25, 22, 0), goalMinutes: 5)
        XCTAssertEqual(streak.current, 3)
    }

    func testStreakEndsYesterdayWhenTodayIsNotYetMet() {
        store.add(seconds: 600, listening: false, endingAt: date(2026, 10, 23, 12, 0))
        store.add(seconds: 600, listening: false, endingAt: date(2026, 10, 24, 12, 0))

        let streak = store.streak(now: date(2026, 10, 25, 8, 0), goalMinutes: 5)
        XCTAssertEqual(streak.current, 2)
    }

    func testGapBreaksStreakAndLongestIsKept() {
        for day in 18...20 {
            store.add(seconds: 600, listening: false, endingAt: date(2026, 10, day, 12, 0))
        }
        for day in 24...25 {
            store.add(seconds: 600, listening: false, endingAt: date(2026, 10, day, 12, 0))
        }

        let streak = store.streak(now: date(2026, 10, 25, 22, 0), goalMinutes: 5)
        XCTAssertEqual(streak.current, 2)
        XCTAssertEqual(streak.longest, 3)
        XCTAssertEqual(streak.daysRead, 5)
    }

    func testChangingGoalRecomputesStreak() {
        store.add(seconds: 600, listening: false, endingAt: date(2026, 10, 24, 12, 0))
        store.add(seconds: 600, listening: false, endingAt: date(2026, 10, 25, 12, 0))

        XCTAssertEqual(store.streak(now: date(2026, 10, 25, 22, 0), goalMinutes: 5).current, 2)
        XCTAssertEqual(store.streak(now: date(2026, 10, 25, 22, 0), goalMinutes: 15).current, 0)
        XCTAssertEqual(store.streak(now: date(2026, 10, 25, 22, 0), goalMinutes: 15).daysRead, 0)
    }

    func testSummaryBucketsUseCalendarBoundaries() {
        store.add(seconds: 600, listening: false, endingAt: date(2026, 10, 28, 9, 0))  // today
        store.add(seconds: 300, listening: false, endingAt: date(2026, 10, 27, 9, 0))  // this week
        store.add(seconds: 900, listening: false, endingAt: date(2026, 10, 25, 9, 0))  // Sunday, previous week
        store.add(seconds: 7200, listening: false, endingAt: date(2026, 10, 1, 9, 0))  // earlier this month
        store.add(seconds: 3600, listening: false, endingAt: date(2025, 12, 31, 9, 0)) // last year

        let summary = store.summary(now: date(2026, 10, 28, 12, 0), goalMinutes: 5)

        XCTAssertEqual(summary.todayMinutes, 10)
        XCTAssertEqual(summary.weekMinutes, 15)
        XCTAssertEqual(summary.monthMinutes, 150)
        XCTAssertEqual(summary.yearMinutes, 150)
        XCTAssertEqual(summary.todaySeconds, 600, accuracy: 0.001)
        XCTAssertTrue(summary.todayReachedGoal)
        XCTAssertEqual(summary.bestDayMinutes, 120)
        XCTAssertEqual(summary.bestDay, calendar.startOfDay(for: date(2026, 10, 1)))
    }

    func testNoDataSummaryIsZero() {
        let summary = store.summary(now: date(2026, 10, 25, 12, 0), goalMinutes: 5)

        XCTAssertEqual(summary.todayMinutes, 0)
        XCTAssertEqual(summary.weekMinutes, 0)
        XCTAssertEqual(summary.monthMinutes, 0)
        XCTAssertEqual(summary.yearMinutes, 0)
        XCTAssertEqual(summary.streak, ReadingStreak(current: 0, longest: 0, daysRead: 0))
        XCTAssertFalse(summary.todayReachedGoal)
        XCTAssertNil(summary.bestDay)
        XCTAssertEqual(summary.bestDayMinutes, 0)
        XCTAssertEqual(summary.longestSessionMinutes, 0)
    }

    func testDaysSurviveANewStore() {
        store.add(seconds: 420, listening: false, endingAt: date(2026, 10, 25, 9, 30))

        let reopened = ReadingStatsStore(defaults: defaults, calendar: calendar)
        XCTAssertEqual(reopened.minutes(on: date(2026, 10, 25)), 7, accuracy: 0.001)
    }

    func testSessionCountsFromStartToEnd() {
        store.startSession(at: date(2026, 10, 25, 10, 0))
        store.noteActivity(at: date(2026, 10, 25, 10, 2))
        store.endSession(at: date(2026, 10, 25, 10, 10))

        XCTAssertEqual(store.minutes(on: date(2026, 10, 25)), 7, accuracy: 0.001)
        XCTAssertEqual(store.sessionCount, 1)
    }

    func testIdleGapIsCappedAtFiveMinutes() {
        store.startSession(at: date(2026, 10, 25, 10, 0))
        store.noteActivity(at: date(2026, 10, 25, 10, 2))
        store.endSession(at: date(2026, 10, 25, 10, 40))

        XCTAssertEqual(store.minutes(on: date(2026, 10, 25)), 7, accuracy: 0.001)
    }

    func testIdleGapIsNotCountedTwiceAfterActivity() {
        store.startSession(at: date(2026, 10, 25, 10, 0))
        store.noteActivity(at: date(2026, 10, 25, 10, 2))
        store.noteActivity(at: date(2026, 10, 25, 10, 20))
        store.endSession(at: date(2026, 10, 25, 10, 40))

        XCTAssertEqual(store.minutes(on: date(2026, 10, 25)), 12, accuracy: 0.001)
    }

    func testListeningIsCountedWhileIdle() {
        store.startSession(at: date(2026, 10, 25, 10, 0))
        store.setListening(true, at: date(2026, 10, 25, 10, 0))
        store.endSession(at: date(2026, 10, 25, 10, 30))

        XCTAssertEqual(store.days["2026-10-25"]?.listeningSeconds ?? 0, 1800, accuracy: 0.001)
        XCTAssertEqual(store.days["2026-10-25"]?.seconds ?? 0, 0, accuracy: 0.001)
    }

    func testListeningDoesNotCountAsReading() {
        store.startSession(at: date(2026, 10, 25, 10, 0))
        store.noteActivity(at: date(2026, 10, 25, 10, 5))
        store.endSession(at: date(2026, 10, 25, 10, 10))
        store.startSession(at: date(2026, 10, 25, 11, 0))
        store.setListening(true, at: date(2026, 10, 25, 11, 0))
        store.endSession(at: date(2026, 10, 25, 11, 5))

        XCTAssertEqual(store.days["2026-10-25"]?.seconds ?? 0, 600, accuracy: 0.001)
        XCTAssertEqual(store.days["2026-10-25"]?.listeningSeconds ?? 0, 300, accuracy: 0.001)
    }

    func testSessionCrossingMidnightSplitsDays() {
        store.startSession(at: date(2026, 10, 25, 23, 55))
        store.noteActivity(at: date(2026, 10, 26, 0, 0))
        store.noteActivity(at: date(2026, 10, 26, 0, 5))
        store.endSession(at: date(2026, 10, 26, 0, 10))

        XCTAssertEqual(store.minutes(on: date(2026, 10, 25)), 5, accuracy: 0.001)
        XCTAssertEqual(store.minutes(on: date(2026, 10, 26)), 10, accuracy: 0.001)
    }

    func testLongestSessionAndCountAreRecorded() {
        store.startSession(at: date(2026, 10, 25, 10, 0))
        store.noteActivity(at: date(2026, 10, 25, 10, 5))
        store.endSession(at: date(2026, 10, 25, 10, 10))
        store.startSession(at: date(2026, 10, 25, 12, 0))
        store.noteActivity(at: date(2026, 10, 25, 12, 2))
        store.endSession(at: date(2026, 10, 25, 12, 5))

        XCTAssertEqual(store.longestSession, 600, accuracy: 0.001)
        XCTAssertEqual(store.sessionCount, 2)
    }

    func testEmptySessionWritesNothing() {
        store.startSession(at: date(2026, 10, 25, 10, 0))
        store.endSession(at: date(2026, 10, 25, 10, 0))

        XCTAssertTrue(store.days.isEmpty)
        XCTAssertEqual(store.sessionCount, 0)
    }
}
