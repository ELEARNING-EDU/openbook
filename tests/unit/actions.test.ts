import { beforeEach, describe, expect, it, vi } from "vitest";
import { link, setupTestDB } from "../helpers/mongo";

const guard = vi.hoisted(() => ({ requireAdmin: vi.fn(), requireUser: vi.fn() }));
vi.mock("@/lib/auth-guard", () => guard);
vi.mock("next/cache", () => ({ revalidatePath: vi.fn() }));

const { saveBook, deleteBook, toggleFeatured, recordRead, bulkSetBookStatus, bulkAddCategory, bulkDeleteBooks, checkEpubLink } = await import("@/actions/books");
const { saveAuthor, deleteAuthor, mergeAuthors, markNotDuplicate } = await import("@/actions/authors");
const { saveCategory, saveCategoryMenu, deleteCategory } = await import("@/actions/categories");
const { saveSeries, deleteSeries } = await import("@/actions/series");
const { savePublisher, deletePublisher } = await import("@/actions/publishers");
const { submitReview, setReviewHidden, deleteReview } = await import("@/actions/reviews");
const { recordDownload } = await import("@/lib/book-service");
const { Author } = await import("@/models/Author");
const { Book } = await import("@/models/Book");
const { BookDailyStat } = await import("@/models/BookDailyStat");
const { Category } = await import("@/models/Category");
const { Publisher } = await import("@/models/Publisher");
const { Review } = await import("@/models/Review");
const { Series } = await import("@/models/Series");
const { dayKey } = await import("@/lib/stats");

setupTestDB();

const reader = { id: "g-1", name: "Đọc Giả", email: "r@x.com", image: "https://x/a.png", isAdmin: false };
const admin = { ...reader, id: "g-admin", isAdmin: true };
const validBook = { title: "Truyện Kiều", author: "Nguyễn Du", description: "", coverUrl: "", links: link(), categoryIds: [], featured: false };

const asAdmin = () => {
  guard.requireAdmin.mockResolvedValue(admin);
  guard.requireUser.mockResolvedValue(admin);
};
const asReader = () => {
  guard.requireAdmin.mockRejectedValue(new Error("FORBIDDEN"));
  guard.requireUser.mockResolvedValue(reader);
};
const asGuest = () => {
  guard.requireAdmin.mockRejectedValue(new Error("UNAUTHORIZED"));
  guard.requireUser.mockRejectedValue(new Error("UNAUTHORIZED"));
};

beforeEach(() => {
  guard.requireAdmin.mockReset();
  guard.requireUser.mockReset();
});

describe("phân quyền admin", () => {
  it("người đọc không tạo/xoá/sửa được", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    asReader();
    const r = await saveBook(null, validBook);
    expect(r).toEqual({ ok: false, errors: { _form: ["Bạn không có quyền thực hiện thao tác này"] } });
    expect((await deleteBook(id)).ok).toBe(false);
    expect((await toggleFeatured(id)).ok).toBe(false);
    expect((await saveCategory(null, { name: "X" })).ok).toBe(false);
    expect(await Book.countDocuments()).toBe(1);
    expect(await Category.countDocuments()).toBe(0);
  });
});

describe("saveBook", () => {
  it("báo lỗi theo trường khi URL không hợp lệ", async () => {
    asAdmin();
    const r = await saveBook(null, { ...validBook, coverUrl: "javascript:x" });
    expect(r.ok).toBe(false);
    expect(!r.ok && r.errors.coverUrl).toBeTruthy();
  });
  it("slug duy nhất, sửa giữ slug của chính nó", async () => {
    asAdmin();
    const a = await saveBook(null, validBook);
    await saveBook(null, validBook);
    expect((await Book.find().sort({ createdAt: 1 }).lean()).map((b) => b.slug)).toEqual(["truyen-kieu", "truyen-kieu-2"]);
    const id = (a as { id: string }).id;
    expect((await saveBook(id, { ...validBook, author: "N. Du" })).ok).toBe(true);
    expect((await Book.findById(id).lean())?.slug).toBe("truyen-kieu");
  });
  it("id không tồn tại → lỗi", async () => {
    asAdmin();
    expect((await saveBook("0123456789abcdef01234567", validBook)).ok).toBe(false);
    expect((await saveBook("abc", validBook)).ok).toBe(false);
  });
  it("lưu epubUrl", async () => {
    asAdmin();
    const url = "https://www.gutenberg.org/ebooks/84.epub3.images";
    const { id } = (await saveBook(null, { ...validBook, epubUrl: url })) as { id: string };
    expect((await Book.findById(id).lean())?.epubUrl).toBe(url);
  });
  it("recordRead tăng lượt tải, không cần đăng nhập", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    asGuest();
    await recordRead("truyen-kieu");
    expect((await Book.findById(id).lean())?.downloadCount).toBe(1);
    const stat = await BookDailyStat.findOne({ bookId: id }).lean();
    expect(stat).toMatchObject({ reads: 1, downloads: 0, date: dayKey(new Date()) });
  });
  it("recordRead không đổi updatedAt", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    const before = (await Book.findById(id).lean())?.updatedAt;
    await new Promise((r) => setTimeout(r, 5));
    asGuest();
    await recordRead("truyen-kieu");
    expect((await Book.findById(id).lean())?.updatedAt).toEqual(before);
  });
  it("recordRead cho sách không tồn tại không ghi số liệu", async () => {
    asGuest();
    await recordRead("khong-co");
    expect(await BookDailyStat.countDocuments()).toBe(0);
  });
  it("toggleFeatured đảo trạng thái", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    await toggleFeatured(id);
    expect((await Book.findById(id).lean())?.featured).toBe(true);
  });
});

describe("xoá dây chuyền", () => {
  it("xoá sách xoá review và số liệu ngày của sách", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    await submitReview(id, { rating: 5, comment: "Hay" });
    asGuest();
    await recordRead("truyen-kieu");
    asAdmin();
    expect((await deleteBook(id)).ok).toBe(true);
    expect(await Review.countDocuments()).toBe(0);
    expect(await BookDailyStat.countDocuments()).toBe(0);
  });
  it("xoá thể loại giữ sách, gỡ id", async () => {
    asAdmin();
    const { id: catId } = (await saveCategory(null, { name: "Văn học" })) as { id: string };
    const { id } = (await saveBook(null, { ...validBook, categoryIds: [catId] })) as { id: string };
    expect((await deleteCategory(catId)).ok).toBe(true);
    const b = await Book.findById(id).lean();
    expect(b).not.toBeNull();
    expect(b?.categoryIds).toEqual([]);
  });
});

describe("menu thể loại", () => {
  it("lưu thứ tự và ẩn/hiện", async () => {
    asAdmin();
    const { id: a } = (await saveCategory(null, { name: "A" })) as { id: string };
    const { id: b } = (await saveCategory(null, { name: "B" })) as { id: string };
    expect((await saveCategoryMenu([{ id: b, showInMenu: false }, { id: a, showInMenu: true }])).ok).toBe(true);
    const rows = await Category.find().sort({ menuOrder: 1 }).lean();
    expect(rows.map((c) => [c.name, c.menuOrder, c.showInMenu])).toEqual([
      ["B", 0, false],
      ["A", 1, true],
    ]);
  });
  it("từ chối người không phải admin và dữ liệu sai", async () => {
    asAdmin();
    const { id } = (await saveCategory(null, { name: "A" })) as { id: string };
    asReader();
    expect((await saveCategoryMenu([{ id, showInMenu: false }])).ok).toBe(false);
    asAdmin();
    expect((await saveCategoryMenu([{ id: "abc", showInMenu: false }])).ok).toBe(false);
    expect(
      (await saveCategoryMenu([
        { id, showInMenu: false },
        { id, showInMenu: true },
      ])).ok,
    ).toBe(false);
    const row = await Category.findById(id).lean();
    expect([row?.showInMenu, row?.menuOrder]).toEqual([true, 0]);
  });
  it("thể loại mới nối vào cuối menu", async () => {
    asAdmin();
    await saveCategory(null, { name: "A" });
    await saveCategory(null, { name: "B" });
    const { id } = (await saveCategory(null, { name: "C" })) as { id: string };
    expect((await Category.findById(id).lean())?.menuOrder).toBe(2);
  });
  it("xoá kèm chuyển sách không nhân đôi và không xoá sách", async () => {
    asAdmin();
    const { id: from } = (await saveCategory(null, { name: "Từ" })) as { id: string };
    const { id: to } = (await saveCategory(null, { name: "Đến" })) as { id: string };
    const { id: onlyFrom } = (await saveBook(null, { ...validBook, categoryIds: [from] })) as { id: string };
    const { id: both } = (await saveBook(null, { ...validBook, title: "Khác", categoryIds: [from, to] })) as { id: string };
    expect((await deleteCategory(from, to)).ok).toBe(true);
    expect(await Book.countDocuments()).toBe(2);
    expect((await Book.findById(onlyFrom).lean())?.categoryIds.map(String)).toEqual([to]);
    expect((await Book.findById(both).lean())?.categoryIds.map(String)).toEqual([to]);
  });
  it("không xoá khi đích chuyển không hợp lệ", async () => {
    asAdmin();
    const { id } = (await saveCategory(null, { name: "A" })) as { id: string };
    expect((await deleteCategory(id, id)).ok).toBe(false);
    expect((await deleteCategory(id, "0123456789abcdef01234567")).ok).toBe(false);
    expect(await Category.countDocuments()).toBe(1);
  });
});


describe("tác giả", () => {
  it("saveBook tìm hoặc tạo tác giả và giữ tên chuẩn trên sách", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    const author = await Author.findOne().lean();
    const book = await Book.findById(id).lean();
    expect(String(book?.authorId)).toBe(String(author?._id));
    expect(book?.author).toBe("Nguyễn Du");

    await Author.updateOne({ _id: author?._id }, { $set: { aliases: ["Du Nguyen"] } });
    const { id: other } = (await saveBook(null, { ...validBook, title: "Khác", author: "du nguyen" })) as { id: string };
    expect(await Author.countDocuments()).toBe(1);
    expect((await Book.findById(other).lean())?.author).toBe("Nguyễn Du");
    expect(String((await Book.findById(other).lean())?.authorId)).toBe(String(author?._id));
  });

  it("tên hoặc tên khác trùng tác giả khác trả lỗi theo trường", async () => {
    asAdmin();
    await saveAuthor(null, { name: "Nguyễn Du", aliases: ["Tố Như"] });
    const sameName = await saveAuthor(null, { name: "nguyễn du" });
    expect(sameName.ok).toBe(false);
    expect(!sameName.ok && sameName.errors.name).toBeTruthy();
    const sameAlias = await saveAuthor(null, { name: "Khác", aliases: ["tố như"] });
    expect(sameAlias.ok).toBe(false);
    expect(!sameAlias.ok && sameAlias.errors.aliases).toBeTruthy();
    expect((await saveAuthor("abc", { name: "Khác" })).ok).toBe(false);
    expect(await Author.countDocuments()).toBe(1);
  });

  it("đổi tên cập nhật sách nhưng không đổi updatedAt", async () => {
    asAdmin();
    const { id: authorId } = (await saveAuthor(null, { name: "Nguyễn Du" })) as { id: string };
    const { id: bookId } = (await saveBook(null, validBook)) as { id: string };
    const before = (await Book.findById(bookId).lean())?.updatedAt;
    await new Promise((resolve) => setTimeout(resolve, 5));
    expect((await saveAuthor(authorId, { name: "Nguyễn Du Mới" })).ok).toBe(true);
    const book = await Book.findById(bookId).lean();
    expect(book?.author).toBe("Nguyễn Du Mới");
    expect(book?.updatedAt).toEqual(before);
  });

  it("không xoá tác giả còn sách", async () => {
    asAdmin();
    const { id: authorId } = (await saveAuthor(null, { name: "Nguyễn Du" })) as { id: string };
    const { id: bookId } = (await saveBook(null, validBook)) as { id: string };
    expect((await deleteAuthor(authorId)).ok).toBe(false);
    expect(await Author.countDocuments()).toBe(1);
    expect((await deleteBook(bookId)).ok).toBe(true);
    expect((await deleteAuthor(authorId)).ok).toBe(true);
    expect(await Author.countDocuments()).toBe(0);
  });

  it("gộp chuyển sách, giữ tên cũ làm tên khác và lấp chỗ trống", async () => {
    asAdmin();
    const { id: keepId } = (await saveAuthor(null, { name: "Lão Tử" })) as { id: string };
    await saveBook(null, { ...validBook, title: "Tao Te Ching", author: "Lão Tử" });
    const { id: removeId } = (await saveAuthor(null, {
      name: "Laozi",
      aliases: ["Lao Tse"],
      bio: "Triết gia",
      lifespan: "Thế kỷ 6 TCN",
    })) as { id: string };
    const { id: secondBook } = (await saveBook(null, { ...validBook, title: "Đạo Đức Kinh", author: "Laozi" })) as { id: string };
    const { id: thirdId } = (await saveAuthor(null, { name: "Khác" })) as { id: string };
    await markNotDuplicate(thirdId, removeId);

    expect((await mergeAuthors(keepId, removeId)).ok).toBe(true);
    const keep = await Author.findById(keepId).lean();
    expect(keep?.aliases).toEqual(expect.arrayContaining(["Laozi", "Lao Tse"]));
    expect(keep?.bio).toBe("Triết gia");
    expect(keep?.lifespan).toBe("Thế kỷ 6 TCN");
    expect(await Author.findById(removeId).lean()).toBeNull();
    for (const book of await Book.find({ authorId: keepId }).lean()) {
      expect(book.author).toBe("Lão Tử");
    }
    expect(String((await Book.findById(secondBook).lean())?.authorId)).toBe(keepId);
    expect((await Author.findById(thirdId).lean())?.notDuplicateOf.map(String)).not.toContain(removeId);
  });

  it("không gộp cùng một tác giả, id sai hoặc khi không phải admin", async () => {
    asAdmin();
    const { id: a } = (await saveAuthor(null, { name: "A" })) as { id: string };
    const { id: b } = (await saveAuthor(null, { name: "B" })) as { id: string };
    expect((await mergeAuthors(a, a)).ok).toBe(false);
    expect((await mergeAuthors(a, "0123456789abcdef01234567")).ok).toBe(false);
    asReader();
    expect((await mergeAuthors(a, b)).ok).toBe(false);
    expect(await Author.countDocuments()).toBe(2);
  });

  it("đánh dấu hai người khác nhau lưu trên cả hai hồ sơ", async () => {
    asAdmin();
    const { id: a } = (await saveAuthor(null, { name: "A" })) as { id: string };
    const { id: b } = (await saveAuthor(null, { name: "B" })) as { id: string };
    expect((await markNotDuplicate(a, b)).ok).toBe(true);
    expect((await Author.findById(a).lean())?.notDuplicateOf.map(String)).toEqual([b]);
    expect((await Author.findById(b).lean())?.notDuplicateOf.map(String)).toEqual([a]);
  });
});

describe("reviews", () => {
  it("gửi lại cập nhật review cũ và tính lại điểm", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    asReader();
    await submitReview(id, { rating: 5, comment: "" });
    await submitReview(id, { rating: 3, comment: "Đổi ý" });
    expect(await Review.countDocuments()).toBe(1);
    const b = await Book.findById(id).lean();
    expect([b?.ratingAvg, b?.ratingCount]).toEqual([3, 1]);
    expect((await Review.findOne().lean())?.userName).toBe("Đọc Giả");
  });
  it("review đã bị admin ẩn vẫn ẩn khi người viết gửi lại", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    asReader();
    await submitReview(id, { rating: 1, comment: "spam" });
    const rid = String((await Review.findOne().lean())?._id);
    asAdmin();
    await setReviewHidden(rid, true);
    asReader();
    await submitReview(id, { rating: 1, comment: "spam" });
    expect((await Review.findById(rid).lean())?.hidden).toBe(true);
    expect((await Book.findById(id).lean())?.ratingCount).toBe(0);
  });
  it("khách không gửi được", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    asGuest();
    expect((await submitReview(id, { rating: 5, comment: "" })).ok).toBe(false);
    expect(await Review.countDocuments()).toBe(0);
  });
  it("review cho sách không tồn tại → lỗi", async () => {
    asReader();
    expect((await submitReview("0123456789abcdef01234567", { rating: 5, comment: "" })).ok).toBe(false);
    expect(await Review.countDocuments()).toBe(0);
  });
  it("ẩn/xoá review cập nhật điểm", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    await submitReview(id, { rating: 4, comment: "" });
    const rid = String((await Review.findOne().lean())?._id);
    await setReviewHidden(rid, true);
    expect((await Book.findById(id).lean())?.ratingCount).toBe(0);
    await setReviewHidden(rid, false);
    expect((await Book.findById(id).lean())?.ratingCount).toBe(1);
    await deleteReview(rid);
    expect((await Book.findById(id).lean())?.ratingCount).toBe(0);
  });
});

describe("bản nháp", () => {
  it("không được đếm, không tải được và không nhận đánh giá", async () => {
    asAdmin();
    const { id } = (await saveBook(null, { ...validBook, status: "draft" })) as { id: string };
    asGuest();
    await recordRead("truyen-kieu");
    expect(await recordDownload("truyen-kieu", 0)).toBeNull();
    const book = await Book.findById(id).lean();
    expect(book?.downloadCount).toBe(0);
    expect(await BookDailyStat.countDocuments()).toBe(0);
    asReader();
    expect((await submitReview(id, { rating: 5, comment: "Hay" })).ok).toBe(false);
    expect(await Review.countDocuments()).toBe(0);
  });
});

describe("trường mới của sách", () => {
  it("tìm hoặc tạo bộ sách, nhà xuất bản và người đóng góp", async () => {
    asAdmin();
    const { id } = (await saveBook(null, {
      ...validBook,
      series: "Sherlock Holmes",
      seriesNumber: 2,
      publisher: "NXB Trẻ",
      language: "vi",
      isbn: "978-0-306-40615-7",
      publishYear: 2020,
      contributors: [
        { name: "Dịch Giả", role: "translator" },
        { name: "Dịch Giả", role: "translator" },
        { name: "Biên Tập", role: "editor" },
      ],
    })) as { id: string };
    const book = await Book.findById(id).lean();
    expect(book).toMatchObject({ seriesNumber: 2, language: "vi", publishYear: 2020, isbn: "978-0-306-40615-7" });
    expect(book?.contributors).toHaveLength(2);
    expect(await Series.countDocuments()).toBe(1);
    expect(await Publisher.countDocuments()).toBe(1);
    expect(await Author.countDocuments()).toBe(3);

    const { id: second } = (await saveBook(null, {
      ...validBook,
      title: "Sách khác",
      series: "sherlock holmes",
      publisher: "nxb trẻ",
      contributors: [{ name: "Dịch Giả", role: "translator" }],
    })) as { id: string };
    expect(await Series.countDocuments()).toBe(1);
    expect(await Publisher.countDocuments()).toBe(1);
    expect(await Author.countDocuments()).toBe(3);
    expect((await Book.findById(second).lean())?.contributors).toHaveLength(1);
  });

  it("báo lỗi ISBN, năm và tập số thiếu bộ sách", async () => {
    asAdmin();
    const isbn = await saveBook(null, { ...validBook, isbn: "123" });
    expect(!isbn.ok && isbn.errors.isbn).toBeTruthy();
    const year = await saveBook(null, { ...validBook, publishYear: 0 });
    expect(!year.ok && year.errors.publishYear).toBeTruthy();
    const number = await saveBook(null, { ...validBook, series: "", seriesNumber: 2 });
    expect(!number.ok && number.errors.seriesNumber).toBeTruthy();
    expect(await Book.countDocuments()).toBe(0);
  });
});

describe("thao tác hàng loạt", () => {
  const oid = (n: number) => n.toString(16).padStart(24, "0");

  it("từ chối người không phải admin, id sai, trùng và quá số lượng", async () => {
    asAdmin();
    const { id: a } = (await saveBook(null, validBook)) as { id: string };
    const { id: b } = (await saveBook(null, { ...validBook, title: "Khác" })) as { id: string };
    asReader();
    expect((await bulkSetBookStatus([a], "draft")).ok).toBe(false);
    expect((await bulkAddCategory([a], "0123456789abcdef01234567")).ok).toBe(false);
    expect((await bulkDeleteBooks([a])).ok).toBe(false);
    asAdmin();
    expect((await bulkSetBookStatus(["abc"], "published")).ok).toBe(false);
    expect((await bulkSetBookStatus([a, a], "published")).ok).toBe(false);
    expect((await bulkSetBookStatus(Array.from({ length: 21 }, (_, i) => oid(i + 1)), "published")).ok).toBe(false);
    expect((await bulkDeleteBooks([])).ok).toBe(false);
    expect(await Book.countDocuments()).toBe(2);
    expect(b).toBeTruthy();
  });

  it("xuất bản, chuyển nháp, thêm thể loại", async () => {
    asAdmin();
    const { id: a } = (await saveBook(null, validBook)) as { id: string };
    const { id: b } = (await saveBook(null, { ...validBook, title: "Khác" })) as { id: string };
    const { id: cat } = (await saveCategory(null, { name: "Văn học" })) as { id: string };

    expect((await bulkSetBookStatus([a, b], "draft")).ok).toBe(true);
    expect(await Book.countDocuments({ status: "draft" })).toBe(2);
    expect((await bulkSetBookStatus([a, b], "published")).ok).toBe(true);
    expect((await bulkAddCategory([a, b], cat)).ok).toBe(true);
    for (const book of await Book.find().lean()) expect(book.categoryIds.map(String)).toEqual([cat]);
  });

  it("xoá hàng loạt kèm đánh giá và số liệu", async () => {
    asAdmin();
    const { id: a } = (await saveBook(null, validBook)) as { id: string };
    const { id: b } = (await saveBook(null, { ...validBook, title: "Khác" })) as { id: string };
    asReader();
    await submitReview(a, { rating: 5, comment: "" });
    await submitReview(b, { rating: 4, comment: "" });
    asGuest();
    await recordRead("truyen-kieu");
    asAdmin();
    expect((await bulkDeleteBooks([a, b])).ok).toBe(true);
    expect(await Book.countDocuments()).toBe(0);
    expect(await Review.countDocuments()).toBe(0);
    expect(await BookDailyStat.countDocuments()).toBe(0);
  });
});

describe("bộ sách và nhà xuất bản", () => {
  it("đổi tên, báo trùng tên và chỉ admin được sửa", async () => {
    asAdmin();
    const { id } = (await saveSeries(null, { name: "Bộ A" })) as { id: string };
    expect((await saveSeries(id, { name: "Bộ A mới" })).ok).toBe(true);
    expect((await Series.findById(id).lean())?.name).toBe("Bộ A mới");
    await saveSeries(null, { name: "Bộ B" });
    const clash = await saveSeries(null, { name: "bộ b" });
    expect(!clash.ok && clash.errors.name).toBeTruthy();
    expect((await saveSeries(null, { name: " " })).ok).toBe(false);
    asReader();
    expect((await saveSeries(null, { name: "Bộ C" })).ok).toBe(false);
    expect((await savePublisher(null, { name: "NXB" })).ok).toBe(false);
  });

  it("xoá bộ sách: chuyển sách, và xoá hẳn thì bỏ số tập", async () => {
    asAdmin();
    const { id: from } = (await saveSeries(null, { name: "Bộ A" })) as { id: string };
    const { id: to } = (await saveSeries(null, { name: "Bộ B" })) as { id: string };
    const { id: bookId } = (await saveBook(null, { ...validBook, series: "Bộ A", seriesNumber: 3 })) as { id: string };
    expect((await deleteSeries(from, to)).ok).toBe(true);
    let book = await Book.findById(bookId).lean();
    expect([String(book?.seriesId), book?.seriesNumber]).toEqual([to, 3]);
    expect((await deleteSeries(to)).ok).toBe(true);
    book = await Book.findById(bookId).lean();
    expect([book?.seriesId, book?.seriesNumber]).toEqual([null, null]);
    expect(await Book.countDocuments()).toBe(1);
    expect((await deleteSeries(from, "abc")).ok).toBe(false);
  });

  it("xoá nhà xuất bản: chuyển sách rồi bỏ tham chiếu", async () => {
    asAdmin();
    const { id: from } = (await savePublisher(null, { name: "NXB A" })) as { id: string };
    const { id: to } = (await savePublisher(null, { name: "NXB B" })) as { id: string };
    const { id: bookId } = (await saveBook(null, { ...validBook, publisher: "NXB A" })) as { id: string };
    expect((await deletePublisher(from, to)).ok).toBe(true);
    expect(String((await Book.findById(bookId).lean())?.publisherId)).toBe(to);
    expect((await deletePublisher(to)).ok).toBe(true);
    expect((await Book.findById(bookId).lean())?.publisherId).toBeNull();
    expect(await Book.countDocuments()).toBe(1);
  });
});

describe("người đóng góp của tác giả", () => {
  it("không xoá tác giả còn là người đóng góp; gộp thì chuyển và bỏ trùng", async () => {
    asAdmin();
    const { id: keep } = (await saveAuthor(null, { name: "Lão Tử" })) as { id: string };
    const { id: remove } = (await saveAuthor(null, { name: "Laozi" })) as { id: string };
    const { id: bookId } = (await saveBook(null, {
      ...validBook,
      author: "Ai Đó",
      contributors: [
        { name: "Lão Tử", role: "translator" },
        { name: "Laozi", role: "translator" },
        { name: "Lão Tử", role: "editor" },
      ],
    })) as { id: string };

    expect((await deleteAuthor(remove)).ok).toBe(false);
    expect(await Author.countDocuments()).toBe(3);
    expect((await mergeAuthors(keep, remove)).ok).toBe(true);
    const book = await Book.findById(bookId).lean();
    expect(book?.contributors).toHaveLength(2);
    expect(book?.contributors.map((c) => String(c.authorId))).toEqual([keep, keep]);
    expect(book?.contributors.map((c) => c.role)).toEqual(["translator", "editor"]);
    expect(await Author.findById(remove).lean()).toBeNull();
  });
});

describe("kiểm tra link EPUB", () => {
  it("từ chối người không phải admin và địa chỉ riêng mà không gọi mạng", async () => {
    const fetchSpy = vi.fn();
    vi.stubGlobal("fetch", fetchSpy);
    try {
      asReader();
      const forbidden = await checkEpubLink("https://a.org/x.epub");
      expect(forbidden.ok).toBe(false);
      asAdmin();
      for (const url of ["http://127.0.0.1/x.epub", "http://192.168.1.5/x.epub", "http://localhost:3000/x.epub", "http://169.254.169.254/x.epub"]) {
        expect(await checkEpubLink(url)).toEqual({ ok: true, status: "unreachable" });
      }
      expect(fetchSpy).not.toHaveBeenCalled();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
