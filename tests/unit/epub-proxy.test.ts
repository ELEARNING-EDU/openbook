import { describe, expect, it } from "vitest";
import { link, setupTestDB } from "../helpers/mongo";
import { Book } from "@/models/Book";
import { getEpubUrl, MAX_EPUB_BYTES, proxyEpub } from "@/lib/epub-proxy";

setupTestDB();

const fakeFetch = (res: Response | (() => never)) =>
  (async () => (typeof res === "function" ? res() : res)) as unknown as typeof fetch;
const withUrl = (res: Response, url: string) => (Object.defineProperty(res, "url", { value: url }), res);

describe("proxyEpub", () => {
  it("stream 200 với header đúng", async () => {
    const r = await proxyEpub("https://a.org/x.epub", fakeFetch(withUrl(new Response(new Uint8Array([1, 2, 3])), "https://a.org/x.epub")));
    expect(r.status).toBe(200);
    expect(r.headers.get("content-type")).toBe("application/epub+zip");
    expect(r.headers.get("cache-control")).toBe("public, max-age=86400, s-maxage=86400");
    expect(new Uint8Array(await r.arrayBuffer())).toEqual(new Uint8Array([1, 2, 3]));
  });
  it("Content-Length quá lớn → 413", async () => {
    const src = withUrl(new Response("x", { headers: { "content-length": "30000000" } }), "https://a.org/x.epub");
    expect((await proxyEpub("https://a.org/x.epub", fakeFetch(src))).status).toBe(413);
  });
  it("stream vượt 20MB không có Content-Length → body lỗi", async () => {
    const chunk = new Uint8Array(1024 * 1024);
    let sent = 0;
    const body = new ReadableStream({
      pull(c) {
        if (sent++ > 21) c.close();
        else c.enqueue(chunk);
      },
    });
    const r = await proxyEpub("https://a.org/x.epub", fakeFetch(withUrl(new Response(body), "https://a.org/x.epub")));
    await expect(r.arrayBuffer()).rejects.toThrow();
    expect(MAX_EPUB_BYTES).toBe(20 * 1024 * 1024);
  });
  it("nguồn lỗi / throw / redirect sai giao thức → 502", async () => {
    const r1 = await proxyEpub("https://a.org/x", fakeFetch(withUrl(new Response("no", { status: 500 }), "https://a.org/x")));
    expect(r1.status).toBe(502);
    expect(await r1.text()).toBe("Không tải được sách");
    const r2 = await proxyEpub("https://a.org/x", fakeFetch(() => { throw new DOMException("t", "TimeoutError"); }));
    expect(r2.status).toBe(502);
    const r3 = await proxyEpub("https://a.org/x", fakeFetch(withUrl(new Response("x"), "ftp://evil/x")));
    expect(r3.status).toBe(502);
  });
});

describe("getEpubUrl", () => {
  it("trả URL hoặc null", async () => {
    await Book.create({ title: "A", slug: "a", author: "X", links: link(), epubUrl: "https://a.org/a.epub" });
    await Book.create({ title: "B", slug: "b", author: "X", links: link() });
    expect(await getEpubUrl("a")).toBe("https://a.org/a.epub");
    expect(await getEpubUrl("b")).toBeNull();
    expect(await getEpubUrl("khong-co")).toBeNull();
  });
});
