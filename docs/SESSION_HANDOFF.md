# Session handoff — Sách (Android EPUB reader)

## Checkpoint 2026-11-11 (evening) — Claude, Android + iOS parity session

- Owner rule: Android and iOS are built together; shared notes on master: `docs/PARITY.md` (gaps, decisions,
  per-batch log), `docs/CHANGELOG.md` (one shared changelog), `docs/PLAN.md` (roadmap). Per-platform state stays here.
- Coding is delegated to the opencode agent in each worktree via Orca (`orca orchestration send --to dispatch:<id>`);
  both run `opencode-go/deepseek-v4.1-flash` (the free model hit its daily quota). Claude reviews, tests, does device QA.
- NOTHING IS COMMITTED in `android` or `ios` this session (owner has not asked). Do not stage `AGENTS.md`/`CLAUDE.md`
  or the toolkit deletions (`.ai-toolkit/`, `.agents/`, `.claude/`) — they are the owner's.
- Done this session (both platforms unless noted): wordmark icon; UI copy unified ("đánh dấu" = bookmark,
  "tô sáng" = highlight); reader guide + review prompt (iOS); stats sheet (Android); highlight palette/notes/Markdown
  export; open EPUB from other apps; selection menu Copy/Share/Search/Translate; margin 30→20; contents/search sheet
  styling; reader light/dark follows the phone until chosen; rotation + 2 columns when width > 560; paused read-aloud
  resumes from the visible page; demo launches skip the review prompt.
- Android specifics: versionCode/versionName NOT bumped yet (still `2026110700` / `20261107.00`); proposal to owner:
  `2026111100` / `2026.11.11`. E2 = `emulator-5554` now has the RELEASE-signed build installed (install release over it,
  a debug install needs an uninstall). Tests: `./gradlew :app:testDebugUnitTest` → 152 tests, 33 skipped, 0 failed.
- Next: owner decides commit + Android version bump; then PLAN.md batch "backup/restore".


Cập nhật 2026-11-10. Chỉ mô tả trạng thái hiện tại; checkpoint trước 2026-11-07 xem trong Git
(`git show edd6de4:docs/SESSION_HANDOFF.md`).

## Trạng thái hiện tại

- Repo/worktree: `/Users/cuong/Workspace/Projects/openbook/android`, branch `android`,
  HEAD `edd6de4` (docs + xoá skill cũ). Chuỗi commit gần: `edd6de4` → `ba6a431` → `1bdce0a`.
- Working tree: chỉ `AGENTS.md` bị sửa (không phải việc của phiên này — để nguyên, không
  commit). Không có thay đổi code nào chưa commit. Chưa push gì.
- Bản build: `versionCode 2026110700`, `versionName 20261107.00`.
  - APK release: `app/build/outputs/apk/release/app-release.apk`, 1.788.427 bytes, sha256
    `802d64cf…846f17`, ký `CN=OpenBook` (cert SHA-1 `6B:59:9A:7A:…:C8:9C:F4`).
  - AAB: `app/build/outputs/bundle/release/app-release.aab`, 4.362.665 bytes, sha256
    `059c9869…10998c`, `bundletool validate` OK. **Đây là file để upload Play.**
- Kiểm chứng trên bản này: 137 test (104 pass, 33 skip do thiếu corpus, 0 fail), lint 113
  warning / 0 error, `assembleRelease` + `bundleRelease` xanh. E1 và E2 đều chạy bản cài từ
  AAB (`splits=[base, config.arm64_v8a, config.en]`), đã smoke: mở sách, lật trang, đọc to
  (highlight + đĩa + player), nghe nền khi Back, settings sheet + popover, không FATAL.
- Review Play: bộ đếm `launches` chạy 1→10 và đặt `asked=true` trên E2; thẻ review của Play
  hiện thật ở lần mở thứ 10 (`review-sheet.png` trong thư mục temp của phiên).

## Đang chờ

**Play Console từ chối AAB vì sai upload key.** Play chờ cert SHA-1
`B3:A7:F5:1F:0E:2F:FA:1B:15:0A:24:75:64:25:C3:DB:00:88:46:5F`; AAB hiện tại ký bằng
`6B:59:9A:7A:…:C8:9C:F4`. Hai keystore trong máy (`openbook-release`, `gplay_elearning` 2016
`CN=E LEARNING`) đều không khớp. Chủ dự án đã gửi **Request upload key reset** ngày
2026-11-07 kèm `keystore/upload_certificate.pem`; chờ Google duyệt (1–2 ngày làm việc).

- Duyệt xong: upload lại đúng file AAB trên, **không build lại**, không đổi keystore.
- Cặp fingerprint `B7:99:61:DA:…` / `6C:01:30:82:…` chủ dự án dán vào chưa xác định được là
  app-signing key của Google hay upload cert khác; không có key nào trên máy tạo ra nó.
- Rủi ro nếu app chưa bật Play App Signing: không có upload key để reset, app đã phát hành sẽ
  không update được nữa — cần xác nhận trạng thái App integrity trước khi làm gì thêm.

## Việc tiếp theo

1. Có email duyệt reset → upload `app/build/outputs/bundle/release/app-release.aab` lên track
   nội bộ, kiểm tra Play nhận đúng cert `6B:59…`.
2. Không có việc code nào bắt buộc. Việc còn treo: **T-119** (P1, `TextCleanup` bỏ qua budget
   ZIP), **T-120** (P1, xoá từ lặp hợp lệ), **T-121** (P2, sửa cả `<style>/<pre>/<code>`), và
   **T-111** (khôi phục corpus để 33 skip thành pass). Chi tiết ở [`TASKS.md`](TASKS.md).
3. Kế hoạch tính năng (slide/scroll mode, TXT/PDF/CBZ, MOBI) ở [`../TODO.md`](../TODO.md).

## Môi trường

- Build: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` (JDK 17 mặc định
  của máy; Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20). Release: `:app:assembleRelease` và
  `:app:bundleRelease`.
- Thiết bị: **E1 = `emulator-5556`**, **E2 = `emulator-5554`** (cả hai Android 16 / API 36,
  1344×2992 @480). Tên AVD lấy bằng `adb -s <serial> emu avd name`; đừng đoán theo số serial.
  E1 đã đặt `animator_duration_scale=1` để đĩa quay khi chụp màn hình.
- Cài bản từ AAB (AAB không `adb install` được):

  ```bash
  bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
    --output=/tmp/openbook.apks \
    --ks="keystore/openbook-release (12345678!@#).jks" --ks-pass=pass:'12345678!@#' \
    --ks-key-alias=openbook --key-pass=pass:'12345678!@#'
  bundletool install-apks --apks=/tmp/openbook.apks --device-id=emulator-5556
  ```

  APK ký cùng keystore thì `adb install -r` giữ dữ liệu; đổi chữ ký (debug → release) phải
  `adb uninstall` trước.
- Bộ đếm review (chỉ đọc được với bản debug, `run-as`):
  `adb shell run-as com.elearning.openbook cat shared_prefs/reviewPrompt.xml`.
- Điều khiển máy: lấy toạ độ từ `uiautomator dump` (bounds), không dùng toạ độ nhớ sẵn; mỗi
  lần một thao tác `input`, đọc logcat ngay sau đó. Hai emulator dùng chung với phiên khác nên
  có thể bị `force-stop`/giật tap giữa test.
- Nếu cần xem lại thẻ review: xoá `shared_prefs/reviewPrompt.xml` trên bản debug rồi mở app
  10 lần (Play vẫn có thể tự giới hạn tần suất hiển thị).

## Cảnh báo còn đúng

- `LaunchedEffect` sống lâu hơn composition: mọi quyết định bên trong phải đọc state sống
  (`speech.state`, `chrome.visible`), không đọc giá trị chụp lúc compose — đã một lần làm
  toolbar tự ẩn khi đang đọc.
- `adb exec-out screencap` làm nghẽn pipeline hiển thị và không bắt được frame animation;
  dùng `adb shell 'timeout 15 screencap -p /sdcard/x.png'` rồi `adb pull`.
- Không có `INTERNET` permission; Store là thông báo offline có chủ đích.
