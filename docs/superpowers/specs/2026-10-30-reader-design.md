# OpenBook — Trình đọc sách trong trang (Reader)

Ngày: 2026-10-30 · Trạng thái: chờ duyệt · Mở rộng: `2026-10-29-openbook-design.md`

## 1. Mục tiêu

Trên iPad và máy tính, bấm "Đọc online" mở trình đọc EPUB ngay bên dưới thông tin sách (không chuyển trang), giao diện kiểu Apple Books, lật trang page curl hoặc slide. Trên điện thoại, mời người dùng mở sách trong ứng dụng OpenBook (hoặc tải ứng dụng).

**Tiêu chí thành công**
- iPad/máy tính: trình đọc mở trong trang, phân trang theo màn hình, lật trang curl (mặc định) hoặc slide, hai trang cạnh nhau khi đủ rộng.
- Chỉnh cỡ chữ, nền đọc, kiểu lật; mục lục; thanh tiến độ + kéo nhanh; toàn màn hình; nhớ vị trí đọc và cài đặt.
- Điện thoại: không mở trình đọc; hiện hộp mời mở sách trong app / tải app.
- Nội dung EPUB không thể chạy script hay làm lệch giao diện website.

## 2. Phân loại thiết bị

- **Điện thoại**: `min(window.innerWidth, window.innerHeight) < 600` (đúng cả khi xoay ngang). Còn lại là tablet/máy tính.
- Kiểm tra tại thời điểm bấm nút (client), không dựa vào user-agent.

## 3. Dữ liệu

- `Book.epubUrl: string` (mặc định `""`). Zod: `""` hoặc URL http/https có hostname là tên miền (cùng quy tắc với `links[].url`).
- Form admin thêm ô "Link EPUB (để đọc online)".
- `BookDTO` thêm `epubUrl`.
- Seed: điền `epubUrl` cho sách mẫu — Gutenberg `https://www.gutenberg.org/ebooks/<id>.epub3.images`; Wikisource qua `https://ws-export.wmcloud.org/?lang=vi&format=epub&page=<Tên_trang>`.

## 4. Route `/books/[slug]/epub` (GET)

- Tìm sách theo slug; không có sách hoặc `epubUrl` rỗng → 404.
- `fetch(epubUrl)` với timeout 15 s, `redirect: "follow"`; URL cuối cùng phải là `http:`/`https:`.
- Nguồn trả lỗi / timeout → 502 `"Không tải được sách"`.
- Giới hạn 20 MB: `Content-Length` > 20 MB → 413; nếu không có header, đếm byte khi stream và ngắt khi vượt (client nhận lỗi).
- Trả stream với `Content-Type: application/epub+zip`, `Cache-Control: public, max-age=86400, s-maxage=86400`.
- Không nhận URL từ query; chỉ dùng URL lưu trong DB.
- Gọi route này **không** tăng `downloadCount`; lượt tải được tăng khi mở trình đọc (xem §5).

## 5. Trang chi tiết sách

- Nút **"Đọc online"** chỉ hiện khi sách có `epubUrl`. Các nút link hiện có giữ nguyên.
- Bấm "Đọc online":
  - **Tablet/máy tính**: tải động (`next/dynamic`, `ssr: false`) component `Reader` và hiển thị bên dưới khối thông tin sách, cuộn tới đó; gọi Server Action `recordRead(slug)` tăng `downloadCount` 1 lần mỗi lần mở.
  - **Điện thoại**: mở `AppPromptDialog`.
- Trên điện thoại, các nút link đọc/tải hiện có cũng mở `AppPromptDialog` thay vì đi tới link.

## 6. Hộp mời mở ứng dụng (điện thoại)

- Bìa nhỏ + "Đọc sách này trên ứng dụng OpenBook".
- Nút **"Mở trong ứng dụng"** → `NEXT_PUBLIC_APP_BOOK_URL` với `{slug}` được thay bằng slug sách (ẩn nếu biến rỗng).
- Nút **App Store** (`NEXT_PUBLIC_APP_STORE_URL`) và **Google Play** (`NEXT_PUBLIC_PLAY_STORE_URL`) (ẩn nút có biến rỗng).
- Nút **Đóng**; dùng `<dialog>` gốc (Esc đóng, giữ focus).

## 7. Xử lý EPUB (client)

- Tải `/books/<slug>/epub` → `ArrayBuffer` → giải nén bằng `fflate`.
- Đọc `META-INF/container.xml` → file OPF → manifest + spine (thứ tự chương) bằng `DOMParser`.
- Mục lục: EPUB3 `nav` (`epub:type="toc"`) nếu có, ngược lại EPUB2 `toc.ncx`; thiếu cả hai → dùng spine với tên "Chương N".
- Mỗi chương (XHTML): parse, lọc bằng **DOMPurify** (bỏ `script`, `style`, `link`, `iframe`, `object`, `form`, thuộc tính `on*`, `style` inline), giữ cấu trúc chữ (p, h1–h6, em, strong, blockquote, ul/ol/li, img, a, br, hr, table cơ bản).
- Ảnh: thay `src` bằng blob URL tạo từ file trong EPUB; ảnh không tìm thấy → bỏ.
- Link: link tới file/anchor trong EPUB → nhảy tới chương tương ứng; link ngoài http/https → `target="_blank" rel="noopener noreferrer"`; còn lại → bỏ `href`.
- Lỗi (không phải zip, thiếu OPF, spine rỗng) → trạng thái lỗi của trình đọc.

## 8. Phân trang & lật trang

- Phân trang theo **từng chương**: render chương vào vùng ẩn có kích thước một trang, dùng CSS multi-column (`column-width` = chiều rộng trang) để chia; mỗi cột thành một trang (`div` hiển thị đoạn offset tương ứng).
- Số trang toàn sách = tổng số trang các chương (tính lần lượt; thanh tiến độ cập nhật khi tính xong).
- **Một trang / hai trang**: vùng đọc rộng ≥ 1024 px → hai trang (trang lẻ bên phải như sách), nhỏ hơn → một trang.
- **Page curl**: thư viện `page-flip` (StPageFlip, chế độ HTML), kéo góc bằng chuột/cảm ứng, bấm mép trái/phải; bóng đổ mặc định của thư viện. Chỉ nạp các trang của chương hiện tại ± 1 để nhẹ.
- **Slide**: CSS `transform: translateX` với transition 300 ms; vuốt ngang trên cảm ứng.
- `prefers-reduced-motion` → buộc slide không animation.
- Phím: ← / → lật trang; Esc thoát toàn màn hình.
- Đổi cỡ chữ hoặc kích thước khung → phân trang lại, giữ vị trí (§10).

## 9. Giao diện trình đọc

- Khung cao `85vh`, bo góc lớn, bóng đổ; toàn màn hình qua Fullscreen API.
- **Thanh trên**: ☰ Mục lục · tên chương · Aa · ⛶ · ✕. Tự mờ sau 3 s không tương tác; hiện lại khi rê chuột/chạm giữa trang.
- **Vùng trang**: chữ serif (Playfair cho tiêu đề, `Georgia`/serif cho nội dung), căn đều, `hyphens: auto`, lề rộng; hai trang có rãnh gáy giữa.
- **Thanh dưới**: "Trang X / Y · <tên chương>" + thanh kéo (`<input type="range">`).
- **Bảng Aa**: cỡ chữ A−/A+ (5 mức: 16, 18, 20, 22, 24 px; mặc định 18); nền **Trắng** (`#ffffff`/`#1f1f1f`), **Sepia** (`#f6efe0`/`#5b4636`), **Tối** (`#1c1c1e`/`#e5e5e7`); kiểu lật **Cong trang / Trượt**.
- **Mục lục**: ngăn kéo trái, tô đậm chương hiện tại.
- **Đang tải**: khung trang mờ + "Đang mở sách…". **Lỗi**: "Không mở được sách" + **Thử lại** + link `epubUrl` gốc.
- Trợ năng: nút có `aria-label` tiếng Việt, điều khiển bằng bàn phím, nội dung là chữ thật; thông báo trang hiện tại qua `aria-live="polite"`.

## 10. Ghi nhớ (localStorage)

- `openbook:reader:settings` → `{ fontSize, theme, flip }`.
- `openbook:reader:pos:<slug>` → `{ chapter: number, progress: number }` (progress 0–1 trong chương).
- Mở lại → về chương + trang gần nhất với progress. Giá trị không hợp lệ (chương vượt phạm vi, JSON hỏng) → bỏ qua, mở từ đầu.

## 11. Bảo mật

- Proxy chỉ dùng URL trong DB, đã kiểm tra http/https + hostname tên miền (không IP/localhost); giới hạn 20 MB, timeout 15 s.
- Mọi HTML từ EPUB đi qua DOMPurify trước khi vào DOM; CSS của EPUB bị bỏ.
- Link ngoài trong sách: `noopener noreferrer`.

## 12. Kiểm thử

- **Vitest**: đọc OPF/spine/TOC (EPUB3 nav, EPUB2 ncx, thiếu TOC), đường dẫn tương đối, file hỏng; lọc HTML (`<script>`, `onerror`, `javascript:` bị xoá); lưu/đọc vị trí và cài đặt (gồm JSON hỏng); `isPhoneViewport`; route epub: 404 (không có `epubUrl`), 413 (quá lớn), 502 (nguồn lỗi), 200 stream; `recordRead` tăng lượt tải; schema `epubUrl`.
- **Playwright**: EPUB mẫu nhỏ trong `public/test-fixtures/`; seed ghi thẳng DB (không qua Zod) một sách có `epubUrl` = `http://localhost:<port>/test-fixtures/<file>.epub`; desktop: mở trình đọc, → lật trang, đổi nền Tối, reload + mở lại đúng trang; điện thoại: bấm "Đọc online" → hộp mời mở app, không có trình đọc.

## 13. Phụ thuộc mới

`fflate` (giải nén), `dompurify` (lọc HTML), `page-flip` (lật trang curl; bản cuối 2022, MIT — rủi ro: không còn bảo trì; nếu lỗi với React 19 thì bọc trong `useEffect` thuần DOM hoặc thay bằng hiệu ứng CSS 3D).

## 14. Ngoài phạm vi

Bookmark, highlight, ghi chú; tìm trong sách; đồng bộ vị trí giữa thiết bị; PDF; đọc offline; upload EPUB.
