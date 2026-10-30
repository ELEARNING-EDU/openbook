"use server";

import { runAction, formError } from "@/actions/types";
import { requireAdmin } from "@/lib/auth-guard";
import { isObjectId, uniqueSlug } from "@/lib/book-service";
import { connectDB } from "@/lib/db";
import { slugify } from "@/lib/slug";
import { bookSchema, type BookInput } from "@/lib/validation";
import { Book } from "@/models/Book";
import { Review } from "@/models/Review";

export async function saveBook(id: string | null, input: BookInput) {
  return runAction(async () => {
    await requireAdmin();
    const data = bookSchema.parse(input);
    await connectDB();
    if (id !== null && !(isObjectId(id) && (await Book.exists({ _id: id })))) return formError("Không tìm thấy sách");
    const slug = await uniqueSlug(slugify(data.title), "Book", id ?? undefined);
    if (id) {
      await Book.updateOne({ _id: id }, { ...data, slug });
      return { ok: true, id };
    }
    const book = await Book.create({ ...data, slug });
    return { ok: true, id: book.id };
  });
}

export async function deleteBook(id: string) {
  return runAction(async () => {
    await requireAdmin();
    if (!isObjectId(id)) return formError("Không tìm thấy sách");
    await connectDB();
    await Book.deleteOne({ _id: id });
    await Review.deleteMany({ bookId: id });
    return { ok: true };
  });
}

export async function toggleFeatured(id: string) {
  return runAction(async () => {
    await requireAdmin();
    if (!isObjectId(id)) return formError("Không tìm thấy sách");
    await connectDB();
    const r = await Book.updateOne({ _id: id }, [{ $set: { featured: { $not: "$featured" } } }], { updatePipeline: true });
    return r.matchedCount ? { ok: true } : formError("Không tìm thấy sách");
  });
}
