import { describe, expect, it } from "vitest";
import { makeT } from "@/lib/i18n";
import { makeBookSchema, makeCategorySchema, makeReviewSchema, parsePage } from "@/lib/validation";

const t = makeT("vi");
const bookSchema = makeBookSchema(t);
const categorySchema = makeCategorySchema(t);
const reviewSchema = makeReviewSchema(t);

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
  it("ISBN: 10 hoặc 13 chữ số, cho phép gạch nối, bỏ trống được", () => {
    expect(bookSchema.safeParse({ ...validBook, isbn: "" }).success).toBe(true);
    expect(bookSchema.safeParse({ ...validBook, isbn: "0-306-40615-2" }).success).toBe(true);
    expect(bookSchema.safeParse({ ...validBook, isbn: "978-0-306-40615-7" }).success).toBe(true);
    expect(bookSchema.safeParse({ ...validBook, isbn: "123" }).success).toBe(false);
    expect(bookSchema.safeParse({ ...validBook, isbn: "12345abcde" }).success).toBe(false);
    expect(bookSchema.safeParse({ ...validBook, isbn: "97803064061577" }).success).toBe(false);
  });
  it("năm xuất bản: số nguyên từ 1 đến năm sau, hoặc để trống", () => {
    const nextYear = new Date().getFullYear() + 1;
    expect(bookSchema.safeParse({ ...validBook, publishYear: null }).success).toBe(true);
    expect(bookSchema.safeParse({ ...validBook, publishYear: 2024 }).success).toBe(true);
    expect(bookSchema.safeParse({ ...validBook, publishYear: nextYear }).success).toBe(true);
    expect(bookSchema.safeParse({ ...validBook, publishYear: 0 }).success).toBe(false);
    expect(bookSchema.safeParse({ ...validBook, publishYear: nextYear + 1 }).success).toBe(false);
    expect(bookSchema.safeParse({ ...validBook, publishYear: "abc" as unknown as number }).success).toBe(false);
    expect(bookSchema.safeParse({ ...validBook, publishYear: 2024.5 }).success).toBe(false);
  });
  it("tập số mà không có bộ sách là lỗi", () => {
    expect(bookSchema.safeParse({ ...validBook, series: "", seriesNumber: 2 }).success).toBe(false);
    expect(bookSchema.safeParse({ ...validBook, series: "Sherlock Holmes", seriesNumber: 2 }).success).toBe(true);
    expect(bookSchema.safeParse({ ...validBook, series: "", seriesNumber: null }).success).toBe(true);
  });
  it("mặc định trạng thái xuất bản, ngôn ngữ trống; từ chối vai trò lạ", () => {
    const parsed = bookSchema.parse(validBook);
    expect(parsed.status).toBe("published");
    expect(parsed.language).toBe("");
    expect(
      bookSchema.safeParse({ ...validBook, contributors: [{ name: "A", role: "painter" }] }).success,
    ).toBe(false);
    expect(
      bookSchema.safeParse({ ...validBook, contributors: [{ name: "A", role: "translator" }] }).success,
    ).toBe(true);
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
