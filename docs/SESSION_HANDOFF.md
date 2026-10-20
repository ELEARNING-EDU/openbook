# Session Handoff

Updated: 2026-10-20 (+07)

## Resume entry point
- Exact next action: nothing pending. Seven rounds of work are finished and verified —
  engine/read-aloud, the chrome pass, the appearance pass, the iOS-parity pass, the player
  pass, the diagonal curl, and now the spine-hinged curl.
- Verification record: the shipped APK (`12,290,159 bytes`, 2026-10-20 13:05,
  sha256 `e00f886e…4775`) is installed on `emulator-5554` and was last seen taking real
  drags: an 18% pull falling back onto page 6, a 60% pull turning to page 7, and a 60% pull
  the other way returning to page 6, with the fold hinged on the gutter corner in light,
  dark and cream. Its states are in
  `/Users/cuong/.ao/data/artifacts/standalone-20/screenshots-spine/` with `logs.txt` and
  `spine.sh`.
- If more is wanted, the natural next steps are: the five small search/bookmark gaps listed
  at the end of `PROJECT_STATE.md` (placeholder wording, snippet lines, "tr. N", swipe to
  delete, sheet vs full screen); a check at tablet width (where `Cột — Tự động` would
  resolve to 2, and where the customizer preview's margin would read closer to the page's
  own proportion); a two-column page that carries two chapters (the only case where the
  highlight band's chapter comparison is load-bearing; the sample book's chapters are too
  long to stage it); a long chapter title to exercise the running head's ellipsis; `<img>`
  support (non-null `ImageGetter`); the book's own CSS; word-anchored bookmarks; or a real
  audio-output check on hardware.

## The spine-hinged curl (Part 7, 2026-10-20) — the fold now hangs off the gutter
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

## The curl pass (Part 6, 2026-10-20) — superseded by Part 7, kept as history
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

## Environment (unchanged, but it bites every time)
- Build: `cd /Users/cuong/Workspace/CODE/AI/OpenCode/epub-reader-android &&
  JAVA_HOME=/Users/cuong/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
  ./gradlew :app:assembleDebug`. adb: `$HOME/Library/Android/sdk/platform-tools/adb`.
- AVD `Pixel_9_Pro_XL`, API 36, 1344×2992, density 3.0, ~5.7s cold start. Screenshot with
  `adb shell 'timeout 15 screencap -p /sdcard/x.png'` + `adb pull`; `exec-out` stalls.
- `pm clear` + a fresh `am start -S` per capture; demo config is cumulative in prefs.
- The auto-hide is 4.5s and tool latency can exceed it: tap and screencap must be one shell
  command, and the reliable handshake is polling `adb logcat -d -s ReaderScreen:I` for the
  line the app prints at the moment you care about (`typeset …`, `curl fold held …`).
- `input swipe` delivers its last movement at 92–96% of the nominal translation, so a
  logged `sweep` reads a little short of `hypot`. For a *held* drag use `input motionevent
  DOWN x y` / `MOVE x y` … / `UP x y` instead: it delivers the coordinates exactly, and the
  finger can stay down while `screencap` runs (this is how `06`–`12` were taken). Its
  events carry no velocity, so a drag injected that way always logs `vx=0` — use one
  `input swipe … 90` for the fling arm. The scripted pull (`demoDrag*`) delivers `t = 1.0`
  and is exact, and logs `vx = -130…-88`.
- `--ei demoPage N` lands a few seconds *after* the first `typeset` line (the book is
  re-seeded on `pm clear`), so waiting only for `typeset` can photograph the wrong page.
  Wait ~8 s, and read the footer in the shot to confirm.
- The emulator can wedge: a pointer left down by `input motionevent` without an `UP` once
  left `com.android.systemui` ANR'ing with the splash screen stuck on top of a live app
  (the app logs normally behind it). If screenshots look transition-y or scaled, `adb emu
  kill` and relaunch the AVD; a mere `adb reboot` did not clear it.
- `am start --es key "a b"` splits on the space through `adb shell`; wrap the whole command
  in single quotes with inner double quotes.
- No git repository: everything is uncommitted plain files; nothing is published.
