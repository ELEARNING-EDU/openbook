# Reading stats, goals and "finished" — design

Date: 2026-10-25
Owner: project owner (iOS OpenBook)
Status: proposed, waiting for approval

## Intent

The Home tab currently only offers the books in progress. The owner wants the reader to see
their reading achievement: time read today / this week / this month / this year, in the style
of Apple Books (daily minutes goal + streak), and to be able to mark a book as finished by
hand. Charts ("diagram") come later; this design leaves room for them.

Why: the app is a reading place, not a shop. The only feedback a reader gets today is "page 3
of 4". A small, honest reading record makes the habit visible without turning the app into a
game or a social network.

## Goals

1. Record reading time locally and show it on Home: today, week, month, year.
2. A daily goal (default 5 minutes, adjustable) and a streak counted in days that reach it —
   Apple Books' rule: default goal 5 min, streak = consecutive days meeting the goal.
3. A compact Home card (style A) that opens a "Reading achievement" sheet with totals, records
   and the books finished this year.
4. Let the reader mark a book finished / not finished, from the library; finished books leave
   the Home carousel and count in stats.
5. Vietnamese and English strings, current visual language, no server, no notifications.

## Non-goals (this phase)

- Charts, heatmap, bar/line diagrams, yearly recap, share cards. (Phase 2; the stored data
  already allows them.)
- Reading speed / pages-per-minute, social features, cloud sync, reminders/notifications.
- Backfilling history: stats start when the feature first runs. Existing positions stay as they
  are; no retroactive minutes.
- Counting time when the app is not in the foreground (there is no background audio mode).
- Reworking the library grid or reader chrome.

## Anti-goals

- A card that pushes "continue reading" below the fold or makes Home feel like a dashboard.
- Guilt mechanics: red broken-streak badges, loss framing, nagging. A missed day is a missed
  day.
- Fake precision: no "pages read" because the app re-paginates by type size and screen, so the
  number is not comparable between devices or settings.
- Gold-plating: goals per book, per genre, badges, levels.

## Constraints from the current app

- `ReadingProgressStore` keeps one position per book (`readerPositions` in `UserDefaults`); it
  has no time history, so a new store is needed.
- Home (`HomeView` + `HomeFeed.continueReading`) shows books with `0 < fraction < 1`, ordered by
  last update. Finished books are simply `fraction >= 1` today; there is no manual "finished".
- Library metadata (`LibraryMetadata`) already persists per book and is the natural place for a
  finished date.
- Visual language: paper background, serif headings, uppercase 12 pt section labels, capsule
  fills, accent terracotta, card fill, dark mode via `ReaderPalette.shell`. All new UI must use
  the palette, not system colors.
- Reader lifecycle: `ReaderView.onAppear` / `onDisappear` / `scenePhase` already exist and are
  where a session can start and stop. `SpeechController.state` gives playing/paused/idle.

## Approaches considered

- **A. Goal card + sheet (chosen).** A compact card above "Đang đọc" with a daily ring, today's
  minutes, streak, and a week/month/year line; tapping it opens a sheet with totals and records.
  Charts come later. Keeps Home about the current book while making the habit visible.
- **B. Neutral stats strip.** Three or four numbers plus a sparkline, no goal, no streak. Least
  pressure, but no answer to "am I keeping the habit?", and the owner picked a goal-based model.
- **C. Full reading journal on Home.** Inline heatmap and charts. Strong identity, but it pushes
  the continue-reading shelf down and is the heaviest thing to keep fast; postponed to phase 2
  as the sheet content instead.

Chosen: **A**, because the daily goal is the smallest complete answer to "how am I doing", the
detail can grow into charts without redesigning Home, and it stays out of the reading path.

## Product rules

### Time

- A **reading session** starts when a book's reader is on screen and the app is active; it ends
  when the reader is dismissed, the app leaves the foreground, or the app is terminated.
- **Idle pause:** when narration is idle and no page has been turned for 5 minutes, accumulation
  stops; the next page turn (or starting narration) resumes it. This keeps "left the book open
  on the table" from counting as reading.
- **Listening (TTS)** is recorded in its own bucket and included in the displayed totals. The
  sheet shows the read/listen split only when listening is non-zero.
- **Midnight:** a session crossing local midnight is split between the two days. Day keys are
  `yyyy-MM-dd` in the user's calendar at the moment of writing.
- Time is stored in seconds (Double), rounded for display: under 60 minutes → "12 phút"; 60+ →
  "1 giờ 20 phút". No seconds shown.

### Daily goal and streak

- One setting: **daily goal**, default **5 minutes**, options 1 / 5 / 10 / 15 / 30 / 60 / 120.
- A **reading day** = a day whose total (read + listened) reaches the goal. The same threshold
  defines the streak, exactly like Apple Books.
- **Current streak**: consecutive days ending today if today has reached the goal, otherwise
  ending yesterday (today still has a chance). **Longest streak**: the best run ever recorded.
- Days with no record are zero; there is no "rest day" credit in this phase.
- Changing the goal applies to future and past days alike when computing the streak (it is
  derived, not stored). This is the Apple Books behaviour and keeps the model simple; the
  record sheet states the current goal so the number is explainable.

### Finished books

- Manual action only in this phase: library book menu gets **"Đánh dấu đã đọc xong"**, and once
  finished, **"Bỏ đánh dấu đã đọc xong"**.
- Finishing stores `finishedAt = now` in the book's metadata, leaves the reading position
  untouched, and does not add reading time.
- Finished books: shown with a "Đã đọc xong" badge instead of a percentage in the library;
  excluded from the Home continue-reading carousel (`HomeFeed` filters them out); counted in
  "sách đọc xong năm nay" by `finishedAt` year; unmarking restores normal behaviour.
- Reopening a finished book resumes where it was, like any book.
- Deleting a book removes its finished date with the rest of its metadata (existing path).
- No automatic finishing at 100% in this phase (reaching the last page does not mean finished
  for a reader who skipped a chapter); this can be added later if the owner wants.

## Data model

New file `EpubReader/Reader/ReadingStatsStore.swift` (name may move):

```swift
struct ReadingDay: Codable, Equatable {
    var seconds: Double          // eyes on the page
    var listeningSeconds: Double // TTS while the reader is on screen
    var total: Double { seconds + listeningSeconds }
}

@MainActor final class ReadingStatsStore: ObservableObject {
    static let shared = ReadingStatsStore()
    @Published private(set) var days: [String: ReadingDay]   // "yyyy-MM-dd" -> day
    @Published private(set) var longestSession: Double       // seconds, record
    @Published private(set) var sessionCount: Int

    func add(seconds: Double, listening: Bool, at date: Date)  // splits at midnight
    func finishSession(started: Date, ended: Date)             // used by the reader
    func total(from: Date, to: Date) -> Double
    func streak(asOf: Date, goalMinutes: Int) -> (current: Int, longest: Int)
    func minutes(on: Date) -> Double
}
```

- Persisted as JSON under `readingStats` in `UserDefaults` beside the existing stores. Size is
  ~40 bytes/day; 5 years is under 100 KB. Pruning is not needed in this phase.
- `finishedAt: Date?` added to `LibraryMetadata` (Codable; missing key decodes as nil, so old
  data loads unchanged). `LibraryStore` gains `markFinished(id:finished:)`.
- Goal stored in `AppSettingsView`'s defaults: `readingGoalMinutes` (Int, default 5).

Session wiring in `ReaderView`:

- On appear / scene active: `stats.startSession(bookID:)` (timestamp kept in memory).
- On disappear / scene inactive / termination: `stats.endSession(at:)` writes the elapsed time.
- On page turn, tap, or narration start: `stats.noteActivity()` resets the idle clock.
- TTS: elapsed listening time recorded the same way, in the listening bucket; toggling TTS does
  not start a new session.
- The store takes an injectable clock (`now: () -> Date`) for tests.

## UI

### Home card (above the carousel)

```
THÀNH TÍCH
┌────────────────────────────────────────────┐
│  ◔   12 phút hôm nay          🔥 6 ngày    │
│  5/5 phút · Đã đạt mục tiêu                │
│  Tuần 1g20 · Tháng 4g15 · Năm 21g          │
└────────────────────────────────────────────┘
```

- Section label "THÀNH TÍCH" styled like "ĐANG ĐỌC".
- Left: 44 pt ring using accent for today's progress toward the goal (clamped at 1), a check
  glyph inside when the goal is met.
- Right: current streak with a small flame; if the streak is 0, "Chưa có chuỗi" in soft ink.
- Middle line: goal and state ("còn 3 phút", "Đã đạt mục tiêu").
- Bottom line: week / month / year totals, separated by middots.
- Whole card is one button; tapping opens the achievement sheet.
- First run (no data): 0 minutes, ring empty, same layout; the card is hidden only when the
  library has no books.
- Palette: `cardFill`, `swiftInk`, `swiftInkSoft`, `swiftAccent`; no system colors.

### Achievement sheet

- Medium detent (~60%) with drag handle, expandable; `ReaderPalette.shell` for the surface.
- Header: "Thành tích đọc" + close, and the current goal ("Mục tiêu 5 phút/ngày") with an
  "Đổi mục tiêu" link that opens the app settings.
- Phase 1 content, top to bottom:
  1. Four tiles: Hôm nay / Tuần này / Tháng này / Năm nay (minutes → hours formatting).
  2. Streak row: current streak, longest streak, number of reading days (all time).
  3. Records: longest session, best day (date + minutes).
  4. Books finished this year: covers in a row with title and finish month; "Chưa có sách nào"
     when empty.
- Phase 2 (later, same sheet): week/month/year bar chart, 52-week heatmap, read/listen split.
  The layout leaves a slot under the tiles for it; no placeholder in phase 1.

### Library changes

- Book menu adds the finish toggle above Edit (finish, edit, delete). Same `Menu` location.
- Finished row/grid cell: "Đã đọc xong" in accent instead of the percentage; no progress bar.
- No new confirmation dialog: the action is reversible from the same menu.

### Settings

- New section in `AppSettingsView`: "Thành tích đọc" → "Mục tiêu mỗi ngày" with the seven
  options in a menu/picker, and a footer explaining that a reading day and the streak are
  counted against this goal. A "Xoá dữ liệu đọc" (clear stats) action is proposed but optional;
  include only if the owner wants it (destructive, needs confirmation).

## Localization

All strings new keys in both `en` and `vi` tables (same keys):
`home.stats.title`, `home.stats.today`, `home.stats.goal`, `home.stats.met`, `home.stats.remaining`
(format), `home.stats.streak`, `home.stats.noStreak`, `home.stats.week`, `home.stats.month`,
`home.stats.year`, `stats.title`, `stats.today`, `stats.week`, `stats.month`, `stats.year`,
`stats.streak.current`, `stats.streak.longest`, `stats.daysRead`, `stats.longestSession`,
`stats.bestDay`, `stats.finished`, `stats.finished.empty`, `stats.goalLink`,
`library.markFinished`, `library.unmarkFinished`, `library.finished`,
`settings.reading.title`, `settings.reading.goal`, `settings.reading.footer`.

Duration formatting goes through one helper (locale-aware hour/minute words), not string
concatenation in views.

## Accessibility

- Card: one accessibility element with a sentence, e.g. "Hôm nay 12 phút, mục tiêu 5 phút, đã
  đạt. Chuỗi 6 ngày. Tuần 1 giờ 20 phút…" and a button trait.
- Ring is decorative to VoiceOver (the sentence carries it).
- Sheet tiles read label + value; covers read book title + month.
- Menu item uses the same wording as the visible label.
- Minimum 44 pt touch targets; Dynamic Type respected for text lines (the card's numbers use
  scalable text styles, not fixed sizes, where possible).

## Tests

Unit (no UI):

1. Seconds added to the correct day; a session crossing midnight splits at 00:00 local.
2. Idle pause: no accumulation after 5 minutes of no activity while TTS is idle; activity
   resumes it; TTS playing counts continuously.
3. Goal met at exactly the threshold; minutes below it are not a reading day.
4. Current streak: today met → includes today; today not met but yesterday met → ends
   yesterday; a gap breaks it; longest streak from history; goal change re-derives.
5. Totals for week/month/year use calendar boundaries (Mon-first week per locale) and exclude
   out-of-range days.
6. `markFinished` sets/clears `finishedAt`, persists across a new `LibraryStore`, excludes the
   book from `HomeFeed`, and counts it in the finished-year list.
7. Old `LibraryMetadata` JSON without `finishedAt` still decodes.
8. Duration formatting: minutes vs hours, Vietnamese and English.

Manual/UI checks on the simulator: card on first run, card after a session, sheet, menu toggle,
finished badge, English/Vietnamese, light/dark shell.

## Acceptance criteria

- [ ] Home shows the achievement card above the carousel; hidden only when the library is empty.
- [ ] Card shows today's minutes, goal state, streak, and week/month/year totals.
- [ ] Reading in the reader accumulates time on the right day; leaving the app stops it; idle
      5 minutes stops it until the next page turn; TTS adds listening time.
- [ ] Goal default 5 minutes, changeable in settings; streak and reading days follow it.
- [ ] Sheet shows the four totals, streaks, reading days, records and finished books this year.
- [ ] A book can be marked finished and unmarked from the library menu; finished books show the
      100% state, leave the Home carousel and count in the year's finished list.
- [ ] All new text exists in both languages; keys match.
- [ ] No charts in this phase; the stored data supports them for phase 2.
- [ ] Tests above pass; no change to reader layout, pagination or TTS behaviour.

## What is left to the builder

- Exact ring drawing and spacing to match the existing card language.
- Where to keep the sheet's period tiles (all four at once vs a segmented control) if the four
  labels feel crowded on small screens.
- Whether to add "clear reading data" now; if added, its confirmation copy.
- Whether the Home card should show a small 7-day sparkline before phase 2 (a decision for the
  owner, not required by this design).
