---
name: adt-session-recovery
description: Recover interrupted development from files Git and concise persisted checkpoints.
---

# session-recovery

Trước sửa: đọc PROJECT_STATE.md, TASKS.md và SESSION_HANDOFF.md nếu có. Kiểm tra repo root, branch, git status --short --branch, git log -3 --oneline, git diff --stat và diff cần thiết. Nếu không có Git, nói rõ; không tự init hoặc giả định workspace sạch.
So sánh ghi chú với file/test thực tế; phân loại verified complete, in progress, pending, blocked. Không làm lại việc đã xác minh; không xóa thay đổi chưa commit. Nếu checkpoint mâu thuẫn, ghi bằng chứng và chọn bước an toàn nhỏ nhất.
Sau milestone, cập nhật ba file state: mục tiêu, quyết định/phê duyệt, file thay đổi, test/exit code thực, blocker và đúng một next action với command cụ thể. Ghi thời gian kèm timezone. Không copy token/keys hoặc log đầy đủ.
Git checkpoint tùy chọn: sau quyền commit rõ ràng, xem diff, stage đường dẫn đã chọn, xem staged diff rồi commit. Không tự push; không add -A/reset/clean. Có thể dùng git diff để xem lại, nhưng diff không chứa untracked files hay là backup đầy đủ.
Khi resume sau mất quota/restart, dựng lại bằng chứng từ đĩa. Không hứa tự lưu sau khi máy tắt hay hết quota; checkpoint chỉ chạy khi agent còn hoạt động.
