"use server";

import { runAction, formError } from "@/actions/types";
import { requireAdmin } from "@/lib/auth-guard";
import { isObjectId, uniqueSlug } from "@/lib/book-service";
import { connectDB } from "@/lib/db";
import { getT } from "@/lib/i18n/server";
import { slugify } from "@/lib/slug";
import { makeCategoryMenuSchema, makeCategorySchema, type CategoryInput, type CategoryMenuInput } from "@/lib/validation";
import { Book } from "@/models/Book";
import { Category } from "@/models/Category";

export async function saveCategory(id: string | null, input: CategoryInput) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    const data = makeCategorySchema(t).parse(input);
    await connectDB();
    if (id !== null && !(isObjectId(id) && (await Category.exists({ _id: id })))) return formError(t("action.categoryNotFound"));
    const slug = await uniqueSlug(slugify(data.name), "Category", id ?? undefined);
    if (id) {
      // Renaming keeps the menu placement unchanged.
      await Category.updateOne({ _id: id }, { ...data, slug });
      return { ok: true, id };
    }
    const last = await Category.findOne({ menuOrder: { $exists: true } }, { menuOrder: 1 })
      .sort({ menuOrder: -1 })
      .lean<{ menuOrder?: number }>();
    const menuOrder = (last?.menuOrder ?? -1) + 1;
    const cat = await Category.create({ ...data, slug, menuOrder });
    return { ok: true, id: cat.id };
  });
}

export async function saveCategoryMenu(items: CategoryMenuInput) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    const menu = makeCategoryMenuSchema(t).parse(items);
    await connectDB();
    if (menu.length > 0) {
      await Category.bulkWrite(
        menu.map((item, index) => ({
          updateOne: { filter: { _id: item.id }, update: { $set: { menuOrder: index, showInMenu: item.showInMenu } } },
        })),
      );
    }
    return { ok: true };
  });
}

export async function deleteCategory(id: string, moveToId?: string) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    if (!isObjectId(id)) return formError(t("action.categoryNotFound"));
    await connectDB();
    if (moveToId !== undefined) {
      if (!isObjectId(moveToId) || moveToId === id || !(await Category.exists({ _id: moveToId }))) {
        return formError(t("action.categoryMoveInvalid"));
      }
      // Add the target first so a book never loses its only category.
      await Book.updateMany({ categoryIds: id }, { $addToSet: { categoryIds: moveToId } });
    }
    await Book.updateMany({ categoryIds: id }, { $pull: { categoryIds: id } });
    await Category.deleteOne({ _id: id });
    return { ok: true };
  });
}
