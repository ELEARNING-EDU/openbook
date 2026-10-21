---
name: adt-android-development
description: Develop and review native Android Kotlin Java Compose or Views projects using their existing architecture and Gradle toolchain.
---

# Android development

## Phạm vi và khảo sát
Dùng cho native Android Kotlin/Java, Compose hoặc XML/Views; không áp cho Unity/Godot chỉ vì export Android. Đọc settings.gradle(.kts), module build files, version catalog, manifest và Gradle wrapper. Ghi nhận AGP/Kotlin/JDK, min/target SDK, variants, DI và navigation hiện tại. Không tự nâng toolchain/dependencies hoặc migrate Views sang Compose.

## Kiến trúc và dữ liệu
Giữ conventions hiện có; UI → state holder/ViewModel → repository → data source. Chỉ thêm domain/use case khi có business logic cần dùng lại hoặc tách phức tạp; không tạo module cho từng class. Constructor injection; dùng Hilt/Koin/manual DI đang có, không tự thêm framework. Repository chịu trách nhiệm data mapping và nguồn dữ liệu; UI không trực tiếp query database/API.
Dùng immutable UI state, actions rõ ràng và state hoisting. Với Compose, collect Flow theo lifecycle, không thực hiện IO trong composition; quản lý effect keys và cancellation. Với Views, gắn collection vào viewLifecycleOwner/repeatOnLifecycle khi phù hợp. Không giữ Activity/View trong ViewModel, không GlobalScope cho công việc màn hình. Dùng SavedStateHandle/persistence cho trạng thái cần phục hồi; ViewModel không sống qua process death.

## Platform correctness
Suspend/Flow không tự biến blocking IO thành nonblocking: chọn dispatcher phù hợp và inject khi cần test. Không nuốt CancellationException. Room migration phải bảo toàn dữ liệu; không thêm destructive migration để chữa lỗi build. WorkManager cho việc bền vững phù hợp, không thay mọi coroutine bằng background worker. Xử lý permission denied/permanently denied, rotation, process recreation và offline/error/retry khi liên quan.
Giữ signing keys, keystore, google-services/secrets ngoài logs và patch ngoài scope; API key đóng trong APK không phải secret an toàn. Kiểm tra exported components, intent inputs và network policy khi thay phần tương ứng.

## Review và QA
Chọn task bằng Gradle wrapper và variants thực tế, không đoán tên task; Windows dùng gradlew.bat. Unit test logic/state bằng fake repositories và coroutine test scheduler; instrumentation/UI test cho navigation, lifecycle, permissions và persistence cần runtime. Không khẳng định emulator test đã chạy nếu chỉ compile/unit test. Kiểm tra accessibility, font scale, edge-to-edge/insets và kích thước cửa sổ theo UI thay đổi. Báo commands, exit status, SDK/device và NOT RUN nếu thiếu toolchain.

Đọc tài liệu đúng phiên bản khi cần: [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations). Làm tuần tự theo AGENTS.md; checkpoint sau milestone, không tự spawn agents hay đổi kiến trúc rộng.
