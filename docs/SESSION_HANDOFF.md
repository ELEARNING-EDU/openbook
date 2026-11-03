# Session handoff

Cập nhật: 2026-11-03

## Trạng thái
- Nhánh `web`. Đã triển khai đủ 7 task của `docs/superpowers/plans/2026-10-29-openbook.md` (spec: `docs/superpowers/specs/2026-10-29-openbook-design.md`) và đủ 7 task của trình đọc EPUB (`docs/superpowers/plans/2026-10-30-reader.md`, spec: `docs/superpowers/specs/2026-10-30-reader-design.md`).
- `TODO.md` (yêu cầu gốc) chưa commit — để nguyên.
- Chưa push, chưa deploy. Chưa thử đăng nhập Google thật (cần OAuth client của người dùng).

## Kiểm chứng (2026-11-03, máy local)
- `npm test` (`npx vitest run`): 13 file, 75/75 pass.
- `npm run e2e`: 18 pass, 2 skip (mỗi test desktop/mobile tự skip ở project còn lại), 0 fail; DB MongoDB in-memory `127.0.0.1:27999`, seed bằng `SEED_BASE_URL=http://localhost:3200 npm run seed`.
- `npx tsc --noEmit`: pass. `npm run lint`: pass. `npm run build`: pass.
- (2026-10-30) Luồng admin/review đã kiểm tra bằng script trình duyệt với cookie JWT tự tạo: pass.

## Tính năng trình đọc sách (Reader)
- Đã có code và e2e; spec `docs/superpowers/specs/2026-10-30-reader-design.md`.
- Máy tính/iPad: nút "Đọc online" mở trình đọc trong trang (`section#reader`) — lật trang curl/slide, đổi cỡ chữ/nền, mục lục, nhớ vị trí. Điện thoại: hiện hộp mời mở sách trong ứng dụng OpenBook.
- E2E: `e2e/book-reader.spec.ts` (mở đọc, lật trang, đổi nền Tối, reload giữ vị trí; mời mở app trên mobile; route EPUB 200/404). EPUB mẫu: `public/test-fixtures/sample.epub`, seed kèm sách `sach-mau-openbook` bằng `SEED_BASE_URL=http://localhost:3200 npm run seed`.
- Kế tiếp: điền `.env.local` thật và `NEXT_PUBLIC_APP_*` nếu muốn, rồi `npm run dev` và thử đăng nhập Google.

## Bước tiếp theo (ứng dụng chính)
1. Người dùng điền `.env.local` thật (Atlas + Google OAuth), `npm run seed`, `npm run dev`, thử đăng nhập Google.
2. Deploy Vercel theo README.
