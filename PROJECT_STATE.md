# Project state
Updated: 2026-10-21 13:55 +07 (Asia/Ho_Chi_Minh)

> English first, bản tiếng Việt ở nửa dưới của cùng tệp.

Goal / acceptance criteria:
- Native iOS demo EPUB reader (SwiftUI shell + UIKit page curl) for the Vietnamese sample
  book "Ngọn Hải Đăng Cuối Cùng" (13 pages at 19 pt on iPhone 17 Pro), the iOS twin of
  `epub-reader-android`. Vietnamese UI labels, no third-party dependencies.
- Acceptance criteria approved round by round in chat (2026-10-18 … 2026-10-20):
  1. Import an EPUB; library grid of generated covers; bundled sample seeded on first launch.
  2. Page turning with the system `UIPageViewController` page curl (`.pageCurl`,
     `isDoubleSided = true`, `spineLocation = .min`): fold pinned to the spine, follows the
     finger, back of the sheet painted with the theme's paper plus a faint mirrored
     print-through. A hand-drawn curl was rejected ("Curl thế này thì xấu lắm").
  3. Chrome: floating toolbar (back + contents left, search + bookmark right, running head
     centred), auto-hides in 4.5 s; tap zones left third = previous page, right third =
     next page, middle = toggle chrome; while narration plays the sides reveal chrome and
     the middle seeks the narration.
  4. Read-aloud: headphone button unfurls a player (book title, chapter, slower,
     play/pause, faster, stop, speed label, progress line flush with the bottom edge of
     the capsule). While the toolbar is up the player shortens itself (title, play/pause,
     stop) so the settings button keeps its corner.
  5. Settings: half-height sheet closed by dragging, no "Xong" button; theme grid with "*"
     only for themes changed in Tuỳ chỉnh (never for panel A/A size changes); customise
     screen with its own reset.
  6. Reading place persisted per book (chapter + character offset) and resumed on open;
     library shows the percentage read per book.
  7. Back button obeys the same show/hide rule as the other toolbar buttons: never alone,
     never removed.
  8. Behaviour verified on the simulator with injected touches/swipes (idb + `simctl`),
     not from screenshots alone.

Approved scope and gates:
- Owner approvals are conversational, one round at a time; no written ticket exists:
  - 2026-10-20: settings-panel contrast, player expands/collapses, player steps aside for
    the toolbar (later replaced, see below), swipe matrix fixes at first/last page.
  - 2026-10-20: back button must hide together with the other buttons, not be deleted;
    page that "turned itself back" must be fixed (commit c999ad0).
  - 2026-10-20: reading position saved and resumed; library shows % read (commit 458ae98).
  - 2026-10-21: adopt AI Development Toolkit — survey first, bilingual state files at the
    project root, keep `--cli all`, add a README, then land it as one commit (approved).
- Superseded decision: "no way out of the book while the voice reads" (commit caaec44) was
  withdrawn by the owner on 2026-10-20 ("ẩn cùng với nút khác chứ có bảo là xoá nó đi đâu")
  and replaced by "back hides and returns with the toolbar" (commit c999ad0).
- Not approved: publishing (no remote configured, nothing pushed), refactoring, architecture
  migration, iPad-specific layout, unit-test target, Android changes from this project.

Architecture decisions:
- Layers as they exist (`EpubReader/App`, `Data`, `Domain`, `Library`, `Reader`). Pragmatic
  Clean Architecture: `Domain` holds models and pure algorithms (`AttributedBuilder`,
  `Paginator`) and imports UIKit for `NSAttributedString` only; `Data` reads EPUB/ZIP;
  presentation is SwiftUI plus one `UIViewControllerRepresentable` for the pager. No
  repository protocols, no DI container: stores are `ObservableObject` singletons or
  `@StateObject` at the view that owns them (per adt-ios-clean-architecture: do not add
  layers that the project does not need).
- EPUB parsing: `EpubArchiveReader` reads `META-INF/container.xml` → OPF → spine, TOC from
  EPUB3 `nav` / EPUB2 `toc.ncx`; chapter XHTML → `NSAttributedString` via HTML import on
  the main thread, therefore split into `RawChapter` (bytes, read off-main) and `Chapter`
  (attributed, converted on main).
- Pagination: CoreText framesetter per chapter (`Paginator`), identical fonts and geometry
  to the page renderer (`ReaderPageViewController`), so a page is a string range that can be
  mapped back to chapter coordinates.
- Pictures: `EpubArchiveReader` resolves every `<img src>` of a chapter against the archive while
  the zip is open (case-folded lookup, `..`, `%20`, `?v=`; caps of 8 MB per picture and 24 MB per
  chapter) and hands the bytes to `RawChapter`. The HTML importer drops `<img>` — it has no base
  URL — so `ChapterImages` swaps each tag for an ASCII token before the import, and
  `AttributedBuilder` replaces the token afterwards with an `NSTextAttachment` cut to the column
  (never upscaled, at most 55 % of the column height) or, when the picture is missing or
  undecodable, with its alt text. A `CTRunDelegate` reports the picture's size to CoreText:
  without it the paginator measures the placeholder character and the text view then clips the
  picture. A change of column width re-typesets the book (`builtWidth` in `ReaderViewModel`).
- Curl model: a double-sided `UIPageViewController` walks *sides*, not pages (front N → back
  N → front N+1). `SheetBackViewController` paints the theme's paper with a 9 % mirrored
  snapshot of its front. Programmatic turns pass `[front, back]`; every landing calls
  `settle(on:)` to stand the book back on one side. `turning` / `waitingPage` defer pages
  requested mid-gesture; `turnID` stops an interrupted curl from settling on its older page;
  a rebind naming the page already on screen is ignored (commit c999ad0).
- Persistence: `UserDefaults` for reader settings (`reader*` keys), narration rate
  (`narrationRateFactor`), bookmarks and `readerPositions` (JSON); books live in
  `Documents/Books`; the bundled `sample.epub` is seeded once.
- Demo harness in-app: launch-argument hooks (`-demoOpenBook`, `-demoPage`, `-demoSpeak`,
  `-demoDark`, `-demoSettings`, `-demoDebug`, `-demoCurl`, `-demoDrag*`,
  `-demoRebindOnTurn`, …) drive deterministic states for simulator verification.
- Appearance is app-wide and reader-owned: `ReaderSettings.shared` is one object for the whole
  process, and the library reads it, so the shelf's paper, ink, accent and hairlines are the
  palette in force (Android does the same with its process-wide `ReaderSettings` →
  `ReaderTheme`). `preferredColorScheme` follows the palette's own dark state, because the
  reader's day/night is an explicit setting rather than the system appearance.
- Working rules: the project now carries AI Development Toolkit v1.1.0 (`AGENTS.md`,
  `.agents/skills`, `.opencode/commands`, `.ai-toolkit`); work is sequential in one agent,
  multi-step work needs an approved plan before code, commits stage explicit paths only.

Verified complete:
- T01–T10, T12 and T16 (see TASKS.md) are implemented and verified. Functional code is committed
  on `master` up to `e224196` ("Reader: show the pictures a book carries", 2026-10-21 12:42 +07),
  which carries T12; the toolkit/docs adoption is `742433d`.
- SUITE-003 (the library prints on the reader's palette) is implemented and verified in the working
  tree; its three source files are **not** committed yet (the owner's go-ahead is needed). Only the
  documentation correction was committed ("Docs: T12 is committed, not pending").

In progress / pending / blocked:
- In progress: none.
- Pending: T11 app icon, T13 chapter-opening page weight, T14 dark fold sheen decision, T15
  automated tests.
- Blocked: none.
- SUITE-009 (iOS paused-tap fixes, 2026-10-22) is implemented and verified on the simulator but
  still uncommitted: `Reader/ReaderPage` (hit test that tells paper from a character),
  `Reader/ReaderView` (paused routing, chapter offset passed through), `Reader/SpeechController`
  (a paused synthesizer is stopped so the new utterance is heard). Report and evidence:
  `build/qa/SUITE-009/REPORT.md` (`build/` is ignored).

Git branch / HEAD / dirty files:
- Branch `master`; HEAD `e224196` "Reader: show the pictures a book carries" (2026-10-21 12:42 +07)
  carries T12. Under it: the adoption commit "Toolkit: adopt the AI development workflow, and write
  the state in both languages" (`742433d`) with the toolkit set, `README.md` and these three state
  files, then the last functional commit before the pictures, `c999ad0` (2026-10-20 16:40 +07).
- Before the adoption commit the tracked tree was clean and the only untracked entries were the 42
  toolkit files installed by the owner (2026-10-20 23:58, `manage.py install --cli all`) plus
  these docs; `manage.py uninstall` reports 39 toolkit files intact and KEEPs the three
  modified state files.
- SUITE-003 (2026-10-21 13:55 +07) is in the working tree and not committed: `Library/LibraryView`
  (palette from `ReaderSettings.shared`, two capture hooks), `Reader/ReaderSettings` (the app-wide
  `shared` instance) and `Reader/ReaderView` (settings observed from that instance). Only the
  documentation is committed ("Docs: T12 is committed, not pending"); a code commit needs the
  owner's go-ahead.
- SUITE-009 (2026-10-22) is in the working tree and not committed, on top of the SUITE-003 files:
  `Reader/ReaderPage.swift`, `Reader/ReaderView.swift` (which also carries SUITE-003's
  `ReaderSettings.shared` hunk), `Reader/SpeechController.swift`. No commit, no push.
- No remote. Auxiliary worktrees exist under `~/orca/workspaces/EpubReader-iOS/{EpubReader,
  catshark, mooneye}` pinned at `62b44ce`; they are not part of this project's work.

Verification:
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`, 0 warnings (2026-10-21 10:53 +07, Xcode 27.1; the only
  `warning:` line in the log is Apple's `appintentsmetadataprocessor` notice about no
  `AppIntents.framework` dependency).
- Simulator checks on 2026-10-20 (UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  swipe matrix one page per gesture both ways and at the first/last page, with and without
  narration; mid-drag rebind hook `-demoRebindOnTurn` (old code flipped back, current code
  does not); player, back button and library % (20 % → 36 % → 100 %); resume after terminate
  (page 3 → reopen page 3; 19 pt page 3 → 24 pt same sentence).
- Pictures, 2026-10-21 (same simulator): a generated test book `zz-anh-thu.epub` (one chapter, a
  900×300, a 600×400 and a 300×1200 PNG, plus an `<img>` whose file is absent) drawn as: 2 columns
  at 19 pt → images at 160 pt wide, the tall one capped at 353 pt (55 % of the 642 pt column) and
  on its own page, alt text `[Ảnh: Ảnh thiếu tệp]` for the broken `src`; 1 column → the same
  pictures at 342 pt; dark paper → pictures unchanged, alt text legible; page count 3 (2 columns)
  and 4 (1 column); `-demoPage 4` → quit → reopen page 4; sample book unchanged at 13 pages.
  Pixel rows were measured, not eyeballed: the 88 pt picture sits centred in its 160 pt column
  (image 478–735 px, column centre 603 px). Two regressions found and fixed on the way: the
  paginator ignored the attachment size (pictures were laid into space that did not exist and
  clipped), and a second book opened in the same view model kept the first book's chapters.
- `python3 .ai-toolkit/scripts/checkpoint.py .` → `master`, clean tracked tree (2026-10-21).
- SUITE-003, 2026-10-21 (iPhone 17 Pro, UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`. The bare destination without `OS=26.5` now fails
  (exit 70: it resolves to `OS:latest` = 27.0, and no iPhone 17 Pro exists on that runtime).
  Installed on the booted simulator and photographed the library (1206×2622, screenshots outside
  the repo in `…/T/epub-ios/`):
  - live repaint without relaunch (`-demoLibraryTheme focus -demoLibraryDark 1`): shelf background
    #FFFFFF (Original light) before the change, #2A2114 (Focus dark paper) after;
  - Paper light: background #FBF7EE (2,016,748 px), progress track #EFE9DC (3,586 px), accent
    #9E5932 (3,529 px), soft ink #6E6459 (819 px);
  - Paper dark: background #0F0D0B (2,198,333 px), track #1E1B17 (3,583 px), accent #D9A06A
    (3,529 px), soft ink #9C9385 (819 px);
  - reader spot check after the change (`-demoOpenBook 1 -demoPage 3 -demoTheme paper
    -demoDark 1`): page paper #0F0D0B, reading behaviour unchanged;
  - system appearance flipped light→dark with the shelf on Paper light: background stays #FBF7EE,
    because appearance is the reader's own day/night setting (Android does the same); the system
    chrome is pinned to the palette with `preferredColorScheme`.
- SUITE-009, 2026-10-22 (iPhone 17 Pro, UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`. Before/after runs of a pre-fix build and the fixed
  build (report: `build/qa/SUITE-009/REPORT.md`):
  - paused + tap on a sentence: before — nothing happened (`state paused`, `spoken 710+79`
    unchanged); after — `speak chapter 2 from 789`, `state playing`, `spoken 789+229`;
  - paused + tap on the blank margin: before — the page turned back (`Trang 5/13` → `4/13`)
    and the chrome stayed `playerOnly`; after — `chrome full`, `state paused` unchanged, same
    page;
  - paused + tap on blank paper inside a column: before — logged as `chapter 1018` (nearest
    sentence selected); after — logged as `blank`, chrome `full`, nothing selected;
  - regressions: playing + text tap still seeks (`speak … from 789`), idle + middle tap still
    toggles the chrome, idle + right-third tap still turns the page (`Trang 5/13` → `6/13`).
  - The finger itself is simulated: this machine's Xcode 27.1 install has no `Simulator.app`
    and no `idb`/`applesimutils`/`cliclick`, so the taps are the app's own demo hook
    (`-demoTapX/-demoTapY`), which calls the same `emitTap` the `UITapGestureRecognizer`
    calls. Everything downstream of the recogniser is exercised for real.
  - Repeated the same matrix on the **device hub** with real touches (`serve-sim` HID →
    the simulator's digitizer; normalized 0..1). Xcode 27's Device Hub had disconnected the
    legacy touch service — `serve-sim repair-input -d <udid>` (restarts SpringBoard, closes
    the running apps) brings it back; after that the hub's touches land. On the same page
    (index 4 of 13, chapter 2): paused + real tap on a sentence → the baseline build logs
    the tap and does nothing (`state paused`); the fixed build logs `speak chapter 2 from
    789 … wasPaused true` and the overlay goes `state paused` → `state playing`. Paused +
    real tap on the blank paper → the baseline build reports it as `chapter 1018` (the last
    sentence) and the chrome stays `playerOnly`; the fixed build reports `blank`, the chrome
    becomes `full`, the state stays `paused` and the page does not change. Regressions with
    real touches: playing + text tap still seeks, idle + middle tap still reveals the
    chrome, idle + right-third tap still turns the page (`Trang 5/13` → `6/13`).
- No XCTest target exists (NOT RUN: unit/UI tests).

Known limitations:
- System light/dark does not repaint the library or the reader: appearance is the reader's explicit
  day/night setting, as on Android. The shelf keeps the reader's palette and pins the system chrome
  to it. Following the system needs an "automatic" appearance option — owner decision.
- The library's large navigation title and status bar are system chrome (label ink chosen by
  `preferredColorScheme`); the shelf's own text (percentages, empty state) uses the palette inks.
- Pictures are raster only (PNG/JPEG/GIF/WebP). SVG, `<object>`, `<image>` inside inline SVG and
  CSS background images are not drawn — an SVG shows its alt text.
- A block picture that does not fit the space left on a page starts the next page, so the foot of
  the previous page can be blank; pictures never split across pages.
- Picture bytes stay in memory while the book is open (8 MB per picture, 24 MB per chapter) and are
  decoded again when the column width changes; re-typesetting imports every chapter on the main
  thread, so a restyle of a large book pauses for a moment.
- No app icon: there is no asset catalog; only `Resources/sample.epub` ships.
- Chapter-opening pages render visibly heavier (page 2 bold vs page 3); cause not found.
- `ReaderView.swift` is 1309 lines and owns chrome, player, settings wiring and demo hooks;
  a split is a refactor that needs a separate approval.
- No automated test target; verification depends on the simulator, idb and screenshots.
- Progress percentage is measured at the foot of the visible page, so the first page of a
  13-page book reads ~8 % and the last page 100 %.
- Portrait only, iPhone and iPad (`TARGETED_DEVICE_FAMILY = 1,2`) with no iPad layout.
- Narration quality depends on the device's installed Vietnamese voices.

Exact next action:
- Ask the owner three things: approve committing SUITE-003's three source files (explicit paths, no
  push); whether the library should follow the system appearance or keep the reader's day/night
  (currently the latter, matching Android); and which of T11, T13–T15 comes next — then write that
  task's plan and wait for approval before touching code.

---

## Bản tiếng Việt

Cập nhật: 2026-10-21 13:55 +07 (Asia/Ho_Chi_Minh)

Mục tiêu / tiêu chí nghiệm thu:
- Ứng dụng đọc EPUB demo chạy native trên iOS (vỏ SwiftUI + lật trang bằng UIKit page curl)
  cho sách mẫu tiếng Việt "Ngọn Hải Đăng Cuối Cùng" (13 trang ở cỡ 19 pt trên iPhone 17 Pro),
  bản iOS song song với `epub-reader-android`. Giao diện tiếng Việt, không thư viện ngoài.
- Các tiêu chí được duyệt theo từng vòng trao đổi (2026-10-18 … 2026-10-20):
  1. Nhập được EPUB; thư viện dạng lưới với bìa tự sinh; sách mẫu được chép sẵn lần đầu mở app.
  2. Lật trang bằng page curl của hệ thống (`UIPageViewController`, `.pageCurl`,
     `isDoubleSided = true`, `spineLocation = .min`): nếp gấp bám gáy, theo ngón tay; mặt sau
     tờ giấy là màu giấy của theme kèm hình in xuyên qua rất mờ, lật ngược. Curl tự vẽ đã bị
     loại ("Curl thế này thì xấu lắm").
  3. Thanh công cụ nổi (back + mục lục bên trái, tìm kiếm + đánh dấu bên phải, tên chương ở
     giữa), tự ẩn sau 4,5 giây; vùng chạm: 1/3 trái = trang trước, 1/3 phải = trang sau, giữa =
     bật/tắt nút; khi đang đọc thành tiếng thì chạm hai bên để hiện nút, chạm giữa để nhảy tới
     câu tương ứng.
  4. Đọc thành tiếng: nút tai nghe mở player (tên sách, chương, chậm, phát/tạm dừng, nhanh,
     dừng, nhãn tốc độ, vạch tiến trình sát mép dưới của viên thuốc). Khi thanh công cụ đang
     hiện, player tự thu ngắn (tên sách, phát/tạm dừng, dừng) để nút cài đặt giữ nguyên góc.
  5. Cài đặt: bảng cao nửa màn hình, kéo xuống để đóng, không có nút "Xong"; lưới theme có dấu
     "*" chỉ cho theme đã sửa trong Tuỳ chỉnh (A/A ở bảng không đánh dấu); màn Tuỳ chỉnh có
     nút đặt lại riêng.
  6. Lưu vị trí đọc theo từng sách (chương + độ lệch ký tự), mở lại đúng chỗ; thư viện hiện
     phần trăm đã đọc của từng sách.
  7. Nút back tuân theo đúng quy tắc hiện/ẩn của các nút còn lại: không đứng một mình, không
     bị xoá.
  8. Hành vi được kiểm trên simulator bằng chạm/vuốt thật (idb + `simctl`), không chỉ ảnh chụp.

Phạm vi và các mốc duyệt:
- Các lần duyệt đều qua trao đổi trực tiếp, mỗi lần một vòng; không có ticket bằng văn bản:
  - 2026-10-20: độ tương phản bảng cài đặt, player mở/thu có animation, player nhường chỗ cho
    thanh công cụ (sau đó bị thay, xem dưới), sửa ma trận vuốt ở trang đầu/cuối.
  - 2026-10-20: nút back phải ẩn cùng các nút khác chứ không bị xoá; sửa lỗi trang "tự lật lại"
    (commit c999ad0).
  - 2026-10-20: lưu và khôi phục vị trí đọc; thư viện hiện % đã đọc (commit 458ae98).
  - 2026-10-21: áp dụng AI Development Toolkit — khảo sát trước, tài liệu state song ngữ ở root,
    giữ cài `--cli all`, thêm README, rồi gộp thành một commit (đã duyệt).
- Quyết định bị thay thế: "không có đường ra khỏi sách khi đang đọc thành tiếng" (commit
  caaec44) đã được anh rút lại ngày 2026-10-20 ("ẩn cùng với nút khác chứ có bảo là xoá nó đi
  đâu") và thay bằng "back ẩn/hiện cùng thanh công cụ" (commit c999ad0).
- Chưa được duyệt: publish (không có remote, chưa push lần nào), refactor, đổi kiến trúc, bố
  cục riêng cho iPad, target unit test, thay đổi repo Android từ dự án này.

Quyết định kiến trúc:
- Các tầng hiện có (`EpubReader/App`, `Data`, `Domain`, `Library`, `Reader`). Clean
  Architecture thực dụng: `Domain` giữ model và thuật toán thuần (`AttributedBuilder`,
  `Paginator`), chỉ import UIKit cho `NSAttributedString`; `Data` đọc EPUB/ZIP; phần trình bày
  là SwiftUI cộng một `UIViewControllerRepresentable` cho pager. Không có repository protocol,
  không có DI container: store là `ObservableObject` dạng singleton hoặc `@StateObject` tại
  view sở hữu (theo adt-ios-clean-architecture: không thêm tầng dự án không cần).
- Phân tích EPUB: `EpubArchiveReader` đọc `META-INF/container.xml` → OPF → spine; mục lục lấy từ
  `nav` (EPUB3) hoặc `toc.ncx` (EPUB2); XHTML của chương → `NSAttributedString` bằng HTML import
  trên main thread, vì vậy tách `RawChapter` (dữ liệu thô, đọc ngoài main) và `Chapter` (đã
  chuyển, làm trên main).
- Phân trang: CoreText framesetter cho từng chương (`Paginator`), dùng đúng font và hình học
  như bộ vẽ trang (`ReaderPageViewController`), nên mỗi trang là một range ký tự ánh xạ ngược
  được về toạ độ chương.
- Ảnh: `EpubArchiveReader` tra mọi `<img src>` của chương trong archive lúc còn mở zip (tra không
  phân biệt hoa/thường, xử lý `..`, `%20`, `?v=`; trần 8 MB mỗi ảnh và 24 MB mỗi chương) rồi đưa
  bytes vào `RawChapter`. HTML importer bỏ `<img>` — nó không có base URL — nên `ChapterImages`
  đổi mỗi thẻ thành một token ASCII trước khi import, và `AttributedBuilder` thay token đó bằng
  `NSTextAttachment` cắt theo cột (không phóng to, cao tối đa 55 % chiều cao cột), hoặc bằng alt
  text khi ảnh thiếu/không giải mã được. Một `CTRunDelegate` báo kích thước ảnh cho CoreText: nếu
  không có nó, bộ phân trang đo ký tự giữ chỗ rồi `UITextView` cắt mất ảnh. Đổi bề rộng cột thì
  dựng lại toàn bộ chữ (`builtWidth` trong `ReaderViewModel`).
- Mô hình curl: `UIPageViewController` hai mặt đi theo *side* chứ không theo trang (front N →
  back N → front N+1). `SheetBackViewController` vẽ giấy của theme kèm ảnh chụp mặt trước lật
  ngược, mờ 9 %. Lượt lật do chương trình gọi truyền `[front, back]`; mỗi lần hạ cánh đều gọi
  `settle(on:)` để đưa sách về một mặt. `turning` / `waitingPage` hoãn trang được yêu cầu giữa
  cú kéo; `turnID` không cho cú lật bị chen ngang hạ cánh về trang cũ; yêu cầu trùng trang đang
  hiện bị bỏ qua (commit c999ad0).
- Lưu trữ: `UserDefaults` cho cài đặt đọc (`reader*`), tốc độ đọc (`narrationRateFactor`), đánh
  dấu và `readerPositions` (JSON); sách nằm trong `Documents/Books`; `sample.epub` trong bundle
  được chép một lần.
- Bộ móc demo trong app: các cờ khi khởi chạy (`-demoOpenBook`, `-demoPage`, `-demoSpeak`,
  `-demoDark`, `-demoSettings`, `-demoDebug`, `-demoCurl`, `-demoDrag*`,
  `-demoRebindOnTurn`, …) tạo trạng thái xác định để kiểm trên simulator.
- Giao diện là toàn app và do reader sở hữu: `ReaderSettings.shared` là một đối tượng cho cả
  tiến trình, thư viện đọc chính đối tượng đó nên giấy, mực, accent và đường kẻ của kệ sách là
  palette đang dùng (Android cũng vậy: `ReaderSettings` cấp tiến trình → `ReaderTheme`).
  `preferredColorScheme` đi theo trạng thái tối của palette, vì ngày/đêm của reader là cài đặt
  riêng chứ không phải giao diện hệ thống.
- Quy tắc làm việc: dự án đã có AI Development Toolkit v1.1.0 (`AGENTS.md`, `.agents/skills`,
  `.opencode/commands`, `.ai-toolkit`); làm tuần tự trong một agent, việc nhiều bước phải duyệt
  kế hoạch trước khi sửa code, commit chỉ stage đường dẫn cụ thể.

Đã xong và đã kiểm:
- T01–T10, T12 và T16 (xem TASKS.md) đã làm và đã kiểm. Code chức năng đã commit trên `master`
  tới `e224196` ("Reader: show the pictures a book carries", 2026-10-21 12:42 +07), commit này
  chứa T12; commit áp dụng toolkit/tài liệu là `742433d`.
- SUITE-003 (thư viện in trên palette của reader) đã làm và đã kiểm trong cây làm việc; ba tệp
  nguồn của nó **chưa** commit (cần anh cho phép). Chỉ phần sửa tài liệu được commit
  ("Docs: T12 is committed, not pending").

Đang làm / chờ / bị chặn:
- Đang làm: không.
- Chờ: T11 icon app, T13 trang mở đầu chương bị đậm, T14 quyết định nếp gấp ở theme tối,
  T15 test tự động.
- Bị chặn: không.
- SUITE-009 (sửa lỗi chạm khi giọng đọc đang tạm dừng, 2026-10-22) đã làm và đã kiểm trên
  simulator nhưng **chưa** commit: `Reader/ReaderPage` (hit test phân biệt giấy trắng với ký tự),
  `Reader/ReaderView` (định tuyến khi tạm dừng, offset theo chương), `Reader/SpeechController`
  (dừng cả synthesizer đang tạm dừng để câu mới được đọc). Báo cáo và bằng chứng:
  `build/qa/SUITE-009/REPORT.md` (`build/` bị ignore).

Nhánh Git / HEAD / tệp chưa commit:
- Nhánh `master`; HEAD `e224196` "Reader: show the pictures a book carries" (2026-10-21 12:42 +07)
  chứa T12. Bên dưới là commit áp dụng toolkit "Toolkit: adopt the AI development workflow, and
  write the state in both languages" (`742433d`) với bộ toolkit, `README.md` và ba tệp state này,
  rồi commit chức năng trước đó `c999ad0` (2026-10-20 16:40 +07).
- Trước commit áp dụng, cây tracked sạch; các mục untracked chỉ gồm 42 tệp toolkit do anh cài
  (2026-10-20 23:58, `manage.py install --cli all`) và ba tệp tài liệu này; `manage.py uninstall`
  báo 39 tệp toolkit còn nguyên hash và giữ lại ba tệp state đã sửa.
- SUITE-003 (2026-10-21 13:55 +07) đang ở trong cây làm việc, chưa commit:
  `Library/LibraryView` (lấy palette từ `ReaderSettings.shared`, thêm hai móc chụp ảnh),
  `Reader/ReaderSettings` (thể hiện `shared` dùng chung toàn app) và `Reader/ReaderView` (theo dõi
  cài đặt từ thể hiện đó). Chỉ tài liệu được commit ("Docs: T12 is committed, not pending");
  commit code cần anh cho phép.
- SUITE-009 (2026-10-22) đang ở trong cây làm việc, chưa commit, chồng lên các tệp của SUITE-003:
  `Reader/ReaderPage.swift`, `Reader/ReaderView.swift` (tệp này mang thêm hunk
  `ReaderSettings.shared` của SUITE-003), `Reader/SpeechController.swift`. Không commit, không push.
- Không có remote. Có các worktree phụ ở `~/orca/workspaces/EpubReader-iOS/{EpubReader,
  catshark, mooneye}` neo tại `62b44ce`; không thuộc phạm vi dự án này.

Kiểm chứng:
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`, 0 warning (2026-10-21 10:53 +07, Xcode 27.1; dòng
  `warning:` duy nhất trong log là thông báo của `appintentsmetadataprocessor` về việc không phụ
  thuộc `AppIntents.framework`).
- Kiểm trên simulator ngày 2026-10-20 (UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  ma trận vuốt một trang mỗi cú, cả hai chiều và ở trang đầu/cuối, có và không có giọng đọc;
  móc `-demoRebindOnTurn` (bản cũ lật ngược lại, bản hiện tại không); player, nút back và % thư
  viện (20 % → 36 % → 100 %); mở lại sau khi thoát (trang 3 → mở trang 3; 19 pt trang 3 → 24 pt
  đúng câu đó).
- `python3 .ai-toolkit/scripts/checkpoint.py .` → `master`, cây tracked sạch (2026-10-21).
- SUITE-003, 2026-10-21 (iPhone 17 Pro, UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`. Lệnh không kèm `OS=26.5` giờ fail (exit 70: nó hiểu
  là `OS:latest` = 27.0, mà runtime đó không có iPhone 17 Pro). Cài lên simulator đang chạy và
  chụp thư viện (1206×2622, ảnh lưu ngoài repo ở `…/T/epub-ios/`):
  - đổi palette ngay trong một phiên, không khởi động lại (`-demoLibraryTheme focus
    -demoLibraryDark 1`): nền kệ #FFFFFF (Original sáng) trước, #2A2114 (giấy Focus tối) sau;
  - Paper sáng: nền #FBF7EE (2.016.748 px), vạch tiến trình #EFE9DC (3.586 px), accent #9E5932
    (3.529 px), mực mờ #6E6459 (819 px);
  - Paper tối: nền #0F0D0B (2.198.333 px), vạch #1E1B17 (3.583 px), accent #D9A06A (3.529 px),
    mực mờ #9C9385 (819 px);
  - kiểm lại reader sau thay đổi (`-demoOpenBook 1 -demoPage 3 -demoTheme paper -demoDark 1`):
    giấy trang #0F0D0B, hành vi đọc không đổi;
  - đổi giao diện hệ thống sáng→tối khi kệ đang ở Paper sáng: nền vẫn #FBF7EE, vì ngày/đêm là
    cài đặt của reader (Android cũng vậy); phần chrome hệ thống bám theo palette nhờ
    `preferredColorScheme`.
- SUITE-009, 2026-10-22 (iPhone 17 Pro, UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`. Chạy bản cũ (trước sửa) và bản mới rồi so ảnh + log
  (báo cáo: `build/qa/SUITE-009/REPORT.md`):
  - đang tạm dừng + chạm vào câu: trước — không có gì xảy ra (`state paused`, `spoken 710+79`
    không đổi); sau — `speak chapter 2 from 789`, `state playing`, `spoken 789+229`;
  - đang tạm dừng + chạm vào lề trắng: trước — trang lật ngược (`Trang 5/13` → `4/13`) và chrome
    vẫn `playerOnly`; sau — `chrome full`, `state paused` không đổi, vẫn trang cũ;
  - đang tạm dừng + chạm vào khoảng trắng trong cột: trước — log ra `chapter 1018` (chọn nhầm câu
    gần nhất); sau — log ra `blank`, chrome `full`, không chọn câu nào;
  - hồi quy: đang đọc + chạm câu vẫn nhảy đúng (`speak … from 789`); rảnh + chạm giữa trang vẫn
    bật/tắt thanh công cụ; rảnh + chạm 1/3 phải vẫn lật trang (`Trang 5/13` → `6/13`).
  - Ngón tay là giả lập ở lượt đầu: bản cài Xcode 27.1 trên máy này không có `Simulator.app`, cũng
    không có `idb`/`applesimutils`/`cliclick`, nên chạm được đưa vào bằng móc demo của chính app
    (`-demoTapX/-demoTapY`), gọi đúng hàm `emitTap` mà `UITapGestureRecognizer` gọi. Mọi thứ
    sau gesture recogniser được kiểm thật.
  - Lặp lại toàn bộ bảng kiểm trên **device hub với chạm thật** (serve-sim HID → digitizer của
    simulator; tọa độ chuẩn hoá 0..1). Device Hub của Xcode 27 đã ngắt dịch vụ touch cũ, sửa
    bằng `serve-sim repair-input -d <udid>` (khởi động lại SpringBoard, đóng app đang chạy) rồi
    khởi động lại serve-sim và mở lại app. Cùng trang (index 4/13, chương 2): tạm dừng + chạm thật
    vào câu → bản trước sửa log `tap 200.0,230.0 column 737 local 107 chapter 844` mà không có
    dòng `speak` (`state paused`); bản sau sửa log `speak chapter 2 from 789 … wasPaused true` và
    overlay đi từ `state paused` sang `state playing`. Tạm dừng + chạm thật vào giấy trắng → bản
    trước sửa log `tap 200.0,500.0 column 737 local 281 chapter 1018` (coi giấy trắng là câu cuối
    cùng) và chrome kẹt ở `playerOnly`; bản sau sửa log `tap 200.0,500.0 blank`, chrome thành
    `full`, trạng thái vẫn `paused`, trang không đổi. Hồi quy với chạm thật: đang đọc + chạm câu
    vẫn nhảy, rảnh + chạm giữa vẫn hiện thanh công cụ, rảnh + chạm 1/3 phải vẫn lật trang
    (`Trang 5/13` → `6/13`).
- Dự án không có target XCTest (CHƯA CHẠY: test unit/UI).

Hạn chế đã biết:
- Giao diện sáng/tối của hệ thống không làm đổi màu thư viện hay reader: ngày/đêm là cài đặt riêng
  của reader, giống Android. Kệ giữ palette của reader và ghim chrome hệ thống theo palette. Muốn
  theo hệ thống thì cần thêm lựa chọn "Tự động" — chờ anh quyết.
- Tiêu đề lớn của thư viện và thanh trạng thái là chrome hệ thống (mực do `preferredColorScheme`
  chọn); chữ do thư viện tự vẽ (phần trăm, màn trống) dùng mực của palette.
- Ảnh chỉ hỗ trợ dạng raster (PNG/JPEG/GIF/WebP). SVG, `<object>`, `<image>` trong SVG nội tuyến và
  ảnh nền CSS không được vẽ — SVG sẽ hiện alt text.
- Ảnh dạng khối không vừa chỗ còn lại thì sang trang sau, nên chân trang trước có thể trống; ảnh
  không bao giờ bị cắt đôi giữa hai trang.
- Bytes của ảnh nằm trong bộ nhớ suốt thời gian mở sách (8 MB mỗi ảnh, 24 MB mỗi chương) và được
  giải mã lại khi bề rộng cột đổi; mỗi lần dựng lại phải import toàn bộ chương trên main thread nên
  đổi cỡ chữ/lề/cột ở sách lớn sẽ khựng một nhịp.
- Chưa có icon app: không có asset catalog; chỉ có `Resources/sample.epub`.
- Trang mở đầu chương hiển thị đậm hơn rõ rệt (trang 2 đậm hơn trang 3); chưa tìm ra nguyên nhân.
- `ReaderView.swift` dài 1.309 dòng, ôm cả thanh công cụ, player, cài đặt và móc demo; tách nhỏ
  là refactor, cần duyệt riêng.
- Không có target test tự động; kiểm chứng phụ thuộc simulator, idb và ảnh chụp.
- Phần trăm đọc tính ở chân trang đang hiện, nên trang đầu của sách 13 trang khoảng 8 %, trang
  cuối là 100 %.
- Chỉ dọc, hỗ trợ iPhone và iPad (`TARGETED_DEVICE_FAMILY = 1,2`) nhưng chưa có bố cục iPad.
- Chất lượng giọng đọc phụ thuộc giọng tiếng Việt cài trên máy.

Hành động tiếp theo chính xác:
- Hỏi anh ba việc: có commit ba tệp nguồn của SUITE-003 không (stage đúng đường dẫn, không push);
  thư viện nên theo giao diện hệ thống hay giữ ngày/đêm của reader (hiện giữ, giống Android); và
  việc nào trong T11, T13–T15 làm tiếp; sau đó viết kế hoạch cho việc đó và chờ duyệt trước khi
  sửa code.
