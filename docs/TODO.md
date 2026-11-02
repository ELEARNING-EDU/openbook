# TODO — Android

Cập nhật 2026-11-01. Kết quả review 2026-10-29 nằm trong [TASKS.md](TASKS.md); các mục dưới
đây là việc **chưa làm** và đã xếp ưu tiên. Đường dẫn source tính từ root `android/`, dù tài
liệu nằm trong `docs/`.

## Đã xong 2026-11-01

- [x] **T-112:** giới hạn giải nén 64 MiB/entry, 256 MiB/archive ở `ZipExtract` (khai báo và
  luồng thực tế) + `EpubProblem.TOO_LARGE`; `EpubArchive` đọc metadata theo cùng ngân sách.
- [x] **T-108, T-109:** điều khiển notification và sleep timer chuyển sang vòng đời process
  (`playback/Narration`, `playback/SleepTimer`), Back không còn huỷ chúng.
- [x] Đồng bộ tài liệu: `docs/README.md`, `PROJECT_STATE.md`, `TASKS.md`, `SESSION_HANDOFF.md`.

## Ưu tiên sửa lỗi

- [ ] **P2 · T-114:** ưu tiên tác giả do người dùng sửa (`LibraryStore.describe`); test lưu,
  reload, sort và Home. Nhỏ nhất, làm trước.
- [ ] **P2 · T-116:** sửa tìm kiếm Đ/NFD và ánh xạ offset về text gốc; test nhảy trang đúng
  sau emoji/combining marks.
- [ ] **P2 · T-110, T-115:** sau khi xóa EPUB thành công, xóa highlights và bìa cache đúng ID;
  failure phải giữ dữ liệu. Test reimport cùng tên với bìa khác nhưng cùng số byte.
- [ ] **P2 · T-113:** lưu anchor khi TTS chạy sau Back và khi hoàn tất; kiểm cold reopen khớp
  vị trí đã nghe. Làm cùng ownership playback, không thêm store trùng.
- [ ] **P2 · T-117:** stop hủy pending TTS start; test init chậm và đổi sách.

## Tối ưu có căn cứ, làm sau lỗi chức năng

- [x] **A-OPT-01 · bỏ search screen không còn caller.** Đã xoá
  `app/src/main/java/com/elearning/openbook/ui/SearchScreen.kt` (240 dòng) ngày 2026-11-01 sau
  khi xác nhận không còn caller/config; luồng tìm kiếm dùng `SearchSheet`. Gate chạy lại xanh.
- [ ] **A-OPT-02 · tránh parse lại mọi sách sau một thay đổi metadata.** `LibraryStore.books/describe`
  đọc lại ZIP/OPF/bìa cho cả thư viện; `AppViewModel.updateMetadata/markFinished` bỏ `BookItem`
  trả về rồi gọi `books()` lại. Đo số lượt đọc/thời gian trước-sau; giữ quy tắc sort và lỗi đọc.
- [ ] **T-107 · chương rất dài:** số đo 3,2 giây là lịch sử, đo lại trước khi đổi phân trang.
- [ ] **T-106 · icon dependency:** kiểm glyph đang dùng trước khi thay `material-icons-extended`.
- [ ] **T-105 · benchmark:** đo startup/open/page-turn trên release trước khi thêm Baseline Profile.

## Kiểm chứng và tài liệu

- [ ] **T-111:** khôi phục generator + corpus EPUB vào vị trí bền vững trong repo (hiện 33 test
  skip vì thiếu corpus), cập nhật đường dẫn test hai nền tảng; CI/QA phải báo thiếu corpus thay
  vì âm thầm xanh do skip.
- [ ] Sau từng nhóm sửa, chạy test tập trung; gate cuối: `./gradlew :app:testDebugUnitTest
  :app:lintDebug :app:assembleDebug`. Ghi số pass/skip/fail thực, không dùng số QA cũ.
- [ ] QA lifecycle TTS trên thiết bị: init chậm, Back/reopen, pause/seek, kết thúc sách, timer,
  gọi điện/đổi đầu ra audio. `emulator-5554` đang bị phiên khác điều khiển nên cần đọc logcat
  để loại nhiễu.
- [ ] Giữ backlog T-066/T-067 về tablet/cột và T-073 audio thật; không đánh dấu xong từ phân
  tích tĩnh.

## Giới hạn review

Đã rà luồng chức năng và code quan trọng; ba lỗi P1 đã sửa và có gate + bằng chứng device.
Chưa chạy lại toàn bộ corpus, chưa đo hiệu năng/device/accessibility mới, chưa review từng dòng
UI trang trí/curl geometry.

## Kế hoạch tính năng — ghi 2026-11-02, làm sau

### Đọc theo kiểu slide và scroll

Hiện trạng: `ReaderEngine` phân trang bằng `StaticLayout`, `CurlView` vẽ lật cong, `ReadingPosition`
lưu `page/pageCount/chapter/offset`.

- [ ] **P1 · Slide mode.** Thêm `ReadingMode { CURL, SLIDE, SCROLL }` vào `ReaderSettings` (prefs như
  theme). `CurlView` nhận mode: `SLIDE` bỏ phần vẽ cong, translate theo ngón tay + snap. UI ở
  Reading settings dạng popover. Không đổi vị trí/TTS/tìm kiếm. Chỉ đụng `CurlView` + settings.
- [ ] **P2 · Scroll mode.** `LazyColumn` theo chương, mỗi chương một `StaticLayout` không chia cột.
  Chuyển các chỗ đang giả định "page" sang offset: vị trí đọc (`ReadingPosition.chapter/offset`),
  `rangeRects` cho highlight, `Hit.pageIndex` khi tìm kiếm, auto-scroll theo câu TTS, thanh tiến độ
  ở Library tính gần đúng từ tổng ký tự. `HighlightStore` dùng được nguyên. Ước lượng 3–5 ngày;
  rủi ro chính là đồng bộ TTS ↔ scroll và các phép đổi page ↔ offset.

### Đọc định dạng khác ngoài EPUB

Hiện trạng: importer/`describe()` chỉ hiểu EPUB; `RawChapter` chứa HTML.

- [ ] **P3 · TXT / Markdown / HTML.** Bọc thành HTML chapter → dùng nguyên ReaderEngine (text, TTS,
  tìm kiếm, highlight). ~1 ngày.
- [ ] **P4 · PDF + CBZ/CBR.** Thêm track render ảnh: `BookContent.PageBook(pages)`, xem bằng
  `PdfRenderer`/ảnh giải nén, lật hoặc cuộn, zoom, cache LRU theo trang. TTS/highlight/tìm kiếm
  không áp dụng (ẩn nút đọc to). ~3–4 ngày cho PDF, +1 ngày cho CBZ.
- [ ] **P5 · MOBI/AZW3.** Không tự parse (PalmDOC/HUFF-CDIC, dễ sai, dễ vướng DRM/bản quyền).
  Chỉ báo "hãy chuyển sang EPUB", trừ khi người dùng đồng ý nhúng converter ngoài.
- [ ] **Kiến trúc chung:** `BookFormat` + `FormatDetector` (magic bytes/đuôi file) lúc import;
  `BookContent` sealed `TextBook` / `PageBook`; `LibraryStore.import` rẽ nhánh; `ReaderScreen` chọn
  `TextReaderScreen` (P1/P2) hoặc `PageReaderScreen` (P4); metadata/cover theo từng định dạng.
- [ ] **Thứ tự:** P1 → P2 → P3 → P4 → P5. Rủi ro: scroll đụng nhiều chỗ giả định page; PDF tốn RAM;
  MOBI cần thư viện ngoài; chưa có `androidTest` nên P1/P2 cần thêm unit test map offset↔vị trí và
  QA E3 cho TTS/highlight.

### Việc còn treo từ QA 2026-11-02

- [ ] **T-119** (P1): `TextCleanup` đọc mọi entry, bỏ qua budget 64 MiB của `ZipExtract` — stream
  entry không sửa, từ chối trước khi cấp phát, không ghi đè bản gốc khi lỗi.
- [ ] **T-120** (P2): luật gộp từ lặp xóa câu đúng (`"had had"`); cân nhắc bỏ hoặc chỉ áp dụng khi
  có ngữ cảnh.
- [ ] **T-121** (P2): cleanup sửa cả `<style>`, `<pre>`, `<code>`; chỉ sửa text node hợp lệ.
