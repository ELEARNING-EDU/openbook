# TODO — Android

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
  QA trên E1/E2 cho TTS/highlight.

### Việc còn treo từ QA 2026-11-02

- [ ] **T-119** (P1): `TextCleanup` đọc mọi entry, bỏ qua budget 64 MiB của `ZipExtract` — stream
  entry không sửa, từ chối trước khi cấp phát, không ghi đè bản gốc khi lỗi.
- [ ] **T-120** (P2): luật gộp từ lặp xóa câu đúng (`"had had"`); cân nhắc bỏ hoặc chỉ áp dụng khi
  có ngữ cảnh.
- [ ] **T-121** (P2): cleanup sửa cả `<style>`, `<pre>`, `<code>`; chỉ sửa text node hợp lệ.
