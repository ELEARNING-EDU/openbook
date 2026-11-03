# Session handoff

Cập nhật: 2026-11-03

## Trạng thái
- Nhánh `web`, remote `openbook-web`. Code đã push; chưa deploy.
- Đã xong 7 task của ứng dụng chính (`docs/superpowers/plans/2026-10-29-openbook.md`) và 7 task của trình đọc EPUB (`docs/superpowers/plans/2026-10-30-reader.md`).
- `AGENTS.md`, `CLAUDE.md`, `TODO.md` có thay đổi hoặc chưa được track, chủ ý để ngoài commit. Người dùng chưa quyết định có commit không.

## Môi trường local (`.env.local`, đã kiểm tra 2026-11-03, không ghi giá trị)
- `MONGODB_URI` trỏ tới Atlas, DB `openbook`: kết nối được. Có 4 categories, 11 books, 0 reviews, và một collection `admin` rỗng mà code không dùng.
- `npm run seed` XOÁ toàn bộ sách, thể loại, bình luận. DB Atlas giờ đã có dữ liệu, đừng seed lại nếu chưa hỏi người dùng. Không đặt `SEED_BASE_URL` khi seed lên Atlas.
- `AUTH_GOOGLE_ID`, `AUTH_GOOGLE_SECRET` đúng định dạng của Google. Chưa xác nhận đăng nhập thật.
- Chưa có `NEXT_PUBLIC_APP_*`, nên các nút mở app và store chưa hiện.

## Kiểm chứng gần nhất (2026-11-03, trước commit `4fec6ae`, cùng code hiện tại)
- `npx tsc --noEmit`, `npm run lint`: pass.
- `npm test`: 13 file, 83/83 pass.
- `npm run e2e -- --repeat-each=2`: 46 pass, 10 skip (test chỉ dành cho một loại màn hình), 0 fail. Build chạy trong bước này.
- E2E cần DB local: đổi tạm `MONGODB_URI` sang `127.0.0.1:27999`, chạy `node .superpowers/devdb.mjs`, rồi `SEED_BASE_URL=http://localhost:3200 npm run seed`. KHÔNG chạy e2e seed khi `MONGODB_URI` còn trỏ tới Atlas.

## Sách seed thật (thử trên trình duyệt 2026-11-03, 1280×800)
- Mở và lật trang được: Pride and Prejudice, Alice, On the Origin of Species, Truyện Kiều, Chinh phụ ngâm.
- Chưa xác nhận lật trang: Sherlock Holmes, Frankenstein, The Art of War, Meditations, Tao Te Ching, Lục Vân Tiên.
- Sách Wikisource mất 10–12 giây để mở, trong khi proxy chỉ chờ 15 giây. Chưa đo trên Vercel.
- "Số đỏ" đã bỏ khỏi seed vì link EPUB và link Wikisource đều 404.

## Còn tồn
- Mức thấp, chưa làm: thanh dưới trình đọc khác định dạng spec §9 và ẩn tên chương ở màn nhỏ; Esc khi toàn màn hình chưa xử lý tường minh (trình duyệt thường tự thoát); e2e chưa tự seed.
- Cần cập nhật spec: code dùng JWT session, spec ghi MongoDB adapter.
- Proxy EPUB không phân giải DNS, tên miền công khai trỏ về IP nội bộ vẫn lọt. Chấp nhận được khi chỉ admin đặt link.
- `src/app/admin/error.tsx` mới qua build, chưa kích hoạt thử.
- Luồng admin/review kiểm tra lần cuối ngày 2026-10-30, chưa chạy lại sau các thay đổi mới.

## Giao việc cho subagent
- Dùng `.opencode/agents/worker.md`, model `opencode-go/deepseek-v4.1-flash`: `opencode run --standalone --agent worker --format json "<brief>" </dev/null`. Thiếu `</dev/null` thì lệnh bị treo.
- Worker làm, Claude QA, review và test. Quy tắc chặn commit, push, seed, e2e nằm trong file agent. Chưa thử worker sửa file thật.

## Bước tiếp theo
1. Người dùng chạy `npm run dev`, đăng nhập Google tại <http://localhost:3000>, vào `/admin` bằng email trong `ADMIN_EMAILS`.
2. Deploy Vercel theo README, thêm origin và redirect URI của domain vào OAuth Client, rồi thử mở sách Wikisource trên production.
