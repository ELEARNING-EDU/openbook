# Session handoff — Sách (Android EPUB reader)

Updated: 2026-10-21T11:30+07:00 (Asia/Saigon)

Canonical handoff. `PROJECT_STATE.md` holds the state and the reasoning; `TASKS.md` holds the
work; this file says where the last session stopped and what to do next.

## Read first

1. `PROJECT_STATE.md` — "Facts at a glance", then "Verified current state".
2. `TASKS.md` — the `Open` block is the whole to-do list; `Closed` carries the evidence.
3. This file — the last session, the uncommitted work, the open questions.
4. `docs/` is the app's user guide only; it holds no project state.

## Where the work stands

- Part 11 is closed: the fold finishes head-to-tail, the player's progress line sits on the
  capsule's floor edge-to-edge, and a tap in either third turns the leaf while the player is
  closed. All three were verified with real input (T-057…T-060).
- Nothing is half-written. `CurlView.kt` and `ReaderScreen.kt` are at their finished Part 11
  state; the APK matches the sources.
- Device `emulator-5554` is running the app, reader open, resumed at page 10/14, chrome hidden,
  voice `IDLE`.

## What happened in the last sessions (2026-10-21, docs only)

- Session A: built and reported Part 11 (T-057…T-060) with `screenshots-part11/`.
- Session B: surveyed code, Git and docs, applied the AI Development Toolkit checkpoint set,
  then — at the owner's instruction — moved all long-form project data out of `docs/` into the
  three root files, and reserved `docs/` for the app's user guide.
- **No Kotlin, Gradle or manifest file was touched in either session.** The build stayed
  up-to-date, which is the proof.

## Verified in the last session

| Check | Result |
| --- | --- |
| `./gradlew :app:assembleDebug` (JDK 17) | `BUILD SUCCESSFUL in 1s`, 35 tasks up-to-date (docs-only change) |
| APK | `12,302,465 bytes`, 2026-10-21 09:20, sha256 `2c0ac424…b8ab` |
| APK vs sources | `find app/src -newer app-debug.apk` → empty |
| Launch `--ez demoOpenBook true` | focused `com.example.epubreader/.MainActivity`, pid 6142 |
| Reader | `typeset paper/light 23.0sp/1.2 … 14 pages; resumed at page 10 (left on page 10/14, anchor chapter 5+0)` |
| Chrome | `chrome=full` then `chrome=hidden`, `speech=IDLE playerOpen=false` |
| Crashes | 0 `FATAL EXCEPTION` |
| Task list | 78 IDs, unique, 7 cells per row — no malformed row |
| Docs | no `<fill>` placeholder left; no table with mixed column counts |

## Git state — clean, on the owner's instruction

- Branch `master`, **2 commits**:
  - `9e1cdf2` — 2026-10-20 14:15, *EPUB reader for Android: library, reader, themes, narration,
    page curl* (the Part 7 baseline).
  - the 2026-10-21 commit — *Parts 8-11: curl reaches head and tail, flush progress line, tap
    thirds; checkpoint docs*: 8 Kotlin files + `reader/ReadingPositions.kt`, the three root
    checkpoint files, the deletion of the three `docs/` state files, `docs/README.md`, and the
    AI Development Toolkit.
- Working tree clean; nothing staged; **no remote**, so the history exists only on this disk.
- Consequence to keep in mind: code, docs and toolkit share one commit. A `git revert` of it
  removes all three together; make future rounds small separate commits.
- The `.gitignore` still excludes `build/`, `.gradle/`, `.kotlin/`, `local.properties`,
  `.idea/`, `.DS_Store` — build output and the machine SDK path are not in the repo.

## Blocked / awaiting the owner

1. **T-075** — the Part 11 curl was built from the owner's sentence; the attached picture never
   reached this session. Ask them to re-send it (or confirm the head-to-tail reading).
2. **T-076** — should a nearly vertical swipe turn the page? Today `DRAG_AXIS_FRACTION = 0.35`
   says no; it is a one-line change if they want it.
3. **T-078** — should a size change made inside the customizer mark the theme card with `*`?
   Open since Part 10; the config does not record where a size came from, so it needs a rule.
4. **T-079** — Part 9's "ẩn luôn cái nút back": the dimmed back at the voice's level (built) or
   no back button in the reader at all (not built)?
5. Nothing else blocks work; `ffmpeg` is missing, so T-074 (film of the commit animation) waits.

## Exact next action

Ask for the Part 11 curl picture and the T-076 decision. If the answer is "as built", the
project is at a clean stopping point and the parked list (T-061…T-073, T-077) needs an explicit
go-ahead before anything starts.

## Resume prompt (paste to a new agent)

> Đọc `PROJECT_STATE.md`, `TASKS.md` và `SESSION_HANDOFF.md` ở gốc dự án
> `/Users/cuong/Workspace/CODE/AI/OpenCode/epub-reader-android`. Đối chiếu lại với
> `git status` và APK thật, không sửa code trước khi báo cáo khảo sát. Cây làm việc đang sạch:
> Part 8–11 + bộ tài liệu + toolkit đã nằm trong commit ngày 2026-10-21 trên `master`
> (chưa có remote). Không chạy `git reset`, `git checkout --` hay `git add -A` khi chưa hỏi.
> Việc tiếp theo: hỏi owner ảnh curl của Part 11 và quyết định T-076 (vuốt gần thẳng đứng).

## Environment (bites every time)

- Build: `JAVA_HOME=/Users/cuong/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
  ./gradlew :app:assembleDebug`; adb at `~/Library/Android/sdk/platform-tools/adb`; AVD
  `Pixel_9_Pro_XL`, 1344×2992, density 480, API 36.
- Screenshots: `adb shell 'timeout 15 screencap -p /sdcard/x.png'` + `adb pull`; held drags via
  `input motionevent DOWN/MOVE…/UP`, never within ~50 px of an edge, with a small first MOVE
  (≤100 px) and a ~0.4 s pause or the lean locks at 0.
- Captures: set `adb logcat -G 32M` first; the glyph/Skia atlas corrupts after long runs — fix
  with `adb emu kill` + relaunch, it is the emulator, not the app.
- Evidence: `/Users/cuong/.ao/data/artifacts/standalone-20/` (`report.md` §85–§90 and
  `screenshots-part11/` for the latest round).

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

### Part 11 — three reports (2026-10-21) — current
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
