---
name: adt-godot-development
description: Develop debug and review Godot GDScript or C# projects with version-aware scenes resources signals and export validation.
---

# Godot development

## Khảo sát trước sửa
Dùng cho Godot GDScript/C# và export game; không áp Unity conventions. Đọc project.godot, config_version, feature tags, scenes, autoloads, addons và .csproj nếu có; xác minh engine executable/version thực tế. Không suy ra minor version chỉ từ config_version. Phân biệt Godot 3/4, standard/.NET build và renderer; không tự migrate engine, renderer, GDScript syntax hay addons. Với C#, đối chiếu Godot .NET và SDK tương thích; không hứa mọi export target đều hỗ trợ C#.

## Scene architecture
Scene tự chứa, public interface rõ; tránh hard-code đường dẫn sâu xuyên scene khác. Parent/composition root nối dependencies; signals cho event, direct method khi dependency rõ. Không tạo event bus/autoload cho mọi quan hệ. Autoload chỉ cho service có lifetime toàn app thực sự; kiểm soát reset khi đổi màn/restart.
Tách game rules có thể test khỏi Node lifecycle khi có ích, không ép Clean Architecture nhiều tầng cho từng node. Dùng typed GDScript hoặc C# nhất quán với project, tránh dynamic lookup che lỗi tên node.

## Lifetime, resource và identity
Phân biệt _enter_tree, _ready và _exit_tree; bảo vệ callback async sau khi node bị free, quản lý signal connection tránh đăng ký lặp. Không dùng node đang queued for deletion; dùng queue_free/deferred operations khi phù hợp tree/physics mutation. Logic vật lý chạy tick phù hợp, input actions từ Input Map, tính movement theo delta khi API/phiên bản cần.
Resource có thể được chia sẻ giữa instances: không mutate config dùng chung nếu state phải riêng; duplicate/local-to-scene theo semantics engine và kiểm tra nested resources. Giữ .tscn/.tres references, scene ownership, exported properties và UID files theo phiên bản. Không xóa .uid, rename resource bằng raw filesystem hoặc sửa scene hàng loạt mà không kiểm tra references. .godot/ là cache, không là source truth. Save runtime data vào user:// theo design, không ghi res:// trong exported app; xử lý version/migration và write failure.

## Kiểm thử và export
Dùng --help của executable đúng bản trước chọn flags. Headless import/check chạy trong project copy hoặc sau khi chấp nhận cache/import side effects; không gọi mọi thao tác headless là read-only. --check-only/--script có phạm vi script cụ thể, không chứng minh toàn scene/runtime. Godot không có một lệnh unit test mặc định dùng cho mọi repo: dùng framework đã cài (như GUT/GdUnit) hoặc harness hiện có, không tự cài addon.
Smoke test scene instantiation, signal wiring, input/focus, pause, restart/scene transition, resource state isolation và save/load theo thay đổi. Headless không chứng minh rendering/audio/game feel. Export cần preset/templates/SDK đúng bản; không log export credentials hoặc thay preset ngoài scope. Báo version, command, exit status và NOT RUN nếu thiếu runtime/export templates.

Nguồn: [Scene organization](https://docs.godotengine.org/en/stable/tutorials/best_practices/scene_organization.html), [Command line](https://docs.godotengine.org/en/stable/tutorials/editor/command_line_tutorial.html). stable docs có thể khác project; chuyển về version đúng trước dùng API/flags. Làm tuần tự, giữ gameplay/visual và approvals theo AGENTS.md, không tự spawn agents.
