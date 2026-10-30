import { describe, expect, it } from "vitest";
import { isAdmin } from "@/lib/admin";

describe("isAdmin", () => {
  it("so khớp email không phân biệt hoa thường, bỏ khoảng trắng", () => {
    expect(isAdmin("A@x.com", "a@x.com, b@y.com")).toBe(true);
  });
  it("từ chối email không có trong danh sách", () => {
    expect(isAdmin("c@z.com", "a@x.com")).toBe(false);
  });
  it("từ chối khi thiếu email hoặc danh sách rỗng", () => {
    expect(isAdmin(undefined, "a@x.com")).toBe(false);
    expect(isAdmin("a@x.com", "")).toBe(false);
    expect(isAdmin("", " , ")).toBe(false);
  });
});
