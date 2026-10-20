# Project State

Updated: 2026-10-19 (+07)

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
       buttons; the gear is hidden while the player is open.
    4. Bottom RIGHT gear -> reading-settings sheet (size 16/17/19/21/23, leading
       1.0/1.14/1.3/1.5, preview), saved to SharedPreferences; a change re-typesets the
       book and keeps the reading position (chapter + character offset).
    5. The two bottom buttons sit 42dp above the bottom edge, `"Trang n / m"` below them,
       buttons never overlap text, and one `PageMetrics` feeds paginator and renderer.
    6. Chrome has three levels (2026-10-19): `FULL`, `PLAYER_ONLY` — level (b), the back
       button alone plus the player, while the voice runs (playing **or** paused) — and
       `HIDDEN`. The ~4.5s auto-hide falls to `PLAYER_ONLY` while the voice runs and to
       `HIDDEN` otherwise, and stands down only for a sheet / the customizer / search.
       Tapping text at level (b) reads from that sentence and leaves the chrome alone;
       tapping blank paper brings the whole toolbar back; stopping the voice returns `FULL`,
       which then hides on its own. There is never a state with a running voice and no way
       out of the book.
    7. The two top-bar button pairs are capsules: left (back · mục lục), right (tìm ·
       đánh dấu) — one capsule per pair, a hairline divider between the icons, each icon
       44dp, the round button's material and shadow. At level (b) the left capsule collapses
       to back alone and the right capsule disappears. Search (case- and diacritic-
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
    11. While the player is open the back button alone dims to ~0.5 opacity (riding the player's
       own 250ms fade: `Modifier.alpha(1f - 0.5f * playerAlpha)`); mục lục keeps its weight, and
       closing the player restores the back button.
    12. The player carries an audio progress hairline: 2.5dp, a dim groove with the read part in
       the accent, running from the book/chapter headline round to the close button's frame, at a
       fraction of **characters read / the book's characters** (not pages), refreshed whenever
       the cursor moves to a new sentence.
- Platform: Pixel_9_Pro_XL AVD, Android 16 / API 36, 1344x2992, density 480.

## Verified current state
- Current branch / worktree: not a git repository. Working tree only, at
  `/Users/cuong/Workspace/CODE/AI/OpenCode/epub-reader-android`.
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

### The fold (2026-10-20)
- The crease has a direction of its own. The pull gives it a normal
  `n = -normalise(dx, dy)` (the sheet is dragged away from the finger), never leaning more
  than **60°** off straight across; the direction of the turn is still the sign of `dx`.
- Everything is measured along `n` as `s(p) = p·n`: `extent = sMax - sMin` over the four
  corners, `crease = sMax - sweep`, and
  **`sweep = clamp(hypot(dx, dy) / width, 0…1) · extent`** — the hand measured against the
  page's width and stretched onto the fold's own extent. A square pull moves the crease
  exactly with the finger (`extent = width`); a leaning one keeps the crease ahead of it
  (`extent/width` = 1.98 at a 30° lean, 2.22 at 41°), so a diagonal pull folds faster and
  reaches the 35% bar after 0.35 x width of hand travel whatever the lean.
  This is the second version of the rule: the first pass (08:10 build) used
  `sweep = min(hypot, extent)`, following an earlier instruction that the `extent/width`
  scaling was the iOS bug. `CurlPager.swift` at HEAD still does not scale, so the two apps
  differ by this one factor by request; changing it back is one line in `moveDrag`.
- The release log prints the mapping, so it can be checked by eye:
  `curl pull: direction=… finger=…px of … wide -> sweep=…/…px (N%) tilt=…° vx=… -> turn|fall back`.
- Bands, all across the same normal: the page still flat at `s <= crease`, a 42dp shadow
  ramp at `crease … crease + 42dp`, and the rolled-over paper at
  `crease - 0.6366·sweep … crease` (paper fill, the mirrored print-through, the cylinder
  gradient along `n`, a 2.4dp crest highlight at the crease). `0.6366 = 2/pi`.
- `clipRect` is gone: each band is a **Sutherland-Hodgman half-plane clip** of the view's
  corners (`foldRegion` / `clipToHalfPlane`, two fixed scratch buffers), because a leaning
  crease cuts along a line and not a vertical edge. At `tilt = 0` every polygon equals the
  rectangle the old code cut, and the arithmetic collapses to it exactly
  (`extent = width`, `crease = width - sweep`, `strip = 0.6366·sweep`).
- The print-through mirror is taken in the fold's own frame — about the point `n·crease`,
  `translate → rotate(-angle) → scale(-0.6366, 1) → rotate(angle) → translate` — so it
  leans with the crease instead of staying vertical.
- The pull begins when `hypot(dx,dy) > touchSlop` **and** `abs(dx) > 0.35·abs(dy)` (a
  mostly up-and-down gesture is not a page turn). The lean is read when the turn begins and
  re-read only while `sweep < 12%` of the extent and the pull is still
  `abs(dx) > 0.5·abs(dy)`, so a drifting hand cannot twist the sheet.
- The turn commits at **35% of the extent** (was 35% of the page's width) or on a 900 px/s
  fling, and the animation runs to `extent` (was `width`).
- `setPageIndex` and `animateToPage` return immediately when the index they are handed is
  already `currentIndex` — the old `&& direction == 0` escape let a recomposition re-assert a
  page **while a curl was live** and cancel it. The re-assertion is now logged:
  `page N re-asserted mid-turn (direction=…, sweep=…): the curl is left alone`.
- Harness: `--ef demoCurl <0…1> --ef demoCurlAngle <deg, signed> [--ez demoCurlBack true]`
  holds a fold open for a screenshot, and `--ef demoDragX/--ef demoDragY/
  --ef demoDragSeconds` plays a pull back through the same `beginDrag`/`moveDrag`/`endDrag`
  the finger drives.

## Architecture decisions
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

## Validation
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
- No git history: there is no commit/rollback safety net for this project.
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

## Next milestone
- Nothing pending: the spine-hinged curl is built, installed, verified and written up
  (`report.md`, Part 7, §53–§59; `screenshots-spine/00`…`12`, `logs.txt`, `spine.sh`) on top
  of Part 6's diagonal fold, the player pass (Part 5, `screenshots-player/`) and Parts 1–4
  (`screenshots/`, `screenshots-chrome/`, `screenshots-appearance/`, `screenshots-parity/`).

## The spine-hinged curl (Part 7, 2026-10-20)
- The owner's report was that the fold did not hold the gutter: "nếp gấp/trục cuộn không
  đứng ở mép gáy mà trôi tự do giữa trang". Part 6's cylinder could not satisfy it — a
  crease swept along its own normal has no fixed point. The model is now a **leaf turning
  about a corner of the spine**: `apex` is a gutter corner picked once from where the hand
  goes down, `theta = pull * 90deg`, `n = (outward*sin(theta), down*cos(theta))`,
  `crease = n . apex`, so `n . apex = crease` at every angle and the foot of the crease is
  on the spine by construction. The flap is the exact mirror of the peeled wedge, so its
  width follows the pull with no constant to tune.
- `pull = clamp(|finger travel| / page width, 0…1)`; commit at **half the pull**
  (`COMMIT_FRACTION = 0.5`, was 0.35 of the extent); the animation's span is
  `|target - pull|`, so a fall-back is quick and a long turn takes longer.
- The back face is `ReaderTheme.paper` (white / black / cream), the page mirrored about the
  crease at 8.6% for print-through, a roll gradient, a 2.4dp crest and a 1.2dp
  `blend(paper, ink, 30%)` edge. On dark paper the roll is *light the sheet catches*
  (`CYLINDER_COLORS_DARK`, 4.7/9.4/14% white) rather than shadow, and the edge stroke is
  what makes the sheet legible at all.
- The diagonal-lean machinery of Part 6 (`foldTilt`, `MAX_FOLD_TILT`, `LEAN_SETTLE`,
  `CYLINDER_FACTOR`) is gone: with a fixed hinge the crease's angle *is* the pull, so a
  leaning crease no longer exists as a concept.
- Verified with real injected gestures (`input motionevent DOWN/MOVE…/UP`, screenshots taken
  with the finger still down): 18% and 45% pulls fall back with the footer unchanged, 60%
  turns one page forward and one page back, and the fold hugs the gutter in light, dark and
  cream. The evidence page's crease at 45% runs (0, 2992) → (1344, 1844), exactly what
  `0.6494x + 0.7604y = 2275` predicts (`report.md` §54).
- One thing parked with a caveat: the `demoCurl` harness, which applies a fold at startup,
  sometimes photographs a doubled render of the page (§58). It never appears on the gesture
  path; three such captures are kept in `screenshots-spine/harness-double-render/`.
- Still open, and deliberately untouched (the pass was scoped to the fold): the five small
  gaps between the search/bookmark surface and its spec — the field placeholder reads
  "Nhập chữ cần tìm…" where the spec says "Tìm trong sách"; a snippet is `maxLines = 3`
  where the spec says 2; a search row says "Trang 4" where the spec says "tr. 4"; the marks
  list removes a row with a ✕ where the spec asks for a swipe; and the search surface is a
  full screen rather than a sheet.
- Natural next steps if more is wanted: a check at tablet width (where `Cột — Tự động` would
  resolve to 2), the two-column page that carries two chapters (the only case where the
  highlight's chapter comparison is load-bearing), `<img>` support, the book's own CSS, a
  `<blockquote>`/quote style beyond the colour, word-anchored rather than page-anchored
  bookmarks, or a real audio-output check on a physical device.
