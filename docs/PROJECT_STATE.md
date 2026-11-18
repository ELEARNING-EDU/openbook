# Project state

## Current state — 2026-11-11

- Worktree `ios` of `/Users/cuong/Workspace/Projects/OpenBook` (siblings `master`, `android`,
  `web`); branch `ios`; HEAD `7c88d03` "perf: signpost restyle, pagination and search". App, tests
  and config are clean.
- Done and committed: T01–T10, T12, T15–T17, T22–T44, SUITE-003, SUITE-009, reading stats (plan
  2026-10-25 — all ten tasks, commits `3640250`…`5eb85a6`) and Home sections (2026-10-26,
  `5288aef`).
- Fixed on 2026-11-11: the five review defects T18–T21 and T24 (`331d05e`, `8f5818f`, `2e9a362`,
  `1be3691`, `f1a64ef`) and the four optimisations I‑OPT‑01…04 (`e6b6a5e`, `8f5818f`, `d159223`,
  `7c88d03`): staged import with atomic replace, ZIP allocation ceilings, UTF‑16 offsets for
  search/bookmarks, bookmarks and highlights erased with the book, pagination to the last
  character, bounded curl cache, cached package metadata, signposts on restyle/pagination/search.
- Open: T11 app icon, T13 chapter-opening weight, T14 dark-fold decision; the shared corpus
  manifest is still missing (5 tests skip); device-only QA (VoiceOver, lock-screen controls,
  mini-player tap, real `BookEditSheet`, a printed cover, selection drag, memory after 500 turns,
  Instruments numbers for I‑OPT‑01/04) is still owed. `TODO.md` at the repo root tracks it.
- Not verified by hand yet: VoiceOver, lock-screen/headphone controls, mini-player tap, real
  `BookEditSheet`, a real cover with a printed title, selection-handle drag with a real finger.
- Verification on 2026-11-11: `xcodebuild test -project EpubReader.xcodeproj -scheme EpubReader
  -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro'
  -derivedDataPath build/dd` → `** TEST SUCCEEDED **`, **140 tests, 5 skipped (corpus manifest
  missing), 0 failures** (iPhone 17 Pro, iOS 27.0, Xcode 27.2). Each fix had its own failing test
  watched first (the long-chapter test reported 8.000/8.200 characters, the same-name import lost
  the old file, the offset test read 100 instead of 200, and the shelf test dropped a parsed title
  when the file was unreadable).
- Docs live in `docs/` only; the root `PROJECT_STATE.md`/`TASKS.md`/`SESSION_HANDOFF.md`/`README.md`
  are deleted in the working tree and `TODO.md` is untracked. The rest of the dirty tree is
  toolkit only (`.ai-toolkit/`, `.agents/`, `.claude/` deletions, `AGENTS.md`, `CLAUDE.md`,
  untracked `.commandcode/`). Do not stage or discard any of it without the owner.
- Docs in this update: the finished reading-stats spec and plan (`docs/superpowers/`) were deleted
  — recoverable from `5b246b4`; `README.md` and `TASKS.md` refreshed to the current state.
- QA artifacts on this machine: only `build/qa/{book-covers,highlight,mini-player,voice-lang}`;
  older screenshot folders referenced below (`reading-stats`, `home-sections`, `SUITE-009`,
  `epub-corpus`, `kilocode-*`) are gone or lived on the previous machine.
- Next action: owner decides T11/T13/T14 or the device QA list; the `ios` branch has no remote
  and nothing has been pushed.

## 2026-11-02 — state before handoff/compact

- Worktree `ios/`; branch `ios`; HEAD `0a78999` "fix: read each book in its own voice and
  tidy the cover titles". App code tree clean.
- Latest commits: `0a78999` (T42–T44), `5288aef` (covers + Home baseline), `97bbc33` (mini
  player), `44b17c0` (T23), `5865312` (highlight + background audio).
- Build: `xcodebuild build` succeeded. Full suite: **128 tests, 5 skipped, 0 failures**
  (iPhone 18 Pro, iOS 27.0; full run at 15:08, the later minus/plus icon change was build-only).
- Done: T23; T25–T34; T35–T37; T38–T41; T42–T44; T22 (empty chapters) fixed in `5865312`.
  Pending review defects: T18–T21, T24. Remaining manual QA: VoiceOver, lock-screen/headphone
  controls, tapping the mini player, real `BookEditSheet`, a real cover with printed title.
- Dirty tree is documentation/toolkit only: root `PROJECT_STATE.md`/`SESSION_HANDOFF.md`/
  `TASKS.md`/`README.md` deleted in favour of untracked `docs/` versions; `.commandcode/`,
  pre-existing `.ai-toolkit`/`.claude`/`.agents` deletions, `AGENTS.md` modified, `CLAUDE.md`
  deleted. Do not stage/discard without the owner.
- Simulator: iPhone 18 Pro UDID `0AD96A11-BAFB-468D-A68D-4E545EF58BE8`, iOS 27.0, app
  installed/running; last visible state is the Vietnamese reader with mini player
  `− play/pause +`. QA fixtures/scripts are in `build/qa/` (gitignored); the white fixture was
  renamed "A Brand New Title" by a demo hook.
- Next action: owner review/QA; decide whether to commit the docs move; then remaining manual
  QA/tests above. No remote, nothing pushed.

## Review checkpoint — 2026-10-29 (code không đổi)

- Đã hoàn tất review luồng chức năng và code quan trọng; thêm 7 lỗi Pending (T18–T24) trong TASKS.md và TODO.md riêng cho ưu tiên sửa/tối ưu/QA. Chi tiết tình huống, vị trí code, mức bằng chứng và nghiệm thu ở TASKS.md.
- Chỉ cập nhật tài liệu. Không stage/commit, không sửa app/test/config, không cài app hoặc thay dữ liệu người dùng. Git root `master`, HEAD `c6633fb`; android/ios/design và các file untracked có sẵn được giữ nguyên; khi tiếp tục xuất hiện thêm web/ từ công việc khác, không đụng tới.
- Khi tiếp tục, tài liệu Android đã nằm ở android/docs/; giữ vị trí hiện hữu, không tạo lại bản root. Đối chiếu SHA-256 của 135 source/resource/test/config với snapshot review: không khác.
- Verification mới: probe Kotlin tìm kiếm tái hiện lỗi Đ/NFD; probe Swift/macOS tái hiện lệch tuần, offset Unicode và mất đuôi sau 4.000 cột, đồng thời xác nhận ZIP không chặn expansion 8 MiB. Compile bản cuối/run đều exit 0; đây là tái hiện lỗi, không phải app test pass. Probe tạm ở /tmp/openbook-audit, có thể mất sau reboot; kết quả và cách dựng được ghi trong TASKS.md.
- Không chạy Gradle/xcodebuild/full test/lint/device QA mới. Corpus chung hiện thiếu manifest; kết quả QA trước là lịch sử.
- Bước tiếp theo: đọc TASKS.md phần Review codebase và TODO.md, chọn nhóm P1 để xử lý khi có yêu cầu sửa. Trước sửa đối chiếu lại source/Git; hiện phạm vi được giao chỉ review, nên tất cả defect vẫn Pending.

Updated: 2026-10-23 09:35 +07 (Asia/Ho_Chi_Minh)

## 2026-10-23 — Main shell: Home / Library / Store + floating tab bar
- `MainView` is the app root. Exactly three tabs (Home, Library, Store); the initial tab is Home.
  A compact, leading-aligned capsule bar floats 12 pt above the bottom safe area, leaving free
  room on its right; each scrolling screen reserves 104 pt under its content. The bar is hidden
  while a book is open.
- Home lists only genuinely in-progress books (`0 < fraction < 1`), newest first, from
  `ReadingProgressStore` via `HomeFeed.continueReading`; empty state links to Library; tapping
  opens the existing `ReaderView`, which restored page 10/13 from saved progress.
- Library keeps EPUB import and reader entry and adds a persisted list/grid switch
  (`@AppStorage("libraryLayout")`), reusing the existing cover and progress row. Persistence was
  verified across a relaunch (`build/qa/library-list-persisted.png`).
- Store is a static demo notice: no server, purchases or catalogue.
- Tests: 4 new `HomeFeedTests`; `xcodebuild test` → 17 tests, 0 failures (iPhone 17 Pro, iOS 26.5);
  `xcodebuild build` → `** BUILD SUCCEEDED **`.
- KiloCode independent QA: code/test report `build/qa/kilocode-code-qa.md` (no blockers/majors;
  two minor accessibility/contrast findings fixed and retested); visual device report
  `build/qa/kilocode-device-visual-qa.md` (6/6 PASS); screenshots `build/qa/kilocode-device-*.png`
  and final `build/qa/final-home.png`.
- App left running on Home in the booted iPhone 17 Pro simulator (UDID 6B98AE70…). The machine
  has no Simulator.app GUI (headless simctl only), so screenshots are the visual evidence.
- Source and tests are uncommitted; no commit was made.

## 2026-11-02 (tiếp 3) — Mini player `- play/pause +` (committed `0a78999`)

- Trong `ReaderView.playerControls`, hai nút tốc độ `gobackward`/`goforward` đổi thành
  `minus`/`plus`; hàng hiển thị `title · 1× − play/pause + ×` (nút dừng giữ nguyên).
  Accessibility vẫn là `reader.slower`/`reader.faster`.
- Ảnh `build/qa/voice-lang/mini-player-minus-plus.png`; build `** BUILD SUCCEEDED **`.
  Không đổi engine TTS/logic đọc. Commit `0a78999`.

## 2026-11-02 (tiếp 2) — Giọng đọc theo ngôn ngữ sách (committed `0a78999`)

**Nguyên nhân:** `BookLanguage.vietnameseSignals` cũ tính cả các dấu Latin phổ thông
(`é`, `è`, `ê`, `à`, `ù`, `ô`, `á`, `í`…). Sách Pháp/Tây Ban Nha có nhiều dấu này nên
`strongLanguage` trả `vi`, đè lên tag `fr`/`es`; `bookLanguage` thành `vi`, voice Linh được
chấp nhận và Voice picker lọc còn nhóm Việt Nam.

**Sửa:** bộ dấu chỉ còn ký tự đặc trưng Việt: `ă`, `đ`, `ơ`, `ư`, các nguyên âm dấu hỏi/nặng,
và tổ hợp thanh trên `ă/â/ê/ô/ơ/ư`; bỏ các dấu sắc/huyền/mũ trơn dùng chung với Pháp/Tây Ban
Nha/Bồ Đào Nha. Thêm test `fr`, `es`, `pt` giữ tag; test tiếng Việt cũ vẫn xanh.

**Kiểm chứng:** `BookLanguageTests` 11; full suite **128 tests, 5 skipped, 0 failures**
(2026-11-02 15:08 +07). QA `build/qa/voice-lang/`:
- trước sửa: `french-book.png` — sách Pháp `La Lettre`, Voice sheet chỉ có
  `VIETNAMESE — Linh (vi-VN)`; đây là lỗi anh báo;
- sau sửa: `french-after-fix.png` → `Amélie [fr-CA]`; `spanish-after-fix.png` → `Mónica
  [es-ES]`; `japanese.png` → `Kyoko [ja-JP]`; `chinese.png` → `Meijia [zh-TW]`;
  `vietnamese.png` → `Linh [vi-VN]`; `english-book.png` → `Samantha [en-US]`.
- Sách Tây Ban Nha `state idle` trong ảnh vì chương cuối đã đọc xong; dòng `voice Mónica`
  vẫn xác nhận ngôn ngữ.

**Committed:** `BookLanguage.swift` + `BookLanguageTests.swift` cùng T42/T44 trong `0a78999`.

## 2026-11-02 (tiếp) — Title sách + bìa thật/bìa tự tạo (T38–T41 committed `5288aef`; T42 committed `0a78999`)

**Khảo sát:** iOS trước đây chỉ có `BookCover` gradient sinh từ title, **không có bìa thật**;
`HomeBookRow` in title ngoài + author ngoài cho mọi sách; `ContinueReadingCard` đã có title/
author ngoài; Library grid không title/author ngoài; Library list có title/author/progress
ngoài (cover 58). `BookItem`/`LibraryMetadata` không có cover; `EpubArchiveReader` chưa đọc
`<meta name="cover">`/`properties="cover-image"`.

**Đã làm:**
- `EpubArchiveReader`: đọc `coverHref`/`coverID`, fallback id/href chứa "cover"; thêm
  `coverData(url:)` (trần 8 MB), không đổi `read`/`readMetadata`/`validate`.
- `Domain/BookCoverImage.swift`: decode + nhận diện trang trắng; heuristic: ít pixel không
  trắng (<10%) **và** độ bão hòa trung bình <0.15 → blank. Bản đầu (chỉ đếm pixel gần trắng
  >98%) đã để lọt trang title có vạch đen (~3% đen); đã sửa và thêm test.
- `Library/BookCoverStore.swift`: cache lazy theo `id + contentModificationDate`, coalesce
  in-flight, state `.unknown/.generated/.image`; không persist cover.
- `BookCover` dùng chung: ảnh thật nếu có (không chồng title/author); nếu không, gradient từ
  title + title trắng bold (4 dòng, ellipsis) + author trắng 78% (2 dòng), gáy/viền/shadow,
  `.accessibilityHidden(true)`.
- `HomeBookRow`: bỏ title ngoài; author ngoài chỉ khi `.state != .image` và author không rỗng;
  subtitle giữ. `HomeView` ContinueCard label đổi sang `home.book.a11y` (title, author).
  `LibraryView.listRow` cover 58 → 64; label unfinished dùng `home.book.a11y`; bỏ `percent(for:)`.
- QA hook `-demoRenameBook/-demoRenameTitle` để chụp title edit.

**Kiểm chứng:** build `** BUILD SUCCEEDED **`; full suite **125 tests, 5 skipped, 0 failures**
(2026-11-02 11:12 +07). Simulator **iPhone 18 Pro, iOS 27.0**. Ảnh `build/qa/book-covers/`:
- `generated-home/library-grid/library-list` (bìa tự tạo trước fixture);
- `covers-library-grid` (bìa thật giữ ảnh; bìa trắng → bìa tự tạo sau khi sửa heuristic);
- `covers-home-shelf` (bìa thật không title/author ngoài; bìa tự tạo title trong + author ngoài);
- `covers-home-card-real/white` (title/author ngoài cả hai loại; title dài 2 dòng ellipsis);
- `covers-library-list` (title/author/progress ngoài; cover 64);
- `covers-title-edited` (đổi title bìa tự tạo → gradient/chữ đổi ngay);
- `covers-library-dark` (chữ cover trắng, nền tối).
Fixture/script trong `build/qa/book-covers/`: `zz-real-cover.epub`, `zz-white-cover.epub`,
`make_cover_books.py`; đã copy vào simulator.

**Tinh chỉnh theo phản hồi (2026-11-02):** Home READING dùng
`BookCover(showsGeneratedText: false)` — bìa tự tạo chỉ còn gradient, title/author **ngoài**
giữ nguyên; các shelf khác bỏ dòng author **ngoài** (bìa tự tạo vẫn in title/author bên trong,
bìa thật không in); Library list dùng `showsGeneratedText: false` cho cả hai loại — title/
author/progress nằm ngoài; Library grid giữ nguyên. Ảnh `final-home-after-tweaks.png`,
`final-library-list-after-tweaks.png`; full suite **125 tests**. Commit `0a78999`.

**Chưa kiểm runtime:** VoiceOver đọc `"<title>, <author>"` (label/cover-hidden đã review code,
chưa bật VoiceOver headless); title edit bằng `BookEditSheet` thật (đã kiểm bằng demo hook cùng
`updateMetadata`); ảnh bìa thật của fixture là gradient trừu tượng nên title không nằm trong
ảnh — quy tắc "không chồng title lên ảnh" vẫn đúng, nhưng ảnh thật có chữ cần thử với sách thật.

**Baseline:** Home-sections baseline + cover T38–T41 commit `5288aef`; T42–T44 commit
`0a78999`; các file docs state vẫn untracked.

## 2026-11-02 — Nút player nổi trên cả 3 tab (committed `97bbc33`)

**Khảo sát:** iOS trước đây **không có** nút player ở Home; chỉ có player row trong
`ReaderView`. Đây là overlay dùng chung mới ở `MainView`, không phải di chuyển nút có sẵn.

**Đã làm:**
- `EpubReader/App/MiniPlayerButton.swift`: nút 56 pt, đĩa `VinylCover` 48 pt, góc dưới-phải,
  cách tab bar 8 pt; đĩa quay khi `SpeechController.state == .playing`, dừng đúng góc khi
  paused; `@Environment(\.accessibilityReduceMotion)` tắt quay khi Reduce Motion.
- `MainView`: một chỗ render duy nhất; `MiniPlayerVisibility` quyết định hiện/ẩn (chưa mở sách,
  idle, reader mở đều ẩn); bấm gọi `openPlayingBook()` (đúng sách/vị trí); truyền
  `playerInset` để 3 tab chừa đáy.
- `HomeView`/`LibraryView`/`StoreView`: cộng `playerInset` vào reserve đáy khi nút hiện.
- Accessibility: label `reader.player.open` ("Mở trình phát"/"Open player"), value
  `reader.player.state.playing`/`.paused`; thêm 2 key en/vi.
- Hook QA `-demoSwitchTabAfter`/`-demoSwitchTab` để chụp đủ 3 tab.

**Kiểm chứng:** build `** BUILD SUCCEEDED **`; full suite **119 tests, 5 skipped, 0 failures**
(2026-11-02 08:06 +07). Ảnh QA `build/qa/mini-player/`: playing/paused trên Home, Library,
Store; stopped; no-book; reader-open; `final-home-paused`. Reduce Motion: 2 ảnh cách 1,5 s,
crop vùng nút trùng SHA-256 `937dadd0…` → đĩa không quay. Simulator: **iPhone 18 Pro, iOS 27.0**.

**Chưa kiểm bằng tay:** bấm nút thật để mở sách (hàm `openPlayingBook` đã được kiểm ở luồng
activation trước đó), VoiceOver đọc runtime, pause/play từ lock screen/nút tai nghe thật
(`-demoPause` dùng cùng `SpeechController.togglePlayPause`). Nút đĩa reset góc khi reader
mở/đóng vì view được tạo lại; trong lúc nút hiện, pause dừng đúng góc.

**Dirty:** `MainView.swift`, `HomeView.swift`, `LibraryView.swift`, `StoreView.swift`,
2 `Localizable.strings`, mới `MiniPlayerButton.swift`, `MiniPlayerVisibilityTests.swift`;
committed `97bbc33`; docs state chưa commit.

## 2026-11-01 — Highlight chọn đoạn + audio nền (committed `5865312`)

**Phạm vi đã làm (đúng hai phần của prompt):**
- Chọn từ bằng nhấn giữ: band theo line fragment + hai thanh dọc, kéo thanh theo ranh giới từ, giữ
  vùng chọn sau khi nhấc tay, chạm ngoài huỷ; đang chọn thì khoá lật trang và seek TTS; bắt đầu
  nhấn giữ thì tạm dừng TTS (không tự phát lại).
- Popup neo tại vùng chọn (không bottom sheet): hàng gọn `Highlight · Translate · Search · ›`;
  `›` mở danh sách dọc có icon; chỉ Highlight hoạt động; 4 màu; không toast; popup kẹp trong màn hình.
- Highlight đã lưu: đúng ARGB, đúng range ký tự, vẽ dưới vùng chọn, sống qua lật trang/đóng mở/đổi
  theme/đổi cỡ chữ; nhấn giữ highlight cũ → `Highlight · Remove` (Remove đỏ); đổi màu hoặc xoá.
- Sheet Nội dung 3 tab native: Contents · Bookmarks (n) · Highlights (n); tab Highlights có tên
  chương accent + text 2 dòng + số trang; trượt trái xoá bookmark/highlight; tab rỗng đúng câu.
- Dữ liệu: `chapterIndex`, `startOffset`, `endOffset` (UTF-16), `text`, `color` (ARGB), `createdAt`;
  id ổn định `chapterIndex-startOffset-endOffset`. iOS chưa từng lưu highlight theo câu nên
  migration là no-op.
- `SpeechController` thành phiên dùng chung (`static shared`): audio tiếp tục khi rời màn đọc,
  khi app ra nền; mở lại đúng sách không restart; sách khác dừng sách cũ trước.
- `AVAudioSession` `.playback`/`.spokenAudio` + `UIBackgroundModes: audio` (Info.plist thật);
  `MPNowPlayingInfoCenter` (bìa gradient + tên sách + chương) và `MPRemoteCommandCenter`
  play/pause/next/previous; pause giữ now-playing, stop xoá hẳn.
- App activation mở lại đúng sách đang đọc (iOS không có callback riêng cho tap now-playing);
  thêm URL scheme `epubreader://book/<file>` + handler `onOpenURL`.
- Ngôn ngữ: `BookLanguage` ưu tiên tín hiệu mạnh từ nội dung (dấu Việt/kana/Hán), text mơ hồ giữ
  `dc:language`; `EpubArchiveReader` parse thêm `dc:language`; `SpeechController` reset theo sách.
- T22 (auto-advance/skip qua chương rỗng) được sửa cùng `playableChapter`.

**Kiểm chứng thực tế:**
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug -destination
  'generic/platform=iOS Simulator' -derivedDataPath build/dd build` → **BUILD SUCCEEDED**, không
  warning từ code dự án.
- `xcodebuild test -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -derivedDataPath build/dd` →
  **114 tests, 5 skipped (corpus thiếu manifest), 0 failures** (2026-11-01 19:31 +07).
- T23 đã sửa: `weekInterval(now:calendar:)` dùng `firstWeekday = 2` cho cả `summary.weekMinutes`
  và `week`; test calendar bắt đầu Chủ nhật + ranh giới năm 29/12/2025 – 4/1/2026.
- Info.plist đã build có `UIBackgroundModes = [audio]`, `CFBundleURLTypes` scheme `epubreader`.
- Ảnh QA trong `build/qa/highlight/`: `01-selection`, `02-saved`, `03-highlights-tab`,
  `04-edit-popup`, `05-colors`, `06-recoloured`, `07-removed`, `08-expanded`, `09-tts-paused`,
  `10-extended`, `11-background-library`, `12-after-foreground`, `13-paused`, `14-deeplink`,
  `15-selection-after-refactor`, `16-outside-tap`, `17-saved-edit-final`.
- Log runtime: `speak chapter 1…4`, `nowplaying … artwork true` theo chương; audio chạy tiếp khi
  ở Library và khi Settings foreground; foreground lại có `[route] opening the book the voice is
  reading` và reader mở đúng chương 4/trang 8 với `spoken 298+137`; `stop` → `nowplaying cleared`;
  next/prev/pause đổi chương và `nowplaying paused`.

**Hạn chế / chưa kiểm bằng ngón tay thật:**
- Máy này không có `serve-sim`/`idb`/`cliclick` và simulator không có GUI; các thao tác chọn dùng
  móc demo gọi đúng hàm xử lý (`beginSelection`, `handleMoved`, `emitTap`), còn việc nhận diện
  pan của thanh kéo chỉ được review code, chưa kéo bằng ngón thật.
- iOS không cho app callback khi chạm now-playing; app dùng activation (đã kiểm). Deep link URL
  scheme được hệ thống nhận (hiện hộp “Open in Books?”) nhưng không chạm được nút Open trong
  simulator headless; handler `onOpenURL` được review code.
- Quy tắc “sang sách khác thì dừng sách cũ” nằm trong `SpeechController.openBook`; móc demo đổi
  NavigationStack không điều khiển được do state cũ, nên đường này được review code + `stop()`
  runtime qua nút ✕, chưa đổi sách thật trong một phiên.
- Thanh kéo hai cột đã tính theo offset ký tự (không dùng first/last rect) nhưng chưa kéo thử trên
  iPad; iPhone một cột đã kiểm.
- Listening time của thống kê vẫn chỉ cộng khi màn đọc mở (hành vi cũ), dù audio tiếp tục ở nền.
- Commit `5865312`. `Info.plist` là file thật ở root; target app đã bỏ `GENERATE_INFOPLIST_FILE`.

> English first, bản tiếng Việt ở nửa dưới của cùng tệp.

Goal / acceptance criteria:
- Native iOS demo EPUB reader (SwiftUI shell + UIKit page curl) for the Vietnamese sample
  book "Ngọn Hải Đăng Cuối Cùng" (13 pages at 19 pt on iPhone 17 Pro), the iOS twin of
  `epub-reader-android`. Vietnamese UI labels, no third-party dependencies.
- Acceptance criteria approved round by round in chat (2026-10-18 … 2026-10-20):
  1. Import an EPUB; library grid of generated covers; bundled sample seeded on first launch.
  2. Page turning with the system `UIPageViewController` page curl (`.pageCurl`,
     `isDoubleSided = true`, `spineLocation = .min`): fold pinned to the spine, follows the
     finger, back of the sheet painted with the theme's paper plus a faint mirrored
     print-through. A hand-drawn curl was rejected ("Curl thế này thì xấu lắm").
  3. Chrome: floating toolbar (back + contents left, search + bookmark right, running head
     centred), auto-hides in 4.5 s; tap zones left third = previous page, right third =
     next page, middle = toggle chrome; while narration plays the sides reveal chrome and
     the middle seeks the narration.
  4. Read-aloud: headphone button unfurls a player (book title, chapter, slower,
     play/pause, faster, stop, speed label, progress line flush with the bottom edge of
     the capsule). While the toolbar is up the player shortens itself (title, play/pause,
     stop) so the settings button keeps its corner.
  5. Settings: half-height sheet closed by dragging, no "Xong" button; theme grid with "*"
     only for themes changed in Tuỳ chỉnh (never for panel A/A size changes); customise
     screen with its own reset.
  6. Reading place persisted per book (chapter + character offset) and resumed on open;
     library shows the percentage read per book.
  7. Back button obeys the same show/hide rule as the other toolbar buttons: never alone,
     never removed.
  8. Behaviour verified on the simulator with injected touches/swipes (idb + `simctl`),
     not from screenshots alone.

Approved scope and gates:
- Owner approvals are conversational, one round at a time; no written ticket exists:
  - 2026-10-20: settings-panel contrast, player expands/collapses, player steps aside for
    the toolbar (later replaced, see below), swipe matrix fixes at first/last page.
  - 2026-10-20: back button must hide together with the other buttons, not be deleted;
    page that "turned itself back" must be fixed (commit c999ad0).
  - 2026-10-20: reading position saved and resumed; library shows % read (commit 458ae98).
  - 2026-10-21: adopt AI Development Toolkit — survey first, bilingual state files at the
    project root, keep `--cli all`, add a README, then land it as one commit (approved).
  - 2026-10-22: the iOS project now lives in the OpenBook repository as worktree `ios`; the
    whole tree (code, docs, toolkit) is committed there as one baseline (approved, no push).
  - 2026-10-22: T15 — unit-test target for `Paginator`, `EpubArchiveReader`, `ReadingProgress`;
    the smoke UI test was dropped by the owner ("không cần"). `xcodebuild test` must be green.
- Superseded decision: "no way out of the book while the voice reads" (commit caaec44) was
  withdrawn by the owner on 2026-10-20 ("ẩn cùng với nút khác chứ có bảo là xoá nó đi đâu")
  and replaced by "back hides and returns with the toolbar" (commit c999ad0).
- Not approved: publishing (no remote configured, nothing pushed), refactoring, architecture
  migration, iPad-specific layout, UI-test target, Android changes from this project.

Architecture decisions:
- Layers as they exist (`EpubReader/App`, `Data`, `Domain`, `Library`, `Reader`). Pragmatic
  Clean Architecture: `Domain` holds models and pure algorithms (`AttributedBuilder`,
  `Paginator`) and imports UIKit for `NSAttributedString` only; `Data` reads EPUB/ZIP;
  presentation is SwiftUI plus one `UIViewControllerRepresentable` for the pager. No
  repository protocols, no DI container: stores are `ObservableObject` singletons or
  `@StateObject` at the view that owns them (per adt-ios-clean-architecture: do not add
  layers that the project does not need).
- EPUB parsing: `EpubArchiveReader` reads `META-INF/container.xml` → OPF → spine, TOC from
  EPUB3 `nav` / EPUB2 `toc.ncx`; chapter XHTML → `NSAttributedString` via HTML import on
  the main thread, therefore split into `RawChapter` (bytes, read off-main) and `Chapter`
  (attributed, converted on main).
- Pagination: CoreText framesetter per chapter (`Paginator`), identical fonts and geometry
  to the page renderer (`ReaderPageViewController`), so a page is a string range that can be
  mapped back to chapter coordinates.
- Pictures: `EpubArchiveReader` resolves every `<img src>` of a chapter against the archive while
  the zip is open (case-folded lookup, `..`, `%20`, `?v=`; caps of 8 MB per picture and 24 MB per
  chapter) and hands the bytes to `RawChapter`. The HTML importer drops `<img>` — it has no base
  URL — so `ChapterImages` swaps each tag for an ASCII token before the import, and
  `AttributedBuilder` replaces the token afterwards with an `NSTextAttachment` cut to the column
  (never upscaled, at most 55 % of the column height) or, when the picture is missing or
  undecodable, with its alt text. A `CTRunDelegate` reports the picture's size to CoreText:
  without it the paginator measures the placeholder character and the text view then clips the
  picture. A change of column width re-typesets the book (`builtWidth` in `ReaderViewModel`).
- Curl model: a double-sided `UIPageViewController` walks *sides*, not pages (front N → back
  N → front N+1). `SheetBackViewController` paints the theme's paper with a 9 % mirrored
  snapshot of its front. Programmatic turns pass `[front, back]`; every landing calls
  `settle(on:)` to stand the book back on one side. `turning` / `waitingPage` defer pages
  requested mid-gesture; `turnID` stops an interrupted curl from settling on its older page;
  a rebind naming the page already on screen is ignored (commit c999ad0).
- Persistence: `UserDefaults` for reader settings (`reader*` keys), narration rate
  (`narrationRateFactor`), bookmarks and `readerPositions` (JSON); books live in
  `Documents/Books`; the bundled `sample.epub` is seeded once.
- Demo harness in-app: launch-argument hooks (`-demoOpenBook`, `-demoPage`, `-demoSpeak`,
  `-demoDark`, `-demoSettings`, `-demoDebug`, `-demoCurl`, `-demoDrag*`,
  `-demoRebindOnTurn`, …) drive deterministic states for simulator verification.
- Appearance is app-wide and reader-owned: `ReaderSettings.shared` is one object for the whole
  process, and the library reads it, so the shelf's paper, ink, accent and hairlines are the
  palette in force (Android does the same with its process-wide `ReaderSettings` →
  `ReaderTheme`). `preferredColorScheme` follows the palette's own dark state, because the
  reader's day/night is an explicit setting rather than the system appearance.
- Working rules: the project now carries AI Development Toolkit v1.1.0 (`AGENTS.md`,
  `.agents/skills`, `.opencode/commands`, `.ai-toolkit`); work is sequential in one agent,
  multi-step work needs an approved plan before code, commits stage explicit paths only.

Verified complete:
- T01–T10, T12, T15 and T16 (see TASKS.md) are implemented and verified, and all of it is
  committed: it arrived in the OpenBook repository as baseline commit `e727837` ("Initial import:
  EpubReader for iOS", 2026-10-22), which also carries SUITE-003 and SUITE-009 because the
  standalone `EpubReader-iOS` repository ended at `e224196` with those files still in its working
  tree.
- SUITE-003 (the library prints on the reader's palette) and SUITE-009 (paused narration taps) are
  implemented, verified and committed in that baseline.
- T15 (unit tests) is implemented and verified in the working tree; the commit needs the owner's
  go-ahead.

In progress / pending / blocked:
- In progress: none.
- Pending: T11 app icon, T13 chapter-opening page weight, T14 dark fold sheen decision.
- Blocked: none.

Git branch / HEAD / dirty files:
- The project is worktree `ios` of the OpenBook repository (parent
  `/Users/cuong/Workspace/CODE/AI/Orca/OpenBook`, siblings `master` and `android`). Branch `ios`;
  baseline `e727837` "Initial import: EpubReader for iOS" (2026-10-22) on top of the empty
  `c6633fb Initial commit`. The older history (`e224196` back to `7cfd579`) still exists only in
  `/Users/cuong/Workspace/CODE/AI/OpenCode/EpubReader-iOS`; that copy is identical to this tree
  apart from `build/` and `.git`.
- T15 (2026-10-22) is in the working tree and not committed: new `EpubReaderTests/` (three test
  files), `EpubReader.xcodeproj/project.pbxproj` (test target), new shared scheme
  `EpubReader.xcodeproj/xcshareddata/xcschemes/EpubReader.xcscheme`, `Reader/ReadingProgress.swift`
  (`init(defaults:)`) and these state files.
- No remote; nothing is pushed. The auxiliary worktrees under `~/orca/workspaces/EpubReader-iOS/`
  no longer exist.

Verification:
- `xcodebuild test -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd`
  → `** TEST SUCCEEDED **`, 13 tests, 0 failures (2026-10-22 10:15 +07, Xcode 27.1). The app build
  is green again after the change (`** BUILD SUCCEEDED **`, the only `warning:` line is Apple's
  `appintentsmetadataprocessor` notice). Tests: `PaginatorTests` 4, `EpubArchiveReaderTests` 4,
  `ReadingProgressTests` 5.
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`, 0 warnings (2026-10-21 10:53 +07, Xcode 27.1; the only
  `warning:` line in the log is Apple's `appintentsmetadataprocessor` notice about no
  `AppIntents.framework` dependency).
- Simulator checks on 2026-10-20 (UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  swipe matrix one page per gesture both ways and at the first/last page, with and without
  narration; mid-drag rebind hook `-demoRebindOnTurn` (old code flipped back, current code
  does not); player, back button and library % (20 % → 36 % → 100 %); resume after terminate
  (page 3 → reopen page 3; 19 pt page 3 → 24 pt same sentence).
- Pictures, 2026-10-21 (same simulator): a generated test book `zz-anh-thu.epub` (one chapter, a
  900×300, a 600×400 and a 300×1200 PNG, plus an `<img>` whose file is absent) drawn as: 2 columns
  at 19 pt → images at 160 pt wide, the tall one capped at 353 pt (55 % of the 642 pt column) and
  on its own page, alt text `[Ảnh: Ảnh thiếu tệp]` for the broken `src`; 1 column → the same
  pictures at 342 pt; dark paper → pictures unchanged, alt text legible; page count 3 (2 columns)
  and 4 (1 column); `-demoPage 4` → quit → reopen page 4; sample book unchanged at 13 pages.
  Pixel rows were measured, not eyeballed: the 88 pt picture sits centred in its 160 pt column
  (image 478–735 px, column centre 603 px). Two regressions found and fixed on the way: the
  paginator ignored the attachment size (pictures were laid into space that did not exist and
  clipped), and a second book opened in the same view model kept the first book's chapters.
- `python3 .ai-toolkit/scripts/checkpoint.py .` → `master`, clean tracked tree (2026-10-21).
- SUITE-003, 2026-10-21 (iPhone 17 Pro, UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`. The bare destination without `OS=26.5` now fails
  (exit 70: it resolves to `OS:latest` = 27.0, and no iPhone 17 Pro exists on that runtime).
  Installed on the booted simulator and photographed the library (1206×2622, screenshots outside
  the repo in `…/T/epub-ios/`):
  - live repaint without relaunch (`-demoLibraryTheme focus -demoLibraryDark 1`): shelf background
    #FFFFFF (Original light) before the change, #2A2114 (Focus dark paper) after;
  - Paper light: background #FBF7EE (2,016,748 px), progress track #EFE9DC (3,586 px), accent
    #9E5932 (3,529 px), soft ink #6E6459 (819 px);
  - Paper dark: background #0F0D0B (2,198,333 px), track #1E1B17 (3,583 px), accent #D9A06A
    (3,529 px), soft ink #9C9385 (819 px);
  - reader spot check after the change (`-demoOpenBook 1 -demoPage 3 -demoTheme paper
    -demoDark 1`): page paper #0F0D0B, reading behaviour unchanged;
  - system appearance flipped light→dark with the shelf on Paper light: background stays #FBF7EE,
    because appearance is the reader's own day/night setting (Android does the same); the system
    chrome is pinned to the palette with `preferredColorScheme`.
- SUITE-009, 2026-10-22 (iPhone 17 Pro, UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`. Before/after runs of a pre-fix build and the fixed
  build (report: `build/qa/SUITE-009/REPORT.md`):
  - paused + tap on a sentence: before — nothing happened (`state paused`, `spoken 710+79`
    unchanged); after — `speak chapter 2 from 789`, `state playing`, `spoken 789+229`;
  - paused + tap on the blank margin: before — the page turned back (`Trang 5/13` → `4/13`)
    and the chrome stayed `playerOnly`; after — `chrome full`, `state paused` unchanged, same
    page;
  - paused + tap on blank paper inside a column: before — logged as `chapter 1018` (nearest
    sentence selected); after — logged as `blank`, chrome `full`, nothing selected;
  - regressions: playing + text tap still seeks (`speak … from 789`), idle + middle tap still
    toggles the chrome, idle + right-third tap still turns the page (`Trang 5/13` → `6/13`).
  - The finger itself is simulated: this machine's Xcode 27.1 install has no `Simulator.app`
    and no `idb`/`applesimutils`/`cliclick`, so the taps are the app's own demo hook
    (`-demoTapX/-demoTapY`), which calls the same `emitTap` the `UITapGestureRecognizer`
    calls. Everything downstream of the recogniser is exercised for real.
  - Repeated the same matrix on the **device hub** with real touches (`serve-sim` HID →
    the simulator's digitizer; normalized 0..1). Xcode 27's Device Hub had disconnected the
    legacy touch service — `serve-sim repair-input -d <udid>` (restarts SpringBoard, closes
    the running apps) brings it back; after that the hub's touches land. On the same page
    (index 4 of 13, chapter 2): paused + real tap on a sentence → the baseline build logs
    the tap and does nothing (`state paused`); the fixed build logs `speak chapter 2 from
    789 … wasPaused true` and the overlay goes `state paused` → `state playing`. Paused +
    real tap on the blank paper → the baseline build reports it as `chapter 1018` (the last
    sentence) and the chrome stays `playerOnly`; the fixed build reports `blank`, the chrome
    becomes `full`, the state stays `paused` and the page does not change. Regressions with
    real touches: playing + text tap still seeks, idle + middle tap still reveals the
    chrome, idle + right-third tap still turns the page (`Trang 5/13` → `6/13`).
- Unit tests exist and pass for `Paginator`, `EpubArchiveReader` and `ReadingProgress`; the app UI
  itself is only covered by the simulator checks above (the smoke UI test was dropped by the owner).

Known limitations:
- System light/dark does not repaint the library or the reader: appearance is the reader's explicit
  day/night setting, as on Android. The shelf keeps the reader's palette and pins the system chrome
  to it. Following the system needs an "automatic" appearance option — owner decision.
- The library's large navigation title and status bar are system chrome (label ink chosen by
  `preferredColorScheme`); the shelf's own text (percentages, empty state) uses the palette inks.
- Pictures are raster only (PNG/JPEG/GIF/WebP). SVG, `<object>`, `<image>` inside inline SVG and
  CSS background images are not drawn — an SVG shows its alt text.
- A block picture that does not fit the space left on a page starts the next page, so the foot of
  the previous page can be blank; pictures never split across pages.
- Picture bytes stay in memory while the book is open (8 MB per picture, 24 MB per chapter) and are
  decoded again when the column width changes; re-typesetting imports every chapter on the main
  thread, so a restyle of a large book pauses for a moment.
- No app icon: there is no asset catalog; only `Resources/sample.epub` ships.
- Chapter-opening pages render visibly heavier (page 2 bold vs page 3); cause not found.
- `ReaderView.swift` is 1309 lines and owns chrome, player, settings wiring and demo hooks;
  a split is a refactor that needs a separate approval.
- Unit tests cover the pure logic only; the UI is still checked by hand on the simulator (the smoke
  UI test was dropped by the owner on 2026-10-22).
- Progress percentage is measured at the foot of the visible page, so the first page of a
  13-page book reads ~8 % and the last page 100 %.
- Portrait only, iPhone and iPad (`TARGETED_DEVICE_FAMILY = 1,2`) with no iPad layout.
- Narration quality depends on the device's installed Vietnamese voices.

Exact next action:
- Ask the owner for the go-ahead to commit T15 (explicit paths, no push), then which of T11, T13 or
  T14 comes next — write that task's plan and wait for approval before touching code.

---

## Bản tiếng Việt

Cập nhật: 2026-10-22 10:20 +07 (Asia/Ho_Chi_Minh)

Mục tiêu / tiêu chí nghiệm thu:
- Ứng dụng đọc EPUB demo chạy native trên iOS (vỏ SwiftUI + lật trang bằng UIKit page curl)
  cho sách mẫu tiếng Việt "Ngọn Hải Đăng Cuối Cùng" (13 trang ở cỡ 19 pt trên iPhone 17 Pro),
  bản iOS song song với `epub-reader-android`. Giao diện tiếng Việt, không thư viện ngoài.
- Các tiêu chí được duyệt theo từng vòng trao đổi (2026-10-18 … 2026-10-20):
  1. Nhập được EPUB; thư viện dạng lưới với bìa tự sinh; sách mẫu được chép sẵn lần đầu mở app.
  2. Lật trang bằng page curl của hệ thống (`UIPageViewController`, `.pageCurl`,
     `isDoubleSided = true`, `spineLocation = .min`): nếp gấp bám gáy, theo ngón tay; mặt sau
     tờ giấy là màu giấy của theme kèm hình in xuyên qua rất mờ, lật ngược. Curl tự vẽ đã bị
     loại ("Curl thế này thì xấu lắm").
  3. Thanh công cụ nổi (back + mục lục bên trái, tìm kiếm + đánh dấu bên phải, tên chương ở
     giữa), tự ẩn sau 4,5 giây; vùng chạm: 1/3 trái = trang trước, 1/3 phải = trang sau, giữa =
     bật/tắt nút; khi đang đọc thành tiếng thì chạm hai bên để hiện nút, chạm giữa để nhảy tới
     câu tương ứng.
  4. Đọc thành tiếng: nút tai nghe mở player (tên sách, chương, chậm, phát/tạm dừng, nhanh,
     dừng, nhãn tốc độ, vạch tiến trình sát mép dưới của viên thuốc). Khi thanh công cụ đang
     hiện, player tự thu ngắn (tên sách, phát/tạm dừng, dừng) để nút cài đặt giữ nguyên góc.
  5. Cài đặt: bảng cao nửa màn hình, kéo xuống để đóng, không có nút "Xong"; lưới theme có dấu
     "*" chỉ cho theme đã sửa trong Tuỳ chỉnh (A/A ở bảng không đánh dấu); màn Tuỳ chỉnh có
     nút đặt lại riêng.
  6. Lưu vị trí đọc theo từng sách (chương + độ lệch ký tự), mở lại đúng chỗ; thư viện hiện
     phần trăm đã đọc của từng sách.
  7. Nút back tuân theo đúng quy tắc hiện/ẩn của các nút còn lại: không đứng một mình, không
     bị xoá.
  8. Hành vi được kiểm trên simulator bằng chạm/vuốt thật (idb + `simctl`), không chỉ ảnh chụp.

Phạm vi và các mốc duyệt:
- Các lần duyệt đều qua trao đổi trực tiếp, mỗi lần một vòng; không có ticket bằng văn bản:
  - 2026-10-20: độ tương phản bảng cài đặt, player mở/thu có animation, player nhường chỗ cho
    thanh công cụ (sau đó bị thay, xem dưới), sửa ma trận vuốt ở trang đầu/cuối.
  - 2026-10-20: nút back phải ẩn cùng các nút khác chứ không bị xoá; sửa lỗi trang "tự lật lại"
    (commit c999ad0).
  - 2026-10-20: lưu và khôi phục vị trí đọc; thư viện hiện % đã đọc (commit 458ae98).
  - 2026-10-21: áp dụng AI Development Toolkit — khảo sát trước, tài liệu state song ngữ ở root,
    giữ cài `--cli all`, thêm README, rồi gộp thành một commit (đã duyệt).
  - 2026-10-22: dự án iOS nay nằm trong repo OpenBook dưới worktree `ios`; toàn bộ cây (code, tài
    liệu, toolkit) được commit làm baseline (đã duyệt, không push).
  - 2026-10-22: T15 — target unit test cho `Paginator`, `EpubArchiveReader`, `ReadingProgress`;
    smoke UI test được anh bỏ ("không cần"). `xcodebuild test` phải xanh.
- Quyết định bị thay thế: "không có đường ra khỏi sách khi đang đọc thành tiếng" (commit
  caaec44) đã được anh rút lại ngày 2026-10-20 ("ẩn cùng với nút khác chứ có bảo là xoá nó đi
  đâu") và thay bằng "back ẩn/hiện cùng thanh công cụ" (commit c999ad0).
- Chưa được duyệt: publish (không có remote, chưa push lần nào), refactor, đổi kiến trúc, bố
  cục riêng cho iPad, target UI test, thay đổi repo Android từ dự án này.

Quyết định kiến trúc:
- Các tầng hiện có (`EpubReader/App`, `Data`, `Domain`, `Library`, `Reader`). Clean
  Architecture thực dụng: `Domain` giữ model và thuật toán thuần (`AttributedBuilder`,
  `Paginator`), chỉ import UIKit cho `NSAttributedString`; `Data` đọc EPUB/ZIP; phần trình bày
  là SwiftUI cộng một `UIViewControllerRepresentable` cho pager. Không có repository protocol,
  không có DI container: store là `ObservableObject` dạng singleton hoặc `@StateObject` tại
  view sở hữu (theo adt-ios-clean-architecture: không thêm tầng dự án không cần).
- Phân tích EPUB: `EpubArchiveReader` đọc `META-INF/container.xml` → OPF → spine; mục lục lấy từ
  `nav` (EPUB3) hoặc `toc.ncx` (EPUB2); XHTML của chương → `NSAttributedString` bằng HTML import
  trên main thread, vì vậy tách `RawChapter` (dữ liệu thô, đọc ngoài main) và `Chapter` (đã
  chuyển, làm trên main).
- Phân trang: CoreText framesetter cho từng chương (`Paginator`), dùng đúng font và hình học
  như bộ vẽ trang (`ReaderPageViewController`), nên mỗi trang là một range ký tự ánh xạ ngược
  được về toạ độ chương.
- Ảnh: `EpubArchiveReader` tra mọi `<img src>` của chương trong archive lúc còn mở zip (tra không
  phân biệt hoa/thường, xử lý `..`, `%20`, `?v=`; trần 8 MB mỗi ảnh và 24 MB mỗi chương) rồi đưa
  bytes vào `RawChapter`. HTML importer bỏ `<img>` — nó không có base URL — nên `ChapterImages`
  đổi mỗi thẻ thành một token ASCII trước khi import, và `AttributedBuilder` thay token đó bằng
  `NSTextAttachment` cắt theo cột (không phóng to, cao tối đa 55 % chiều cao cột), hoặc bằng alt
  text khi ảnh thiếu/không giải mã được. Một `CTRunDelegate` báo kích thước ảnh cho CoreText: nếu
  không có nó, bộ phân trang đo ký tự giữ chỗ rồi `UITextView` cắt mất ảnh. Đổi bề rộng cột thì
  dựng lại toàn bộ chữ (`builtWidth` trong `ReaderViewModel`).
- Mô hình curl: `UIPageViewController` hai mặt đi theo *side* chứ không theo trang (front N →
  back N → front N+1). `SheetBackViewController` vẽ giấy của theme kèm ảnh chụp mặt trước lật
  ngược, mờ 9 %. Lượt lật do chương trình gọi truyền `[front, back]`; mỗi lần hạ cánh đều gọi
  `settle(on:)` để đưa sách về một mặt. `turning` / `waitingPage` hoãn trang được yêu cầu giữa
  cú kéo; `turnID` không cho cú lật bị chen ngang hạ cánh về trang cũ; yêu cầu trùng trang đang
  hiện bị bỏ qua (commit c999ad0).
- Lưu trữ: `UserDefaults` cho cài đặt đọc (`reader*`), tốc độ đọc (`narrationRateFactor`), đánh
  dấu và `readerPositions` (JSON); sách nằm trong `Documents/Books`; `sample.epub` trong bundle
  được chép một lần.
- Bộ móc demo trong app: các cờ khi khởi chạy (`-demoOpenBook`, `-demoPage`, `-demoSpeak`,
  `-demoDark`, `-demoSettings`, `-demoDebug`, `-demoCurl`, `-demoDrag*`,
  `-demoRebindOnTurn`, …) tạo trạng thái xác định để kiểm trên simulator.
- Giao diện là toàn app và do reader sở hữu: `ReaderSettings.shared` là một đối tượng cho cả
  tiến trình, thư viện đọc chính đối tượng đó nên giấy, mực, accent và đường kẻ của kệ sách là
  palette đang dùng (Android cũng vậy: `ReaderSettings` cấp tiến trình → `ReaderTheme`).
  `preferredColorScheme` đi theo trạng thái tối của palette, vì ngày/đêm của reader là cài đặt
  riêng chứ không phải giao diện hệ thống.
- Quy tắc làm việc: dự án đã có AI Development Toolkit v1.1.0 (`AGENTS.md`, `.agents/skills`,
  `.opencode/commands`, `.ai-toolkit`); làm tuần tự trong một agent, việc nhiều bước phải duyệt
  kế hoạch trước khi sửa code, commit chỉ stage đường dẫn cụ thể.

Đã xong và đã kiểm:
- T01–T10, T12, T15 và T16 (xem TASKS.md) đã làm và đã kiểm, tất cả đều đã commit: chúng vào repo
  OpenBook trong commit baseline `e727837` ("Initial import: EpubReader for iOS", 2026-10-22), commit
  này mang luôn SUITE-003 và SUITE-009, vì repo `EpubReader-iOS` riêng kết thúc ở `e224196` với các
  tệp đó còn trong cây làm việc.
- SUITE-003 (thư viện in trên palette của reader) và SUITE-009 (chạm khi giọng đọc tạm dừng) đã làm,
  đã kiểm và nay đã nằm trong baseline đó.
- T15 (unit test) đã làm và đã kiểm trong cây làm việc; muốn commit cần anh cho phép.

Đang làm / chờ / bị chặn:
- Đang làm: không.
- Chờ: T11 icon app, T13 trang mở đầu chương bị đậm, T14 quyết định nếp gấp ở theme tối.
- Bị chặn: không.

Nhánh Git / HEAD / tệp chưa commit:
- Dự án là worktree `ios` của repo OpenBook (thư mục cha `/Users/cuong/Workspace/CODE/AI/Orca/OpenBook`,
  anh em `master` và `android`). Nhánh `ios`; baseline `e727837` "Initial import: EpubReader for
  iOS" (2026-10-22) trên commit rỗng `c6633fb Initial commit`. Lịch sử cũ (`e224196` lùi về
  `7cfd579`) chỉ còn ở `/Users/cuong/Workspace/CODE/AI/OpenCode/EpubReader-iOS`; bản đó giống hệt cây
  này, chỉ khác `build/` và `.git`.
- T15 (2026-10-22) đang ở trong cây làm việc, chưa commit: thư mục mới `EpubReaderTests/` (ba tệp
  test), `EpubReader.xcodeproj/project.pbxproj` (target test), scheme dùng chung mới
  `EpubReader.xcodeproj/xcshareddata/xcschemes/EpubReader.xcscheme`, `Reader/ReadingProgress.swift`
  (`init(defaults:)`) và ba tệp state này.
- Không có remote; chưa push gì. Các worktree phụ ở `~/orca/workspaces/EpubReader-iOS/` không còn nữa.

Kiểm chứng:
- `xcodebuild test -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd`
  → `** TEST SUCCEEDED **`, 13 test, 0 lỗi (2026-10-22 10:15 +07, Xcode 27.1). Sau thay đổi, build
  app vẫn xanh (`** BUILD SUCCEEDED **`, dòng `warning:` duy nhất là thông báo của
  `appintentsmetadataprocessor`). Test: `PaginatorTests` 4, `EpubArchiveReaderTests` 4,
  `ReadingProgressTests` 5.
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`, 0 warning (2026-10-21 10:53 +07, Xcode 27.1; dòng
  `warning:` duy nhất trong log là thông báo của `appintentsmetadataprocessor` về việc không phụ
  thuộc `AppIntents.framework`).
- Kiểm trên simulator ngày 2026-10-20 (UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  ma trận vuốt một trang mỗi cú, cả hai chiều và ở trang đầu/cuối, có và không có giọng đọc;
  móc `-demoRebindOnTurn` (bản cũ lật ngược lại, bản hiện tại không); player, nút back và % thư
  viện (20 % → 36 % → 100 %); mở lại sau khi thoát (trang 3 → mở trang 3; 19 pt trang 3 → 24 pt
  đúng câu đó).
- `python3 .ai-toolkit/scripts/checkpoint.py .` → `master`, cây tracked sạch (2026-10-21).
- SUITE-003, 2026-10-21 (iPhone 17 Pro, UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`. Lệnh không kèm `OS=26.5` giờ fail (exit 70: nó hiểu
  là `OS:latest` = 27.0, mà runtime đó không có iPhone 17 Pro). Cài lên simulator đang chạy và
  chụp thư viện (1206×2622, ảnh lưu ngoài repo ở `…/T/epub-ios/`):
  - đổi palette ngay trong một phiên, không khởi động lại (`-demoLibraryTheme focus
    -demoLibraryDark 1`): nền kệ #FFFFFF (Original sáng) trước, #2A2114 (giấy Focus tối) sau;
  - Paper sáng: nền #FBF7EE (2.016.748 px), vạch tiến trình #EFE9DC (3.586 px), accent #9E5932
    (3.529 px), mực mờ #6E6459 (819 px);
  - Paper tối: nền #0F0D0B (2.198.333 px), vạch #1E1B17 (3.583 px), accent #D9A06A (3.529 px),
    mực mờ #9C9385 (819 px);
  - kiểm lại reader sau thay đổi (`-demoOpenBook 1 -demoPage 3 -demoTheme paper -demoDark 1`):
    giấy trang #0F0D0B, hành vi đọc không đổi;
  - đổi giao diện hệ thống sáng→tối khi kệ đang ở Paper sáng: nền vẫn #FBF7EE, vì ngày/đêm là
    cài đặt của reader (Android cũng vậy); phần chrome hệ thống bám theo palette nhờ
    `preferredColorScheme`.
- SUITE-009, 2026-10-22 (iPhone 17 Pro, UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5):
  `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **`. Chạy bản cũ (trước sửa) và bản mới rồi so ảnh + log
  (báo cáo: `build/qa/SUITE-009/REPORT.md`):
  - đang tạm dừng + chạm vào câu: trước — không có gì xảy ra (`state paused`, `spoken 710+79`
    không đổi); sau — `speak chapter 2 from 789`, `state playing`, `spoken 789+229`;
  - đang tạm dừng + chạm vào lề trắng: trước — trang lật ngược (`Trang 5/13` → `4/13`) và chrome
    vẫn `playerOnly`; sau — `chrome full`, `state paused` không đổi, vẫn trang cũ;
  - đang tạm dừng + chạm vào khoảng trắng trong cột: trước — log ra `chapter 1018` (chọn nhầm câu
    gần nhất); sau — log ra `blank`, chrome `full`, không chọn câu nào;
  - hồi quy: đang đọc + chạm câu vẫn nhảy đúng (`speak … from 789`); rảnh + chạm giữa trang vẫn
    bật/tắt thanh công cụ; rảnh + chạm 1/3 phải vẫn lật trang (`Trang 5/13` → `6/13`).
  - Ngón tay là giả lập ở lượt đầu: bản cài Xcode 27.1 trên máy này không có `Simulator.app`, cũng
    không có `idb`/`applesimutils`/`cliclick`, nên chạm được đưa vào bằng móc demo của chính app
    (`-demoTapX/-demoTapY`), gọi đúng hàm `emitTap` mà `UITapGestureRecognizer` gọi. Mọi thứ
    sau gesture recogniser được kiểm thật.
  - Lặp lại toàn bộ bảng kiểm trên **device hub với chạm thật** (serve-sim HID → digitizer của
    simulator; tọa độ chuẩn hoá 0..1). Device Hub của Xcode 27 đã ngắt dịch vụ touch cũ, sửa
    bằng `serve-sim repair-input -d <udid>` (khởi động lại SpringBoard, đóng app đang chạy) rồi
    khởi động lại serve-sim và mở lại app. Cùng trang (index 4/13, chương 2): tạm dừng + chạm thật
    vào câu → bản trước sửa log `tap 200.0,230.0 column 737 local 107 chapter 844` mà không có
    dòng `speak` (`state paused`); bản sau sửa log `speak chapter 2 from 789 … wasPaused true` và
    overlay đi từ `state paused` sang `state playing`. Tạm dừng + chạm thật vào giấy trắng → bản
    trước sửa log `tap 200.0,500.0 column 737 local 281 chapter 1018` (coi giấy trắng là câu cuối
    cùng) và chrome kẹt ở `playerOnly`; bản sau sửa log `tap 200.0,500.0 blank`, chrome thành
    `full`, trạng thái vẫn `paused`, trang không đổi. Hồi quy với chạm thật: đang đọc + chạm câu
    vẫn nhảy, rảnh + chạm giữa vẫn hiện thanh công cụ, rảnh + chạm 1/3 phải vẫn lật trang
    (`Trang 5/13` → `6/13`).
- Dự án không có target XCTest (CHƯA CHẠY: test unit/UI).

Hạn chế đã biết:
- Giao diện sáng/tối của hệ thống không làm đổi màu thư viện hay reader: ngày/đêm là cài đặt riêng
  của reader, giống Android. Kệ giữ palette của reader và ghim chrome hệ thống theo palette. Muốn
  theo hệ thống thì cần thêm lựa chọn "Tự động" — chờ anh quyết.
- Tiêu đề lớn của thư viện và thanh trạng thái là chrome hệ thống (mực do `preferredColorScheme`
  chọn); chữ do thư viện tự vẽ (phần trăm, màn trống) dùng mực của palette.
- Ảnh chỉ hỗ trợ dạng raster (PNG/JPEG/GIF/WebP). SVG, `<object>`, `<image>` trong SVG nội tuyến và
  ảnh nền CSS không được vẽ — SVG sẽ hiện alt text.
- Ảnh dạng khối không vừa chỗ còn lại thì sang trang sau, nên chân trang trước có thể trống; ảnh
  không bao giờ bị cắt đôi giữa hai trang.
- Bytes của ảnh nằm trong bộ nhớ suốt thời gian mở sách (8 MB mỗi ảnh, 24 MB mỗi chương) và được
  giải mã lại khi bề rộng cột đổi; mỗi lần dựng lại phải import toàn bộ chương trên main thread nên
  đổi cỡ chữ/lề/cột ở sách lớn sẽ khựng một nhịp.
- Chưa có icon app: không có asset catalog; chỉ có `Resources/sample.epub`.
- Trang mở đầu chương hiển thị đậm hơn rõ rệt (trang 2 đậm hơn trang 3); chưa tìm ra nguyên nhân.
- `ReaderView.swift` dài 1.309 dòng, ôm cả thanh công cụ, player, cài đặt và móc demo; tách nhỏ
  là refactor, cần duyệt riêng.
- Unit test chỉ phủ phần logic thuần; giao diện vẫn kiểm bằng tay trên simulator (smoke UI test được
  anh bỏ ngày 2026-10-22).
- Phần trăm đọc tính ở chân trang đang hiện, nên trang đầu của sách 13 trang khoảng 8 %, trang
  cuối là 100 %.
- Chỉ dọc, hỗ trợ iPhone và iPad (`TARGETED_DEVICE_FAMILY = 1,2`) nhưng chưa có bố cục iPad.
- Chất lượng giọng đọc phụ thuộc giọng tiếng Việt cài trên máy.

Hành động tiếp theo chính xác:
- Xin anh cho phép commit T15 (stage đúng đường dẫn, không push), rồi hỏi việc nào trong T11, T13,
  T14 làm tiếp — viết kế hoạch cho việc đó và chờ duyệt trước khi sửa code.

## 2026-10-24 — Tinh gọn giao diện theo yêu cầu trực tiếp (xong, kiểm chứng bằng build + test)
- `FloatingTabBar` bỏ chữ, chỉ còn icon (`MainView.swift`).
- `ContinueReadingCard` bỏ nhãn % rời, giữ "Trang x/y · %" (`HomeView.swift`).
- `BookCover` bỏ tên sách + tên tác giả, chỉ còn gradient + gáy (`LibraryView.swift`).
- Thư viện: một nút `...` (popup: Chọn / Sắp xếp / Đổi dạng) ghép đôi nút `+` import; bỏ nút cài đặt
  ở đây.
- Cài đặt (`AppSettingsView`) chuyển vào tab Trang Chủ, nút `gearshape` trên toolbar.
- Kiểm chứng: `xcodebuild test` → 33 tests / 0 failures; `xcodebuild build` → BUILD SUCCEEDED.

### 2026-10-24 (tiếp) — vòng chỉnh 2
- Tab bar có lại chữ; bỏ title lớn trên Home/Library.
- Library: `+` + `...` chung một capsule theo metrics/màu của reader (`ReaderMetrics` 44/15).
- Lưới nhỏ lại (min 96), `...` từng cuốn xuống hàng `%`, bỏ vòng tròn — chỉ ở grid.
- Build SUCCEEDED; ảnh chụp xác nhận trên simulator iPhone 17 Pro.

### 2026-10-24 (tiếp) — vòng chỉnh 3
- Library: `...` ngoài, `+` trong; ẩn nav bar (trừ khi chọn nhiều), nền capsule `cardFill` mờ, bỏ quầng trắng.
- Menu sách chỉ còn Chỉnh sửa / Xoá.
- Home: gear tự vẽ trên nền giấy, không còn chip trắng của system toolbar.
- Build SUCCEEDED.

### 2026-10-25 — Full player
- Thêm `NowPlayingView` mở từ title của player: thông tin sách, slider tua, play/pause, chuyển
  chương, hẹn giờ tắt, chọn giọng đọc.
- `SpeechController` + `ReaderViewModel.narrationPlace`/`NarrationMath`; L10n 8 key mới.
- Test: 37/37 xanh. Chưa commit.

### 2026-10-25 (tiếp) — popup player tinh chỉnh
- Bìa to, dòng tốc độ, "Trang x/y" + % trên slider, hẹn giờ/giọng đọc dạng icon.
- Nền panel + nút theo giấy reader nhưng sáng hơn (`popupPaper`, `popupControlFill`).
- 37/37 test xanh. Chưa commit.

### 2026-10-25 (tiếp) — popup player vòng 2
- Speed thành menu bấm được; sleep + voice vào chung hàng transport; nút dùng `controlFill` theo theme.
- 37/37 test xanh. Chưa commit.

### 2026-10-25 (tiếp) — popup player vòng 3
- Speed `− rate +` full-width; 4 nút phụ 48pt, sleep ngoài cùng trái.
- Bỏ disabled ở skip; `NarrationMath.skipTarget` clamp hai đầu (38/38 test).
- Ảnh chụp paper + focus dark xác nhận màu theo theme. Chưa commit.

### 2026-10-25 (tiếp) — popup player vòng 4
- Demo hook chỉ chạy 1 lần/launch → vào sách không tự mở full player.
- ⏮/⏭ disable đúng ở chương đầu/cuối. Nút tốc độ nhỏ nằm dưới tên chương. Sheet height 352.
- 37/37 test xanh. Chưa commit.

### 2026-10-25 (tiếp) — popup chọn giọng đọc
- Giọng đọc: sheet nhóm theo vùng/ngôn ngữ, nghe thử + Dùng, hướng dẫn tải thêm trong Cài đặt.
- 41/41 test xanh (thêm VoiceCatalogTests). Chưa commit.

### 2026-10-25 (tiếp) — voice search + ngôn ngữ máy
- Popup giọng: tìm kiếm theo ngôn ngữ/mã locale/tên giọng; nhóm ngôn ngữ điện thoại lên đầu,
  rồi tiếng Việt. Giọng mặc định theo ngôn ngữ điện thoại. 45/45 test xanh. Chưa commit.

### 2026-10-25 (tiếp) — voice theo ngôn ngữ sách
- Popup giọng chỉ mở bằng nút voice; tên giọng hiện cạnh nút tốc độ; danh sách lọc theo ngôn ngữ
  sách (NLLanguageRecognizer), giọng lưu không khớp ngôn ngữ sách bị bỏ qua. 47/47 test xanh.
  Đã cài lại bản sạch lên simulator. Chưa commit.

### 2026-10-25 (tiếp) — sample books
- `SampleBooks/` có 5 EPUB mẫu en/fr/es/ja/zh + script tạo; đã cài vào simulator.
  Đã xác nhận lọc giọng đúng cho en/ja/zh. Chưa commit.

### 2026-10-25 (tiếp) — player layout + theme flash
- Player: chương → divider → "Giọng đọc: …" → `− 1× +`.
- Hết nháy nền tối khi thoát sách: preferredColorScheme chuyển từ ReaderView lên NavigationStack
  của Library/Home, chỉ giữ khi path có sách. 47/47 test xanh, đã cài lại simulator. Chưa commit.

### 2026-10-25 (tiếp) — language audit + Android handoff
- iOS commit `9da6500`. App đang override tiếng Việt dù máy chính là English; bỏ override thì UI
  theo máy đúng. Cần quyết định: giữ vi hay mặc định theo máy (kèm fix relaunch cho "Theo máy").
- Spec full player đã đặt tại `android/docs/full-player-parity.md` và gửi agent Android.

### 2026-10-25 (tiếp) — ngôn ngữ theo máy
- Quy tắc: máy ưu tiên tiếng Việt → UI Việt, còn lại → English; hệ thống ngôn ngữ trong Settings
  vẫn override được. Resolve tường minh (không dựa fallback bundle). 49/49 test xanh; đã kiểm chứng
  bằng cài sạch trên simulator mới với en-VN và vi-VN. Chưa commit.

### 2026-10-25 — EPUB compatibility + performance audit (chưa sửa code)
- Corpus 31 file tổng hợp + 1 EPUB thật (Gutenberg) tại `build/qa/epub-corpus/` (build/ bị gitignore);
  test `EpubReaderTests/EpubCorpusTests.swift` (tự skip khi thiếu corpus).
- PASS: EPUB2/NCX, EPUB3/nav, OPF gốc, streamed zip, %-encoded/unicode, absolute href,
  linear="no", font obfuscation, fixed-layout, image-heavy, nested TOC, RTL, CJK, BOM/entities,
  ZIP64, HTML lỗi, spine thiếu idref, NFD, thiếu mimetype, Gutenberg thật; từ chối đúng: thiếu
  container, spine rỗng, PDF đổi đuôi.
- FAIL: (1) EPUB DRM AES/LCP **mở được** và vẽ 25 trang ký tự rác — chưa phát hiện
  `META-INF/encryption.xml`; (2) tên file zip CHƯƠNG HOA vs href thường → mất chương im lặng (2/4);
  (3) entry `./OEBPS/...` → từ chối cả sách hợp lệ; (4) nav.xhtml lỗi XML → mất tiêu đề chương (nhẹ).
- Hiệu năng: 1.500 trang/1,13M ký tự mở 1,34 s; 1.000 chương ngắn mở 4,51 s (paginate đồng bộ
  trên main thread); 1 chương 585k ký tự 0,28 s; RAM sau khi mở 157–264 MB. Không crash/OOM.
- Đề xuất sửa 3 điểm đầu (~40 dòng + test), chờ duyệt. Chi tiết: `build/qa/epub-corpus/REPORT.md`.

### 2026-10-25 — sửa lỗi EPUB (phương án B)
- Commit trước khi sửa: `95e82dd` (ngôn ngữ theo máy + corpus test).
- Đã sửa: entry lookup khoan dung (bỏ `./`, fold hoa/thường) cho container/OPF/nav/NCX/chương/ảnh;
  phát hiện DRM qua `META-INF/encryption.xml` khi tài nguyên văn bản bị mã hoá → `EpubError.protected`
  (vi/en), kiểm ở read/readMetadata/validate; import từ chối sách DRM/hỏng và xoá bản copy.
- Test: 54/54 xanh (49 cũ + 5 corpus). Corpus lỗi cũ giờ đúng: 05 = 4 chương, 28 mở được,
  10/11 bị từ chối; ảnh chụp `drm-refused.png` hiện thông báo bảo vệ bản quyền.
- Chưa commit phần sửa. Hiệu năng không đổi (1.500 trang 1,5 s; 1.000 chương 4,4 s).

### 2026-10-25 — Reading achievement card + mark finished
- Home có thẻ "THÀNH TÍCH" trên "ĐANG ĐỌC": vòng mục tiêu ngày, số phút hôm nay, chuỗi ngày,
  tổng tuần/tháng/năm; chạm mở sheet "Thành tích đọc" (4 ô, chuỗi/ngày đọc, phiên dài nhất,
  ngày đọc nhiều nhất, sách đọc xong năm nay).
- Mục tiêu mặc định 5 phút/ngày, đổi trong Settings (1/5/10/15/30/60/120). "Ngày đọc" và streak
  dùng cùng ngưỡng này; streak và tổng luôn tính lại từ dữ liệu ngày.
- Ghi thời gian khi reader mở + app foreground; TTS tính riêng nhưng cộng vào tổng; quá 5 phút
  không lật trang và TTS không chạy thì ngừng cộng; phiên qua nửa đêm chia hai ngày.
- Thư viện: menu sách có "Đánh dấu đã đọc xong"/"Bỏ đánh dấu"; sách xong hiện nhãn accent thay %,
  rời carousel Home (vị trí đọc giữ nguyên).
- Tests: 79/79 xanh (20 test mới cho store, 4 cho finished/metadata, 1 duration, 1 HomeFeed).
- Ảnh chứng cứ: build/qa/reading-stats/ (card vi/en/dark, sheet vi/en, goal 60, nhãn Finished).
  Demo trên simulator dùng `-demoStatsSeed 1` và `-demoStatsSheet 1`; dữ liệu demo còn trong app.
- Chưa làm (phase sau): biểu đồ/heatmap, tách đọc/nghe trong UI, thông báo, xoá dữ liệu đọc.

### 2026-10-26 — Home: Mới thêm / Gợi ý / Đọc xong / Tuần này
- Home giữ THÀNH TÍCH → ĐANG ĐỌC (tối đa 5), sau đó khi thư viện ≥ 6 cuốn:
  MỚI THÊM (5, chưa đọc, import mới nhất) → GỢI Ý CHO BẠN (5, cùng tác giả với 3 sách
  vừa đọc, cần ≥ 2, không trùng hai hàng kia) → ĐỌC XONG GẦN ĐÂY (5, trong 12 tháng) →
  TUẦN NÀY (7 cột thứ→chủ nhật, cột hôm nay accent, vạch mục tiêu 5 phút).
- Hàng/biểu đồ tự ẩn khi rỗng; thư viện < 6 cuốn chỉ còn THÀNH TÍCH + ĐANG ĐỌC.
- Logic thuần trong `HomeSections` + `ReadingStatsStore.week`; UI `HomeBookRow`,
  `WeeklyReadingChart`. Không đổi Thư viện, không thêm dữ liệu/API.
- Tests: 89/89 xanh (7 HomeSections, 2 week, 1 HomeFeed limit mới).
- Ảnh: `build/qa/home-sections/` (iPad đủ 4 hàng + chart vi/en; iPhone sau khi hoàn nguyên).
  Dữ liệu demo (3 sách copy, metadata tác giả) đã hoàn nguyên; simulator về trạng thái cũ.
