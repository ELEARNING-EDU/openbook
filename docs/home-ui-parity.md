# Home parity — đọc sách, thành tích, kệ sách, biểu đồ tuần

Nguồn: bản iOS đang chạy trên simulator (nhánh `ios`, phần Home mới **chưa commit**, HEAD `120aed3`).
Đối chiếu UI/UX hiện tại của Android và làm cho khớp hành vi dưới đây. Không copy code iOS;
dùng Compose theo cấu trúc sẵn có của repo.

**Trước khi sửa:** đọc `PROJECT_STATE.md`, `SESSION_HANDOFF.md`, `TASKS.md`, chạy `git status`/`git diff`.
Repo đang có thay đổi chưa commit (`315c8e6` + dirty); **không xoá/ghi đè**. Không commit/push khi chưa được duyệt.

**Phạm vi:** Home, Library (menu + nhãn), dữ liệu đọc/thành tích. Không đổi reader, TTS, import EPUB, player.

---

## Phần 0 — Dữ liệu phải có trước (nếu Android chưa có)

Home dưới đây cần 3 nguồn dữ liệu. Nếu repo chưa có, làm phần này trước; nếu đã có thì chỉ cần đối chiếu.

### 0.1. Thời gian đọc theo ngày
- Ghi khi màn hình đọc đang mở và app foreground. Rời app/khoá máy → dừng.
- TTS/nghe tính vào bucket riêng `listeningSeconds`, nhưng **tổng = đọc + nghe**.
- Không lật trang và TTS không chạy quá **5 phút** → ngừng cộng; lật trang/đọc tiếp thì cộng lại.
- Phiên qua nửa đêm chia cho hai ngày theo giờ máy.
- Lưu local (DataStore/Room/JSON tuỳ repo), khoá ngày `yyyy-MM-dd`, không cần server.
- Ghi thêm: phiên dài nhất, số phiên.

### 0.2. Mục tiêu + chuỗi + tổng
- Mục tiêu ngày mặc định **5 phút**, đổi được 1/5/10/15/30/60/120 (Settings).
- "Ngày đọc" = ngày có tổng ≥ mục tiêu; streak = số ngày liên tiếp đạt mục tiêu
  (hôm nay chưa đạt thì tính tới hôm qua). Kỷ lục streak tính từ dữ liệu ngày.
- Summary cần: hôm nay/tuần/tháng/năm (phút), hôm nay đã đạt chưa, streak hiện tại/kỷ lục/tổng
  ngày đọc, phiên dài nhất, ngày đọc nhiều nhất.
- Đổi mục tiêu → tính lại streak/tổng ngày từ dữ liệu, không lưu cứng.

### 0.3. Trạng thái "đã đọc xong"
- Thêm `finishedAt` vào metadata sách (local, optional, tương thích dữ liệu cũ).
- Thư viện: menu từng sách có **"Đánh dấu đã đọc xong"** / **"Bỏ đánh dấu"**.
- Sách xong: hiện nhãn accent "Đã đọc xong" thay cho %, rời Home "Đang đọc", vẫn giữ vị trí đọc;
  bấm mở vẫn đọc tiếp được.
- Không tự đánh dấu khi tới trang cuối.

---

## Phần 1 — Cấu trúc Home

Thứ tự từ trên xuống:

```
<tiêu đề lớn "Home">
THÀNH TÍCH
ĐANG ĐỌC
MỚI THÊM
GỢI Ý CHO BẠN
ĐỌC XONG GẦN ĐÂY
TUẦN NÀY
```

- **ĐANG ĐỌC**: tối đa **5**, sách `0 < fraction < 1`, chưa đánh dấu xong, cập nhật gần nhất trước.
- **MỚI THÊM**: tối đa **5**, sách chưa đọc (không có vị trí hoặc fraction ≤ 0), chưa xong,
  `importedAt` mới nhất trước.
- **GỢI Ý CHO BẠN**: tối đa **5**, sách chưa đọc **cùng tác giả** với 3 sách vừa đọc xong/đang đọc
  gần nhất; loại trùng với MỚI THÊM và ĐỌC XONG; **cần ≥ 2 cuốn** mới hiện; không có thì ẩn hẳn
  (không gợi ý ngẫu nhiên).
  - So tác giả: bỏ dấu, không phân biệt hoa/thường, gộp khoảng trắng
    ("Nguyễn Minh An" = "Nguyen Minh An").
- **ĐỌC XONG GẦN ĐÂY**: tối đa **5**, `finishedAt` trong **12 tháng**, mới nhất trước.
- **Ẩn từng hàng khi rỗng.**
- **Thư viện < 6 cuốn**: chỉ hiện **THÀNH TÍCH + ĐANG ĐỌC**, ẩn cả 3 hàng dưới và biểu đồ.
- Không thêm nút sửa/xoá sách ở Home; Thư viện vẫn là nơi quản lý.

---

## Phần 2 — Thẻ THÀNH TÍCH

Một card ngang, nền `cardFill`, bo 16, padding 14×12, bấm mở sheet "Thành tích đọc".

- Trái: vòng tròn 44dp, track ink 14%, cung accent theo `hôm nay / mục tiêu` (clamp 1),
  giữa là dấu ✓ khi đạt, ngược lại "%".
- Giữa:
  - Dòng 1 (17sp serif semibold): "22 phút hôm nay".
  - Dòng 2 (12sp, inkSoft): "Mục tiêu 5 phút · đã đạt" hoặc "Còn 3 phút nữa".
  - Dòng 3 (12sp rounded): "Tuần 22 phút · Tháng 4 giờ 24 phút · Năm 19 giờ 24 phút".
- Phải: 🔥 + "Chuỗi 4 ngày" (hoặc "Chưa có chuỗi"), 11sp semibold.
- "Còn N phút" tính từ **giây thật**, làm tròn lên, không dựa vào số phút đã làm tròn.
- Sheet (nếu đã có/không bắt buộc trong prompt này): 4 ô Hôm nay/Tuần/Tháng/Năm, chuỗi hiện
  tại/kỷ lục/ngày đọc, phiên dài nhất, ngày đọc nhiều nhất, sách xong năm nay.
  **Chưa có biểu đồ trong sheet.**
- Thẻ cập nhật khi mở lại app/qua nửa đêm (không đóng băng số cũ).

---

## Phần 3 — ĐANG ĐỌC (giữ kiểu card, không đổi)

- Card ngang 300dp, bìa 72, nền cardFill, bo 14, viền ink 10%.
- Tên 17sp serif semibold (2 dòng), tác giả 12sp, tiến độ "Trang 4/12 · 35%" 11sp,
  dòng accent "Đọc tiếp"/"Continue reading" 12sp.
- Cuộn ngang, tối đa 5.

---

## Phần 4 — Ba hàng sách (MỚI THÊM / GỢI Ý / ĐỌC XONG)

**Một khung chung cho cả hàng**, không phải mỗi sách một card:

- Khung: nền `cardFill`, viền ink 10%, bo 16. **Lề trái 20dp** (thấy góc bo trái).
  **Mép phải tràn ra ngoài màn hình 20dp** (góc phải nằm ngoài, không thấy).
  Gợi ý Compose: `BoxWithConstraints` → `Row(Modifier.offset(x = 20.dp).width(maxWidth - 20.dp + 20.dp))`
  và để parent clip theo màn hình; hoặc `Layout` tương đương.
- Bên trong: cuộn ngang, content padding ngang **14dp**, dọc **12dp**, spacing **16dp**,
  tối đa 5 sách.
- Mỗi sách (150dp rộng, không nền riêng):
  - Bìa **96dp**, tỉ lệ 0.68, bo 10, đổ bóng nhẹ như hiện tại.
  - Tên: **17sp serif semibold, 1 dòng**, cửa sổ ~150dp (khoảng 5–6 chữ); nếu dài hơn thì
    **chạy chữ** (marquee) chứ không cắt — tốc độ ~26dp/s, khoảng lặp 40dp, chỉ chạy khi tràn,
    dừng khi không tràn. TalkBack đọc full tên.
  - Tác giả: 12sp, inkSoft, 1 dòng.
  - Phụ đề 11sp rounded inkSoft: MỚI THÊM = "Thêm thg 10" / "Added Oct";
    ĐỌC XONG = "Xong thg 10" / "Finished Oct"; GỢI Ý = "Cùng tác giả" / "Same author".
- Tiêu đề hàng nằm **ngoài khung**, 14sp, uppercase, letterSpacing 1.0, inkSoft, lề 20dp,
  cách khung 10dp.
- Không dùng nền/viền riêng cho từng sách.

---

## Phần 5 — TUẦN NÀY (biểu đồ ở đáy Home)

- Sát cuối nội dung, trên thanh tab; **chỉ hiện khi tuần hiện tại có ≥ 1 phút**; cả tuần 0 → ẩn.
- Header: "TUẦN NÀY" (14sp uppercase) + tổng tuần bên phải (11sp).
- 7 cột **thứ Hai → Chủ nhật** của tuần hiện tại; ngày chưa tới = 0.
- Vùng cột cao **88dp**; mỗi cột rộng **18dp** (căn giữa ô), bo 3; hôm nay màu accent,
  ngày khác inkSoft 45%; ngày 0 vẫn có vạch mảnh 2dp.
- Vạch mục tiêu: đường accent 35% ngang suốt biểu đồ, cao theo `goal / max(goal, phút lớn nhất)`.
- Nhãn thứ 11sp dưới cột; hôm nay semibold + accent.
- Accessibility: mỗi cột đọc "Thứ Hai, 22 phút"; container đọc tiêu đề.

---

## Phần 6 — Tiêu đề app, gear, scrollbar

- **Home**: tiêu đề lớn "Home"/"Trang chủ" **30sp serif semibold**, lề 20dp, cách safe-area
  trên 8dp; cuộn đi cùng nội dung (không pinned).
- **Library**: tiêu đề lớn "Library"/"Thư viện" cùng kiểu, cùng vị trí; các nút sắp xếp/menu
  giữ ở góc phải.
- **Gear Home** (mở Settings): chỉ hiện khi **đang ở top** của scroll
  (`firstVisibleItemIndex == 0 && offset == 0`); cuộn xuống thì fade ẩn (200ms), về top hiện lại.
  Nút 44dp, nền cardFill, icon ink.
- **Bỏ thanh cuộn dọc** nếu app đang vẽ scrollbar (Compose mặc định không có; nếu có custom
  indicator thì tắt).
- Khoảng cách giữa các khối: **24dp**; tiêu đề khối cách nội dung 10dp.

---

## Phần 7 — Ngôn ngữ & accessibility

- Chuỗi mới đủ **vi + en**, key khớp nhau:
  `home.title`, `library.title`, `home.stats.*`, `home.new.title`, `home.suggestions.title`,
  `home.suggestions.reason`, `home.finished.title`, `home.added.month`, `home.finished.month`,
  `home.weekly.*`, `home.book.a11y`, `duration.minutes/hours/hoursMinutes`,
  `library.markFinished/unmarkFinished/finished`, `stats.*`, `settings.reading.*`.
- Số phút dùng một helper chung: < 60 → "N phút"/"N min"; đủ giờ → "N giờ"/"N hr";
  còn lại "N giờ M phút"/"N hr M min".
- TalkBack:
  - Thẻ thành tích: một câu đầy đủ ("Hôm nay 22 phút, mục tiêu 5 phút, đã đạt. Chuỗi 4 ngày. Tuần…").
  - Sách trong hàng: "Tên, tác giả".
  - Vòng mục tiêu là trang trí (không đọc riêng).
  - Chart: mỗi cột đọc ngày + phút.
- Màu lấy từ palette của app (`shell`), không dùng màu Material mặc định.

---

## Phần 8 — Kiểm thử & acceptance

- Unit test:
  - Lọc/sắp xếp/cắt 5 của ĐANG ĐỌC, MỚI THÊM, ĐỌC XONG.
  - Luật gợi ý: chỉ cùng tác giả, loại trùng, cần ≥ 2.
  - Ngưỡng < 6 sách → không hiện hàng phụ/biểu đồ.
  - Tuần: thứ Hai đầu tuần, 7 điểm, cờ hôm nay, ẩn khi 0 phút.
  - `finishedAt`: đánh dấu/bỏ, sách xong rời ĐANG ĐỌC, năm thống kê.
  - Day split qua nửa đêm, idle 5 phút, streak/đổi mục tiêu (nếu làm Phần 0).
- Kiểm tra UI:
  - Home thư viện ít (<6) → chỉ thẻ + đang đọc.
  - Home đủ 11 sách demo → 5 đang đọc, 5 mới thêm, 2 gợi ý, 3 đọc xong, chart 22 phút.
  - Cuộn: gear ẩn; về top: gear hiện.
  - Khung hàng: trái 20dp, phải tràn mép; không có card riêng từng sách.
  - Tên dài chạy chữ; tên ngắn đứng yên.
  - Chart: cột mảnh, cao, vạch mục tiêu đúng.
  - vi/en, sáng/tối, TalkBack.
- Build + test xanh; ghi rõ lệnh, exit code, số test.
- Báo lại: files changed, kết quả test, ảnh chụp Home/Library, phần chưa làm.

## Acceptance checklist
- [ ] Home đúng thứ tự: tiêu đề → thành tích → đang đọc → mới thêm → gợi ý → đọc xong → tuần này.
- [ ] <6 sách chỉ còn thành tích + đang đọc.
- [ ] Mỗi hàng tối đa 5; hàng rỗng ẩn.
- [ ] Gợi ý cùng tác giả, loại trùng, cần ≥2.
- [ ] Ba hàng ngoài READING: một khung chung/hàng, trái 20dp, phải tràn mép, tên 17sp chạy chữ.
- [ ] Chart 88dp, cột 18dp, thứ Hai→CN, ẩn khi tuần 0.
- [ ] Tiêu đề Home/Library hiện; gear chỉ hiện ở top.
- [ ] Sách đánh dấu xong có nhãn, rời Home, bỏ đánh dấu được.
- [ ] vi/en đủ key, TalkBack đọc đúng.
- [ ] Build + test xanh, chưa commit.

---

*Bộ dữ liệu demo để so sánh: 3 đang đọc (35%, 92%, 38%), 5 mới thêm, 2 gợi ý cùng tác giả,
3 đọc xong trong tháng, tuần 22 phút. Dựng bằng dữ liệu mẫu trong test, không cần EPUB thật.*
