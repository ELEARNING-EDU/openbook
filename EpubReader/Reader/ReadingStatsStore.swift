import Foundation

/// One day of reading. Seconds are kept apart by kind so the detail sheet can
/// later show eyes-on-page and listening side by side, while the goal and the
/// streak count both.
struct ReadingDay: Codable, Equatable {
    var seconds: Double = 0
    var listeningSeconds: Double = 0

    var total: Double { seconds + listeningSeconds }
}

/// The daily goal, shared by the Home card, the sheet and the app settings.
enum ReadingGoal {
    static let storageKey = "readingGoalMinutes"
    static let defaultMinutes = 5
    /// Apple Books opens at 5 minutes a day and lets the reader move it; the same
    /// value defines a reading day and the streak.
    static let options = [1, 5, 10, 15, 30, 60, 120]
}

/// The reader's own record: how long each local day was spent in a book.
///
/// Time is bucketed by the day it ends in, split at local midnight, so a session
/// that runs past midnight never lands wholly on one side. Only a day with a
/// recorded second exists in `days`; a day off is simply absent.
@MainActor
final class ReadingStatsStore: ObservableObject {

    static let shared = ReadingStatsStore()

    @Published private(set) var days: [String: ReadingDay] = [:]
    @Published private(set) var longestSession: TimeInterval = 0
    @Published private(set) var sessionCount = 0

    private static let key = "readingStats"

    private let defaults: UserDefaults
    private let calendar: Calendar
    private let idleLimit: TimeInterval

    init(
        defaults: UserDefaults = .standard,
        calendar: Calendar = .current,
        idleLimit: TimeInterval = 300
    ) {
        self.defaults = defaults
        self.calendar = calendar
        self.idleLimit = idleLimit
        load()
    }

    // MARK: - Reading the record

    /// `yyyy-MM-dd` in the store's own calendar. Not a `DateFormatter`: this runs
    /// on every page turn and must not depend on process-wide formatter state.
    func dayKey(for date: Date) -> String {
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        return String(
            format: "%04d-%02d-%02d",
            parts.year ?? 0, parts.month ?? 0, parts.day ?? 0
        )
    }

    func minutes(on day: Date) -> Double {
        (days[dayKey(for: day)]?.total ?? 0) / 60
    }

    /// Seconds recorded for whole days starting at `from` and ending before `to`.
    func total(from: Date, to: Date) -> TimeInterval {
        var recorded: TimeInterval = 0
        var cursor = calendar.startOfDay(for: from)
        while cursor < to {
            recorded += days[dayKey(for: cursor)]?.total ?? 0
            guard let next = calendar.date(byAdding: .day, value: 1, to: cursor) else { break }
            cursor = next
        }
        return recorded
    }

    // MARK: - Writing the record

    /// Adds a slice of reading that ended at `date`, walking backwards across
    /// midnight when the slice began on an earlier day.
    func add(seconds: TimeInterval, listening: Bool, endingAt date: Date) {
        guard seconds > 0 else { return }

        var remaining = seconds
        var end = date
        var updated = days

        while remaining > 0.000_001 {
            // The instant just before `end` decides which day the last second
            // belongs to, so a slice ending exactly at 00:00 stays on the day
            // that just finished.
            let reference = end.addingTimeInterval(-0.000_001)
            let dayStart = calendar.startOfDay(for: reference)
            let secondsIntoDay = reference.timeIntervalSince(dayStart) + 0.000_001
            let take = min(remaining, secondsIntoDay)
            let key = dayKey(for: reference)

            var day = updated[key] ?? ReadingDay()
            if listening {
                day.listeningSeconds += take
            } else {
                day.seconds += take
            }
            updated[key] = day

            remaining -= take
            end = dayStart
        }

        days = updated
        persist()
    }

    // MARK: - Sessions

    private var sessionStart: Date?
    private var lastEvent: Date?
    private var lastActivity: Date?
    private var isListening = false
    private var sessionSeconds: TimeInterval = 0

    /// Opens a session if none is open. The reader calls this on appear and on
    /// returning to the foreground; opening twice must not start a second one.
    func startSession(at date: Date) {
        guard sessionStart == nil else { return }
        sessionStart = date
        lastEvent = date
        lastActivity = date
        isListening = false
        sessionSeconds = 0
    }

    /// A page turn, a tap or a spoken sentence: the reader is still at the book.
    func noteActivity(at date: Date) {
        guard sessionStart != nil else { return }
        flush(to: date)
        lastActivity = date
    }

    /// Narration is its own kind of attendance: it keeps counting while the
    /// reader does not touch the page.
    func setListening(_ active: Bool, at date: Date) {
        guard sessionStart != nil else { return }
        flush(to: date)
        isListening = active
        lastActivity = date
    }

    func endSession(at date: Date) {
        guard sessionStart != nil else { return }
        flush(to: date)

        if sessionSeconds >= 1 {
            longestSession = max(longestSession, sessionSeconds)
            sessionCount += 1
        }

        sessionStart = nil
        lastEvent = nil
        lastActivity = nil
        isListening = false
        sessionSeconds = 0
        persist()
    }

    /// Writes the time since the last event. Reading slots stop at the idle
    /// limit; listening slots run to the event. Everything after an idle gap is
    /// skipped once, so it can never be counted by a later flush.
    private func flush(to now: Date) {
        guard let last = lastEvent, now > last else { return }

        let sliceEnd: Date
        if isListening {
            sliceEnd = now
        } else {
            sliceEnd = min(now, (lastActivity ?? last).addingTimeInterval(idleLimit))
        }

        let slice = max(0, sliceEnd.timeIntervalSince(last))
        if slice > 0 {
            add(seconds: slice, listening: isListening, endingAt: sliceEnd)
            sessionSeconds += slice
        }
        lastEvent = now
    }

    // MARK: - Storage

    private struct Snapshot: Codable {
        var days: [String: ReadingDay]
        var longestSession: TimeInterval
        var sessionCount: Int
    }

    private func load() {
        guard let data = defaults.data(forKey: Self.key),
              let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data)
        else { return }
        days = snapshot.days
        longestSession = snapshot.longestSession
        sessionCount = snapshot.sessionCount
    }

    private func persist() {
        let snapshot = Snapshot(
            days: days,
            longestSession: longestSession,
            sessionCount: sessionCount
        )
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        defaults.set(data, forKey: Self.key)
    }
}

/// A run of days that reached the goal, plus the best run ever recorded.
struct ReadingStreak: Equatable {
    let current: Int
    let longest: Int
    let daysRead: Int
}

/// Everything the Home card and the sheet need, computed once per render.
struct ReadingSummary: Equatable {
    let todaySeconds: Double
    let todayMinutes: Int
    let weekMinutes: Int
    let monthMinutes: Int
    let yearMinutes: Int
    let goalMinutes: Int
    let todayReachedGoal: Bool
    let streak: ReadingStreak
    let longestSessionMinutes: Int
    let bestDay: Date?
    let bestDayMinutes: Int
}

extension ReadingStatsStore {

    func streak(now: Date, goalMinutes: Int) -> ReadingStreak {
        let threshold = TimeInterval(goalMinutes * 60)
        let qualifying = Set(
            days
                .filter { $0.value.total >= threshold }
                .keys
                .compactMap(date(fromKey:))
                .map { calendar.startOfDay(for: $0) }
        ).sorted()

        var longest = 0
        var run = 0
        var previous: Date?
        for day in qualifying {
            if let previous,
               calendar.dateComponents([.day], from: previous, to: day).day == 1 {
                run += 1
            } else {
                run = 1
            }
            longest = max(longest, run)
            previous = day
        }

        func reached(_ day: Date) -> Bool {
            (days[dayKey(for: day)]?.total ?? 0) >= threshold
        }

        var cursor = calendar.startOfDay(for: now)
        if !reached(cursor) {
            cursor = calendar.date(byAdding: .day, value: -1, to: cursor) ?? cursor
        }
        var current = 0
        while reached(cursor) {
            current += 1
            guard let previous = calendar.date(byAdding: .day, value: -1, to: cursor) else { break }
            cursor = previous
        }

        return ReadingStreak(current: current, longest: longest, daysRead: qualifying.count)
    }

    func summary(now: Date, goalMinutes: Int) -> ReadingSummary {
        let today = calendar.startOfDay(for: now)
        let todaySeconds = days[dayKey(for: today)]?.total ?? 0

        func minutes(in interval: DateInterval?) -> Int {
            guard let interval else { return 0 }
            return Int((total(from: interval.start, to: interval.end) / 60).rounded())
        }

        let best = days.max { $0.value.total < $1.value.total }
        let bestDate = best.flatMap { date(fromKey: $0.key) }
        let goalSeconds = TimeInterval(goalMinutes * 60)

        return ReadingSummary(
            todaySeconds: todaySeconds,
            todayMinutes: Int((todaySeconds / 60).rounded()),
            weekMinutes: minutes(in: calendar.dateInterval(of: .weekOfYear, for: now)),
            monthMinutes: minutes(in: calendar.dateInterval(of: .month, for: now)),
            yearMinutes: minutes(in: calendar.dateInterval(of: .year, for: now)),
            goalMinutes: goalMinutes,
            todayReachedGoal: todaySeconds >= goalSeconds,
            streak: streak(now: now, goalMinutes: goalMinutes),
            longestSessionMinutes: Int((longestSession / 60).rounded()),
            bestDay: bestDate.map { calendar.startOfDay(for: $0) },
            bestDayMinutes: Int(((best?.value.total ?? 0) / 60).rounded())
        )
    }

    private func date(fromKey key: String) -> Date? {
        let parts = key.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(
            year: parts[0], month: parts[1], day: parts[2]
        ))
    }
}
