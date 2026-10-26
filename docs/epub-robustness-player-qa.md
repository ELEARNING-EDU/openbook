# Kiểm tra EPUB lỗi / bản quyền + benchmark sách lớn + animation mini player

Nguồn: bản iOS đã hoàn thiện và kiểm chứng (commit `98d47c4`). Đối chiếu Android với spec
này, chỗ nào lệch thì sửa trong đúng phạm vi, không viết lại toàn bộ reader.

**Trước khi sửa:** đọc `PROJECT_STATE.md`, `SESSION_HANDOFF.md`, `TASKS.md`, chạy `git status`
và `git diff`. Repo đang có thay đổi chưa commit (commit gần nhất `315c8e6`); **không xoá hay
ghi đè** các thay đổi đó. Không commit/push khi chưa được duyệt.

Phạm vi: `epub/` (đọc/parse), `library/LibraryStore.kt` (import), `ui/ReaderScreen.kt` +
`ui/AudioPlayerSheet.kt` (player), `reader/SpeechReader.kt` (trạng thái đọc).
Strings: `values/strings.xml` + `values-vi/strings.xml`.

---

## A. Sách lỗi / tương thích định dạng

Yêu cầu: **không crash, không ANR, không hiển thị nội dung rác, không bỏ chương âm thầm.**

### A1. Import phải validate trước khi đưa lên kệ
- Kiểm tra: file là ZIP, có `META-INF/container.xml`, OPF đọc được, spine có ít nhất 1 chương
  văn bản tồn tại trong archive, và không phải sách DRM (mục B).
- File không đạt: **không copy vào thư viện** (hoặc xoá bản copy), hiện lỗi rõ ràng. Sách đã
  nằm sẵn trong thư mục sách thì vẫn hiện trên kệ, khi mở báo lỗi (không vẽ rác).

### A2. Các dạng **phải mở được**

| Dạng | Ghi chú |
|---|---|
| EPUB 2.0 chỉ có `toc.ncx` | tiêu đề chương lấy từ NCX |
| EPUB 3 chỉ có `nav.xhtml` | không có NCX |
| `content.opf` nằm ở gốc archive | không có thư mục OEBPS |
| ZIP ghi size bằng data descriptor (bit 3) | zip kiểu stream/Java |
| href `%C6%B0%C6%A1ng%201.xhtml` | percent-encoded + tên Unicode |
| href tuyệt đối `/OEBPS/ch01.xhtml` | |
| Tên file trong zip khác hoa/thường với href (`CH02.XHTML` vs `ch02.xhtml`) | phải tìm được chương, không mất chương |
| Entry có tiền tố `./OEBPS/...` | phải mở được |
| Tên file NFD (macOS) vs href NFC | phải khớp |
| `mimetype` bị nén / đặt cuối / thiếu hẳn | vẫn mở |
| ZIP64 extra (file nhỏ) | vẫn mở |
| Spine `linear="no"` | hiện như chương bình thường |
| Fixed-layout `rendition:layout=pre-paginated` | mở được, mỗi trang 1 ảnh |
| Sách nhiều ảnh (30 ảnh, ~12 MB) | không OOM, ảnh render đúng |
| RTL Arabic, CJK (Nhật/Trung) | không crash, phân trang hợp lý |
| OPF có BOM + entity (`&amp;`, `&#8212;`) | title/author decode đúng |
| HTML lỗi kiểu Word (thẻ không đóng, `<br>`, `<script>`, entity) | vẫn hiện text |
| Spine trỏ tới idref không tồn tại | bỏ qua idref đó, giữ chương hợp lệ |
| EPUB 3 thật của Project Gutenberg | 15 chương, ảnh, mở bình thường |

### A3. Các dạng **phải từ chối** với thông điệp đúng
- Không có `META-INF/container.xml`
- Manifest/spine rỗng
- File không phải ZIP (ví dụ PDF đổi đuôi `.epub`)
- Sách DRM (mục B)

### A4. Hành vi
- Lỗi có chuỗi riêng, không dùng "Không đọc được tệp EPUB" cho mọi trường hợp. Tối thiểu 4
  thông điệp: thiếu container, thiếu OPF, spine rỗng, sách bảo vệ bản quyền.
- Không log nội dung sách/bytes; log tên file + loại lỗi.
- Nếu một chương không tìm thấy trong archive: bỏ qua chương đó nhưng **không được làm mất các
  chương khác**; nếu mất > 0 chương thì ghi log rõ (file, href, số chương mất).

### A5. Bộ test có sẵn (dùng lại, đừng tạo mới từ đầu)
Trên máy này có corpus do iOS tạo:
```
/Users/cuong/Workspace/Orca/OpenBook/ios/build/qa/epub-corpus/
├── manifest.json          # mỗi file: expect = opens|throws, chapters, notes
└── books/                 # 31 file tổng hợp + 90-real-gutenberg.epub
```
- Copy `books/` + `manifest.json` vào test resources của Android (hoặc đọc trực tiếp đường dẫn
  trên khi chạy test nội bộ).
- Viết test JVM/Robolectric chạy qua `EpubArchive` (parse thật, không mock) và đối chiếu
  `expect`, `chapters`, `title`, `author`.
- Các ca từng lỗi trên iOS: `05-case-mismatch`, `28-dot-slash`, `10-drm-aes`, `11-lcp`,
  `24-renamed-pdf`, `09-obfuscated-font` — phải đúng như manifest.
- Nếu parser Android dùng `ZipInputStream`: corpus có ca data descriptor và ZIP64, kiểm tra kỹ
  vì luồng này dễ đọc sai size.

---

## B. Sách bản quyền (DRM)

### B1. Nhận diện
- Đọc `META-INF/encryption.xml`; lấy mọi `CipherReference URI`.
- Nếu URI trỏ tới `.xhtml`, `.html`, `.htm` → **sách có bảo vệ bản quyền** (LCP, Adobe
  ADEPT/AES, DRM của shop).
- Không cần phân biệt loại DRM, không cần giải mã.

### B2. Không được từ chối nhầm
- `encryption.xml` chỉ liệt kê font (`.ttf/.otf`, IDPF obfuscation) hoặc ảnh → **vẫn đọc bình
  thường** (ca `09-obfuscated-font`).
- Không dựa vào sự tồn tại của `META-INF/license.lcpl` một mình; phải dựa vào tài nguyên văn
  bản bị mã hoá.

### B3. Hành vi
- Import: từ chối, không lưu file, alert/thông báo có nội dung (vi + en), ví dụ:
  - vi: *"Sách có bảo vệ bản quyền (DRM) nên không đọc được ở đây. Hãy gỡ DRM hoặc mở bằng
    ứng dụng đã bán sách."*
  - en: *"This book is protected by DRM, so it cannot be read here. Remove the DRM or open it
    in the app that sold it."*
- Sách đã có sẵn trong thư mục: khi mở hiện đúng thông báo trên, **tuyệt đối không hiển thị
  ciphertext** (lỗi cũ của iOS: 25 trang ký tự rác).
- Không thêm thư viện DRM, không thử giải mã, không log key/license.
- Test: `10-drm-aes` và `11-lcp` phải báo protected; `09-obfuscated-font` phải mở 3 chương.

---

## C. Benchmark sách nhiều trang

### C1. Bộ sách đo (có sẵn trong corpus)
| File | Nội dung | Kỳ vọng iOS (tham khảo) |
|---|---|---|
| `25-big-250-chapters.epub` | 250 chương, ~1,13M ký tự | ~1.500 trang, mở 1,5 s |
| `26-1000-chapters.epub` | 1.000 chương ngắn | 1.000 trang, mở 4,4 s (ca xấu nhất) |
| `27-single-huge-chapter.epub` | 1 chương, 585k ký tự | ~846 trang, mở 0,25 s |
| `15-image-heavy.epub` | 5 chương, 30 ảnh | không OOM |
| `90-real-gutenberg.epub` | sách thật | 273 trang |

Số iOS là **Debug + simulator**, chỉ để tham chiếu, không phải mục tiêu Android.

### C2. Kịch bản đo (build **release**, máy thật nếu có; ghi rõ model + Android version)
1. Import file (copy + validate) → thời gian, bộ nhớ.
2. Mở sách từ kệ → thời điểm trang đầu **đọc được** (không phải lúc spinner biến mất).
3. Phân trang toàn bộ sách (lần đầu) → thời gian, có block main thread không.
4. Nhảy chương đầu ↔ cuối, kéo scrubber, lật 20 trang → latency mỗi thao tác.
5. Bắt đầu đọc (TTS) từ trang hiện tại → thời gian tới tiếng đầu tiên.
6. Bộ nhớ đỉnh (Android Studio Memory / `dumpsys meminfo`) và frame jank
   (`dumpsys gfxinfo` hoặc Perfetto).

### C3. Ngưỡng chấp nhận (đề xuất, mid-range)
- Mở sách 1.500 trang → trang đầu đọc được **≤ 1,5 s**.
- Không thao tác nào block main thread > **200 ms**; tuyệt đối không ANR.
- Nếu phân trang toàn bộ lâu: phải chia nhỏ (chunk/yield/lazy theo chương) và hiện tiến độ;
  không khoá UI > 500 ms.
- Lật trang ổn định 60 fps (frame ≤ 16 ms; máy 120 Hz càng tốt), không drop > 1 frame khi lật.
- Nhảy chương ≤ 300 ms; bắt đầu TTS ≤ 500 ms.
- RAM đỉnh ≤ **300 MB** với sách 1.500 trang (không giữ bitmap/ảnh không cần).
- Kết quả **trước/sau** nếu có sửa hiệu năng.

### C4. Báo cáo
Bảng: thiết bị, Android, build type, từng bước C2, số đo, ghi chú. Kèm tên trace Perfetto/ảnh
`gfxinfo` nếu có. Nếu chưa đo được trên máy thật thì nói rõ "chưa đo", không suy đoán.

---

## D. Animation mini player

Hiện Android đã có player row; yêu cầu là **hoàn thiện animation/trạng thái cho khớp spec**,
không viết lại.

### D1. Trạng thái
- **idle** (chưa đọc): nút tròn tai nghe ở góc dưới trái.
- **playing/paused**: nút tròn biến thành **đĩa than** (bìa sách) + pill mở ra bên phải:
  `Tên sách · Chương` (marquee) → nhãn tốc độ → `−  play/pause  +  ✕`.
- **chrome đầy đủ** (toolbar hiện): pill **compact** — ẩn nhãn tốc độ và 2 nút ±, chỉ còn
  play/pause + ✕; nút cài đặt hiện ở góc phải.
- **chrome ẩn**: chỉ còn nút đĩa + pill (player-only), không có nút khác.

### D2. Mở/đóng
- Vào sách: **không tự mở** full player, **không tự mở** popup giọng.
- Bấm đĩa khi idle → bắt đầu đọc + bung pill. Bấm đĩa lần nữa → thu pill lại (đọc vẫn tiếp tục).
- Dừng đọc (✕) → pill thu về đĩa, đĩa ngừng quay, nút trở lại tai nghe.
- Chạm **tiêu đề trong pill** → mở full player (bottom sheet 352dp, kéo lên large được).
  Đóng sheet → pill giữ nguyên, đồng hồ auto-hide của toolbar đếm lại từ đầu.

### D3. Motion (quan trọng)
- Bung/thu pill bằng **spring ~320 ms, damping ~0.78** (không nảy quá đà). Compose gợi ý:
  `spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)` hoặc tween 320 ms
  `FastOutSlowIn`.
- Bung: scale từ **0.55** neo mép trái (phía đĩa) + fade in.
- Thu: scale về **0.75** + fade out, không biến mất đột ngột.
- Đổi compact ↔ đầy đủ dùng **cùng spring**, không nhảy layout, không giật chữ.
- Sheet full player trượt lên chuẩn Material bottom sheet; không có animation riêng lạ.

### D4. Đĩa than
- Quay **~30°/giây** khi đang đọc; **dừng đúng góc** khi pause; quay tiếp từ góc đó khi resume.
- Bìa có gradient, 2–3 vòng rãnh, nhãn giữa + lỗ trục; có điểm sáng lệch tâm để thấy rõ đang quay.
- Không dùng animation lặp vô hạn vô nghĩa; phải gắn với trạng thái TTS.

### D5. Marquee tiêu đề
- Chỉ chạy khi text dài hơn chỗ trống. Tốc độ **~26 dp/s**, khoảng lặp **40 dp**, chạy vòng
  liền mạch.
- Đổi chương → đổi text, đo lại, không giật.
- Đủ chỗ → đứng yên.

### D6. Progress line
- Đường mảnh **3 dp** ở đáy pill, màu accent, chạy theo câu đang đọc (không theo trang).
- Cắt theo bo tròn của pill (clip theo capsule), không thò ra ngoài.

### D7. Accessibility / hiệu năng animation
- TalkBack: "Mở trình phát", "Tạm dừng"/"Phát", "Chương trước", "Chương sau", "Tốc độ đọc",
  "Dừng đọc"; vùng chạm ≥ 48dp.
- Tôn trọng `Settings.Global.ANIMATOR_DURATION_SCALE = 0` / Reduce Motion: thay scale + quay +
  marquee bằng fade/đứng yên.
- Animation không được đọc dữ liệu/đo layout mỗi frame; dùng `animate*AsState`/`graphicsLayer`,
  không gây recomposition toàn màn.
- Kiểm bằng `adb shell dumpsys gfxinfo` hoặc Perfetto: không drop frame khi bung/thu pill và
  khi chrome đổi.

### D8. Bằng chứng cần nộp
- Quay màn hình (hoặc chuỗi ảnh các mốc 0/100/200/320 ms) cho: idle → bung pill,
  playing → pause, chrome hiện → compact, chuyển chương, dừng đọc; kèm mô tả ngắn.
- Log trạng thái `idle/playing/paused`, `chrome`, `playerOpen` khi chạy.

---

## E. Ràng buộc chung
- Chuỗi **vi + en** cùng key, không hard-code tiếng Việt trong code.
- Build + test phải xanh trước khi báo xong; ghi rõ lệnh, exit code, số test.
- Không đổi kiến trúc, không thêm thư viện DRM/benchmark mới nếu không cần; không commit/push.
- Cập nhật `PROJECT_STATE.md` + `SESSION_HANDOFF.md`: đã làm, số đo, file dirty, bước tiếp theo.
- Báo lại: files changed, kết quả test, bảng benchmark, bằng chứng animation, phần chưa
  làm/không làm được.

## F. Acceptance checklist
- [ ] Import chặn sách DRM và file hỏng, không để lại file trên kệ.
- [ ] Không bao giờ hiển thị ciphertext; sách DRM có thông báo vi/en.
- [ ] 20 dạng EPUB ở A2 mở đúng; 4 dạng ở A3 bị từ chối đúng.
- [ ] Case mismatch và `./` không còn mất chương/cả sách.
- [ ] Sách 1.500 trang: trang đầu ≤ 1,5 s, không ANR, số đo kèm bảng.
- [ ] Sách 1.000 chương: nêu rõ thời gian phân trang và cách không khoá UI.
- [ ] Pill bung/thu đúng spring, không nhảy; đĩa quay/dừng đúng trạng thái; marquee + progress đúng.
- [ ] Chrome hiện → pill compact (ẩn ±/tốc độ, hiện bánh răng), không giật.
- [ ] Full player chỉ mở khi chạm tiêu đề; không tự mở khi vào sách.
- [ ] TalkBack đọc đúng; Reduce Motion được tôn trọng.
- [ ] Đủ key vi/en, build + test xanh, chưa commit.

---

*Corpus nằm trong `build/` của repo iOS (đã gitignore) nên chỉ có trên máy này; nếu cần đưa
vào repo Android thì copy vào `app/src/test/resources/epub-corpus/` và giữ nguyên
`manifest.json`.*
