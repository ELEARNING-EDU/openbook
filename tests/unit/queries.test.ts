import { describe, expect, it, vi } from "vitest";
vi.mock("next/server", () => ({ connection: async () => {} }));
import { link, setupTestDB } from "../helpers/mongo";
import { Author } from "@/models/Author";
import { Book } from "@/models/Book";
import { Category } from "@/models/Category";
import { Review } from "@/models/Review";
import {
  countBooksWithoutAuthor,
  getAuthorById,
  getBookById,
  getBookBySlug,
  getCategorySections,
  getFeaturedBooks,
  getLatestBooks,
  getRelatedBooks,
  getTopDownloaded,
  getVisibleReviews,
  listAdminBooks,
  listAdminCategories,
  listAdminReviews,
  listAuthorSuggestions,
  listAuthors,
  listBooks,
  listCategories,
  listMenuCategories,
} from "@/lib/queries";
import { getEpubUrl } from "@/lib/epub-proxy";

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

describe("admin authors", () => {
  const mkAuthor = (over: Record<string, unknown>) =>
    Author.create({ aliases: [], lifespan: "", bio: "", notDuplicateOf: [], ...over });

  it("tìm theo tên khác, đếm sách, chi tiết và sách chưa gắn", async () => {
    const a = await mkAuthor({ name: "Nguyễn Du", nameKey: "nguyễn du", slug: "nguyen-du", aliases: ["Tố Như"] });
    await mk({ title: "Truyện Kiều", author: "Nguyễn Du", authorId: a._id });
    await mk({ title: "Chưa gắn", author: "Ai Đó" });

    const list = await listAuthors({ q: "tố như", page: 1 });
    expect(list.total).toBe(1);
    expect(list.authors[0]).toMatchObject({ name: "Nguyễn Du", bookCount: 1 });
    expect(await countBooksWithoutAuthor()).toBe(1);

    const detail = await getAuthorById(a.id);
    expect(detail?.books.map((b) => b.title)).toEqual(["Truyện Kiều"]);
  });

  it("gợi ý tác giả trùng và bỏ qua cặp đã đánh dấu", async () => {
    const a = await mkAuthor({ name: "Nguyễn Du", nameKey: "nguyễn du", slug: "nguyen-du" });
    const b = await mkAuthor({ name: "Du Nguyen", nameKey: "du nguyen", slug: "du-nguyen" });
    const suggestions = await listAuthorSuggestions();
    expect(suggestions).toHaveLength(1);
    expect([suggestions[0].a.id, suggestions[0].b.id].sort()).toEqual([a.id, b.id].sort());
    await Author.updateOne({ _id: a._id }, { $set: { notDuplicateOf: [b._id] } });
    expect(await listAuthorSuggestions()).toEqual([]);
  });
});

describe("getTopDownloaded", () => {  it("sắp theo lượt tải, lọc theo thể loại", async () => {
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

describe("getRelatedBooks", () => {
  it("cùng thể loại, bỏ chính nó, xếp theo lượt tải", async () => {
    const a = await Category.create({ name: "A", slug: "a" });
    const b = await Category.create({ name: "B", slug: "b" });
    const base = await mk({ title: "Gốc", categoryIds: [a._id], downloadCount: 1000 });
    await mk({ title: "Cùng A ít tải", categoryIds: [a._id], downloadCount: 1 });
    await mk({ title: "Cùng A nhiều tải", categoryIds: [a._id], downloadCount: 9 });
    const unrelated = await mk({ title: "Khác B", categoryIds: [b._id], downloadCount: 50 });
    const related = await getRelatedBooks((await getBookBySlug(base.slug))!);
    expect(related.map((x) => x.title)).toEqual(["Cùng A nhiều tải", "Cùng A ít tải"]);
    expect(related.map((x) => x.id)).not.toContain(unrelated.id);
    expect(await getRelatedBooks((await getBookBySlug((await mk({ title: "Không thể loại" })).slug))!)).toEqual([]);
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

describe("menu thể loại", () => {
  it("tài liệu cũ không có trường menu vẫn hiện, xếp theo tên", async () => {
    await Category.collection.insertMany([
      { name: "Zeta", slug: "zeta" },
      { name: "Alpha", slug: "alpha" },
    ]);
    expect((await listMenuCategories()).map((c) => c.name)).toEqual(["Alpha", "Zeta"]);
  });
  it("thể loại ẩn không có trong menu nhưng vẫn có trong danh sách đầy đủ", async () => {
    const hidden = await Category.create({ name: "Ẩn", slug: "an", showInMenu: false, menuOrder: 0 });
    const shown = await Category.create({ name: "Hiện", slug: "hien", showInMenu: true, menuOrder: 1 });
    expect((await listMenuCategories()).map((c) => c.id)).toEqual([shown.id]);
    expect((await listCategories()).map((c) => c.id).sort()).toEqual([hidden.id, shown.id].sort());
  });
  it("query admin trả thứ tự menu và số sách", async () => {
    const second = await Category.create({ name: "B", slug: "b", menuOrder: 1 });
    const first = await Category.create({ name: "A", slug: "a", menuOrder: 0 });
    await mk({ categoryIds: [first._id] });
    await mk({ categoryIds: [first._id, second._id] });
    const rows = await listAdminCategories();
    expect(rows.map((r) => [r.name, r.menuOrder, r.bookCount])).toEqual([
      ["A", 0, 2],
      ["B", 1, 1],
    ]);
  });
});

describe("bản nháp", () => {
  it("sách cũ không có status vẫn công khai", async () => {
    await Book.collection.insertOne({ title: "Cũ", slug: "cu", author: "X", links: link(), categoryIds: [] });
    const book = await getBookBySlug("cu");
    expect(book?.title).toBe("Cũ");
    expect((await getLatestBooks()).map((b) => b.slug)).toContain("cu");
  });

  it("bản nháp vắng khỏi mọi truy vấn công khai và bị 404", async () => {
    const cat = await Category.create({ name: "Văn học", slug: "van-hoc" });
    const draft = await mk({
      title: "Bản nháp",
      status: "draft",
      featured: true,
      downloadCount: 999,
      categoryIds: [cat._id],
      epubUrl: "https://a.org/draft.epub",
    });
    const publicBook = await mk({ title: "Công khai", categoryIds: [cat._id] });
    await Review.create({ bookId: draft._id, userId: "u", userName: "U", rating: 5, comment: "Hay" });

    expect(await getBookBySlug(draft.slug)).toBeNull();
    expect((await getFeaturedBooks()).map((b) => b.id)).not.toContain(draft.id);
    expect((await getLatestBooks()).map((b) => b.id)).not.toContain(draft.id);
    expect((await getTopDownloaded({})).map((b) => b.id)).not.toContain(draft.id);
    expect((await getTopDownloaded({ categoryId: cat.id })).map((b) => b.id)).not.toContain(draft.id);
    expect((await listBooks({ q: "Bản nháp", page: 1 })).books).toEqual([]);
    expect((await listBooks({ category: "van-hoc", page: 1 })).books.map((b) => b.id)).not.toContain(draft.id);
    expect((await getCategorySections()).flatMap((s) => s.books).map((b) => b.id)).not.toContain(draft.id);
    expect((await getVisibleReviews(draft.id))).toEqual([]);
    expect(await getEpubUrl(draft.slug)).toBeNull();
    const related = await getRelatedBooks((await getBookBySlug(publicBook.slug))!);
    expect(related.map((b) => b.id)).not.toContain(draft.id);
  });

  it("truy vấn admin vẫn thấy bản nháp và đếm đúng tab", async () => {
    const draft = await mk({ title: "Nháp", status: "draft", featured: true, downloadCount: 5 });
    const published = await mk({ title: "Xuất bản" });
    await Review.create({ bookId: draft._id, userId: "u", userName: "U", rating: 1, comment: "Dở" });

    const all = await listAdminBooks({ status: "all", page: 1 });
    expect(all.books.map((b) => b.id)).toContain(draft.id);
    const drafts = await listAdminBooks({ status: "draft", page: 1 });
    expect(drafts.books.map((b) => b.id)).toEqual([draft.id]);
    expect(drafts.counts).toMatchObject({ all: 2, draft: 1, published: 1 });
    const publishedTab = await listAdminBooks({ status: "published", page: 1 });
    expect(publishedTab.books.map((b) => b.id)).toEqual([published.id]);
    const detail = await getBookById(draft.id);
    expect(detail).toMatchObject({ id: draft.id, status: "draft" });
    expect((await listAdminReviews({ filter: "all" })).reviews.map((r) => r.bookTitle)).toContain("Nháp");
    expect((await listAdminCategories()).reduce((n, c) => n + c.bookCount, 0)).toBe(0);
  });

  it("tìm admin khớp ISBN và lọc ngôn ngữ", async () => {
    const vi = await mk({ title: "Việt", status: "draft", language: "vi", isbn: "978-0-306-40615-7" });
    await mk({ title: "Anh", language: "en" });
    expect((await listAdminBooks({ q: "0306406157", status: "all", page: 1 })).books.map((b) => b.id)).toEqual([vi.id]);
    expect((await listAdminBooks({ language: "vi", status: "all", page: 1 })).books.map((b) => b.id)).toEqual([vi.id]);
  });
});

describe("tác giả đóng góp", () => {
  it("đếm và liệt kê sách theo vai trò đóng góp", async () => {
    const a = await Author.create({
      name: "Nguyễn Du",
      nameKey: "nguyễn du",
      slug: "nguyen-du",
      aliases: [],
      lifespan: "",
      bio: "",
      notDuplicateOf: [],
    });
    const main = await mk({ title: "Truyện Kiều", author: "Nguyễn Du", authorId: a._id });
    const translated = await mk({
      title: "Sách dịch",
      author: "Ai Đó",
      contributors: [{ authorId: a._id, role: "translator" }],
    });
    const list = await listAuthors({ page: 1 });
    expect(list.authors.find((x) => x.id === a.id)?.bookCount).toBe(2);
    const detail = await getAuthorById(a.id);
    expect(detail?.books.map((b) => [b.title, b.role])).toEqual([
      ["Sách dịch", "translator"],
      ["Truyện Kiều", "author"],
    ]);
    expect([main.id, translated.id]).toContain(detail?.books[0].id);
  });
});
