# Tasks — Sách (Android EPUB reader)

Cập nhật 2026-11-10. Chỉ giữ việc đang mở; việc đã xong nằm trong Git và trong
`SESSION_HANDOFF.md`. Kế hoạch tính năng dài hạn ở [`../TODO.md`](../TODO.md).

## Đang mở

| ID | Mức | Owner | Nghiệm thu | Ghi chú |
| --- | --- | --- | --- | --- |
| T-119 | P1 | Chưa giao | Import không đọc quá 64 MiB mỗi entry / 256 MiB mỗi EPUB trong `TextCleanup`, từ chối trước khi cấp phát, giữ file cũ khi lỗi; có regression test | `epub/TextCleanup.kt:70` gọi `readBytes()` thẳng, bỏ qua budget của `ZipExtract` (T-112) |
| T-120 | P1 | Chưa giao | `"She had had enough."` giữ nguyên sau khi sửa | `TextCleanup.kt:178,185` — `doubledWord` xoá cả từ lặp hợp lệ |
| T-121 | P2 | Chưa giao | `<style>`, `<script>`, `<pre>`, `<code>` không bị `tidy` sửa | Chỉ sửa text node; hiện regex chạy trên cả khối |
| T-111 | P2 | Chưa giao | 33 ca corpus chạy thật thay vì skip | Corpus không có ở `../ios/build/qa/epub-corpus`. `EpubCorpusTest` còn fallback path cũ `/Users/cuong/Workspace/Orca/OpenBook/ios/…` và không còn nhận `-DepubCorpus` từ Gradle — cần khôi phục corpus rồi truyền đường dẫn (hoặc sửa fallback) |

Ba lỗi T-119/T-120/T-121 có probe tái hiện trong báo cáo QA 2026-11-02 (đã xoá khỏi
`docs/`; xem Git history). Không phụ thuộc task khác.

## Chặn phát hành (không phải task code)

- **Upload AAB lên Play** đang chờ Google duyệt "Request upload key reset" (gửi 2026-11-07
  với `keystore/upload_certificate.pem`, SHA-1 `6B:59:9A:7A:…:C8:9C:F4`). Play đang chờ
  cert `B3:A7:F5:1F:…:88:46:5F` — không keystore nào trong máy tạo ra nó. Duyệt xong thì
  upload lại `app/build/outputs/bundle/release/app-release.aab`, không cần build lại.

## Vừa xong

| Ngày | Việc | Commit / bằng chứng |
| --- | --- | --- |
| 2026-11-07 | Review Play sau lần mở app thứ 10 (`ReviewPromptStore`, `ReaderApplication`, `MainActivity`), version `20261107.00` | `1bdce0a`; 3 test mới, 137 test tổng; thẻ review hiện thật trên E2 |
| 2026-11-07 | Tách hunk `epubCorpus` không liên quan ra khỏi commit trên (không amend) | `ba6a431` |
| 2026-11-07 | Đổi tên keystore thành `openbook-release (12345678!@#).jks`, export `upload_certificate.pem` | file bị `.gitignore` |
| 2026-11-02 | T-110, T-114–T-117 (tác giả, bìa khi xoá, tìm Đ/NFD, Stop khi TTS đang khởi tạo) | `git log --grep T-11` |
| 2026-11-02 | T-113 lưu vị trí khi nghe nền; T-118 dừng sau 2 lỗi TTS; sửa chính tả mặc định tắt; settings bottom sheet + popover theo hàng | `c19e4e1`, `095f15c`, `cfe05a0` |

T-001…T-109 (dựng app, curl, chrome, player, Home/Library, Hilt, test suite) đều đã
"Verified complete" — bảng bằng chứng cũ ở `git show edd6de4:docs/TASKS.md`.
