import type { Types } from "mongoose";
import { connection } from "next/server";
import { connectDB } from "@/lib/db";
import { BOOK_ISSUE_FILTER, bookIssues, type BookIssue } from "@/lib/book-issues";
import { addDays, dayKey, fillDays, groupWeekly, type PeriodStat, type StatsRange, type StatsSort } from "@/lib/stats";
import { Book } from "@/models/Book";
import { BookDailyStat } from "@/models/BookDailyStat";
import { Category } from "@/models/Category";
import { Review } from "@/models/Review";

// Reads are per-request: opt out of prerendering before the driver touches clocks.
async function ready() {
  await connection();
  await connectDB();
}

type DailyAgg = { _id: string; reads: number; downloads: number };
type BookAgg = { _id: Types.ObjectId; reads: number; downloads: number };

export type StatsBookRow = {
  id: string;
  title: string;
  reads: number;
  downloads: number;
  categories: { id: string; name: string }[];
};

export type StatsCategoryRow = { id: string; name: string; value: number };

export type StatsData = {
  start: string;
  end: string;
  points: PeriodStat[];
  totalReads: number;
  totalDownloads: number;
  earliest: string | null;
  books: StatsBookRow[];
  categories: StatsCategoryRow[];
};

async function existingBookIds(): Promise<Types.ObjectId[]> {
  return (await Book.find({}, { _id: 1 }).lean<{ _id: Types.ObjectId }[]>()).map((b) => b._id);
}

export async function getStatsData(range: StatsRange, sort: StatsSort): Promise<StatsData> {
  await ready();
  const end = dayKey(new Date());
  const start = addDays(end, -(range - 1));
  const ids = await existingBookIds();
  const match = { bookId: { $in: ids }, date: { $gte: start, $lte: end } };
  const [dailyAgg, bookAgg, earliestDoc] = await Promise.all([
    BookDailyStat.aggregate<DailyAgg>([
      { $match: match },
      { $group: { _id: "$date", reads: { $sum: "$reads" }, downloads: { $sum: "$downloads" } } },
    ]),
    BookDailyStat.aggregate<BookAgg>([
      { $match: match },
      { $group: { _id: "$bookId", reads: { $sum: "$reads" }, downloads: { $sum: "$downloads" } } },
      { $sort: { [sort]: -1, _id: 1 } },
    ]),
    BookDailyStat.findOne({}, { date: 1 }).sort({ date: 1 }).lean<{ date: string }>(),
  ]);

  const daily = fillDays(
    end,
    range,
    dailyAgg.map((d) => ({ date: d._id, reads: d.reads, downloads: d.downloads })),
  );
  const points =
    range === 90
      ? groupWeekly(daily)
      : daily.map((d) => ({ start: d.date, end: d.date, reads: d.reads, downloads: d.downloads }));

  const books = await Book.find({ _id: { $in: bookAgg.map((b) => b._id) } }, { title: 1, categoryIds: 1 })
    .populate("categoryIds", "name")
    .lean<{ _id: Types.ObjectId; title: string; categoryIds: { _id: Types.ObjectId; name: string }[] }[]>();
  const bookMap = new Map(books.map((b) => [String(b._id), b]));

  const rows: StatsBookRow[] = [];
  for (const agg of bookAgg) {
    const book = bookMap.get(String(agg._id));
    if (!book) continue; // stat left behind by a book that no longer exists
    rows.push({
      id: String(book._id),
      title: book.title,
      reads: agg.reads,
      downloads: agg.downloads,
      categories: book.categoryIds.filter(Boolean).map((c) => ({ id: String(c._id), name: c.name })),
    });
  }

  const catMap = new Map<string, StatsCategoryRow>();
  for (const row of rows) {
    for (const c of row.categories) {
      const entry = catMap.get(c.id) ?? { id: c.id, name: c.name, value: 0 };
      entry.value += row[sort];
      catMap.set(c.id, entry);
    }
  }
  const categories = [...catMap.values()].sort((a, b) => b.value - a.value || a.name.localeCompare(b.name));

  return {
    start,
    end,
    points,
    totalReads: daily.reduce((n, d) => n + d.reads, 0),
    totalDownloads: daily.reduce((n, d) => n + d.downloads, 0),
    earliest: earliestDoc?.date ?? null,
    books: rows,
    categories,
  };
}

export type AdminTaskIssue = BookIssue;
export type AdminTask = { id: string; title: string; issue: BookIssue };

export type AdminOverview = {
  reads30: number;
  downloads30: number;
  books: number;
  authors: number;
  categories: number;
  reviews7: number;
  tasks: AdminTask[];
  recent: { id: string; title: string; updatedAt: string }[];
};

type FlaggedBook = {
  _id: Types.ObjectId;
  title: string;
  coverUrl?: string;
  epubUrl?: string;
  links?: unknown[];
  categoryIds?: unknown[];
};

export async function getAdminOverview(): Promise<AdminOverview> {
  await ready();
  const end = dayKey(new Date());
  const start = addDays(end, -29);
  const ids = await existingBookIds();
  const [totals, books, authors, categories, reviews7, flagged, recent] = await Promise.all([
    BookDailyStat.aggregate<{ _id: null; reads: number; downloads: number }>([
      { $match: { bookId: { $in: ids }, date: { $gte: start, $lte: end } } },
      { $group: { _id: null, reads: { $sum: "$reads" }, downloads: { $sum: "$downloads" } } },
    ]),
    Book.countDocuments(),
    Book.distinct("author"),
    Category.countDocuments(),
    Review.countDocuments({ createdAt: { $gte: new Date(Date.now() - 7 * 86_400_000) } }),
    Book.find(
      { $or: [...BOOK_ISSUE_FILTER] },
      { title: 1, coverUrl: 1, epubUrl: 1, links: 1, categoryIds: 1 },
    )
      .sort({ title: 1 })
      .lean<FlaggedBook[]>(),
    Book.find({}, { title: 1, updatedAt: 1 })
      .sort({ updatedAt: -1 })
      .limit(3)
      .lean<{ _id: Types.ObjectId; title: string; updatedAt: Date }[]>(),
  ]);

  const tasks: AdminTask[] = [];
  for (const b of flagged) {
    const id = String(b._id);
    for (const issue of bookIssues(b)) tasks.push({ id, title: b.title, issue });
  }

  return {
    reads30: totals[0]?.reads ?? 0,
    downloads30: totals[0]?.downloads ?? 0,
    books,
    authors: authors.length,
    categories,
    reviews7,
    tasks,
    recent: recent.map((b) => ({ id: String(b._id), title: b.title, updatedAt: new Date(b.updatedAt).toISOString() })),
  };
}
