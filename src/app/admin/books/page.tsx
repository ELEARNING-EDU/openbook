import Form from "next/form";
import Link from "next/link";
import { Suspense } from "react";
import { AdminBooksTable } from "@/components/admin/AdminBooksTable";
import { Pagination } from "@/components/Pagination";
import { assertAdminPage } from "@/lib/auth-guard";
import { isObjectId } from "@/lib/book-service";
import { getLocale, getT } from "@/lib/i18n/server";
import { localeTag } from "@/lib/i18n";
import { listAdminBooks, listAdminCategories, type BookListStatus } from "@/lib/queries";
import { parsePage } from "@/lib/validation";

export async function generateMetadata() {
  const t = await getT();
  return { title: t("admin.navBooks") };
}

const one = (value: string | string[] | undefined) => (typeof value === "string" ? value : undefined);
const parseStatus = (value: string | undefined): BookListStatus =>
  value === "published" || value === "draft" || value === "issue" ? value : "all";
const parseLanguage = (value: string | undefined) => (value === "vi" || value === "en" ? value : "");

const TABS: { id: BookListStatus; key: "admin.tabAll" | "admin.tabPublished" | "admin.tabDraft" | "admin.tabIssue" }[] = [
  { id: "all", key: "admin.tabAll" },
  { id: "published", key: "admin.tabPublished" },
  { id: "draft", key: "admin.tabDraft" },
  { id: "issue", key: "admin.tabIssue" },
];

async function BookList({ searchParams }: { searchParams: PageProps<"/admin/books">["searchParams"] }) {
  const [t, locale] = await Promise.all([getT(), getLocale()]);
  await assertAdminPage();
  const sp = await searchParams;
  const q = one(sp.q)?.trim() ?? "";
  const categoryRaw = one(sp.category) ?? "";
  const category = isObjectId(categoryRaw) ? categoryRaw : "";
  const language = parseLanguage(one(sp.language));
  const status = parseStatus(one(sp.status));
  const page = parsePage(sp.page);

  const [{ books, total, totalPages, counts }, categories] = await Promise.all([
    listAdminBooks({ q, category, language, status, page }),
    listAdminCategories(),
  ]);

  const href = (patch: Partial<{ q: string; category: string; language: string; status: BookListStatus }> = {}) => {
    const next = { q, category, language, status, ...patch };
    const params = new URLSearchParams();
    if (next.q) params.set("q", next.q);
    if (next.category) params.set("category", next.category);
    if (next.language) params.set("language", next.language);
    if (next.status !== "all") params.set("status", next.status);
    const query = params.toString();
    return query ? `/admin/books?${query}` : "/admin/books";
  };

  return (
    <div className="space-y-4">
      <nav aria-label={t("admin.colStatus")} className="flex flex-wrap gap-2">
        {TABS.map((tab) => {
          const active = tab.id === status;
          return (
            <Link
              key={tab.id}
              href={href({ status: tab.id })}
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

      <Form action="/admin/books" className="flex flex-wrap items-end gap-3">
        {status !== "all" && <input type="hidden" name="status" value={status} />}
        <label className="min-w-0 flex-1 basis-72 text-sm font-semibold text-ink/70">
          {t("admin.searchBooksLabel")}
          <input
            key={`q-${q}`}
            name="q"
            type="search"
            defaultValue={q}
            placeholder={t("admin.searchBooksPlaceholder")}
            className="mt-1 block h-11 w-full rounded-xl border border-ink/15 bg-surface px-3.5 text-sm font-normal text-ink outline-none focus:border-brick focus:ring-4 focus:ring-brick/15"
          />
        </label>
        <label className="w-full text-sm font-semibold text-ink/70 sm:w-52">
          {t("admin.colCategory")}
          <select key={`category-${category}`} name="category" defaultValue={category} className="mt-1 block h-11 w-full rounded-xl border border-ink/15 bg-surface px-3 text-sm font-normal text-ink">
            <option value="">{t("common.all")}</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </label>
        <label className="w-full text-sm font-semibold text-ink/70 sm:w-44">
          {t("admin.filterLanguage")}
          <select key={`language-${language}`} name="language" defaultValue={language} className="mt-1 block h-11 w-full rounded-xl border border-ink/15 bg-surface px-3 text-sm font-normal text-ink">
            <option value="">{t("common.all")}</option>
            <option value="vi">{t("admin.langVi")}</option>
            <option value="en">{t("admin.langEn")}</option>
          </select>
        </label>
        <button type="submit" className="min-h-11 rounded-full bg-ink px-5 text-sm font-semibold text-cream">
          {t("common.search")}
        </button>
      </Form>

      <p className="text-sm text-ink/60">{t("common.booksCount", { n: total })}</p>

      {books.length === 0 ? (
        <p className="rounded-2xl bg-surface/60 p-10 text-center text-ink/70">{t("admin.noBooksFiltered")}</p>
      ) : (
        <AdminBooksTable
          key={`${status}|${q}|${category}|${language}|${page}`}
          books={books}
          categories={categories}
          localeTag={localeTag(locale)}
        />
      )}

      <Pagination page={page} totalPages={totalPages} baseHref={href()} />
    </div>
  );
}

export default async function AdminBooksPage(props: PageProps<"/admin/books">) {
  const t = await getT();
  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-bold">{t("admin.navBooks")}</h1>
        <Link href="/admin/books/new" className="rounded-full bg-brick px-5 py-2.5 font-semibold text-white shadow hover:bg-orange-700">
          {t("admin.addBook")}
        </Link>
      </div>
      <Suspense fallback={<p className="text-ink/60">{t("common.loading")}</p>}>
        <BookList searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}
