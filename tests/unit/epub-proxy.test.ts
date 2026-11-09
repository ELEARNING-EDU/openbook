import { describe, expect, it, vi } from "vitest";
import { link, setupTestDB } from "../helpers/mongo";
import { Book } from "@/models/Book";
import { getEpubUrl, MAX_EPUB_BYTES, checkEpub, proxyEpub } from "@/lib/epub-proxy";

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

describe("proxyEpub redirect", () => {
  const redirect = (location: string) => new Response(null, { status: 302, headers: { location } });
  const sequence = (...responses: Response[]) => {
    const requested: string[] = [];
    const fetchImpl = (async (url: string) => {
      requested.push(url);
      return responses.shift() ?? redirect("https://a.org/again");
    }) as unknown as typeof fetch;
    return { requested, fetchImpl };
  };

  it("theo redirect hợp lệ, kể cả đường dẫn tương đối", async () => {
    const { requested, fetchImpl } = sequence(redirect("https://b.org/y.epub"), redirect("/z.epub"), new Response("ok"));
    const r = await proxyEpub("https://a.org/x.epub", fetchImpl);
    expect(r.status).toBe(200);
    expect(requested).toEqual(["https://a.org/x.epub", "https://b.org/y.epub", "https://b.org/z.epub"]);
  });
  it.each(["http://127.0.0.1/x", "http://169.254.169.254/latest", "http://localhost:3000/x", "http://[::1]/x", "ftp://a.org/x"])(
    "chặn redirect tới %s mà không gọi tới đó",
    async (target) => {
      const { requested, fetchImpl } = sequence(redirect(target));
      expect((await proxyEpub("https://a.org/x.epub", fetchImpl)).status).toBe(502);
      expect(requested).toEqual(["https://a.org/x.epub"]);
    },
  );
  it("quá nhiều redirect → 502", async () => {
    const { requested, fetchImpl } = sequence();
    expect((await proxyEpub("https://a.org/x.epub", fetchImpl)).status).toBe(502);
    expect(requested).toHaveLength(6);
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
  it("bản nháp không lộ URL EPUB", async () => {
    await Book.create({ title: "Nháp", slug: "nhap", author: "X", links: link(), status: "draft", epubUrl: "https://a.org/d.epub" });
    expect(await getEpubUrl("nhap")).toBeNull();
  });
});

describe("checkEpub", () => {
  const called = () => {
    const urls: string[] = [];
    const fetchImpl = (async (url: string) => {
      urls.push(url);
      return withUrl(new Response(new Uint8Array([1, 2, 3])), "https://a.org/x.epub");
    }) as unknown as typeof fetch;
    return { urls, fetchImpl };
  };

  it("báo dung lượng khi mở được", async () => {
    const { fetchImpl } = called();
    expect(await checkEpub("https://a.org/x.epub", fetchImpl)).toEqual({ status: "ok", bytes: 3 });
  });

  it("báo quá lớn theo Content-Length và theo luồng", async () => {
    const big = withUrl(new Response("x", { headers: { "content-length": String(MAX_EPUB_BYTES + 1) } }), "https://a.org/x.epub");
    expect(await checkEpub("https://a.org/x.epub", fakeFetch(big))).toEqual({ status: "tooLarge" });
    const chunk = new Uint8Array(1024 * 1024);
    let sent = 0;
    const body = new ReadableStream({
      pull(c) {
        if (sent++ > 21) c.close();
        else c.enqueue(chunk);
      },
    });
    expect(await checkEpub("https://a.org/x.epub", fakeFetch(withUrl(new Response(body), "https://a.org/x.epub")))).toEqual({ status: "tooLarge" });
  });

  it("báo không mở được khi lỗi, và không gọi mạng với địa chỉ riêng", async () => {
    expect(await checkEpub("https://a.org/x.epub", fakeFetch(withUrl(new Response("no", { status: 500 }), "https://a.org/x.epub")))).toEqual({ status: "unreachable" });
    expect(await checkEpub("https://a.org/x.epub", fakeFetch(() => { throw new DOMException("t", "TimeoutError"); }))).toEqual({ status: "unreachable" });
    const spy = vi.fn();
    for (const url of ["http://127.0.0.1/x.epub", "http://192.168.0.1/x.epub", "http://localhost/x.epub", "http://[::1]/x.epub"]) {
      expect(await checkEpub(url, spy as unknown as typeof fetch)).toEqual({ status: "unreachable" });
    }
    expect(spy).not.toHaveBeenCalled();
  });
});
