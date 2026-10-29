# OpenBook — Thiết kế website thư viện sách mở

Ngày: 2026-10-29 · Trạng thái: chờ duyệt

## 1. Mục tiêu

Website thư viện/kho sách mở: người đọc xem thông tin sách và mở link ngoài để đọc/tải miễn phí; người đọc có tài khoản để đánh giá và bình luận; admin quản lý sách, thể loại, bình luận.

**Tiêu chí thành công**
- Người đọc tìm, lọc, xem chi tiết sách và mở link đọc/tải trên mọi kích thước màn hình (≥ 360px).
- Người đọc đăng nhập Google, chấm 1–5 sao và bình luận; điểm trung bình cập nhật ngay.
- Admin thêm/sửa/xóa sách và thể loại, ẩn/xóa bình luận; người không phải admin không truy cập được chức năng admin.
- Giao diện ấm áp cổ điển, có điểm nhấn sinh động; deploy chạy trên Vercel.

**Giả định**: giao diện tiếng Việt; ít admin; không bán hàng/thanh toán; website không lưu file sách hay ảnh.

## 2. Công nghệ

- Next.js (App Router, TypeScript), Server Components + Server Actions
- MongoDB Atlas qua Mongoose
- Auth.js (NextAuth v5), provider Google duy nhất, MongoDB adapter
- Tailwind CSS; font qua `next/font` (Playfair Display, Be Vietnam Pro)
- Zod kiểm tra dữ liệu
- Vitest (logic), Playwright (E2E)
- Deploy: Vercel

## 3. Trang

**Công khai**
| Route | Nội dung |
|---|---|
| `/` | Banner, sách nổi bật, sách mới, danh sách thể loại |
| `/books` | Danh sách sách; tìm theo tên/tác giả (`?q=`), lọc thể loại (`?category=`), phân trang (`?page=`, 20 sách/trang) |
| `/books/[slug]` | Bìa, tiêu đề, tác giả, mô tả, thể loại, nút link đọc/tải, điểm trung bình, form đánh giá (khi đăng nhập), danh sách bình luận không bị ẩn |
| `/categories/[slug]` | Sách thuộc thể loại, phân trang |

**Admin** (`/admin/*`, chỉ admin)
| Route | Nội dung |
|---|---|
| `/admin` | Chuyển hướng tới `/admin/books` |
| `/admin/books` | Bảng sách, tìm kiếm, nút thêm/sửa/xóa, bật/tắt nổi bật |
| `/admin/books/new`, `/admin/books/[id]` | Form sách, xem trước ảnh bìa |
| `/admin/categories` | Thêm/sửa/xóa thể loại |
| `/admin/reviews` | Danh sách review mới nhất, ẩn/hiện/xóa |

## 4. Dữ liệu (MongoDB)

**Book**: `title` (bắt buộc), `slug` (unique, sinh từ title bỏ dấu), `author` (bắt buộc), `description`, `coverUrl`, `links: [{label, url}]` (≥ 1), `categoryIds: ObjectId[]`, `featured: boolean`, `ratingAvg: number`, `ratingCount: number`, `createdAt`, `updatedAt`.

**Category**: `name` (bắt buộc), `slug` (unique).

**Review**: `bookId`, `userId`, `rating` (1–5, số nguyên), `comment` (≤ 2000 ký tự, có thể rỗng), `hidden: boolean`, `createdAt`, `updatedAt`. Unique index `(bookId, userId)`; gửi lại thì cập nhật review cũ.

**User / Account / Session**: do Auth.js adapter quản lý.

**Quy tắc**
- Slug trùng → thêm hậu tố `-2`, `-3`, …
- Sau mỗi thao tác tạo/sửa/xóa/ẩn/hiện review → tính lại `ratingAvg`, `ratingCount` của sách từ các review không bị ẩn.
- Xóa thể loại → gỡ id khỏi `categoryIds` của mọi sách.
- Xóa sách → xóa toàn bộ review của sách.

## 5. Xác thực & phân quyền

- Đăng nhập: Google OAuth qua Auth.js.
- Admin: email (so sánh không phân biệt hoa thường) nằm trong biến môi trường `ADMIN_EMAILS` (danh sách phân cách bằng dấu phẩy).
- Hàm `isAdmin(email)` dùng chung; middleware chặn `/admin/*`, và **mọi** Server Action admin tự kiểm tra lại quyền phía server.
- Server Action review yêu cầu đã đăng nhập.

## 6. Giao diện

**Màu**: nền kem `#FBF6EC`, chữ nâu `#3B2A1A`, nhấn đỏ gạch `#C2410C`, phụ xanh rêu `#4D7C0F`; gradient nhẹ cam đào → hồng đất cho banner và nút chính. Độ tương phản chữ/nền đạt WCAG AA.

**Chữ**: Playfair Display (tiêu đề), Be Vietnam Pro (nội dung).

**Thành phần**
- Thẻ sách: bo góc lớn, bìa 2:3, hover nhấc lên + nghiêng nhẹ, hiển thị sao.
- Header: logo, ô tìm kiếm, menu thể loại, avatar/nút "Đăng nhập Google"; mobile dùng hamburger mở drawer.
- Hiệu ứng hiện dần khi cuộn, chỉ dùng CSS/Tailwind; tắt khi `prefers-reduced-motion`.
- Bìa thiếu/lỗi → ảnh placeholder theo màu chủ đề.

**Responsive (mobile-first)**
- Lưới sách: 2 cột mobile, 3 tablet (`md`), 4 (`lg`), 5 (`xl`).
- Chi tiết sách: mobile xếp dọc; desktop bìa trái, thông tin phải.
- Admin: desktop sidebar + bảng; mobile thanh điều hướng đáy, bảng hiển thị dạng thẻ.

**Admin**: cùng tông màu, bố cục gọn; form báo lỗi theo từng trường; toast khi lưu; hộp xác nhận trước khi xóa.

## 7. Bảo mật

- Zod kiểm tra mọi input Server Action. URL (`coverUrl`, `links[].url`) chỉ chấp nhận `http:`/`https:`.
- Bình luận render dạng text thuần (React escape), không dùng `dangerouslySetInnerHTML`.
- Ảnh bìa từ domain bất kỳ → dùng `<img>` thường (không cấu hình `next/image` remotePatterns cho mọi domain).
- Secret chỉ trong biến môi trường: `MONGODB_URI`, `AUTH_SECRET`, `AUTH_GOOGLE_ID`, `AUTH_GOOGLE_SECRET`, `ADMIN_EMAILS`. Có `.env.example`, `.env*.local` nằm trong `.gitignore`.

## 8. Xử lý lỗi

- `not-found.tsx` cho sách/thể loại không tồn tại; `error.tsx` thân thiện khi lỗi DB.
- Server Action trả `{ ok: false, errors }` để form hiển thị lỗi theo trường; không lộ stack trace.
- Kết nối Mongoose được cache theo module (tránh mở nhiều kết nối trên serverless).

## 9. Kiểm thử

- **Vitest**: schema Zod (URL hợp lệ/không hợp lệ), `slugify` (tiếng Việt có dấu), tính điểm trung bình, `isAdmin`.
- **Playwright**: trang chủ → danh sách → chi tiết sách, ở viewport mobile (390px) và desktop (1280px); `/admin` khi chưa đăng nhập bị chuyển hướng.
- Script `npm run seed`: tạo ~12 sách public domain mẫu và vài thể loại.

## 10. Ngoài phạm vi

Đa ngôn ngữ, upload file/ảnh, thống kê, email thông báo, đăng nhập bằng mật khẩu, tìm kiếm full-text nâng cao (dùng regex trên title/author), dark mode, danh sách yêu thích.
