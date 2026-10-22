# Session handoff
> English first, bản tiếng Việt ở nửa dưới của cùng tệp.

Timestamp / timezone: 2026-10-22 01:15 +07 (Asia/Ho_Chi_Minh)

Read first: PROJECT_STATE.md, TASKS.md, `git status --short --branch`, `git log -3 --oneline`.

Verified files and checks:
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **` (Xcode 27.1, 2026-10-21 10:53 +07). The only `warning:`
  line in the log is Apple's `appintentsmetadataprocessor` notice; no warning comes from the
  project's own code.
- `python3 .ai-toolkit/scripts/checkpoint.py .` → branch `master`, HEAD `742433d` (2026-10-21).
- T12 is committed as `e224196` "Reader: show the pictures a book carries" (2026-10-21 12:42 +07);
  the state files no longer describe it as pending.
- SUITE-003 (library on the reader's palette), 2026-10-21: build with
  `-destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5'` → exit 0. The same command
  without `OS=26.5` now fails with exit 70 (`OS:latest` is 27.0 and has no iPhone 17 Pro), so always
  name the runtime. Installed on the booted iPhone 17 Pro (UDID 6B98AE70…, iOS 26.5) and
  photographed the library at 1206×2622 (files outside the repo in `…/T/epub-ios/`):
  - live repaint in one session (`-demoLibraryTheme focus -demoLibraryDark 1`): shelf background
    #FFFFFF (Original light) → #2A2114 (Focus dark paper), no relaunch;
  - Paper light: background #FBF7EE (2,016,748 px), progress track #EFE9DC (3,586 px), accent
    #9E5932 (3,529 px), soft ink #6E6459 (819 px);
  - Paper dark: background #0F0D0B (2,198,333 px), track #1E1B17 (3,583 px), accent #D9A06A
    (3,529 px), soft ink #9C9385 (819 px);
  - reader spot check (`-demoOpenBook 1 -demoPage 3 -demoTheme paper -demoDark 1`): page paper
    #0F0D0B, reading behaviour unchanged;
  - system appearance flipped light→dark while the shelf wore Paper light: background stays
    #FBF7EE — appearance is the reader's own day/night setting, as on Android; system chrome is
    pinned with `preferredColorScheme`.
- T12 (pictures), simulator UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5, generated test book
  `zz-anh-thu.epub` with a 900×300, a 600×400, a 300×1200 PNG and one `<img>` whose file is absent:
  - 2 columns, 19 pt → the wide and the inline pictures at 160 pt (column width), the tall one at
    88×353 pt (capped at 55 % of the 642 pt column), each on the page its token belongs to, nothing
    clipped; 3 pages.
  - 1 column (`-demoColumns one`) → the same pictures at 342 pt; 4 pages.
  - Dark paper → pictures unchanged, alt text legible; `[Ảnh: Ảnh thiếu tệp]` for the broken `src`.
  - Resume: `-demoPage 4` → terminate → reopen → page 4.
  - Sample book still 13 pages, text and curl unchanged.
  - Alignment measured on pixel rows (image 478–735 px inside a 160 pt column whose centre is
    603 px), not eyeballed.
- Two defects were found by these runs and fixed in the same change: CoreText ignored the
  attachment's size, so the paginator placed a 353 pt picture in the few points left at the foot of
  a column and the text view clipped it (fixed with a `CTRunDelegate`); and a second URL loaded into
  the same `ReaderViewModel` kept the previous book's typeset chapters (fixed by resetting the
  typeset state in `load`).
- SUITE-009 (iOS paused audio taps), 2026-10-22: `xcodebuild -project EpubReader.xcodeproj -scheme
  EpubReader -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5'
  -derivedDataPath build/dd build` → exit 0, `** BUILD SUCCEEDED **`. A pre-fix build and the
  fixed build were run alternately on the booted iPhone 17 Pro (UDID 6B98AE70…, iOS 26.5); report
  and evidence in `build/qa/SUITE-009/` (`REPORT.md`, `fix.diff`, `run.sh`, `logs/`, `screenshots/`):
  - paused + tap a sentence: before — nothing happened (`state paused`, `spoken 710+79`); after —
    `speak chapter 2 from 789`, `state playing`, `spoken 789+229`;
  - paused + tap the blank margin: before — the page turned back (`Trang 5/13` → `4/13`), chrome
    stayed `playerOnly`; after — `chrome full`, `state paused`, same page;
  - paused + tap blank paper inside a column: before — logged `chapter 1018` (nearest sentence);
    after — logged `blank`, chrome `full`;
  - regressions: playing + text tap still seeks, idle + middle tap still toggles the chrome,
    idle + right-third tap still turns the page (`Trang 5/13` → `6/13`).
  - Input fidelity: the first pass used the app's own demo hook (`-demoTapX/-demoTapY`) — the
    same `emitTap` the `UITapGestureRecognizer` calls — because this machine's Xcode 27.1 install
    has no `Simulator.app` and no `idb`/`applesimutils`/`cliclick`. The whole matrix was then
    repeated on the **device hub with real touches** (serve-sim HID → the simulator's digitizer,
    normalized 0..1); Xcode 27's Device Hub had dropped the legacy touch service, fixed with
    `serve-sim repair-input -d <udid>` (restarts SpringBoard, closes the running apps) followed
    by a serve-sim restart and reopening the app:
    - paused + real tap on a sentence: before — `tap 200.0,230.0 column 737 local 107 chapter
      844` with no `speak` line, `state paused`; after — `speak chapter 2 from 789 … wasPaused
      true`, `state playing`, `spoken 789+229`, same page;
    - paused + real tap on the blank paper: before — `tap 200.0,500.0 column 737 local 281
      chapter 1018` (the blank paper taken as the last sentence) and chrome stuck at `playerOnly`;
      after — `tap 200.0,500.0 blank`, `chrome full`, `state paused` unchanged, same page;
    - regressions with real touches: playing + text tap still seeks (`speak chapter 2 from 710`),
      idle + blank tap reveals the chrome without turning the page, idle + right-third tap still
      turns the page (`Trang 5 / 13` → `6 / 13`).
    - Runner: `build/qa/SUITE-009/run-hub.sh`; report §9.
- Not re-verified today: swipe matrix, narration, library percentages (unchanged code paths, and a
  spot check of narration on the picture book started and played without error). No XCTest target
  exists (NOT RUN: unit/UI tests).

Work landed / ownership:
- T12 is implemented and verified but **not committed**: the working tree carries the new
  `EpubReader/Domain/ChapterImages.swift`, changes in `Domain/AttributedBuilder`,
  `Data/EpubArchiveReader`, `Domain/Models`, `Reader/ReaderViewModel`, `Reader/ReaderView` (new
  `-demoColumns` demo hook) and the four state/README files. Committing needs the owner's go-ahead;
  nothing is pushed (no remote).
- The previous commit `742433d` holds the toolkit adoption and this file set; `build/` stays ignored.

Blockers and pending approvals:
- Commit of T12 — waiting for the owner.
- Product decisions outstanding: T11 app icon (needs a 1024×1024 asset), T13 chapter-opening weight,
  T14 fold sheen on dark themes; T15 (tests) is scoped but not started. Each needs its own approved
  plan before code.

Exact next action:
- SUITE-009 is done and verified; its three source files are still uncommitted, so the next
  owner question is whether to commit them (explicit paths, no push) together with, or apart
  from, SUITE-003's three files. Then: which of T11, T13–T15 comes next — write that task's
  plan and wait for approval before touching code.

Resume prompt: Đọc state và đối chiếu files/Git; tiếp tục tác vụ chưa xong trong scope đã duyệt, không discard thay đổi.

---

## Bản tiếng Việt

Thời điểm / múi giờ: 2026-10-22 01:15 +07 (Asia/Ho_Chi_Minh)

Đọc trước: PROJECT_STATE.md, TASKS.md, `git status --short --branch`, `git log -3 --oneline`.

Tệp và bài kiểm đã chạy:
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **` (Xcode 27.1, 2026-10-21 10:53 +07). Dòng `warning:` duy
  nhất trong log là thông báo của `appintentsmetadataprocessor`; không có cảnh báo nào từ code dự án.
- `python3 .ai-toolkit/scripts/checkpoint.py .` → nhánh `master`, HEAD `742433d` (2026-10-21).
- T12 (ảnh), simulator UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5, sách thử sinh ra
  `zz-anh-thu.epub` gồm PNG 900×300, 600×400, 300×1200 và một `<img>` trỏ tệp không có:
  - 2 cột, 19 pt → ảnh ngang và ảnh nội tuyến rộng đúng 160 pt (bề rộng cột), ảnh cao 88×353 pt
    (bị chặn ở 55 % của cột 642 pt), mỗi ảnh nằm đúng trang của token, không bị cắt; 3 trang.
  - 1 cột (`-demoColumns one`) → cùng các ảnh ở 342 pt; 4 trang.
  - Nền tối → ảnh giữ nguyên, alt text đọc được; `[Ảnh: Ảnh thiếu tệp]` cho `src` hỏng.
  - Mở lại: `-demoPage 4` → thoát → mở lại → trang 4.
  - Sách mẫu vẫn 13 trang, chữ và cú lật không đổi.
  - Canh lề được đo trên hàng pixel (ảnh 478–735 px trong cột 160 pt có tâm 603 px), không đoán
    bằng mắt.
- Hai lỗi do chính các lần kiểm này phát hiện và đã sửa trong cùng thay đổi: CoreText không đọc
  kích thước attachment nên bộ phân trang xếp ảnh cao 353 pt vào vài point còn lại ở chân cột rồi
  `UITextView` cắt mất (sửa bằng `CTRunDelegate`); và khi một `ReaderViewModel` nhận URL thứ hai,
  nó giữ lại chương đã dựng của sách trước (sửa bằng cách đặt lại trạng thái dựng chữ trong `load`).
- SUITE-009 (chạm khi giọng đọc đang tạm dừng), 2026-10-22: `xcodebuild -project EpubReader.xcodeproj
  -scheme EpubReader -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro,
  OS=26.5' -derivedDataPath build/dd build` → exit 0, `** BUILD SUCCEEDED **`. Chạy xen kẽ bản
  trước sửa và bản sau sửa trên iPhone 17 Pro đang chạy (UDID 6B98AE70…, iOS 26.5); báo cáo và bằng
  chứng ở `build/qa/SUITE-009/` (`REPORT.md`, `fix.diff`, `run.sh`, `logs/`, `screenshots/`):
  - tạm dừng + chạm một câu: trước — không có gì xảy ra (`state paused`, `spoken 710+79`); sau —
    `speak chapter 2 from 789`, `state playing`, `spoken 789+229`;
  - tạm dừng + chạm lề trắng: trước — trang lật ngược (`Trang 5/13` → `4/13`), chrome vẫn
    `playerOnly`; sau — `chrome full`, `state paused`, trang không đổi;
  - tạm dừng + chạm giấy trắng trong cột: trước — log `chapter 1018` (chọn nhầm câu gần nhất);
    sau — log `blank`, chrome `full`;
  - hồi quy: đang đọc + chạm câu vẫn nhảy đúng, rảnh + chạm giữa vẫn bật/tắt thanh công cụ,
    rảnh + chạm 1/3 phải vẫn lật trang (`Trang 5/13` → `6/13`).
  - Độ trung thực của thao tác chạm: lượt đầu dùng móc demo của chính app (`-demoTapX/-demoTapY`)
    — đúng hàm `emitTap` mà `UITapGestureRecognizer` gọi — vì bản cài Xcode 27.1 trên máy này
    không có `Simulator.app`, cũng không có `idb`/`applesimutils`/`cliclick`. Sau đó lặp lại toàn
    bộ bảng kiểm trên **device hub với chạm thật** (serve-sim HID → digitizer của simulator,
    tọa độ chuẩn hoá 0..1); Device Hub của Xcode 27 đã ngắt dịch vụ touch cũ, sửa bằng
    `serve-sim repair-input -d <udid>` (khởi động lại SpringBoard, đóng app đang chạy) rồi
    khởi động lại serve-sim và mở lại app:
    - tạm dừng + chạm thật vào một câu: trước — `tap 200.0,230.0 column 737 local 107 chapter
      844` mà không có dòng `speak`, `state paused`; sau — `speak chapter 2 from 789 … wasPaused
      true`, `state playing`, `spoken 789+229`, trang không đổi;
    - tạm dừng + chạm thật vào giấy trắng: trước — `tap 200.0,500.0 column 737 local 281
      chapter 1018` (giấy trắng bị coi như câu cuối cùng) và chrome kẹt ở `playerOnly`; sau —
      `tap 200.0,500.0 blank`, `chrome full`, `state paused` giữ nguyên, trang không đổi;
    - hồi quy với chạm thật: đang đọc + chạm câu vẫn nhảy (`speak chapter 2 from 710`), rảnh +
      chạm giấy trắng vẫn hiện thanh công cụ mà không lật trang, rảnh + chạm 1/3 phải vẫn lật
      trang (`Trang 5 / 13` → `6 / 13`).
    - Kịch bản dùng lại: `build/qa/SUITE-009/run-hub.sh`; báo cáo §9.
- Chưa kiểm lại hôm nay: ma trận vuốt, đọc thành tiếng, phần trăm thư viện (đường code không đổi;
  có chạy thử giọng đọc trên sách có ảnh, chạy được, không lỗi). Dự án không có target XCTest
  (CHƯA CHẠY: test unit/UI).

Việc đã bàn giao / quyền sở hữu:
- T12 đã làm và đã kiểm nhưng **chưa commit**: cây làm việc có tệp mới
  `EpubReader/Domain/ChapterImages.swift`, sửa `Domain/AttributedBuilder`, `Data/EpubArchiveReader`,
  `Domain/Models`, `Reader/ReaderViewModel`, `Reader/ReaderView` (thêm móc demo `-demoColumns`) và
  bốn tệp state/README. Muốn commit cần anh cho phép; chưa push (không có remote).
- Commit trước `742433d` chứa bộ toolkit và chính bộ tài liệu này; `build/` vẫn bị ignore.

Chỗ tắc và việc chờ duyệt:
- Chờ anh duyệt commit T12.
- Còn chờ anh quyết: T11 icon app (cần asset 1024×1024), T13 độ đậm trang mở đầu chương, T14 nếp
  gấp ở theme tối; T15 (test) đã có phạm vi nhưng chưa làm. Mỗi việc cần kế hoạch được duyệt trước
  khi sửa code.

Hành động tiếp theo chính xác:
- SUITE-009 đã làm xong và đã kiểm; ba tệp nguồn của nó vẫn chưa commit, nên câu hỏi tiếp theo
  cho anh là có commit chúng không (stage đúng đường dẫn, không push), cùng với hay tách riêng ba
  tệp của SUITE-003. Sau đó: việc nào trong T11, T13–T15 làm tiếp — viết kế hoạch cho việc đó và
  chờ duyệt trước khi sửa code.

Prompt resume: Đọc state và đối chiếu files/Git; tiếp tục tác vụ chưa xong trong scope đã duyệt, không discard thay đổi.
