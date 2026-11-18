# Feature map

Đường tới từng tính năng và dấu hiệu "chạy đúng". Cột `id` khớp với `PAGES` trong `scripts/verify.mjs`; thêm tính năng mới thì thêm cả hai.

| id | Tính năng | Đường tới | Cần | Đúng khi |
|---|---|---|---|---|
| home | Trang chủ: slider, sách nổi bật, mới, hàng theo thể loại | `/` | dữ liệu seed | có tiêu đề "Sách nổi bật", slider `aria-roledescription="carousel"` |
| books | Danh sách sách, phân trang | `/books` | seed | có thẻ sách, không có chữ lỗi |
| books-filter | Lọc theo thể loại | `/books?category=triet-hoc` | seed | chỉ sách Triết học (ví dụ Tao Te Ching) |
| search-special | Tìm kiếm với ký tự đặc biệt | `/books?q=(` | — | HTTP 200, chữ "Không tìm thấy sách" |
| category | Trang thể loại | `/categories/van-hoc-nuoc-ngoai` | seed | slider + lưới sách |
| book | Chi tiết sách, link đọc/tải, sách liên quan | `/books/pride-and-prejudice` | seed | link `/go/0` mở tab mới, có "Sách liên quan" |
| book-reader | Nút "Đọc online" (trình đọc EPUB) | `/books/sach-mau-openbook` | seed với `SEED_BASE_URL` | desktop: có nút "Đọc online"; điện thoại: mời mở ứng dụng. Lật trang: e2e `book-reader.spec.ts` |
| book-signed-in | Form đánh giá khi đã đăng nhập | `/books/pride-and-prejudice` | phiên người đọc giả | form chấm sao + bình luận hiện |
| not-found | Trang 404 | đường dẫn không tồn tại | — | HTTP 404, "Không tìm thấy trang" |
| privacy | Chính sách quyền riêng tư | `/privacy.html` | — | HTTP 200 |
| admin-guest | Admin khi chưa đăng nhập | `/admin/books` | — | chuyển tới `/api/auth/signin` |
| admin-reader | Admin với người không phải admin | `/admin/books` | phiên người đọc giả | giao diện 404, không lộ dữ liệu (HTTP là 200, xem Known gaps) |
| admin | Tổng quan admin | `/admin` | phiên admin giả | không có chữ lỗi |
| admin-stats | Thống kê | `/admin/stats` | phiên admin giả | biểu đồ hiện |
| admin-books | Quản lý sách, thao tác hàng loạt | `/admin/books` | phiên admin giả | bảng sách |
| admin-book-new | Form thêm sách | `/admin/books/new` | phiên admin giả | form; luồng lưu và kiểm tra hợp lệ: chưa có kịch bản tự động |
| admin-categories | Thể loại và menu | `/admin/categories` | phiên admin giả | danh sách thể loại |
| admin-authors | Tác giả, gộp trùng | `/admin/authors` | phiên admin giả | danh sách tác giả |
| admin-reviews | Kiểm duyệt bình luận | `/admin/reviews` | phiên admin giả | danh sách / trạng thái rỗng |
| epub-route | Proxy EPUB | `/books/<slug>/epub` | seed | e2e `book-reader.spec.ts` (không chụp ảnh) |
| go-link | Đếm lượt tải rồi chuyển hướng | `/books/<slug>/go/<i>` | seed | chỉ số sai → 404 (e2e `reader.spec.ts`) |
