# EpubReader (iOS)

Native demo EPUB reader for **Ngọn Hải Đăng Cuối Cùng**: SwiftUI shell, a UIKit
`UIPageViewController` page curl, a Vietnamese read-aloud player, themes and per-book reading
position. The iOS twin of the Android demo `epub-reader-android`. No third-party dependencies.

> English first, bản tiếng Việt ở nửa dưới của cùng tệp.

## Requirements

- macOS with Xcode 27.1 (the version this was last built and verified with).
- iOS 18.0 deployment target; Swift 5 language mode.
- A simulator, e.g. `iPhone 17 Pro` on runtime iOS 26.5 (the device used for verification).

## Build and run

```sh
xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' \
  -derivedDataPath build/dd build

xcrun simctl boot "iPhone 17 Pro" ; open -a Simulator
xcrun simctl install booted build/dd/Build/Products/Debug-iphonesimulator/EpubReader.app
xcrun simctl launch booted com.example.epubreader.ios
```

Launch arguments are passed as `-key value` pairs and become `UserDefaults` values for that run,
which is how the reader is driven for verification:

```sh
xcrun simctl launch booted com.example.epubreader.ios -demoOpenBook 1 -demoPage 4 -demoDebug 1
```

| Argument | Effect |
| --- | --- |
| `-demoOpenBook 1` | opens the first library book on launch |
| `-demoPage N` | jumps to page N (1-based) once the pages are ready |
| `-demoSpeak 1` / `-demoSpeakAfter S` | starts narration, optionally S seconds after the page appears |
| `-demoDark 0\|1`, `-demoAppearance …` | forces the paper/appearance |
| `-demoSettings 1` | opens the settings sheet |
| `-demoBodySize N`, `-demoResetTheme 1` | sets the type size, clears theme customisations |
| `-demoColumns auto\|one\|two` | forces the column setting (one column is twice as wide, which is what a picture is cut to) |
| `-demoCurl 0.45 [-demoCurlBack 1]` | holds a fold open at a 0.45 pull so the curl can be photographed (add `-demoCurlBack 1` for the backward fold) |
| `-demoDragX/-demoDragY/-demoDragFromX/-demoDragFromY/-demoDragSeconds/-demoDragHold` | drives a slow drag for mid-gesture screenshots |
| `-demoTapX/-demoTapY/-demoTapAfter` | taps a point in simulator points after S seconds |
| `-demoTurnEvery S -demoTurnCount N` | turns a page every S seconds, N times |
| `-demoNoAutoHide 1` | keeps the toolbar on screen (the auto-hide is 4.5 s) |
| `-demoRebindOnTurn 1` | regression hook: rebinds the pager mid-turn (reproduces the old "page turns itself back") |
| `-demoDebug 1` | prints `[curl]` state transitions |

## Verifying

There is no test target yet (T15 in `TASKS.md`). Behaviour is checked on the simulator with
injected touches — `xcrun simctl io booted screenshot` for stills (retake after an animation,
the first frame may tear) and `idb` for taps/swipes in **points** (402×874 on the iPhone 17 Pro).

## Layout

```
EpubReader/
  App/       EpubReaderApp.swift          app entry, library at launch
  Data/      EpubArchiveReader, ZipArchive  container.xml → OPF → spine, TOC
  Domain/    Models, AttributedBuilder, Paginator
  Library/   LibraryStore, LibraryView, VinylCover
  Reader/    ReaderView, PageCurlPager, ReaderPage, ReaderViewModel, ReaderTheme,
             ReaderSettings, ThemesSettingsSheet, CustomizeThemeSheet, SpeechController,
             ReadingProgress, BookmarkStore, SearchSheet, MarqueeText
  Resources/ sample.epub                  seeded into the library on first launch
```

Persistent state lives in `UserDefaults` (`reader*` settings, `narrationRateFactor`, bookmarks,
`readerPositions`) and in `Documents/Books` for the EPUB files.

## Project docs and working rules

- `PROJECT_STATE.md`, `TASKS.md`, `SESSION_HANDOFF.md` — state, task list and handoff, both
  English and Vietnamese in each file. Read them before changing anything.
- `AGENTS.md` and `.ai-toolkit/`, `.agents/skills/`, `.opencode/commands/` — AI Development
  Toolkit v1.1.0 (work sequentially, plan approval before multi-step code changes, stage explicit
  paths when committing).
- Known limitations (no app icon, EPUB images not rendered, chapter-opening page weight, no test
  target, …) are listed in `PROJECT_STATE.md`.

The repository is local only: no remote, nothing has been pushed.

---

## Bản tiếng Việt

Ứng dụng đọc EPUB demo chạy native trên iOS cho sách **Ngọn Hải Đăng Cuối Cùng**: vỏ SwiftUI,
lật trang bằng page curl của `UIPageViewController` (UIKit), player đọc thành tiếng tiếng Việt,
theme và vị trí đọc riêng cho từng sách. Bản iOS song song với demo Android `epub-reader-android`.
Không dùng thư viện ngoài.

### Yêu cầu

- macOS có Xcode 27.1 (phiên bản dùng để build và kiểm lần cuối).
- Deployment target iOS 18.0; Swift 5.
- Một simulator, ví dụ `iPhone 17 Pro` runtime iOS 26.5 (máy đã dùng để kiểm chứng).

### Build và chạy

```sh
xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' \
  -derivedDataPath build/dd build

xcrun simctl boot "iPhone 17 Pro" ; open -a Simulator
xcrun simctl install booted build/dd/Build/Products/Debug-iphonesimulator/EpubReader.app
xcrun simctl launch booted com.example.epubreader.ios
```

Tham số khởi chạy truyền theo cặp `-key value` và trở thành giá trị `UserDefaults` cho riêng lần
chạy đó — đây là cách trình đọc được điều khiển khi kiểm chứng:

```sh
xcrun simctl launch booted com.example.epubreader.ios -demoOpenBook 1 -demoPage 4 -demoDebug 1
```

| Tham số | Tác dụng |
| --- | --- |
| `-demoOpenBook 1` | mở sách đầu tiên trong thư viện ngay khi khởi chạy |
| `-demoPage N` | nhảy tới trang N (tính từ 1) khi các trang đã sẵn sàng |
| `-demoSpeak 1` / `-demoSpeakAfter S` | bắt đầu đọc thành tiếng, hoặc sau S giây kể từ khi trang hiện |
| `-demoDark 0\|1`, `-demoAppearance …` | ép màu giấy / chế độ sáng tối |
| `-demoSettings 1` | mở bảng cài đặt |
| `-demoBodySize N`, `-demoResetTheme 1` | đặt cỡ chữ, xoá phần tuỳ chỉnh theme |
| `-demoColumns auto\|one\|two` | ép chế độ cột (một cột rộng gấp đôi — đúng bề rộng mà ảnh bị cắt theo) |
| `-demoCurl 0.45 [-demoCurlBack 1]` | giữ nếp gấp mở ở mức kéo 0,45 để chụp ảnh cú lật (thêm `-demoCurlBack 1` cho chiều lùi) |
| `-demoDragX/-demoDragY/-demoDragFromX/-demoDragFromY/-demoDragSeconds/-demoDragHold` | kéo chậm để chụp ảnh giữa cú lật |
| `-demoTapX/-demoTapY/-demoTapAfter` | chạm vào một điểm (đơn vị point) sau S giây |
| `-demoTurnEvery S -demoTurnCount N` | lật trang mỗi S giây, N lần |
| `-demoNoAutoHide 1` | giữ thanh công cụ luôn hiện (bình thường tự ẩn sau 4,5 giây) |
| `-demoRebindOnTurn 1` | móc hồi quy: yêu cầu vẽ lại pager giữa cú lật (tái hiện lỗi cũ "trang tự lật lại") |
| `-demoDebug 1` | in các bước chuyển trạng thái `[curl]` |

### Cách kiểm chứng

Dự án chưa có test target (T15 trong `TASKS.md`). Hành vi được kiểm trên simulator bằng chạm thật —
`xcrun simctl io booted screenshot` để chụp (chụp lại sau khi animation xong, khung đầu có thể bị
rách), và `idb` để chạm/vuốt theo **point** (402×874 trên iPhone 17 Pro).

### Bố cục mã

```
EpubReader/
  App/       EpubReaderApp.swift          điểm vào app, mở thư viện
  Data/      EpubArchiveReader, ZipArchive  container.xml → OPF → spine, mục lục
  Domain/    Models, AttributedBuilder, Paginator
  Library/   LibraryStore, LibraryView, VinylCover
  Reader/    ReaderView, PageCurlPager, ReaderPage, ReaderViewModel, ReaderTheme,
             ReaderSettings, ThemesSettingsSheet, CustomizeThemeSheet, SpeechController,
             ReadingProgress, BookmarkStore, SearchSheet, MarqueeText
  Resources/ sample.epub                  được chép vào thư viện lần mở đầu
```

Trạng thái lưu bền nằm trong `UserDefaults` (cài đặt `reader*`, `narrationRateFactor`, đánh dấu,
`readerPositions`) và trong `Documents/Books` là các tệp EPUB.

### Tài liệu dự án và quy tắc làm việc

- `PROJECT_STATE.md`, `TASKS.md`, `SESSION_HANDOFF.md` — trạng thái, danh sách việc và bàn giao,
  mỗi tệp có cả tiếng Anh và tiếng Việt. Đọc trước khi thay đổi bất cứ thứ gì.
- `AGENTS.md` cùng `.ai-toolkit/`, `.agents/skills/`, `.opencode/commands/` — AI Development
  Toolkit v1.1.0 (làm tuần tự, việc nhiều bước phải duyệt kế hoạch trước khi sửa code, khi commit
  chỉ stage đường dẫn cụ thể).
- Các hạn chế đã biết (chưa có icon, ảnh nhúng EPUB chưa hiển thị, trang mở đầu chương bị đậm,
  chưa có test target, …) nằm trong `PROJECT_STATE.md`.

Repo chỉ nằm trên máy: không có remote, chưa push lần nào.
