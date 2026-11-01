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

- [ ] **A-OPT-01 · bỏ search screen không còn caller.** `app/src/main/java/com/elearning/openbook/ui/SearchScreen.kt`
  (~240 dòng) chỉ còn declaration/log; luồng thực dùng `SearchSheet`. Xóa sau khi xác nhận
  caller/config, compile và smoke search sheet.
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
