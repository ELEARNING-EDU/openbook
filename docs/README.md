# EpubReader (iOS)

Native demo EPUB reader for **Ngọn Hải Đăng Cuối Cùng**: SwiftUI shell, a UIKit
`UIPageViewController` page curl, a Vietnamese read-aloud player, themes and per-book reading
position. The iOS twin of the Android demo `epub-reader-android`. No third-party dependencies.

> English first, bản tiếng Việt ở nửa dưới của cùng tệp.

## Requirements

- macOS with Xcode 27.2 (the version last built and tested, 2026-11-11).
- iOS 18.0 deployment target; Swift 5 language mode.
- A simulator, e.g. `iPhone 17 Pro` on runtime iOS 27.0 (the device used for this run).

## Build and run

```sh
xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath build/dd build

xcrun simctl boot "iPhone 17 Pro" ; open -a Simulator
xcrun simctl install booted build/dd/Build/Products/Debug-iphonesimulator/EpubReader.app
xcrun simctl launch booted com.elearning.openbook
```

Launch arguments are passed as `-key value` pairs and become `UserDefaults` values for that run,
which is how the reader is driven for verification:

```sh
xcrun simctl launch booted com.elearning.openbook -demoOpenBook 1 -demoPage 4 -demoDebug 1
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

`EpubReaderTests` covers the pure logic (pagination, EPUB/ZIP parsing, reading progress, stats,
language, cover detection, localisation):

```sh
xcodebuild test -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -derivedDataPath build/dd
```

`EpubCorpusTests` skips while `build/qa/epub-corpus/manifest.json` is absent. Behaviour is checked
on the simulator with injected touches — `xcrun simctl io booted screenshot` for stills (retake
after an animation, the first frame may tear) and the app's own `-demoTap*` hooks or `idb` for
taps/swipes in **points** (402×874 on the iPhone 17 Pro).

## Release build (App Store)

Bundle ID `com.elearning.openbook`, team `KTKL66WRBK`, automatic signing. The archive is built unsigned and
signed with the App Store profile at export (Xcode creates "iOS Team Store Provisioning Profile:
com.elearning.openbook" itself), so it does not depend on a development profile:

```bash
xcodebuild archive -project EpubReader.xcodeproj -scheme EpubReader -configuration Release \
  -destination 'generic/platform=iOS' -archivePath build/OpenBook.xcarchive CODE_SIGNING_ALLOWED=NO
xcodebuild -exportArchive -archivePath build/OpenBook.xcarchive -exportOptionsPlist ExportOptions.plist \
  -exportPath build/export -allowProvisioningUpdates
```

Output: `build/export/EpubReader.ipa`. Needs the "Apple Distribution: QUANG CUONG DINH" identity in the login
keychain (`security find-identity -v -p codesigning`) and the Apple ID signed in to Xcode.

Run on a cabled iPhone (registered to the team on 2026-11-11, Developer Mode on):

```bash
xcodebuild build -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug \
  -destination 'id=<UDID>' -derivedDataPath build/dd-device -allowProvisioningUpdates
xcrun devicectl device install app --device <UDID> build/dd-device/Build/Products/Debug-iphoneos/EpubReader.app
xcrun devicectl device process launch --device <UDID> com.elearning.openbook
```

`xcrun devicectl list devices` shows the UDID.

## Layout

```
EpubReader/
  App/       EpubReaderApp, MainView, MiniPlayerButton, AppSettingsView, L10n
  Data/      EpubArchiveReader, ZipArchive  container.xml → OPF → spine, TOC
  Domain/    Models, AttributedBuilder, Paginator, ChapterImages, BookLanguage,
             BookCoverImage, TextSelection
  Home/      HomeView, HomeFeed, HomeSections, HomeBookRow, ReadingStatsCard,
             ReadingStatsSheet, ReadingStatsText, WeeklyReadingChart
  Library/   LibraryStore, LibraryView, LibraryMetadata, BookCoverStore, BookEditSheet
  Reader/    ReaderView, PageCurlPager, ReaderPage, ReaderViewModel, ReaderTheme,
             ReaderSettings, ThemesSettingsSheet, CustomizeThemeSheet, SpeechController,
             VoiceCatalog, VoicePickerSheet, NowPlayingView, HighlightStore, BookmarkStore,
             ReadingProgress, ReadingStatsStore, SearchSheet, MarqueeText
  Store/     StoreView                     static demo, no server
  Resources/ sample.epub                  seeded into the library on first launch
```

Persistent state lives in `UserDefaults` (`reader*` settings, `readingGoalMinutes`,
`narrationRateFactor`, `narrationVoiceID`, `readingStats`, bookmarks, highlights, `readerPositions`)
and in `Documents/Books` for the EPUB files; finished books carry `finishedAt` in the library
metadata.

## Project docs and working rules

- `docs/PROJECT_STATE.md`, `docs/TASKS.md`, `docs/SESSION_HANDOFF.md` — state, task list and
  handoff, both English and Vietnamese in each file. Read them before changing anything.
- `AGENTS.md` and `.ai-toolkit/`, `.agents/skills/`, `.opencode/commands/` — AI Development
  Toolkit v1.1.0 (work sequentially, plan approval before multi-step code changes, stage explicit
  paths when committing).
- `EpubReaderTests/` — unit tests for the pure logic; `EpubCorpusTests` needs the corpus manifest
  in `build/qa/epub-corpus/` and skips without it.
- Known limitations (no app icon, chapter-opening page weight, dark-theme fold undecided, …) are
  listed in `docs/PROJECT_STATE.md`.

The `ios` branch is local only: nothing has been pushed.

---

## Bản tiếng Việt

Ứng dụng đọc EPUB demo chạy native trên iOS cho sách **Ngọn Hải Đăng Cuối Cùng**: vỏ SwiftUI,
lật trang bằng page curl của `UIPageViewController` (UIKit), player đọc thành tiếng tiếng Việt,
theme và vị trí đọc riêng cho từng sách. Bản iOS song song với demo Android `epub-reader-android`.
Không dùng thư viện ngoài.

### Yêu cầu

- macOS có Xcode 27.2 (phiên bản dùng để build và kiểm lần cuối, 2026-11-11).
- Deployment target iOS 18.0; Swift 5.
- Một simulator, ví dụ `iPhone 17 Pro` runtime iOS 27.0 (máy đã dùng cho lần chạy này).

### Build và chạy

```sh
xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath build/dd build

xcrun simctl boot "iPhone 17 Pro" ; open -a Simulator
xcrun simctl install booted build/dd/Build/Products/Debug-iphonesimulator/EpubReader.app
xcrun simctl launch booted com.elearning.openbook
```

Tham số khởi chạy truyền theo cặp `-key value` và trở thành giá trị `UserDefaults` cho riêng lần
chạy đó — đây là cách trình đọc được điều khiển khi kiểm chứng:

```sh
xcrun simctl launch booted com.elearning.openbook -demoOpenBook 1 -demoPage 4 -demoDebug 1
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

`EpubReaderTests` phủ phần logic thuần (phân trang, đọc EPUB/ZIP, vị trí đọc, thống kê, ngôn ngữ,
nhận diện bìa, bản địa hoá):

```sh
xcodebuild test -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -derivedDataPath build/dd
```

`EpubCorpusTests` tự skip khi thiếu `build/qa/epub-corpus/manifest.json`. Hành vi được kiểm trên
simulator bằng thao tác chạm — `xcrun simctl io booted screenshot` để chụp (chụp lại sau khi
animation xong, khung đầu có thể bị rách), và móc `-demoTap*` của app hoặc `idb` để chạm/vuốt theo
**point** (402×874 trên iPhone 17 Pro).

### Bản phát hành (App Store)

Bundle ID `com.elearning.openbook`, team `KTKL66WRBK`, ký tự động. Archive không ký, rồi ký bằng hồ sơ
App Store ở bước export (Xcode tự tạo hồ sơ), nên không phụ thuộc hồ sơ Development. Dùng hai lệnh ở mục
"Release build" phía trên; kết quả là `build/export/EpubReader.ipa`. Cần danh tính "Apple Distribution:
QUANG CUONG DINH" trong keychain login và Apple ID đã đăng nhập trong Xcode.

### Bố cục mã

```
EpubReader/
  App/       EpubReaderApp, MainView, MiniPlayerButton, AppSettingsView, L10n
  Data/      EpubArchiveReader, ZipArchive  container.xml → OPF → spine, mục lục
  Domain/    Models, AttributedBuilder, Paginator, ChapterImages, BookLanguage,
             BookCoverImage, TextSelection
  Home/      HomeView, HomeFeed, HomeSections, HomeBookRow, ReadingStatsCard,
             ReadingStatsSheet, ReadingStatsText, WeeklyReadingChart
  Library/   LibraryStore, LibraryView, LibraryMetadata, BookCoverStore, BookEditSheet
  Reader/    ReaderView, PageCurlPager, ReaderPage, ReaderViewModel, ReaderTheme,
             ReaderSettings, ThemesSettingsSheet, CustomizeThemeSheet, SpeechController,
             VoiceCatalog, VoicePickerSheet, NowPlayingView, HighlightStore, BookmarkStore,
             ReadingProgress, ReadingStatsStore, SearchSheet, MarqueeText
  Store/     StoreView                     demo tĩnh, không có server
  Resources/ sample.epub                  được chép vào thư viện lần mở đầu
```

Trạng thái lưu bền nằm trong `UserDefaults` (cài đặt `reader*`, `readingGoalMinutes`,
`narrationRateFactor`, `narrationVoiceID`, `readingStats`, đánh dấu, highlight, `readerPositions`)
và trong `Documents/Books` là các tệp EPUB; sách đã đọc xong mang `finishedAt` trong metadata thư
viện.

### Tài liệu dự án và quy tắc làm việc

- `docs/PROJECT_STATE.md`, `docs/TASKS.md`, `docs/SESSION_HANDOFF.md` — trạng thái, danh sách việc
  và bàn giao, mỗi tệp có cả tiếng Anh và tiếng Việt. Đọc trước khi thay đổi bất cứ thứ gì.
- `AGENTS.md` cùng `.ai-toolkit/`, `.agents/skills/`, `.opencode/commands/` — AI Development
  Toolkit v1.1.0 (làm tuần tự, việc nhiều bước phải duyệt kế hoạch trước khi sửa code, khi commit
  chỉ stage đường dẫn cụ thể).
- `EpubReaderTests/` — unit test cho phần logic thuần; `EpubCorpusTests` cần manifest corpus trong
  `build/qa/epub-corpus/`, thiếu thì skip.
- Các hạn chế đã biết (chưa có icon, trang mở đầu chương bị đậm, chưa quyết nếp gấp theme tối, …)
  nằm trong `docs/PROJECT_STATE.md`.

Nhánh `ios` chỉ nằm trên máy: chưa push lần nào.
