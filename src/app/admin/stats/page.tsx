import Link from "next/link";
import { Suspense } from "react";
import { getStatsData } from "@/lib/admin-stats";
import { assertAdminPage } from "@/lib/auth-guard";
import { localeTag } from "@/lib/i18n";
import { getLocale, getT } from "@/lib/i18n/server";
import { parseRange, parseSort, STATS_RANGES, type PeriodStat } from "@/lib/stats";

export async function generateMetadata() {
  const t = await getT();
  return { title: t("admin.navStats") };
}

type SearchParams = Promise<{ range?: string; sort?: string }>;

const RANGE_KEYS = {
  7: "admin.statsRange7",
  30: "admin.statsRange30",
  90: "admin.statsRange90",
} as const;

async function Stats({ searchParams }: { searchParams: SearchParams }) {
  const [t, locale] = await Promise.all([getT(), getLocale()]);
  await assertAdminPage();
  const sp = await searchParams;
  const range = parseRange(sp.range);
  const sort = parseSort(sp.sort);
  const data = await getStatsData(range, sort);
  const tag = localeTag(locale);
  const num = (n: number) => n.toLocaleString(tag);
  const short = (key: string) =>
    new Date(`${key}T00:00:00Z`).toLocaleDateString(tag, { day: "2-digit", month: "2-digit", timeZone: "UTC" });
  const periodLabel = (p: PeriodStat) => (p.start === p.end ? short(p.start) : `${short(p.start)} – ${short(p.end)}`);
  const charts = [
    { key: "reads" as const, title: t("admin.statsReads"), total: data.totalReads },
    { key: "downloads" as const, title: t("admin.statsDownloads"), total: data.totalDownloads },
  ];
  const empty = data.totalReads + data.totalDownloads === 0;
  const earliest = data.earliest
    ? new Date(`${data.earliest}T00:00:00Z`).toLocaleDateString(tag, {
        day: "numeric",
        month: "long",
        year: "numeric",
        timeZone: "UTC",
      })
    : null;
  const sortedBy = (key: "reads" | "downloads") =>
    sort === key ? "text-brick dark:text-orange-400" : "text-ink/80 hover:text-brick dark:hover:text-orange-400";

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">{t("admin.navStats")}</h1>
          {earliest && <p className="mt-1 text-sm text-ink/70">{t("admin.statsEarliest", { date: earliest })}</p>}
        </div>
        <nav aria-label={t("admin.statsRangeLabel")} className="flex flex-wrap gap-2">
          {STATS_RANGES.map((r) => {
            const active = r === range;
            return (
              <Link
                key={r}
                href={`/admin/stats?range=${r}&sort=${sort}`}
                aria-current={active ? "true" : undefined}
                className={`inline-flex min-h-11 items-center rounded-full px-4 font-semibold ${active ? "bg-ink text-cream" : "bg-surface ring-1 ring-ink/15 hover:bg-tint"}`}
              >
                {t(RANGE_KEYS[r])}
              </Link>
            );
          })}
        </nav>
      </div>

      {empty ? (
        <div className="rounded-2xl bg-surface/70 p-10 text-center ring-1 ring-ink/5">
          <p className="text-ink/80">{t("admin.statsNoData")}</p>
          {!earliest && <p className="mt-1 text-sm text-ink/70">{t("admin.statsNoStats")}</p>}
        </div>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2">
            {charts.map((chart) => {
              const max = Math.max(0, ...data.points.map((p) => p[chart.key]));
              return (
                <section key={chart.key} className="flex flex-col gap-3 rounded-2xl bg-surface/70 p-5 ring-1 ring-ink/5">
                  <div>
                    <h2 className="text-sm font-semibold text-ink/80">{chart.title}</h2>
                    <p className="text-4xl font-bold tabular-nums">{num(chart.total)}</p>
                  </div>
                  <div aria-hidden className="flex h-40 items-stretch gap-0.5 border-b border-ink/15">
                    {data.points.map((p) => {
                      const value = p[chart.key];
                      const pct = max === 0 ? 0 : Math.round((value / max) * 100);
                      return (
                        <div
                          key={p.start}
                          title={t("admin.statsBarTitle", { date: periodLabel(p), value: num(value) })}
                          className="flex min-w-0 flex-1 items-end"
                        >
                          <div className="w-full rounded-t bg-brick" style={{ height: `${pct}%` }} />
                        </div>
                      );
                    })}
                  </div>
                  <div aria-hidden className="flex justify-between gap-2 text-xs text-ink/70">
                    <span>{short(data.start)}</span>
                    <span>{short(data.end)}</span>
                  </div>
                </section>
              );
            })}
          </div>

          <details className="rounded-2xl bg-surface/70 px-5 ring-1 ring-ink/5">
            <summary className="flex min-h-11 cursor-pointer items-center font-semibold">{t("admin.statsTableToggle")}</summary>
            <div className="max-h-64 overflow-auto pb-4">
              <table className="w-full text-left text-sm tabular-nums">
                <thead className="text-xs text-ink/80">
                  <tr>
                    <th scope="col" className="py-2 font-semibold">
                      {range === 90 ? t("admin.statsPeriodWeek") : t("admin.statsPeriodDay")}
                    </th>
                    <th scope="col" className="py-2 text-right font-semibold">{t("admin.statsReads")}</th>
                    <th scope="col" className="py-2 text-right font-semibold">{t("admin.statsDownloads")}</th>
                  </tr>
                </thead>
                <tbody>
                  {[...data.points].reverse().map((p) => (
                    <tr key={p.start} className="border-t border-ink/5">
                      <td className="py-1.5">{periodLabel(p)}</td>
                      <td className="py-1.5 text-right">{num(p.reads)}</td>
                      <td className="py-1.5 text-right">{num(p.downloads)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </details>

          <div className="flex flex-wrap items-start gap-4">
            <section aria-labelledby="top-books" className="min-w-0 flex-[3_1_480px] overflow-hidden rounded-2xl bg-surface/70 ring-1 ring-ink/5">
              <h2 id="top-books" className="px-5 pt-5 pb-3 text-2xl font-bold">{t("admin.statsByBook")}</h2>
              {data.books.length === 0 ? (
                <p className="px-5 pb-5 text-ink/70">{t("admin.statsNoBooks")}</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm tabular-nums">
                    <thead className="bg-tint/60 text-xs">
                      <tr>
                        <th scope="col" className="px-5 py-1 font-semibold">{t("admin.colBook")}</th>
                        <th scope="col" aria-sort={sort === "reads" ? "descending" : "none"} className="px-1 py-1 text-right">
                          <Link href={`/admin/stats?range=${range}&sort=reads`} className={`inline-flex min-h-11 items-center px-2 font-semibold ${sortedBy("reads")}`}>
                            {t("admin.statsReads")}
                          </Link>
                        </th>
                        <th scope="col" aria-sort={sort === "downloads" ? "descending" : "none"} className="px-4 py-1 text-right">
                          <Link href={`/admin/stats?range=${range}&sort=downloads`} className={`inline-flex min-h-11 items-center px-2 font-semibold ${sortedBy("downloads")}`}>
                            {t("admin.statsDownloads")}
                          </Link>
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-ink/5">
                      {data.books.map((b) => (
                        <tr key={b.id} className="hover:bg-tint/40">
                          <td className="px-5 py-2.5">
                            <Link href={`/admin/books/${b.id}`} className="font-semibold hover:text-brick dark:hover:text-orange-400">
                              {b.title}
                            </Link>
                          </td>
                          <td className="px-3 py-2.5 text-right">{num(b.reads)}</td>
                          <td className="px-5 py-2.5 text-right">{num(b.downloads)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <section aria-labelledby="by-cat" className="min-w-0 flex-[2_1_320px] rounded-2xl bg-surface/70 p-5 ring-1 ring-ink/5">
              <h2 id="by-cat" className="mb-4 text-2xl font-bold">
                {sort === "reads" ? t("admin.statsByCategoryReads") : t("admin.statsByCategoryDownloads")}
              </h2>
              {data.categories.length === 0 ? (
                <p className="text-ink/70">{t("admin.statsNoCategories")}</p>
              ) : (
                <ul className="flex flex-col gap-4">
                  {data.categories.map((c) => {
                    const top = data.categories[0].value;
                    const pct = top === 0 ? 0 : Math.round((c.value / top) * 100);
                    return (
                      <li key={c.id}>
                        <div className="flex justify-between gap-2">
                          <span>{c.name}</span>
                          <span className="font-semibold tabular-nums">{num(c.value)}</span>
                        </div>
                        <div aria-hidden className="mt-1 h-2">
                          <div className="h-2 rounded-r bg-brick" style={{ width: `${pct}%` }} />
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>
          </div>
        </>
      )}
    </div>
  );
}

export default async function AdminStatsPage({ searchParams }: { searchParams: SearchParams }) {
  const t = await getT();
  return (
    <Suspense fallback={<p className="text-ink/60">{t("common.loading")}</p>}>
      <Stats searchParams={searchParams} />
    </Suspense>
  );
}
