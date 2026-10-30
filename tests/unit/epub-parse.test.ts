// @vitest-environment jsdom
import { describe, expect, it } from "vitest";
import { strToU8, zipSync } from "fflate";
import { makeEpub } from "../helpers/epub";
import { openEpub, resolvePath } from "@/lib/epub/parse";

const chapters = [
  { name: "ch1.xhtml", html: "<p>Một</p>", title: "Khởi đầu" },
  { name: "ch2.xhtml", html: "<p>Hai</p>", title: "Tiếp theo" },
];

describe("openEpub", () => {
  it("đọc spine và mục lục EPUB3 nav", () => {
    const b = openEpub(makeEpub({ chapters, nav: true }));
    expect(b.title).toBe("Sách thử");
    expect(b.chapters.map((c) => c.href)).toEqual(["OEBPS/Text/ch1.xhtml", "OEBPS/Text/ch2.xhtml"]);
    expect(b.toc.map((t) => [t.label, t.chapter])).toEqual([["Khởi đầu", 0], ["Tiếp theo", 1]]);
    expect(b.chapterHtml(1)).toContain("Hai");
  });
  it("mục lục EPUB2 ncx", () => {
    const b = openEpub(makeEpub({ chapters, ncx: true }));
    expect(b.toc.map((t) => t.label)).toEqual(["Khởi đầu", "Tiếp theo"]);
  });
  it("không có mục lục → Chương N", () => {
    expect(openEpub(makeEpub({ chapters })).toc.map((t) => t.label)).toEqual(["Chương 1", "Chương 2"]);
  });
  it("file hỏng → EPUB_INVALID", () => {
    expect(() => openEpub(new Uint8Array([1, 2, 3]))).toThrow("EPUB_INVALID");
    expect(() => openEpub(zipSync({ "a.txt": strToU8("x") }))).toThrow("EPUB_INVALID");
  });
  it("chapterIndexOf và resource", () => {
    const b = openEpub(makeEpub({ chapters, images: { "Images/a b.png": new Uint8Array([9]) } }));
    expect(b.chapterIndexOf("OEBPS/Text/ch2.xhtml")).toBe(1);
    expect(b.chapterIndexOf("OEBPS/Text/zzz.xhtml")).toBe(-1);
    expect(b.resource("OEBPS/Images/a b.png")).toEqual(new Uint8Array([9]));
  });
});

describe("resolvePath", () => {
  it("xử lý .., fragment, %20", () => {
    expect(resolvePath("OEBPS/Text/ch2.xhtml", "../Images/a%20b.png#x")).toBe("OEBPS/Images/a b.png");
    expect(resolvePath("OEBPS/content.opf", "Text/ch1.xhtml")).toBe("OEBPS/Text/ch1.xhtml");
    expect(resolvePath("content.opf", "ch1.xhtml")).toBe("ch1.xhtml");
  });
});
