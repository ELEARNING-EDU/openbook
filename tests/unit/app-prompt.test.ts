import { afterEach, describe, expect, it } from "vitest";
import { appBookUrl } from "@/lib/reader/app-links";

afterEach(() => {
  delete process.env.NEXT_PUBLIC_APP_BOOK_URL;
});

describe("appBookUrl", () => {
  it("thay {slug}", () => {
    process.env.NEXT_PUBLIC_APP_BOOK_URL = "openbook://books/{slug}";
    expect(appBookUrl("so-do")).toBe("openbook://books/so-do");
  });
  it("biến rỗng → null", () => {
    process.env.NEXT_PUBLIC_APP_BOOK_URL = "";
    expect(appBookUrl("so-do")).toBeNull();
    delete process.env.NEXT_PUBLIC_APP_BOOK_URL;
    expect(appBookUrl("so-do")).toBeNull();
  });
});
