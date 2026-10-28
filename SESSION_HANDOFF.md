# Session handoff — Sách (Android EPUB reader)

Updated: 2026-10-26T12:30+07:00 (Asia/Saigon)

Canonical handoff. `PROJECT_STATE.md` holds the state and the reasoning; `TASKS.md` holds the
work; this file says where the last session stopped and what to do next. The top checkpoint
below supersedes the older Part 12 details further down.

## Read first

1. `PROJECT_STATE.md` — "Facts at a glance", then "Verified current state" and Part 12.
2. `TASKS.md` — the `Open` block is the whole to-do list; `Closed` carries the evidence.
3. This file — the last session, the uncommitted work, the open questions.
4. `docs/` is the app's user guide; `docs/qa/2026-10-23/` holds the test/QA evidence.

## Latest checkpoint — 2026-10-28 (release signing + release APK on E1)

- **Package rename (same day, after the release build):** `namespace` + every Kotlin
  `package`/`import` + the test sources moved from `com.example.epubreader` to
  **`com.elearning.openbook`** (62 files; `git mv` of `app/src/main/java/com/example/epubreader`
  and the test tree, then a string replace; Kover include globs updated with the same replace).
  `applicationId` was already `com.elearning.openbook`. A stale Hilt transform needed
  `:app:clean` once ("Could not find class file for 'com.example.epubreader.ReaderApplication'").
  Rebuilt: `assembleRelease` green, 112/112 tests green, APK **1,776,989 bytes** sha256
  `df3c7032a2354dcd1df0090f209442d90035dcb13b376c6dec44d98891e89ee9`, installed over the
  previous build with `install -r` (same package, same key → **data kept**), launches, reader
  resumes page 1/12, 0 crash-log lines.
- Generated cover (book without a usable image): title and author are now **white** (author at
  78%) instead of the theme ink, which was low contrast on the five dark gradients
  (`Components.BookCover`). Rebuilt + installed: `assembleRelease` green, sha256
  `368b712c57f39423e53dc9aa45b5d2ec17a7fd7e33c1b5cbf8f07f484232af48`; verified on E1 in the
  Home card and in Library.
- **New release key (same day, owner request after the old one was lost):** the old
  `openbook-release.jks` was replaced with a fresh one (alias `openbook`, same DN, same
  password in `keystore.properties`). The signing certificate is now SHA-256
  **`4bd1456eaa9ce2f21e2206bc4c75646ea55724698e3a8c8f16095663fb0d97f3`** (the old builds
  were signed by `c39dec49…`). Release rebuilt: sha256
  `d800185cb762a641b0a8e29946c27495c9cbaead28cb04eeac4569a9e02282d8`, 0 crashes after
  `uninstall` + `install` on E1 (the signature change forces a data wipe again).
- Stray file: `keystore/upload.jks` (alias `upload`, random password) was created by another
  tool run at 12:18 and had taken over `keystore.properties`; it is left in place untouched and
  is no longer used. Delete it if it is unwanted.
- Added a real release key and signing config. Key: `keystore/openbook-release.jks` (alias
  `openbook`, PKCS12, 30 years, DN `CN=OpenBook, O=OpenBook, L=Ho Chi Minh, C=VN`), gitignored
  together with `keystore.properties` (new `.gitignore` entries `keystore.properties`,
  `keystore/`, `*.jks`). `app/build.gradle.kts` reads `keystore.properties` at the root and
  wires `signingConfigs.release`; without the file the release build stays unsigned.
  Password/alias are in `keystore.properties` only — not in Git, not in this file.
- `:app:assembleRelease` BUILD SUCCESSFUL (57 s, R8 + resource shrinking as before).
  APK `app/build/outputs/apk/release/app-release.apk`, **1,777,001 bytes**, sha256
  `38afdf81f841bea2ecbd48b053a4765a560d5e0d9705eddae27cadf23613ae39`,
  signer SHA-256 `c39dec49…ff038` (CN=OpenBook, not the debug key).
- Installed on `emulator-5554` (AVD `E1`) after `uninstall com.elearning.openbook` — the
  previously installed build was signed with the debug key, so the signature change forced a
  data wipe (the emulator's book list and reading progress are gone; the sample book was
  re-seeded on launch).
- Smoke on the release APK: cold start → notification permission asked → Home (0 min, no
  streak) → Library (sample `Ngọn Hải Đăng Cuối Cùng`) → reader page 1/12 with the first-run
  gesture guide → Skip dismisses it; 0 crash-log entries. Screenshots in `/tmp`
  (`openbook-release.png`, `lib.png`, `final2.png`).
- Not done: Play upload/`.aab` (`bundleRelease`), version bump, unit tests/lint not re-run for
  this build-config-only change, and the release build was not exercised beyond the smoke
  above (TTS/notification path).

## Previous checkpoint — 2026-10-26 (Home parity: stats, shelves, week chart)

- HEAD is now `694069c` on branch `android` (checkpoint commit with the parity, robustness,
  Home, Hilt/DI, ViewModel and quality-gate work); nothing pushed. Left out of it: the
  environment-removed skill folders/`CLAUDE.md` and the untracked `docs/qa/` evidence (16 MB).
- Spec: `docs/home-ui-parity.md`. Scope: Home, Library labels/menu, reading ledger, settings —
  no reader/TTS/import/player changes.
- Data: `reader/ReadingLog` (pure) + `library/ReadingLogStore`; `ReaderScreen` adds a one-second
  RESUMED loop (5-min idle cutoff, 2-min session gap, midnight split by local date, save every
  15 s and on pause/close); `BookItem.finishedAt` + `BookMetadata.finishedAt` (old 3-field
  records still decode) + `LibraryStore.markFinished`.
- Selectors: `library/HomeSections` — reading ≤5, added ≤5, same-author suggestions (≥2, no
  duplicates), finished within 12 months, under-six collapses to card + reading.
- UI: `ui/HomeScreen.kt` (30sp title, achievement card with 44dp ring and real-seconds goal
  line, 300dp reading cards, shared off-screen frame per shelf row, 17sp marquee titles, chart
  88dp / 18dp bars / goal line), `LibraryScreen` large title + `Đã đọc xong` label + mark/unmark
  menu, settings dialog goal chips. Gear only at the top, 200ms fade.
- Tests: **106/106** (`HomeSectionsTest` 10, `ReadingLogTest` 10), `assembleDebug` green.
  APK `13,791,702 bytes`.
- Device (`emulator-5558`, vi + en, light + dark): Home card `2 phút hôm nay`, 53%, `Còn 3 phút
  nữa`, `Tuần 2 phút · Tháng 2 phút · Năm 2 phút`, 5 reading cards, 5 newly added, 3 same-author
  suggestions, 1 finished, chart with Monday accent bar and goal line; Library `Thư viện` with
  `Đã đọc xong` and the mark-finished menu; goal chips; language switch. Screenshots
  `home-vi`, `home-vi-dark`, `home3-mid`, `home3-chart`, `settings-goal2`, `lib-vi3`,
  `home1`/`home2` (English) in the session temp dir.
- Not done: achievements sheet (optional in the spec), on-device under-six Home, marquee
  mid-scroll capture, TalkBack enabled run; the QA library has two accidental duplicate imports
  from the SAF taps (test data only).
- Owner follow-ups (same day, all in): reading cards 10dp apart (`PageSize.Fixed(300.dp)`), shelf
  frames wrap content so padding is even, Library capsule `… | +`, list cover 64dp with
  `Trang n/m · %` under the author, pencil/bin icons in the book menu, weekday labels fixed to
  `Mo…Su`/`T2…CN` (vi SHORT names had all read "Th"). Screenshots `home4-vi`, `home5-frame`,
  `home6-week`, `lib4-list`, `lib5-list`, `lib6/7-list-progress`, `grid-menu-icons`.
- Second owner follow-up (same day): reading card 152dp so the content sits tight, floating tab
  bar 4dp above the nav inset (content reserve 88dp), and an empty shelf now opens Library,
  whose empty state has an `Import EPUB` button (verified by wiping the debug app's Books folder
  with `sampleDeleted=true`). Screenshots `home8-tight`, `empty-library2`, `lib8-three`.
- Demo library for review: 17 corpus books in `files/Books` (debug `run-as`), with
  `reading_positions`, `libraryMetadata` and `readingLog` seeded to the spec shape (5 reading,
  5 added, 4 suggestions, 3 finished, 22 min today, 2-day streak). Home screenshots
  `home-full-vi-top`, `home-full-vi-mid`, `home-full-vi-chart`; app left in Vietnamese.
- Next action: owner review of Home; then commit when asked.

## Previous checkpoint — 2026-10-26 (robustness, DRM, benchmark, player motion)

- HEAD is now `694069c` on branch `android` (checkpoint commit with the parity, robustness,
  Home, Hilt/DI, ViewModel and quality-gate work); nothing pushed. Left out of it: the
  environment-removed skill folders/`CLAUDE.md` and the untracked `docs/qa/` evidence (16 MB).
- Spec: `docs/epub-robustness-player-qa.md` (iOS `98d47c4`). Implemented inside `epub/`, the import
  path, the two string sets and the mini player; no reader rewrite, no new dependency.
- Parser: `ZipExtract` uses `ZipFile` (central directory) instead of `ZipInputStream`, so the
  stored-entry-with-data-descriptor case (`04`) no longer throws; names are NFC-normalised and
  matched case-insensitively (closes `05`, `30`); ZIP64 (`20`) and `./` (`28`) already worked.
  `EpubArchive.readMetadata` now validates container → OPF → spine → one existing chapter → no
  encrypted text, without decompressing the book, so import refuses early. `read` refuses DRM
  before touching a chapter and skips+logs missing chapter files.
- DRM: `META-INF/encryption.xml` `CipherReference URI` ending in `.xhtml/.html/.htm` means
  protected; fonts only stays readable (`09`). `10`/`11` refused, never painted.
- Errors: `EpubProblem` on `EpubException` → five `epub_error_*` strings (EN+VI, 140/140 keys).
  `LibraryStore.import` deletes its copy on failure and logs name + problem.
- Test: `EpubCorpusTest` runs the whole 32-file corpus in place (`-DepubCorpus=` override).
  **86/86 JVM tests** pass (was 54; `EpubCorpusTest` adds 32).
- Pagination: chapter conversion yields every 8 chapters; `ReaderScreen` logs read/paginate/
  first-page ms, import logs ms in `LibraryStore`.
- Player: spring 320 ms / damping 0.78, scale 0.55 anchored at the disc → 0.75 on close; compact
  pill when the toolbar shows (rate + `±` hidden); full pill when the chrome hides; disc frame-timed
  at 30°/s and holds its angle; 3 dp progress line; TalkBack labels; animator scale 0 stops disc
  and marquee. No auto-open of the sheet.
- Measured on `emulator-5558` with the release APK (signed with the debug key): 1,250 pages
  first-page **1,044 ms**, 1,000 chapters **1,419 ms**, single huge chapter **3,220 ms** (misses
  ≤1.5 s), image-heavy 137 ms, Gutenberg 457 ms; peak PSS 128 MB / RSS 267 MB; TTS ≈0.6 s; 96%
  janky page turns under the software renderer. Table in `TASKS.md` T-100.
- Animation evidence (session temp dir): `compact-playing2` (compact with toolbar),
  `player-only-full` (full pill, no toolbar), `spr1`/`spr2` (slow-motion unfurl at animator scale
  10), `rmA`/`rmB` (identical disc crops at scale 0), `spinA`/`spinB` (different crops while
  playing), `pauseA`/`pauseB` (identical crops when paused). Logs show
  `chrome=full|player_only`, `speech state: PLAYING|PAUSED`, `playerOpen=`.
- Not done: chapter-jump/scrubber timings, import memory, Perfetto, real hardware; 32 dp wide
  player buttons stay below 48 dp (T-085); the one-huge-chapter book is over the open target.
- Next action: owner review of the numbers; then commit when asked.

## Previous checkpoint — 2026-10-25 (mini player title/rate, full-player header, chapter icons)

- HEAD is now `694069c` on branch `android` (checkpoint commit with the parity, robustness,
  Home, Hilt/DI, ViewModel and quality-gate work); nothing pushed. Left out of it: the
  environment-removed skill folders/`CLAUDE.md` and the untracked `docs/qa/` evidence (16 MB).
- The owner's three follow-ups after the parity pass; Android only, iOS untouched.
  - `ReaderScreen.PlayerRow`: the headline and `speech.rateLabel` are one clickable `Row`
    (`weight(1f)`, full height, `player_open`), so a tap on that part opens the full player
    and the speed sits right after the title. The `maxWidth >= 400.dp` gate is deleted; the
    pill is ~313dp on this device, so the rate had never shown there.
  - `AudioPlayerSheet` header: the text column (title / author / chapter / hairline /
    `Voice: …` / `− speed +`) now sits **left**, the 96dp cover **right**; the old strip
    above `Page n / m` is folded into the column.
  - `Components.SkipPreviousGlyph`/`SkipNextGlyph` (M3 `skip_previous`/`skip_next`, bar +
    triangle, 18dp) replace the seek rings on the sheet's chapter buttons; the mini player's
    `±` keep the rings (they change speed). Disabled tint unchanged (`Ink` at 30%).
- Verified: `:app:testDebugUnitTest` **54/54** (0 failures/errors), `:app:assembleDebug`
  green; APK `13,192,927 bytes`, sha256
  `ae4c4cc66e4cd28e2424b9a33a3dbd4c850b3515adc023f676f1bc69da408b42`. `emulator-5558`
  (AVD `E2`, the renamed `OpenBookReview`; Vietnamese sample, paused at page 2): pill
  `Ngọn Hải Đăng Cuối Cùng · 1. Ngọn đèn trên mũi đá` + `1×`; real tap at `(500,2794)` inside
  `[252,2722][720,2866]` opened the sheet; the header shows the 96dp cover on the **left** and
  the text column (title / author / chapter / hairline / `Voice: …` / `− 1× +`) on its right;
  real tap on `⏭` logged `chapter 1 -> #2, sentence #17` and the sheet/reader moved to
  chapter 2, page 5 / 16, 20% with both skips enabled (`⏮` had been dimmed at chapter 1).
  Screenshots `full-cover-left`, `mini-final`, `full-final-next`, `mini-final-after-skip` in
  the session temp dir.
- Cover side: the first build had text left / cover right; the owner then chose **cover left,
  text right**, so the header `Row` was swapped back and rebuilt (this APK). The mini player,
  the icons and everything else are unchanged from the earlier check.
- AVD note: `~/.android/avd/OpenBookReview.ini` is now `E2.ini` pointing at the same
  `OpenBookReview.avd`; launch it with `-avd E2 -port 5558`.
- Not verified: the sound of the rate/skip changes (the AVD TTS is silent), and 360dp
  crowding is worse now that the rate always shows — still T-085.
- Environment: `emulator-5558` (AVD `E2`) is running this build with the full player open;
  `emulator-5556` belongs to `com.cleanstorage.app`.
- Next action: the owner checks the new header on the device; commit when asked.

## Previous checkpoint — 2026-10-25 (full-player parity with the iOS spec)

- HEAD is now `694069c` on branch `android` (checkpoint commit with the parity, robustness,
  Home, Hilt/DI, ViewModel and quality-gate work); nothing pushed. Left out of it: the
  environment-removed skill folders/`CLAUDE.md` and the untracked `docs/qa/` evidence (16 MB).
- Source of truth: `docs/full-player-parity.md` (iOS `9da6500`, the owner's decisions). Only
  deviations inside the full player and the voice picker were changed; iOS untouched.
- Done, all in the working tree:
  - `reader/VoiceCatalog.kt` (new, pure JVM): resolves the book's language
    (`dc:language` → script/diacritic guess → phone), matches voices by language only
    (`vi` = `vi-VN`), offers every voice when the device has none for the book, ignores a
    saved voice from another language, and groups/search-orders the picker.
  - Book language wired end to end: `RawBook.language` + `EpubArchive.Package.language`
    (DOM and regex fallback), `ReaderEngine.bookLanguage`, `speech.useLanguage()`.
  - `SpeechReader`: `applyLanguage` (set language → reload → pick saved/book/phone/`vi-VN` →
    persist only on `SUCCESS`), `refreshVoices()`, `setVoice(String)` without the null path,
    `preview(voice, sample)` on a **second** `TextToSpeech` that pauses the book first and is
    shut down with the reader.
  - `ReaderScreen`: `skipChapter(±1)` (chapter head, disabled at first/last, stays paused)
    replaces the sentence skips; passes chapter bounds, book language, voice label,
    `previewing` and the preview callbacks to the sheet; pill title gets "Open player".
  - `AudioPlayerSheet` rewritten to the spec: 96dp cover, title/author/chapter, hairline,
    `Voice: <name>`, 34dp speed capsule, `Page n / m` + `%`, book-wide scrubber, and one
    transport row (Sleep · previous chapter · 60dp play · next chapter · Voice) with 48dp
    targets. The Voice button opens the new picker dialog: header, count, search, language
    groups, `quality · locale`, preview with a stop square, **Use**/tick, "Get more voices"
    → system TTS settings.
  - Strings: 24 new keys in EN + VI, 5 stale ones removed,
    `speech_no_vietnamese_voice` → `speech_no_voice_for_language(locale, code)`. 135/135.
- Theme flash: checked, no change needed — nothing sets the Activity/window theme from the
  reader (only the status/nav icon appearance follows the paper). Verified on device below.
- Verified: `:app:testDebugUnitTest` **54/54** (0 failures/errors; `VoiceCatalogTest` is 11,
  `EpubArchiveTest` now also asserts `language == "vi"`), `:app:assembleDebug` green; APK
  `12,716,645 bytes`, sha256 `ca4b621bab0a1cd2183fbd5196e0ffb19412c478fffd545e515f204e738753e2`.
  AVD `OpenBookReview` (`emulator-5558`), screenshots `vp1`–`vp3`, `en1`–`en6`, `dark1`,
  `back1`–`back4`, `slp0`/`slp1` in the session temp dir:
  - picker: `Voice` · `11 voices for this book` · search · `Vietnamese (Vietnam)` · rows with
    `High · vi-VN` · **Use** / tick (the vi-VN default reads as `language` because the engine
    names it `vi-VN-language`);
  - real tap on **Use** → `voice -> vi-vn-x-vic-local (vi-VN, code=0)`, sheet shows
    `Voice: x vic local`, transport still `Play` (paused);
  - preview → `preview -> vi-vn-x-vic-local` on the second engine, stop square in the row, and
    after closing the sheet reads `Play` (the book was left paused);
  - chapter skips: from the cover `chapter 1 -> #1 …` through `chapter 1 -> #6, sentence #72`,
    `Next chapter` then `enabled=false` in the a11y tree with page 12/13, `Previous chapter`
    `enabled=false` at the cover chapter; state `PAUSED` on every line;
  - English book: an imported copy of the sample with `dc:language=en` logged
    `book language -> en`, `setLanguage(en) -> 0`, `voices for en: 51`; the picker showed
    `51 voices for this book` / `English (Australia)`, `united` kept the English groups,
    `zzz` said `No voices match.`; a preview on `x aua network` logged
    `preview -> en-au-x-aua-network`;
  - wrong-language memory: after the English session saved `en-au-x-aua-network`, reopening
    the Vietnamese book showed the Vietnamese default again;
  - sleep timer: `sleep timer: 5 min` with `4:57` under the clock (accent), then `off`;
  - theme: dark reader (`demoDark 1`) → Back — the first screenshot after the key is already
    the light Home, no dark frame; `Get more voices` opened
    `com.android.settings…TextToSpeechSettingsActivity`.
- Kept as-is: the speed multiplier maps 1:1 to `TextToSpeech.setSpeechRate` (Android's
  argument is already a multiplier; the spec's conversion note is iOS's `AVSpeechUtterance.rate`).
- Not verified: the sound of a preview or of a faster rate (this AVD's TTS produces no audio
  and finishes instantly), and the picker with a third language (no Japanese/Chinese book
  imported — the rules are covered by `VoiceCatalogTest`).
- Next action: nothing open from the owner. Older backlog unchanged (T-085, T-075/076/078/079).

## Previous checkpoint — 2026-10-25 (third UI pass: capsule, bubbles, player controls)

- HEAD is now `694069c` on branch `android` (checkpoint commit with the parity, robustness,
  Home, Hilt/DI, ViewModel and quality-gate work); nothing pushed. Left out of it: the
  environment-removed skill folders/`CLAUDE.md` and the untracked `docs/qa/` evidence (16 MB).
- Done, all in the working tree:
  - Library header: import and `…` are one 44dp capsule again, with `+` *inside* on the left,
    a hairline divider, then `…`. Both are bare `IconHit` targets (no circle of their own).
  - The shelf menu and a book's Edit/Delete now open `BubbleMenu`: a fixed-width card with
    12dp corners, a shadow, and a 16 × 8dp tail centred under the `…` — a message bubble.
    `BubbleRow`/`BubbleDivider` replace `MenuRow`/`MenuDivider` inside them (`danger` colours
    Delete); `DropdownMenu` is no longer used in the library.
  - Grid: the per-book `…` lost its ring — `BookActions` is a bare 40dp glyph below the cover
    on the right; no title/author (unchanged from the second pass).
  - Full player sheet: takes `page`/`pageCount` and the rate callbacks. "Reading progress" →
    `Page n / m`. Under the transport one row holds `− 1.25× +` (the label comes from
    `speech.rateFactor`, so it recomposes), a Sleep timer button (accent + countdown when
    armed) and a Voice button; each opens its own `DropdownMenu` with a tick on the current
    choice. New `player_slower`/`player_faster` in EN/VI; `ChoiceChip` gone.
  - Rate fix: every change goes through `SpeechReader.setRate` (clamp → persist → log
    `rate -> Nx` → restart the sentence, staying paused if paused).
- Verified: `:app:testDebugUnitTest` 43/43, `:app:assembleDebug` OK, APK 12,710,585 bytes,
  sha256 `085fa0b9fb1e4bd3cc275e3492140cd66aae561c40f5832d80b2c3323b1f9ec7`.
  `emulator-5554`: capsule, both bubble popups, ring-free grid `…` (screenshots `p3-lib3`,
  `p3-bubble3`, `p3-bookbubble`). AVD `OpenBookReview` (`emulator-5558`): sheet opened by a
  real tap, `0.75×` → `+` → `1.25×` in the header and in the capsule, prefs
  `narrationRateFactor=1.25`, `Page 2 / 3`, Sleep menu (Off ticked, 5/15/30/60) and Voice
  menu (Default + 11 vi-VN voices).
- Emulators: `Pixel_9_Pro_XL` is shared with other work and lost the foreground twice
  (`com.cleanstorage.app`, `com.wordquest.game`). A second AVD `OpenBookReview` was made by
  copying `config.ini` into a new directory (fresh userdata, cold boot) and runs on port
  5558; delete `~/.android/avd/OpenBookReview.avd` + `~/.android/avd/OpenBookReview.ini` to
  remove it. After the final reinstall both emulators are left on Home with the final APK.
- Not verified on device: the actual audio speed change (this AVD's TTS finishes sentences
  almost instantly — no sound is produced, so 1.25× cannot be heard; the label, pref and
  `rate` log are the proof), and the dark-mode look of the new bubbles.
- Next action: nothing open from the owner. Older backlog unchanged (T-085, T-075/076/078/079).

## Previous checkpoint — 2026-10-24 (second UI pass: Home, Library header, grid, menu icons)

- HEAD is now `694069c` on branch `android` (checkpoint commit with the parity, robustness,
  Home, Hilt/DI, ViewModel and quality-gate work); nothing pushed. Left out of it: the
  environment-removed skill folders/`CLAUDE.md` and the untracked `docs/qa/` evidence (16 MB).
- Done:
  - Home: the entire "Your library" section (heading, count, shelf row) removed; only the
    "Reading now" pager remains. `home_your_library` + `book_count` strings deleted in EN/VI;
    `HomeScreen` no longer takes `progress`.
  - Library header: import and `…` are now two separate 40dp `ShellIconButton`s, 8dp apart,
    instead of one capsule.
  - Library grid: title and author removed from cards; under each cover a row holds the
    percentage on the left and the book's `…` on the right. Selection still badges the cover
    and hides the `…`. List view unchanged.
  - Popup: every row has an icon — grid, list, clock (`Recently imported`), serif `A`
    (`Title`), person (`Author`), tick (`Select`). New `ClockGlyph`/`PersonGlyph` in
    `ui/Components.kt`.
- Verified: `:app:testDebugUnitTest` 43/43, `:app:assembleDebug` OK, APK 12,710,217 bytes,
  sha256 `1f1754870cbf8c04b216370f808873b54d8b49308dea743b8afc31be6ecb45da`, reinstalled.
  Device screenshots confirm each change; a real tap on a grid `…` opens Edit/Delete.
- Device left on Library, grid view, Author sort, English, light; `uimode night no`.
- Next action: nothing open from the owner. Older backlog unchanged (T-085, T-075/076/078/079).

## Previous checkpoint — 2026-10-24 (four UI changes)

- HEAD unchanged: `315c8e6` on branch `android`. Nothing committed, staged or pushed; the
  working tree holds every iteration plus this polish.
- Done, all in the working tree:
  - Home/Store/Library no longer print their screen titles; only the bottom tab bar names
    the tabs. Home keeps the settings gear, Store keeps its `DEMO` badge, Library keeps its
    control capsule.
  - `ContinueCard` lost `ReadBar`; it shows `Page n / m · N%` and "Read on".
  - `BookCover` is gradient + spine + border only. Title/author now sit outside the cover:
    beside the thumbnail on Home's pager, under the cover on Home's shelf and Library's
    grid, beside it in Library's list.
  - Library's `+` and a new `…` share one 40dp pill. `…` opens `LibraryMenu` with
    Grid/List, the three sorts, a divider and Select (hidden when the shelf is empty).
    `ViewToggle` and the old sort button are deleted; `library_options` is the new
    accessibility label in EN/VI.
- Verified: `:app:testDebugUnitTest` 43/43 pass; `:app:assembleDebug` BUILD SUCCESSFUL,
  APK 12,710,813 bytes, sha256
  `7cf70633b054c487fd3d13ad48098d756d12f18200ab599ff81274ce77322838`.
- Device `emulator-5554`: screenshots confirm Home (no title, no bar, text-free covers with
  title/author/% beneath), Library grid (titles under covers), the open popup, List chosen
  from the popup, Select at "0 selected", and Store with only DEMO.
- Note: an unrelated app (`com.cleanstorage.app`) kept stealing the foreground during QA;
  force-stopping it was needed between taps. One stray tap into a book moved the first
  sample's position from 58% to 92% — that is real saved progress, not test data.
- Device left on Library, list view, Author sort, English, light; `uimode night no`.
- Next action: nothing open from the owner. Older backlog: T-085 (reading-bar headline at
  360dp), T-075/T-076/T-078/T-079 need owner decisions.

## Previous checkpoint — 2026-10-23 (reading iteration: Home, Library, language, pill)

- HEAD is now `694069c` on branch `android` (checkpoint commit with the parity, robustness,
  Home, Hilt/DI, ViewModel and quality-gate work); nothing pushed. Left out of it: the
  environment-removed skill folders/`CLAUDE.md` and the untracked `docs/qa/` evidence (16 MB). All
  iteration work is in the working tree (`git status --short` lists the modified files plus
  the new `library/`, `settings/`, `ui/HomeShelf.kt`, `ui/ShellTheme.kt`, `values-vi/`,
  `app/src/test/`, `docs/qa/`).
- Done: Home pager of genuinely in-progress books; Library list/grid + 3 persisted sorts
  (locale collator, id tie-break, import date frozen on first sight); metadata edit that
  keeps id/progress/bookmarks; selection + confirmed delete that only removes app copies;
  percentage-only rows; shell theme follows the phone while the reader keeps its own; full
  English/Vietnamese resources with an app-only language override; floating Home/Library/Store
  pill hidden in the reader; reader/import failures show generic localized text.
- Fixed during review: RECENT sorting used title as a tie-break (now id only, so a rename
  cannot reshuffle); the Select button preselected every book (now an explicit `selecting`
  mode starting at zero).
- Verified: `JAVA_HOME=/Users/cuong/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
  ./gradlew :app:testDebugUnitTest` → 43/43 pass; `:app:assembleDebug` → BUILD SUCCESSFUL,
  APK 12,711,557 bytes, sha256
  `b6b3014b2b928883d86e0e2e472fd5d34cd0ffd2ee724bb6460659614cb9c4f2`.
- KiloCode 7.8.15 did the independent review and three device passes; logs and screenshots
  are in `docs/qa/2026-10-23-iteration/`. Two Kilo "failures" were tester artifacts (corrupt
  QA leftover; three cards sharing one title) and are documented with evidence in
  `qa-lead-verification.md`. Do not re-open them without new evidence.
- Device: `emulator-5554` installed with the final APK; app left on Home, English, light;
  6 books, all real titles/progress intact. `cmd uimode night no`; phone locale never changed;
  no uninstall or data wipe. The emulator was restarted once during the session and the app
  reopened normally.
- Next action: none required for this scope. Optional follow-ups: a physical-device
  audio-output check, a tablet-width pass, and the stale unused `ui/SearchScreen.kt` left
  untouched on purpose.

## Earlier checkpoint — 2026-10-23 (test suite + KiloCode QA)

- HEAD unchanged: `315c8e6` on branch `android` (3 commits). Nothing committed, pushed or staged.
- Added the first automated suite: **26 JUnit 4 JVM tests** under
  `app/src/test/java/com/example/epubreader/` — `SentencesTest` (11), `ReadingPositionTest`
  (6), `EpubArchiveTest` (6, real `sample.epub` plus missing-container/missing-OPF errors),
  `ZipExtractTest` (3). Test-only dependency: JUnit 4.13.2 in
  `gradle/libs.versions.toml` + `testImplementation(libs.junit)`.
- Verified: `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL, 26/26, 0 failures/skips;
  `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL, APK 12,182,409 bytes, sha256
  `26a8795c257ad81fa4422b02ece0a903665d339045a60e00e1d1828b3ea5d991`.
- KiloCode 7.8.15 (real CLI child; Orca exposes no Kilo worker type) reviewed the change,
  re-ran both Gradle tasks and returned **APPROVE** (`docs/qa/2026-10-23/kilo-review.log`,
  `kilo-reverify.log`). Device QA on `emulator-5554` passed: Home / Library / Store, reader
  resumed (`Trang 2 / 12`), page turn to `3 / 12`, Back to Home, no crash; a vision pass
  verified every PNG (`kilo-device-qa-report.md`). The first QA attempt falsely claimed the
  reader opened while still on Home; it was caught from the screenshots and re-run with exact
  coordinates.
- Installed APK, app left open in the reader: chapter 6, `Trang 12 / 13`
  (`docs/qa/2026-10-23/device-final-reader.png`). Emulator data was not wiped. The emulator
  was restarted once at 10:39 after its previous background process shut down; the app
  reopened at the same page. A system CleanStorage permission dialog appeared during a tap
  and was dismissed with Back (nothing granted or deleted), then the reader was reopened.
- Working tree: `M ReaderTheme.kt` is the pre-existing owner highlight edit — **do not
  revert**; `M app/build.gradle.kts`, `M gradle/libs.versions.toml`; untracked
  `app/src/test/`, `docs/qa/`, `COORDINATOR_STATUS.md`.
- Next action: only T-085 remains for this scope; no test or build work is outstanding.

## Where the work stands

- Part 12 is built and verified by hand: tapping the book title in the player opens the full
  player sheet (cover, seek slider, back / play-pause / forward, sleep timer, voice picker), and
  the main screen is three tabs — Home, Library, Store — with one shared import picker. The
  Store is an honest offline notice plus `Nhập EPUB`, not a catalogue (the app has no `INTERNET`
  permission; the owner chose this).
- Nothing is half-written in this round. `AudioPlayerSheet.kt` and `MainScreen.kt` are new files;
  `SpeechReader.kt`, `ReaderEngine.kt`, `ReaderScreen.kt`, `Components.kt`, `LibraryScreen.kt`
  and `EpubReaderApp.kt` carry the wiring.
- `emulator-5554` is running the app in portrait (1344x2992, 480dpi, 13 pages), reader open on
  page 12/13, chrome hidden, voice `IDLE`; the T-084 display overrides were reset. The sample
  book shows the uncommitted image work: page 4 carries a colour-bar image with the caption
  "[Ảnh: Bản vẽ thất lạc]" and the voice reads such captions aloud.

## Verified in the last session (Part 12)

| Check | Result |
| --- | --- |
| `./gradlew :app:assembleDebug` (JDK 17) | `BUILD SUCCESSFUL` |
| APK | `12,366,927 bytes`, 2026-10-22 09:07, sha256 `37a4c560…c509`, no source newer |
| Tabs | real taps Home / Library / Store; Back from a book opened on Library returns to Library (`10`) |
| Sheet | real tap on the headline (`[252,2722][638,2866]` per `uiautomator`) opens it, light (`03`) and dark (`05`) |
| Slider | `slider 79% -> sentence #75`; `slider 15% -> sentence #18 (page 4)`, page moved to `Trang 4/13` while paused (`06`) |
| Skip | `skip 1 -> #53`, `skip -1 -> #52`, highlight moves while paused |
| Sleep timer | 15 phút → `tự dừng sau 14:57` (`07`); harness `--ei demoSleep 1` → `sleep timer: 1min up -> stopping the voice`, `speech state: IDLE` (`08`) |
| Voice | `voice -> vi-vn-x-gft-local (code=0)`, restarted the paused sentence and stayed paused; prefs keep `<string name="narrationVoice">vi-vn-x-gft-local</string>`; relaunch logs `saved voice vi-vn-x-gft-local -> 0` |
| Store | "Cửa hàng chưa kết nối" + `Nhập EPUB`; no fake shelves (`09`/`11`) |
| Small phone / landscape (T-084) | `wm size 720x1280` + `wm density 320` (360dp): sheet ~93% of height, every control fits without scrolling (`14`); `user_rotation 1` (2992x1344): Material caps the sheet at 640dp and one swipe reaches the timer/voice rows (`12`, `13`); display settings reset afterwards |
| Crashes | 0 `FATAL EXCEPTION` |
| Evidence | `/Users/cuong/.ao/data/artifacts/standalone-20/screenshots-player-sheet/01`…`11` |

## Git state — dirty, uncommitted, nothing staged

- Branch `master`, **2 commits**, **no remote**:
  - `9e1cdf2` — 2026-10-20, the Part 7 baseline.
  - `fa27d98` — 2026-10-21, *Parts 8-11: curl reaches head and tail, flush progress line, tap
    thirds; checkpoint docs*.
- Working tree from this session: `M PROJECT_STATE.md`, `M TASKS.md`, and the Kotlin files above;
  new `ui/AudioPlayerSheet.kt`, `ui/MainScreen.kt`.
- **Also dirty from earlier, not authored or verified by this session**: image rendering
  (`epub/ChapterImages.kt` new, `epub/HtmlText.kt`, `epub/EpubArchive.kt`, `model/Models.kt`,
  `reader/ReaderSettings.kt`, `reader/ReaderTheme.kt`), and `ui/SearchSheet.kt` replacing the
  deleted `ui/SearchScreen.kt` — this is the open T-065/T-069 work. Do not revert it.
- No `.gitignore` change; `build/`, `.gradle/`, `local.properties`, `.idea/` stay out.
- Suggested staging when the owner authorises a commit — **ask first, do not `git add -A`**:
  `PROJECT_STATE.md TASKS.md SESSION_HANDOFF.md` plus `ui/AudioPlayerSheet.kt ui/MainScreen.kt
  ui/Components.kt ui/ReaderScreen.kt ui/LibraryScreen.kt ui/EpubReaderApp.kt
  reader/SpeechReader.kt reader/ReaderEngine.kt MainActivity.kt`. The image/`SearchSheet` set
  should be its own commit once it is verified.

## Blocked / awaiting the owner

1. **T-085** — at 360dp the reading bar's headline collapses to "Hải Đ": the rate label and the
   five 48dp controls leave it ~50dp. Decide the fix (share the row, or drop the rate label when
   narrow) — not built.
2. **T-075** — the Part 11 curl was built from the owner's sentence; the picture never reached
   the session. Ask them to re-send it or confirm the head-to-tail reading.
3. **T-076** — should a nearly vertical swipe turn the page? `DRAG_AXIS_FRACTION = 0.35` today.
4. **T-078** — should a size change inside the customizer mark the theme card with `*`?
5. **T-079** — Part 9's "ẩn luôn cái nút back": the dimmed back (built) or no back at all?
6. The image work in the dirty tree is live in the shipped APK but unverified by this session: on
   landscape page 4 the sample book shows a colour-bar image plus "[Ảnh: Tệp hỏng]", and the
   voice reads the caption aloud as if it were text (`reading #10`, `#11`). T-069's premise
   ("`ImageGetter` is null") describes HEAD only. Owner input needed on whether that work is in
   scope for this round.
7. Nothing else blocks work; `ffmpeg` is still missing, so T-074 (film of the commit animation)
   waits.
8. **Cover side — settled** — the owner chose cover **left**, text **right** on 2026-10-25; built
   and verified on `emulator-5558`.

## Highlights, 3-column grid, tighter player (2026-10-27)

- `HighlightStore` (per book, chapter+offset+text), toolbar pencil toggle, tap a sentence to
  save, amber bands for saved highlights, third Contents tab with swipe-to-delete (bookmarks
  already had swipe). Library grid fixed at 3 columns; player buttons 36x48dp. Build/tests/lint
  green (111/111). Device smoke of the highlight flow was **not** completed (tap coordinates /
  shared-AVD focus); the code compiles and the store is wired through Hilt, but treat the flow
  as unverified until a clean device pass.

## 48dp player targets (2026-10-27)

- Owner decision: shorten the title. `PLAYER_BUTTON_WIDTH` = 48dp (height 48dp already), so the
  mini-player controls meet the 48dp target; headline gives up the room and marquees when long.
  T-085 closed. 111/111 tests, lint PASS, assemble PASS. Pill screenshot blocked by this AVD's
  instant TTS (the pill closes before a capture).

## P2b-a (2026-10-26)

- `ReaderViewModel` + `StateFlow<ReaderUiState>` now own `pageIndex`, `failure` and
  `savedPosition`; `ReaderScreen` forwards every page change through `setPage`, and
  `SavedPlaces` (implemented by `ReadingPositions`) is the test seam.
- `ReaderViewModelTest` (4) added; suite is **110/110**, lint PASS, assemble PASS, APK
  `13,304,156 bytes` sha256 `8eeb88c1…e70d81`, installed on `emulator-5558`.
- Device smoke: TTS followed the page to 3, Back/reopen resumed page 3. Kover still
  **22.62%** branch (waiver W-001). Chrome/sheets/speech stay view-local (transient UI).

## P2/P3 (2026-10-26)

- Shell state is now `AppViewModel` + `StateFlow<AppUiState>`; `EpubReaderApp` only collects and
  forwards events. Reader stores come from `ReaderStoresViewModel`; the reader's transient
  page/chrome/speech state is still local (P2b remains).
- Gates: Kover 0.9.11 (`:app:koverVerifyDebug`, 80% branch on domain/data/VM), lint gate
  (`abortOnError`), `CONSTRAINTS.md`. Lint found and now passes after three real fixes:
  API-27-only `windowLightNavigationBar` removed (styled in code), `LineBreaker` constant on
  API 29+, observable locale in `ReaderScreen`. Kover **FAILS at 22.65% branch** — waiver W-001
  in `CONSTRAINTS.md` (owner cuong, expires 2026-12-31), threshold unchanged.
- Empty-shelf launch re-verified after the refactor: Library + "Chưa có sách nào" + Nhập EPUB
  (`empty-after-hilt`); the 17-book demo library was restored afterwards (`home-after-hilt`).
- Blocked: independent QA/review/test subagents — free models answer "Free usage limit reached";
  no paid fallback used.

## Hilt/DI P1 (2026-10-26)

- Done: Hilt 2.60.1 + kapt in the catalog; `ReaderApplication`, `@AndroidEntryPoint`
  `MainActivity`, `di/AppModule` (six singleton stores), `AppDependenciesViewModel`
  (`@HiltViewModel`) consumed by `EpubReaderApp`; `ReaderScreen` receives `bookmarks` and
  `readingLogStore` instead of constructing them. No behaviour change.
- Verified: 106/106 tests, `assembleDebug` green; emulator launch crash-free, Home + reader +
  TTS + resume-position smoke passed (screenshots `hilt-home`, `hilt-reader`, `hilt-resume`).
- Still open: P2 screen state into ViewModel/StateFlow (ReaderScreen 1,700 lines), P3
  Kover/lint/`CONSTRAINTS.md`; independent QA/review/test subagents blocked by "Free usage
  limit reached" on every free model — no paid fallback used.

## Tooling (2026-10-26)

- Added project subagents under `.opencode/agents/`: `android-reader` (read-only),
  `android-builder` (scoped edits + Gradle), `android-qa` (tests/adb, no edits). All pinned to
  `opencode/deepseek-v4.1-flash-free` (zero-cost). Discovery confirmed: launching
  `android-reader` reached the model, which refused with "Free usage limit reached" — the free
  allowance is exhausted for now, so these agents cannot run until it resets. No app code
  changed.

## Exact next action

Owner review of the T-100 numbers (especially the 3.2 s single-huge-chapter open and the
emulator-only jank figures), then commit when authorised. T-085 (360dp headline) and the 32 dp
player buttons are the open accessibility/width trade-off.

## Resume prompt (paste to a new agent)

> Đọc `PROJECT_STATE.md`, `TASKS.md` và `SESSION_HANDOFF.md` ở gốc dự án
> `/Users/cuong/Workspace/CODE/AI/OpenCode/epub-reader-android`. Đối chiếu với `git status` và
> APK thật, không sửa code trước khi báo cáo khảo sát. Cây làm việc đang **bẩn và chưa commit**:
> Part 12 (player sheet + 3 tab) của phiên 2026-10-22, cộng phần image/`SearchSheet` cũ hơn mà
> phiên này không viết và không kiểm — không được xoá hay revert. Không chạy `git reset`,
> `git checkout --`, `git clean` hay `git add -A` khi chưa hỏi; không tự commit/push.
> Việc tiếp theo: T-085 (ở 360dp tiêu đề trên thanh đọc bị bóp còn "Hải Đ"); sau đó hỏi owner
> ảnh curl Part 11 (T-075) và quyết định T-076.

## Environment (bites every time)

- Build: `JAVA_HOME=/Users/cuong/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
  ./gradlew :app:assembleDebug`; adb at `~/Library/Android/sdk/platform-tools/adb`; AVD
  `Pixel_9_Pro_XL`, 1344×2992, density 480, API 36.
- Screen targets move when the sheet or the chrome changes: take coordinates from
  `uiautomator dump` + bounds, not from memory.
- Screenshots: `adb shell 'timeout 15 screencap -p /sdcard/x.png'` + `adb pull`; held drags via
  `input motionevent DOWN/MOVE…/UP`, never within ~50 px of an edge, with a small first MOVE
  (≤100 px) and a ~0.4 s pause or the lean locks at 0.
- Captures: set `adb logcat -G 32M` first; the glyph/Skia atlas corrupts after long runs — fix
  with `adb emu kill` + relaunch, it is the emulator, not the app.
- Harness extras used in Part 12: `--ez demoOpenBook true`, `--ei demoSleep <minutes>`.
- Evidence: `/Users/cuong/.ao/data/artifacts/standalone-20/` (`screenshots-player-sheet/` for
  the latest round).

## Appendix — round-by-round detail (Parts 11 → 6)

Moved here from `docs/SESSION_HANDOFF.md` on 2026-10-21. This is the long record: what each
round found, fixed and measured, in the order the rounds happened.

### Handoff entry point written at the end of Part 11 (kept for its record)
- Exact next action: Part 11 is built, verified and reported. The one thing to confirm with the
  owner is the **curl** reading: their attached picture never reached this session, so the fold
  was changed on the interpretation in `report.md` §89 ("the crease must run head-to-tail, not
  stop on the fore-edge"). If that is not what the picture showed, the next step is the picture.
  The second question to keep in view: a nearly vertical swipe is still not a page turn
  (`DRAG_AXIS_FRACTION = 0.35`), and the owner's wording ("vuốt từ dưới lên") may be that.
- Verification record: the shipped APK (`12,302,465 bytes`, 2026-10-21 09:20,
  sha256 `2c0ac424…d8ab`) is installed on `emulator-5554` and was last seen taking real input:
  two committing real drags (up at 59% pull, back at 54%, both `-> turn`, both drawing a crease
  from the head edge to the tail edge), a left/right/middle tap run (page 5 -> 6 -> 5, chrome on
  the middle tap), two right taps 0.35 s apart taking page 7 to page 9, and the player's progress
  line measured flush on the capsule's bottom edge. Evidence in
  `/Users/cuong/.ao/data/artifacts/standalone-20/screenshots-part11/` with `logs.txt` and
  `measurements.txt`; Part 10's set is next door in `screenshots-part10/`, Part 9's in
  `screenshots-part9/`, Part 8's in `screenshots-spine2/`.
- Open question 1: the customizer's own "Cỡ chữ" slider writes the same `sizeSp` the A/A pill
  writes, so after Part 10 it does not mark the theme card either. The owner's rule was "only
  editing Tuỳ chỉnh gets the `*`"; making the two sizes disagree means remembering *where* the
  size came from, which the config does not record today. Ask before building it.
- Open question 2 (from Part 9, unanswered but the work was accepted): whether "ẩn luôn cái nút
  back" meant the dimmed back at the voice's level (built) or the back button in the reader
  altogether (not built).
- If more is wanted, the natural next steps are: the five small search/bookmark gaps listed
  at the end of `PROJECT_STATE.md` (placeholder wording, snippet lines, "tr. N", swipe to
  delete, sheet vs full screen); a check at tablet width (where `Cột — Tự động` would
  resolve to 2, and where the customizer preview's margin would read closer to the page's
  own proportion); a two-column page that carries two chapters (the only case where the
  highlight band's chapter comparison is load-bearing; the sample book's chapters are too
  long to stage it); a long chapter title to exercise the running head's ellipsis; `<img>`
  support (non-null `ImageGetter`); the book's own CSS; word-anchored bookmarks; or a real
  audio-output check on hardware.

### Part 11 — three reports (2026-10-21) — history, Part 12 is current
- The owner: (a) the player's progress bar is still not flush with the bottom edge, must run end
  to end but must not spill out; (b) the curl now hugs the spine, but the part above does not
  curve up to the head of the book when swiping up (or the tail when swiping down) — "xem hình
  đính kèm"; (c) with the player closed a tap on either side turns the leaf and a tap in the
  middle shows the buttons.
- **The attached picture never arrived.** No image from today exists in
  `~/.ao/data/attachments/standalone-20/`, and the session's prompt journal holds only text. So
  (b) is an interpretation, stated in full in `report.md` §89.
- What (b) actually was, measured on the Part 10 build: on a 1,344 × 2,992 page a 30° lean walks
  the crease 1,727 px sideways over the page's height, i.e. more than its 1,344 px width, so a
  leaning crease always leaves through the **fore-edge** — the fold could only ever be a corner
  triangle, and at full pull a flat triangle of the old page was still lying in the corner when
  the turn committed (`pull=1.0 lean=30 -> (0%,0%) -> (100%,78%)`).
- The fix: the hand's lean is still read early (Part 9's up/down direction, unchanged) but fades
  out of the geometry as the pull grows (`leanScale`, fade 0.12 → 0.62), so the crease turns
  upright and finishes square to the gutter. Now every fold past ~55% runs head-to-tail:
  real drag up 59% -> `(43%,0%) -> (41%,100%)`, back drag 54% -> `(54%,0%) -> (41%,100%)`, full
  pull -> `(0%,0%) -> (0%,100%)` with nothing left flat.
- (a): the line loses its 14dp / 36dp / 3dp insets, `align(BottomCenter) + fillMaxWidth()`, and
  the capsule's own 24dp clip rounds its ends off inside the corners. Measured: line y
  2858…2865, capsule bottom 2866 — a 0 px gap — and no line colour outside the silhouette.
- (c): `pageTapped` consults the thirds only while `speechState == IDLE` (the same condition as
  "the player is closed"), turns through `jumpToPage` (the same single curl the voice uses), and
  counts a second tap from the page the first is still turning to. A third with no leaf behind
  it falls through to the chrome so a hidden toolbar is never stranded.
- Not verified this pass: film of the commit animation (`ffmpeg` still absent), the harness
  search/bookmark/customizer paths, the fling arm, three or more queued taps, dark-mode fold
  shading. The emulator's glyph atlas corrupted two capture frames mid-pass; both were discarded
  and every shipped frame was checked for clean text.

### Part 10 — five reports (2026-10-21)
- The owner: the gear must hide with the other buttons; remove "Xong" and stop the `*`
  appearing on a size change; animate the player and put its progress bar on the capsule's
  floor; the diagonal curl still does not hug the spine; and the book must reopen where it
  was left with a progress bar on the shelf.
- **Gear.** `if (ready != null && chrome == ChromeMode.FULL)` with a 44dp `Spacer` otherwise,
  so the pill (weight 1f between disc and gear) keeps its width — measured: the capsule's
  right border starts at x = 1137 in both levels, the gear's own circle at 1164 only at FULL.
- **Sheet.** `onDone`/"Xong" gone; `TypeConfig.signature()` drops `sizeSp`, so the `*` marks
  the customizer's eight fields only. Verified 21sp -> 23sp with no `*`, Cột auto -> 1 with
  one.
- **Player.** `PlayerSlot` = `AnimatedVisibility(fadeIn + expandHorizontally(Start))` /
  `(fadeOut + shrinkHorizontally(Start))`, 260ms; a named composable because
  `RowScope.AnimatedVisibility` cannot be reached from the `Box` that owns `weight(1f)`.
  Progress line inset 5dp -> 3dp (3dp is the minimum that clears the 24dp corner with the
  line's 14dp start).
- **Curl.** `travel = max(w·|cos lean|, 1)` — the width's projection on the normal — instead of
  the page's diagonal span, so a leaning fold ends on the gutter corner instead of 1,496 px
  past it (which peeled the whole bound edge). Level fold unchanged; `creaseReport` logs the
  crease's border ends and whether any of the bound edge is peeled (0.5px tolerance so a
  tangency at full pull is not called a peel).
- **Reading position.** New `reader/ReadingPositions.kt` (`page|pageCount|chapter|offset` per
  book id); recorded on every page change and restored through the text anchor; the shelf
  draws `(page + 1) / pageCount` as a bar plus "N%".
- Not re-run: the harness's search / marks / customizer / late-cursor paths, the fling arm,
  a *harness* fold that commits, multi-page drags. The unfurl has no film — at 1× the 260ms
  open is shorter than `screencap`'s start-up; `14` was caught with the emulator's animator
  scale at 10× and the scale restored afterwards.

### Part 9 — four reports (2026-10-20, later) — history, the gear's level superseded

- The owner: the voice starts by itself when a book is opened; the fold leans against the
  hand; hide the back button; the player covers the settings button.
- **The voice.** A plain launch never started it (checked at 15:53 and 16:04). A process
  launched by the screenshot harness replayed the whole staging on every book open, because
  `DemoOptions` comes from the intent while the staging effect lived in `ReaderScreen`, which
  is rebuilt per visit: `15:55:07.794 speech state: PLAYING` 2.7 s after the second open. The
  staging is now one shot per launch — `EpubReaderApp.demoPending` + `ReaderScreen.onDemoUsed`,
  with `demo` itself untouched so the sheets still open on the harness's tab/query. Same repro
  after the fix: `chrome=full speech=IDLE` and nothing more for 14 s.
- **The lean.** `n`'s y component had the wrong sign; `n = -normalise(drag)` =
  `(outward*cos(lean), -sin(lean))`. One sign in `foldGeometry` (plus `holdFold`'s doc). Level
  folds are unchanged — checked again because the sign is in the same function.
- **The chrome.** The left capsule (`back · mục lục`) is drawn at `chrome == FULL` only, so the
  voice's level has no dimmed back (`PLAYER_BACK_DIM` deleted); the gear is drawn at every
  visible level, which shortens the pill to 210…1140 px against the gear's 1158…1302 px.
  *(Superseded in Part 10 — the owner asked for the gear to follow the toolbar; it is now
  `FULL` only, with its 44dp reserved at both levels so the pill does not change width.)*
- Not re-run: the harness's search / marks / customizer / late-cursor paths, the fling arm,
  and committing or multi-page tilted drags. One unexplained event is written up in §69 (a
  toolbar reveal plus a voice start at 16:20 with no input from this session; a three-minute
  idle test afterwards was silent).

### The level fold and the voice follows the hand (Part 8, 2026-10-20) — history, the lean's sign superseded
- Two owner reports: "khi vuốt ngang thì nó là curl từ dưới lên. Khi vuốt ngang nó phải curl
  hai đầu bằng nhau chứ", and "khi audio đang chạy thì khi người dùng lật trang thì audio phải
  nhảy đến chơi câu đầu tiên của trang đó … khi người dùng nhảy chương cũng vậy".
- **The fold is square to the pull.** Part 7's crease passed through a *corner* of the spine,
  which cannot be level — hence "from below". The crease is now the fold line halfway between
  the sheet's free edge and where it started: `lean = clamp(atan2(dy, |dx|), ±45°)` read under
  12% of the pull and then frozen, `n = (outward*cos(lean), -sin(lean))` (the minus is
  Part 9's correction — Part 8 shipped `+sin` and peeled against the hand),
  `crease = sMax - pull*extent`, and the flap is still the exact mirror of the peeled region
  with the theme's paper and 8.6% print-through. The commit is still half the pull, and at
  exactly half the sheet's free edge is exactly on the spine — that is what holds the gáy now.
- **The voice follows the hand.** `pageShown(page)` is the one funnel for every page change;
  it seeks the active voice to the page's first sentence (`SpeechReader.seek`: playing —
  highlight at once, engine restarts there; paused — place and highlight only). The narrator's
  own follow effect is **forward only**, a contents/search jump sets `jumpTarget` so the voice
  stands aside until it arrives, a multi-page run reports the page it *arrives at* once
  (`commitTurn`), and utterance ids carry an **epoch** compared inside each posted callback so
  a flushed sentence cannot re-assert the old page.
- Measured (`screenshots-spine2/measurements.txt`, `crease.py` — the column with the highest
  median brightness step down the page, which type cannot fake): level 60% crease at 542 px
  (light), 544 (dark), 542 (cream) = 40.3–40.5% of the width, **one x at top, middle and
  bottom**; level 30% free edge 544 px, crease 942 px against 537.6 and 940.8 predicted.
- The four voice cases, from `logs.txt`: forward turn while playing -> page 7, sentence #41;
  backward turn -> page 6, #29; contents jump to chapter 3 -> page 4, #17 then #18–#20 (run
  twice, identical); turn while paused (real tap on ‖ first) -> highlight on page 7 and no
  `reading #…`, still paused.
- Three defects were found and closed while verifying, all in the logs of the first attempts:
  a queued jump reported every page it flipped past (the voice chased it and undid it); a
  flushed sentence's `onStart` arrived after the seek and re-asserted the old page; and a
  playing seek left the old highlight up for an engine start-up. See T-045.
- Not verified: the fling arm (injected `motionevent`s carry no velocity), one book / device /
  density, and the lean's deliberate freeze (a hand that starts level and turns diagonal keeps
  the level fold).
- The old `demoCurl` harness is not used anywhere in this pass, so §58's doubled-render caveat
  is neither reproduced nor fixed here.

### The spine-hinged curl (Part 7) — history, superseded by Part 8
- The complaint was that the crease "trôi tự do giữa trang". It was structural: Part 6's
  fold was a cylinder swept along its own normal, and a line swept along its normal has no
  fixed point. The model is now a **leaf turning about a spine corner**.
- `apex` = a gutter corner, chosen **once** from where the hand goes down (`downY < h/2` ->
  top corner, else bottom), and never moved during the gesture; forward turns hinge on the
  left edge, backward turns on the right. `pull = clamp(|travel| / width, 0…1)`,
  `theta = pull * 90deg`, `n = (outward*sin(theta), down*cos(theta))`, `crease = n . apex`.
  Since the crease is the line through `apex` with normal `n`, `n . apex = crease` holds for
  every angle — **the foot of the crease is on the spine by construction**, not by clamping.
  The far end sweeps up the page as the finger drags: at 0.45 the crease on the evidence page
  runs (0, 2992) → (1344, 1844) for the bottom corner and (0, 0) → (1344, 1148) for the top.
- The flap is the **exact mirror of the peeled wedge** (`reflectedRegion` + `mirrorAbout`,
  one reflection matrix used for both the polygon and the bitmap), not the old
  `0.6366 × sweep` strip: its width follows the pull, and at full pull it lies past the
  spine with the next page whole.
- `COMMIT_FRACTION = 0.5` of the pull (was 0.35 of the extent). Span = `|target - pull|`, so
  duration = 150 + 240·span ms. Both ends move exactly one page.
- The back is `ReaderTheme.paper` + the page mirrored at 8.6% (alpha 22) + a roll gradient +
  a 2.4dp crest + a 1.2dp `blend(paper, ink, 30%)` edge. On dark paper the roll is light the
  sheet catches (`CYLINDER_COLORS_DARK`, halved from the first attempt because 8/18/25% white
  read as grey), and the edge stroke is what makes the sheet legible.
- `foldTilt`, `MAX_FOLD_TILT`, `LEAN_SETTLE`, `LEAN_UPDATE_FRACTION` and `CYLINDER_FACTOR`
  are gone. `holdFold(pull, tiltDegrees, back)` survives as a harness entry, with the second
  argument now meaning "top corner if negative, bottom if not".
- **Known harness quirk (not a user path):** a fold applied by `demoCurl` at startup
  sometimes photographs the page drawn twice with a sub-glyph offset — stable within a run,
  absent from the no-fold control, and absent from every injected gesture at the same pull
  (1.5s-apart captures are byte-identical, so it is a state, not tearing). Three such
  captures are in `screenshots-spine/harness-double-render/`; `report.md` §58 states plainly
  that the cause was not established. If the harness is ever used for verification again,
  apply the fold *after* the first settled frame rather than at startup.

### The curl pass (Part 6, 2026-10-20) — superseded by Part 7, kept as history
- `reader/CurlView.kt` was rebuilt around the fold's own normal: `n = -normalise(drag)`
  clamped to ±60°, `s(p) = p·n`, `extent = sMax - sMin`, `crease = sMax - sweep`.
  Bands are Sutherland–Hodgman half-plane polygons (`foldRegion`/`clipToHalfPlane`) instead
  of `clipRect`; the print-through mirror is taken in the fold's frame; the pull is split
  into `beginDrag`/`moveDrag`/`endDrag` so the harness can drive it. Entry
  `hypot > touchSlop && abs(dx) > 0.35·abs(dy)`; the lean is re-read only under 12% of the
  extent; the turn commits at 35% of the **extent** (was the width) and animates to `extent`.
- **`sweep` had two versions in one morning.** The 08:10 build used
  `sweep = min(hypot, extent)` (the hand's own distance). The shipped 08:45 build uses
  `pull = clamp(hypot/width, 0…1)`, `sweep = pull · extent`, on the owner's later
  instruction — so a leaning fold keeps the crease ahead of the hand (`extent/width` = 1.98
  at 30°, 2.22 at 41°) and a diagonal pull reaches the 35% bar after 0.35 × width of hand
  travel. `CurlPager.swift` at HEAD still does not scale; the difference is deliberate and
  one line in `moveDrag` if it ever has to go back.
- `setPageIndex` / `animateToPage` now return on `index == currentIndex` (the
  `&& direction == 0` escape is gone), so a recomposition can no longer cancel a live curl;
  the re-assertion logs `page N re-asserted mid-turn …: the curl is left alone`.
- Harness extras: `demoCurl`, `demoCurlAngle` (signed degrees), `demoCurlBack`,
  `demoDragX`/`demoDragY`/`demoDragSeconds` (all floats, so `--ef`, `--ez` for the flag).
- Measured off the screenshots (`curl_measure.py`): the crease's lean is `0.00 / +29.98 /
  -30.01 / 0.00 / -29.90 / -19.71 / +20.01°` against `0 / +30 / -30 / 0 / -30 / -19.70 /
  +20` asked for; the square fold's crease sits at x 740 against 739.2 predicted (0.8px) and
  a real 900px drag's at 444.0 against 444.0 (0.0px). The evidence that the rule changed:
  `demoDrag -260,90` logs `finger 275.1 → sweep 460.4/2248.8 (20%)` (was 12%), and the
  `-800,+700` gesture that fell back at 34% now reads 79% and turns.

## Reader gesture guide (2026-10-28)

`ui/ReaderGuide.kt` — a four-step walkthrough card over the first page a reader opens
(turn the page, the top bar, highlighting, listening). It waits for `ready != null` so it
never explains buttons that are not there yet, is suppressed for harness demo launches
(`staged != DemoOptions.None`), and is marked seen on **open** in `readerGuide.xml`, so a
rotation, a second book or a force-stop cannot bring it back. Skip and "Start reading"
both just close it; no setting re-opens it yet. Verified on E1 (`emulator-5554`, now the
only AVD in use): all four steps, Skip, close, reopen another book = no guide; Vietnamese
strings checked by temporarily setting `appLanguage` to `vi`, then removed.
