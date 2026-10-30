import { describe, expect, it } from "vitest";
import { link, setupTestDB } from "../helpers/mongo";
import { Book } from "@/models/Book";
import { Review } from "@/models/Review";
import { recalcBookRating, recordDownload, uniqueSlug } from "@/lib/book-service";

setupTestDB();

const book = (over: Record<string, unknown> = {}) =>
  Book.create({ title: "Truyện Kiều", slug: "truyen-kieu", author: "Nguyễn Du", links: link(), ...over });

describe("uniqueSlug", () => {
  it("thêm hậu tố khi trùng", async () => {
    const b = await book();
    expect(await uniqueSlug("truyen-kieu", "Book")).toBe("truyen-kieu-2");
    await book({ slug: "truyen-kieu-2" });
    expect(await uniqueSlug("truyen-kieu", "Book")).toBe("truyen-kieu-3");
    expect(await uniqueSlug("truyen-kieu", "Book", b.id)).toBe("truyen-kieu");
  });
  it("giữ nguyên khi chưa có", async () => {
    expect(await uniqueSlug("van-hoc", "Category")).toBe("van-hoc");
  });
});

describe("recalcBookRating", () => {
  it("bỏ qua review bị ẩn", async () => {
    const b = await book();
    await Review.create([
      { bookId: b._id, userId: "u1", userName: "A", rating: 5 },
      { bookId: b._id, userId: "u2", userName: "B", rating: 4 },
      { bookId: b._id, userId: "u3", userName: "C", rating: 1, hidden: true },
    ]);
    await recalcBookRating(b.id);
    const fresh = await Book.findById(b._id).lean();
    expect(fresh?.ratingAvg).toBe(4.5);
    expect(fresh?.ratingCount).toBe(2);
  });
});

describe("recordDownload", () => {
  it("tăng lượt tải và trả URL", async () => {
    const b = await book({ links: [...link("https://a.org/1"), ...link("https://a.org/2")] });
    expect(await recordDownload("truyen-kieu", 1)).toBe("https://a.org/2");
    expect((await Book.findById(b._id).lean())?.downloadCount).toBe(1);
  });
  it("index/slug không hợp lệ → null, không tăng", async () => {
    const b = await book();
    for (const i of [5, 1, -1, NaN, 0.5]) expect(await recordDownload("truyen-kieu", i)).toBeNull();
    expect(await recordDownload("khong-co", 0)).toBeNull();
    expect((await Book.findById(b._id).lean())?.downloadCount).toBe(0);
  });
});

describe("Review unique index", () => {
  it("không cho 2 review cùng user/sách", async () => {
    const b = await book();
    await Review.create({ bookId: b._id, userId: "u1", userName: "A", rating: 5 });
    await expect(Review.create({ bookId: b._id, userId: "u1", userName: "A", rating: 3 })).rejects.toThrow(/duplicate key/);
  });
});
