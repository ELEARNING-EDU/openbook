---
name: adt-sequential-workflow
description: Plan and execute development in sequential approved phases with bounded context and verification.
---

# sequential-workflow

1. Recover trạng thái trước khi lập kế hoạch. Xác định outcome, acceptance criteria, phạm vi và cách test.
2. Với tác vụ nhiều bước, đưa kế hoạch ngắn và chờ người dùng duyệt; lưu nội dung/phạm vi được duyệt trong PROJECT_STATE.md. Không tự nhận đã duyệt. Nếu yêu cầu đã đủ cụ thể cho việc nhỏ, tiến hành trong phạm vi đó.
3. Triển khai một lát cắt hoàn chỉnh, không spawn agents. Đọc file theo nhu cầu, ưu tiên tìm symbol và diff.
4. Review diff theo correctness/security, sửa lỗi thuộc phạm vi rồi QA. Nếu đổi kiến trúc hoặc scope, quay lại gate duyệt.
5. Lưu trạng thái factual; báo outcome, test đã chạy/chưa chạy, hạn chế. Commit/push/deploy cần quyền riêng, không mặc định suy ra từ quyền sửa code.
Không dự đoán phần trăm quota; khi context dài, checkpoint và đề xuất phiên mới. Hai lần sửa cùng lỗi không thành công: dừng vòng thử đó, ghi nguyên nhân và yêu cầu quyết định nếu cần.
