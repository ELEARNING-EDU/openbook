---
name: adt-ios-clean-architecture
description: Implement or review native iOS Swift and SwiftUI features with pragmatic Clean Architecture.
---

# ios-clean-architecture

Đọc cấu trúc, deployment target và Swift language mode thực tế. Giữ conventions hiện có; không tạo nhiều tầng chỉ để theo mẫu.
Dependency hướng vào domain: Domain chứa entity/value object và repository protocol; Data triển khai repository, DTO mapping, persistence/network; Presentation chứa SwiftUI và state/view model. Composition root inject dependencies qua initializer, tránh service locator và singleton ẩn. Use case chỉ thêm khi có orchestration/business rule cần tách.
Domain không import SwiftUI, UIKit hay SDK mạng. Repository trả domain models/errors; ánh xạ DTO ở Data. Không gộp domain với Core Data managed object.
UI state và mutation ở @MainActor; async/await có cancellation, kiểm tra Task cancellation trước publish state. Không dùng Task.detached để né actor isolation; không thêm @unchecked Sendable để dập cảnh báo. Chọn ObservableObject hay Observation theo target thực tế. Tránh retain cycles và task sống lâu hơn màn hình.
Phân biệt loading/empty/error/content; hỗ trợ retry, accessibility, Dynamic Type và localization. Credentials vào Keychain theo yêu cầu, không UserDefaults/log. Không đưa API keys trong bundle như bí mật an toàn.
Test use case và state transitions bằng fake repository deterministic; kiểm tra lỗi, cancellation và race response. Chỉ chạy xcodebuild với scheme/destination đã xác minh. Nếu không có macOS/Xcode, ghi rõ chưa build iOS; test mock không chứng minh runtime.
Bàn giao mapping file/layer, quyết định tradeoff và bằng chứng test. Chỉ thay kiến trúc rộng sau gate duyệt.
