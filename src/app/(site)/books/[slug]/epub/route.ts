import type { NextRequest } from "next/server";
import { getEpubUrl, proxyEpub } from "@/lib/epub-proxy";
import { clientKey, rateLimit } from "@/lib/rate-limit";

export async function GET(req: NextRequest, ctx: RouteContext<"/books/[slug]/epub">) {
  if (!rateLimit(`epub:${clientKey(req.headers)}`, 30, 60_000)) {
    return new Response("Quá nhiều yêu cầu", { status: 429 });
  }
  const url = await getEpubUrl((await ctx.params).slug);
  return url ? proxyEpub(url) : new Response("Không tìm thấy", { status: 404 });
}
