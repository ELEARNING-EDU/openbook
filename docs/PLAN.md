# PLAN — Roadmap tính năng theo độ ưu tiên

Cập nhật 2026-11-11 (đã bỏ các mục P0 làm xong ở bản 2026.11.11: mở file từ app khác, xoay ngang + 2 cột,
menu chọn chữ, ghi chú + xuất Markdown — xem `CHANGELOG.md`). Khảo sát gốc 2026-11-10, dựa trên khảo sát 10+ app đọc sách EPUB trên Google Play / App Store và thị trường
Việt Nam (ReadEra, Moon+ Reader, Readest, Yomu, Apple Books, PocketBook, Lectern, Waka, Voice Truyện…),
kèm review 1–3★ để tìm đúng khoảng trống người dùng than phiền.

File này là roadmap tính năng chung cho Android và iOS (làm cùng lúc). Không thay thế `PROJECT_STATE.md`,
`TASKS.md` hay `TODO.md` của từng nhánh.

## Nguyên tắc ưu tiên

1. **Tác động người dùng trên chi phí** — tính năng rẻ mà nhiều người gặp phải lên trước.
2. **Giữ offline-first và ràng buộc "không quyền network"** đã duyệt. Mọi tính năng cần mạng phải là
   quyết định sản phẩm riêng, không lén thêm quyền.
3. **Đào sâu thế mạnh đọc + nghe tiếng Việt**, không dàn trải đuổi kịp Moon+/ReadEra về số lượng
   định dạng, theme, gesture.
4. **Không phá vỡ lõi**: curl, TTS theo câu, highlight câu, lưu vị trí, tìm kiếm bỏ dấu.

## Đối chiếu hiện trạng

| Điểm | Hiện trạng | Hệ quả |
|---|---|---|
| `reader/ReadingPositions.kt:13` | Lưu `page/pageCount/chapter/offset` | Chưa có word-anchor; đổi cỡ chữ/cột có thể lệch |
| Toàn app | Không widget, collections/tags/series, dictionary, sync | Thiếu nhóm giữ chân và nhóm học tập |

## Bảng tổng hợp theo ưu tiên

### P0 — làm ngay, chi phí thấp, tác động cao

| Tính năng | Vì sao | App tham chiếu |
|---|---|---|
| Widget màn hình chính ("đang đọc", quote, streak) | Nhắc đọc mỗi ngày, tăng retention | Yomu, Moon+ Pro, DogEar, Widget Reader |
| Nhắc đọc theo giờ + backup/restore file (SAF) | Streak thiếu "kéo" quay lại; backup tránh mất dữ liệu khi đổi máy | Apple Books, ReadEra |

### P1 — đã có trong TODO hoặc là chuẩn của reader nghiêm túc

| Tính năng | Vì sao | App tham chiếu |
|---|---|---|
| Scroll mode + slide mode | Đã ở `TODO.md` P1/P2; chuẩn cho web-novel/điện thoại | ReadEra, Moon+, Readest, Apple Books |
| TXT/Markdown/HTML → PDF/CBZ/CBR | ReadEra vừa thêm Markdown (11/2026); mở rộng tệp người dùng | ReadEra, Moon+, Readest, PocketBook |
| Collections/tags/series + shelves (Muốn đọc/Đang đọc/Đã đọc/Yêu thích) | Thư viện hiện chỉ sort | ReadEra, Moon+, Yomu, Readest |
| Từ điển offline tiếng Việt + tra từ bằng tap | Tính năng "phải có", không cần network | Moon+, Kindle, Apple Books, Readest |
| Accessibility: OpenDyslexic, reading ruler/focus, bionic reading | Xu hướng 2025–2026, ít app Việt có | Yomu, Moon+, Readest, ReadOma |
| Footnote popup + CSS publisher cơ bản + hyphenation | "Sách đẹp như tác giả set" giữ người dùng | ReadEra, KOReader, Readest |

### P2 — khác biệt hóa, đầu tư lớn hơn

| Tính năng | Vì sao | App tham chiếu |
|---|---|---|
| Neural TTS offline tiếng Việt (Piper/Kokoro/Supertonic) | "Moat" mạnh nhất; TTS hệ thống tiếng Việt vẫn robotic, vẫn offline | Lectern, Voice Truyện, Readest |
| Word-level highlight (karaoke), Android Auto, điều khiển tai nghe | Đào sâu thế mạnh nghe sách; Readest bị chê là chỉ nhảy theo chương | Readest, Moon+ Pro |
| Reading stats dashboard sâu (tốc độ đọc, heatmap, thể loại) | Đã có streak/chart nên mở rộng rẻ | Readest, Apple Books |
| Vocabulary builder + export Anki | Hấp dẫn người học ngoại ngữ | Readest, Yomu, Kindle |
| Sync vị trí/highlight | Complaint #1 toàn ngành; làm file-based trước, cloud sau | Apple Books, Readest, BookFusion |

### P3 — cân nhắc, dễ thành hype

| Tính năng | Vì sao | App tham chiếu |
|---|---|---|
| AI tóm tắt chương / hỏi đáp grounded | Hot nhưng đắt và dễ lệch khỏi việc đọc; chỉ opt-in, ưu tiên on-device | Chaptera, ReadDeep, Reader Alive |

## Chi tiết từng đợt

### P0

- **Widget + nhắc đọc** — widget "đang đọc" + streak/quote; `AlarmManager`/WorkManager cho nhắc theo giờ.
  Cần thêm `appwidget-provider`, receiver, layout. Ưu tiên bản đọc-một-chiều trước.
- **Backup/restore** — xuất toàn bộ `SharedPreferences` + file `.epub` ra một file ZIP qua SAF, nhập lại
  có kiểm tra phiên bản. Không cần cloud.

### P1

- **Scroll/Slide** — theo `TODO.md` P1/P2; chuyển các chỗ giả định "page" sang offset, giữ nguyên
  highlight/TTS/tìm kiếm. Rủi ro chính là đồng bộ TTS ↔ scroll.
- **Đa định dạng** — theo `TODO.md` P3/P4/P5: TXT/MD/HTML bọc thành HTML chapter; PDF/CBZ dùng track
  ảnh riêng; MOBI/AZW3 chỉ báo chuyển sang EPUB.
- **Collections/shelves** — mở rộng `LibraryStore` + `LibraryScreen`: nhóm, tag, series, trạng thái đọc.
- **Từ điển offline** — đóng gói từ điển Việt (nhỏ, có giấy phép rõ) + bottom sheet tra từ khi chọn chữ.
- **Accessibility** — thêm font OpenDyslexic (nếu giấy phép cho phép), reading ruler, bionic reading
  dạng toggle trong Appearance.
- **Footnote/CSS** — popup footnote từ link nội bộ; đọc một tập CSS publisher tối thiểu (in đậm,
  nghiêng, căn lề, blockquote); hyphenation tiếng Việt.

### P2

- **Neural TTS offline** — đánh giá model Piper/Kokoro tiếng Việt, đóng gói on-device, giữ nguyên hàng
  đợi câu và highlight. Đây là quyết định lớn về dung lượng APK/AAB.
- **Karaoke + Android Auto** — highlight theo từ, điều khiển media session mở rộng.
- **Stats dashboard** — mở rộng `ReadingLog`/`HomeScreen`.
- **Vocabulary builder** — danh sách từ đã tra, trạng thái Learning/Mastered, xuất Anki.
- **Sync** — giai đoạn 1 export/import file; giai đoạn 2 cloud là quyết định sản phẩm riêng (sẽ phá vỡ
  ràng buộc "không network").

### P3

- **AI** — chỉ làm nếu có nguồn model on-device hoặc người dùng opt-in rõ ràng; không dùng AI để thay
  thế việc đọc.

## Không nên làm

- OPDS/Calibre, store/catalogue, mạng xã hội — đụng ràng buộc không network và không phải thế mạnh.
- AI tóm tắt thay thế việc đọc.
- Nhồi hàng chục theme/gesture kiểu Moon+; ưu tiên chất lượng trang đọc.

## Thứ tự thực hiện gợi ý

1. P0 "backup/restore".
2. P0 "widget + nhắc đọc".
3. P1 "scroll/slide" → "đa định dạng" → "collections" → "từ điển" → "accessibility" → "footnote/CSS".
4. P2 theo thứ tự: neural TTS → karaoke/Auto → stats → vocabulary → sync.

## Cách đo thành công

- Retention D1/D7 và số ngày có streak (đã có dữ liệu cục bộ).
- Số sách import qua "Mở bằng"/share target (kênh tăng trưởng mới).
- Số highlight/note được tạo và export.
- Đánh giá store và nội dung review nhắc tới tính năng mới.

## Nguồn tham khảo

- [Best EPUB Reader for Android 2026 – Lectern](https://lecternreader.app/blog/best-epub-reader-android)
- [iOS ebook readers 2026 – Product Impossible](https://productimpossible.com/review/ios-ebook-readers-2026/)
- [10 Best ePub Reader Apps 2026 – Freedom251](https://freedom251.com/10-best-epub-reader-apps-for-android-and-ios-2026/)
- [ReadEra trên Google Play](https://play.google.com/store/apps/details?id=org.readera)
- [Moon+ Reader trên Google Play](https://play.google.com/store/apps/details?id=com.flyersoft.moonreader)
- [Readest trên Google Play](https://play.google.com/store/apps/details?id=com.bilingify.readest)
- [Yomu trên App Store](https://apps.apple.com/us/app/yomu-ebook-reader/id562211012)
- [5 E-Reader Apps Ranked – Unstar](https://unstar.app/blog/kindle-apple-books-kobo-libby-scribd-ereader-apps-ranked-2026)
- [The Best AI Reading Apps in 2026 – ReadOma](https://readoma.com/blog/best-ai-reading-apps)
- [Waka trên Google Play](https://play.google.com/store/apps/details?id=anim.dqh.tvw)
- [Voice Truyện – TTS tiếng Việt cho EPUB/PDF](https://voicetruyen.com/)
- [Apple Books iOS 27 features – Good e-Reader](https://goodereader.com/blog/e-book-news/apple-books-gets-a-few-new-features-in-ios-27)
- [Widget Reader – home screen reading widget](https://reader.jameshaoyu.com/en)
