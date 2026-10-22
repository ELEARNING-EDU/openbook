---
name: adt-unity-development
description: Develop debug and review Unity C# games using the existing editor version scenes prefabs serialization and test setup.
---

# Unity development

## Xác định project và giới hạn
Dùng cho Unity C# kể cả game export Android/iOS/macOS; chỉ tải native platform skill khi sửa bridge/plugin native tương ứng. Đọc ProjectSettings/ProjectVersion.txt, Packages/manifest.json, package lock, asmdefs và scene/prefab liên quan. Giữ editor version, render pipeline và Input System hiện tại; không tự upgrade engine/package hoặc migrate Built-in/URP/HDRP.

## Thiết kế gameplay
Giữ gameplay rules và visual đã được duyệt. Pure C# cho logic có lợi khi test; MonoBehaviour cho lifecycle và liên kết engine. ScriptableObject phù hợp config/assets, không mặc định làm mutable global runtime state. Inject references rõ ràng, tránh service locator/singleton ẩn; dùng asmdefs theo ranh giới có ích, không thêm assembly cho mọi folder. Editor-only APIs phải tách khỏi runtime build.
Quản lý Awake/OnEnable/Start/OnDisable/OnDestroy: subscribe/unsubscribe đúng lifetime, reset pooled objects, cancel asynchronous work khi owner bị hủy. Không giả định callback giữa hai GameObject có thứ tự ổn định. Không gọi Unity APIs cần main thread từ worker. Update dùng thời gian phù hợp; physics tick, input sampling và pause/unscaled time phải theo gameplay đã xác minh.

## Bảo toàn assets và serialization
Giữ .meta/GUID khi move/rename bằng workflow Unity hoặc công cụ giữ nguyên identity. Không xóa/recreate assets để né conflict. Serialized fields, prefab overrides và scene references có thể vỡ khi đổi tên/type; dùng migration hoặc FormerlySerializedAs khi phù hợp và kiểm tra data. Không sửa YAML hàng loạt thiếu validation. Không sửa Library/Temp hay generated cache làm nguồn truth. Không đổi player settings, signing, build target hoặc package lock ngoài scope.
Profile trước tối ưu: allocations trong frame loop, instantiate/destroy, pooling và loading chỉ điều chỉnh khi có bằng chứng. Không tự chuyển toàn bộ sang ECS/Jobs/Addressables.

## QA và handoff
Xác minh Unity Test Framework package có sẵn. EditMode cho pure logic/editor behavior; PlayMode cho scene/lifecycle/physics/UI khi cần. CLI batchmode/test arguments phải tra đúng editor/Test Framework; lấy exit status và test report, không chỉ xem process kết thúc. Batch smoke không thay thế chơi thật hoặc test device. Kiểm tra compile, missing scripts/references, prefab overrides, save/load, scene transition, pause/restart và platform build theo thay đổi. Báo editor version, target, test mode, kết quả và NOT RUN nếu thiếu editor/license/device. Không tự mở project để trigger migration chưa duyệt.

Nguồn: [Unity serialization](https://docs.unity3d.com/6000.0/Documentation/Manual/script-serialization.html), [assembly definitions](https://docs.unity.com/en-us/engine/6000.3/manual/programming-environment/script-compilation/assembly-definition-files). Dùng docs khớp phiên bản project. Tuân thủ AGENTS.md, không tự spawn agents; checkpoint file/scene thay đổi trước handoff.
