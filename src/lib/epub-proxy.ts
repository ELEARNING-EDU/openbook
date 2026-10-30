import { connectDB } from "@/lib/db";
import { Book } from "@/models/Book";

export const MAX_EPUB_BYTES = 20 * 1024 * 1024;
export const EPUB_TIMEOUT_MS = 15_000;

const failed = () => new Response("Không tải được sách", { status: 502 });

export async function getEpubUrl(slug: string): Promise<string | null> {
  await connectDB();
  const book = await Book.findOne({ slug }, { epubUrl: 1 }).lean();
  return book?.epubUrl || null;
}

// Streams a stored EPUB to the browser (sources don't send CORS headers), capped in time and size.
export async function proxyEpub(url: string, fetchImpl: typeof fetch = fetch): Promise<Response> {
  let src: Response;
  try {
    src = await fetchImpl(url, { signal: AbortSignal.timeout(EPUB_TIMEOUT_MS), redirect: "follow" });
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
