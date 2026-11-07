# docs/ — tài liệu dự án Sách (Android EPUB reader)

Cập nhật 2026-11-01. Toàn bộ tài liệu trạng thái, task và bàn giao đã chuyển từ gốc repo
vào thư mục này; không tạo lại bản ở root.

| Cần gì | Đọc ở đâu |
| --- | --- |
| Trạng thái dự án, kiến trúc, Git, điều đã kiểm chứng | `PROJECT_STATE.md` |
| Danh sách task (đang mở và đã xong, kèm bằng chứng) | `TASKS.md` |
| Bàn giao phiên làm việc, việc tiếp theo | `SESSION_HANDOFF.md` |
| Việc chưa làm, đã xếp ưu tiên | `TODO.md` |
| Spec QA độ bền EPUB + benchmark + mini player (vẫn dùng làm tiêu chí nghiệm thu) | `epub-robustness-player-qa.md` |
| Prompt giao agent iOS: nút player trên cả 3 tab | `ios-player-control-three-tabs-prompt.md` |
| Prompt giao agent iOS: title sách ở HOME/LIBRARY (bìa tự tạo vs bìa thật) | `ios-book-title-home-library-prompt.md` |

QA hiện tại: [QA_REVIEW_2026-11-02.md](QA_REVIEW_2026-11-02.md), bằng chứng tại `qa/2026-11-02/`.
Báo cáo 2026-11-01 đã được xóa vì được thay thế. Bằng chứng QA trước 2026-11-02 đã được
xóa trong đợt dọn cũ; các tham chiếu đó trong lịch sử không còn file để mở.

Spec Home parity (`home-ui-parity.md`) và full player parity (`full-player-parity.md`) đã bị
xoá khỏi cây làm việc; nội dung còn lại được ghi trong `PROJECT_STATE.md` (các pass 4–7).

## Nội dung dự kiến

- `HUONG_DAN_SU_DUNG.md` — cài đặt, thư viện và nhập sách, đọc và lật trang, nghe đọc,
  giao diện và cỡ chữ, tìm kiếm, đánh dấu, vị trí đọc được ghi nhớ.

Chưa viết — sẽ bổ sung khi có yêu cầu.
