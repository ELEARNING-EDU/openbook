# Prompt cho iOS — Highlight chọn đoạn + Audio nền

Mục đích: chuyển sang iOS **đúng hai phần** đã làm trên Android. Không kèm theo các thay đổi khác
(Home, player UI, import, TTS engine…). Đọc hết rồi triển khai theo acceptance criteria ở cuối.

---

## Phần 1 — Highlight chọn đoạn văn bản

### 1.1 Chọn đoạn
- **Nhấn giữ** trên chữ → chọn **từ** tại điểm chạm (không chọn cả câu).
- Vùng chọn hiện **band màu** trên các dòng đã chọn + **hai thanh dọc** ở đầu và cuối đoạn.
- Người dùng **kéo thanh trái** để đổi điểm bắt đầu, **kéo thanh phải** để đổi điểm kết thúc; mỗi
  lần kéo mở rộng/thu theo **ranh giới từ**.
- **Nhấc tay ra band + hai thanh + popup vẫn còn** (không tự mất).
- Chạm ra ngoài vùng chọn → huỷ chọn.
- Khi đang chọn: **không lật trang, không seek TTS** dù player đang chạy.
- Khi bắt đầu nhấn giữ: **tạm dừng TTS** (không tự phát lại).
- Đổi cỡ chữ/theme/ xoay màn hình → huỷ vùng chọn (offset cũ không còn đúng layout).
- Vùng chọn lưu theo **chapter + startOffset/endOffset ký tự**, không theo số trang/câu.

### 1.2 Popup (neo tại vùng chọn)
- Popup hiện **ngay trên** đoạn chọn (nếu thiếu chỗ thì dưới), **không dùng bottom sheet**.
- Hàng gọn: **Highlight · Translate · Search · ›**.
- Bấm **›** → popup dọc đủ mục, **icon bên trái**: Highlight, Translate, Search, Copy, Share.
- Giai đoạn này **chỉ Highlight hoạt động**; 4 mục còn lại hiển thị mờ (disabled), chưa nối hành vi.
- Bấm **Highlight** → hiện **4 màu** (vàng, xanh lá, xanh dương, hồng). Chọn màu → lưu highlight
  và đóng popup. **Không hiện toast “đã lưu”.**
- Nếu vùng chọn ở sát mép phải, popup phải được **kẹp trong màn hình** để không cắt mất nút.

### 1.3 Hiển thị highlight đã lưu
- Band highlight vẽ **đúng màu đã chọn**, đúng **đoạn ký tự** đã lưu.
- Band tồn tại qua **lật trang, đóng/mở lại sách**, đổi theme; tính lại theo layout mới sau khi đổi
  cỡ chữ.
- Nhiều highlight trên cùng trang hiển thị đồng thời; vẽ **dưới** vùng chọn đang kéo.

### 1.4 Sửa / xoá highlight đã lưu
- **Nhấn giữ** vào một highlight đã lưu → popup tại chỗ: **Highlight · Remove**.
- **Highlight** → 4 chấm màu để đổi màu; **Remove** → xoá, chữ màu đỏ.
- Chạm ra ngoài → popup đóng (không lật trang, không seek).
- Chuyển trang → popup tự đóng.

### 1.5 Tab quản lý trong mục lục
- Sheet Nội dung có **3 tab**: Contents · Bookmarks (n) · Highlights (n) — dùng tab chuẩn của
  platform, không phải nút pill tự vẽ.
- Tab Highlights liệt kê: tên chương (accent) + text đoạn đã đánh dấu (2 dòng) + số trang.
- **Trượt sang trái để xoá** highlight (và bookmark cũng vậy).
- Tab rỗng hiện “Chưa có đoạn nào được đánh dấu”.

### 1.6 Dữ liệu
- Lưu theo sách: `chapterIndex`, `startOffset`, `endOffset`, `text`, `color` (ARGB), `createdAt`.
- Khoá định danh ổn định: `chapterIndex-startOffset-endOffset`.
- Dữ liệu cũ (nếu lưu theo câu) migrate sang cặp offset khi mở sách; chấp nhận lệch tối đa vài ký
  tự do text cũ đã trim.

---

## Phần 2 — Audio nền + media controls

### 2.1 Hành vi
- Audio/TTS **tiếp tục khi app ra nền** hoặc người dùng bấm **Back** khỏi màn đọc.
- **Không** dừng/khởi động lại TTS khi mở lại đúng cuốn đang đọc (từ nút player hoặc notification):
  đọc tiếp từ câu/trang hiện tại.
- Chuyển sang **sách khác** thì dừng sách cũ trước khi bắt đầu sách mới.
- Khi chọn text để highlight → **tạm dừng** TTS; không tự phát lại sau khi lưu/huỷ.
- Nút ✕ trong player = dừng hẳn, xoá notification.

### 2.2 Media controls trên màn hình khoá / trung tâm điều khiển
- Hiện **bìa sách + tên sách + tên chương**.
- Ba nút nằm **cạnh nhau**: **Chương trước · Play/Pause · Chương sau**.
- Bấm play/pause điều khiển đúng engine đang đọc; prev/next nhảy **đầu chương** trước/sau (bỏ qua
  chương chỉ có ảnh, không có câu).
- **Chạm vào notification/now-playing** → mở **đúng sách đang đọc** và **đúng trang/câu đang đọc**
  (resume theo vị trí đã lưu, không mở Home).
- iOS: `AVAudioSession` category `.playback` + `UIBackgroundModes: audio`; cập nhật
  `MPNowPlayingInfoCenter`; đăng ký `MPRemoteCommandCenter` cho play/pause/next/previous; deep link
  khi mở app từ now-playing (giữ bookId + vị trí).

### 2.3 Quyền và trạng thái
- Android xin `POST_NOTIFICATIONS` lần đầu; iOS không cần quyền thông báo cho now-playing.
- Nếu người dùng từ chối quyền thông báo trên Android, audio nền vẫn chạy (notification có thể ẩn).
- Notification là **ongoing**, không tự xoá khi pause; chỉ mất khi dừng hẳn.

### 2.4 Ngôn ngữ giọng đọc
- Sách tiếng nào đọc giọng tiếng đó.
- **Nội dung thắng metadata**: nhiều EPUB bán trên mạng ghi sai `dc:language=en` cho sách tiếng
  Việt. Nếu text có dấu hiệu ngôn ngữ mạnh (dấu tiếng Việt, kana, chữ Hán) mà khác tag → chọn theo
  nội dung; text mơ hồ thì giữ tag.

---

## Acceptance checklist (QA trên máy thật)

- [ ] Giữ một từ → band + 2 thanh hiện, **vẫn còn sau khi nhấc tay**.
- [ ] Kéo thanh trái/phải mở rộng theo từ; band không nhảy sang dòng khác.
- [ ] Popup gọn 4 mục; `›` mở danh sách dọc có icon; chỉ Highlight bấm được.
- [ ] Chọn màu → band đúng màu, không toast; lật trang/đóng sách/mở lại vẫn còn.
- [ ] Giữ vào highlight cũ → Highlight/Remove; Remove đỏ; chạm ngoài đóng; chuyển trang đóng.
- [ ] Tab Highlights liệt kê đúng; trượt trái xoá được cả bookmark và highlight.
- [ ] Đang đọc (player chạy) mà giữ để chọn: TTS pause, không lật trang, không seek; lưu xong vẫn
      pause.
- [ ] Bấm Home/Back khi đang đọc: audio tiếp tục; mở lại đúng sách từ nút đĩa/now-playing thì đọc
      tiếp đúng trang.
- [ ] Now-playing có bìa + tên sách + chương; 3 nút cạnh nhau hoạt động.
- [ ] Sách tiếng Việt (kể cả EPUB ghi `en`) đọc bằng giọng tiếng Việt.
- [ ] Không crash/ANR ở các luồng trên; rời app 5 phút audio vẫn chạy.

## Ngoài phạm vi (đừng làm trong prompt này)
Home/achievements, mini-player animation, import/DRM, benchmark, icon set, các mục Translate/
Search/Copy/Share của popup (để sau).
