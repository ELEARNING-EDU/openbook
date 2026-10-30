"use server";

import { runAction, formError } from "@/actions/types";
import { requireAdmin } from "@/lib/auth-guard";
import { isObjectId, uniqueSlug } from "@/lib/book-service";
import { connectDB } from "@/lib/db";
import { slugify } from "@/lib/slug";
import { categorySchema, type CategoryInput } from "@/lib/validation";
import { Book } from "@/models/Book";
import { Category } from "@/models/Category";

export async function saveCategory(id: string | null, input: CategoryInput) {
  return runAction(async () => {
    await requireAdmin();
    const data = categorySchema.parse(input);
    await connectDB();
    if (id !== null && !(isObjectId(id) && (await Category.exists({ _id: id })))) return formError("Không tìm thấy thể loại");
    const slug = await uniqueSlug(slugify(data.name), "Category", id ?? undefined);
    if (id) {
      await Category.updateOne({ _id: id }, { ...data, slug });
      return { ok: true, id };
    }
    const cat = await Category.create({ ...data, slug });
    return { ok: true, id: cat.id };
  });
}

export async function deleteCategory(id: string) {
  return runAction(async () => {
    await requireAdmin();
    if (!isObjectId(id)) return formError("Không tìm thấy thể loại");
    await connectDB();
    await Category.deleteOne({ _id: id });
    await Book.updateMany({ categoryIds: id }, { $pull: { categoryIds: id } });
    return { ok: true };
  });
}
