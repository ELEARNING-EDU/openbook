"use server";

import { runAction, formError } from "@/actions/types";
import { requireAdmin, requireUser } from "@/lib/auth-guard";
import { isObjectId, recalcBookRating } from "@/lib/book-service";
import { connectDB } from "@/lib/db";
import { getT } from "@/lib/i18n/server";
import { rateLimit } from "@/lib/rate-limit";
import { makeReviewSchema, type ReviewInput } from "@/lib/validation";
import { Book } from "@/models/Book";
import { Review } from "@/models/Review";

export async function submitReview(bookId: string, input: ReviewInput) {
  return runAction(async () => {
    const t = await getT();
    const user = await requireUser();
    if (!rateLimit(`review:${user.id}`, 10, 60 * 60_000)) return formError(t("action.generic"));
    const data = makeReviewSchema(t).parse(input);
    if (!isObjectId(bookId)) return formError(t("action.bookNotFound"));
    await connectDB();
    if (!(await Book.exists({ _id: bookId, status: { $ne: "draft" } }))) return formError(t("action.bookNotFound"));
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
    const t = await getT();
    await requireAdmin();
    if (!isObjectId(id)) return formError(t("action.reviewNotFound"));
    await connectDB();
    const r = await Review.findByIdAndUpdate(id, { hidden }).lean();
    if (!r) return formError(t("action.reviewNotFound"));
    await recalcBookRating(String(r.bookId));
    return { ok: true };
  });
}

export async function deleteReview(id: string) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    if (!isObjectId(id)) return formError(t("action.reviewNotFound"));
    await connectDB();
    const r = await Review.findByIdAndDelete(id).lean();
    if (!r) return formError(t("action.reviewNotFound"));
    await recalcBookRating(String(r.bookId));
    return { ok: true };
  });
}
