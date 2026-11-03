# OpenBook

Thư viện sách mở: giới thiệu sách kèm link đọc/tải miễn phí, người đọc đăng nhập Google để chấm điểm và bình luận, admin quản lý sách, thể loại và bình luận.

- Trang chủ: slider sách tải nhiều nhất, sách nổi bật, sách mới, mỗi thể loại một hàng sách.
- Trang thể loại: slider sách tải nhiều nhất của thể loại + toàn bộ sách, phân trang.
- Lượt tải được đếm khi người đọc bấm link (`/books/<slug>/go/<i>` → chuyển tới link thật).
- Admin (`/admin`): chỉ các email trong `ADMIN_EMAILS`.

Công nghệ: Next.js 16 (App Router, Cache Components), MongoDB + Mongoose, Auth.js (Google), Tailwind CSS, Zod, Vitest, Playwright.

Thiết kế: `docs/superpowers/specs/2026-10-29-openbook-design.md`.

## Chạy trên máy

1. `npm install`
2. `cp .env.example .env.local` rồi điền:
   - `MONGODB_URI` — chuỗi kết nối MongoDB Atlas (hoặc MongoDB local).
   - `AUTH_SECRET` — tạo bằng `npx auth secret` hoặc `openssl rand -base64 32`.
   - `AUTH_GOOGLE_ID`, `AUTH_GOOGLE_SECRET` — tạo OAuth Client (loại *Web application*) tại Google Cloud Console → APIs & Services → Credentials. Thêm *Authorized redirect URI*: `http://localhost:3000/api/auth/callback/google`.
   - `ADMIN_EMAILS` — email Google của admin, phân cách bằng dấu phẩy.
3. `npm run seed` — tạo 4 thể loại và 12 sách mẫu (**xoá** sách, thể loại, bình luận hiện có).
4. `npm run dev` → <http://localhost:3000>

## Đọc online

- Sách có `epubUrl` (URL `http`/`https`) hiện nút **"Đọc online"**. Máy tính/iPad mở trình đọc EPUB ngay trong trang; điện thoại (`min(innerWidth, innerHeight) < 600`) không mở trình đọc mà hiện hộp mời mở sách trong ứng dụng OpenBook.
- Đặt `NEXT_PUBLIC_APP_BOOK_URL` (dùng `{slug}` được thay bằng slug sách), `NEXT_PUBLIC_APP_STORE_URL`, `NEXT_PUBLIC_PLAY_STORE_URL` để hiện nút **Mở trong ứng dụng**, **App Store**, **Google Play**; biến rỗng thì ẩn nút tương ứng. Các biến `NEXT_PUBLIC_*` được nhúng lúc build nên phải build lại sau khi đổi.
- Sách mẫu "Sách mẫu OpenBook" dùng cho e2e/demo chỉ được seed khi có `SEED_BASE_URL`: `SEED_BASE_URL=http://localhost:3200 npm run seed`.

## Kiểm thử

- `npm test` — unit/integration (Vitest, MongoDB chạy trong bộ nhớ, không cần Atlas).
- `npm run e2e` — Playwright ở khung mobile 390px và desktop 1280px. Cần `.env.local` có `AUTH_TRUST_HOST=true` và DB đã seed bằng `SEED_BASE_URL=http://localhost:3200 npm run seed` (thêm sách mẫu có EPUB). Lần đầu chạy `npx playwright install chromium`.
- `npm run lint`, `npm run build`.

## Deploy lên Vercel

1. MongoDB Atlas → *Network Access* → cho phép `0.0.0.0/0` (Vercel không có IP cố định). Tạo database user và lấy chuỗi kết nối.
2. Vercel → *Add New Project* → import repo. Nếu app nằm trong thư mục con, đặt *Root Directory* tương ứng.
3. *Environment Variables*: `MONGODB_URI`, `AUTH_SECRET`, `AUTH_GOOGLE_ID`, `AUTH_GOOGLE_SECRET`, `ADMIN_EMAILS` (không cần `AUTH_TRUST_HOST` trên Vercel).
4. Deploy. Sau đó thêm redirect URI `https://<domain-của-bạn>/api/auth/callback/google` vào OAuth Client trên Google Cloud.
5. Muốn có dữ liệu mẫu trên production: chạy `MONGODB_URI="<chuỗi Atlas>" npm run seed` từ máy của bạn.
