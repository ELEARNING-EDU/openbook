import { Types } from "mongoose";
import { connectDB } from "@/lib/db";
import { computeRating } from "@/lib/rating";
import { Book } from "@/models/Book";
import { Category } from "@/models/Category";
import { Review } from "@/models/Review";

export async function uniqueSlug(base: string, model: "Book" | "Category", excludeId?: string): Promise<string> {
  await connectDB();
  const M = (model === "Book" ? Book : Category) as typeof Category;
  const taken = new Set(
    (await M.find({ slug: new RegExp(`^${base}(-\\d+)?$`) }, { slug: 1 }).lean())
      .filter((d) => String(d._id) !== excludeId)
      .map((d) => d.slug),
  );
  if (!taken.has(base)) return base;
  let i = 2;
  while (taken.has(`${base}-${i}`)) i++;
  return `${base}-${i}`;
}

export async function recalcBookRating(bookId: string): Promise<void> {
  await connectDB();
  const reviews = await Review.find({ bookId, hidden: false }, { rating: 1 }).lean();
  await Book.updateOne({ _id: bookId }, computeRating(reviews.map((r) => r.rating)));
}

export async function recordDownload(slug: string, index: number): Promise<string | null> {
  if (!Number.isInteger(index) || index < 0) return null;
  await connectDB();
  const book = await Book.findOneAndUpdate(
    { slug, [`links.${index}`]: { $exists: true } },
    { $inc: { downloadCount: 1 } },
    { projection: { links: 1 } },
  ).lean();
  return book?.links[index]?.url ?? null;
}

export const isObjectId = (id: string) => Types.ObjectId.isValid(id) && /^[a-f\d]{24}$/i.test(id);
