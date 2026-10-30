# Session handoff

Cập nhật: 2026-10-30

## Trạng thái
- Nhánh `web`. Đã triển khai đủ 7 task của `docs/superpowers/plans/2026-10-29-openbook.md` (spec: `docs/superpowers/specs/2026-10-29-openbook-design.md`).
- `TODO.md` (yêu cầu gốc) chưa commit — để nguyên.
- Chưa push, chưa deploy. Chưa thử đăng nhập Google thật (cần OAuth client của người dùng).

## Kiểm chứng (2026-10-30)
- `npm test`: 8 file, 49/49 pass.
- `npm run e2e`: 14/14 pass (mobile 390px + desktop 1280px), DB MongoDB in-memory đã seed.
- `npm run lint`, `npm run build` (kể cả khi không có env): pass.
- Luồng admin/review kiểm tra bằng script trình duyệt với cookie JWT tự tạo: pass.

## Tính năng đang làm: trình đọc sách (Reader)
- Spec: `docs/superpowers/specs/2026-10-30-reader-design.md` (chờ người dùng duyệt). Chưa có code.
- Kế tiếp: duyệt spec → `superpowers:writing-plans`.

## Bước tiếp theo (ứng dụng chính)
1. Người dùng điền `.env.local` thật (Atlas + Google OAuth), `npm run seed`, `npm run dev`, thử đăng nhập Google.
2. Deploy Vercel theo README.
