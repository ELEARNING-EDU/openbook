import { Types } from "mongoose";
import { connection } from "next/server";
import { connectDB } from "@/lib/db";
import { BOOK_ISSUE_FILTER, bookIssues, type BookIssue } from "@/lib/book-issues";
import { suggestDuplicateAuthors } from "@/lib/authors";
import { isObjectId } from "@/lib/book-service";
import { PAGE_SIZE } from "@/lib/validation";
import { Author, type AuthorDoc } from "@/models/Author";
import { Book, type BookDoc, type BookStatus, type ContributorRole } from "@/models/Book";
import { BookDailyStat } from "@/models/BookDailyStat";
import { Category } from "@/models/Category";
import { Publisher } from "@/models/Publisher";
import { Review, type ReviewDoc } from "@/models/Review";
import { Series } from "@/models/Series";

export { PAGE_SIZE };

// Reads are per-request: opt out of prerendering before the driver touches clocks/randomness.
async function ready() {
  await connection();
  await connectDB();
}

export type CategoryDTO = { id: string; name: string; slug: string };
export type NamedTermDTO = { id: string; name: string };

export type AdminCategoryDTO = CategoryDTO & { showInMenu: boolean; menuOrder: number; bookCount: number };
export type AdminTermDTO = CategoryDTO & { bookCount: number };

export type BookDTO = {
  id: string;
  title: string;
  slug: string;
  author: string;
  description: string;
  coverUrl: string;
  epubUrl: string;
  links: { label: string; url: string }[];
  categories: CategoryDTO[];
  featured: boolean;
  ratingAvg: number;
  ratingCount: number;
  downloadCount: number;
};

export type AdminBookDTO = BookDTO & {
  status: BookStatus;
  language: string;
  isbn: string;
  publishYear: number | null;
  series: NamedTermDTO | null;
  seriesNumber: number | null;
  publisher: NamedTermDTO | null;
  contributors: { id: string; name: string; role: ContributorRole }[];
};

export type AdminBookRowDTO = BookDTO & {
  status: BookStatus;
  issues: BookIssue[];
  reads: number;
  downloads: number;
};

export type BookListStatus = "all" | "published" | "draft" | "issue";

export type AdminBookList = {
  books: AdminBookRowDTO[];
  total: number;
  totalPages: number;
  counts: Record<BookListStatus, number>;
};

export type AdminAuthorDTO = {
  id: string;
  name: string;
  slug: string;
  aliases: string[];
  lifespan: string;
  bio: string;
  bookCount: number;
};

export type AuthorBookRole = "author" | ContributorRole;
export type AdminAuthorDetail = AdminAuthorDTO & { books: { id: string; title: string; role: AuthorBookRole }[] };

export type AuthorOption = { id: string; name: string };

export type AuthorSuggestion = { a: AuthorOption; b: AuthorOption };

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

export type AdminReviewFilter = "all" | "low" | "hidden";

type CatLean = { _id: Types.ObjectId; name: string; slug: string };
type BookLean = Omit<BookDoc, "categoryIds"> & { _id: Types.ObjectId; categoryIds: CatLean[] };
type NamedLean = { _id: Types.ObjectId; name: string };
type ContributorLean = { authorId: NamedLean | null; role: ContributorRole };
type AdminBookLean = Omit<BookLean, "seriesId" | "publisherId" | "contributors"> & {
  seriesId: NamedLean | null;
  publisherId: NamedLean | null;
  contributors: ContributorLean[];
};

const toCategoryDTO = (c: CatLean): CategoryDTO => ({ id: String(c._id), name: c.name, slug: c.slug });

function toBookDTO(b: BookLean): BookDTO {
  return {
    id: String(b._id),
    title: b.title,
    slug: b.slug,
    author: b.author,
    description: b.description ?? "",
    coverUrl: b.coverUrl ?? "",
    epubUrl: b.epubUrl ?? "",
    links: b.links.map((l) => ({ label: l.label, url: l.url })),
    categories: (b.categoryIds ?? []).filter(Boolean).map(toCategoryDTO),
    featured: b.featured,
    ratingAvg: b.ratingAvg,
    ratingCount: b.ratingCount,
    downloadCount: b.downloadCount,
  };
}

const statusOf = (status: string | undefined | null): BookStatus => (status === "draft" ? "draft" : "published");

function toAdminBookDTO(b: AdminBookLean): AdminBookDTO {
  return {
    ...toBookDTO(b as unknown as BookLean),
    status: statusOf(b.status),
    language: b.language ?? "",
    isbn: b.isbn ?? "",
    publishYear: b.publishYear ?? null,
    series: b.seriesId ? { id: String(b.seriesId._id), name: b.seriesId.name } : null,
    seriesNumber: b.seriesNumber ?? null,
    publisher: b.publisherId ? { id: String(b.publisherId._id), name: b.publisherId.name } : null,
    contributors: (b.contributors ?? [])
      .filter((c): c is ContributorLean & { authorId: NamedLean } => Boolean(c.authorId))
      .map((c) => ({ id: String(c.authorId._id), name: c.authorId.name, role: c.role })),
  };
}

// Public reads always exclude drafts, including legacy books with no status field.
async function findBooks(filter: object, sort: Record<string, 1 | -1>, skip: number, limit: number) {
  await ready();
  const docs = await Book.find({ ...filter, status: { $ne: "draft" } })
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
  const term = q?.trim().slice(0, 100);
  if (term) {
    const re = { $regex: escapeRegex(term), $options: "i" };
    filter.$or = [{ title: re }, { author: re }];
  }
  if (category) {
    const cat = await Category.findOne({ slug: category }, { _id: 1 }).lean();
    if (!cat) return { books: [] as BookDTO[], total: 0, totalPages: 0 };
    filter.categoryIds = cat._id;
  }
  const total = await Book.countDocuments({ ...filter, status: { $ne: "draft" } });
  const books = await findBooks(filter, { createdAt: -1, _id: -1 }, (page - 1) * PAGE_SIZE, PAGE_SIZE);
  return { books, total, totalPages: Math.ceil(total / PAGE_SIZE) };
}

export async function listAdminBooks({
  q,
  category,
  language,
  status,
  page,
}: {
  q?: string;
  category?: string;
  language?: string;
  status: BookListStatus;
  page: number;
}): Promise<AdminBookList> {
  await ready();
  const conditions: object[] = [];
  const term = q?.trim().slice(0, 100);
  if (term) {
    const re = { $regex: escapeRegex(term), $options: "i" };
    // ISBN is stored with hyphens, so digits typed without them still match.
    const isbnRe = { $regex: term.replace(/-/g, "").split("").map(escapeRegex).join("-?"), $options: "i" };
    conditions.push({ $or: [{ title: re }, { author: re }, { isbn: isbnRe }] });
  }
  if (category && isObjectId(category) && (await Category.exists({ _id: category }))) {
    conditions.push({ categoryIds: category });
  }
  if (language === "vi" || language === "en") conditions.push({ language });

  const where = (...extra: object[]) => {
    const parts = [...conditions, ...extra];
    return parts.length > 0 ? { $and: parts } : {};
  };
  const published = { status: { $ne: "draft" } };
  const draft = { status: "draft" };
  const issue = { $or: [...BOOK_ISSUE_FILTER] };
  const statusFilter =
    status === "draft" ? [draft] : status === "published" ? [published] : status === "issue" ? [issue] : [];

  const [all, publishedCount, draftCount, issueCount] = await Promise.all([
    Book.countDocuments(where()),
    Book.countDocuments(where(published)),
    Book.countDocuments(where(draft)),
    Book.countDocuments(where(issue)),
  ]);
  const counts: Record<BookListStatus, number> = {
    all,
    published: publishedCount,
    draft: draftCount,
    issue: issueCount,
  };

  const docs = await Book.find(where(...statusFilter))
    .sort({ createdAt: -1, _id: -1 })
    .skip((page - 1) * PAGE_SIZE)
    .limit(PAGE_SIZE)
    .populate("categoryIds", "name slug")
    .lean<BookLean[]>();
  const totals = await bookTotals(docs.map((b) => String(b._id)));
  const books = docs.map((b) => {
    const stats = totals.get(String(b._id)) ?? { reads: 0, downloads: 0 };
    return {
      ...toBookDTO(b),
      status: statusOf(b.status),
      issues: bookIssues(b),
      ...stats,
    };
  });
  return { books, total: counts[status], totalPages: Math.ceil(counts[status] / PAGE_SIZE), counts };
}

export async function getBookTotals(ids: string[]): Promise<Map<string, { reads: number; downloads: number }>> {
  await ready();
  return bookTotals(ids);
}

async function bookTotals(ids: string[]): Promise<Map<string, { reads: number; downloads: number }>> {
  const objectIds = ids.filter(isObjectId).map((id) => new Types.ObjectId(id));
  if (objectIds.length === 0) return new Map();
  const rows = await BookDailyStat.aggregate<{ _id: Types.ObjectId; reads: number; downloads: number }>([
    { $match: { bookId: { $in: objectIds } } },
    { $group: { _id: "$bookId", reads: { $sum: "$reads" }, downloads: { $sum: "$downloads" } } },
  ]);
  return new Map(rows.map((r) => [String(r._id), { reads: r.reads, downloads: r.downloads }]));
}

export async function getBookAllTimeStats(bookId: string) {
  const totals = await getBookTotals([bookId]);
  return totals.get(bookId) ?? { reads: 0, downloads: 0 };
}

export async function getBookBySlug(slug: string): Promise<BookDTO | null> {
  return (await findBooks({ slug }, {}, 0, 1))[0] ?? null;
}

// Admin-only: drafts stay editable.
export async function getBookById(id: string): Promise<AdminBookDTO | null> {
  if (!isObjectId(id)) return null;
  await ready();
  const book = await Book.findById(id)
    .populate("categoryIds", "name slug")
    .populate("seriesId", "name")
    .populate("publisherId", "name")
    .populate("contributors.authorId", "name")
    .lean<AdminBookLean>();
  return book ? toAdminBookDTO(book) : null;
}

export async function getRelatedBooks(book: BookDTO, limit = 6): Promise<BookDTO[]> {
  if (book.categories.length === 0) return [];
  return findBooks(
    { _id: { $ne: book.id }, categoryIds: { $in: book.categories.map((c) => c.id) } },
    { downloadCount: -1, createdAt: -1 },
    0,
    limit,
  );
}

export async function listCategories(): Promise<CategoryDTO[]> {
  await ready();
  return (await Category.find().sort({ name: 1 }).lean<CatLean[]>()).map(toCategoryDTO);
}

type MenuCatLean = CatLean & { showInMenu?: boolean | null; menuOrder?: number | null };

// Old documents have no menu fields: treat them as shown, ordered by name.
const MENU_SORT = { menuOrder: 1, name: 1 } as const;

export async function listMenuCategories(): Promise<CategoryDTO[]> {
  await ready();
  return (await Category.find({ showInMenu: { $ne: false } }).sort(MENU_SORT).lean<MenuCatLean[]>()).map(toCategoryDTO);
}

export async function listAdminCategories(): Promise<AdminCategoryDTO[]> {
  await ready();
  const [cats, counts] = await Promise.all([
    Category.find().sort(MENU_SORT).lean<MenuCatLean[]>(),
    Book.aggregate<{ _id: Types.ObjectId; count: number }>([
      { $unwind: "$categoryIds" },
      { $group: { _id: "$categoryIds", count: { $sum: 1 } } },
    ]),
  ]);
  const countById = new Map(counts.map((c) => [String(c._id), c.count]));
  return cats.map((c) => ({
    ...toCategoryDTO(c),
    showInMenu: c.showInMenu !== false,
    menuOrder: c.menuOrder ?? 0,
    bookCount: countById.get(String(c._id)) ?? 0,
  }));
}

async function listAdminTerms(model: typeof Series | typeof Publisher, field: "seriesId" | "publisherId") {
  await ready();
  const [terms, counts] = await Promise.all([
    model.find().sort({ name: 1 }).lean<CatLean[]>(),
    Book.aggregate<{ _id: Types.ObjectId; count: number }>([
      { $match: { [field]: { $ne: null } } },
      { $group: { _id: `$${field}`, count: { $sum: 1 } } },
    ]),
  ]);
  const countById = new Map(counts.map((c) => [String(c._id), c.count]));
  return terms.map((term): AdminTermDTO => ({ ...toCategoryDTO(term), bookCount: countById.get(String(term._id)) ?? 0 }));
}

export function listAdminSeries() {
  return listAdminTerms(Series, "seriesId");
}

export function listAdminPublishers() {
  return listAdminTerms(Publisher, "publisherId");
}

export async function listSeriesOptions(): Promise<NamedTermDTO[]> {
  await ready();
  return (await Series.find({}, { name: 1 }).sort({ name: 1 }).lean<NamedLean[]>()).map((s) => ({
    id: String(s._id),
    name: s.name,
  }));
}

export async function listPublisherOptions(): Promise<NamedTermDTO[]> {
  await ready();
  return (await Publisher.find({}, { name: 1 }).sort({ name: 1 }).lean<NamedLean[]>()).map((p) => ({
    id: String(p._id),
    name: p.name,
  }));
}

type AuthorLean = AuthorDoc & { _id: Types.ObjectId };

const toAuthorDTO = (author: AuthorLean, bookCount: number): AdminAuthorDTO => ({
  id: String(author._id),
  name: author.name,
  slug: author.slug,
  aliases: author.aliases ?? [],
  lifespan: author.lifespan ?? "",
  bio: author.bio ?? "",
  bookCount,
});

// Counts a book once per person, whether they are the main author or a contributor.
async function authorBookCounts(): Promise<Map<string, number>> {
  const counts = await Book.aggregate<{ _id: Types.ObjectId; count: number }>([
    {
      $project: {
        people: {
          $setUnion: [
            { $cond: [{ $ne: ["$authorId", null] }, ["$authorId"], []] },
            { $ifNull: ["$contributors.authorId", []] },
          ],
        },
      },
    },
    { $unwind: "$people" },
    { $group: { _id: "$people", count: { $sum: 1 } } },
  ]);
  return new Map(counts.map((c) => [String(c._id), c.count]));
}

export async function listAuthors({ q, page }: { q?: string; page: number }) {
  await ready();
  const term = q?.trim().slice(0, 100);
  const filter: Record<string, unknown> = {};
  if (term) {
    const re = { $regex: escapeRegex(term), $options: "i" };
    filter.$or = [{ name: re }, { aliases: re }];
  }
  const [docs, total, counts] = await Promise.all([
    Author.find(filter)
      .sort({ name: 1 })
      .skip((page - 1) * PAGE_SIZE)
      .limit(PAGE_SIZE)
      .lean<AuthorLean[]>(),
    Author.countDocuments(filter),
    authorBookCounts(),
  ]);
  return {
    authors: docs.map((a) => toAuthorDTO(a, counts.get(String(a._id)) ?? 0)),
    total,
    totalPages: Math.ceil(total / PAGE_SIZE),
  };
}

type AuthorBookLean = {
  _id: Types.ObjectId;
  title: string;
  authorId: Types.ObjectId | null;
  contributors?: { authorId: Types.ObjectId; role: ContributorRole }[];
};

export async function getAuthorById(id: string): Promise<AdminAuthorDetail | null> {
  if (!isObjectId(id)) return null;
  await ready();
  const author = await Author.findById(id).lean<AuthorLean>();
  if (!author) return null;
  const books = await Book.find(
    { $or: [{ authorId: id }, { "contributors.authorId": id }] },
    { title: 1, authorId: 1, contributors: 1 },
  )
    .sort({ title: 1 })
    .lean<AuthorBookLean[]>();
  return {
    ...toAuthorDTO(author, books.length),
    books: books.map((b) => {
      const role: AuthorBookRole =
        String(b.authorId) === id ? "author" : (b.contributors?.find((c) => String(c.authorId) === id)?.role ?? "editor");
      return { id: String(b._id), title: b.title, role };
    }),
  };
}

export async function listAuthorOptions(): Promise<AuthorOption[]> {
  await ready();
  return (await Author.find({}, { name: 1 }).sort({ name: 1 }).lean<{ _id: Types.ObjectId; name: string }[]>()).map(
    (a) => ({ id: String(a._id), name: a.name }),
  );
}

export async function listAuthorSuggestions(): Promise<AuthorSuggestion[]> {
  await ready();
  const docs = await Author.find(
    {},
    { name: 1, aliases: 1, notDuplicateOf: 1 },
  ).lean<Pick<AuthorLean, "_id" | "name" | "aliases" | "notDuplicateOf">[]>();
  return suggestDuplicateAuthors(
    docs.map((a) => ({
      id: String(a._id),
      name: a.name,
      aliases: a.aliases ?? [],
      notDuplicateOf: (a.notDuplicateOf ?? []).map(String),
    })),
  ).map(({ a, b }) => ({ a: { id: a.id, name: a.name }, b: { id: b.id, name: b.name } }));
}

export async function countBooksWithoutAuthor(): Promise<number> {
  await ready();
  return Book.countDocuments({ authorId: null });
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
  if (!(await Book.exists({ _id: bookId, status: { $ne: "draft" } }))) return [];
  const docs = await Review.find({ bookId, hidden: false }).sort({ createdAt: -1 }).lean<ReviewLean[]>();
  return docs.map(toReviewDTO);
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

export async function listAllReviews(limit = 100): Promise<ReviewDTO[]> {
  return (await listAdminReviews({ filter: "all", limit })).reviews;
}

export async function listAdminReviews({
  filter,
  limit = 100,
}: {
  filter: AdminReviewFilter;
  limit?: number;
}) {
  await ready();
  const where = filter === "low" ? { rating: { $lte: 2 } } : filter === "hidden" ? { hidden: true } : {};
  const [docs, all, low, hidden] = await Promise.all([
    Review.find(where).sort({ createdAt: -1 }).limit(limit).populate("bookId", "title slug").lean<ReviewLean[]>(),
    Review.countDocuments(),
    Review.countDocuments({ rating: { $lte: 2 } }),
    Review.countDocuments({ hidden: true }),
  ]);
  return { reviews: docs.map(toReviewDTO), counts: { all, low, hidden } };
}

export async function getUserReview(bookId: string, userId: string): Promise<ReviewDTO | null> {
  if (!isObjectId(bookId)) return null;
  await ready();
  if (!(await Book.exists({ _id: bookId, status: { $ne: "draft" } }))) return null;
  const r = await Review.findOne({ bookId, userId }).lean<ReviewLean>();
  return r ? toReviewDTO(r) : null;
}
