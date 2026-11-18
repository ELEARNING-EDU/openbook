# Android ↔ iOS parity

Rule (owner, 2026-11-11): both apps match in design and features; every mobile change ships on both.
Every user-visible change gets a line in the shared `docs/CHANGELOG.md` (master); per-platform state/tasks/handoff stay in each branch's `docs/`.
Review 2026-11-11 compared UI strings (Android `values/strings.xml` 197 keys, iOS
`en.lproj/Localizable.strings` 221 keys) and the code behind each gap. Nothing below is fixed yet.

## Same on both

Tabs Home/Library/Store (offline demo store) · EPUB import, DRM/damaged-file errors · grid/list,
sort recent/title/author, edit title/author, multi-select delete, mark finished · Home: reading now,
newly added, suggestions (same author), recently finished, this week chart, goal/streak card ·
reader: curl turn, TOC, bookmarks, highlights tab, search (≥2 chars), 6 themes (Original, Quiet,
Paper, Bold, Calm, Focus) × light/dark, customize screen (font, bold, spacing, columns, justify,
reset) · read aloud: sentence highlight, auto turn, mini player on tabs, full player, speed,
voice picker with preview, sleep timer, background audio + lock-screen/notification controls ·
app language System/VI/EN · portrait only, ZIP size limits, bookmarks/highlights erased with the book.

## Gaps

| # | Item | Android | iOS | Proposed |
|---|------|---------|-----|----------|
| P1 | App name on home screen | `OpenBook` | `Books` | `OpenBook` on both |
| P2 | Brand colours | accent `#9E5932`, paper `#FBF7EE` | tint `#9E5932`, system grouped backgrounds | decide: keep terracotta or move to the new icon's brick `#C2410C` / cream `#FBF6EC` |
| P3 | First-run reader guide (4 steps) | yes | no | add to iOS |
| P4 | Reading stats detail sheet (longest streak, days read, records, finished this year) | data computed, no screen | yes | add screen to Android |
| P5 | App settings content | language, goal, fix spelling, sleep timer | language, goal | align list |
| P6 | Fix spelling on import (Vietnamese) | yes (off by default; open bugs T-119/120/121) | no | fix Android bugs first, then decide iOS |
| P7 | Highlight colours | 4 colours at alpha 0x66 | 4 named colours at alpha 0xB3, explicit Remove | one palette, names, Remove on both |
| P8 | In-app review prompt | Play review on 10th launch | none | add `requestReview` to iOS |
| P9 | Customize: discard/keep | live, X restores snapshot | Discard / Keep buttons | one pattern |
| P10 | Copy differences | "NEWLY ADDED", "SUGGESTED FOR YOU", "Read on" | "Recently added", "For you", "Continue reading" | one wording set, both languages |
| P11 | iPad / tablet | phone layout | iPhone + iPad target | decide scope |

## Code health (from review)

- Android T-119 (P1): `TextCleanup` bypasses the ZIP 64/256 MiB budget — same class of bug iOS fixed as T19.
- Android T-120, T-121 (P2): spelling cleanup deletes valid repeated words, edits `<style>/<pre>/<code>`.
- Both: shared EPUB corpus missing (Android 33 tests skip, iOS 5 skip) — one fixture set for both.
- Largest files: Android `ReaderScreen.kt` 2,357 lines, `Components.kt` 1,484; iOS `ReaderView.swift` 1,980.
  Split only when a parity change touches them.

## Decisions (owner, 2026-11-11)

- P1 name: `OpenBook` on both. P2: keep terracotta `#9E5932`; only the icon uses brick/cream.
- Do now: P3 guide + P8 review on iOS; P4 stats sheet on Android; P7 highlight palette + Remove,
  P9 customize pattern, P10 copy. Not now: P6 spelling fix (and T-119/120/121), P11 tablet.

## Progress

- 2026-11-11 iOS: P1 display name `OpenBook` (Info.plist + both InfoPlist.strings); P3 `Reader/ReaderGuide.swift`
  (one-time, skipped on `-demo…` launches, wired in `ReaderView`); P8 `App/ReviewPrompt.swift` + `requestReview`
  in `MainView`. Tests `ParityPromptTests` + `LocalizationTests` pass (iPhone 18 Pro sim). Not yet seen on screen.
- 2026-11-11 Android: P4 `ui/ReadingStatsSheet.kt` opened from Home's achievement card (goal link → app settings),
  `ReadingSummary.bestDayDate`; strings EN/VI. `ReadingLogTest` 11/11. Not yet seen on screen.
- 2026-11-11 both: P7 Android highlight hues = iOS (`#FFD54A #A5D66A #6FA8DC #F28DB2`, alpha stays 0x66 because
  Android draws over the text), spoken colour names, "Remove" label; P9 already the same behaviour (live apply,
  X restores, ✓ keeps) — Android button labels now "Discard/Keep changes"; P10 copy unified (VI: "đánh dấu" =
  bookmark, "tô sáng" = highlight). Left different on purpose: store body, delete-dialog body, voice search hint.
- Verified: Android `testDebugUnitTest` 138 / 33 skipped / 0 failed + `assembleDebug`; iOS full suite 142 / 5 skipped
  / 0 failed (iPhone 18 Pro). iOS guide card rendered to PNG and checked; system dialog shows "OpenBook".
  NOT checked on screen: Android stats sheet and highlight colours (both emulators busy with other projects),
  iOS review sheet (system decides), guide over a real book.
- Next: owner QA of the items above on a device; then commit in `android` and `ios` repos (not done).
- 2026-11-11 QA on E2 (`emulator-5554`, debug build, English UI): new icon on splash; Home copy; stats sheet opens
  from the card, all sections shown, "Change goal" opens App settings; reader guide; colour swatches read
  Yellow/Green/Blue/Pink; green band readable; long-press on a saved highlight → Highlight / Remove; removed the
  test highlight (prefs back to `[]`).
- Found, not fixed: (a) editing a saved highlight is long-press on Android, tap on iOS — pick one;
  (b) Android page footer reads "Trang 1 / 15" in the English UI (known, see design BRIEF).

## Batch 1 (owner, 2026-11-11) — from PLAN.md P0, delegated to the opencode agent in each worktree
- A. Saved highlight is edited by long-press only (owner rule); short tap = normal page tap. iOS change; guide text both.
- B. Android page footer follows app language.
- C. Open/share an `.epub` from other apps → existing import → open book.
- D. Selection menu: Copy, Share (“quote” — title, author), Search (prefilled), Translate (system).
- E. Note on a highlight + Markdown export (same format and test fixture on both).
- Later batches from PLAN.md: backup/restore, widget + reminder, landscape/tablet, then P1.
- Specs: scratchpad `spec-android.md` / `spec-ios.md` (shared section identical). Workers must not commit; Claude reviews,
  runs tests, does device QA, then commits.
- 2026-11-11 batch 1 done by both workers; Claude review OK (Markdown fixture byte-identical on both; note optional
  and backward compatible). One defect found on E2 and fixed by the Android worker: compact selection row
  Translate/Search were dead text. Tests rerun by Claude: Android 141/33 skipped/0 failed + assembleDebug;
  iOS 146/5 skipped/0 failed.
- E2 QA (Android, English UI): open `.epub` from Files → imported (419 ms) and opened; footer "Page 1 / 15";
  compact Search → search sheet prefilled + result; Translate → Google translate entry point; Share text
  `“người”\n— Ngọn Hải Đăng Cuối Cùng, Nguyễn Minh An`; long-press saved highlight → Highlight/Note/Remove; note
  saved (prefs `note`), shown italic in Highlights tab; Export → SAF "…- highlights.md", file content correct.
  Copy not checked. Test book, its highlights and Download files removed afterwards.
- iOS: unit/build verified only. Not checked by hand (no Simulator UI to tap here): open-in from Files, selection
  actions, note sheet, export dialog, translate sheet.
- Next: owner decides commit; batch 2 = backup/restore.

## Batch 2 (owner TODO.md, 2026-11-11) — delegated to both opencode workers
1. Default side margin 30 → 20 on both. 2. Contents sheet: paper rows, 12 gap under tabs, accent tab indicator
(Android was Material purple). 3. iOS search sheet: field and rows share the 16 left edge. 4. Reader light/dark
follows the phone until the reader toggles it (both defaulted to light). 5. Rotation allowed on both (Android
manifest lock removed; iOS iPhone portrait+landscape, iPad all); auto columns = 2 when page width > 560.
- 2026-11-11 batch 2 status: both opencode sessions ran out of the free DeepSeek quota (reset 2026-11-12 07:52);
  switched both to `opencode-go/deepseek-v4.1-flash` via the model picker. Android worker finished all 5 items
  before stopping (no report sent); Claude reviewed. iOS worker did nothing; Claude implemented iOS:
  `ReaderMetrics.hInset` 20; TOC rows `.listRowBackground(.clear)` + 12pt gap; search rows 16pt inset; reader
  dark = saved ?? phone (UIScreen trait, re-checked on scene active); rotation allowed (iPhone portrait+landscape,
  iPad all) with position kept by (chapter, offset) in `layoutChanged`. Tests: Android 147/33 skipped/0 failed;
  iOS 149/5 skipped/0 failed.
- E2 QA (release build installed over the existing release install): margin narrower; contents tabs accent
  underline, paper rows; landscape → 2 columns at the same place; system dark → reader dark live; restored E2
  rotation + light mode. iOS installed on the iPhone; not checked by hand. iOS segmented tab tint left native.
- 2026-11-11 owner: iOS search sheet looked broken (field centred, white background). Fixed by Claude: header
  like Android (serif title left, Done right), content top-aligned, paper background + presentationBackground;
  TOC sheet nav bar/presentation on paper; iOS search snippets now get "…" on cut sides like Android. Verified by
  simulator screenshots (demo hooks `-demoTab library -demoOpenBook 1 -demoSearch 1 -demoTOC 1`); 149 tests pass.
  Open: iOS TOC still uses a system nav title + segmented control, Android uses underlined tabs and no title.

## Batch 3 (owner, 2026-11-11): paused read-aloud resumes from the visible page
Spec: paused + page changed → Play starts at the first sentence of the visible page; same page → resume in place;
reader closed → resume in place. Delegated to both opencode workers (now on opencode-go/deepseek-v4.1-flash).
- 2026-11-11 batch 3 done. Android: behaviour already correct (pageShown → seek moves the paused place); worker
  still added an explicit Play-time rule `reader/SpeechResume.kt` + 5 tests (kept; redundant but consistent).
  iOS: real bug fixed — `SpeechController.setVisiblePage` fed from `ReaderView.publishVisiblePage()` on every
  page change; `resume()` uses `NarrationMath.resumeStart`. Tests: Android 152/33/0, iOS 153/5/0.
  Device: installed on iPhone + E2. E2 check cut short: media-key pause not routed on the emulator and E2 was taken
  by another project (com.cdfx.bbc) mid-test; paused→turn→Play NOT verified by hand on either platform yet.
- 2026-11-11 iOS audio: worker moved AVAudioSession setCategory/setActive to a serial queue (speech waits for
  `sessionReady`) and removed `.duckOthers` (it implies mixWithOthers, which kept OpenBook out of Control Center /
  Lock Screen Now Playing); nowPlayingInfo now has duration/elapsed at a nominal 15 chars/s. Also: Markdown export
  UTType from the `.md` extension (runtime "type not declared" fault gone); demo launches skip the review prompt on
  both platforms (Android sets `asked` only when the sheet is really requested). Tests iOS 154/5/0, Android 152/33/0.
  Hang-risk fault gone on the simulator. Control Center play/pause: owner to check on the iPhone.
  Release archive re-made (2026.11.11 / 20261111) and copied to Xcode Organizer; not uploaded.

- 2026-11-11 evening checkpoint: App Store pack in `stores/app-store/` (listing.md, creative header/search, raw);
  privacy page `web/public/privacy.html` committed on `web`. Screenshots uploaded at 1206×2622. iOS build archived,
  not uploaded. Android version bump pending owner. Nothing committed in android/ios. Handoffs updated in both branches.
