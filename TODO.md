# TODO — iOS

Cập nhật 2026-11-11. Chi tiết lỗi và bằng chứng trong [docs/TASKS.md](docs/TASKS.md), T18–T24. Ô `[ ]` là việc còn mở; ô `[x]` là việc đã xong và đã commit.

## Ưu tiên sửa lỗi

- [x] **P1 · T18:** giữ sách cũ khi import trùng tên thất bại — copy vào file staging ẩn, validate, rồi `replaceItemAt`; metadata (title/author/importedAt/finishedAt) giữ nguyên khi thay thế. Test: import hỏng cùng tên giữ nguyên byte + metadata; import hợp lệ thay file, giữ metadata. Commit `331d05e`.
- [x] **P1 · T19:** chặn archive trước khi cấp phát — `ZipArchive.checkResourceLimits` (64 MB/entry, 256 MB/tổng, 10.000 entry) chạy ngay sau khi mở, ở cả `read`/`readMetadata`/`validate`/`coverData`; `inflate` vẫn kiểm lại. Test archive tự dựng khai 900 MB bị `ZipError.tooLarge`. Commit `8f5818f`. Chưa đo peak memory trên thiết bị.
- [x] **P2 · T20:** offset thống nhất UTF-16 — `TextOffsets` dùng ở `search` (thay `text.distance`) và `hit` (thay `offsetBy:`), kẹp hai đầu. Test emoji (200 vs 100), NFD tiếng Việt (3 vs 1), kẹp offset. Commit `2e9a362`.
- [x] **P2 · T21:** xoá sách thì xoá `readerBookmarks` và `readerHighlights` theo ID, chỉ với ID xoá file thành công. `BookmarkStore.erase` + `HighlightStore.erase` (đã có) gọi từ `LibraryView.delete`. Test erase giữ sách khác và store mới. Commit `1be3691`.
- [x] **P2 · T22:** đã sửa cùng highlight/audio (commit `5865312`) — `SpeechController.playableChapter` bỏ chương rỗng cho auto-advance/next/previous, hết sách thì stop. Chưa có fixture chương rỗng để test runtime.
- [x] **P2 · T23:** đã sửa 2026-11-01 — `weekInterval` dùng thứ Hai cho cả `summary.weekMinutes` và `week`; test calendar bắt đầu Chủ nhật + ranh giới năm; full suite 114 tests, 5 skipped, 0 failures. Commit `44b17c0`.
- [x] **Player button 3 tab (T35–T37):** overlay dùng chung trong `MainView`, hiện khi có sách playing/paused, ẩn khi idle/reader mở/chưa có sách; đĩa + Reduce Motion + reserve đáy. Build + **119 tests** xanh; ảnh `build/qa/mini-player/`. Còn QA tap/VoiceOver/lock-screen thật; commit `97bbc33`.
- [x] **Title + cover thật/tự tạo (T38–T41):** đọc cover EPUB + nhận diện trang trắng, `BookCover` dùng chung (ảnh/gradient + title/author), Home shelf bỏ title ngoài, author ngoài chỉ bìa tự tạo, Library list cover 64. Build + **125 tests** xanh; ảnh `build/qa/book-covers/`. Còn VoiceOver thật, ảnh thật có chữ, BookEditSheet thật; commit `5288aef`.
- [x] **Tinh chỉnh title matrix (T42):** Home READING bỏ chữ trong cover, giữ ngoài; Home shelf bỏ author ngoài; Library list bỏ chữ trong cover cả hai loại; grid giữ. Build + **125 tests** xanh; ảnh `final-home-after-tweaks.png`, `final-library-list-after-tweaks.png`; commit `0a78999`.
- [x] **Giọng theo ngôn ngữ sách (T43):** bỏ dấu Latin phổ thông khỏi tín hiệu Việt; sách Pháp/Tây Ban Nha/Bồ Đào Nha giữ tag và dùng đúng giọng. Full **128 tests**; ảnh `build/qa/voice-lang/` (EN/FR/ES/JA/ZH/VI); commit `0a78999`.
- [x] **Mini player `- play/pause +` (T44):** đổi hai nút tốc độ sang minus/plus; ảnh `mini-player-minus-plus.png`; build succeeded; commit `0a78999`.
- [x] **P2 · T24:** paginator phủ đủ text — bỏ trần 4.000 trang (mỗi vòng luôn tiến ít nhất 1 ký tự) và thêm trang cuối cho phần CoreText không vẽ được. Test 4.100 dòng phủ 8.200/8.200 (trước: 8.000/8.200). Commit `f1a64ef`.

## Tối ưu có căn cứ

- [x] **I-OPT-01 · cache page controller/snapshot:** `trimCache(around:)` giữ cửa sổ ±4 trang quanh trang đang đọc, gọi sau mỗi lượt lật; UIKit vẫn giữ controller đang bay trong animation. Commit `e6b6a5e`. Chưa đo memory khi lật 500 trang (cần thiết bị/Instruments).
- [x] **I-OPT-02 · ZIP index một lần:** central directory đọc ở `init` thành `allEntries` + `entriesByName` + `names`; lookup là tra dictionary, `allEntryNames()` dùng lại mảng tên. Test trùng tên vẫn lấy entry đầu. Commit `8f5818f` (cùng T19) + test `63a6358`.
- [x] **I-OPT-03 · không re-read cả kệ cho một edit/mark:** `parsedPackage` cache theo id + `contentModificationDate`; xoá cache khi import thay file và khi xoá sách. Test: file không đọc được (chmod 000, mtime không đổi) vẫn giữ title đã parse khi đổi tên sách khác. Commit `d159223`.
- [x] **I-OPT-04 · signpost cho restyle/pagination/search:** `OSSignposter` category `reader`, interval `restyle`, `paginate`, `search`. Commit `7c88d03`. Chưa đo trên máy thật — số đo và quyết định tối ưu thêm vẫn cần Instruments/device.

## Kiểm chứng và tính đầy đủ chức năng

- [ ] Khôi phục generator/corpus dùng chung với Android ở vị trí bền vững, không nằm duy nhất trong `build/qa`. `EpubCorpusTests` hiện skip vì thiếu manifest (suite 2026-11-11: 140 tests, **5 skipped**, 0 failures — đúng 5 test corpus bị skip). Gate QA đầy đủ cần bắt thiếu fixture rõ ràng.
- [x] Chạy lại suite trên simulator có thật (2026-11-11): `xcodebuild test -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -derivedDataPath build/dd` → `** TEST SUCCEEDED **`, 140 tests, 5 skipped, 0 failures (iPhone 17 Pro, iOS 27.0, Xcode 27.2).
- [ ] QA thiết bị/simulator: import lỗi, Unicode search, delete/reimport, sách dài, chương rỗng, page curl sau jump/restyle, TTS pause/seek/timer; ghi rõ phần nào đã test và phần nào chỉ code review.
- [x] Đã làm highlight chọn đoạn + audio nền (T25–T34 trong `docs/TASKS.md`; prompt gốc đã xoá ở `edd6de4`): selection/popup/4 màu/sửa-xoá/3 tab, `SpeechController` dùng chung, `UIBackgroundModes audio`, now-playing + remote commands, activation route + URL scheme, `BookLanguage`. Build + 112 tests xanh; ảnh/log QA trong `build/qa/highlight/`. Còn QA ngón tay thật cho pan thanh kéo, swipe xoá, lock-screen controls, đổi sách trong một phiên và hai cột iPad.
- [x] Đã làm mới README/state (2026-11-11): README hết ghi "chưa có test target"/"ảnh EPUB chưa hiển thị", có lệnh `xcodebuild test`, Xcode 27.2 / iOS 27.0, bố cục `App/Home/Store` hiện tại; state trỏ về `docs/` và ghi số test hiện tại.
- [ ] Các task cũ T11/T13/T14 giữ trạng thái cũ cho đến khi có bằng chứng mới; review này không giải quyết icon, độ đậm trang đầu chương hoặc quyết định ánh nếp gấp.

## Giới hạn review

Đợt review 2026-10-29 đã rà luồng chức năng/code quan trọng, chạy probe macOS dùng code ZIP/stats/paginator và kiểm offset Unicode. Bổ sung 2026-11-11: đã chạy full unit suite trên simulator (140 tests, 5 skipped, 0 failures) sau khi sửa T18–T21, T24 và 4 mục tối ưu; vẫn chưa chạy Instruments, accessibility, audio hardware hay QA thiết bị thật; chưa review từng dòng theme/view trang trí/gesture. Ưu tiên bảo toàn sách khi import và trần bộ nhớ ZIP trước tối ưu tốc độ.
