---
name: adt-qa
description: Validate changes against acceptance criteria with focused tests and an honest verification report.
---

# qa

Map mỗi acceptance criterion sang test/manual evidence. Chọn kiểm tra ít tốn nhất có thể phát hiện regression: unit → integration → smoke/UI theo rủi ro. Không tải dependencies hay thực hiện network/data mutation ngoài quyền hiện có.
Xác định toolchain, environment và commands từ repo; không bịa npm test hay scheme. Chạy test liên quan trước; giữ exit code thật, không biến failure thành success bằng || true. Phân biệt PASS, FAIL, SKIP, NOT RUN. SKIP không phải PASS.
Với UI: kiểm tra states, navigation, bàn phím, accessibility, localization và kích thước phù hợp. Với backend: input boundaries, auth, failure/retry/idempotency khi liên quan.
Nếu lỗi baseline, chứng minh bằng bằng chứng sẵn có hoặc isolated baseline được phép; không reset workspace. Tối đa hai vòng sửa cùng lỗi rồi ghi blocker. Báo command, environment, kết quả, criterion coverage và residual risk. Lưu tóm tắt trong PROJECT_STATE.md.
