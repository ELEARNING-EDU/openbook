import type { Types } from "mongoose";
import { connection } from "next/server";
import { connectDB } from "@/lib/db";
import { isObjectId } from "@/lib/book-service";
import { Book, type BookDoc } from "@/models/Book";
import { Category } from "@/models/Category";
import { Review, type ReviewDoc } from "@/models/Review";

export const PAGE_SIZE = 20;

// Reads are per-request: opt out of prerendering before the driver touches clocks/randomness.
async function ready() {
  await connection();
  await connectDB();
}

export type CategoryDTO = { id: string; name: string; slug: string };

export type BookDTO = {
  id: string;
  title: string;
  slug: string;
  author: string;
  description: string;
  coverUrl: string;
  links: { label: string; url: string }[];
  categories: CategoryDTO[];
  featured: boolean;
  ratingAvg: number;
  ratingCount: number;
  downloadCount: number;
};

export type ReviewDTO = {
  id: string;
  rating: number;
  comment: string;
  userName: string;
  userImage?: string;
  hidden: boolean;
  createdAt: string;
  bookTitle?: string;
  bookSlug?: string;
};

type CatLean = { _id: Types.ObjectId; name: string; slug: string };
type BookLean = Omit<BookDoc, "categoryIds"> & { _id: Types.ObjectId; categoryIds: CatLean[] };

const toCategoryDTO = (c: CatLean): CategoryDTO => ({ id: String(c._id), name: c.name, slug: c.slug });

function toBookDTO(b: BookLean): BookDTO {
  return {
    id: String(b._id),
    title: b.title,
    slug: b.slug,
    author: b.author,
    description: b.description ?? "",
    coverUrl: b.coverUrl ?? "",
    links: b.links.map((l) => ({ label: l.label, url: l.url })),
    categories: (b.categoryIds ?? []).filter(Boolean).map(toCategoryDTO),
    featured: b.featured,
    ratingAvg: b.ratingAvg,
    ratingCount: b.ratingCount,
    downloadCount: b.downloadCount,
  };
}

type ReviewLean = ReviewDoc & {
  _id: Types.ObjectId;
  createdAt: Date;
  bookId: Types.ObjectId | { title: string; slug: string } | null;
};

function toReviewDTO(r: ReviewLean): ReviewDTO {
  const book = r.bookId && "title" in r.bookId ? r.bookId : null;
  return {
    id: String(r._id),
    rating: r.rating,
    comment: r.comment ?? "",
    userName: r.userName,
    userImage: r.userImage ?? undefined,
    hidden: r.hidden,
    createdAt: new Date(r.createdAt).toISOString(),
    ...(book && { bookTitle: book.title, bookSlug: book.slug }),
  };
}

async function findBooks(filter: object, sort: Record<string, 1 | -1>, skip: number, limit: number) {
  await ready();
  const docs = await Book.find(filter)
    .sort(sort)
    .skip(skip)
    .limit(limit)
    .populate("categoryIds", "name slug")
    .lean<BookLean[]>();
  return docs.map(toBookDTO);
}

const escapeRegex = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

export function getFeaturedBooks(limit = 8): Promise<BookDTO[]> {
  return findBooks({ featured: true }, { createdAt: -1 }, 0, limit);
}

export function getLatestBooks(limit = 10): Promise<BookDTO[]> {
  return findBooks({}, { createdAt: -1 }, 0, limit);
}

export async function getTopDownloaded({ categoryId, limit = 8 }: { categoryId?: string; limit?: number }) {
  return findBooks(categoryId ? { categoryIds: categoryId } : {}, { downloadCount: -1, createdAt: -1 }, 0, limit);
}

export async function listBooks({ q, category, page }: { q?: string; category?: string; page: number }) {
  await ready();
  const filter: Record<string, unknown> = {};
  const term = q?.trim();
  if (term) {
    const re = { $regex: escapeRegex(term), $options: "i" };
    filter.$or = [{ title: re }, { author: re }];
  }
  if (category) {
    const cat = await Category.findOne({ slug: category }, { _id: 1 }).lean();
    if (!cat) return { books: [] as BookDTO[], total: 0, totalPages: 0 };
    filter.categoryIds = cat._id;
  }
  const total = await Book.countDocuments(filter);
  const books = await findBooks(filter, { createdAt: -1, _id: -1 }, (page - 1) * PAGE_SIZE, PAGE_SIZE);
  return { books, total, totalPages: Math.ceil(total / PAGE_SIZE) };
}

export async function getBookBySlug(slug: string): Promise<BookDTO | null> {
  return (await findBooks({ slug }, {}, 0, 1))[0] ?? null;
}

export async function getBookById(id: string): Promise<BookDTO | null> {
  if (!isObjectId(id)) return null;
  return (await findBooks({ _id: id }, {}, 0, 1))[0] ?? null;
}

export async function listCategories(): Promise<CategoryDTO[]> {
  await ready();
  return (await Category.find().sort({ name: 1 }).lean<CatLean[]>()).map(toCategoryDTO);
}

export async function getCategoryBySlug(slug: string): Promise<CategoryDTO | null> {
  await ready();
  const c = await Category.findOne({ slug }).lean<CatLean>();
  return c ? toCategoryDTO(c) : null;
}

export async function getCategorySections(perCategory = 10) {
  const categories = await listCategories();
  const sections = await Promise.all(
    categories.map(async (category) => ({
      category,
      books: await getTopDownloaded({ categoryId: category.id, limit: perCategory }),
    })),
  );
  return sections.filter((s) => s.books.length > 0);
}

export async function getVisibleReviews(bookId: string): Promise<ReviewDTO[]> {
  if (!isObjectId(bookId)) return [];
  await ready();
  const docs = await Review.find({ bookId, hidden: false }).sort({ createdAt: -1 }).lean<ReviewLean[]>();
  return docs.map(toReviewDTO);
}

export async function listAllReviews(limit = 100): Promise<ReviewDTO[]> {
  await ready();
  const docs = await Review.find()
    .sort({ createdAt: -1 })
    .limit(limit)
    .populate("bookId", "title slug")
    .lean<ReviewLean[]>();
  return docs.map(toReviewDTO);
}

export async function getUserReview(bookId: string, userId: string): Promise<ReviewDTO | null> {
  if (!isObjectId(bookId)) return null;
  await ready();
  const r = await Review.findOne({ bookId, userId }).lean<ReviewLean>();
  return r ? toReviewDTO(r) : null;
}
