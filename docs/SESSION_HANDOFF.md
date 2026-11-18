# Session handoff

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
- iOS specifics: bundle `com.elearning.openbook`, team `KTKL66WRBK`, automatic signing, iPhone only, version
  `2026.11.11` (build `20261111`) via `$(MARKETING_VERSION)`/`$(CURRENT_PROJECT_VERSION)`. Added `PrivacyInfo.xcprivacy`,
  `ITSAppUsesNonExemptEncryption=NO`, `ExportOptions.plist` (destination export). Audio session activates off the main
  thread; `.duckOthers` removed so Control Center / Lock Screen controls work (owner to confirm on device).
  Owner's iPhone 13 Pro (UDID 00008110-0008190214FB801E) is registered, Developer Mode on.
- Release archive: `build/OpenBook.xcarchive` + copy in `~/Library/Developer/Xcode/Archives/2026-11-11/`; local IPA
  `build/export/EpubReader.ipa`. NOT uploaded: Claude's upload command is blocked; owner uploads via Organizer →
  Distribute App. Screenshots already uploaded by owner (1206×2622); store pack in master `stores/app-store/`.
- Tests: `xcodebuild test ... -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -derivedDataPath build/dd`
  → 154 tests, 5 skipped, 0 failures.
- Next: owner uploads the build and submits; owner decides commit.


## Current state — 2026-11-11

- Worktree `ios` of `/Users/cuong/Workspace/Projects/OpenBook`; branch `ios`, HEAD `7c88d03`;
  app/test/config tree clean.
- `xcodebuild test` on 2026-11-11 → `** TEST SUCCEEDED **`, **140 tests, 5 skipped (corpus
  manifest missing), 0 failures** (iPhone 17 Pro, iOS 27.0, Xcode 27.2).
- Done and committed: T01–T10, T12, T15–T17, T22–T44, SUITE-003, SUITE-009, reading stats
  (2026-10-25 plan, all ten tasks; commits `3640250`…`5eb85a6`) and Home sections (2026-10-26).
  The finished reading-stats spec and plan in `docs/superpowers/` were deleted on 2026-11-11
  (recover from `5b246b4`).
- Fixed on 2026-11-11: T18–T21 and T24 (`331d05e`, `8f5818f`, `2e9a362`, `1be3691`, `f1a64ef`) and
  I‑OPT‑01…04 (`e6b6a5e`, `8f5818f`, `d159223`, `7c88d03`); see `TODO.md` for what each commit
  changed and which part still needs a device (memory after 500 turns, Instruments numbers).
- Open: T11 icon, T13 chapter-opening weight, T14 dark-fold decision; corpus manifest; device
  QA. `TODO.md` at the repo root tracks the rest.
- Not verified by hand: VoiceOver, lock-screen/headphone controls, mini-player tap, real
  `BookEditSheet`, a real cover with a printed title, selection-handle drag with a real finger.
- Dirty tree is documentation/toolkit only: root state files deleted in favour of `docs/`,
  untracked `TODO.md`/`.commandcode/`, pre-existing `.ai-toolkit`/`.agents`/`.claude` deletions,
  `AGENTS.md`/`CLAUDE.md` edits. Do not stage or discard without the owner.
- QA artifacts on this machine: only `build/qa/{book-covers,highlight,mini-player,voice-lang}`.
- Exact next action: owner decides T11/T13/T14 or the device QA list; no `ios` remote, nothing
  pushed.

## 2026-11-02 — state before handoff/compact

- Branch `ios`, HEAD `0a78999`; app code tree clean. Recent commits: `0a78999` (T42–T44),
  `5288aef` (covers + Home baseline), `97bbc33` (mini player), `44b17c0` (T23), `5865312`
  (highlight/background audio).
- Build succeeded; full suite **128 tests, 5 skipped, 0 failures** (iPhone 18 Pro, iOS 27.0).
  The last icon-only change (`gobackward/goforward` → `minus/plus`) was built but did not
  re-run the suite.
- Done: T22–T23, T25–T44. Pending review defects T18–T21/T24. Remaining manual QA:
  VoiceOver, lock-screen/headphone controls, mini-player tap, real `BookEditSheet`, real cover
  with printed title.
- State docs are the untracked `docs/` copies; root versions are deleted in the working tree.
  Other dirty items are toolkit/AGENTS/`CLAUDE.md`/`.commandcode` pre-existing. Do not stage or
  discard without the owner.
- Simulator iPhone 18 Pro (UDID `0AD96A11-BAFB-468D-A68D-4E545EF58BE8`, iOS 27.0) has the app
  installed; last screen is the Vietnamese reader with mini player `− play/pause +`.
- Exact next action: owner review/QA and the docs-move/commit decision; then the manual QA
  above. No remote, nothing pushed.

## Review checkpoint — 2026-10-29 (code không đổi)

- Đã hoàn tất review luồng chức năng và code quan trọng; thêm 7 lỗi Pending (T18–T24) trong TASKS.md và TODO.md riêng cho ưu tiên sửa/tối ưu/QA. Chi tiết tình huống, vị trí code, mức bằng chứng và nghiệm thu ở TASKS.md.
- Chỉ cập nhật tài liệu. Không stage/commit, không sửa app/test/config, không cài app hoặc thay dữ liệu người dùng. Git root `master`, HEAD `c6633fb`; android/ios/design và các file untracked có sẵn được giữ nguyên; khi tiếp tục xuất hiện thêm web/ từ công việc khác, không đụng tới.
- Khi tiếp tục, tài liệu Android đã nằm ở android/docs/; giữ vị trí hiện hữu, không tạo lại bản root. Đối chiếu SHA-256 của 135 source/resource/test/config với snapshot review: không khác.
- Verification mới: probe Kotlin tìm kiếm tái hiện lỗi Đ/NFD; probe Swift/macOS tái hiện lệch tuần, offset Unicode và mất đuôi sau 4.000 cột, đồng thời xác nhận ZIP không chặn expansion 8 MiB. Compile bản cuối/run đều exit 0; đây là tái hiện lỗi, không phải app test pass. Probe tạm ở /tmp/openbook-audit, có thể mất sau reboot; kết quả và cách dựng được ghi trong TASKS.md.
- Không chạy Gradle/xcodebuild/full test/lint/device QA mới. Corpus chung hiện thiếu manifest; kết quả QA trước là lịch sử.
- Bước tiếp theo: đọc TASKS.md phần Review codebase và TODO.md, chọn nhóm P1 để xử lý khi có yêu cầu sửa. Trước sửa đối chiếu lại source/Git; hiện phạm vi được giao chỉ review, nên tất cả defect vẫn Pending.

## 2026-11-02 (tiếp 3) — Mini player `- play/pause +` (committed `0a78999`)

`ReaderView.playerControls`: `gobackward/goforward` → `minus/plus`; hàng
`title · 1× − play/pause + ×`; accessibility labels giữ `reader.slower/faster`. Ảnh
`build/qa/voice-lang/mini-player-minus-plus.png`; build succeeded; no TTS logic change.
Commit `0a78999`.

## 2026-11-02 (tiếp 2) — Giọng đọc theo ngôn ngữ sách (committed `0a78999`)

**Nguyên nhân:** bộ `vietnameseSignals` cũ gồm cả `é/è/ê/à/ù/ô/á/í…` — dấu dùng chung với
Pháp/Tây Ban Nha. Sách Pháp bị `strongLanguage` nhận là `vi`, đè tag `fr`; Voice picker chỉ
còn `VIETNAMESE — Linh` và mọi sách dùng Linh.

**Sửa:** chỉ giữ ký tự đặc trưng Việt (`ă`, `đ`, `ơ`, `ư`, dấu hỏi/nặng, tổ hợp thanh trên
`ă/â/ê/ô/ơ/ư`); bỏ dấu sắc/huyền/mũ trơn. Thêm test `fr`, `es`, `pt` giữ tag.

**Kiểm chứng:** full suite **128 tests, 5 skipped, 0 failures** (2026-11-02 15:08 +07).
Ảnh `build/qa/voice-lang/`: `french-book.png` (trước sửa, Voice sheet chỉ Linh vi-VN);
`french-after-fix.png` (Amélie fr-CA); `spanish-after-fix.png` (Mónica es-ES);
`japanese.png` (Kyoko ja-JP); `chinese.png` (Meijia zh-TW); `vietnamese.png` (Linh vi-VN);
`english-book.png` (Samantha en-US).

**Files:** `EpubReader/Domain/BookLanguage.swift`, `EpubReaderTests/BookLanguageTests.swift`
(cùng T42 `LibraryView/HomeView/HomeBookRow`) đã commit `0a78999`; docs state vẫn untracked.

## 2026-11-02 (tiếp) — Title sách + bìa thật/tự tạo (T38–T41 committed `5288aef`; T42 committed `0a78999`)

**Đã làm:** T38–T41. Đọc cover từ EPUB (`coverData`, meta/properties/fallback), nhận diện
trang trắng bằng tỉ lệ pixel không trắng + độ bão hòa, cache lazy theo file date, `BookCover`
dùng chung (ảnh thật/gradient + title/author), Home shelf bỏ title ngoài + author chỉ bìa tự
tạo, Home card label title/author, Library list cover 64, title edit hook.

**Kiểm chứng:**
- `xcodebuild build …` → `** BUILD SUCCEEDED **`.
- `xcodebuild test … -destination 'platform=iOS Simulator,name=iPhone 18 Pro'` → exit 0,
  `** TEST SUCCEEDED **`, **125 tests, 5 skipped, 0 failures** (2026-11-02 11:12 +07).
- Ảnh QA `build/qa/book-covers/`: generated-home/grid/list; covers-library-grid (real giữ ảnh,
  white → generated), covers-home-shelf, covers-home-card-real/white, covers-library-list,
  covers-title-edited, covers-library-dark. Fixture `zz-real-cover.epub`,
  `zz-white-cover.epub`, `make_cover_books.py` (build ignored; đã copy vào simulator).
  Simulator **iPhone 18 Pro, iOS 27.0**.
- Lỗi heuristic đầu tiên: >98% gần trắng để lọt trang title có vạch đen (~3% đen); đã đổi
  thành non-white <10% + saturation <0.15; test white/mark/logo/picture.

**File dirty do đợt này:** `EpubReader/Data/EpubArchiveReader.swift`,
`EpubReader/Library/LibraryView.swift`, `EpubReader/Home/HomeView.swift`,
`EpubReader/Home/HomeBookRow.swift` (untracked Home work), mới
`EpubReader/Domain/BookCoverImage.swift`, `EpubReader/Library/BookCoverStore.swift`,
`EpubReaderTests/BookCoverImageTests.swift`; docs state.

**Chưa kiểm:** VoiceOver runtime (cover hidden + label code-reviewed), title edit bằng
`BookEditSheet` thật (demo hook dùng cùng `updateMetadata`), ảnh bìa thật có chữ (fixture là
gradient trừu tượng). Bìa thật load lazy có thể hiện placeholder gradient trong thoáng đầu.

**Baseline:** Home-sections baseline + cover T38–T41 commit `5288aef`; T42–T44 commit
`0a78999`; docs state vẫn untracked.

**2026-11-02 (tiếp) — tinh chỉnh title matrix:** Home READING
`BookCover(showsGeneratedText: false)` (bìa tự tạo chỉ gradient, title/author ngoài giữ); Home
shelf bỏ author ngoài (title/author trong bìa tự tạo vẫn giữ, bìa thật không); Library list
`showsGeneratedText: false` cả hai loại (title/author/progress ngoài); grid giữ. Files:
`LibraryView.swift`, `HomeView.swift`, `HomeBookRow.swift`. Build + **125 tests** xanh; ảnh
`final-home-after-tweaks.png`, `final-library-list-after-tweaks.png`. Phần tinh chỉnh **chưa
commit**.

## 2026-11-02 — Player button on Home/Library/Store (committed `97bbc33`)

**Đã làm:** T35–T37. Một overlay `MiniPlayerButton` trong `MainView`; visibility qua
`MiniPlayerVisibility` (idle/chưa mở sách/reader mở → ẩn); bấm gọi `openPlayingBook()`.
Đĩa 48 trong nút 56, góc dưới-phải, cách tab bar 8 pt; `VinylCover` dừng đúng góc khi paused;
Reduce Motion tắt quay. Reserve đáy động qua `EnvironmentValues.playerInset` cho Home/Library/
Store. Thêm key `reader.player.state.playing`/`.paused` (en/vi) và hook QA
`-demoSwitchTabAfter`/`-demoSwitchTab`.

**Kiểm chứng:**
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'generic/platform=iOS Simulator' -derivedDataPath build/dd build` → exit 0,
  `** BUILD SUCCEEDED **`.
- `xcodebuild test … -destination 'platform=iOS Simulator,name=iPhone 18 Pro'` → exit 0,
  `** TEST SUCCEEDED **`, **119 tests, 5 skipped, 0 failures** (2026-11-02 08:06 +07).
- QA ảnh trong `build/qa/mini-player/`: playing/paused Home/Library/Store, stopped,
  no-book, reader-open, Reduce Motion 2 crop trùng SHA-256, final-home-paused.
  Simulator **iPhone 18 Pro, iOS 27.0**.

**Chưa kiểm bằng tay:** bấm nút thật để mở sách (dùng `openPlayingBook`, đã kiểm route trước
đó), VoiceOver đọc runtime, pause/play từ lock screen/nút tai nghe thật; đĩa reset góc khi
reader mở/đóng.

**Dirty:** `EpubReader/App/MainView.swift`, `EpubReader/Home/HomeView.swift`,
`EpubReader/Library/LibraryView.swift`, `EpubReader/Store/StoreView.swift`,
`EpubReader/Resources/{en,vi}.lproj/Localizable.strings`; mới
`EpubReader/App/MiniPlayerButton.swift`, `EpubReaderTests/MiniPlayerVisibilityTests.swift`.
Committed `97bbc33`; docs state chưa commit.

**Bước tiếp theo:** QA tap/VoiceOver/lock-screen trên GUI/thiết bị thật.

## 2026-11-01 — Highlight chọn đoạn + audio nền (committed `5865312`)

**Đã làm:** T25–T34 trong `docs/TASKS.md` (chọn từ + popup + 4 màu + sửa/xoá + 3 tab; lưu
highlight theo chapter/offset UTF-16; `SpeechController` dùng chung; audio nền; now-playing +
remote commands; activation route; URL scheme; `BookLanguage`; sửa T22 chương rỗng). Sửa thêm
T23: `weekInterval` thứ Hai dùng chung cho `summary.weekMinutes` và `week`.

**Kiểm chứng (exit status thật):**
- Build: `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'generic/platform=iOS Simulator' -derivedDataPath build/dd build` → exit 0,
  `** BUILD SUCCEEDED **`, không warning từ code dự án (2026-11-01).
- Test: `xcodebuild test -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -derivedDataPath build/dd` → exit 0,
  `** TEST SUCCEEDED **`, 114 tests, 5 skipped (corpus thiếu manifest), 0 failures.
- Info.plist build: `UIBackgroundModes = [audio]`, `CFBundleURLTypes` scheme `epubreader`.
- QA ảnh + log: `build/qa/highlight/` (01…17) và log `simctl launch --console`:
  `speak chapter 1…4`, `nowplaying … artwork true`, `[route] opening the book the voice is
  reading`, `[speech] stop …`, `[nowplaying] cleared`.

**File dirty do đợt này (chưa stage/commit):**
- `Info.plist` (mới, root), `EpubReader.xcodeproj/project.pbxproj`.
- `EpubReader/App/MainView.swift`, `EpubReader/Data/EpubArchiveReader.swift`,
  `EpubReader/Domain/Models.swift`, `EpubReader/Library/LibraryView.swift`,
  `EpubReader/Reader/{PageCurlPager,ReaderPage,ReaderView,ReaderViewModel,SpeechController}.swift`,
  `EpubReader/Reader/ReadingStatsStore.swift`,
  `EpubReader/Resources/{en,vi}.lproj/Localizable.strings`.
- Mới: `EpubReader/Domain/{BookLanguage,TextSelection}.swift`,
  `EpubReader/Reader/{BookArtwork,HighlightStore}.swift`,
  `EpubReaderTests/{BookLanguageTests,HighlightStoreTests,TextSelectionTests}.swift`;
  T23 sửa `EpubReaderTests/ReadingStatsStoreTests.swift` (+2 test).
- Các thay đổi Home/docs/.commandcode có từ trước vẫn giữ nguyên, không đụng.

**Chưa kiểm bằng ngón tay thật:** pan thanh kéo (móc demo gọi `handleMoved`/`beginSelection`);
swipe xoá bookmark/highlight (dùng `.onDelete`); ba nút now-playing trên lock screen (actions
đã kiểm qua móc cùng engine); đổi sách trong một phiên (logic `openBook` review code + `stop`
runtime); hai cột iPad; EPUB tiếng Việt gắn sai `dc:language` trên thiết bị (unit test đã cover).

**Bước tiếp theo chính xác:** chờ owner review/approve; nếu duyệt, stage đúng các đường dẫn trên
(không `git add -A`, không push) và commit. Sau đó chạy QA thiết bị thật cho các mục “chưa kiểm
bằng ngón tay thật”, đặc biệt thanh kéo và lock-screen controls.

Prompt resume: Đọc state, đối chiếu files/Git; tiếp tục QA thiết bị thật cho T25/T29/T32/T33,
không discard thay đổi chưa commit.

> English first, bản tiếng Việt ở nửa dưới của cùng tệp.

Timestamp / timezone: 2026-10-23 09:35 +07 (Asia/Ho_Chi_Minh)

## 2026-10-23 — main shell delivered, app left on Home

- Scope: `MainView` (Home/Library/Store + floating tab bar), `HomeFeed`/`HomeView`, `StoreView`,
  `LibraryView` list/grid and path binding, `EpubReaderApp` root, `HomeFeedTests`.
- Verified: `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  -collect-test-diagnostics never test` → exit 0, 17 tests, 0 failures; same command with `build`
  → exit 0, `** BUILD SUCCEEDED **` (logs: `build/qa/xcodebuild-test-after-fixes.log`,
  `build/qa/xcodebuild-build-after-fixes.log`).
- KiloCode QA (real CLI, free Kilo model because the paid balance is 0):
  `build/qa/kilocode-code-qa.md` (no blockers/majors; Store row a11y and list separator contrast
  fixed afterwards), `build/qa/kilocode-device-qa.md` (capture only; text model could not see
  images), and `build/qa/kilocode-device-visual-qa.md` (vision model, 6/6 PASS). Screenshots:
  `build/qa/kilocode-device-01-home.png`, `03-library-grid.png`, `04-library-list.png`,
  `05-store.png`, `06-reader.png`, final `build/qa/final-home.png`.
- Simulator: iPhone 17 Pro, UDID `6B98AE70-9B11-4915-85AB-A3F347F40381`, iOS 26.5, booted.
  App PID 8420, running, default launch, Home tab. Library list/grid persistence was also verified
  across a relaunch (`build/qa/library-list-persisted.png`); layout was restored to grid afterwards.
  No Simulator.app GUI exists on this machine;
  simctl screenshots are the evidence. Simulator data was not reset.
- Dirty files (no commit made): `EpubReader/App/EpubReaderApp.swift`,
  `EpubReader/Library/LibraryView.swift`, plus new `EpubReader/App/MainView.swift`,
  `EpubReader/Home/`, `EpubReader/Store/`, `EpubReaderTests/HomeFeedTests.swift`, and these three
  checkpoint files. `COORDINATOR_STATUS.md` remains untracked from the coordinator.
- Next action: owner review of the running Home screen and the listed screenshots; then decide on
  commit/stage. Reading score/gamification stays deferred.

Read first: PROJECT_STATE.md, TASKS.md, `git status --short --branch`, `git log -3 --oneline`.

Verified files and checks:
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **` (Xcode 27.1, 2026-10-21 10:53 +07). The only `warning:`
  line in the log is Apple's `appintentsmetadataprocessor` notice; no warning comes from the
  project's own code.
- `python3 .ai-toolkit/scripts/checkpoint.py .` → branch `master`, HEAD `742433d` (2026-10-21).
- T12 is committed as `e224196` "Reader: show the pictures a book carries" (2026-10-21 12:42 +07);
  the state files no longer describe it as pending.
- SUITE-003 (library on the reader's palette), 2026-10-21: build with
  `-destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5'` → exit 0. The same command
  without `OS=26.5` now fails with exit 70 (`OS:latest` is 27.0 and has no iPhone 17 Pro), so always
  name the runtime. Installed on the booted iPhone 17 Pro (UDID 6B98AE70…, iOS 26.5) and
  photographed the library at 1206×2622 (files outside the repo in `…/T/epub-ios/`):
  - live repaint in one session (`-demoLibraryTheme focus -demoLibraryDark 1`): shelf background
    #FFFFFF (Original light) → #2A2114 (Focus dark paper), no relaunch;
  - Paper light: background #FBF7EE (2,016,748 px), progress track #EFE9DC (3,586 px), accent
    #9E5932 (3,529 px), soft ink #6E6459 (819 px);
  - Paper dark: background #0F0D0B (2,198,333 px), track #1E1B17 (3,583 px), accent #D9A06A
    (3,529 px), soft ink #9C9385 (819 px);
  - reader spot check (`-demoOpenBook 1 -demoPage 3 -demoTheme paper -demoDark 1`): page paper
    #0F0D0B, reading behaviour unchanged;
  - system appearance flipped light→dark while the shelf wore Paper light: background stays
    #FBF7EE — appearance is the reader's own day/night setting, as on Android; system chrome is
    pinned with `preferredColorScheme`.
- T12 (pictures), simulator UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5, generated test book
  `zz-anh-thu.epub` with a 900×300, a 600×400, a 300×1200 PNG and one `<img>` whose file is absent:
  - 2 columns, 19 pt → the wide and the inline pictures at 160 pt (column width), the tall one at
    88×353 pt (capped at 55 % of the 642 pt column), each on the page its token belongs to, nothing
    clipped; 3 pages.
  - 1 column (`-demoColumns one`) → the same pictures at 342 pt; 4 pages.
  - Dark paper → pictures unchanged, alt text legible; `[Ảnh: Ảnh thiếu tệp]` for the broken `src`.
  - Resume: `-demoPage 4` → terminate → reopen → page 4.
  - Sample book still 13 pages, text and curl unchanged.
  - Alignment measured on pixel rows (image 478–735 px inside a 160 pt column whose centre is
    603 px), not eyeballed.
- Two defects were found by these runs and fixed in the same change: CoreText ignored the
  attachment's size, so the paginator placed a 353 pt picture in the few points left at the foot of
  a column and the text view clipped it (fixed with a `CTRunDelegate`); and a second URL loaded into
  the same `ReaderViewModel` kept the previous book's typeset chapters (fixed by resetting the
  typeset state in `load`).
- SUITE-009 (iOS paused audio taps), 2026-10-22: `xcodebuild -project EpubReader.xcodeproj -scheme
  EpubReader -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5'
  -derivedDataPath build/dd build` → exit 0, `** BUILD SUCCEEDED **`. A pre-fix build and the
  fixed build were run alternately on the booted iPhone 17 Pro (UDID 6B98AE70…, iOS 26.5); report
  and evidence in `build/qa/SUITE-009/` (`REPORT.md`, `fix.diff`, `run.sh`, `logs/`, `screenshots/`):
  - paused + tap a sentence: before — nothing happened (`state paused`, `spoken 710+79`); after —
    `speak chapter 2 from 789`, `state playing`, `spoken 789+229`;
  - paused + tap the blank margin: before — the page turned back (`Trang 5/13` → `4/13`), chrome
    stayed `playerOnly`; after — `chrome full`, `state paused`, same page;
  - paused + tap blank paper inside a column: before — logged `chapter 1018` (nearest sentence);
    after — logged `blank`, chrome `full`;
  - regressions: playing + text tap still seeks, idle + middle tap still toggles the chrome,
    idle + right-third tap still turns the page (`Trang 5/13` → `6/13`).
  - Input fidelity: the first pass used the app's own demo hook (`-demoTapX/-demoTapY`) — the
    same `emitTap` the `UITapGestureRecognizer` calls — because this machine's Xcode 27.1 install
    has no `Simulator.app` and no `idb`/`applesimutils`/`cliclick`. The whole matrix was then
    repeated on the **device hub with real touches** (serve-sim HID → the simulator's digitizer,
    normalized 0..1); Xcode 27's Device Hub had dropped the legacy touch service, fixed with
    `serve-sim repair-input -d <udid>` (restarts SpringBoard, closes the running apps) followed
    by a serve-sim restart and reopening the app:
    - paused + real tap on a sentence: before — `tap 200.0,230.0 column 737 local 107 chapter
      844` with no `speak` line, `state paused`; after — `speak chapter 2 from 789 … wasPaused
      true`, `state playing`, `spoken 789+229`, same page;
    - paused + real tap on the blank paper: before — `tap 200.0,500.0 column 737 local 281
      chapter 1018` (the blank paper taken as the last sentence) and chrome stuck at `playerOnly`;
      after — `tap 200.0,500.0 blank`, `chrome full`, `state paused` unchanged, same page;
    - regressions with real touches: playing + text tap still seeks (`speak chapter 2 from 710`),
      idle + blank tap reveals the chrome without turning the page, idle + right-third tap still
      turns the page (`Trang 5 / 13` → `6 / 13`).
    - Runner: `build/qa/SUITE-009/run-hub.sh`; report §9.
- Not re-verified today: swipe matrix, narration, library percentages (unchanged code paths, and a
  spot check of narration on the picture book started and played without error). No XCTest target
  exists (NOT RUN: unit/UI tests).

Work landed / ownership:
- T12 is implemented and verified but **not committed**: the working tree carries the new
  `EpubReader/Domain/ChapterImages.swift`, changes in `Domain/AttributedBuilder`,
  `Data/EpubArchiveReader`, `Domain/Models`, `Reader/ReaderViewModel`, `Reader/ReaderView` (new
  `-demoColumns` demo hook) and the four state/README files. Committing needs the owner's go-ahead;
  nothing is pushed (no remote).
- The previous commit `742433d` holds the toolkit adoption and this file set; `build/` stays ignored.

Blockers and pending approvals:
- Commit of T12 — waiting for the owner.
- Product decisions outstanding: T11 app icon (needs a 1024×1024 asset), T13 chapter-opening weight,
  T14 fold sheen on dark themes; T15 (tests) is scoped but not started. Each needs its own approved
  plan before code.

Exact next action:
- SUITE-009 is done and verified; its three source files are still uncommitted, so the next
  owner question is whether to commit them (explicit paths, no push) together with, or apart
  from, SUITE-003's three files. Then: which of T11, T13–T15 comes next — write that task's
  plan and wait for approval before touching code.

Resume prompt: Đọc state và đối chiếu files/Git; tiếp tục tác vụ chưa xong trong scope đã duyệt, không discard thay đổi.

---

## Bản tiếng Việt

Thời điểm / múi giờ: 2026-10-22 01:15 +07 (Asia/Ho_Chi_Minh)

Đọc trước: PROJECT_STATE.md, TASKS.md, `git status --short --branch`, `git log -3 --oneline`.

Tệp và bài kiểm đã chạy:
- `xcodebuild -project EpubReader.xcodeproj -scheme EpubReader -configuration Debug
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' -derivedDataPath build/dd
  build` → exit 0, `** BUILD SUCCEEDED **` (Xcode 27.1, 2026-10-21 10:53 +07). Dòng `warning:` duy
  nhất trong log là thông báo của `appintentsmetadataprocessor`; không có cảnh báo nào từ code dự án.
- `python3 .ai-toolkit/scripts/checkpoint.py .` → nhánh `master`, HEAD `742433d` (2026-10-21).
- T12 (ảnh), simulator UDID 6B98AE70-9B11-4915-85AB-A3F347F40381, iOS 26.5, sách thử sinh ra
  `zz-anh-thu.epub` gồm PNG 900×300, 600×400, 300×1200 và một `<img>` trỏ tệp không có:
  - 2 cột, 19 pt → ảnh ngang và ảnh nội tuyến rộng đúng 160 pt (bề rộng cột), ảnh cao 88×353 pt
    (bị chặn ở 55 % của cột 642 pt), mỗi ảnh nằm đúng trang của token, không bị cắt; 3 trang.
  - 1 cột (`-demoColumns one`) → cùng các ảnh ở 342 pt; 4 trang.
  - Nền tối → ảnh giữ nguyên, alt text đọc được; `[Ảnh: Ảnh thiếu tệp]` cho `src` hỏng.
  - Mở lại: `-demoPage 4` → thoát → mở lại → trang 4.
  - Sách mẫu vẫn 13 trang, chữ và cú lật không đổi.
  - Canh lề được đo trên hàng pixel (ảnh 478–735 px trong cột 160 pt có tâm 603 px), không đoán
    bằng mắt.
- Hai lỗi do chính các lần kiểm này phát hiện và đã sửa trong cùng thay đổi: CoreText không đọc
  kích thước attachment nên bộ phân trang xếp ảnh cao 353 pt vào vài point còn lại ở chân cột rồi
  `UITextView` cắt mất (sửa bằng `CTRunDelegate`); và khi một `ReaderViewModel` nhận URL thứ hai,
  nó giữ lại chương đã dựng của sách trước (sửa bằng cách đặt lại trạng thái dựng chữ trong `load`).
- SUITE-009 (chạm khi giọng đọc đang tạm dừng), 2026-10-22: `xcodebuild -project EpubReader.xcodeproj
  -scheme EpubReader -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro,
  OS=26.5' -derivedDataPath build/dd build` → exit 0, `** BUILD SUCCEEDED **`. Chạy xen kẽ bản
  trước sửa và bản sau sửa trên iPhone 17 Pro đang chạy (UDID 6B98AE70…, iOS 26.5); báo cáo và bằng
  chứng ở `build/qa/SUITE-009/` (`REPORT.md`, `fix.diff`, `run.sh`, `logs/`, `screenshots/`):
  - tạm dừng + chạm một câu: trước — không có gì xảy ra (`state paused`, `spoken 710+79`); sau —
    `speak chapter 2 from 789`, `state playing`, `spoken 789+229`;
  - tạm dừng + chạm lề trắng: trước — trang lật ngược (`Trang 5/13` → `4/13`), chrome vẫn
    `playerOnly`; sau — `chrome full`, `state paused`, trang không đổi;
  - tạm dừng + chạm giấy trắng trong cột: trước — log `chapter 1018` (chọn nhầm câu gần nhất);
    sau — log `blank`, chrome `full`;
  - hồi quy: đang đọc + chạm câu vẫn nhảy đúng, rảnh + chạm giữa vẫn bật/tắt thanh công cụ,
    rảnh + chạm 1/3 phải vẫn lật trang (`Trang 5/13` → `6/13`).
  - Độ trung thực của thao tác chạm: lượt đầu dùng móc demo của chính app (`-demoTapX/-demoTapY`)
    — đúng hàm `emitTap` mà `UITapGestureRecognizer` gọi — vì bản cài Xcode 27.1 trên máy này
    không có `Simulator.app`, cũng không có `idb`/`applesimutils`/`cliclick`. Sau đó lặp lại toàn
    bộ bảng kiểm trên **device hub với chạm thật** (serve-sim HID → digitizer của simulator,
    tọa độ chuẩn hoá 0..1); Device Hub của Xcode 27 đã ngắt dịch vụ touch cũ, sửa bằng
    `serve-sim repair-input -d <udid>` (khởi động lại SpringBoard, đóng app đang chạy) rồi
    khởi động lại serve-sim và mở lại app:
    - tạm dừng + chạm thật vào một câu: trước — `tap 200.0,230.0 column 737 local 107 chapter
      844` mà không có dòng `speak`, `state paused`; sau — `speak chapter 2 from 789 … wasPaused
      true`, `state playing`, `spoken 789+229`, trang không đổi;
    - tạm dừng + chạm thật vào giấy trắng: trước — `tap 200.0,500.0 column 737 local 281
      chapter 1018` (giấy trắng bị coi như câu cuối cùng) và chrome kẹt ở `playerOnly`; sau —
      `tap 200.0,500.0 blank`, `chrome full`, `state paused` giữ nguyên, trang không đổi;
    - hồi quy với chạm thật: đang đọc + chạm câu vẫn nhảy (`speak chapter 2 from 710`), rảnh +
      chạm giấy trắng vẫn hiện thanh công cụ mà không lật trang, rảnh + chạm 1/3 phải vẫn lật
      trang (`Trang 5 / 13` → `6 / 13`).
    - Kịch bản dùng lại: `build/qa/SUITE-009/run-hub.sh`; báo cáo §9.
- Chưa kiểm lại hôm nay: ma trận vuốt, đọc thành tiếng, phần trăm thư viện (đường code không đổi;
  có chạy thử giọng đọc trên sách có ảnh, chạy được, không lỗi). Dự án không có target XCTest
  (CHƯA CHẠY: test unit/UI).

Việc đã bàn giao / quyền sở hữu:
- T12 đã làm và đã kiểm nhưng **chưa commit**: cây làm việc có tệp mới
  `EpubReader/Domain/ChapterImages.swift`, sửa `Domain/AttributedBuilder`, `Data/EpubArchiveReader`,
  `Domain/Models`, `Reader/ReaderViewModel`, `Reader/ReaderView` (thêm móc demo `-demoColumns`) và
  bốn tệp state/README. Muốn commit cần anh cho phép; chưa push (không có remote).
- Commit trước `742433d` chứa bộ toolkit và chính bộ tài liệu này; `build/` vẫn bị ignore.

Chỗ tắc và việc chờ duyệt:
- Chờ anh duyệt commit T12.
- Còn chờ anh quyết: T11 icon app (cần asset 1024×1024), T13 độ đậm trang mở đầu chương, T14 nếp
  gấp ở theme tối; T15 (test) đã có phạm vi nhưng chưa làm. Mỗi việc cần kế hoạch được duyệt trước
  khi sửa code.

Hành động tiếp theo chính xác:
- SUITE-009 đã làm xong và đã kiểm; ba tệp nguồn của nó vẫn chưa commit, nên câu hỏi tiếp theo
  cho anh là có commit chúng không (stage đúng đường dẫn, không push), cùng với hay tách riêng ba
  tệp của SUITE-003. Sau đó: việc nào trong T11, T13–T15 làm tiếp — viết kế hoạch cho việc đó và
  chờ duyệt trước khi sửa code.

Prompt resume: Đọc state và đối chiếu files/Git; tiếp tục tác vụ chưa xong trong scope đã duyệt, không discard thay đổi.

---

## 2026-10-24 — UI tinh gọn theo yêu cầu trực tiếp của anh (đã làm xong, chưa commit)

Yêu cầu anh đưa ra và cách đã sửa:
1. Bỏ chữ "Trang Chủ / Thư Viện / Cửa hàng" ở màn hình chính → `App/MainView.swift`: `FloatingTabBar`
   chỉ còn icon, bỏ `Text(verbatim: tab.title)`; tên tab vẫn còn để dùng cho VoiceOver.
2. Sách đang đọc dở bỏ nhãn % rời → `Home/HomeView.swift`: bỏ `ReadPercentLabel` trong
   `ContinueReadingCard`, giữ một dòng duy nhất "Trang x/y · %" (`placeLabel`).
3. Bìa sách không còn tên sách + tên tác giả, cả trang đang đọc lẫn thư viện →
   `Library/LibraryView.swift`: `BookCover` giờ chỉ là gradient màu theo title + gáy sẫm màu.
   Màu vẫn bám theo title nên mỗi sách một màu.
4. Tab Thư viện: mọi chức năng gom vào nút `...` hiện dạng popup (`Menu`) — Chọn, Sắp xếp (submenu),
   Đổi dạng lưới/danh sách; nút `+` import sách ghép đôi ở cạnh còn lại. Đã bỏ `ToolbarItemGroup`
   với 3 nút riêng và bỏ nút cài đặt khỏi toolbar.
5. Nút cài đặt chuyển sang tab Trang Chủ → `Home/HomeView.swift`: `gearshape` ở `.topBarTrailing`,
   mở sheet `AppSettingsView`.

Kiểm chứng: `xcodebuild test` (iPhone 17 Pro, iOS 26.5) → 33 tests, 0 failures;
`xcodebuild build` → BUILD SUCCEEDED. Không sửa test nào vì logic (HomeFeed/LibraryStore/L10n) không đổi.

Chưa commit gì trong lần này; thay đổi nằm trên 3 tệp đã sửa + trạng thái cũ chưa commit từ trước.

### 2026-10-24 (tiếp) — vòng chỉnh thứ hai theo phản hồi của anh
- Khôi phục chữ trên `FloatingTabBar`; bỏ `navigationTitle` ở Home và Library (anh muốn bỏ title
  phía trên, không phải chữ ở tab).
- Library: `+` và `...` gộp vào **một capsule**, dùng đúng metrics/màu của nút trong reader
  (`ReaderMetrics.buttonSize` 44, `buttonIconSize` 15, divider 22, `controlFill`,
  stroke `swiftInk@0.14`, shadow 0.20/7/3).
- Lưới: bìa nhỏ lại (`adaptive(minimum: 96)`, spacing 16/18) → 3 cuốn/hàng.
- Nút `...` của từng cuốn ở dạng lưới: dời xuống cùng hàng với `%`, bỏ khoanh tròn + nền, chỉ còn glyph.
- Dạng list giữ nguyên nút `...` bên phải như cũ (anh sẽ yêu cầu riêng).
- Kiểm chứng: `xcodebuild build` → BUILD SUCCEEDED; đã chụp màn hình Home + Library trên
  iPhone 17 Pro (iOS 26.5) qua DeviceHub.

### 2026-10-24 (tiếp) — vòng chỉnh 3
- Library: cặp nút đổi thứ tự — `...` ngoài cùng, `+` phía trong; bỏ thanh navigation (chỉ hiện khi
  đang chọn nhiều sách) và vẽ cặp nút trên nền giấy để không còn quầng trắng của toolbar glass.
  Nền capsule = `palette.cardFill`, viền `swiftInk@0.10`, bóng 0.08/6/2.
- Menu từng cuốn sách: chỉ còn Chỉnh sửa + Xoá, bỏ Chọn.
- Home: ẩn navigation bar, nút cài đặt tự vẽ (44pt, `cardFill`, viền 0.10) thay cho chip trắng hệ thống.
- Build SUCCEEDED, đã chụp lại Home + Library.

### 2026-10-25 — Full player trong phần đọc sách
- `Reader/NowPlayingView.swift` (mới): sheet mở bằng cách bấm vào **title của player**. Gồm bìa + tên
  sách + tác giả + tên chương, slider tua toàn sách, play/pause cỡ lớn, 2 nút chuyển chương
  (trước/sau), menu hẹn giờ tắt (Tắt / 5 / 15 / 30 / 60 phút) và menu chọn giọng đọc.
- `SpeechController`: thêm `skipChapter(by:)`, `setSleepTimer(minutes:)` (Task ngủ rồi pause),
  `availableVoices()`, `setVoice(id:)` (lưu UserDefaults `narrationVoiceID`), `selectedVoice` dùng
  cho utterance.
- `ReaderViewModel.narrationPlace(fraction:)` + `NarrationMath.place` (thuần, có test).
- `ReaderView`: `showNowPlaying`, title player bọc trong Button, sheet mới; thêm hook
  `-demoNowPlaying <giây>` để chụp ảnh.
- L10n: thêm 8 key ở cả en/vi (reader.player.open/progress/previous/next, reader.sleep,
  reader.sleep.off/minutes, reader.voice).
- Kiểm chứng: `xcodebuild test` → **37 tests, 0 failures** (thêm `NarrationMathTests`, 4 test);
  ảnh chụp sheet trên iPhone 17 Pro.
- Chưa kiểm bằng tay: bấm title để mở (demo mở bằng state), kéo slider, chờ hết hẹn giờ, đổi giọng
  trên máy có nhiều voice.

### 2026-10-25 (tiếp) — chỉnh popup player
- Bìa 54 → 96; thêm dòng "Tốc độ đọc · 1×" dưới tên chương.
- Trên slider: trái "Trang x/y", phải "%" (truyền `pageNumber`/`pageCount` từ ReaderView).
- Hẹn giờ tắt và giọng đọc thành 2 nút icon tròn, bấm ra menu; icon hẹn giờ đổi màu accent khi
  đang bật.
- `ReaderPalette.popupPaper` (giấy reader +14% trắng sáng / +12% tối) và `popupControlFill`
  (+55% / +28%) cho nền panel và nút; dùng thêm `.presentationBackground` để nền sheet cũng theo màu.
- Kiểm chứng: 37/37 test xanh; ảnh chụp theme "paper" xác nhận panel sáng hơn trang, nút nổi rõ.
- Lưu ý: theme "original" có giấy trắng nên panel cũng trắng — khi đó chỉ còn bóng đổ phân tách.

### 2026-10-25 (tiếp) — popup player, vòng 2
- Tốc độ đọc giờ là **menu bấm được** (các mốc 0.5×…1.5×), có chevron; trước đó chỉ là chữ.
- Hẹn giờ + giọng đọc gộp vào **cùng hàng** với chuyển chương và play/pause (5 nút, spacing 12;
  play 66 → 62 cho vừa).
- Nút trong popup dùng `palette.controlFill`/`swiftInk` — đúng màu nút của màn đọc theo theme;
  bỏ `popupControlFill` khỏi `ReaderTheme`.
- 37/37 test xanh; ảnh chụp xác nhận.

### 2026-10-25 (tiếp) — popup player, vòng 3
- Tốc độ thành nút `− 1× +` full-width (cao 48), nằm dưới hàng transport.
- Transport: sleep (trái ngoài cùng) · lùi chương · play 60 · tiến chương · giọng đọc;
  bốn nút phụ đều 48pt.
- Nguyên nhân "backward/forward không ấn được": nút bị `.disabled` khi `canSkip*` false.
  Demo resume ở chương cuối nên ⏭ luôn tắt (opacity 0.3, chạm không ăn). Bỏ disabled;
  `skipChapter` clamp qua `NarrationMath.skipTarget` — ở đầu/cuối sách thì phát lại chính chương đó,
  nên nút luôn phản hồi. Thêm 1 test (38/38 xanh).
- Nút dùng `palette.controlFill` / `swiftInk` / `swiftAccent` nên đổi đúng theo theme + dark.
- Kiểm chứng: build xanh, 38/38 test, ảnh chụp theme paper (sáng) và focus (tối).
- Không tap được bằng máy: máy này không có Simulator.app GUI và không có công cụ inject tap,
  nên hành vi chạm chỉ suy ra từ logic + ảnh chụp.

### 2026-10-25 (tiếp) — popup player, vòng 4
- Full player không còn tự mở khi vào sách: hook demo `demoNowPlaying`/`demoSpeak` giờ chạy
  **một lần mỗi lần launch** (`ReaderView.demoApplied`, `demoNarrationApplied` static) thay vì mỗi
  lần mở sách. Mở sách chỉ hiện player row; bấm tiêu đề mới mở full player.
- Nút ⏮ disable ở chương đầu, ⏭ disable ở chương cuối (`canSkipBackward/Forward` dùng lại;
  bỏ `NarrationMath.skipTarget` + test vòng 3).
- Nút `− 1× +` chuyển vào bookRow, ngay dưới tên chương, dạng capsule nhỏ (cao 34, không tràn ngang).
- Sheet dùng detent `.height(352)` thay `.medium` → panel thấp hơn, bớt khoảng trống.
- Kiểm chứng: build xanh, 37/37 test; ảnh chụp: sách mở với player row (không tự mở sheet),
  và sheet mới có nút tốc độ dưới chương.

### 2026-10-25 (tiếp) — popup chọn giọng đọc (giọng hệ thống)
- Nút giọng trong full player giờ mở `VoicePickerSheet` (sheet, không còn `Menu`).
- `VoiceCatalog` nhóm giọng đã cài theo ngôn ngữ/vùng (`vi-VN`, `en-GB`…), Tiếng Việt lên đầu,
  tên giọng A→Z; tiêu đề nhóm lấy bản địa hoá của locale (vd "Tiếng Việt (Việt Nam)").
- Mỗi dòng: nghe thử (play/stop, `VoicePreviewer` riêng, tạm dừng narration khi nghe thử),
  nhãn chất lượng + locale, nút "Dùng"/✓ cho giọng đang dùng.
- Cuối sheet: khối "Muốn thêm giọng đọc?" giải thích iOS chỉ cho tải trong Cài đặt + nút mở
  `UIApplication.openSettingsURLString` (không dùng deep link private).
- `SpeechController.availableVoices()` trả về **toàn bộ** giọng đã cài (trước chỉ lọc `vi`);
  giọng mặc định vẫn là tiếng Việt.
- L10n: thêm 16 key `reader.voice.*` ở cả `en.lproj` và `vi.lproj` (khớp key).
- Test: thêm `EpubReaderTests/VoiceCatalogTests.swift` (4 test) → **41/41 pass**.
- Ảnh chụp simulator: sheet hiện 68 giọng đã cài, nhóm Tiếng Việt đầu tiên, giọng "Linh" đang dùng.
- Chưa commit.

### 2026-10-25 (tiếp) — tìm kiếm giọng + mặc định theo ngôn ngữ máy
- Popup giọng có ô tìm kiếm: gõ tên ngôn ngữ ("việt", "anh"), mã locale ("en-GB") hoặc tên giọng
  ("Linh"); khớp ngôn ngữ thì giữ cả nhóm, khớp tên thì lọc đúng dòng. Không có kết quả → câu báo.
- Thứ tự nhóm: **ngôn ngữ của điện thoại trước**, rồi Tiếng Việt, rồi A→Z; trong nhóm vẫn A→Z.
- Giọng mặc định khi user chưa chọn: theo `Locale.preferredLanguages.first`, fallback `vi-VN`,
  rồi giọng vi bất kỳ, rồi giọng đầu tiên đã cài (`SpeechController.defaultVoice()`).
- L10n thêm `reader.voice.search`, `.clearSearch`, `.noResults` (en + vi khớp key).
- Test: VoiceCatalogTests lên 8 test (ưu tiên ngôn ngữ máy, tìm theo ngôn ngữ, theo tên, query rỗng)
  → **45/45 pass**. Ảnh chụp: ô tìm kiếm + nhóm Tiếng Việt đầu danh sách.
- Chưa làm: tải engine/model TTS open-source trong app (tuỳ chọn, chưa triển khai).
- Chưa commit.

### 2026-10-25 (tiếp) — giọng theo ngôn ngữ sách, tên giọng cạnh tốc độ
- Popup giọng **không tự mở**: chỉ còn một chỗ đặt `showVoices = true` là nút voice trong full
  player. (Lần trước bản cài trên simulator còn hook chụp ảnh `demoVoices`; hook đã gỡ và **đã cài
  lại bản sạch** — xác nhận bằng grep + ảnh chụp mở sách: chỉ có player row.)
- Cạnh `− tốc độ +` trong bookRow hiện **tên giọng đang đọc** (`speech.currentVoiceName`).
- Giọng mặc định/giọng đã lưu phải **khớp ngôn ngữ sách**: `SpeechController.bookLanguage` phát hiện
  bằng `NLLanguageRecognizer` trên ~1200 ký tự đầu; giọng lưu mà khác ngôn ngữ sách thì bị bỏ qua,
  dùng `defaultVoice(for:)` (sách → ngôn ngữ máy → vi-VN → giọng vi → giọng đầu).
- Popup chỉ liệt kê giọng cùng ngôn ngữ sách (`VoiceCatalog.voices(_:speaking:)`); nếu máy không có
  giọng nào cho ngôn ngữ đó thì hiện tất cả để không bị sheet rỗng. Tiêu đề đổi thành
  "N giọng cho sách này".
- Test: thêm 2 test lọc theo ngôn ngữ sách → **47/47 pass**.
- Ảnh chụp: popup chỉ có nhóm Tiếng Việt (Linh ✓, "1 giọng cho sách này"); player có "Linh" cạnh 1×.
- Chưa commit.

### 2026-10-25 (tiếp) — sách mẫu đa ngôn ngữ
- Thêm `SampleBooks/make_books.py` + 5 EPUB mẫu (2 chương, có `dc:language`):
  01 English, 02 French, 03 Spanish, 04 Japanese, 05 Chinese.
- Đã copy vào simulator `Documents/Books` (cùng 3 sách cũ) → thư viện 8 sách.
- Kiểm chứng lọc giọng theo ngôn ngữ sách: English → 25 giọng en (en-IN, en-US…);
  Japanese → 1 giọng ja (Kyoko); Chinese → 2 giọng zh (Tingting zh-CN, Meijia zh-TW).
- Chưa mở thử French/Spanish; chưa commit (SampleBooks/ đang untracked).

### 2026-10-25 (tiếp) — bố cục player + hết nháy nền khi thoát sách
- BookRow của full player theo đúng bố cục yêu cầu: tên chương → đường kẻ mảnh →
  "Giọng đọc: <tên>" → `− tốc độ +` (capsule) trên dòng riêng. Key mới:
  `reader.voice.currentName` = "Giọng đọc: %@" / "Voice: %@".
- Nháy nền: `.preferredColorScheme` của reader nằm trên ReaderView nên sau khi pop, thư viện
  vẫn nhận scheme tối cho tới khi animation xong → nền đen rồi mới trắng. Đã **chuyển** preference
  lên `NavigationStack` của `LibraryView` và `HomeView`, chỉ áp khi `path` không rỗng
  (`readerScheme`), và **bỏ** dòng đó khỏi ReaderView. Khi pop, path rỗng ngay → thư viện sáng
  suốt transition; status bar reader vẫn trắng vì path còn sách.
- Kiểm chứng: chụp liên tiếp lúc pop (12 frame, hook tạm `-demoCloseAfter`): frame giữa transition
  thấy thư viện trắng bên trái, reader tối trượt ra; frame cuối thư viện trắng hoàn toàn.
  Hook tạm đã gỡ, build + cài lại bản sạch; 47/47 test vẫn xanh.
- Chưa commit.

### 2026-10-25 (tiếp) — kiểm tra ngôn ngữ theo máy
- Commit `9da6500` đã tạo. `COORDINATOR_STATUS.md` để ngoài commit.
- Máy simulator: `AppleLanguages = (en-VN, vi-VN)`, `AppleLocale = en_VN` (Anh là chính).
- App đang **ghim tiếng Việt**: prefs app `appLanguage = vi`, `AppleLanguages = (vi)` — do đã chọn
  trong Settings trước đó, không phải giá trị mặc định của bản cài mới.
- Bỏ override (launch `-appLanguage system -AppleLanguages '(en-VN)'`) → UI đổi sang tiếng Anh
  ("READING", "Page 3/4", "Continue reading") ⇒ cơ chế `system` theo ngôn ngữ máy chạy đúng.
- Đã trả prefs về như cũ (`appLanguage = vi`, `AppleLanguages = (vi)`) và chụp lại UI tiếng Việt.
- Lưu ý cần xử lý nếu muốn "Theo máy": chọn system chỉ xoá `AppleLanguages` cho lần chạy sau;
  `Bundle.main` đã chốt localization từ lúc launch nên UI hiện tại không đổi ngay (phải relaunch).
- Đã gửi spec full player cho Android: `android/docs/full-player-parity.md`, prompt qua Orca terminal;
  Android đang kiểm tra/sửa (chưa commit).

### 2026-10-25 (tiếp) — quy tắc ngôn ngữ: Việt nếu máy ưu tiên Việt, còn lại English
- `AppLanguage.resolved(for: Locale.preferredLanguages)` + `AppLanguage.device`: chỉ khi phần tử
  **đầu tiên** bắt đầu bằng `vi` mới là tiếng Việt; mọi trường hợp khác (kể cả danh sách rỗng)
  là tiếng Anh. Không dựa vào fallback của `Bundle.main` (trước đây có thể chọn `vi` nhờ vị trí 2).
- `L10n.bundle(for: .system)` lấy bundle theo `AppLanguage.device`; `L10n.locale` cũng theo ngôn ngữ
  đã resolve (số/trang định dạng đúng ngôn ngữ đang hiển thị).
- `EpubReaderApp.init`: khi đang ở "Theo máy" thì xoá `AppleLanguages` còn sót từ lần chọn trước,
  để app thực sự theo máy ở lần mở sau.
- Test: +2 test `AppLanguage.resolved(for:)` (vi-VN đầu → vi; en-VN/ja/rỗng → en) và
  `bundle(.system) == bundle(.device)`. Tổng **49/49 xanh**.
- Kiểm chứng trên simulator mới (iOS 26.5, cài sạch, không prefs): máy `en-VN` (chính) → UI tiếng Anh
  ("READING", "Page 3/4 · 60%", "Continue reading"); đổi `AppleLanguages` máy sang `vi-VN` → UI tiếng
  Việt ("Chưa có sách đang đọc", "Mở Thư viện", "Trang chủ"). Simulator test đã xoá.
- Simulator chính: đã bỏ override (`appLanguage`/`AppleLanguages`) và khôi phục metadata + tiến độ đọc
  từ backup prefs; hiện UI tiếng Anh theo máy (en-VN), 8 sách còn nguyên.
- **Chưa commit** phần ngôn ngữ.

### 2026-10-25 (tiếp) — kiểm thử EPUB lạ + hiệu năng sách lớn
- Đã tạo corpus và chạy qua pipeline thật; chi tiết + số đo trong `build/qa/epub-corpus/REPORT.md`.
- 4 lỗi: DRM/LCP vẽ ký tự rác thay vì báo lỗi; chapter lookup phân biệt hoa/thường (mất chương);
  entry `./` bị từ chối; nav lỗi XML mất tiêu đề chương.
- Sách 1.500 trang mở 1,34 s, 1.000 chương 4,51 s; chưa sửa gì, chờ duyệt hướng sửa.

### 2026-10-25 (tiếp) — phương án B đã sửa xong (chưa commit)
- `EpubArchiveReader`: `entryIndex` + `data(for:in:index:)` (bỏ "./", fold hoa/thường) dùng cho
  container, OPF, nav, NCX, chương, ảnh; `ensureUnprotected` đọc `META-INF/encryption.xml`.
- `EpubError.protected` + chuỗi vi/en; Reader hiện thông báo thay vì ký tự rác.
- `LibraryStore.importBook`: `validate` cấu trúc + DRM + spine trước khi thêm vào kệ; file bị từ chối
  thì xoá bản copy.
- `LibraryTests` dùng sample.epub thật thay cho file giả. 54/54 test xanh.
- Tiếp theo: chờ duyệt commit phần sửa.

### 2026-10-25 (tiếp) — reading stats + finished (đã commit từng task)
- Task 1–10 theo `docs/superpowers/plans/2026-10-25-reading-stats.md`; commit
  `3640250..HEAD` (store, streak/summary, session, reader wiring, finished data/UI, card, sheet,
  settings goal). 79/79 test xanh.
- Đã kiểm: ghi thời gian thật khi mở sách + TTS (readingStats trong prefs), card vi/en/dark,
  sheet, goal 60 qua `-readingGoalMinutes`, nhãn "Đã đọc xong".
- Chưa kiểm bằng chạm: menu đánh dấu xong và Picker mục tiêu trong Settings (không có inject tap);
  logic đã có unit test. Chart/heatmap để phase sau.
- Simulator: appearance trả về light; app đang mở Home với dữ liệu demo (seed 1 lần).

### 2026-10-26 — Home sections (chưa commit)
- Thêm `EpubReader/Home/HomeSections.swift`, `HomeBookRow.swift`, `WeeklyReadingChart.swift`;
  sửa `HomeView`, `HomeFeed` (limit 5), `ReadingStatsStore.week`, `LibraryStore.allMetadata`, strings.
- Quy tắc: <6 sách chỉ card + đang đọc; mỗi hàng tối đa 5; gợi ý cùng tác giả, cần 2 cuốn,
  loại trùng Mới thêm/Đọc xong; chart ẩn khi tuần 0 phút.
- 89/89 test; ảnh iPad/iPhone trong `build/qa/home-sections/`. Demo đã hoàn nguyên prefs + sách.
- Bước tiếp theo: chờ duyệt commit.

### 2026-10-26 (tiếp) — demo Home đầy đủ trên simulator
- Đã thêm 6 sách demo (demo-01…06) + đổi metadata 2 sách cũ và vị trí đọc để Home hiện đủ:
  ĐANG ĐỌC 3 · MỚI THÊM 5 · GỢI Ý 2 · ĐỌC XONG 3 · TUẦN NÀY 22 phút (thứ Hai).
- Ảnh: `build/qa/home-demo/` (iPhone + iPad đủ trang). App trên simulator đang mở Home vi
  với dữ liệu demo; **prefs gốc lưu ở `/tmp/prefs-before-home-demo.plist`** để hoàn nguyên.
- Sách demo trong thư viện simulator chỉ để xem, không nằm trong repo; code vẫn chưa commit.

### 2026-10-26 (tiếp) — chỉnh theo phản hồi giao diện Home
- Heading các khối lên 14 pt (tracking 1.0); khoảng cách giữa các khối 12 → 24.
- MỚI THÊM / GỢI Ý / ĐỌC XONG dùng cùng kiểu card với ĐANG ĐỌC: bìa 64, tên 17 serif
  semibold (2 dòng), tác giả 12, phụ đề 11, nền cardFill bo 14; rộng 260.
- GỢI Ý bỏ lặp tên tác giả ở phụ đề, thay bằng "Cùng tác giả"/"Same author".
- Biểu đồ: cao 88 (trước 58), cột rộng 18 (trước giãn hết cột), nhãn thứ 11 pt.
- 89/89 test xanh. Ảnh mới: `build/qa/home-demo/home-polish-{phone,ipad}.png`.
- Đã xoá appLanguage/AppleLanguages bị ghim trong prefs (app về theo ngôn ngữ máy: English);
  dữ liệu demo Home vẫn còn trên simulator. Backup gốc: `/tmp/prefs-before-home-demo.plist`.

### 2026-10-26 (tiếp) — shelf liền mạch + marquee + chart cao
- Ngoài ĐANG ĐỌC, các hàng bỏ card nền/viền: bìa 96 đứng liền nhau (spacing 16), không còn
  khung rời. Tên sách 17 serif semibold dùng `MarqueeText` (cửa sổ ~5–6 chữ, chạy ngang khi dài),
  tác giả 12, phụ đề 11.
- Heading 14 pt; khoảng cách khối 24 pt.
- Biểu đồ cao 88, cột 18 pt, nhãn 11 pt.
- 89/89 test. Ảnh: `build/qa/home-demo/home-shelf-{phone,ipad}.png`.

### 2026-10-26 (tiếp) — một khung chung cho mỗi hàng
- Các hàng ngoài READING bọc trong **một** khung card (nền cardFill, viền, bo 16) chứa tối đa
  5 sách, cuộn ngang bên trong; bỏ khung từng quyển. Tiêu đề hàng nằm ngoài khung.
- Ảnh: `build/qa/home-demo/home-rowframe-phone.png`. 89/89 test. Chưa commit.

### 2026-10-26 (tiếp) — khung hàng tràn mép màn hình
- Khung của MỚI THÊM / GỢI Ý / ĐỌC XONG kéo rộng ra ngoài hai mép (-20 mỗi bên), góc bo nằm
  ngoài màn hình; content margin trong 36 để bìa đầu cách mép ~16.
- Ảnh: `build/qa/home-demo/home-bleed-phone.png`. 89/89 test. Chưa commit.

### 2026-10-26 (tiếp) — khung chỉ tràn phải
- Khung hàng: lề trái 20 pt (góc bo trái thấy), phải kéo ra ngoài -20 pt (góc phải nằm ngoài
  màn hình). Content margin trong 14.
- Ảnh: `build/qa/home-demo/home-rightbleed-phone.png`.
- Lưu ý: trong lúc dựng lại simulator có uninstall/reinstall để xoá scroll state; books + prefs
  demo đã copy lại từ `/tmp/home-demo-state/`, dữ liệu demo còn (22 phút, 4 ngày chuỗi).

### 2026-10-26 (tiếp) — title Home/Library + ẩn gear khi cuộn
- Home và Library có lại tiêu đề lớn ("Trang chủ"/"Thư viện") ở đầu nội dung (30 pt serif),
  thay cho khoảng trống trên cùng; bỏ padding top 56/58 cũ.
- Gear ở Home chỉ hiện khi `contentOffset.y <= 1` (đầu trang), ẩn khi cuộn; dùng
  `onScrollGeometryChange` + fade 0.2 s.
- Ẩn scroll indicator dọc ở cả Home và Library (`.scrollIndicators(.hidden)`).
- Ảnh: `build/qa/home-demo/home-title-phone.png`, `library-title-phone.png`.
