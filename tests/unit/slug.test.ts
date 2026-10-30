import { describe, expect, it } from "vitest";
import { slugify } from "@/lib/slug";

describe("slugify", () => {
  it("bỏ dấu tiếng Việt và chữ Đ", () => {
    expect(slugify("Đất Rừng Phương Nam")).toBe("dat-rung-phuong-nam");
  });
  it("cắt khoảng trắng và ký tự đặc biệt", () => {
    expect(slugify("  Truyện Kiều!! ")).toBe("truyen-kieu");
    expect(slugify("C++ & Bạn")).toBe("c-ban");
  });
  it("fallback khi rỗng", () => {
    expect(slugify("!!!")).toBe("sach");
  });
});
