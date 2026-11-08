import { afterEach, describe, expect, it, vi } from "vitest";
import { clientKey, rateLimit } from "@/lib/rate-limit";

afterEach(() => {
  vi.useRealTimers();
});

describe("rateLimit", () => {
  it("cho qua đúng số lần rồi chặn", () => {
    const key = `limit-${Math.random()}`;
    expect(rateLimit(key, 2, 1000)).toBe(true);
    expect(rateLimit(key, 2, 1000)).toBe(true);
    expect(rateLimit(key, 2, 1000)).toBe(false);
  });

  it("mở lại sau khi hết cửa sổ", () => {
    vi.useFakeTimers();
    vi.setSystemTime(0);
    const key = `window-${Math.random()}`;
    expect(rateLimit(key, 1, 1000)).toBe(true);
    expect(rateLimit(key, 1, 1000)).toBe(false);
    vi.setSystemTime(1001);
    expect(rateLimit(key, 1, 1000)).toBe(true);
  });

  it("các key độc lập với nhau", () => {
    const a = `a-${Math.random()}`;
    const b = `b-${Math.random()}`;
    expect(rateLimit(a, 1, 1000)).toBe(true);
    expect(rateLimit(a, 1, 1000)).toBe(false);
    expect(rateLimit(b, 1, 1000)).toBe(true);
  });
});

describe("clientKey", () => {
  it("lấy IP đầu tiên trong x-forwarded-for", () => {
    expect(clientKey(new Headers({ "x-forwarded-for": "1.2.3.4, 10.0.0.1" }))).toBe("1.2.3.4");
  });

  it("fallback về x-real-ip rồi unknown", () => {
    expect(clientKey(new Headers({ "x-real-ip": "5.6.7.8" }))).toBe("5.6.7.8");
    expect(clientKey(new Headers())).toBe("unknown");
  });
});
