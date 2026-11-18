# Changelog — OpenBook (Android + iOS)

Một file chung cho cả hai bản: từ 2026-11-11 mọi thay đổi làm trên cả Android và iOS cùng lúc.
Mục mới nhất ở trên cùng. Chỉ ghi khác biệt riêng một nền tảng khi có, trong mục **Chỉ Android** / **Chỉ iOS**.

Số phiên bản theo ngày:
- iOS: `CFBundleShortVersionString` = `YYYY.MM.DD`, build = `YYYYMMDD` (tối đa 3 số, giới hạn của App Store).
- Android: `versionName` theo ngày, `versionCode` = `YYYYMMDDCC` (phải luôn tăng so với bản trước trên Play).

## [Chưa phát hành] — 2026.11.11

iOS: `2026.11.11 (20261111)`, đã archive, chưa upload. Android: chưa nâng `versionCode`/`versionName`
(đang là `2026110700` / `20261107.00`).

### Thêm mới
- Icon ứng dụng mới dạng chữ "OpenBook" (Open màu mực, Book màu đỏ gạch, nền kem), giống logo website.
- Mở file EPUB từ app khác ("Mở bằng OpenBook" / chia sẻ từ Tệp, Mail, Zalo…): tự nhập vào thư viện rồi mở sách.
- Menu chọn chữ chạy thật: Chép, Chia sẻ trích dẫn (kèm tên sách, tác giả), Tìm trong sách, Dịch.
- Ghi chú cho đoạn tô sáng; xuất toàn bộ đoạn tô sáng và ghi chú của một cuốn ra file Markdown.
- Xoay ngang khi đọc; trang rộng hơn 560 tự chia 2 cột.
- Trang đọc sáng/tối tự theo điện thoại cho tới khi người đọc tự chọn.

### Thay đổi
- Lề hai bên trang đọc hẹp lại (30 → 20).
- Thống nhất câu chữ giao diện hai bản; "đánh dấu" = đánh dấu trang, "tô sáng" = highlight.
- Bảng Mục lục / Đánh dấu / Tô sáng và bảng Tìm kiếm cùng nền giấy, cách tab hợp lý, thẳng mép.
- Đoạn tô sáng đã lưu chỉ mở bảng sửa (màu, ghi chú, xoá) khi nhấn giữ.
- Đang tạm dừng nghe đọc mà sang trang khác, bấm Play sẽ đọc từ trang đang mở.

### Sửa lỗi
- Chạy bằng tham số dựng cảnh (chụp ảnh, test) không còn đếm lượt mở và không hiện hộp mời đánh giá.

### Chỉ iOS
- Tên app trên màn hình chính đổi từ "Books"/"Sách" thành "OpenBook"; bundle ID `com.elearning.openbook`.
- Thêm hướng dẫn đọc 4 bước khi mở sách lần đầu và hộp mời đánh giá App Store ở lần mở thứ 10 (Android đã có).
- Điều khiển nghe đọc từ Trung tâm điều khiển / màn hình khoá (bỏ chế độ phát chồng âm thanh).
- Bật âm thanh không còn chặn giao diện lúc bắt đầu nghe đọc.
- Bảng tìm kiếm: ô tìm ở trên cùng thay vì giữa màn hình; đoạn trích có "…" ở hai đầu.
- Chỉ hỗ trợ iPhone (bỏ iPad ở bản đầu); thêm khai báo quyền riêng tư `PrivacyInfo.xcprivacy`.
- Xuất file Markdown không còn cảnh báo kiểu file chưa khai báo.

### Chỉ Android
- Màn thống kê đọc chi tiết khi chạm thẻ Thành tích (iOS đã có).
- Màu tô sáng cùng bộ màu iOS, ô màu có tên cho trình đọc màn hình.
- Chân trang theo ngôn ngữ app ("Page 1 / 15" khi để tiếng Anh).
- Bỏ khoá màn hình dọc; gạch chân tab mục lục dùng màu nâu của theme thay vì tím mặc định.

## 20261107.00 — 2026-11-07 (Android)

Bản Android trước khi gộp phát triển hai nền tảng. Chi tiết trong Git nhánh `android` và `android/docs/PROJECT_STATE.md`.
