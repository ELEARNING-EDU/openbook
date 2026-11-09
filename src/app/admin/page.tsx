import Link from "next/link";
import { Suspense } from "react";
import { getAdminOverview } from "@/lib/admin-stats";
import { assertAdminPage } from "@/lib/auth-guard";
import { localeTag } from "@/lib/i18n";
import { getLocale, getT } from "@/lib/i18n/server";
import { listAuthorSuggestions } from "@/lib/queries";

export async function generateMetadata() {
  const t = await getT();
  return { title: t("admin.navOverview") };
}

const tile = "rounded-2xl bg-surface/70 p-5 ring-1 ring-ink/5";

async function Overview() {
  const [t, locale] = await Promise.all([getT(), getLocale()]);
  await assertAdminPage();
  const data = await getAdminOverview();
  const suggestions = await listAuthorSuggestions();
  const taskCount = data.tasks.length + suggestions.length;
  const tag = localeTag(locale);
  const num = (n: number) => n.toLocaleString(tag);
  const tiles = [
    { href: "/admin/stats?range=30", value: data.reads30, label: t("admin.overviewReads") },
    { href: "/admin/stats?range=30&sort=downloads", value: data.downloads30, label: t("admin.overviewDownloads") },
    { href: "/admin/books", value: data.books, label: t("admin.navBooks") },
    { href: "/admin/authors", value: data.authors, label: t("admin.overviewAuthors") },
    { href: "/admin/categories", value: data.categories, label: t("admin.navCategories") },
    { href: "/admin/reviews", value: data.reviews7, label: t("admin.overviewReviews") },
  ];

  return (
    <div className="flex flex-col gap-7">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {tiles.map((item) => {
          const body = (
            <>
              <span className="block text-3xl font-bold tabular-nums">{num(item.value)}</span>
              <span className="text-sm text-ink/70">{item.label}</span>
            </>
          );
          return item.href ? (
            <Link key={item.label} href={item.href} className={`${tile} transition hover:bg-tint/40`}>
              {body}
            </Link>
          ) : (
            <div key={item.label} className={tile}>
              {body}
            </div>
          );
        })}
      </div>

      <section aria-labelledby="todo" className="flex flex-col gap-3">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 id="todo" className="text-2xl font-bold">{t("admin.todo")}</h2>
          <span className="text-ink/70">{t("admin.todoCount", { n: taskCount })}</span>
        </div>
        <div className="overflow-hidden rounded-2xl bg-surface/70 ring-1 ring-ink/5">
          {taskCount === 0 ? (
            <p className="p-10 text-center text-ink/70">{t("admin.todoEmpty")}</p>
          ) : (
            <ul className="divide-y divide-ink/5">
              {data.tasks.map((task) => (
                <li key={`${task.id}-${task.issue}`} className="flex flex-wrap items-center gap-3 px-5 py-3.5">
                  <span className="min-w-[88px] rounded-full bg-tint px-2.5 py-0.5 text-center text-xs font-semibold">{t(task.issue)}</span>
                  <p className="min-w-0 flex-1 font-semibold">{task.title}</p>
                  <Link href={`/admin/books/${task.id}`} className="inline-flex min-h-11 items-center px-2 font-semibold text-brick hover:underline dark:text-orange-400">
                    {t("common.edit")}
                  </Link>
                </li>
              ))}
              {suggestions.map((suggestion) => (
                <li key={`${suggestion.a.id}-${suggestion.b.id}`} className="flex flex-wrap items-center gap-3 px-5 py-3.5">
                  <span className="min-w-[88px] rounded-full bg-tint px-2.5 py-0.5 text-center text-xs font-semibold">{t("admin.issueDuplicateAuthor")}</span>
                  <p className="min-w-0 flex-1 font-semibold">{t("admin.dupSuggestion", { a: suggestion.a.name, b: suggestion.b.name })}</p>
                  <Link
                    href={`/admin/authors?id=${suggestion.a.id}&merge=${suggestion.b.id}`}
                    className="inline-flex min-h-11 items-center px-2 font-semibold text-brick hover:underline dark:text-orange-400"
                  >
                    {t("admin.mergeOpen")}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section aria-labelledby="recent" className="flex flex-col gap-3">
        <h2 id="recent" className="text-2xl font-bold">{t("admin.recentlyEdited")}</h2>
        <div className="overflow-hidden rounded-2xl bg-surface/70 ring-1 ring-ink/5">
          {data.recent.length === 0 ? (
            <p className="p-10 text-center text-ink/70">{t("admin.recentEmpty")}</p>
          ) : (
            <ul className="divide-y divide-ink/5">
              {data.recent.map((book) => (
                <li key={book.id}>
                  <Link href={`/admin/books/${book.id}`} className="flex flex-wrap justify-between gap-x-4 gap-y-1 px-5 py-3.5 hover:bg-tint/40">
                    <span className="font-semibold">{book.title}</span>
                    <span className="text-ink/70">
                      {new Date(book.updatedAt).toLocaleDateString(tag, { day: "numeric", month: "short", year: "numeric", timeZone: "Asia/Ho_Chi_Minh" })}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}

export default async function AdminOverviewPage() {
  const t = await getT();
  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-bold">{t("admin.navOverview")}</h1>
        <Link href="/admin/books/new" className="rounded-full bg-brick px-5 py-2.5 font-semibold text-white shadow hover:bg-orange-700">
          {t("admin.addBook")}
        </Link>
      </div>
      <Suspense fallback={<p className="text-ink/60">{t("common.loading")}</p>}>
        <Overview />
      </Suspense>
    </div>
  );
}
