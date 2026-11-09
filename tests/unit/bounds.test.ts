import { describe, expect, it } from "vitest";
import { parsePage } from "@/lib/validation";

describe("parsePage", () => {
  it("mặc định về trang 1 khi thiếu hoặc sai", () => {
    expect(parsePage(undefined)).toBe(1);
    expect(parsePage("abc")).toBe(1);
    expect(parsePage("0")).toBe(1);
    expect(parsePage("-5")).toBe(1);
  });

  it("giữ nguyên trang hợp lệ", () => {
    expect(parsePage("3")).toBe(3);
    expect(parsePage(["7"])).toBe(7);
  });

  it("chặn cận trên để không skip quá lớn", () => {
    expect(parsePage("100000000")).toBe(10_000);
  });
});
