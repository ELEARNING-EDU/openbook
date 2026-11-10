# Project state — Sách (Android EPUB reader)

Cập nhật 2026-11-10. Đây là bản tham chiếu: trạng thái hiện tại, phạm vi, quyết định kiến trúc,
giới hạn. Nhật ký theo ngày trước 2026-11-07 nằm trong Git — xem
`git show edd6de4:docs/PROJECT_STATE.md`.

## Facts at a glance

| Fact | Value |
| --- | --- |
| App / package | **Sách** · `com.elearning.openbook` (applicationId = namespace) |
| Version | `versionCode 2026110700` · `versionName 20261107.00` (date-based, `YYYYMMDDCC`) |
| Module / size | `:app` only · 56 Kotlin files · **16.198 dòng** trong `app/src/main/java`; 14 file test |
| Toolchain | Gradle 9.8.0 · AGP 9.4.1 · Kotlin 2.4.20 · JDK 17 · Hilt 2.60.1 · Kover 0.9.11 |
| SDK | compileSdk 37 · targetSdk 36 · minSdk 26 |
| Compose | BOM 2026.09.00 (ui, foundation, material3) · icon vẽ bằng `Canvas` |
| State / DI | Compose state + `SharedPreferences` + `filesDir/Books`; graph Hilt (`ReaderApplication`, `@AndroidEntryPoint MainActivity`); shell state `AppViewModel` (`StateFlow<AppUiState>`); reader `ReaderViewModel`; chrome/speech state cục bộ; không Nav, không Room |
| Tests | **137 JVM test** (JUnit 4.13.2) — 104 pass, **33 skip** vì thiếu EPUB corpus (T-111), 0 fail/error; `./gradlew :app:testDebugUnitTest` (2026-11-07) |
| Lint | 113 warning, **0 error**; `lint { abortOnError = true }` |
| Release | APK `1.788.427 bytes` sha256 `802d64cf…846f17`; AAB `4.362.665 bytes` sha256 `059c9869…10998c`, `bundletool validate` OK |
| Chữ ký | `CN=OpenBook, OU=Mobile, O=OpenBook, L=Ho Chi Minh, ST=HCMC, C=VN`; cert SHA-1 `6B:59:9A:7A:93:4A:62:54:CC:42:B1:8F:15:CA:ED:7D:3E:C8:9C:F4`, SHA-256 `4B:D1:45:6E:…:0D:97:F3`. Key: `keystore/openbook-release (12345678!@#).jks`, alias `openbook` (bị `.gitignore`) |
| Play upload | **Bị chặn**: Play chờ upload cert `B3:A7:F5:1F:0E:2F:FA:1B:15:0A:24:75:64:25:C3:DB:00:88:46:5F`; đã gửi request reset ngày 2026-11-07 với `keystore/upload_certificate.pem` (cert `6B:59…`), chờ duyệt |
| Sample book | `app/src/main/assets/sample.epub`, 9.720 bytes, seed vào `filesDir/Books` lần đầu |
| Từ điển chính tả | `app/src/main/assets/vi-words.txt`, 1.974 từ tự soạn; không nhúng dữ liệu Viet74K |
| Thiết bị | **E1 = `emulator-5556`**, **E2 = `emulator-5554`**; Android 16 / API 36, 1344×2992 @480; cả hai chạy bản cài từ AAB (`splits=[base, config.arm64_v8a, config.en]`). E1 đặt `animator_duration_scale=1` |
| Git | Worktree `OpenBook/.git/worktrees/android`, branch `android` track `origin/android` (`github.com/cuongdq88/openbook-android`), HEAD `edd6de4` = `origin/android`; working tree chỉ có `AGENTS.md` bị sửa (không thuộc phiên này) |
| Việc tiếp theo | Chờ duyệt upload key → upload AAB; code: T-119/T-120 (P1), T-121, T-111. Xem `TASKS.md` |

## Component map

| Path | Responsibility |
| --- | --- |
| `MainActivity.kt` | `enableEdgeToEdge`, ngôn ngữ app qua `attachBaseContext`, `DemoOptions` từ intent extras, mở review Play ở lần mở thứ 10 |
| `ReaderApplication.kt` | `@HiltAndroidApp`; đếm launch một lần mỗi tiến trình (`ReviewPromptStore.countLaunch`) |
| `settings/ReviewPromptStore.kt` | cờ `launches` / `asked` trong `shared_prefs/reviewPrompt.xml`; ngưỡng 10 |
| `settings/TextRepairStore.kt` | tuỳ chọn "Fix spelling on import" (mặc định **tắt**) |
| `settings/AppLanguage.kt` | override ngôn ngữ app, không đổi locale máy |
| `ui/EpubReaderApp.kt` (463) | state cấp app: sách, sách đang mở, tab, tiến độ, demo budget một lần |
| `ui/MainScreen.kt` | 3 tab Home / Library / Store, import SAF dùng chung |
| `ui/HomeScreen.kt` (717) | goal ring, streak, shelf "đang đọc", chart tuần |
| `ui/LibraryScreen.kt` (934) | shelf grid/list, cover gradient, % đã đọc, menu sửa/xoá |
| `ui/ReaderScreen.kt` (2.357) | phiên đọc: phân trang, chrome 3 mức, tap routing, sheet, player |
| `ui/AudioPlayerSheet.kt` (747) | full player: cover, seek slider, back/play/forward, sleep timer, voice picker |
| `ui/Components.kt` (1.484), `ui/AppearanceSheet.kt`, `ui/CustomizeScreen.kt` (600) | icon vẽ tay, theme sheet, customizer toàn màn hình |
| `reader/CurlView.kt` (1.163) | curl vẽ tay: drag/fling/tap, `foldGeometry`, crease report |
| `reader/ReaderEngine.kt` (804) | EPUB → chương → `StaticLayout` → bitmap trang (LRU 3 trang) |
| `reader/Typesetting.kt`, `reader/Sentences.kt` | một nguồn typography cho trang + preview; chia câu |
| `reader/SpeechReader.kt` (750) | TTS vi-VN: queue, rate, state, voice, callback, hook lưu vị trí |
| `reader/ReaderSettings.kt`, `reader/ReaderTheme.kt` | 6 theme × light/dark, cấu hình theo theme, prefs |
| `reader/BookmarkStore.kt`, `reader/ReadingPositions.kt`, `reader/ReadingLog.kt` | prefs `readerBookmarks`, `reading_positions`, `readingLog` |
| `library/LibraryStore.kt`, `library/BookMetadata.kt`, `library/HomeSections.kt` | file `.epub` trong `filesDir/Books`, metadata, shelf |
| `playback/Narration.kt`, `playback/PlaybackService.kt`, `playback/SleepTimer.kt` | media session, notification, timer theo process |
| `epub/*` (7 file) | `ZipExtract` (budget 64/256 MiB) → OPF → spine, `nav`/`ncx`, `HtmlText`, `ChapterImages`, `TextCleanup` (sửa chính tả), `VietnameseWords` |

## Tính năng đang có

- Thư viện: import EPUB qua SAF, cover gradient, grid/list, 3 kiểu sắp xếp, sửa tên/tác giả,
  xoá đơn/chọn nhiều, % đã đọc, đánh dấu đã đọc xong.
- Đọc: lật trang curl vẽ tay (drag/fling, cuộn chéo), 3 mức chrome, mục lục, tìm kiếm bỏ dấu,
  bookmark, 6 theme × sáng/tối, customizer cỡ chữ/giãn dòng/cột/căn đều.
- Nghe: TTS tiếng Việt từ câu đang chọn, highlight câu, tự lật trang, mini player + full player,
  chọn giọng/tốc độ, sleep timer, nghe nền khi rời màn hình (media notification), lưu vị trí.
- Thống kê: mục tiêu phút/ngày, streak, chart tuần, shelf Home.
- Nhập sách: bản sao được chuẩn hoá Unicode và (tuỳ chọn, mặc định tắt) sửa chính tả tiếng Việt
  im lặng; chặn EPUB DRM/hỏng với thông báo EN/VI.
- Review: thẻ Play In-App Review ở lần mở app thứ 10, một lần duy nhất.

## Scope & requirements

- Sản phẩm: EPUB reader native (Kotlin + Jetpack Compose) cho tiếng Việt, lật trang bằng
  `View` curl tự viết, có read-aloud; sách mẫu và nhãn UI tiếng Việt.
- Ràng buộc đã duyệt:
  - Không thư viện bên thứ ba ngoài những gì khai trong `gradle/libs.versions.toml`
    (androidx core-ktx, activity-compose, lifecycle, Compose BOM, media, Hilt, Play Review).
  - EPUB đọc bằng `java.util.zip.ZipInputStream`; `META-INF/container.xml` → OPF
    (dc:title, dc:creator, manifest, spine); TOC từ EPUB3 `nav`, không có thì `toc.ncx`;
    chương XHTML → `Spannable` qua `Html.fromHtml(..., FROM_HTML_MODE_COMPACT, ...)`.
  - Phân trang `StaticLayout.Builder` (serif 19sp, ink `#29241F`, line spacing 1.14,
    `includePad = false`), cắt theo dòng.
  - Curl trong `View` riêng (không `ViewPager`): bitmap trang dưới, trang hiện tại cắt theo
    nếp, dải gấp `2/π`, print-through, gradient cylinder, commit ở nửa pull hoặc fling.
  - Chrome: nút tròn nổi, đường tiến độ mảnh trên cùng, TOC là bottom sheet.
  - `applicationId`/`namespace` = `com.elearning.openbook`, tên app "Sách".
  - Không quyền network; chỉ launcher activity.
  - Read-aloud: đọc từ câu đầu trang đang mở, tự sang trang/chương; tap câu để đọc từ đó;
    câu đang đọc được highlight và trang tự lật.
  - Chrome 3 mức `FULL` / `PLAYER_ONLY` / `HIDDEN`; auto-hide ~4,5 s; player + gear còn ở mức
    (b) khi giọng đọc chạy (kể cả pause); không bao giờ có trạng thái đang đọc mà không thoát
    được sách.
  - Hai cặp nút trên thành capsule: trái (back · mục lục), phải (tìm · đánh dấu); mỗi icon 44dp;
    ở mức (b) cả hai capsule ẩn.
  - Customizer preview dùng đúng `Typesetting` của trang.
  - Player: 4 nút 32dp trong hàng 48dp, headline marquee, đường tiến độ 2,5dp theo
    ký tự đã đọc / tổng ký tự sách; thanh đọc amber chỉ hiện khi `PLAYING`.
  - Settings hiện tại: bottom sheet, mỗi lựa chọn là một hàng mở popover neo đúng hàng
    (`Popup` + `PopupPositionProvider`, 220dp), nhóm LANGUAGE / READING / AUDIO, có sleep timer.
  - Sửa chính tả: mặc định tắt, chạy im lặng (không hộp thoại), nhận diện tiếng Việt theo nội
    dung từng tài liệu (≥2% chữ cái có dấu, ≥20 chữ cái) chứ không tin `dc:language`.

## Decision index — ràng buộc cho việc mới

1. Curl là `View` viết tay, không phải pager: bitmap nền, trang cắt theo nếp, dải gấp `2/π`,
   print-through, gradient cylinder.
2. Nếp gấp bản lề ở gáy; crease đi `w · |cos lean|`; lean của tay bị fade khỏi hình học khi pull
   tăng (`LEAN_FADE_FROM = 0.12` → `LEAN_FADE_TO = 0.62`), nên pull đủ thì crease thẳng đứng ở gáy.
3. Commit ở nửa pull (`COMMIT_FRACTION = 0.5`) hoặc fling 900 px/s; vuốt gần dọc không lật trang
   (`DRAG_AXIS_FRACTION = 0.35`, mở T-076).
4. Chrome 3 mức; gear chỉ có ở `FULL`; player giữ chỗ 44dp để pill không đổi bề rộng.
5. Tap 1/3 trang chỉ lật khi `speechState == IDLE`; lật qua `jumpToPage`; tap thứ hai xếp hàng
   trên `jumpTarget`.
6. Giọng đọc đi theo tay, chỉ tiến, qua một cửa duy nhất `pageShown(page)`.
7. Typesetting một nguồn cho cả preview và trang.
8. Harness một lần mỗi launch (`staged` / `demoPending`): mở sách không bao giờ tự chạy giọng.
9. Lưu trữ: `readerSettings`, `readerBookmarks`, `reading_positions` (`page|pageCount|chapter|offset`),
   `readingLog`, `reviewPrompt` và file `.epub` trong `filesDir/Books`.

## Architecture decisions

- **Bitmap trang lazy trong LRU 3 trang** (đồng bộ), không render sẵn: một trang ARGB_8888
  ~16 MB ở 1344×2992, render sẵn sẽ nổ heap với sách dài.
- **Khoảng cách đoạn** bằng sentinel `'\uE000'` sau tag đóng block, thay bằng
  `ParagraphGapSpan(LineHeightSpan)`: `FROM_HTML_MODE_COMPACT` chỉ phát một `\n`, không đủ.
- **Range câu** tính trên đúng `CharSequence` mà `StaticLayout` dùng, map sang trang qua
  `lineToPage` của chương: highlight và tap phải khớp chính xác phân trang.
- **`Layout.getOffsetForHorizontal`** sau `getLineForVertical` thay `getOffsetForPosition`
  (API ẩn, không có trong stub).
- **Clip thân trang** ở `min(textTop + textHeight, topShift + getLineBottom(slot.endLine))`:
  dòng cuối đoạn mang gap trong box, clip lỏng làm dòng đầu trang sau rỉ vào (đã sửa trùng dòng).
- **`CurlView.animateToPage`** xếp hàng target và chạy từng curl một (`MAX_QUEUED_STEPS = 4`,
  xa hơn thì đặt ngay): bước nhảy 2 trang ở ranh giới chương trước đây không vẽ gì.
- **`PageMetrics` là nguồn duy nhất** cho paginator, renderer và chrome (`hInset 30dp`,
  header/footer 14dp, gap 14dp, button 44dp, `bottomInset 18dp`, `controlBand 48dp`,
  `hMargin 16dp`), running head nằm trên hàng toolbar.
- **Chrome resting tính từ state sống** tại lúc timer chạy (`speech.state`), không từ giá trị
  chụp lúc compose — coroutine của `LaunchedEffect` sống lâu hơn composition.
- **`Typesetting.kt` là nguồn duy nhất** cho trang và preview customizer.
- **Mọi shadow vẽ tay** (`softCircleShadow`, `softRoundShadow`): `Modifier.shadow` bị
  polygon hoá dưới software renderer, nút tròn thành hình bảy cạnh.
- **Đổi cỡ chữ**: ghi `(chapter, offset)` trước khi rebuild, rồi chọn trang chứa offset;
  `HtmlText.toSpannable` chạy lại trên main thread (span mutate), debounce ~350 ms.
- **Marquee** giữ vị trí đầu khi không chạy (`scrolling = playing`), tránh nhãn rỗng khi pause.
- **Demo extras** phải đổi cả view lẫn Compose state: `AndroidView` update đẩy page của state
  vào view, nên jump imperative không cập nhật state sẽ bị ghi đè.
- **Light/dark là mode toàn app**, không phải thuộc tính của theme; `Palette.dark` suy từ độ
  sáng của `paper` (như iOS suy `isDark`).
- **9 trường typography trong `TypeConfig` theo theme**; `custom` chỉ là cổng cho 4 slider;
  dấu `*` so 9 trường và bỏ qua `custom`.
- **Engine dàn chữ theo bề rộng cột** rồi gom cột thành trang (`chunked(columnCount)`): một
  đường phân trang cho cả 1 và 2 cột.
- **Giãn từ** bằng một `MetricAffectingSpan` cho mỗi dấu cách, không dùng
  `Paint.setWordSpacing` (API 29+, đo trên API 36 không đổi ngắt dòng).
- **Font lấy từ máy** (`Typeface.create`, `createFromFile` cho `file:` trong `/system/fonts`),
  lọc bỏ face `Noto Sans …` theo script, tối đa 36.
- **Customizer áp live** (`settings.update`), X khôi phục snapshot lúc mở màn hình.
- **Sửa chính tả** chỉ chèn `đ` khi từ gốc không có trong `vi-words.txt`, có dấu tiếng Việt và
  dạng `đ`+từ là từ đã biết; sửa chữ lặp trong từ khi dạng gộp có trong từ điển; giữ hoa/thường.
  Chạy im lặng lúc import và một lần cho sách cũ (`LibraryStore.cleanLibrary`, cờ `clean.<id>`).
- **Review** đếm ở `Application.onCreate` (một lần mỗi tiến trình), không đếm ở `onResume`;
  cờ `asked` để không hỏi lại; Play tự lo quota và im lặng khi app không cài từ Play.

## Limitations (as built, 2026-11-10)

1. Chỉ có chế độ lật curl; slide/scroll mode mới là kế hoạch (`TODO.md` P1/P2).
2. `<img>` có render qua `ChapterImages`, nhưng CSS của sách bị bỏ qua; `<blockquote>` chỉ đổi
   màu. Caption ảnh bị đọc thành câu.
3. Bookmark là offset ký tự, không phải word anchor.
4. Chưa từng nghe giọng đọc qua loa/Bluetooth thật; chỉ xác nhận bằng log tổng hợp TTS và
   chuyển trạng thái PLAYING/PAUSED/IDLE.
5. Test chỉ JVM (`app/src/test`), không có CI; reader/TTS/player vẫn phải QA trên emulator.
   33 ca corpus skip vì thiếu `../ios/build/qa/epub-corpus`.
6. Full player co theo nội dung, không phải nửa màn hình: ~60% trên 1344×2992, ~93% trên
   360×640dp; landscape Material chặn 640dp và nội dung phải cuộn. Ở 360dp headline thanh đọc
   co còn "Hải Đ" (T-085).
7. Tablet, trang 2 cột chứa 2 chương, tiêu đề chương dài chưa test.
8. Sửa chính tả còn 3 lỗi đã biết: T-119 (bỏ qua budget ZIP), T-120 (xoá từ lặp hợp lệ),
   T-121 (sửa cả `<style>/<pre>/<code>`).
9. Store offline có chủ đích — không catalogue, không quyền `INTERNET`.
10. Thẻ review do Play quyết định có hiện hay không; mở lần thứ 10 vẫn có thể không thấy thẻ.
11. Upload Play đang chờ reset upload key; AAB hiện tại chưa lên được.

## Verification — cách kiểm lại

```bash
cd /Users/cuong/Workspace/Projects/openbook/android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:assembleRelease :app:bundleRelease

adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 shell am start -n com.elearning.openbook/.MainActivity --ez demoOpenBook true
adb -s emulator-5556 logcat -s ReaderScreen SleepTimer Narration Review
```

- Kỳ vọng `BUILD SUCCESSFUL`; test 137 / skip 33 / fail 0; lint 0 error.
- Log hữu ích: `typeset …; resumed at page N`, `chrome=full|player_only|hidden`,
  `curl pull: … -> turn|fall back`, `crease border …; fold lean=…deg`,
  `review sheet unavailable: …`.
- Vị trí đọc (bản debug): `adb shell run-as com.elearning.openbook cat
  /data/data/com.elearning.openbook/shared_prefs/reading_positions.xml` (page 0-based).
- Bộ đếm review: `…/shared_prefs/reviewPrompt.xml`.
- Harness: bất kỳ extra `demo*` nào, ví dụ `--ef demoCurl 0.7 --ef demoCurlAngle -32.7`;
  trạng thái chỉ bắn một lần mỗi launch.

## Known risks

- Hai emulator dùng chung với phiên khác: có thể bị `force-stop`/giật tap giữa test; đọc logcat
  ngay sau mỗi thao tác. Lấy toạ độ từ `uiautomator dump`, không dùng toạ độ nhớ sẵn.
- `adb exec-out screencap` làm nghẽn pipeline hiển thị và không bắt được frame animation; dùng
  `adb shell 'timeout 15 screencap -p /sdcard/x.png'` + `adb pull`, hoặc burst trên máy.
- `LaunchedEffect` sống lâu hơn composition: quyết định bên trong phải đọc `MutableState` sống.
- `Paint.setWordSpacing` vô tác dụng trên API 36 (giãn từ đã chuyển sang span).
- Touch injection có thể kẹt trên AVD (app vẫn focus, logcat sạch); `adb reboot` giải quyết
  (~85 s). Ưu tiên một bước `input` ngắn mỗi lệnh shell và kiểm tra từng bước.
- `uiautomator dump` không thấy node nào trong trang đọc vì trang là bitmap `CurlView` — bình
  thường, không phải lỗi.
- Sách mẫu có `dc:title`/`dc:creator` là nội dung demo, không phải sách thật.
