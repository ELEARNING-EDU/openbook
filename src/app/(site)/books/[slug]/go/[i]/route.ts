import { NextResponse, type NextRequest } from "next/server";
import { recordDownload } from "@/lib/book-service";
import { clientKey, rateLimit } from "@/lib/rate-limit";

// shortcut: đếm mọi lượt bấm kể cả bot/bấm lặp, nâng cấp sang chống trùng theo IP/user khi số liệu cần chính xác
export async function GET(req: NextRequest, ctx: RouteContext<"/books/[slug]/go/[i]">) {
  if (!rateLimit(`go:${clientKey(req.headers)}`, 60, 60_000)) {
    return new NextResponse("Quá nhiều yêu cầu", { status: 429 });
  }
  const { slug, i } = await ctx.params;
  const url = /^\d+$/.test(i) ? await recordDownload(slug, Number(i)) : null;
  if (!url) return new NextResponse("Không tìm thấy", { status: 404 });
  return NextResponse.redirect(url, 302);
}
