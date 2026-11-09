import Link from "next/link";
import { getT } from "@/lib/i18n/server";

export async function Pagination({ page, totalPages, baseHref }: { page: number; totalPages: number; baseHref: string }) {
  const t = await getT();
  if (totalPages <= 1) return null;
  const href = (p: number) => `${baseHref}${baseHref.includes("?") ? "&" : "?"}page=${p}`;
  const pages = Array.from({ length: totalPages }, (_, i) => i + 1).filter(
    (p) => p === 1 || p === totalPages || Math.abs(p - page) <= 1,
  );
  const item = "grid h-10 min-w-10 place-items-center rounded-full px-3 text-sm font-semibold transition";
  return (
    <nav aria-label={t("common.pagination")} className="mt-10 flex flex-wrap items-center justify-center gap-2">
      {page > 1 && (
        <Link href={href(page - 1)} className={`${item} bg-surface/70 hover:bg-tint`}>
          {t("common.prev")}
        </Link>
      )}
      {pages.map((p, i) => (
        <span key={p} className="contents">
          {i > 0 && p - pages[i - 1] > 1 && <span className="px-1 text-ink/50">…</span>}
          <Link
            href={href(p)}
            aria-current={p === page ? "page" : undefined}
            className={`${item} ${p === page ? "bg-brick text-white" : "bg-surface/70 hover:bg-tint"}`}
          >
            {p}
          </Link>
        </span>
      ))}
      {page < totalPages && (
        <Link href={href(page + 1)} className={`${item} bg-surface/70 hover:bg-tint`}>
          {t("common.next")}
        </Link>
      )}
    </nav>
  );
}
