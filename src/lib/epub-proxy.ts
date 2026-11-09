import { connectDB } from "@/lib/db";
import { httpUrl } from "@/lib/validation";
import { Book } from "@/models/Book";

export const MAX_EPUB_BYTES = 20 * 1024 * 1024;
export const EPUB_TIMEOUT_MS = 15_000;
const MAX_REDIRECTS = 5;

const failed = () => new Response("Không tải được sách", { status: 502 });

export async function getEpubUrl(slug: string): Promise<string | null> {
  await connectDB();
  const book = await Book.findOne({ slug, status: { $ne: "draft" } }, { epubUrl: 1 }).lean();
  return book?.epubUrl || null;
}

// Follows redirects by hand so each target passes the same check as a saved URL before the server requests it (SSRF).
// shortcut: hostnames are not resolved, so a public name pointing at a private IP still passes; resolve and check the IP if non-admins can ever set epubUrl.
async function fetchEpub(url: string, fetchImpl: typeof fetch): Promise<Response> {
  const signal = AbortSignal.timeout(EPUB_TIMEOUT_MS);
  let current = url;
  for (let hop = 0; hop <= MAX_REDIRECTS; hop++) {
    const res = await fetchImpl(current, { signal, redirect: "manual" });
    const location = res.headers.get("location");
    if (res.status < 300 || res.status >= 400 || !location) return res;
    await res.body?.cancel();
    current = new URL(location, current).href;
    if (!httpUrl.safeParse(current).success) throw new Error("EPUB_BAD_REDIRECT");
  }
  throw new Error("EPUB_TOO_MANY_REDIRECTS");
}

export type EpubCheck = { status: "ok"; bytes: number } | { status: "tooLarge" } | { status: "unreachable" };

// Admin link check: goes through the same guard as the proxy and stops reading at the size cap.
export async function checkEpub(url: string, fetchImpl: typeof fetch = fetch): Promise<EpubCheck> {
  if (!httpUrl.safeParse(url).success) return { status: "unreachable" };
  let src: Response;
  try {
    src = await fetchEpub(url, fetchImpl);
  } catch {
    return { status: "unreachable" };
  }
  if (!src.ok || !src.body) return { status: "unreachable" };
  if (Number(src.headers.get("content-length")) > MAX_EPUB_BYTES) {
    await src.body.cancel();
    return { status: "tooLarge" };
  }
  const reader = src.body.getReader();
  let bytes = 0;
  try {
    for (;;) {
      const { done, value } = await reader.read();
      if (done) return { status: "ok", bytes };
      bytes += value.byteLength;
      if (bytes > MAX_EPUB_BYTES) {
        await reader.cancel();
        return { status: "tooLarge" };
      }
    }
  } catch {
    return { status: "unreachable" };
  }
}

// Streams a stored EPUB to the browser (sources don't send CORS headers), capped in time and size.
export async function proxyEpub(url: string, fetchImpl: typeof fetch = fetch): Promise<Response> {
  let src: Response;
  try {
    src = await fetchEpub(url, fetchImpl);
  } catch {
    return failed();
  }
  if (!src.ok || !src.body || !/^https?:/.test(src.url || url)) return failed();
  if (Number(src.headers.get("content-length")) > MAX_EPUB_BYTES) {
    await src.body.cancel();
    return new Response("Sách quá lớn", { status: 413 });
  }
  let bytes = 0;
  const limit = new TransformStream<Uint8Array, Uint8Array>({
    transform(chunk, controller) {
      bytes += chunk.byteLength;
      if (bytes > MAX_EPUB_BYTES) controller.error(new Error("EPUB_TOO_LARGE"));
      else controller.enqueue(chunk);
    },
  });
  return new Response(src.body.pipeThrough(limit), {
    headers: {
      "content-type": "application/epub+zip",
      "cache-control": "public, max-age=86400, s-maxage=86400",
    },
  });
}
