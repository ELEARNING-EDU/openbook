import { NextResponse, type NextRequest } from "next/server";
import { recordDownload } from "@/lib/book-service";

// shortcut: đếm mọi lượt bấm kể cả bot/bấm lặp, nâng cấp sang chống trùng theo IP/user khi số liệu cần chính xác
export async function GET(_req: NextRequest, ctx: RouteContext<"/books/[slug]/go/[i]">) {
  const { slug, i } = await ctx.params;
  const url = /^\d+$/.test(i) ? await recordDownload(slug, Number(i)) : null;
  if (!url) return new NextResponse("Không tìm thấy", { status: 404 });
  return NextResponse.redirect(url, 302);
}
