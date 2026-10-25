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
