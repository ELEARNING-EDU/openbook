# Full player — parity spec (iOS source of truth)

Source: iOS `9da6500` (OpenBook/ios). Nguồn yêu cầu: khách (chủ dự án) chốt trên bản iOS.
Việc cần làm: **đối chiếu `AudioPlayerSheet.kt` + phần TTS hiện có với spec này, chỗ nào khác thì sửa cho khớp**, không mở rộng phạm vi khác.

## 1. Mở / đóng
- Vào sách **không tự mở** full player và **không tự mở** popup chọn giọng.
- Đáy màn hình đọc có player row thu gọn (đĩa + pill: tên sách, tốc độ, hẹn giờ, play/pause, đóng).
- Chỉ chạm **tiêu đề sách** trong pill mới mở full player; chạm nút giọng mới mở popup giọng.
- Đóng sheet → row vẫn còn; auto-hide của chrome đếm lại từ đầu.

## 2. Bố cục full player
```
[bìa 96dp]  Tên sách (≤2 dòng)
            Tác giả
            Chương đang đọc          ← màu accent
            ───────────────────────  ← hairline
            Giọng đọc: <tên giọng>
            [ −   1×   + ]           ← capsule nhỏ, không full width

Trang 13/13                     93%
[━━━━━━━━━━━━━━●━━━━━━━━]         ← scrubber toàn sách

(⏱)   (⏮)   ( ▶ 60dp )   (⏭)   (〰)
```
- Bottom sheet cao ~352dp (kéo lên large được), có drag handle.
- Nút phụ 48dp, play 60dp, cách nhau 10dp.
- Hàng nút: hẹn giờ · lùi chương · play/pause · tiến chương · giọng đọc.
- Nút tốc độ **nằm dưới tên chương**, capsule cao ~34dp, rộng theo nội dung.
- Màu lấy theo palette reader (paper/ink/accent/control fill), không dùng màu Material mặc định.

## 3. Phát / tạm dừng
- Play ⇄ pause phản ánh đúng trạng thái TTS.
- Đang đọc mà người dùng lật trang → đọc trang mới từ đầu.
- TTS tự lật trang theo câu đang đọc; callback trễ không được lật ngược.
- Đang pause thì người dùng tự do lật trang.

## 4. Chuyển chương
- `⏮`/`⏭` nhảy đầu chương trước/sau, đọc từ câu đầu.
- `⏮` disable ở chương đầu; `⏭` disable ở chương cuối (mờ ~30%, không nhận chạm).
- Đang pause mà skip → vẫn pause.

## 5. Tốc độ
- Mức: 0.5 / 0.75 / 1.0 / 1.25 / 1.5; hiển thị `0.5×`, `1×`, `1.25×`.
- `−`/`+` đổi một mức, disable ở biên.
- Map sang `TextToSpeech.setSpeechRate` (Android dùng thang khác, cần hệ số quy đổi và test bằng tai).
- Đổi tốc độ giữa câu → đọc lại **câu hiện tại** với tốc độ mới, không nhảy đầu chương.

## 6. Hẹn giờ tắt
- Tắt / 5 / 15 / 30 / 60 phút; hiện số phút còn lại; hết giờ dừng đọc.
- Đang hẹn giờ: icon accent; không hẹn: ink.
- Không lưu qua lần mở app.

## 7. Scrubber
- Vị trí = tiến độ toàn sách; nhãn `Trang x/y` + `%`.
- Khi kéo: knob theo tay, không để callback TTS giật lại.
- Thả: nhảy tới câu chứa vị trí; nếu trước đó pause thì vẫn pause.
- Khi TTS chạy: knob tự chạy theo câu.

## 8. Chọn giọng
### 8.1 Nguồn
- Chỉ giọng hệ thống đã cài (`TextToSpeech.getVoices()`), không tự tải/xoá.
- Nút "Tải thêm" chỉ mở cài đặt TTS hệ thống (`ACTION_TTS_SETTINGS` / màn hình voice data), kèm giải thích.

### 8.2 Lọc theo ngôn ngữ sách — bắt buộc
- Xác định ngôn ngữ sách (`dc:language` hoặc language ID; fallback heuristic).
- Popup **chỉ giọng cùng ngôn ngữ sách**; so khớp mã ngôn ngữ (`vi` khớp `vi-VN`).
- Giọng đã lưu khác ngôn ngữ sách → bỏ qua, dùng mặc định.
- Mặc định: giọng sách → giọng ngôn ngữ máy → `vi-VN` → giọng Việt bất kỳ → giọng đầu tiên.
- Máy không có giọng nào cho ngôn ngữ sách → hiện toàn bộ (không để rỗng).

### 8.3 Popup
- Header "Giọng đọc" + "N giọng cho sách này" + nút đóng.
- Tìm kiếm: tên ngôn ngữ, mã locale (`en-GB`), tên giọng. Khớp ngôn ngữ giữ cả nhóm; khớp tên giữ đúng dòng; không khớp → "Không có giọng nào khớp."
- Nhóm theo ngôn ngữ/vùng, tiêu đề dạng `Tiếng Việt (Việt Nam)`; ngôn ngữ sách lên đầu, Tiếng Việt ưu tiên sau đó, còn lại A→Z; trong nhóm tên A→Z.
- Mỗi dòng: play/stop preview · tên giọng · `chất lượng · locale` · nút **Dùng** / dấu ✓.
- Nghe thử: TTS instance **riêng**, không đụng vị trí đọc; nếu sách đang đọc thì pause trước; rời popup dừng preview.
- **Dùng** → áp dụng ngay, đóng popup; đang đọc đọc tiếp từ câu hiện tại; đang pause vẫn pause.

## 9. Không nháy theme khi thoát sách
- Reader tối + shell sáng: khi back **không** đổi theme Activity/window theo reader, nếu không thư viện sẽ đen rồi mới sáng.
- Màu reader chỉ áp trong màn reader; pop thì trả màu hệ thống ngay từ đầu transition.

## 10. Trạng thái
- Lưu giọng + tốc độ + vị trí đọc; hẹn giờ không lưu.
- Mở lại sách: phục hồi vị trí; giọng lưu sai ngôn ngữ sách → dùng mặc định.

## 11. Accessibility / i18n
- contentDescription: "Chương trước", "Chương sau", "Hẹn giờ tắt", "Giọng đọc", "Tốc độ đọc", "Mở trình phát".
- Hiển thị "Giọng đọc: Linh"; TalkBack đọc đúng.
- Chuỗi đủ **vi + en**, key khớp nhau (đối chiếu `values/strings.xml` và `values-vi/strings.xml`).

## 12. Acceptance checklist
- [ ] Vào sách không tự mở full player / popup giọng.
- [ ] Chạm tiêu đề → full player; chạm nút giọng → popup giọng.
- [ ] `⏮` tắt ở chương đầu, `⏭` tắt ở chương cuối.
- [ ] Tốc độ đúng mức, disable ở biên, đọc tiếp từ câu hiện tại.
- [ ] Hẹn giờ 5/15/30/60 dừng đúng lúc.
- [ ] Kéo scrubber không giật; thả đúng câu; giữ pause.
- [ ] Sách Việt chỉ giọng Việt; Anh/Nhật/Trung chỉ giọng tương ứng.
- [ ] Giọng lưu sai ngôn ngữ sách bị bỏ qua.
- [ ] Preview dừng đúng, không chồng tiếng.
- [ ] Back về thư viện không nháy nền tối.
- [ ] Đủ vi/en, TalkBack đúng.

## Ghi chú khi đối chiếu
- Android `AudioPlayerSheet.kt` hiện có thể đang dùng bố cục/flow khác; chỉ sửa chỗ lệch spec, không viết lại toàn bộ.
- Kiểm tra `values-vi/strings.xml` đã có đủ key chưa.
- Build + test hiện có phải xanh trước khi báo xong; **chưa commit** nếu chưa được duyệt.
