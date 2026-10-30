import { describe, expect, it } from "vitest";
import { bookSchema, categorySchema, parsePage, reviewSchema } from "@/lib/validation";

const validBook = {
  title: "T",
  author: "A",
  description: "",
  coverUrl: "https://x.com/c.jpg",
  links: [{ label: "Đọc", url: "https://archive.org/x" }],
  categoryIds: [],
  featured: false,
};

describe("bookSchema", () => {
  it("chấp nhận sách hợp lệ", () => {
    expect(bookSchema.safeParse(validBook).success).toBe(true);
  });
  it("từ chối URL javascript:", () => {
    expect(bookSchema.safeParse({ ...validBook, coverUrl: "javascript:alert(1)" }).success).toBe(false);
    expect(
      bookSchema.safeParse({ ...validBook, links: [{ label: "x", url: "javascript:alert(1)" }] }).success,
    ).toBe(false);
  });
  it("URL rỗng/không hợp lệ trả lỗi thay vì throw", () => {
    const r = bookSchema.safeParse({ ...validBook, links: [{ label: "x", url: "" }], coverUrl: "not a url" });
    expect(r.success).toBe(false);
  });
  it("epubUrl rỗng hoặc http/https tên miền", () => {
    expect(bookSchema.safeParse({ ...validBook, epubUrl: "" }).success).toBe(true);
    expect(bookSchema.safeParse({ ...validBook, epubUrl: "https://www.gutenberg.org/ebooks/1342.epub3.images" }).success).toBe(true);
    expect(bookSchema.safeParse({ ...validBook, epubUrl: "http://127.0.0.1/x.epub" }).success).toBe(false);
    expect(bookSchema.safeParse({ ...validBook, epubUrl: "file:///etc/passwd" }).success).toBe(false);
  });
  it("yêu cầu ít nhất 1 link", () => {
    expect(bookSchema.safeParse({ ...validBook, links: [] }).success).toBe(false);
  });
  it("bìa không bắt buộc", () => {
    expect(bookSchema.safeParse({ ...validBook, coverUrl: "" }).success).toBe(true);
  });
  it("từ chối categoryId không phải ObjectId", () => {
    expect(bookSchema.safeParse({ ...validBook, categoryIds: ["abc"] }).success).toBe(false);
  });
  it("từ chối tiêu đề chỉ có khoảng trắng", () => {
    expect(bookSchema.safeParse({ ...validBook, title: "   " }).success).toBe(false);
  });
});

describe("categorySchema", () => {
  it("yêu cầu tên", () => {
    expect(categorySchema.safeParse({ name: " " }).success).toBe(false);
    expect(categorySchema.safeParse({ name: "Văn học" }).success).toBe(true);
  });
});

describe("reviewSchema", () => {
  it("rating nguyên 1–5", () => {
    expect(reviewSchema.safeParse({ rating: 6, comment: "" }).success).toBe(false);
    expect(reviewSchema.safeParse({ rating: 0, comment: "" }).success).toBe(false);
    expect(reviewSchema.safeParse({ rating: 3.5, comment: "" }).success).toBe(false);
    expect(reviewSchema.safeParse({ rating: 5, comment: "" }).success).toBe(true);
  });
  it("comment tối đa 2000 ký tự", () => {
    expect(reviewSchema.safeParse({ rating: 5, comment: "a".repeat(2001) }).success).toBe(false);
    expect(reviewSchema.safeParse({ rating: 5, comment: "a".repeat(2000) }).success).toBe(true);
  });
});

describe("parsePage", () => {
  it("chuẩn hoá trang", () => {
    expect(parsePage("abc")).toBe(1);
    expect(parsePage("-1")).toBe(1);
    expect(parsePage(undefined)).toBe(1);
    expect(parsePage("3")).toBe(3);
    expect(parsePage(["2", "5"])).toBe(2);
  });
});
