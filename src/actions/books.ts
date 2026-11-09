"use server";

import { runAction, formError } from "@/actions/types";
import { requireAdmin } from "@/lib/auth-guard";
import { findOrCreateAuthor } from "@/lib/authors";
import { deleteBooks, isObjectId, recordDailyStat, uniqueSlug } from "@/lib/book-service";
import { connectDB } from "@/lib/db";
import { checkEpub } from "@/lib/epub-proxy";
import { rateLimit, requestClientKey } from "@/lib/rate-limit";
import { getT } from "@/lib/i18n/server";
import { slugify } from "@/lib/slug";
import { findOrCreateNamedTerm } from "@/lib/taxonomy";
import {
  httpUrl,
  makeBookSchema,
  makeBulkCategorySchema,
  makeBulkStatusSchema,
  makeBookIdsSchema,
  type BookInput,
} from "@/lib/validation";
import { Book, type BookStatus, type ContributorRole } from "@/models/Book";
import { Category } from "@/models/Category";

export type CheckEpubLinkResult =
  | { ok: true; status: "ok"; bytes: number }
  | { ok: true; status: "tooLarge" }
  | { ok: true; status: "unreachable" }
  | { ok: false; errors: Record<string, string[]> };

export async function saveBook(id: string | null, input: BookInput) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    const data = makeBookSchema(t).parse(input);
    await connectDB();
    if (id !== null && !(isObjectId(id) && (await Book.exists({ _id: id })))) return formError(t("action.bookNotFound"));
    const slug = await uniqueSlug(slugify(data.title), "Book", id ?? undefined);
    // The typed author name is matched to a profile; Book.author keeps the canonical spelling.
    const author = await findOrCreateAuthor(data.author);
    const series = data.series ? await findOrCreateNamedTerm("series", data.series) : null;
    const publisher = data.publisher ? await findOrCreateNamedTerm("publisher", data.publisher) : null;

    const contributors: { authorId: string; role: ContributorRole }[] = [];
    const seen = new Set<string>();
    for (const contributor of data.contributors) {
      const person = await findOrCreateAuthor(contributor.name);
      const key = `${person.id}:${contributor.role}`;
      if (seen.has(key)) continue;
      seen.add(key);
      contributors.push({ authorId: person.id, role: contributor.role });
    }

    const fields = {
      title: data.title,
      description: data.description,
      coverUrl: data.coverUrl,
      epubUrl: data.epubUrl,
      links: data.links,
      categoryIds: data.categoryIds,
      featured: data.featured,
      status: data.status,
      language: data.language,
      isbn: data.isbn,
      publishYear: data.publishYear,
      seriesNumber: data.seriesNumber,
      author: author.name,
      authorId: author.id,
      slug,
      seriesId: series?.id ?? null,
      publisherId: publisher?.id ?? null,
      contributors,
    };
    if (id) {
      await Book.updateOne({ _id: id }, fields);
      return { ok: true, id };
    }
    const book = await Book.create(fields);
    return { ok: true, id: book.id };
  });
}

export async function deleteBook(id: string) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    if (!isObjectId(id)) return formError(t("action.bookNotFound"));
    await deleteBooks([id]);
    return { ok: true };
  });
}

export async function bulkSetBookStatus(ids: string[], status: BookStatus) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    const data = makeBulkStatusSchema(t).parse({ ids, status });
    await connectDB();
    const result = await Book.updateMany({ _id: { $in: data.ids } }, { $set: { status: data.status } });
    return result.matchedCount > 0 ? { ok: true } : formError(t("action.bookNotFound"));
  });
}

export async function bulkAddCategory(ids: string[], categoryId: string) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    const data = makeBulkCategorySchema(t).parse({ ids, categoryId });
    await connectDB();
    if (!(await Category.exists({ _id: data.categoryId }))) return formError(t("action.categoryNotFound"));
    const result = await Book.updateMany({ _id: { $in: data.ids } }, { $addToSet: { categoryIds: data.categoryId } });
    return result.matchedCount > 0 ? { ok: true } : formError(t("action.bookNotFound"));
  });
}

export async function bulkDeleteBooks(ids: string[]) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    const data = makeBookIdsSchema(t).parse(ids);
    await deleteBooks(data);
    return { ok: true };
  });
}

export async function toggleFeatured(id: string) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    if (!isObjectId(id)) return formError(t("action.bookNotFound"));
    await connectDB();
    const r = await Book.updateOne({ _id: id }, [{ $set: { featured: { $not: "$featured" } } }], { updatePipeline: true });
    return r.matchedCount ? { ok: true } : formError(t("action.bookNotFound"));
  });
}

// Uses the same SSRF guard and size cap as the reader proxy; the check never reports chapters.
export async function checkEpubLink(url: string): Promise<CheckEpubLinkResult> {
  try {
    await requireAdmin();
    if (!httpUrl.safeParse(url).success) return { ok: true, status: "unreachable" };
    return { ok: true, ...(await checkEpub(url)) };
  } catch (err) {
    const t = await getT();
    const msg = err instanceof Error ? err.message : "";
    if (msg === "UNAUTHORIZED" || msg === "FORBIDDEN") return { ok: false, errors: { _form: [t("action.forbidden")] } };
    console.error(err);
    return { ok: false, errors: { _form: [t("action.generic")] } };
  }
}

// Public: counts one read (and keeps downloadCount for public popularity sorting).
export async function recordRead(slug: string): Promise<void> {
  if (typeof slug !== "string" || !/^[a-z0-9-]{1,200}$/.test(slug)) return;
  const ip = await requestClientKey();
  if (ip && !rateLimit(`read:${ip}`, 120, 60_000)) return;
  try {
    await connectDB();
    const book = await Book.findOneAndUpdate(
      { slug, status: { $ne: "draft" } },
      { $inc: { downloadCount: 1 } },
      { projection: { _id: 1 }, timestamps: false },
    ).lean();
    if (book) await recordDailyStat(book._id, "reads");
  } catch (err) {
    console.error("recordRead: could not count read", err);
  }
}
