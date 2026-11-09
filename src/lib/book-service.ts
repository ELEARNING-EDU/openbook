import { Types } from "mongoose";
import { connectDB } from "@/lib/db";
import { computeRating } from "@/lib/rating";
import { dayKey, type StatsSort } from "@/lib/stats";
import { Author } from "@/models/Author";
import { Book } from "@/models/Book";
import { BookDailyStat } from "@/models/BookDailyStat";
import { Category } from "@/models/Category";
import { Publisher } from "@/models/Publisher";
import { Review } from "@/models/Review";
import { Series } from "@/models/Series";

type SlugModel = "Book" | "Category" | "Author" | "Series" | "Publisher";

const MODELS = { Book, Category, Author, Series, Publisher };

export async function uniqueSlug(base: string, model: SlugModel, excludeId?: string): Promise<string> {
  await connectDB();
  const M = MODELS[model] as typeof Category;
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
  // timestamps off: updatedAt means an admin edit, and the overview lists it as such.
  await Book.updateOne({ _id: bookId }, computeRating(reviews.map((r) => r.rating)), { timestamps: false });
}

export async function recordDailyStat(bookId: Types.ObjectId | string, field: StatsSort): Promise<void> {
  await connectDB();
  const date = dayKey(new Date());
  const update = () =>
    BookDailyStat.updateOne({ bookId, date }, { $inc: { [field]: 1 } }, { upsert: true });
  try {
    await update();
  } catch (err) {
    // A concurrent upsert can lose the unique-index race once; retry finds the row.
    if ((err as { code?: number }).code !== 11000) throw err;
    await update();
  }
}

export async function recordDownload(slug: string, index: number): Promise<string | null> {
  if (!Number.isInteger(index) || index < 0) return null;
  await connectDB();
  const book = await Book.findOneAndUpdate(
    { slug, status: { $ne: "draft" }, [`links.${index}`]: { $exists: true } },
    { $inc: { downloadCount: 1 } },
    { projection: { links: 1 }, timestamps: false },
  ).lean();
  if (!book) return null;
  try {
    await recordDailyStat(book._id, "downloads");
  } catch (err) {
    console.error("recordDownload: could not write daily stat", err);
  }
  return book.links[index]?.url ?? null;
}

// Deleting books always takes their reviews and daily figures with them.
export async function deleteBooks(ids: string[]): Promise<void> {
  await connectDB();
  await Promise.all([
    Book.deleteMany({ _id: { $in: ids } }),
    Review.deleteMany({ bookId: { $in: ids } }),
    BookDailyStat.deleteMany({ bookId: { $in: ids } }),
  ]);
}

export const isObjectId = (id: string) => Types.ObjectId.isValid(id) && /^[a-f\d]{24}$/i.test(id);
