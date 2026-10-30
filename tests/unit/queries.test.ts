import { describe, expect, it, vi } from "vitest";
vi.mock("next/server", () => ({ connection: async () => {} }));
import { link, setupTestDB } from "../helpers/mongo";
import { Book } from "@/models/Book";
import { Category } from "@/models/Category";
import { Review } from "@/models/Review";
import {
  getBookBySlug,
  getCategorySections,
  getTopDownloaded,
  getVisibleReviews,
  listBooks,
} from "@/lib/queries";

setupTestDB();

let n = 0;
const mk = (over: Record<string, unknown> = {}) =>
  Book.create({ title: `Sách ${++n}`, slug: `sach-${n}`, author: "X", links: link(), ...over });

describe("listBooks", () => {
  it("tìm với ký tự regex đặc biệt theo nghĩa đen", async () => {
    await mk({ title: "Lập trình C++" });
    await mk({ title: "Lập trình C" });
    const r = await listBooks({ q: "C++", page: 1 });
    expect(r.books.map((b) => b.title)).toEqual(["Lập trình C++"]);
    await expect(listBooks({ q: "(", page: 1 })).resolves.toMatchObject({ books: [] });
  });
  it("tìm không phân biệt hoa thường theo tên hoặc tác giả", async () => {
    await mk({ title: "Truyện Kiều", author: "Nguyễn Du" });
    expect((await listBooks({ q: "kiều", page: 1 })).total).toBe(1);
    expect((await listBooks({ q: "nguyễn du", page: 1 })).total).toBe(1);
  });
  it("phân trang 20 sách/trang", async () => {
    for (let i = 0; i < 25; i++) await mk();
    const p1 = await listBooks({ page: 1 });
    expect(p1.books).toHaveLength(20);
    expect(p1.totalPages).toBe(2);
    expect((await listBooks({ page: 2 })).books).toHaveLength(5);
    expect((await listBooks({ page: 3 })).books).toEqual([]);
  });
  it("lọc theo slug thể loại", async () => {
    const c = await Category.create({ name: "Văn học", slug: "van-hoc" });
    await mk({ title: "A", categoryIds: [c._id] });
    await mk({ title: "B" });
    const r = await listBooks({ category: "van-hoc", page: 1 });
    expect(r.books.map((b) => b.title)).toEqual(["A"]);
    expect(r.books[0].categories).toEqual([{ id: c.id, name: "Văn học", slug: "van-hoc" }]);
    expect((await listBooks({ category: "khong-co", page: 1 })).books).toEqual([]);
  });
});

describe("getTopDownloaded", () => {
  it("sắp theo lượt tải, lọc theo thể loại", async () => {
    const a = await Category.create({ name: "A", slug: "a" });
    const b = await Category.create({ name: "B", slug: "b" });
    await mk({ downloadCount: 5, categoryIds: [a._id] });
    await mk({ downloadCount: 50, categoryIds: [a._id] });
    await mk({ downloadCount: 20, categoryIds: [a._id] });
    await mk({ downloadCount: 100, categoryIds: [b._id] });
    expect((await getTopDownloaded({ categoryId: a.id })).map((x) => x.downloadCount)).toEqual([50, 20, 5]);
    expect((await getTopDownloaded({}))[0].downloadCount).toBe(100);
    expect(await getTopDownloaded({ limit: 2 })).toHaveLength(2);
  });
});

describe("getCategorySections", () => {
  it("bỏ thể loại rỗng, giới hạn số sách mỗi nhóm", async () => {
    const a = await Category.create({ name: "A", slug: "a" });
    await Category.create({ name: "Rỗng", slug: "rong" });
    for (let i = 0; i < 4; i++) await mk({ categoryIds: [a._id], downloadCount: i });
    const s = await getCategorySections(3);
    expect(s.map((x) => x.category.slug)).toEqual(["a"]);
    expect(s[0].books.map((x) => x.downloadCount)).toEqual([3, 2, 1]);
  });
});

describe("getBookBySlug / getVisibleReviews", () => {
  it("trả null khi không có", async () => {
    expect(await getBookBySlug("khong-co")).toBeNull();
  });
  it("chỉ trả review không ẩn, mới nhất trước", async () => {
    const b = await mk();
    await Review.create({ bookId: b._id, userId: "u1", userName: "Cũ", rating: 5, createdAt: new Date(2020, 1, 1) });
    await Review.create({ bookId: b._id, userId: "u2", userName: "Mới", rating: 4 });
    await Review.create({ bookId: b._id, userId: "u3", userName: "Ẩn", rating: 1, hidden: true });
    const r = await getVisibleReviews(b.id);
    expect(r.map((x) => x.userName)).toEqual(["Mới", "Cũ"]);
    expect(typeof r[0].createdAt).toBe("string");
    expect((await getBookBySlug(b.slug))?.id).toBe(b.id);
  });
});
