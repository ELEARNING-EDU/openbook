# Project state — Sách (Android EPUB reader)

Updated: 2026-10-25T10:55+07:00 (Asia/Saigon)
Canonical. Everything about this project lives in this file; `docs/` holds the app's user
guide and `docs/qa/` holds test/QA evidence. Read "Facts at a glance" and "Verified current
state" first, then the Part sections for the reasoning and the measurements behind any number.

## Facts at a glance

| Fact | Value |
| --- | --- |
| App / package | **Sách** · `com.elearning.openbook` (applicationId = namespace; renamed from `com.example.epubreader` on 2026-10-28) |
| Module / size | `:app` only · 37 Kotlin files · **12,191 lines** under `app/src/main/java` |
| Toolchain | Gradle 9.8.0 · AGP 9.4.1 · Kotlin 2.4.20 · JDK 17 (jbr-17.0.14) |
| SDK | compileSdk 37 · targetSdk 36 · minSdk 26 |
| Compose | BOM 2026.09.00 (ui, foundation, material3) · every icon drawn with `Canvas` |
| State / DI | Compose state + `SharedPreferences` + `filesDir/Books`; **Hilt** graph (`ReaderApplication`, `@AndroidEntryPoint MainActivity`); shell state in `AppViewModel` (`StateFlow<AppUiState>`); reader page/failure/saved place in `ReaderViewModel` (`StateFlow<ReaderUiState>`); stores in `ReaderStoresViewModel`; chrome/speech state still local; no Nav, no Room |
| Tests | **110 JVM unit tests** (JUnit 4.13.2) in `app/src/test/java` — `Sentences`, `ReadingPosition`, `EpubArchive`, `ZipExtract`, `EpubCorpusTest` (32 real corpus files: compatibility, DRM, refusal), `HomeSections` (rows, suggestions, finished window), `ReadingLog` (day split, streak, week, codec), `LibraryIteration` (sorting, metadata codec, selection, file safety, Home shelf, language), `VoiceCatalog` (book language, voice filtering and order, grouping, search); `./gradlew :app:testDebugUnitTest` |
| Sample book | `app/src/main/assets/sample.epub`, 9,720 bytes, seeded into `filesDir/Books` |
| APK | debug `13,304,150 bytes` · sha256 `541d5b8125365aa6a2f2a62a4d91f8bdea983e552ce056d404df8d02c36660aa`; installed on emulator `E2` (session 5558/5554) |
| Device | AVD `Pixel_9_Pro_XL` · Android 16 / API 36 · 1344x2992 · density 480. It is shared with other work (CleanStorage, WordQuest) and gets its foreground stolen, so this pass also used a second AVD `OpenBookReview` (same image/config, fresh userdata, `emulator-5558`) |
| Git | branch `android`, **4 commits**, **no remote**. `694069c` is the checkpoint with all app work; the working tree now only carries environment-removed skill files (`.agents/`, `.ai-toolkit/`, `.claude/`, `CLAUDE.md`) and the untracked 16 MB `docs/qa/` evidence folder, both deliberately left out of this commit |
| Tasks | 104 IDs; T-095/T-096/T-097 (UI polish), T-098 (full-player parity), T-099 (mini/full player layout), T-100 (robustness/benchmark/player motion) T-101 (Home parity) T-102 (Hilt/DI) and T-103 (ViewModel/StateFlow + quality gates) and T-104 (reader ViewModel slice) verified; see `TASKS.md`; handoff in `SESSION_HANDOFF.md` |
| Next action | The mini/full player UI pass is finished, device-verified and uncommitted; header is cover-left/text-right per the owner's final choice. T-085 (reading-bar headline at 360dp) and the vertical-swipe decision remain as older backlog |

## Component map

| Path | Responsibility |
| --- | --- |
| `MainActivity.kt` | `enableEdgeToEdge`, builds `DemoOptions` from 24 intent extras (harness) |
| `ui/EpubReaderApp.kt` | app-level state: books, opened book, the tab, shelf progress, one-shot demo budget |
| `ui/MainScreen.kt` | Home / Library / Store tabs, one shared SAF import picker, Home's "carry on" card |
| `ui/LibraryScreen.kt` | shelf, gradient covers, read-percentage bar |
| `ui/AudioPlayerSheet.kt` | the full player: cover, seek slider, back/play/forward, sleep timer, voice picker |
| `ui/ReaderScreen.kt` (1,626) | the reading session: pages, chrome levels, tap routing, sheets, player |
| `reader/CurlView.kt` (995) | hand-drawn curl: drag/fling/taps, `foldGeometry`, crease report |
| `reader/ReaderEngine.kt` (692) | EPUB → chapters → `StaticLayout` pages → page bitmaps |
| `reader/Typesetting.kt`, `reader/Sentences.kt` | one typesetting source for page + preview; sentence spans |
| `reader/SpeechReader.kt` (581) | `TextToSpeech` vi-VN queue, rate, state, voices, callbacks |
| `ui/Components.kt` (1,134), `ui/AppearanceSheet.kt`, `ui/CustomizeScreen.kt` | hand-drawn icons, theme sheet, full-screen customizer |
| `reader/ReaderSettings.kt`, `reader/ReaderTheme.kt` | six themes x light/dark, per-theme config, prefs |
| `reader/BookmarkStore.kt`, `reader/ReadingPositions.kt`, `library/LibraryStore.kt` | prefs `readerBookmarks`, `reading_positions`; `.epub` files in `filesDir/Books` |
| `epub/*` (5 files) | `ZipInputStream` + `META-INF/container.xml` → OPF → spine, `nav`/`ncx`, `Html.fromHtml` (`ChapterImages.kt` is the unverified image work) |

## Scope & requirements
- Product objective: native Android demo EPUB reader (Kotlin + Jetpack Compose) with
  horizontal page turns drawn by a hand-written page-curl `View`, plus a read-aloud
  (TextToSpeech) mode. Vietnamese sample book, Vietnamese UI labels.
- Approved constraints/designs:
  - No third-party libraries. Only the versions declared in `gradle/libs.versions.toml`
    (androidx core-ktx, activity-compose, lifecycle, Compose BOM).
  - EPUB opened with `java.util.zip.ZipInputStream`; `META-INF/container.xml` -> OPF
    (dc:title, dc:creator, manifest, spine); TOC from EPUB3 `nav` (`properties="nav"`)
    else EPUB2 `toc.ncx`. Chapter XHTML -> `Spannable` via
    `Html.fromHtml(html, FROM_HTML_MODE_COMPACT, null, null)`.
  - Pagination with `StaticLayout.Builder` (serif 19sp, ink `#29241F`,
    `setLineSpacing(0f, 1.14f)`, `setIncludePad(false)`, inter-word justification),
    pages cut on line boundaries.
  - Page curl in a custom `View` (no `ViewPager`): under-layer target bitmap, current
    page clipped to the fold, folded strip at `2/pi`, mirrored print-through, cylinder
    gradient, drop shadow, commit at 35% width or a fling.
  - Reader chrome: floating circular buttons, thin top progress line, TOC as a
    `ModalBottomSheet`.
  - `applicationId`/`namespace` = `com.example.epubreader`, app name "Sách".
  - No network permission, launcher activity only.
  - Read-aloud: reads from the first sentence of the open page, continues across pages
    and chapters; tapping a sentence starts there; the sentence being read is highlighted
    and the page turns automatically.
  - Chrome pass (approved 2026-10-18, second round):
    1. Chapter title on the SAME row as the back and TOC buttons, centred, one line,
       ellipsis; no reserved band under the toolbar; body text starts at
       `statusBarTop + 6 + 44 + 14dp`; no earphone in the top bar.
    2. Bottom LEFT: idle = earphone; reading = the book cover spinning like vinyl
       (30 ticks/s, frozen when paused). Press idle = read from the page's first sentence;
       press while reading = open/close the player.
    3. The player unfurls to the right of the disc as ONE 48dp horizontal row:
       `[book · chapter, marquee] [rate] [slower ⏸/▶ faster ✕]`, four 44x44dp icon
       buttons (32dp frames since the player pass); the pill is measured between the disc
       and the gear, so it ends short of the settings button instead of covering it
       (2026-10-20, Part 9 — the gear used to be hidden while the player was open).
    4. Bottom RIGHT gear -> reading-settings sheet (size 16/17/19/21/23, leading
       1.0/1.14/1.3/1.5, preview), saved to SharedPreferences; a change re-typesets the
       book and keeps the reading position (chapter + character offset).
    5. The two bottom buttons sit 42dp above the bottom edge, `"Trang n / m"` below them,
       buttons never overlap text, and one `PageMetrics` feeds paginator and renderer.
    6. Chrome has three levels (2026-10-19): `FULL`, `PLAYER_ONLY` — level (b), the player
       and the settings gear while the voice runs (playing **or** paused); the back button
       used to sit here alone and dimmed, and was removed on 2026-10-20 (Part 9) — and
       `HIDDEN`. The ~4.5s auto-hide falls to `PLAYER_ONLY` while the voice runs and to
       `HIDDEN` otherwise, and stands down only for a sheet / the customizer / search.
       Tapping text at level (b) reads from that sentence and leaves the chrome alone;
       tapping blank paper brings the whole toolbar back; stopping the voice returns `FULL`,
       which then hides on its own. There is never a state with a running voice and no way
       out of the book.
    7. The two top-bar button pairs are capsules: left (back · mục lục), right (tìm ·
       đánh dấu) — one capsule per pair, a hairline divider between the icons, each icon
       44dp, the round button's material and shadow. At level (b) the left capsule and the
       right capsule are both absent — a tap on the page brings them back (2026-10-20,
       Part 9; the left capsule used to collapse to a half-opacity back button). Search (case- and diacritic-
       insensitive, 60 hits) and per-book bookmarks (listed in the contents sheet's second
       tab) are what the right pair opens.
    8. The customizer's preview is typeset by the same `Typesetting` object as the page, so
       "Căn đều hai bên" and the giãn dòng / giãn chữ / giãn từ / lề sliders all move it.
  - Player pass (approved 2026-10-19, fourth round; the iOS side is `standalone-16`):
    9. The player's four control frames are 32dp wide (were 44dp), their glyphs unchanged and
       the row still 48dp tall in a 44dp-tall target; the 48dp the four frames give up belongs
       to the marquee headline. `PanelButton` keeps 44dp as its default (the marks list needs
       it) and the player passes `PLAYER_BUTTON_WIDTH`.
    10. The amber reading band must never appear on a page turned while the voice is paused, and
       nothing but `state == PLAYING` may turn a page. Android's `highlightRects` already
       refused to paint unless `sentencePageIndex[sentence] == page` and skips columns of
       another chapter (§40 of the report); what this pass added is the explicit
       `speech.state != SpeechState.PLAYING` guard on the cursor effect, so the intent is
       visible and logged at the point where the page actually moves.
    11. ~~While the player is open the back button alone dims to ~0.5 opacity~~ — superseded
       2026-10-20 (Part 9): the whole left capsule is drawn at the full chrome level only, so
       there is no dimmed back button at the voice's level at all. `PLAYER_BACK_DIM` is gone.
    12. The player carries an audio progress hairline: 2.5dp, a dim groove with the read part in
       the accent, running from the book/chapter headline round to the close button's frame, at a
       fraction of **characters read / the book's characters** (not pages), refreshed whenever
       the cursor moves to a new sentence.
- Platform: Pixel_9_Pro_XL AVD, Android 16 / API 36, 1344x2992, density 480.

## Verified current state
- Current branch / worktree: Git repository at
  `/Users/cuong/Workspace/CODE/AI/OpenCode/epub-reader-android`, branch `master`, two commits:
  `9e1cdf2` (2026-10-20 14:15 — the Part 7 baseline) and `fa27d98` (2026-10-21 — Parts 8–11
  with that checkpoint set and the AI Development Toolkit). The working tree is **dirty and
  nothing is staged** (Part 12 plus the earlier image/`SearchSheet` work); there is no remote.
  Checked 2026-10-22; the authoritative Git block is "Facts at a glance" at the top of this
  file.
- Verified working features:
  - `:app:assembleDebug` succeeds (JDK 17, Gradle 9.8, AGP/Kotlin from the version
    catalog). APK = `app/build/outputs/apk/debug/app-debug.apk`.
  - Library screen with gradient covers, "＋" import via
    `ActivityResultContracts.OpenDocument` (`application/epub+zip` + `*/*` fallback),
    books stored in `filesDir/Books`, `assets/sample.epub` seeded on first run.
  - Reader: correct Vietnamese diacritics, justified text, paragraph gaps, running head
    on the toolbar row, whole-book footer "Trang n / m" (now 12 pages at 19sp/1.14).
  - Page curl forward and backward, **and on the diagonal** (drag + fling; thirds-taps were
    removed on purpose), TOC sheet, TOC jump.
  - Chrome pass verified on the emulator (2026-10-18, APK 11,965,315 bytes):
    top row on one line; earphone idle / vinyl disc while reading; the unfurled player row
    with marquee, `1x`, tortoise, pause/play, hare, close and no gear; the gear sheet with
    size and leading steppers and a live preview; auto-hide after ~4.5s with the running
    head and page number staying on the paper; a tap on a sentence while reading restarting
    the voice there (`reading #22 on page 4`, skipping #20/#21) and a margin tap only
    hiding the chrome; a genuine mid-curl frame; and the position kept across a restyle at
    three anchors (page 4 and page 8 at a chapter start, page 7 `+800` characters into a
    chapter) with `typeset ...; anchor ... -> page N` log lines proving it.
  - Reading settings persist: `shared_prefs/readerSettings.xml` holds size/leading and the
    narration rate, and a cold launch logs `typeset 21.0sp/1.3: 13 pages` from them.
  - iOS-parity pass verified on the emulator (2026-10-19, APK `a0e9ea10…`, 12,282,420 bytes):
    the three chrome levels under **real taps** — level (b) logs `chrome=player_only
    speech=PLAYING playerOpen=true`, a margin tap logs `chrome=full`, a sentence tap logs
    `reading #7 on page 2` with the chrome unchanged, the player's ✕ logs `IDLE` +
    `chrome=full` and `hidden` 4.5s later, and pause logs `PAUSED` still at level (b). The
    level-(b) accessibility tree holds exactly one toolbar target (the back capsule, 144px =
    one 44dp icon + touch target) plus the player's five. Search was reached by a real tap and
    typed with a real keyboard (`search “ngon hai dang” -> 3 hit(s)`, diacritics folded); a
    bookmark made by a real tap persisted in `shared_prefs/readerBookmarks.xml` and appears in
    the contents sheet's `Đánh dấu (1)` tab; and the preview was measured control by control —
    justified lines end at 1136/1136/1139 px against the ragged 1079/1081/1011/988, lề 12 and
    56dp move the block's left edge to 145 and 277 px, ink 37,829 px -> 72,005 at 26sp and ->
    55,000 for bold. Screenshots `screenshots-parity/01`…`24`, all from that build.
  - Player pass verified on the emulator (2026-10-19, APK `5f99b207…`, 12,288,941 bytes): the
    player's four controls measure 96px = 32dp (their glyph centres 96/96/96.5px apart, and the
    semantics nodes report 96px-wide targets), the headline's box grew to 566px = 188.7dp — the
    exact 144px (48dp) the frames gave up; the audio line is 8 rows (2.5dp) tall with its groove
    running x 252..1163, i.e. from the headline's left edge to the close button's frame's left
    edge, and its fill matches the logged cursor to the pixel (326px at 2380/6660, 354px at
    2588/6660, 267px at 1949/6660); the back button's ink goes 23 -> 135 (α 0.52) in light and
    240 -> 127 (α 0.5) in dark while the contents glyph beside it is byte-identical in both
    states, restored to 23 after the ✕; and pages turned while paused (forward, and back into
    another chapter) carry 0 amber pixels while the cursor's own page keeps its band (75,314
    sampled px). Screenshots `screenshots-player/01`…`15`, all from that build.
  - Part 9 verified on the emulator (2026-10-20, APK `1246acda…`, 12,293,615 bytes), with real
    injected drags and real taps: the fold opens from the **top** for a downward drag
    (`lean=26.6deg`) and from the **bottom** for an upward one (`lean=-26.6deg`), the backward
    diagonal (`lean=30.8deg`, direction -1) opens from the top too, and a level drag
    (`lean=0.0deg`) is still a vertical crease with both ends equal; the voice's level now has
    no node in the toolbar band at all and the gear at 1158…1302 px beside a pill of
    210…1140 px; a real tap on that gear opens `Giao diện & Cài đặt` while the voice reads on;
    and a process launched with the harness extras, after back and a tap to reopen the book,
    stays at `chrome=full speech=IDLE` with no reading line (it used to reach `speech state:
    PLAYING` 2.7 s in). `report.md` §68–§74, `screenshots-part9/01`…`08`, `logs.txt`,
    `measurements.txt`.
  - Launcher icon and "Sách" label.
  - Read-aloud: Vietnamese voice exists (`setLanguage(vi-VN)` -> available, 11 `vi_VN`
    voices from `com.google.android.tts`); reads from the first sentence of the open
    page and continues automatically across pages and chapters; tap on a sentence starts
    from that sentence; the current sentence is highlighted; the page turns
    automatically **with the curl animation**; pressing the button again stops;
    `TextToSpeech.shutdown()` runs when the reader is left (engine unbound).
- Unverified or incomplete features:
  - Audible output: emulator has no speaker path we can listen to. Speech production was
    verified from `GoogleTTSServiceImpl` synthesis logs and the PLAYING/PAUSED/IDLE state
    transitions, not by hearing it.
  - The disc's 30 ticks/s is a code fact (`TICK_MS = 33`); screenshots only show that it
    rotates while reading and is byte-identical while paused.
  - "Frozen when paused" is confounded: Android's cached-app freezer can freeze the whole
    process while it is idle. The app does stop its tick loop when not PLAYING, but a still
    screenshot cannot show which of the two did it.
  - The running head's ellipsis path is implemented (`maxLines = 1` +
    `TextOverflow.Ellipsis` clipped to `headerWidth`) but the sample book's titles are short
    enough that it never triggers on screen.
  - Hare and tortoise are hand-drawn `Canvas` approximations (no icon dependency), not
    recognisable animal icons.
  - `QuoteSpan` recolouring needs API >= 28; only API 36 was exercised.
  - Position retention is page-granular: the anchor is (chapter, char offset) and the page
    containing that offset is chosen, so a paragraph can shift by a line or two.
  - A mid-animation screenshot has to be hunted via device-side `screencap`;
    `adb exec-out screencap` stalls the display pipeline and never samples the animation.

### Appearance (2026-10-19)
- Six themes (`original`, `quiet`, `paper`, `bold`, `calm`, `focus`), each in a **light and a
  dark** variant, switched for the whole app by one moon/sun button in the panel. The library,
  the reader and the panel all repaint from the same live palette state.
- The panel is the iOS-shaped one: header + `Xong`, an `A/A` size pill and an 84dp mode button
  on a `raised` fill, a 3-column theme grid (selected = 3dp accent ring) and a `Tuỳ chỉnh`
  capsule. The old preview and the size/leading steppers are gone.
- `*` on a theme card iff that theme's config differs from its defaults (nine fields: font,
  bold, size, leading, letter, word, margin, columns, justified). Each theme keeps its own
  config; toggling the `Tuỳ chỉnh` enable switch alone changes nothing and marks nothing
  (verified: `custom:1` alone typesets identically to the defaults).
- "Tuỳ chỉnh" opens a separate **full-screen** `Tuỳ chỉnh giao diện`: X (cancel) / ✓ (keep),
  an "Aa" preview, Chữ (font row opening a real device font list + `Chữ đậm`), Tuỳ chỉnh
  (enable switch + line / letter / word / margin sliders), Cột (Tự động/1/2), `Căn đều hai
  bên`, and a red `Đặt lại giao diện`.
- Every control re-typesets for real: logged evidence in one table in the report — leading 1.2
  -> 1.8 is 13 -> 15 pages; letter 0.10 re-breaks 214 -> 251 lines; word 0.50 -> 245 lines;
  margin 32 -> 48dp narrows the column 1151 -> 1055px; `Cột = 2` lays the page out at 545px
  wide as two columns (414 lines); justification off gives 219 ragged lines.
- Sliders, X and ✓ were exercised with real touches: drag -> 1.82/15 pages; X -> 1.2/13 pages
  and the stored value back to 1.2; ✓ -> 1.84 kept, still 1.84 after a cold start.

### The fold and the voice (2026-10-20, Part 8)
- **The crease is square to the pull.** `n = (outward*cos(lean), -sin(lean))` with
  `lean = clamp(atan2(dy, |dx|), ±45°)`, read while the pull is under 12% and then frozen
  (`LEAN_LOCK_FRACTION`), so a level swipe folds level — both ends of the crease the same
  distance from the spine — and a diagonal one leans by its own angle.
- Everything is measured along `n` as `s(p) = p·n`: `extent = sMax - sMin` over the page's
  four corners, **`crease = sMax - pull*extent`**, `revealed = pull*extent`, with
  `pull = clamp(hypot(dx, dy) / width, 0…1)` still the hand measured against the page's width.
  The crease therefore travels from the outer edge (`pull` 0) to the spine (`pull` 1), and the
  folded sheet's free edge — the mirror of the page's outer edge — reaches the spine at exactly
  `pull = 0.5`, which is the commit point. That is where "bám gáy" lives now: in the sheet's
  free edge, not in the crease's foot (a straight crease cannot be both on the spine and level
  with it — that was Part 7's model and it had to go).
- The flap is the **exact mirror** of the peeled region (`reflectedRegion` + `mirrorAbout`, one
  reflection matrix for polygon and bitmap), filled with `ReaderTheme.paper`, the page mirrored
  about the crease at 8.6% for print-through, a roll gradient along `n`, a 2.4dp crest and a
  1.2dp `blend(paper, ink, 30%)` edge. On dark paper the roll is light the sheet catches
  (`CYLINDER_COLORS_DARK`, 4.7/9.4/14% white).
- Bands are Sutherland-Hodgman half-plane clips of the view's corners
  (`foldRegion` / `clipToHalfPlane`, two scratch buffers), and the reflection is taken in the
  fold's own frame, so both lean with the crease.
- The pull begins when `hypot(dx,dy) > touchSlop` **and** `abs(dx) > 0.35·abs(dy)`. The turn
  commits at **half the pull** (`COMMIT_FRACTION = 0.5`) or on a 900 px/s fling; the animation
  runs over `|target - pull|` (150 + 240·span ms, decelerate).
- Measured off the pixels: a level 60% pull puts the crease at 542–544 px = 40.3–40.5% of the
  width and at the *same x* at the top, middle and bottom in light, dark and cream; a level
  30% pull leaves the folded strip between 544 px (its free edge) and 942 px (the crease),
  against 537.6 and 940.8 predicted. Evidence `screenshots-spine2/01`…`05`, `measurements.txt`.
- **The voice follows the hand.** Every page change lands in `pageShown(page)`, which seeks the
  active voice to that page's first sentence: `SpeechReader.seek` moves the highlight at once
  and restarts the engine when playing, and only moves place and highlight when paused. The
  narrator's own follow effect is **forward only**; a contents/search jump sets `jumpTarget`
  so the voice stands aside until the jump arrives; a multi-page run reports the page it
  arrives at once, not every page it flips past (`commitTurn`); utterance ids carry an
  **epoch** (`prefix<epoch>:<sentence>`) compared inside each posted callback, so a callback
  from a flushed sentence cannot re-assert the old page.
- `setPageIndex` and `animateToPage` still return when the index is already `currentIndex`, and
  a mid-turn re-assertion is still logged `page N re-asserted mid-turn (…)`: the curl is left
  alone.
- Harness: `--ef demoCurl <0…1> --ef demoCurlAngle <deg, signed>` holds a fold for a
  screenshot (the angle now means lean), `--ef demoDragX/--ef demoDragY/--ef demoDragSeconds`
  plays a pull through the same drag path, and `--ei demoJumpChapter N` jumps to a chapter a
  moment after the voice starts, through the call the contents rows make.

## Decision index — the rules that constrain new work (2026-10-21)

1. **The curl is a hand-written `View`**, not a pager: under-layer target bitmap, current page
   clipped to the fold, folded strip at `2/pi`, mirrored print-through, cylinder gradient.
2. **The fold is hinged on the spine**; the crease travels `w . |cos lean|` (Part 10). The
   hand's lean is read early and **faded out of the geometry as the pull grows**
   (`LEAN_FADE_FROM = 0.12` -> `LEAN_FADE_TO = 0.62`, smoothstepped), so at full pull the crease
   is upright on the spine — that is how it reaches head and tail (Part 11).
3. **Commit at half the pull** (`COMMIT_FRACTION = 0.5`) or a 900 px/s fling; a near-vertical
   swipe is not a page turn (`DRAG_AXIS_FRACTION = 0.35`, open as T-076).
4. **Chrome has three levels** — `FULL`, `PLAYER_ONLY`, `HIDDEN`; the gear exists only at `FULL`
   and the player reserves 44dp so the pill never changes width.
5. **Tap thirds turn the leaf only while `speechState == IDLE`** (the same condition as "the
   player is closed"); turns go through `jumpToPage` and a second tap queues on `jumpTarget`;
   a third with no leaf behind it falls through to the chrome (T-059).
6. **The voice follows the hand, forward only**, funnelled through `pageShown(page)`.
7. **Typesetting has one source**: the customizer's preview and the page use the same object.
8. **The harness is one-shot per launch** (`staged` / `demoPending`), so opening a book never
   starts the voice by itself.
9. **Persistence is three prefs files plus a folder**: `readerSettings`, `readerBookmarks`,
   `reading_positions` (`page|pageCount|chapter|offset` per book) and `.epub` files in
   `filesDir/Books`.

## Architecture decisions

Written round by round, in the order the rounds happened. Some entries are history: where one
conflicts with the decision index above (for example "page turns are drag/fling only", which
T-059 superseded), the index wins.
- Decision: page bitmaps are rendered lazily into a synchronized 3-entry LRU instead of
  pre-rendering all pages.
  Rationale: a full-screen `ARGB_8888` page is ~16 MB at 1344x2992; pre-rendering blows
  the heap on long books.
- Decision: paragraph spacing is applied by a private-use sentinel `'\uE000'` appended
  after block-closing tags, replaced later by a `ParagraphGapSpan(LineHeightSpan)`.
  Rationale: `FROM_HTML_MODE_COMPACT` emits a single `\n` per block boundary, so plain
  newlines cannot express a visual paragraph gap.
- Decision: sentence ranges are computed on the *same* `CharSequence` the `StaticLayout`
  is built from (i.e. after paragraph spacing), and mapped to pages through a per-chapter
  `lineToPage` array.
  Rationale: highlight geometry and tap offsets must agree exactly with pagination.
- Decision: `Layout.getOffsetForHorizontal(line, x)` after `getLineForVertical(y)` is used
  instead of `Layout.getOffsetForPosition(x, y)`.
  Rationale: `getOffsetForPosition` is a hidden API and does not exist in the platform
  SDK stubs.
- Decision: the page body is clipped to `min(textTop + textHeight, topShift +
  getLineBottom(slot.endLine))`.
  Rationale: a paragraph-final line carries the paragraph gap in its box, so a looser
  clip lets the next page's first line bleed into the bottom of the page (duplicated
  line). Fixing the clip removed the duplicate on page 4 of the sample book.
- Decision: `CurlView.animateToPage` queues a target and plays it as a run of single-page
  curls (up to `MAX_QUEUED_STEPS = 4`, longer leaps are set instantly).
  Rationale: the read-aloud turn can be two pages (a chapter boundary whose previous page
  holds only a sentence tail). Previously such a jump was set instantly, so no curl was
  drawn at all.
- Decision: `PageMetrics` is the single source of truth for the paginator, the page
  renderer and the floating chrome (`hInset 30dp`, header/footer 14dp, gap 14dp, button
  44dp, topButtonGap 6dp, bottomInset 18dp, controlBand 48dp, hMargin 16dp,
  `controlsBottomInset = 18 + 14 + 10 = 42dp`), with the running head riding the toolbar's
  own row between the two buttons.
  Rationale: the request's "buttons must never overlap text" is only checkable if one
  object decides where text may go and where the chrome sits; the old reserved band under
  the toolbar is gone, which is why the sample book went from 11 to 12 pages.
- Decision: page turns are drag/fling only; every tap is reported by `CurlView.onPageTap`
  and interpreted by the reader (`chrome == HIDDEN` -> reveal; `reading && isOnText` ->
  speak from that sentence and re-arm the auto-hide; otherwise `FULL` <-> the resting level).
  Rationale: with the chrome hidden, the page must stay a tap surface for show/hide, so the
  old left/right-third tap-to-turn had to go.
- Decision: the resting chrome level is computed from the **live** `speech.state` at the
  moment the timer fires, never from a value captured at composition time.
  Rationale: a `LaunchedEffect`'s coroutine outlives the composition that launched it, so
  `restingChrome()` reading the composition's `speechState` hid the toolbar outright (level
  `HIDDEN`) when the timer had been armed while the book was quiet and the voice started
  afterwards — the exact state level (b) exists to prevent. Verified fixed: starting the
  voice from the earphone logs `chrome=player_only speech=PLAYING playerOpen=true`.
- Decision: `reader/Typesetting.kt` is the single source of typography for both the page and
  the customizer's preview (body paint, paragraph gap, word-gap spans, recolouring, the
  `StaticLayout` builder). Rationale: a preview that is merely *similar* to the page is how a
  justification switch ends up doing nothing visible.
- Decision: every shadow is hand-drawn (`softCircleShadow` radial gradient,
  `softRoundShadow` stacked rects).
  Rationale: `Modifier.shadow` outlines are polygon-approximated under the emulator's
  software renderer, so circular buttons came out as heptagons.
- Decision: a restyle records `(chapterOfPage, offsetOfPage)` before rebuilding and then
  resolves the page that holds that offset, re-running `HtmlText.toSpannable` per chapter on
  the main thread (spans mutate) with a ~350ms debounce.
  Rationale: the request is "change the type and keep the reading position"; a character
  offset is the only anchor that survives a repagination with a different size.
- Decision: the marquee holds its start position whenever it is not scrolling
  (`scrolling = playing`).
  Rationale: a paused player could otherwise freeze with the headline scrolled out of the
  pill, showing an empty label.
- Decision: the demo extras only drive the view *and* the Compose state together.
  Rationale: the `AndroidView` update pushes the state's page into the view, so an
  imperative jump that does not update the state is silently reverted.

- Decision: light/dark is a **global mode** of the app, not a property of the selected theme;
  `Palette.dark` is derived from the *paper* luminance (as iOS derives `isDark`), and it drives
  `raised` (lighter on dark surfaces) and `hairline`.
  Rationale: the brief asks for one moon/sun button that switches the whole app, and a mode
  derived from luminance keeps a light Quiet (dark-grey surface) legible.
- Decision: the nine typography fields live in a per-theme `TypeConfig`, and `custom` is only
  the gate that decides whether the four spacing sliders apply.
  Rationale: the `*` must mean "this theme was changed", so it compares the nine fields and
  deliberately ignores `custom`.
- Decision: the engine lays text out at **column** width and groups column runs into pages
  (`chunked(columnCount)`), so one pagination path serves both 1 and 2 columns.
  Rationale: nothing else in the reader (taps, highlights, TOC anchors) has to know.
- Decision: word spacing is one `MetricAffectingSpan` per space, not `Paint.setWordSpacing`.
  Rationale: `setWordSpacing` (API 29+) moved neither a line break nor the line count on API 36;
  a metric-affecting span is measured, broken and drawn like any other glyph.
- Decision: fonts come from the device (`Typeface.create` for the generic families,
  `Typeface.createFromFile` for `file:` keys over `/system/fonts`), filtered to drop per-script
  `Noto Sans …` coverage faces and capped at 36.
  Rationale: a real list without a font dependency; the raw enumeration is dominated by
  script-coverage faces that cannot set Vietnamese.
- Decision: a customizer edit applies live (`settings.update`) and X restores the snapshot taken
  when the screen opened.
  Rationale: the page count has to move under the finger, and cancel has to be exact.

## Limitations of the app (as built, 2026-10-22)

1. The Part 11 curl came from the owner's sentence, not their picture — the attachment never
   reached the session (T-075).
2. Five small search/bookmark deviations from the spec are open on purpose (T-061…T-065).
3. Tablet width, a two-column page carrying two chapters, and a long chapter title are
   untested (T-066…T-068).
4. `<img>` does not render (`ImageGetter` is null); the book's CSS is ignored; `<blockquote>`
   gets only a colour (T-069…T-071).
5. Bookmarks are character offsets, not word anchors (T-072).
6. The voice has never been heard through real speakers or Bluetooth (T-073).
7. The fling arm cannot be exercised by `adb` (synthetic events carry no velocity), and the
   150–400 ms commit animation is evidenced by end state only — no film (T-074).
8. Not re-verified after Part 11: dark-mode fold shading, >=3 queued taps, the harness
   search/marks/customizer/`lateCursor` paths, a committing harness fold (T-077).
9. No automated tests, no CI: every guarantee rests on emulator runs. The build is debug-only
   (`versionCode 1`, minify off) and has no release signing.
10. Print-through on the cream back is ~8.6% (a chosen value); a tail-only page (page 5,
    sentence #28) is voiced with no amber band.
11. The full player is content-sized, not the half-screen sheet that was asked for: ~60% of the
    height on 1344x2992 and ~93% on 360x640dp (T-084, verified). In landscape Material caps it
    at 640dp and its content scrolls. At 360dp the reading bar's headline collapses to "Hải Đ"
    (T-085). The Store is offline by design — no catalogue, no `INTERNET` permission.
12. The working tree carries unverified image rendering (`epub/ChapterImages.kt`) that the
    shipped APK already runs: the sample book's page 4 shows a colour-bar image and captions
    like "[Ảnh: Tệp hỏng]", and the voice reads those captions aloud as sentences. T-069's
    premise describes HEAD, not the tree.

## Verification — how to re-check this file

```bash
cd /Users/cuong/Workspace/CODE/AI/OpenCode/epub-reader-android
JAVA_HOME=/Users/cuong/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.epubreader/.MainActivity --ez demoOpenBook true
adb logcat -s ReaderScreen
```

- Expect `BUILD SUCCESSFUL`; the APK hash must equal the one in "Facts at a glance" unless the
  sources changed. `find app/src -newer app-debug.apk` must be empty.
- Signals in logcat: `typeset …; resumed at page N`, `chrome=full|player_only|hidden`,
  `curl pull: … -> turn|fall back`, `crease border (…%, …%) -> (…%, …%); fold lean=…deg`.
- Reading position: `adb shell run-as com.example.epubreader cat
  /data/data/com.example.epubreader/shared_prefs/reading_positions.xml` (0-based page).
- Harness: any of the 24 `demo*` extras, e.g. `--ef demoCurl 0.7 --ef demoCurlAngle -32.7`;
  states fire once per launch and the fold appears ~14-15 s after start.

## Validation log (Parts 1–5, historical)
- Note (2026-10-21): this list records parts 1–5 in one place; parts 6–11 record their own
  commands, hashes and results inside their Part sections. The current build facts are in
  "Facts at a glance" at the top of this file.
- Command / device / date:
  - `JAVA_HOME=.../jbr-17.0.14 ./gradlew :app:assembleDebug` — 2026-10-18, pass.
  - `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk` and
    `adb shell am start -n com.example.epubreader/.MainActivity --ez demoOpenBook true`
    — 2026-10-18, pass.
  - Draw-level instrumentation of `CurlView` (temporary, since removed) — pass: every
    automatic page turn drew curl frames at intermediate fold positions, e.g.
    `drag 188 -> foldX 1155`, `drag 1018 -> foldX 325`, `drag 1227 -> foldX 116`.
  - Final clean build (diagnostics removed), reinstalled: a reading session advanced
    page 2 -> 3 -> 4 with no taps; `pidof` alive; zero `FATAL EXCEPTION` in `logcat`.
  - Leaving the reader after speaking: `onUnbind() called for Google TTS service` and
    `Disconnected from TTS engine` — pass (`TextToSpeech.shutdown()` runs).
  - Chrome pass (2026-10-18, final APK `a054ff38…`, 11,965,315 bytes): `:app:assembleDebug`
    pass; `adb -s emulator-5554 install -r` pass; screenshots `screenshots-chrome/01`…`09`
    and the disc's rotation compared by md5 (two playing frames differ in the disc region,
    two paused frames are byte-identical); restyle logs
    `typeset 23.0sp/1.14: 14 pages; anchor 3. Con tàu không tên+800 -> page 7`,
    `anchor 4. Bão tháng Chín+0 -> page 8`; persistence checked by reading
    `shared_prefs/readerSettings.xml` and by a cold launch logging `typeset 21.0sp/1.3`.
- Appearance pass (2026-10-19, final APK `bb6e5e23…`, 12,239,188 bytes, 13:43):
  `:app:assembleDebug` pass; `install -r` pass; `screenshots-appearance/01`…`20` captured from
  that build (`pm clear` + fresh `am start` per state); the re-captures of the deterministic
  states are pixel-for-pixel identical in their sampled statistics to the earlier ones.
  The `typeset … (lines=N colW=Mpx)` log line is the evidence that each control moved the
  layout; `X`/`✓` and a slider drag were driven by real `input touchscreen` events and checked
  against `shared_prefs/readerSettings.xml` and a cold relaunch.
- iOS-parity pass (2026-10-19, final APK `a0e9ea10…`, 12,282,420 bytes, 16:49):
  `:app:assembleDebug` pass; `install -r` pass; `screenshots-parity/01`…`24` all captured from
  that build (`pm clear` + a cold `am start` per state). Items 1 and 3 were driven by real
  `input tap` / `input text` events with the app's own log checked after each one; the two
  claims that a screenshot cannot settle (the 44dp capsule geometry, the level-(b) button set)
  were read out of the live accessibility tree with `uiautomator dump`; item 2's claim was
  measured off the pixels with a standard-library ink profile (report §31). One real defect was
  found and fixed by this verification (§32): the auto-hide could fall to `HIDDEN` — taking the
  back button with it — while the voice was reading, because the resting level was computed from
  a composition-time value. Re-verified after the fix.
- Player pass (2026-10-19, final APK `5f99b207…`, 12,288,941 bytes, 17:37):
  `:app:assembleDebug` pass; `install -r` pass; `screenshots-player/01`…`15` all captured from
  that build (`pm clear` + a cold `am start` per state), plus `logs.txt` and
  `nodes-and-measurements.txt` in the same directory. Pause, page drags and the player's ✕ were
  real `input tap` / `input swipe`; the accessibility tree was used only for the button widths
  (its x positions for the player row are ~23px off, so the frames were pinned from the glyph
  centres instead); the band, the dim and the progress line were measured off the pixels with a
  standard-library BMP profile (a throwaway `player_stats.py` in the temp dir). One new harness
  hook, `--ei demoLateCursor <page>`, simulates a speech callback that lands after a pause —
  labelled as a simulation in the report (§41), because this device's real engine reports
  `onStart` only while playing and the race cannot be forced from outside the process.
- Result: pass for build, install, launch and the behaviours listed above. Audio output
  not run (no way to listen in this environment; speech was verified from
  `GoogleTTSServiceImpl` synthesis logs and `onStart` callbacks, and in this pass from the
  `speech state: PLAYING/PAUSED/IDLE` transitions plus the amber band moving across pages).

## Known risks / blockers
- The AVD has crashed five times during this work (host windowing/Vulkan, not an app
  fault: no exception in logcat, app process intact). Relaunch with
  `emulator -avd Pixel_9_Pro_XL -no-boot-anim -no-snapshot-save` (no `-no-audio`).
- After such a restart, `adb` commands from a shell that had timed out can be delivered to
  the new boot: taps/drags nobody issued in that session appear, and a restored task
  re-applies its original launch intent (so the app can look like it "started reading by
  itself"). Always `am force-stop` plus a fresh launch after a restart, and give screenshots
  ~2-3s of slack: this emulator's `screencap` lags the input by up to ~2s.
- `adb shell screencap` can stall the display pipeline; use
  `adb shell 'timeout 15 screencap -p /sdcard/x.png'` + `adb pull`, or a device-side burst
  loop, when capturing animation frames.
- The sample book's `dc:creator`/`dc:title` are synthetic demo content, not a real book.
- Git history is two commits (Part 7 baseline, then Parts 8–11 + docs + toolkit on 2026-10-21),
  so there is a rollback point for the current behaviour — but no remote, hence no off-machine
  copy and no backup if this disk is lost.
- Touch injection can wedge on this AVD: taps and drags stop being delivered to the app
  (the chrome stops answering) while `dumpsys window` still shows the app focused and logcat
  is clean. `adb reboot` clears it (~85s to boot); prefer one short `input` step per shell call
  and verify each step before relying on the next.
- `uiautomator dump` is the fast way to get exact node bounds for a touch test (labels, `SeekBar`
  and switch positions); the reader itself exposes no labelled nodes because the page is a
  bitmap drawn by `CurlView` — an empty dump there is expected, not a fault.
- `Paint.setWordSpacing` was inert on API 36 (see the report, §22); word spacing is span-based
  and only API 36 was exercised.
- A `LaunchedEffect` coroutine outlives the composition that launched it, so any decision taken
  inside one (what level the chrome falls to, whether there is anything left to hide) must read
  the live `MutableState` — `speech.state`, `chrome.visible` — and never a `val` computed during
  composition. This bit once already (`report.md` §32: the toolbar and its back button hid
  themselves while the voice was reading) and a still screenshot cannot show it.

## Next milestone (rewritten 2026-10-22, Part 12)
- Part 12 is built, installed and verified by hand, including T-084's small-phone and landscape
  check. The next action is **T-085**: on a 360dp screen the reading bar's headline collapses to
  "Hải Đ" because the rate label and five 48dp controls take the row.
- Then the two Part 11 questions still open: the owner's curl picture (T-075) and whether a
  nearly vertical swipe should turn the page (T-076, `DRAG_AXIS_FRACTION = 0.35`).
- The rest of the open work is IDs **T-061…T-079** plus T-085 in `TASKS.md` — parked gaps,
  tablet, two columns, `<img>`, CSS, bookmarks, audio, film.
- Nothing in the tree is committed: this session's Part 12 files, the earlier image/`SearchSheet`
  work, and the docs share one dirty working tree.
- History — Part 11 answered three reports: the fold finishes head-to-tail (`leanScale`), the
  progress line sits flush on the capsule's floor, and the page's thirds turn while the player
  is closed. Evidence in `report.md` §85–§90 and `screenshots-part11/`.
- History — before that: the fold is square to the pull (a level swipe folds level; a diagonal
  one leans) and a reading voice follows the reader's hand, both built, installed, verified
  and written up (`report.md`, Part 8, §60–§67; `screenshots-spine2/01`…`10`, `logs.txt`,
  `measurements.txt`, `crease.py`, `spine2.sh`) on top of Part 7's spine-hinged fold, Part 6's
  diagonal fold, the player pass (Part 5, `screenshots-player/`) and Parts 1–4
  (`screenshots/`, `screenshots-chrome/`, `screenshots-appearance/`, `screenshots-parity/`).

## Part 13 (2026-10-23) — current: the first JVM test suite

- `app/src/test/java` now holds 26 JUnit 4 tests over the pure-JVM core; no production code
  changed for them. Dependency: `junit = "4.13.2"` in `gradle/libs.versions.toml` +
  `testImplementation(libs.junit)` in `app/build.gradle.kts`.
- `SentencesTest` (11): terminators followed by whitespace, decimals/thousands not split,
  closing quotes/brackets, hard line breaks, ellipsis, no-terminator text, empty/blank,
  trailing newline, `readable` NBSP/whitespace collapsing.
- `ReadingPositionTest` (6): page 0 of 10 = 0.1, middle = 0.5, last = 1.0, unknown page count
  = 0, page beyond end clamps to 1, negative page clamps to 0. The page-on-screen counts as
  read, so page 0 is 10% — the production comment already states this.
- `EpubArchiveTest` (6): real `sample.epub` metadata (title/author), 7 spine chapters and
  the 7-entry nav TOC with indices 0..6, href resolution (`../`, `.`, fragments, `%20`,
  absolute, traversal above root), and two error cases — no container, container pointing at
  a missing OPF.
- `ZipExtractTest` (3): separator/`./`/leading-slash normalisation; archive round-trip with
  directory entries skipped; an empty file yields no entries.
- Commands and results: `./gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL**, 26/26
  (0 failures/errors/skips), XML under `app/build/test-results/testDebugUnitTest/`;
  `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**, APK `12,182,409 bytes`, sha256
  `26a8795c…d991`.
- Independent KiloCode CLI (`7.8.15`, real child process — Orca has no Kilo worker type):
  `docs/qa/2026-10-23/kilo-review.log` (initial review), `kilo-reverify.log` (fixes +
  build/test re-run, verdict APPROVE), `kilo-device-qa.log` (adb run),
  `kilo-visual-qa.log` + `kilo-device-qa-report.md` (PNG-by-PNG vision verification,
  final reader `Trang 12 / 13`). The first QA pass falsely claimed the reader had opened;
  it was caught and the whole flow re-run with exact 1344x2992 coordinates.
- Device QA evidence: `docs/qa/2026-10-23/device-home.png`, `device-library.png`,
  `device-store.png`, `device-reading.png` (`Trang 2 / 12`), `device-reading-next.png`
  (`Trang 3 / 12`), `device-after-back.png` (Home, no crash), `device-final-reader.png`
  (`Trang 12 / 13`, app left open). `adb logcat -b crash` has no
  `com.example.epubreader` entry (only an unrelated Bluetooth stack crash).

## Part 12 (2026-10-22) — current: the full player sheet and the three tabs

- Two owner requests. (a) Tapping the book's title in the player opens a popup player at roughly
  half the screen — cover, slider, back / play-pause / forward, "hẹn giờ tắt" and "chọn giọng
  đọc" — the way a music or podcast player looks. (b) The main screen becomes three tabs:
  Home, Library, Store.
- **The sheet** (`ui/AudioPlayerSheet.kt`, new) is a `ModalBottomSheet` opened by `onTitleTap` on
  the player's headline (`ReaderScreen`); `BookCover` and `ReadBar` moved out of `LibraryScreen`
  into `Components.kt` so the sheet and the shelf draw the same cover. Its slider is a **draft
  under the finger and one seek on release**: `ReaderEngine.sentenceAtBookFraction()` (new)
  binary-searches the chapter's sentence offsets, so a position is a fraction of the whole book,
  not of the page. A seek while paused moves the page silently through the same path the voice
  uses, so the leaf and the highlight agree; while playing it restarts the engine at the
  sentence. Skip is one sentence either way, with the same silent page snap when paused.
- **Sleep timer** (T-081) is UI state, not a service: five chips (Tắt / 5 / 15 / 30 / 60 phút),
  a live `mm:ss` countdown in the sheet, and `LaunchedEffect(speechState)` cancels it whenever
  the voice goes IDLE — so a stale timer cannot stop a later reading. Expiry calls the same
  `stop()` as the button and leaves the note "Hết giờ hẹn — đã dừng đọc".
- **Voice picker** (T-082): `SpeechReader.voices` lists the device's `vi-VN` voices plus "Mặc
  định"; `setVoice()` disposes and re-creates the engine with the chosen voice and restarts the
  sentence in flight, staying paused if it was paused. The name is persisted as `narrationVoice`
  in `readerSettings` and applied at init.
- **The tabs** (`ui/MainScreen.kt`, new) keep one shared SAF picker hoisted above them, so
  Library is the Part 10 shelf unchanged. Home is a "Đang đọc" card — the book opened in this
  process, else the highest `ReadingPositions` progress — plus a shelf row and the import path.
  **Store is honest offline**: "Cửa hàng chưa kết nối" and an `Nhập EPUB` button, because the
  app declares no `INTERNET` permission and the owner chose this over a fake catalogue. The
  selected tab lives in `EpubReaderApp` beside the opened book, so Back from a book returns to
  the tab it was opened from.
- Taps were driven for real on `emulator-5554`: tabs switch and Back returns to the opening tab;
  the headline opens the sheet; slider `79% -> #75` and `15% -> #18 (page 4)`; skips `#53` /
  `#52`; 15 phút shows `tự dừng sau 14:57`; the harness `--ei demoSleep 1` counts down and logs
  `sleep timer: 1min up -> stopping the voice` then `speech state: IDLE`; `x gft local` logs
  `voice -> vi-vn-x-gft-local (code=0)`, survives `am force-stop` as `saved voice … -> 0`.
  11 screenshots in `screenshots-player-sheet/01`…`11`, 0 `FATAL EXCEPTION`; `12`…`15` carry the
  small-phone and landscape check (T-084).
- The sheet measures **~60% of the height on the 1344x2992 device** and **~93% on a 360x640dp
  screen**, where everything fits without scrolling; in landscape Material caps it at 640dp,
  it is taller than the screen, and one swipe reaches the timer and voice rows (T-084, verified).
  So the "half screen" asked for is not what it does — it is content-sized. On the 360dp screen
  the reading bar's own headline collapses to "Hải Đ" (T-085, open). Skip-while-paused gained
  its page snap late and was not re-tested apart from the slider, which uses the same mechanism.
- Not in the sheet: a chapter list, bookmark button or playback speed (the reader already owns
  the first two); a real media session or lock-screen controls; a Store that fetches anything.

## Part 11 (2026-10-21): the fold finishes head-to-tail, the line on the pill's floor, the thirds
- Three owner reports, all in `report.md` Part 11 (§85–§90), with
  `screenshots-part11/01`…`07` plus `logs.txt` and `measurements.txt`: a progress bar still not
  on the bottom edge that must run end to end without spilling; a curl that hugs the spine but
  "does not curve up to the head of the book" going up (or the tail going down); and, with the
  player closed, side taps that should turn the leaf while the middle shows the buttons.
- **The curl's reach — the real defect.** A leaning crease is not merely a tilted fold on a
  tall page: at 30° it walks `h·tan30°` = 1,727 px sideways over the page's 2,992 px height,
  which is more than the page's 1,344 px width. So a leaning crease must exit through the
  fore-edge, the fold can only ever be a corner triangle, and at full pull a flat triangle of
  the old page was still lying in the corner when the turn committed (Part 10 logged
  `pull=1.0 lean=30 -> (0%,0%) -> (100%,78%)`). `foldGeometry` now fades the hand's lean out
  of the geometry as the pull grows — `leanScale(pull)`, all of it up to 0.12, smoothstepped to
  0 by 0.62 — so the crease turns upright and finishes square to the gutter, where a real leaf
  does. **Everything else is untouched**: the normal's sign (Part 9's up/down direction), `far`,
  `travel = w·|cos lean|` (Part 10's spine rule) and the pull's measurement. After: real drag up
  59% → `(43%,0%) -> (41%,100%)`, back drag 54% → `(54%,0%) -> (41%,100%)`, harness 100% →
  `(0%,0%) -> (0%,100%)` with nothing left flat. The up/down difference now lives in the *early*
  part of the gesture, which is where the corner actually lifts.
- `creaseReport` prints the fold's own lean (`fold lean=…deg`) and reports the bound-edge test
  as `n/a (back)` for a back turn — the forward turn's invariant is that the crease may touch
  the spine's corner at full pull but never peel it; a back turn opens the previous page from
  the gutter by design, so the test says nothing there.
- **Progress line.** The 14dp start / 36dp end / 3dp bottom insets are gone:
  `align(BottomCenter) + fillMaxWidth() + height(AUDIO_LINE_HEIGHT)`, with the capsule's own
  24dp `clip` rounding the line's ends off *inside* the corners (that clip is why no inset is
  needed any more). Measured: line rows 2858…2865, capsule bottom 2866 — 0 px gap — straight run
  x ≈ 245…1104, accent at 34% of the line against the voice's 35.7%, nothing outside the pill.
- **The thirds.** `pageTapped` reads the page's thirds only while `speechState == IDLE` — the
  same condition as "the player is closed" — left third back, right third on, middle the chrome
  (shown/hidden). It turns through `jumpToPage`, so the leaf turns with the same single curl the
  voice uses, and bases a second tap on `jumpTarget` rather than on the leaf under the sheet, so
  two taps land two leaves. A third with no leaf behind it falls through to the chrome, so a
  hidden toolbar is never stranded. The voice's own contract is untouched: while a book is being
  read (playing *or* paused) text taps still read from that sentence.
- **`DRAG_AXIS_FRACTION = 0.35` is unchanged**: a swipe must be at least 35% sideways to be a
  page turn. A near-vertical swipe is still not a turn — flagged to the owner, because their
  wording suggests an upward swipe and this threshold, not the fold, is what would bite there.
- The owner's attached picture never reached the session (no image from today in the session's
  attachments folder; the prompt journal holds only text), so the curl item is an interpretation
  of the sentence against the geometry that was actually drawn. Stated in `report.md` §89 so a
  wrong reading is cheap to correct.
- Not re-run this pass: the harness's search / marks / customizer / late-cursor paths, the fling
  arm, a committing harness fold, dark-mode fold shading, three or more queued taps, and film of
  the commit animation (`ffmpeg` still absent). The emulator's glyph atlas corrupted two capture
  frames mid-pass; both were thrown away and every shipped frame was checked for clean text.

## Part 10 (2026-10-21): the gear's level, the sheet's `*`, the unfurl, the fold's travel, the remembered page
- The owner reported five things, all in `report.md` Part 10 (§75–§84), with
  `screenshots-part10/01`…`17` plus `logs.txt` and `measurements.txt`:
  the gear not hiding with the other buttons; "Xong" still in the settings sheet and the `*`
  appearing on a size change; no animation on the player and a progress bar that was not at
  the capsule's floor; the curl still coming away from the spine when pulled diagonally; and
  the reading position not being remembered or shown on the shelf.
- **Gear.** Now `if (ready != null && chrome == ChromeMode.FULL)` with a 44dp `Spacer` in the
  else branch, so the pill (weight 1f between disc and gear) keeps the same width at both
  levels — measured off the frames: the capsule's right border starts at x = 1137 in both.
- **Settings sheet.** `onDone` and the "Xong" capsule are gone; the sheet dismisses by scrim
  or drag. `TypeConfig.signature()` no longer contains `sizeSp`, so the `*` marks the eight
  fields the customizer owns and a size change marks nothing (measured: size 21 -> 23sp, no
  `*`; Cột auto -> 1, `*`). The customizer's own "Cỡ chữ" slider writes the same field, so it
  does not mark either — written down as an open point rather than guessed at.
- **Player.** `PlayerSlot` = `AnimatedVisibility` with `fadeIn + expandHorizontally(
  expandFrom = Start)` / `fadeOut + shrinkHorizontally(shrinkTowards = Start)`,
  `PLAYER_UNFURL_MS = 260`. A named composable because `RowScope.AnimatedVisibility` cannot be
  resolved from inside the `Box` that owns `weight(1f)` (`DSL_SCOPE_VIOLATION`).
- **Progress line.** Bottom inset 5dp -> 3dp. Three is the smallest figure that works: with a
  24dp capsule corner and a 14dp line start, a 3dp inset puts the corner arc at 12.4dp (clear)
  and 2dp would put it at 14.4dp (biting the line's first pixels).
- **Curl travel.** `travel` is now `max(w * |cos lean|, 1)` — the width's projection on the
  normal — instead of the page's whole diagonal span. At full pull a leaning fold ends exactly
  on the gutter corner nearest the pull instead of 1,496 px past it (which put the whole bound
  edge in the peeled half: the page came off the spine). Level fold unchanged. `creaseReport`
  logs where the crease meets the border and whether any of the bound edge is peeled, with a
  0.5px tolerance so a tangency at full pull is not reported as a peel.
- **Reading position.** New `reader/ReadingPositions.kt` (`page|pageCount|chapter|offset` per
  book id, prefs `reading_positions`). The reader records on every `pageIndex` change —
  curl, contents, search hit or voice — and restores through the text anchor
  (`pageForChapterOffset`), so a re-typeset lands on the words. The shelf draws
  `(page + 1) / pageCount` as a hairline bar plus a percentage under each tile.
- Verified with real input: both committing tilted drags (lean ±32.7°, 70% pull), the taps
  through both chrome levels and the settings sheet, a process restart resuming on page 4,
  and the voice reading from page 5 to page 13 with the store following it to `11|14|6|0` and
  the shelf reading 93%.
- Not re-run this pass: the harness's search / marks / customizer / late-cursor paths, the
  fling arm, and a *harness* fold that commits. The unfurl has no film (the 260ms open is
  shorter than `screencap`'s start-up; `14` was caught with the emulator's animator scale at
  10×, restored afterwards).
- Open with the owner: whether the customizer's own size slider should mark the card (it does
  not — see above); and Part 9's §69 self-start stays unexplained, nothing of the kind
  happened this pass.

## Part 9 (2026-10-20, later): the lean's sign, the one-shot harness, the level-(b) chrome
- The owner reported four things: the voice starting on its own when a book is opened; the fold
  leaning against the hand; the back button at the voice's level; and the player covering the
  gear. All four are in `report.md` Part 9 (§68–§74), with `screenshots-part9/01`…`08`.
- **Lean sign.** The peeled side is the side the sheet is pulled *away* from, so the normal runs
  against the displacement: `n = -normalise(drag)`; with the frozen lean that is
  `(outward*cos(lean), -sin(lean))`. A downward drag now opens the fold from the top and an
  upward drag from the bottom, verified with held real drags (`lean=26.6 / -26.6 / 30.8 /
  0.0 deg`). A level swipe is unchanged: vertical crease, both ends equal.
- **The harness's staged states are one shot per launch.** `EpubReaderApp` holds `demoPending`,
  `ReaderScreen` takes `demoPending` + `onDemoUsed`, and the staging effect spends it when it
  finishes (`staged = DemoOptions.None` afterwards, while `demo` itself is untouched so the
  sheets still open on the harness's tab/query). Before this, a process launched with
  `--ez demoReading true` replayed the staging — including starting the voice — every time a
  book was opened, which is the reproducible shape of "tự chạy audio khi mở sách".
- **Level (b) chrome.** The left capsule (`back · mục lục`) is now drawn at `chrome == FULL`
  only, so the voice's level has no dimmed back button; `PLAYER_BACK_DIM` is deleted. The gear
  is drawn at every visible level (FULL and PLAYER_ONLY), so the player's pill — measured in
  the weighted box between the disc and the gear — ends 18 px short of it (row 210…1140 px,
  gear 1158…1302 px). A tap on the page still brings the full toolbar back.
  *Superseded in Part 10: the owner asked for the gear to follow the toolbar, so the gear is
  now drawn at `FULL` only, with its 44dp reserved at both levels.*
- Not re-run this pass: the harness's search / marks / customizer / late-cursor paths, the
  fling arm, and multi-page or committing tilted drags.
- Still open with the owner: whether "ẩn luôn cái nút back" meant the dimmed back at the
  voice's level (implemented) or the back button in the reader altogether (not implemented).

## The fold square to the pull (Part 8, 2026-10-20) — history, superseded by Part 9 for the lean's sign
- The owner's report: "khi vuốt ngang thì nó là curl từ dưới lên. Khi vuốt ngang nó phải curl
  hai đầu bằng nhau chứ." Part 7's hinge was a *corner* of the spine, so a sideways pull put
  the crease on a diagonal — one end at the gutter, the other 1143 px down the outer edge.
- A straight crease cannot both pass through the spine and be level with it: those two
  requirements are one line at infinity apart. What hugs the gutter in a real leaf is its
  **free edge**, which travels from the outer edge to the spine and reaches it exactly at the
  commit point. So the crease is now the fold line halfway between the free edge and where it
  started, and its angle comes from the pull itself:
  `n = (outward*cos(lean), -sin(lean))` (the minus is Part 9's correction; Part 8 shipped
  with `+sin` and peeled against the hand), `lean = clamp(atan2(dy, |dx|), ±45°)` frozen under
  `LEAN_LOCK_FRACTION = 0.12`, `extent = max(p·n) − min(p·n)` over the page's corners,
  `crease = max(p·n) − pull*extent`. The flap is still the exact mirror of the peeled region
  and the back is still `ReaderTheme.paper` + 8.6% print-through.
- Consequences: a level 30% pull shows a folded strip between 40% and 70% of the width with
  the flat page left of it and the next page right of it; a level 60% pull puts the sheet's
  back over the left 40% with its free edge already past the spine; a full pull puts the
  crease on the spine and the leaf has turned. A diagonal pull leans the crease by its own
  angle (26.6° for a `(−800, +400)` pull).
- Measured off the pixels (`crease.py`: the column with the highest median brightness step
  down the page, which type cannot fake): the 60% fold's crease is at 542 / 544 / 542 px
  (40.3–40.5% of the width) and **the same x at the top, middle and bottom** in light, dark
  and cream; the 30% fold's free edge is at 544 px and its crease at 942 px, against 537.6 and
  940.8 predicted.
- The **voice follows the hand**: every page change lands in `pageShown(page)`, which seeks
  the active voice to the page's first sentence (`SpeechReader.seek`: playing restarts there
  with the highlight moved *before* the engine restarts, paused only moves place and
  highlight). The narrator's own follow effect is **forward only** (a cursor behind the page
  is a sentence spanning it, not a reason to turn back), a contents/search jump sets
  `jumpTarget` so the voice stands aside until it arrives, a multi-page run reports the page
  it arrives at once instead of every page it flips past, and utterance ids carry an **epoch**
  so a callback from a flushed sentence cannot re-assert the old page.
- Verified with real injected drags and one real tap: forward and backward turns while
  playing move the voice to the new page and the page stays; a contents jump while playing
  takes the voice into that chapter (page 4, #17, then #18–#20); a turn while paused moves the
  highlight and stays paused; and a hand turn straight after a jump is followed again, which
  is what `pageShown` clearing `jumpTarget` on *any* notification buys.
- Not verified: the fling arm (injected events carry no velocity), one book / device /
  density, and the lean's deliberate freeze — a hand that starts level and turns diagonal
  mid-gesture keeps the level fold.
- Still open, and deliberately untouched: the five small gaps between the search/bookmark
  surface and its spec — the field placeholder reads "Nhập chữ cần tìm…" where the spec says
  "Tìm trong sách"; a snippet is `maxLines = 3` where the spec says 2; a search row says
  "Trang 4" where the spec says "tr. 4"; the marks list removes a row with a ✕ where the spec
  asks for a swipe; and the search surface is a full screen rather than a sheet.
- Natural next steps if more is wanted: a check at tablet width (where `Cột — Tự động` would
  resolve to 2), the two-column page that carries two chapters (the only case where the
  highlight's chapter comparison is load-bearing), `<img>` support, the book's own CSS, a
  `<blockquote>`/quote style beyond the colour, word-anchored rather than page-anchored
  bookmarks, or a real audio-output check on a physical device.

## The spine-hinged curl (Part 7) — history, superseded by Part 8
- Kept because the numbers are still the reference for the committed-turn behaviour and
  because the reason it was replaced is worth having on record: a crease through a corner of
  the spine cannot be level, so a sideways pull read as a peel from below.
- The owner's report was that the fold did not hold the gutter: "nếp gấp/trục cuộn không
  đứng ở mép gáy mà trôi tự do giữa trang". Part 6's cylinder could not satisfy it — a
  crease swept along its own normal has no fixed point. The model was a **leaf turning about
  a corner of the spine**: `apex` picked once from where the hand went down,
  `theta = pull * 90deg`, `n = (outward*sin(theta), down*cos(theta))`, `crease = n . apex`,
  so the foot of the crease was on the spine by construction.
- `pull = clamp(|finger travel| / page width, 0…1)`; commit at **half the pull**
  (`COMMIT_FRACTION = 0.5`) — both still true in Part 8; the animation's span is
  `|target - pull|`.
- The back face, the roll, the crest and the 1.2dp `blend(paper, ink, 30%)` edge are
  unchanged in Part 8, as is `CYLINDER_COLORS_DARK` (4.7/9.4/14% white).
- The diagonal-lean machinery of Part 6 (`foldTilt`, `MAX_FOLD_TILT`, `LEAN_SETTLE`,
  `CYLINDER_FACTOR`) stayed gone in Part 8, though "lean" itself came back — now read from
  the pull's direction rather than from a separate tilt state.
- Verified with real injected gestures: 18% and 45% pulls fell back with the footer
  unchanged, 60% turned one page forward and one page back, and the fold hugged the gutter in
  light, dark and cream. The evidence page's crease at 45% ran (0, 2992) → (1344, 1844),
  exactly what `0.6494x + 0.7604y = 2275` predicts (`report.md` §54).
- One thing parked with a caveat: the `demoCurl` harness, which applies a fold at startup,
  sometimes photographs a doubled render of the page (§58). It never appears on the gesture
  path; three such captures are kept in `screenshots-spine/harness-double-render/`. Part 8
  does not use that harness at all.

## Reading iteration — 2026-10-23 (Home, Library, language, pill)

The iteration turns the single-shelf demo into a small reading app while keeping every
existing reader behaviour. All work is uncommitted on branch `android` at `315c8e6`.

- **Home** — a pager of genuinely in-progress books (`0 < fraction < 1`, most recently read
  first, id tie-break) plus the full shelf; `ui/HomeShelf.kt` is the pure rule and has JVM
  coverage.
- **Library** — list/grid and three sorts are remembered in `libraryList` preferences
  (`ui/LibraryScreen.kt`, `library/LibraryPreferences.kt`). Sorting uses a `Collator` for the
  device locale, and every order ends on the book id, so ties cannot reshuffle. "Recently
  imported" keys on the first-seen timestamp only; a rename cannot move it. Rows show a
  percentage, no bar; the reader still owns page controls.
- **Metadata** — `library/BookMetadata.kt` + `BookMetadataCodec` keep the reader's title and
  author in `libraryMetadata` prefs, escaped so any title survives. Editing keeps the file,
  id, reading position and bookmarks. Delete (`LibraryFiles`) only resolves direct children
  of `filesDir/Books`; the external EPUB the picker returned is never referenced, and a
  failure on one file does not abort the others. Deleting the seeded sample sets
  `sampleDeleted`, so it is not reseeded.
- **Shell vs reader** — `ui/ShellTheme.kt` derives Home/Library/Store colours from
  `isSystemInDarkTheme()`; `ReaderTheme` keeps its own palette and the saved reader theme, so
  a reading theme never leaks into the shell and a phone dark mode never changes the page.
- **Language** — `settings/AppLanguage.kt` wraps the base context in `MainActivity`; System
  follows the phone (Vietnamese only when the phone is Vietnamese, English otherwise), and
  the two explicit choices override this app only. Choosing a language recreates the
  activity; the device locale is never written.
- **Navigation** — MainScreen keeps a floating Home/Library/Store pill: bottom-left,
  safe-area aware, detached from the bottom, active item shows icon + label, hidden in the
  reader, with the trailing space left for a future listening control (not built).
- **Localization** — English is the base resource, Vietnamese is `values-vi`; plurals cover
  book counts, selection, delete titles, minutes and search results. Reader-open and import
  failures show a generic localized line; the exception text stays in logcat.
- **Verification** — 43 JVM tests (26 existing + 17 new) pass; `assembleDebug` succeeds (APK
  `12,711,557 bytes`, sha256 `b6b3014b…`). KiloCode review found one real ordering issue
  (RECENT used title as a tie-break), now id-only with a regression test; Kilo device QA
  found the Select button preselecting every book, fixed and re-verified on device.
  Kilo's claimed reader and edit failures were tester artifacts: the reader was pointed at
  the deliberately truncated `suite006-qa-anh-thu-cut.epub` leftover, and the edit probe
  checked a different card among three books sharing one title. Both are documented with
  evidence in `docs/qa/2026-10-23-iteration/qa-lead-verification.md`; the edit path persists
  (`device2-4-edit-persisted.png`) and the original title was restored.
- **Device state** — app left on Home, English, light; `cmd uimode night no`, phone locale
  never changed, no app data wiped.

## UI polish — 2026-10-24 (headings, progress, covers, shelf controls)

The owner asked for four surface changes; no reader, import, edit or delete logic moved.

- **Headings** — the in-screen "Trang Chủ"/"Thư Viện"/"Cửa Hàng" title rows are gone. Home
  keeps its right-aligned settings gear, Store keeps its `DEMO` badge, Library keeps only its
  control capsule. The labels live on in the bottom tab bar (`MainTab.labelRes`); the now
  unused `home_title`/`store_title`/`library_title` strings were deleted from both locales.
- **Home pager** — `ContinueCard` no longer draws `ReadBar`; the card ends on the
  `Page n / m · N%` line and "Read on". Covers on the shelf below now print title + author
  under the cover, because the cover no longer carries them.
- **Covers** — `BookCover` is gradient + spine + hairline border only; the title/author block
  inside it was removed. Every caller (Home shelf, Home pager, Library grid/list, player
  sheet) therefore shows text outside the cover: the pager has it beside the thumbnail, the
  grid under the cover, the list beside it.
- **Library controls** — the `+` import and a new `…` sit in one 40dp pill at the top right
  (`RoundedCornerShape(20.dp)`, a 1dp hairline divider between them). The `…` opens one
  `DropdownMenu` (`LibraryMenu`) holding Grid/List, the three sorts, a divider and Select
  (Select hidden while empty). `ViewToggle` and the old separate sort `TextButton` are gone.
  The selected option is drawn in the accent colour. `library_options` ("Shelf options" /
  "Tùy chọn thư viện") is the new accessibility description for that button.
- **Verification** — `:app:testDebugUnitTest` 43/43 pass, `:app:assembleDebug` succeeds (APK
  `12,710,813 bytes`, sha256 `7cf70633…`). Device on `emulator-5554`: Home shows no title,
  no bar, text-free covers with title/author/% beneath; Library grid shows titles under the
  covers; the paired capsule opens the full menu; List chosen from the menu renders rows;
  Select opens at "0 selected" with every tick empty; Store keeps only the DEMO badge.
  Screenshots live in the session temp dir (`home2.png`, `lib4.png`, `menu3.png`, `list2.png`,
  `select1.png`, `store1.png`). One stray tap opened a book and moved a sample from 58% to
  92%; the reading position is real app data, not test data.

### Second pass — 2026-10-24 (Home shelf removed, toolbar sizes, grid menu below, menu icons)

- **Home** — the whole "Your library" block is gone: heading, count, `LazyRow` and the
  trailing spacer. Home is now the "Reading now" label plus the swipeable carry-on pager (or
  the empty / nothing-in-progress card). `home_your_library` and the `book_count` plural were
  deleted from both locales; `HomeShelf.inProgress` still drives the pager.
- **Library header** — the shared capsule is gone. Import and overflow are two separate
  `ShellIconButton`s (40dp, the same control used in the reader chrome), import first and
  `…` to its right, 8dp apart. Both align with the status-bar inset row as before.
- **Grid cards** — no title or author under the cover. Below the cover is one row:
  percentage on the left (blank when unread) and the book's `…` button on the right.
  Selection still overlays a badge on the cover and hides the `…`; tapping the card toggles
  the selection as before. List view is untouched.
- **Popup icons** — every `LibraryMenu` row now has a leading glyph: grid (four squares),
  list (three rules), a clock for "Recently imported", a serif `A` for "Title", a
  head-and-shoulders `PersonGlyph` for "Author", and the existing `CheckGlyph` for "Select".
  `ClockGlyph` and `PersonGlyph` are the only new drawing code, both `Canvas`, both 15dp like
  the other circle glyphs.
- **Verification** — `:app:testDebugUnitTest` 43/43, `:app:assembleDebug` OK, APK
  `12,710,217 bytes`, sha256 `1f175487…`, reinstalled. Device: Home shows only the pager;
  Library grid shows percentages and a right-aligned `…` under each cover with no title or
  author; the header pair is two equal 48dp-touch-target buttons; the popup shows all six
  icons with the active row in accent; a real tap on a book's `…` opens Edit/Delete.
  Screenshots (`p2-lib.png`, `p2-libgrid.png`, `p2-menu2.png`, `p2-bookactions2.png`) are in
  the session temp dir.

### Third pass — 2026-10-25 (capsule back, speech-bubble popups, player controls)

- **Library header** — import and `…` are paired again: one 44dp capsule
  (`RoundedCornerShape(22.dp)`, `colors.raised`, 1dp hairline border, 2dp side padding) with
  the `+` *inside* it on the left, a 1dp × 18dp divider, then `…`. Each is a bare
  `IconHit` (40dp, invisible until touched) rather than a `ShellIconButton`, so the capsule
  reads as one control.
- **Popups are speech bubbles** — `BubbleMenu` replaces `DropdownMenu` for both the shelf
  menu and a book's Edit/Delete. It is a `Popup(alignment = TopEnd, offset y = anchor + 4dp)`
  holding a fixed-width card (220dp shelf, 170dp book) with `RoundedCornerShape(12.dp)`, a
  10dp shadow, a hairline border, and a 16 × 8dp `Canvas` triangle in the 7dp strip above the
  card, 14dp from the right edge — i.e. centred under the `…` that opened it. `BubbleRow`
  (icon, label, accent + tick when active, `danger` for Delete) and `BubbleDivider` replace
  `MenuRow`/`MenuDivider` only inside those two menus.
- **Grid `…` has no circle** — `BookActions` is a bare 40dp `MoreGlyph` hit target (no
  background, no border) still sitting below the cover on the right.
- **Full player** — the sheet takes `page`, `pageCount`, `canSlowDown`, `canSpeedUp`,
  `onSlower`, `onFaster`. "Reading progress" is gone; the line above the slider reads
  `page_of_pages` ("Page 13 / 14" / "Trang 13 / 14"). Below the transport there is one
  scrollable row: the speed capsule `−  1.25×  +` (`RateStep` 40dp targets, label backed by
  `speech.rateFactor` Compose state so it recomposes), then `ControlButton` capsules for
  Sleep timer (accent when armed, shows the countdown) and Voice; each opens its own
  `DropdownMenu` with a tick on the current choice. `player_slower`/`player_faster` added in
  both locales; `ChoiceChip` deleted.
- **Rate bug** — the sheet used to receive a snapshot label and the chips wrote the rate
  directly; now every change goes through `SpeechReader.setRate`, which clamps, persists
  (`narrationRateFactor`), logs `rate -> Nx` and restarts the sentence in flight, and the
  label/`canSlowDown`/`canSpeedUp` are getters over `mutableFloatStateOf`.
- **Verification** — `:app:testDebugUnitTest` 43/43, `:app:assembleDebug` OK (APK
  `12,710,585 bytes`, sha256 `085fa0b9…`). Device (`emulator-5554`, light): capsule with the
  `+` inside, both bubbles with their tail under the `…`, grid `…` with no ring. Fresh AVD
  `OpenBookReview` (`emulator-5558`): sheet opened by tapping the reading bar, header
  `Reading · 0.75×` → tap `+` twice → `Reading · 1.25×`, capsule label `1.25×`, and
  `shared_prefs/readerSettings.xml` `<float name="narrationRateFactor" value="1.25" />`;
  `Page 2 / 3` follows the page; the Sleep menu lists Off/5/15/30/60 min with Off ticked; the
  Voice menu lists Default (ticked) plus the 11 `vi-VN` voices.
- **Emulator note** — `Pixel_9_Pro_XL` is shared: `com.cleanstorage.app` and then a Godot app
  (`com.wordquest.game`) took the foreground mid-QA. `OpenBookReview` was created by copying
  `Pixel_9_Pro_XL.avd/config.ini` into a new AVD directory (fresh userdata, no snapshot) and
  runs on port 5558; it needs `adb push` + one import per fresh boot. Delete the directory
  `~/.android/avd/OpenBookReview.avd` (and its `.ini`) when it is no longer wanted.

### Fourth pass — 2026-10-25 (full player + voice picker brought to the iOS parity spec)

Source of truth: `docs/full-player-parity.md` (iOS `9da6500`, the owner's decisions). Only
deviations inside the full player and the voice picker were changed; nothing else was
rewritten and no iOS file was touched.

- **The book's language** — `RawBook.language` now carries `dc:language` (read from the OPF
  in both the DOM and the regex fallback), `ReaderEngine.bookLanguage` resolves it through
  `VoiceCatalog.resolveLanguage`: declared tag → text/script heuristic (Vietnamese
  diacritics, kana, Han) → the phone. `ReaderScreen` hands it to `speech.useLanguage()` when
  the engine is ready.
- **Voice filtering (spec §8.2)** — `reader/VoiceCatalog.kt` (new, pure JVM) decides which
  voices a book may use: same language only (`vi` matches `vi-VN`), all voices when the
  device has none for that language, and a saved voice in another language is ignored rather
  than read with the wrong accent. `SpeechReader.applyLanguage` sets the language, reloads
  the voices, picks saved → book → phone → `vi-VN` → any Vietnamese → first, and persists the
  choice only when `setVoice` returned `SUCCESS`.
- **Chapter skips (spec §4)** — `ReaderEngine.firstSentenceOfChapter` and `ReaderScreen
  .skipChapter(±1)` replace the old sentence skips: `⏮`/`⏭` jump to the head of the
  previous/next chapter, are disabled (30% ink, `enabled=false` in the a11y tree) at the
  first and last chapter, and a skip while paused leaves the voice paused.
- **The picker popup (spec §8.3)** — `VoicePicker` inside `AudioPlayerSheet`: bottom card with
  header, `N voices for this book`, close, search (language, locale code, or voice name;
  a language match keeps the group, a name match keeps the row, no match says so), groups
  ordered book language → Vietnamese → A→Z with names A→Z inside, each row `preview · label
  / quality · locale` · **Use** or a tick.
- **Preview on a second engine (spec §8.3)** — `SpeechReader.preview(voice, sample)` pauses
  the book if it is playing, speaks the sample on its own `TextToSpeech` instance
  (`previewing` drives the stop square), and closing the popup stops it; it never moves the
  reading place.
- **Layout (spec §2)** — cover 96dp · title ≤2 lines · author · chapter in accent · hairline ·
  `Voice: <name>` · a 34dp speed capsule under it · `Page n / m` + `%` · book-wide scrubber ·
  one 48/60/48dp transport row (Sleep, previous chapter, play, next chapter, Voice) with 10dp
  gaps. `⏮`/`⏭` and the voice picker are `TransportButton`s; sleep still opens its menu
  (Off/5/15/30/60) with the countdown under the clock.
- **Strings (spec §11)** — 24 new keys in EN + VI (player open/speed/chapters/voice line,
  voice search/use/more/quality, `voice_count_for_book` plural) and the stale
  `player_reading`/`player_paused`/`player_sleep_left`/`player_no_voices`/`voice_default`
  removed; `speech_no_vietnamese_voice` became `speech_no_voice_for_language(locale, code)`.
  Key sets are identical: 135 / 135.
- **Theme flash (spec §9)** — checked, not changed: nothing sets the Activity/window theme
  from the reader (`enableEdgeToEdge()` stays, `themes.xml` keeps `@color/paper`, and
  `EpubReaderApp` only flips the status/navigation icon appearance). A dark reader leaves no
  dark window behind.
- **Verification** — `:app:testDebugUnitTest` 54/54 (0 failures, 0 errors, `VoiceCatalogTest`
  is 11 of them) and `:app:assembleDebug` green; APK `12,716,645 bytes`, sha256
  `ca4b621bab0a1cd2183fbd5196e0ffb19412c478fffd545e515f204e738753e2`. Device (`emulator-5558`,
  Vietnamese sample): 11 voices, `Voice: x vic local` after a real **Use**, preview logged on
  the second engine and the book left paused; chapter skips logged `chapter 1 -> #6, sentence
  #72` with `⏭` `enabled=false` at the last chapter and `⏮` `enabled=false` at the cover
  chapter, cover state paused throughout; an imported `dc:language=en` copy logged
  `book language -> en`, `setLanguage(en)` and `51 voices for this book`; reopening the
  Vietnamese book after that session still showed a Vietnamese voice (the saved `en-AU` one
  was skipped). Dark reader → Back: the first captured frame after Back is already the light
  Home, no dark frame.
- **Kept as-is** — the speed multiplier maps 1:1 onto `TextToSpeech.setSpeechRate` (Android
  takes a multiplier; the spec's "different scale" note is iOS's `AVSpeechUtterance.rate`).
  An English book with no region gets the engine's first English voice
  (`en-AU-language` here), which is the spec's "book's voice" rule with no region to match.

### Fifth pass — 2026-10-25 (mini player title/speed, full-player header, chapter icons)

The owner's three follow-ups after the parity pass; Android only, iOS untouched, nothing else
in the sheet changed.

- **Mini player** — `PlayerRow` (`ReaderScreen.kt`) now puts the headline and the live
  `speech.rateLabel` in one clickable `Row` (`weight(1f)`, full height): a tap anywhere on
  that part opens the full player (`player_open`), and the rate sits right after the title.
  The old `maxWidth >= 400.dp` gate is gone — at 448dp the pill measures ~313dp, so the rate
  used to be hidden on this very device; the marquee gives up width instead.
- **Full-player header** (`AudioPlayerSheet.kt`) — the header `Row` is the 96dp cover on the
  **left** with the text column on its right (**title / author / chapter / hairline /
  `Voice: …` / `− speed +`**); the hairline, voice line and speed capsule live inside the
  column, so the old strip above `Page n / m` is gone. The owner first read "trái bìa sách" as
  text-left/cover-right, then chose cover-left/text-right after seeing it on the device; the
  swap is one pair of children in the header `Row`.
- **Chapter icons** (`Components.kt`) — `SkipPreviousGlyph` / `SkipNextGlyph` draw Material
  3's `skip_previous` / `skip_next` (bar + triangle) at 18dp in the transport, replacing
  `SeekBackGlyph` / `SeekForwardGlyph` there; the disabled tint is still
  `Ink.copy(alpha = 0.30f)`. The mini player's `±` buttons keep the seek rings because they
  change speed, not chapters.
- **Verification** — `:app:testDebugUnitTest` 54/54 (0 failures/errors) and
  `:app:assembleDebug` green; APK `13,192,927 bytes`, sha256
  `ae4c4cc66e4cd28e2424b9a33a3dbd4c850b3515adc023f676f1bc69da408b42`. Device
  (`emulator-5558`, Vietnamese sample, paused at page 2): the pill shows
  `Ngọn Hải Đăng Cuối Cùng · 1. Ngọn đèn trên mũi đá` with `1×` beside it; a real tap at
  `(500, 2794)` — inside the title/rate target `[252,2722][720,2866]` — opened the sheet;
  the sheet shows the cover on the left and, on its right, title/author/chapter/hairline/
  `Voice: …`/`1×`; `⏮` dimmed (`Previous chapter` disabled at chapter 1) beside a full-ink
  `⏭`, and a real tap on `⏭` logged `chapter 1 -> #2, sentence #17` (page 5 / 16, 20%, both
  skips enabled).
- **Known, not changed** — the default vi-VN voice still reads as `language` because the
  engine names it `vi-VN-language` (see the Fourth pass note).
- **Owner decision and device runs** — the first device run had the text column left and the
  cover right; the owner then chose **cover left, text right** (§ "Full-player header" above),
  so the header `Row` was swapped back and rebuilt. On the final APK: the sheet shows the
  96dp cover on the left and, on its right, title / author / chapter / hairline /
  `Voice: …` / `− 1× +`; a real tap at `(500,2794)` opened it from the pill, and a real tap on
  `⏭` logged `chapter 1 -> #2, sentence #17` (page 5 / 16, 20%). Screenshots `full-cover-left`,
  `mini-final`, `full-final-next`, `mini-final-after-skip`.

### Sixth pass — 2026-10-26 (EPUB robustness + DRM + benchmark + mini-player motion)

Spec: `docs/epub-robustness-player-qa.md` (iOS `98d47c4`). Scope kept to `epub/`, the import
path, the player, and both string sets; no reader rewrite, no new dependency.

- **ZIP reading** — `ZipExtract` now opens the archive as a `ZipFile` (central directory) instead
  of `ZipInputStream`, so stored entries with a data descriptor, ZIP64 extras and the like no
  longer throw (`04-streamed-zip` used to fail with *only DEFLATED entries can have EXT
  descriptor*). Names are normalised to Unicode NFC and looked up ignoring case, which closes
  `05-case-mismatch` and `30-nfd-filename`; `./` prefixes were already stripped.
- **Validation on import** — `EpubArchive.readMetadata` now validates the whole envelope without
  decompressing chapters: container, OPF, non-empty manifest+spine, at least one spine chapter
  that exists in the archive, and no encrypted text. The import copies, validates, and on any
  failure deletes its copy (A1). A missing chapter href is skipped and logged with the file name
  and the list (`epub: <file>: N chapter(s) missing ...`), never silently and never fatal.
- **DRM** — `META-INF/encryption.xml` is read for `CipherReference URI`; only `.xhtml/.html/.htm`
  targets mean protected. Font-only obfuscation opens normally (`09-obfuscated-font`), and
  `10-drm-aes` / `11-lcp` are refused before any chapter is read, so ciphertext can never be
  painted. No decryption, no key handling.
- **Four distinct messages** — `EpubProblem { NOT_ZIP, NO_CONTAINER, NO_OPF, EMPTY_SPINE, DRM }`
  travels on `EpubException`; the UI maps each to `epub_error_*` (EN + VI, keys 140/140). Import
  and the reader's failure overlay both use it; a bad file on the shelf still lists (name
  fallback) but opens to the matching line.
- **Corpus test** — `EpubCorpusTest` (JUnit `Parameterized`) parses all 32 corpus files in place
  (`-DepubCorpus=` override, skipped when absent) and asserts open/refuse, chapter count, and the
  BOM+entity title/author. All 32 pass.
- **Pagination** — chapter HTML conversion is chunked (8 chapters per `yield()`) so the main
  thread is never held for the whole book; `ReaderScreen` logs read, paginate, and first-page
  times.
- **Mini player (D1–D8)** — `PlayerSlot` unfurls with
  `spring(dampingRatio = 0.78, StiffnessMediumLow)` from scale 0.55 anchored at the disc side and
  folds to 0.75 (320 ms), replacing the old tween/expand. With the toolbar showing the pill is
  compact (rate label and `±` hidden, play/pause + `✕` only); with the chrome hidden it shows the
  full set. The disc is frame-time based at 30°/s and holds its angle when paused; the marquee is
  already 26 dp/s with a 40 dp gap; the progress line is now 3 dp; every player button carries a
  TalkBack description (`Phát`/`Tạm dừng`, `Tốc độ`, `Dừng đọc`, `Mở trình phát`). `ANIMATOR_
  DURATION_SCALE = 0` stops the disc and marquee.
- **Measured (release APK signed with the debug key, `emulator-5558`, AVD `E2`, Android 16 /
  API 36, 1344×2992 @480, no hardware GPU claim)** — see the table in `TASKS.md` T-100. Headline:
  `25-big-250-chapters` 1,250 pages, first page **1,044 ms**; `26-1000-chapters` 1,000 pages,
  **1,419 ms**; `27-single-huge-chapter` 586 pages, **3,220 ms** (misses the 1.5 s target —
  one 585k-character `StaticLayout`); `15-image-heavy` 137 ms; `90-real-gutenberg` 15 chapters /
  193 pages, 457 ms. Peak PSS 128 MB / RSS 267 MB on the 1,000-chapter book. Page turns on this
  software-rendered emulator are 96% janky (≈53 ms median) — an emulator number, not a phone one.
  TTS start ≈ 0.6 s. Chapter jump, scrubber drag and import memory were **not measured**.
- **Known gaps** — 32 dp wide player buttons (48 dp tall) remain below the spec's 48 dp touch
  width because of the earlier compact-row decision (T-085); `27` exceeds the open-time target;
  jank was only measured in the emulator.

### Seventh pass — 2026-10-26 (Home parity: stats, shelves, week chart)

Spec: `docs/home-ui-parity.md` (iOS `120aed3`, uncommitted there). Scope kept to Home,
Library's labels/menu, the reading ledger and settings; the reader, TTS, import and player
were not touched.

- **Reading ledger (Phần 0)** — `reader/ReadingLog` (pure JVM): one bucket per local day with
  reading and listening seconds counted apart, `noteSession` for sessions/longest session, and
  `summary(today, goalMinutes)` + `week(today)` for streak, records, totals and the Mon→Sun
  points. `ReadingLogStore` (SharedPreferences `readingLog`) keeps the encoded ledger and the
  goal (1/5/10/15/30/60/120, default 5). `ReaderScreen` ticks one second at a time while the
  screen is RESUMED, stops after five quiet minutes without a page turn or speech, starts a new
  session after a two-minute gap, splits midnight by the local date of each second, saves every
  15 s and on pause/close.
- **Finished (Phần 0.3)** — `BookItem.finishedAt`, `BookMetadata.finishedAt` (codec stays able
  to read the old three-field records) and `LibraryStore.markFinished`. The shelf shows an
  accent `Đã đọc xong` / `Finished` instead of a percentage, the per-book menu has mark/unmark,
  and a finished book leaves Home's reading row while keeping its place.
- **Home (Phần 1–6)** — `library/HomeSections` holds the five rows (reading ≤5 by
  `updatedAt`, added ≤5 unread by import, suggestions from the three most recent reading/finished
  authors with ≥2 matches and no duplicates, finished within 12 months) and the under-six rule
  (`expanded = false` hides the extra rows and the chart). `ui/HomeScreen.kt` draws the 30sp
  title, the achievement card (44dp ring, today/goal line built from real seconds, week/month/year
  and streak), the 300dp reading card, one shared frame per shelf row (20dp left inset, right
  edge 20dp past the screen, 96dp covers, 17sp marquee titles, 11sp month/same-author lines), and
  the Mon–Sun chart (88dp, 18dp bars, accent today, 2dp stubs, 35% goal line). The gear is
  drawn at the top of the scroll only and fades over 200ms; the title scrolls with the content.
- **Library (Phần 0.3/6)** — large `Thư viện`/`Library` title beside the existing import/menu
  pill; finished books show the accent label; the book menu gained mark/unmark.
- **Settings** — the app-settings dialog now carries the daily goal chips beside the language
  rows; changing the goal recomputes streak/days from the ledger (no stored streak).
- **Strings** — 29 new keys, EN + VI, 169/169, including `home_stats_*`, `home_*_month`,
  `home_weekly_*`, `duration_*`, `library_mark/unmark/finished`, `settings_reading_goal*`.
- **Verified** — `:app:testDebugUnitTest` **106/106** (20 new: `HomeSectionsTest` 10,
  `ReadingLogTest` 10), `:app:assembleDebug` green, APK `13,791,702 bytes`. Device
  (`emulator-5558`, Vietnamese + English, light + dark): Home shows the card (`2 phút hôm nay`,
  53% ring, `Còn 3 phút nữa`, week/month/year), 5 reading cards, 5 newly added, 3 same-author
  suggestions, 1 finished book, and the week chart with today's accent bar and the goal line;
  Library shows `Thư viện`, the `Đã đọc xong` label and the mark-finished menu; the goal chips
  open from the gear and the language switch re-renders both screens. Screenshots
  `home-vi`, `home-vi-dark`, `home3-mid`, `home3-chart`, `settings-goal2`, `lib-vi3` (temp dir).
- **Owner follow-ups (same day)** — reading cards now sit 10dp apart via
  `PageSize.Fixed(300.dp)` so the next card peeks; shelf frames wrap their content (even 12dp
  top/bottom) instead of a fixed height; Library's capsule is `…` then `+`; list covers grew to
  64dp with `Trang n / m · %` (or `Đã đọc xong`) under the author; the book menu gained pencil
  and bin icons; the week chart uses fixed `Mo…Su` / `T2…CN` labels (`home_weekdays`) because
  Vietnamese SHORT names all collapsed to "Th".
- **Demo library (same day)** — for the owner's review the emulator's debug build now carries 17
  corpus books copied straight into `files/Books` (debug `run-as`), with `reading_positions`,
  `libraryMetadata` (`importedAt`/`finishedAt`) and `readingLog` seeded to the spec's shape
  (5 reading, 5 added, 4 same-author suggestions, 3 finished, 22 min today, 2-day streak).
  Screenshots `home-full-vi-top`, `home-full-vi-mid`, `home-full-vi-chart`,
  `home-full-top2`/`mid`/`chart` (English).
- **Second owner follow-up (same day)** — the reading card is 152dp tall (was 206dp), so its
  content no longer floats in vertical slack; the floating tab bar sits at `navBottom + 4dp`
  with the content reserve at 88dp; a launch with an empty shelf opens **Library**, whose empty
  state now carries an `Import EPUB` button.
- **Not done** — the achievements *sheet* (the spec marks it optional), a device demo of the
  under-six Home, a real screenshot of the marquee mid-scroll, and the demo dataset's exact
  11-book shape (the QA library holds the corpus imports, including two accidental duplicates);
  TalkBack was checked by content descriptions in code, not by enabling TalkBack.

### Eighth pass — 2026-10-26 (Hilt/DI foundation, P1 of the migration)

Triggered by the skill audit (`android-development-toolkit`: catalog ✅, DI ❌, Hilt ❌).

- **Catalog** — `hilt = "2.60.1"`, `hilt-android`, `hilt-android-compiler` and the
  `com.google.dagger.hilt.android` + `org.jetbrains.kotlin.kapt` plugins live in
  `gradle/libs.versions.toml`; no coordinate or version was written into a build script.
- **Hilt** — `ReaderApplication` (`@HiltAndroidApp`) is the manifest application,
  `MainActivity` is `@AndroidEntryPoint`, and `di/AppModule` provides the six stores
  (`LibraryStore`, `ReaderSettings`, `ReadingPositions`, `BookmarkStore`, `AppLanguageStore`,
  `ReadingLogStore`) as `@Singleton`s from the application context.
- **No manual construction in UI** — `EpubReaderApp` obtains the stores from
  `AppDependenciesViewModel` (`@HiltViewModel`, constructor injection) and hands
  `bookmarks`/`readingLogStore` down to `ReaderScreen`; the two `remember { …(context) }`
  blocks in `ReaderScreen` are gone as well. `AppDependenciesViewModel` is a bridge: it holds
  no screen state, so the next step (P2) can move the state into real ViewModels.
- **Verified** — `:app:testDebugUnitTest` 106/106, `:app:assembleDebug` green; app launches on
  `emulator-5558` without a crash, Home and the Hilt-built reader render, TTS starts
  (`speech state: PLAYING`), and reopening a book resumes its place (`resumed at page 1`).
- **P2 — shell state moved** — `AppViewModel` owns books, reading places, loading, the open
  book, the tab, the summary/week, goal, language, import/delete failures and the demo flag in
  an immutable `AppUiState` published on `StateFlow`; `EpubReaderApp` only collects state and
  forwards events (`open`, `closeBook`, `import`, `markFinished`, …). The reader keeps its
  transient chrome/speech state local but receives its stores from `ReaderStoresViewModel`.
- **P3 — gates** — Kover 0.9.11 (`koverVerifyDebug`), `lint { abortOnError = true }` and
  `CONSTRAINTS.md` with the 80% branch threshold. Lint found three real issues, now fixed:
  `windowLightNavigationBar` on minSdk 26 (attribute removed; the window is styled in code),
  `BREAK_STRATEGY_HIGH_QUALITY` read from `LineBreaker` on API 29+, and a non-observable
  `Locale.getDefault()` in `ReaderScreen` replaced with `LocalConfiguration.current.locales[0]`.
  `lintDebug` now passes. `koverVerifyDebug` **fails at 22.65% branch** (data/domain/VM filter);
  the 80% threshold stands and the shortfall is recorded as waiver W-001 (owner cuong,
  expires 2026-12-31) — no threshold lowered, no class excluded to make it green.
- **Not done** — reader durable state (page, failure) is still `remember` (P2b); ViewModel tests
  (Kover shows 0% on `AppViewModel`); independent QA/review/test subagent runs: every configured
  free model returned **Free usage limit reached**, so the checks above are the primary agent's
  own and no paid fallback was used.

### Tenth pass — 2026-10-27 (48dp player targets)

- The owner chose a shorter headline over narrow buttons: `PLAYER_BUTTON_WIDTH` 32dp -> 48dp
  (height was already 48dp), so every player control meets the 48dp touch target and the title
  takes what is left (the marquee scrolls when it does not fit). Closes T-085.
- Verified: 111/111 tests, `lintDebug` PASS, `assembleDebug` PASS. A device capture of the pill
  was not possible on this AVD because its TTS finishes instantly and the pill closes first.

### Ninth pass — 2026-10-26 (reader ViewModel slice, P2b-a)

- `ui/ReaderViewModel` (`@HiltViewModel`) owns the durable reader state as
  `StateFlow<ReaderUiState>` — `pageIndex`, `failure`, `savedPosition` — and `ReaderScreen`
  reads it instead of `remember`. Every page write (typeset target, curl/page notification,
  contents/search jump, scrubber seek, chapter skip, harness page) goes through `setPage`.
- `SavedPlaces` interface + `ReadingPositions : SavedPlaces` gives the ViewModel a testable
  seam; `AppModule` provides the binding.
- `ReaderViewModelTest` (4 tests) covers begin/saved place, no saved place, immutable page
  updates and failure set/clear.
- Still view-local: chrome mode, sheets, sleep timer and the speech surface (transient UI/engine
  state, not durable screen state).
- Verified: **110/110** tests, `lintDebug` PASS, `assembleDebug` PASS; emulator: open book,
  turn pages, TTS follows to page 3, Back/reopen resumes page 3. `koverVerifyDebug` still fails
  at **22.62%** branch under waiver W-001.
