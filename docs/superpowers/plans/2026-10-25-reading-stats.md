# Reading Stats Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Home tab a reading-achievement card (today/week/month/year time, 5-minute daily goal, streak) with a detail sheet, and let the reader mark a book as finished.

**Architecture:** A new `ReadingStatsStore` keeps per-day seconds (reading and listening) and the session/record totals in `UserDefaults`, fed by `ReaderView`'s existing lifecycle. `LibraryMetadata` grows a `finishedAt` date, `HomeFeed` filters finished books out of continue-reading, and two new Home views (`ReadingStatsCard`, `ReadingStatsSheet`) read the store through small, unit-tested pure helpers (`summary`, `streak`, `duration`).

**Tech Stack:** Swift 5.9+, SwiftUI, Foundation, XCTest. No third-party dependencies.

**Spec:** `docs/superpowers/specs/2026-10-25-reading-stats-design.md`

## Global Constraints

- No new dependencies, no networking; stats stay on the device.
- All new text exists in both `EpubReader/Resources/en.lproj/Localizable.strings` and `vi.lproj` with identical keys; `LocalizationTests.testStringTablesHaveTheSameKeys` must stay green.
- New UI uses `ReaderPalette.shell(dark:)` (`swiftPaper`, `swiftInk`, `swiftInkSoft`, `swiftAccent`, `cardFill`), the existing serif/rounded type styles and 44 pt minimum touch targets. No system default colors.
- Time is stored in seconds, displayed in whole minutes; under 60 minutes shows "N phút", 60+ shows "N giờ M phút" via `ReadingStatsText.duration`, never string concatenation in a view.
- Day buckets use the injected `Calendar`; the app passes `.current`, tests pass a UTC Gregorian calendar.
- Charts, heatmaps, notifications, social and backfill are out of scope for this plan.
- Do not commit until the owner approves; the first task's commit step asks once and later tasks reuse that permission.
- Keep the existing 54 tests passing; run the full suite in Task 10.
- Test command (simulator UDID from this machine):
  `xcodebuild test -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug -destination 'platform=iOS Simulator,id=6B98AE70-9B11-4915-85AB-A3F347F40381' CODE_SIGNING_ALLOWED=NO -only-testing:EpubReaderTests/<Suite>`

## Review Focus

Inputs/conditions the spec implies but whose tests are easy to miss; each is pinned in the task named.

1. **Reader left open and idle** — after 5 minutes with no page turn and narration idle, time must stop counting (Task 3).
2. **Session crossing local midnight** — the slice must split between the two days, not land wholly on one (Task 1).
3. **Goal changed after history exists** — streak and reading days recompute against the new goal; nothing stale is stored (Task 2).
4. **Finished book** — must leave the Home carousel, show the finished label instead of a percentage, and be undoable (Tasks 5, 6).
5. **Old `LibraryMetadata` JSON without `finishedAt`** — must decode and keep title/author/import date (Task 5).

---

### Task 1: ReadingStatsStore — day buckets, totals, persistence

**Files:**
- Create: `EpubReader/Reader/ReadingStatsStore.swift`
- Create: `EpubReaderTests/ReadingStatsStoreTests.swift`

**Interfaces:**
- Consumes: nothing (Foundation only).
- Produces:
  - `struct ReadingDay: Codable, Equatable { var seconds: Double; var listeningSeconds: Double; var total: Double { get } }`
  - `@MainActor final class ReadingStatsStore: ObservableObject`
    - `static let shared`, `init(defaults: UserDefaults = .standard, calendar: Calendar = .current, idleLimit: TimeInterval = 300)`
    - `@Published private(set) var days: [String: ReadingDay]` (key `yyyy-MM-dd`)
    - `func add(seconds: TimeInterval, listening: Bool, endingAt: Date)`
    - `func total(from: Date, to: Date) -> TimeInterval` (from inclusive, to exclusive)
    - `func minutes(on day: Date) -> Double`
    - `func dayKey(for date: Date) -> String` (internal, tests use it)

- [ ] **Step 1: Write the failing tests**

```swift
// EpubReaderTests/ReadingStatsStoreTests.swift
@MainActor
final class ReadingStatsStoreTests: XCTestCase {
    private var defaults: UserDefaults!
    private var calendar: Calendar!   // UTC, gregorian
    private var store: ReadingStatsStore!
    // setUpWithError: unique UserDefaults suite, calendar with timeZone UTC,
    // store = ReadingStatsStore(defaults: defaults, calendar: calendar)
    // tearDown: removePersistentDomain

    func testSecondsLandOnTheEndingDay()
    func testASliceAcrossMidnightIsSplit()          // 600 s ending 00:05 → 300 s prev day, 300 s today
    func testReadingAndListeningAreKeptApart()      // add 300 read + 120 listening → total 420
    func testTotalsCoverOnlyTheRequestedRange()     // two days, range covers one
    func testDaysSurviveANewStore()                 // new store on same defaults reads the days back
}
```

Use fixed dates built with `DateComponents(year:month:day:hour:minute:second:)` in the UTC calendar, e.g. 2026-10-25 00:05:00 for the split test.

- [ ] **Step 2: Run tests to verify they fail**

Run the test command with `ReadingStatsStoreTests`; expected: compile error (types missing).

- [ ] **Step 3: Implement the store**

- `dayKey(for:)` formats `year-month-day` from `calendar.dateComponents`, zero-padded; no `DateFormatter` (thread-safe, locale-free).
- `add` splits the slice backwards from `endingAt`: bucket by the day that contains `endingAt`, take the seconds elapsed into that day, then move to the previous day for the remainder. A slice ending exactly at 00:00:00 belongs to the previous day.
- Persist a Codable snapshot `{days, longestSession: 0, sessionCount: 0}` under `"readingStats"`. Do not build the session fields yet beyond defaults.
- `total(from:to:)` walks day keys from `from` to `to` exclusive using `calendar.date(byAdding: .day, value: 1, to:)`.
- `@Published` must update after every `add`.

- [ ] **Step 4: Run tests to verify they pass**

Run `ReadingStatsStoreTests`; expected PASS (5 tests).

- [ ] **Step 5: Commit (ask the owner once first)**

```bash
git add EpubReader/Reader/ReadingStatsStore.swift EpubReaderTests/ReadingStatsStoreTests.swift
git commit -m "feat: record reading time per day"
```

---

### Task 2: Goal, streak, summary

**Files:**
- Modify: `EpubReader/Reader/ReadingStatsStore.swift`
- Modify: `EpubReaderTests/ReadingStatsStoreTests.swift`

**Interfaces:**
- Produces:
  - `struct ReadingStreak: Equatable { let current: Int; let longest: Int; let daysRead: Int }`
  - `struct ReadingSummary: Equatable { let todaySeconds: Double; let todayMinutes: Int; let weekMinutes: Int; let monthMinutes: Int; let yearMinutes: Int; let goalMinutes: Int; let todayReachedGoal: Bool; let streak: ReadingStreak; let longestSessionMinutes: Int; let bestDay: Date?; let bestDayMinutes: Int }`
  - `func streak(now: Date, goalMinutes: Int) -> ReadingStreak`
  - `func summary(now: Date, goalMinutes: Int) -> ReadingSummary`
  - `enum ReadingGoal { static let storageKey = "readingGoalMinutes"; static let defaultMinutes = 5; static let options = [1, 5, 10, 15, 30, 60, 120] }`

- [ ] **Step 1: Write the failing tests**

Add to `ReadingStatsStoreTests`:

```swift
func testGoalMetExactlyCounts()                     // goal 5 → 300 s is a reading day, 299 s is not
func testStreakEndsTodayWhenMet()
func testStreakEndsYesterdayWhenTodayIsNotYetMet()
func testGapBreaksStreakAndLongestIsKept()           // e.g. 3-day run, gap, 5-day run
func testChangingGoalRecomputesStreak()               // same days, goal 5 vs goal 30
func testSummaryBucketsUseCalendarBoundaries()       // fixed "now", Monday-start week (firstWeekday = 2)
func testNoDataSummaryIsZero()                       // all totals 0, streak 0, bestDay nil
```

Travel is not needed: `now` is a parameter.

- [ ] **Step 2: Run tests to verify they fail**

Run `ReadingStatsStoreTests`; expected: compile error on the new types.

- [ ] **Step 3: Implement streak and summary**

- A qualifying day is `day.total >= goalMinutes * 60` (exact equality qualifies).
- `current`: start at today; if today qualifies, count today and walk back; otherwise walk back from yesterday. Stop on the first non-qualifying day or a missing day.
- `longest`: sort qualifying day dates and count consecutive runs (`+1 day`); `daysRead` is the qualifying-day count.
- Summary ranges come from `calendar.dateInterval(of: .weekOfYear / .month / .year, for: now)` and `total(from:to:)`; `todaySeconds` is the unrounded day total so the card can compute minutes remaining; `bestDay` is the day with the largest total, nil when there is no data; `longestSessionMinutes` rounds seconds to the nearest minute.
- `ReadingGoal` lives in this file.

- [ ] **Step 4: Run tests to verify they pass**

Run `ReadingStatsStoreTests`; expected PASS (12 tests total).

- [ ] **Step 5: Commit**

```bash
git add EpubReader/Reader/ReadingStatsStore.swift EpubReaderTests/ReadingStatsStoreTests.swift
git commit -m "feat: derive reading streak and summary from daily totals"
```

---

### Task 3: Session accounting with idle pause and listening

**Files:**
- Modify: `EpubReader/Reader/ReadingStatsStore.swift`
- Modify: `EpubReaderTests/ReadingStatsStoreTests.swift`

**Interfaces:**
- Produces (on `ReadingStatsStore`):
  - `@Published private(set) var longestSession: TimeInterval`
  - `@Published private(set) var sessionCount: Int`
  - `func startSession(at: Date)` — idempotent while a session is open
  - `func noteActivity(at: Date)`
  - `func setListening(_ active: Bool, at: Date)`
  - `func endSession(at: Date)` — closes, persists, updates records
  - internal state: `sessionStart`, `lastEvent`, `lastActivity`, `listening`, `readingAccumulated`, `listeningAccumulated`

- [ ] **Step 1: Write the failing tests**

```swift
func testSessionCountsFromStartToEnd()          // start 10:00, activity at 10:02, end 10:10 → 7 min (2 + 5 idle cap)
func testIdleGapIsCappedAtFiveMinutes()          // start, note at +2, end at +40 → 7 min, not 40
func testIdleGapIsNotCountedTwiceAfterActivity() // activity at +2 and +20, end +40 → 2 + 5 + 5
func testListeningIsCountedWhileIdle()           // start, setListening(true), end +30 → 30 min listening, 0 reading
func testListeningDoesNotCountAsReading()        // 10 min reading, 5 min listening → split totals
func testSessionCrossingMidnightSplitsDays()     // 23:55 → 00:10 counts 5 min prev day, 10 min today
func testLongestSessionAndCountAreRecorded()     // two sessions, longest is the longer one
func testEmptySessionWritesNothing()             // start then end immediately → no day, no count
```

- [ ] **Step 2: Run tests to verify they fail**

Run `ReadingStatsStoreTests`; expected: compile error on the session API.

- [ ] **Step 3: Implement the accounting**

- Keep `lastEvent` and `lastActivity`. `flush(to:)` adds `now - lastEvent` to the bucket selected by `listening`; when not listening, the slice is capped at `lastActivity + idleLimit - lastEvent` (never negative), so an idle gap is trimmed once and never counted later.
- `noteActivity` flushes first, then sets `lastActivity`. `setListening` flushes, flips the flag, sets `lastActivity`. `endSession` flushes, adds to the day via `add(seconds:listening:endingAt:)` for each accumulated bucket (listening ending at `end`, reading by the same split), updates `longestSession`/`sessionCount` when the total is at least 1 second, persists, resets.
- Keep both accumulators separate so listening is never double-counted as reading.

- [ ] **Step 4: Run tests to verify they pass**

Run `ReadingStatsStoreTests`; expected PASS (20 tests).

- [ ] **Step 5: Commit**

```bash
git add EpubReader/Reader/ReadingStatsStore.swift EpubReaderTests/ReadingStatsStoreTests.swift
git commit -m "feat: count reading sessions with an idle pause"
```

---

### Task 4: Feed the store from the reader

**Files:**
- Modify: `EpubReader/Reader/ReaderView.swift` (state near the other stores; `.onAppear`, `.onDisappear`, `.onChange(of: scenePhase)`, `.onChange(of: pageIndex)`, `.onChange(of: speech.state)`, `.onChange(of: speech.highlighted)`)

**Interfaces:**
- Consumes: `ReadingStatsStore.shared`, `startSession(at:)`, `noteActivity(at:)`, `setListening(_:at:)`, `endSession(at:)`.

- [ ] **Step 1: Wire the lifecycle**

- Add `@ObservedObject private var stats = ReadingStatsStore.shared`.
- `.onAppear`: `stats.startSession(at: Date())`.
- `.onDisappear`: `stats.endSession(at: Date())`.
- Existing `scenePhase` block: for `.active` start a session, for anything else end it (keep the existing `savePosition()` calls).
- `.onChange(of: pageIndex)`: `stats.noteActivity(at: Date())`.
- `.onChange(of: speech.highlighted)`: `stats.noteActivity(at: Date())`.
- `.onChange(of: speech.state)`: `stats.setListening(speech.state == .playing, at: Date())`.

- [ ] **Step 2: Build and install**

Build, install, launch with `-demoTab library -demoOpenBook 1 -demoSpeak 1`, let narration run ~90 s, terminate the app.

- [ ] **Step 3: Verify the stored data**

Read the app preferences plist for the `readingStats` key; expected: today's bucket has `listeningSeconds > 60`, reading seconds may stay 0, and the app did not crash. Relaunch with narration paused; the bucket must not keep growing while the reader is closed.

- [ ] **Step 4: Commit**

```bash
git add EpubReader/Reader/ReaderView.swift
git commit -m "feat: record reading time while a book is open"
```

---

### Task 5: Finished books — data, feed, metadata migration

**Files:**
- Modify: `EpubReader/Library/LibraryMetadata.swift`
- Modify: `EpubReader/Library/LibraryStore.swift`
- Modify: `EpubReader/Home/HomeFeed.swift`
- Modify: `EpubReader/Home/HomeView.swift` (call site only)
- Modify: `EpubReaderTests/LibraryTests.swift`
- Modify: `EpubReaderTests/HomeFeedTests.swift`

**Interfaces:**
- Produces:
  - `LibraryMetadata.finishedAt: Date?` (Codable; missing key decodes to nil)
  - `LibraryStore.markFinished(id: String, finished: Bool) -> Bool`
  - `LibraryStore.finishedIDs: Set<String>` (computed from metadata)
  - `LibraryStore.finishedBooks(in year: Int, calendar: Calendar = .current) -> [BookItem]` (sorted by `finishedAt` descending)
  - `HomeFeed.continueReading(books:positions:finished:)` where `finished: Set<String>`; existing callers pass `library.finishedIDs`

- [ ] **Step 1: Write the failing tests**

```swift
// HomeFeedTests
func testFinishedBooksLeaveTheContinueCarousel()   // default [] keeps old behaviour

// LibraryTests (LibraryStoreTests)
func testMarkingFinishedPersistsAndCanBeUndone()
func testFinishedBooksAreListedByFinishYear()
func testOldMetadataWithoutFinishedAtStillDecodes() // hand-written JSON with title/author/importedAt only
```

- [ ] **Step 2: Run tests to verify they fail**

Run `HomeFeedTests` and `LibraryTests`; expected: compile errors (new parameter/method).

- [ ] **Step 3: Implement**

- Add `finishedAt` to `LibraryMetadata` with `var finishedAt: Date?` (synthesized Codable tolerates the missing key).
- `markFinished` sets `metadata[id]?.finishedAt = finished ? Date() : nil`, persists, reloads, returns false for an unknown id.
- `finishedIDs` = ids whose `finishedAt != nil`; `finishedBooks(in:)` filters by `calendar.component(.year, from: finishedAt)` and sorts descending.
- Filter `finished` inside `HomeFeed.continueReading` before the fraction test; update `HomeView` to pass `library.finishedIDs`.
- Deleting a book already drops the whole metadata entry; no extra cleanup.

- [ ] **Step 4: Run tests to verify they pass**

Run the full suite; expected: all previous tests plus the three new ones pass.

- [ ] **Step 5: Commit**

```bash
git add EpubReader/Library/LibraryMetadata.swift EpubReader/Library/LibraryStore.swift EpubReader/Home/HomeFeed.swift EpubReader/Home/HomeView.swift EpubReaderTests/LibraryTests.swift EpubReaderTests/HomeFeedTests.swift
git commit -m "feat: mark a book finished and keep it out of continue-reading"
```

---

### Task 6: Library menu and finished state

**Files:**
- Modify: `EpubReader/Library/LibraryView.swift`
- Modify: `EpubReader/Resources/en.lproj/Localizable.strings`, `.../vi.lproj/Localizable.strings`

**Interfaces:**
- Consumes: `library.markFinished(id:finished:)`, `library.metadata(for:)?.finishedAt`.
- Produces: keys `library.markFinished`, `library.unmarkFinished`, `library.finished`.

- [ ] **Step 1: Add the menu action**

In `bookMenu(_:)`, above "Edit": a button labelled `library.markFinished` when the book is unfinished, `library.unmarkFinished` when finished, calling `library.markFinished(id: book.id, finished: !isFinished)`. No confirmation.

- [ ] **Step 2: Replace the percentage when finished**

In `gridTile` and `listRow`, when `library.metadata(for: book.id)?.finishedAt != nil`, draw the accent-colored `library.finished` label instead of `ReadPercentLabel`. Update `bookLabel(_:)` to append the finished state for VoiceOver (new format key `library.book.finished.a11y` taking title, author).

- [ ] **Step 3: Add both-language strings**

vi: "Đánh dấu đã đọc xong", "Bỏ đánh dấu đã đọc xong", "Đã đọc xong". en: "Mark as finished", "Mark as not finished", "Finished".

- [ ] **Step 4: Build and check by hand**

Build, install, open the Library, mark a book finished (menu) in both grid and list; verify the badge replaces the percentage, the menu flips to "unmark", the book leaves Home's carousel, and unmarking restores it. Capture a screenshot for the report.

- [ ] **Step 5: Commit**

```bash
git add EpubReader/Library/LibraryView.swift EpubReader/Resources/en.lproj/Localizable.strings EpubReader/Resources/vi.lproj/Localizable.strings
git commit -m "feat: mark books finished from the library"
```

---

### Task 7: Duration text and the Home card

**Files:**
- Create: `EpubReader/Home/ReadingStatsCard.swift`
- Create: `EpubReader/Home/ReadingStatsText.swift`
- Modify: `EpubReader/Home/HomeView.swift`
- Modify: `EpubReader/Resources/en.lproj/Localizable.strings`, `.../vi.lproj/Localizable.strings`
- Modify: `EpubReaderTests/LocalizationTests.swift`

**Interfaces:**
- Consumes: `ReadingStatsStore.summary(now:goalMinutes:)`, `ReadingGoal`, palette.
- Produces:
  - `enum ReadingStatsText { static func duration(minutes: Int, language: AppLanguage = .selected) -> String }`
  - `struct ReadingStatsCard: View { init(summary: ReadingSummary, palette: ReaderPalette, action: () -> Void) }`
  - Home reads `@AppStorage(ReadingGoal.storageKey) private var goalMinutes = ReadingGoal.defaultMinutes`.

- [ ] **Step 1: Write the failing duration tests**

```swift
// LocalizationTests
func testDurationReadsInBothLanguages() {
    XCTAssertEqual(ReadingStatsText.duration(minutes: 0, language: .vietnamese), "0 phút")
    XCTAssertEqual(ReadingStatsText.duration(minutes: 12, language: .english), "12 min")
    XCTAssertEqual(ReadingStatsText.duration(minutes: 80, language: .vietnamese), "1 giờ 20 phút")
    XCTAssertEqual(ReadingStatsText.duration(minutes: 120, language: .english), "2 hr")
}
```

- [ ] **Step 2: Run test to verify it fails**

Run `LocalizationTests`; expected: `ReadingStatsText` not found.

- [ ] **Step 3: Implement the formatter and strings**

Keys: `duration.minutes` (`%d phút` / `%d min`), `duration.hours` (`%d giờ` / `%d hr`), `duration.hoursMinutes` (`%d giờ %d phút` / `%d hr %d min`). `duration` rounds to the nearest minute, minutes < 60 → minutes key, exact hours → hours key, else hours+minutes. Use `L10n.string(_:language:)` and `String(format:locale:arguments:)`.

- [ ] **Step 4: Implement the card**

Layout per the spec: "THÀNH TÍCH" label; a 44 pt accent ring (`Circle().trim`) clamped at the goal with a check when met; today's minutes in serif; streak line with flame when > 0, `home.stats.noStreak` otherwise; goal state line (met / "còn N phút"); week/month/year totals joined by "·". Remaining minutes = `ceil((goal * 60 - todaySeconds) / 60)`, never below 0, so a rounded-up "5 phút" cannot claim the 5-minute goal is met. Whole card in a `Button`; background `palette.cardFill` in a 16 pt rounded rect; horizontal padding 20, top 4. Accessibility: single element with the sentence built from the same values, `.isButton`.

- [ ] **Step 5: Put it on Home**

In `HomeView`, add `@ObservedObject private var stats = ReadingStatsStore.shared`, the `@AppStorage` goal, and show the card above the "ĐANG ĐỌC" section whenever `!library.books.isEmpty`; tappable state opens the sheet (Task 8). Keep the existing carousel/empty state unchanged. Strings: `home.stats.title`, `home.stats.today` (`%d` handled by formatter), `home.stats.goalMet`, `home.stats.remaining` (`Còn %@ để đạt mục tiêu` / `%@ to go`), `home.stats.streak`, `home.stats.noStreak`, `home.stats.week`, `home.stats.month`, `home.stats.year`.

- [ ] **Step 6: Build and check by hand**

Build, install, launch Home. With no data: ring empty, "0 phút hôm nay", "Chưa có chuỗi", totals 0. Use a temporary launch argument `-demoStatsSeed 1` (read in `HomeView.onAppear`, same style as the existing demo hooks) that calls `stats.add` for today/week/month/year, then capture the card in both languages and light/dark shells.

- [ ] **Step 7: Commit**

```bash
git add EpubReader/Home/ReadingStatsCard.swift EpubReader/Home/ReadingStatsText.swift EpubReader/Home/HomeView.swift EpubReader/Resources EpubReaderTests/LocalizationTests.swift
git commit -m "feat: show the reading achievement card on Home"
```

---

### Task 8: Achievement sheet

**Files:**
- Create: `EpubReader/Home/ReadingStatsSheet.swift`
- Modify: `EpubReader/Home/HomeView.swift` (presentation)
- Modify: strings files

**Interfaces:**
- Consumes: `ReadingStatsStore.summary`, `LibraryStore.finishedBooks(in:)`, `ReadingStatsText.duration`, palette.
- Produces: `struct ReadingStatsSheet: View { init(summary: ReadingSummary, finished: [BookItem], palette: ReaderPalette, onGoalTap: () -> Void) }`.

- [ ] **Step 1: Implement the sheet**

Header "Thành tích đọc" + close; goal line with a "Đổi mục tiêu" button calling `onGoalTap`. Four tiles (Hôm nay/Tuần này/Tháng này/Năm nay) in a 2×2 grid; streak block (current, longest, days read); records (longest session, best day with date + minutes; hide the best-day row when nil); "Sách đọc xong năm nay" with covers in a horizontal row and month captions, or `stats.finished.empty`. Medium detent, drag handle, `.preferredColorScheme` from the palette's dark flag. Accessibility: each tile/record is one element reading label and value; covers read title + finish month. No charts in this phase.

- [ ] **Step 2: Present it from Home**

`@State private var showStats = false`; card action sets it; `.sheet(isPresented:)` passes `stats.summary(now: .now, goalMinutes: goalMinutes)` and `library.finishedBooks(in: Calendar.current.component(.year, from: .now))`. `onGoalTap` dismisses the sheet and opens `AppSettingsView` (the existing sheet state).

- [ ] **Step 3: Strings**

`stats.title`, `stats.close`, `stats.goal` (`Mục tiêu %@/ngày`), `stats.changeGoal`, `stats.today`, `stats.week`, `stats.month`, `stats.year`, `stats.streak.current`, `stats.streak.longest`, `stats.daysRead`, `stats.longestSession`, `stats.bestDay`, `stats.bestDay.none`, `stats.finished`, `stats.finished.empty` — both languages.

- [ ] **Step 4: Build and check by hand**

Open the card, verify all values match the card and the seeded data, the finished-books row, light/dark, Vietnamese and English; capture screenshots.

- [ ] **Step 5: Commit**

```bash
git add EpubReader/Home/ReadingStatsSheet.swift EpubReader/Home/HomeView.swift EpubReader/Resources
git commit -m "feat: add the reading achievement sheet"
```

---

### Task 9: Goal setting

**Files:**
- Modify: `EpubReader/App/AppSettingsView.swift`
- Modify: strings files

**Interfaces:**
- Consumes: `ReadingGoal.options`, `ReadingGoal.storageKey`, `ReadingGoal.defaultMinutes`.
- Produces: `@AppStorage(ReadingGoal.storageKey) private var goalMinutes = ReadingGoal.defaultMinutes` in the settings view.

- [ ] **Step 1: Add the section**

A `Section` titled `settings.reading.title` with a `Picker` (`settings.reading.goal`) over `ReadingGoal.options`, label `settings.reading.minutes` (`%d phút/ngày` / `%d min a day`) rendered through `L10n.format`, and footer `settings.reading.footer` ("Một ngày đọc và chuỗi ngày được tính theo mục tiêu này." / "A reading day and the streak follow this goal."). Keep the existing language section untouched and the Form's palette styling.

- [ ] **Step 2: Build and check by hand**

Change the goal to 1 and 30 minutes; reopen Home and the sheet; confirm the ring, "còn N phút" and streak follow the new goal and the choice survives a relaunch.

- [ ] **Step 3: Commit**

```bash
git add EpubReader/App/AppSettingsView.swift EpubReader/Resources
git commit -m "feat: let the reader choose a daily reading goal"
```

---

### Task 10: Strings parity, full verification, checkpoints

**Files:**
- Modify: `PROJECT_STATE.md`, `SESSION_HANDOFF.md`

- [ ] **Step 1: Run the full suite**

Run the full `xcodebuild test` (no `-only-testing`); expected: all tests pass, including `LocalizationTests.testStringTablesHaveTheSameKeys` and the new 20 stats tests.

- [ ] **Step 2: Walk the acceptance list**

First run card; a real 60+ second reading session; background/foreground; idle pause; goal change; mark/unmark finished; sheet; both languages; light/dark shell. Capture screenshots into `build/qa/reading-stats/`.

- [ ] **Step 3: Check for regressions in the reading path**

Open a book, turn pages, start/stop narration, open/close the full player; verify nothing new appears in the reader and page turning is unchanged.

- [ ] **Step 4: Update the checkpoints**

Record commands, exit status, screenshots, what is still deferred (charts, notifications), and the honest gaps (no background listening, no retroactive history).

- [ ] **Step 5: Commit**

```bash
git add PROJECT_STATE.md SESSION_HANDOFF.md
git commit -m "docs: checkpoint reading stats"
```
