import type { NextRequest } from "next/server";
import { getEpubUrl, proxyEpub } from "@/lib/epub-proxy";

export async function GET(_req: NextRequest, ctx: RouteContext<"/books/[slug]/epub">) {
  const url = await getEpubUrl((await ctx.params).slug);
  return url ? proxyEpub(url) : new Response("Không tìm thấy", { status: 404 });
}
