---
name: adt-code-review
description: Review a scoped diff for actionable correctness security and regression findings without changing code.
---

# code-review

Xác minh base/head hoặc phạm vi working diff; xem cả staged, unstaged và file mới liên quan. Đọc caller/test cần thiết, không tải toàn repo. Không checkout/reset hay chỉnh sửa khi chỉ được yêu cầu review.
Ưu tiên crash, mất dữ liệu, auth/permission, concurrency, behavior regression. Mỗi finding có severity P0–P3, file:dòng chính xác, trigger cụ thể, tác động và gợi ý sửa tối thiểu. P0 khẩn cấp diện rộng; P1 lỗi nghiêm trọng; P2 lỗi cần sửa; P3 thấp. Không nâng mức khi chỉ phỏng đoán.
Kiểm tra test có thực sự phân biệt behavior sai/đúng. Tách finding đã chứng minh khỏi câu hỏi/giả định. Không báo style ngoài conventions hoặc vấn đề cũ ngoài diff nếu không do thay đổi gây ra. Không có finding thì nói rõ, nêu khoảng trống kiểm chứng; không khẳng định code hoàn hảo. Review tuần tự trong agent hiện tại.
