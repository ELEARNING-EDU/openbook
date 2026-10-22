---
name: adt-macos-development
description: Develop and review native macOS Swift SwiftUI or AppKit applications including windows documents sandbox and desktop behavior.
---

# macOS development

## Khảo sát đúng desktop target
Dùng cho native macOS SwiftUI/AppKit; không áp riêng quy trình iOS cho desktop hoặc game export macOS. Đọc target/deployment version, Swift language mode, entitlements, package dependencies và scheme. Phân biệt native macOS, Catalyst và shared Swift package; chỉ tải skill iOS thêm khi task thực sự chạm iOS. Không tự đổi deployment target, signing hoặc bật/tắt sandbox để né lỗi.

## Kiến trúc và concurrency
Giữ kiến trúc hiện tại. Tách pure business logic khỏi UI và filesystem/network adapters; inject dependencies ở composition root. SwiftUI view state và AppKit UI mutation tuân thủ main actor; gắn task lifetime với owner/window và xử lý cancellation. Không dùng Task.detached hoặc @unchecked Sendable để che race. Swift compiler mode và isolation defaults phải được xác minh theo project, không giả định mọi class được main-isolate.
Bridge NSView/NSViewController qua representable khi cần, tránh dựng lại cả control mỗi update; quản lý delegate, observation và retain cycle. Tách state theo window/document khỏi app-level services để không rò dữ liệu giữa hai cửa sổ.

## Hành vi desktop và dữ liệu
Xác minh mở/đóng/reopen nhiều cửa sổ, menu commands, shortcuts, focus/first responder, keyboard navigation, undo/redo và settings theo phạm vi. Với app tài liệu, xác định ai sở hữu document, dirty state, autosave và xung đột ghi; không thêm autosave làm mất bản gốc. Dùng file picker và quyền sandbox phù hợp. Nếu giữ quyền file qua lần chạy, quản lý security-scoped bookmark và cân bằng start/stop accessing trên mọi nhánh theo API thực tế; không tự xin quyền rộng hơn.
Không log đường dẫn nhạy cảm/tokens; credentials dùng Keychain theo yêu cầu. Không đưa secrets trong UserDefaults hoặc bundle. Entitlement, notarization, hardened runtime và distribution channel chỉ thay sau khi đã review tác động/phê duyệt.

## QA
Test pure logic trước, rồi target macOS với scheme/destination đã liệt kê. Khi liên quan, smoke test reopen, multi-window isolation, menu validation, clipboard/drag-drop, resize, accessibility và file permission revoked/stale bookmark. Swift test không chứng minh AppKit runtime; build thành công không chứng minh signed distribution/notarization. Thiếu macOS/Xcode thì ghi NOT RUN, không bịa chứng cứ.

Tài liệu tham khảo theo tác vụ: [AppKit](https://developer.apple.com/documentation/appkit), [SwiftUI concurrency](https://developer.apple.com/videos/play/wwdc2025/266/). Duy trì approval gates và checkpoint theo AGENTS.md; không tự refactor UI/kiến trúc hay spawn agents.
