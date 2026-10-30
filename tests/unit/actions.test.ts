import { beforeEach, describe, expect, it, vi } from "vitest";
import { link, setupTestDB } from "../helpers/mongo";

const guard = vi.hoisted(() => ({ requireAdmin: vi.fn(), requireUser: vi.fn() }));
vi.mock("@/lib/auth-guard", () => guard);
vi.mock("next/cache", () => ({ revalidatePath: vi.fn() }));

const { saveBook, deleteBook, toggleFeatured } = await import("@/actions/books");
const { saveCategory, deleteCategory } = await import("@/actions/categories");
const { submitReview, setReviewHidden, deleteReview } = await import("@/actions/reviews");
const { Book } = await import("@/models/Book");
const { Category } = await import("@/models/Category");
const { Review } = await import("@/models/Review");

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
  it("toggleFeatured đảo trạng thái", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    await toggleFeatured(id);
    expect((await Book.findById(id).lean())?.featured).toBe(true);
  });
});

describe("xoá dây chuyền", () => {
  it("xoá sách xoá review của sách", async () => {
    asAdmin();
    const { id } = (await saveBook(null, validBook)) as { id: string };
    await submitReview(id, { rating: 5, comment: "Hay" });
    expect((await deleteBook(id)).ok).toBe(true);
    expect(await Review.countDocuments()).toBe(0);
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
