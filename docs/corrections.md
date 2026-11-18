# Corrections → checks

Mỗi lần phải sửa agent, ghi lại kiểm tra tự động ngăn lỗi đó lặp lại.
Kind: `type` · `lint` · `test` · `ci` · `removed-pattern` · `soft` (dòng trong AGENTS.md, phương án cuối).

| Date | Mistake | Check that prevents it | Kind | Status |
|---|---|---|---|---|
| 2026-11-18 | Seed hoặc e2e chạy khi `MONGODB_URI` trỏ tới Atlas sẽ xoá dữ liệu thật | `scripts/verify.mjs` tự tạo DB trong bộ nhớ và từ chối URI không phải `127.0.0.1` | removed-pattern | done |
| 2026-11-18 | `npm run seed` vẫn xoá DB của `.env.local` nếu chạy trực tiếp | Seed từ chối host không phải local trừ khi có cờ `--allow-remote` | test | todo |
