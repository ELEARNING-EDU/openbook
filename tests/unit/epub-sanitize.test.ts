// @vitest-environment jsdom
import { describe, expect, it } from "vitest";
import { sanitizeChapter } from "@/lib/epub/sanitize";

const wrap = (body: string) => `<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"><head><style>body{color:red}</style></head><body>${body}</body></html>`;
const opts = (imageUrl: (p: string) => string | undefined = () => undefined) => ({
  baseHref: "OEBPS/Text/ch1.xhtml",
  imageUrl,
  chapterIndexOf: (h: string) => (h === "OEBPS/Text/ch2.xhtml" ? 1 : -1),
});

describe("sanitizeChapter", () => {
  it("bỏ script, style, on*", () => {
    const out = sanitizeChapter(wrap('<p onclick="x">Hi</p><script>alert(1)</script><style>p{}</style>'), opts());
    expect(out).toContain("<p>Hi</p>");
    expect(out).not.toMatch(/script|style|onclick/);
  });
  it("ảnh → blob URL, không tìm thấy → xoá", () => {
    const urls = (p: string) => (p === "OEBPS/Images/a.png" ? "blob:1" : undefined);
    expect(sanitizeChapter(wrap('<img src="../Images/a.png" onerror="x" alt="A">'), opts(urls))).toBe('<img src="blob:1" alt="A">');
    expect(sanitizeChapter(wrap('<img src="../Images/none.png">'), opts(urls))).not.toContain("img");
  });
  it("link: javascript bỏ href, ngoài mở tab mới, nội bộ thành data-chapter", () => {
    expect(sanitizeChapter(wrap('<a href="javascript:alert(1)">x</a>'), opts())).not.toContain("href");
    const ext = sanitizeChapter(wrap('<a href="https://x.org">x</a>'), opts());
    expect(ext).toContain('target="_blank"');
    expect(ext).toContain('rel="noopener noreferrer"');
    const internal = sanitizeChapter(wrap('<a href="ch2.xhtml#p">x</a>'), opts());
    expect(internal).toContain('data-chapter="1"');
    expect(internal).not.toContain("href");
  });
  it("bỏ style inline", () => {
    expect(sanitizeChapter(wrap('<p style="color:red">x</p>'), opts())).toBe("<p>x</p>");
  });
});
