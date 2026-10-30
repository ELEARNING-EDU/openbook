"use server";

import { runAction, formError } from "@/actions/types";
import { requireAdmin, requireUser } from "@/lib/auth-guard";
import { isObjectId, recalcBookRating } from "@/lib/book-service";
import { connectDB } from "@/lib/db";
import { reviewSchema, type ReviewInput } from "@/lib/validation";
import { Book } from "@/models/Book";
import { Review } from "@/models/Review";

export async function submitReview(bookId: string, input: ReviewInput) {
  return runAction(async () => {
    const user = await requireUser();
    const data = reviewSchema.parse(input);
    if (!isObjectId(bookId)) return formError("Không tìm thấy sách");
    await connectDB();
    if (!(await Book.exists({ _id: bookId }))) return formError("Không tìm thấy sách");
    await Review.updateOne(
      { bookId, userId: user.id },
      // hidden is set only on insert so resubmitting can't undo an admin's hide.
      { $set: { ...data, userName: user.name, userImage: user.image }, $setOnInsert: { hidden: false } },
      { upsert: true },
    );
    await recalcBookRating(bookId);
    return { ok: true };
  });
}

export async function setReviewHidden(id: string, hidden: boolean) {
  return runAction(async () => {
    await requireAdmin();
    if (!isObjectId(id)) return formError("Không tìm thấy bình luận");
    await connectDB();
    const r = await Review.findByIdAndUpdate(id, { hidden }).lean();
    if (!r) return formError("Không tìm thấy bình luận");
    await recalcBookRating(String(r.bookId));
    return { ok: true };
  });
}

export async function deleteReview(id: string) {
  return runAction(async () => {
    await requireAdmin();
    if (!isObjectId(id)) return formError("Không tìm thấy bình luận");
    await connectDB();
    const r = await Review.findByIdAndDelete(id).lean();
    if (!r) return formError("Không tìm thấy bình luận");
    await recalcBookRating(String(r.bookId));
    return { ok: true };
  });
}
