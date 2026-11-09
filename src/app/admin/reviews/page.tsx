import Link from "next/link";
import { Suspense } from "react";
import { deleteReview, setReviewHidden } from "@/actions/reviews";
import { ConfirmButton } from "@/components/admin/ConfirmButton";
import { Stars } from "@/components/Stars";
import { assertAdminPage } from "@/lib/auth-guard";
import { getLocale, getT } from "@/lib/i18n/server";
import { localeTag } from "@/lib/i18n";
import { listAdminReviews, type AdminReviewFilter } from "@/lib/queries";

export async function generateMetadata() {
  const t = await getT();
  return { title: t("admin.navReviews") };
}

const one = (value: string | string[] | undefined) => (typeof value === "string" ? value : undefined);
const parseFilter = (value: string | undefined): AdminReviewFilter =>
  value === "low" || value === "hidden" ? value : "all";

async function Reviews({ searchParams }: { searchParams: PageProps<"/admin/reviews">["searchParams"] }) {
  const [t, locale] = await Promise.all([getT(), getLocale()]);
  await assertAdminPage();
  const filter = parseFilter(one((await searchParams).filter));
  const { reviews, counts } = await listAdminReviews({ filter });
  const tabs: { id: AdminReviewFilter; key: "admin.tabAll" | "admin.filterLow" | "admin.filterHidden" }[] = [
    { id: "all", key: "admin.tabAll" },
    { id: "low", key: "admin.filterLow" },
    { id: "hidden", key: "admin.filterHidden" },
  ];

  return (
    <>
      <nav aria-label={t("admin.navReviews")} className="mb-4 flex flex-wrap gap-2">
        {tabs.map((tab) => {
          const active = tab.id === filter;
          return (
            <Link
              key={tab.id}
              href={tab.id === "all" ? "/admin/reviews" : `/admin/reviews?filter=${tab.id}`}
              aria-current={active ? "page" : undefined}
              className={`inline-flex min-h-11 items-center rounded-full border px-4 text-sm font-semibold transition ${
                active ? "border-ink bg-ink text-cream" : "border-ink/20 bg-surface hover:bg-tint"
              }`}
            >
              {t(tab.key)} ({counts[tab.id]})
            </Link>
          );
        })}
      </nav>
      {reviews.length === 0 ? (
        <p className="text-ink/60">{t("admin.noReviews")}</p>
      ) : (
        <ul className="space-y-3">
          {reviews.map((r) => (
            <li key={r.id} className={`rounded-2xl p-4 ring-1 ring-ink/5 ${r.hidden ? "bg-ink/5 opacity-75" : "bg-surface/70"}`}>
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
                <span className="font-semibold">{r.userName}</span>
                <Stars value={r.rating} />
                {r.bookSlug ? (
                  <Link href={`/books/${r.bookSlug}`} className="text-brick hover:underline dark:text-orange-400">{r.bookTitle}</Link>
                ) : (
                  <span className="text-ink/50">{t("admin.deletedBook")}</span>
                )}
                <span className="text-xs text-ink/50">{new Date(r.createdAt).toLocaleString(localeTag(locale))}</span>
                {r.hidden && <span className="rounded-full bg-ink/10 px-2 py-0.5 text-xs font-semibold">{t("admin.hiddenBadge")}</span>}
              </div>
              {r.comment && <p className="mt-2 whitespace-pre-line text-ink/85">{r.comment}</p>}
              <div className="mt-3 flex gap-1">
                <ConfirmButton
                  action={setReviewHidden.bind(null, r.id, !r.hidden)}
                  label={r.hidden ? t("admin.show") : t("admin.hide")}
                  successText={r.hidden ? t("admin.reviewShown") : t("admin.reviewHidden")}
                  className="hover:bg-tint"
                />
                <ConfirmButton action={deleteReview.bind(null, r.id)} label={t("common.delete")} confirmText={t("admin.deleteReviewConfirm")} successText={t("admin.deleted")} className="text-red-700 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-950/40" />
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

export default async function AdminReviewsPage(props: PageProps<"/admin/reviews">) {
  const t = await getT();
  return (
    <div>
      <h1 className="mb-2 text-3xl font-bold">{t("admin.navReviews")}</h1>
      <p className="mb-4 text-sm text-ink/60">{t("admin.latest100")}</p>
      <Suspense fallback={<p className="text-ink/60">{t("common.loading")}</p>}>
        <Reviews searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}
