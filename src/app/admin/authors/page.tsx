import Form from "next/form";
import Link from "next/link";
import { Suspense } from "react";
import { AuthorForm } from "@/components/admin/AuthorForm";
import { AuthorMergePanel } from "@/components/admin/AuthorMergePanel";
import { Pagination } from "@/components/Pagination";
import { assertAdminPage } from "@/lib/auth-guard";
import { getT } from "@/lib/i18n/server";
import {
  countBooksWithoutAuthor,
  getAuthorById,
  listAuthorOptions,
  listAuthorSuggestions,
  listAuthors,
} from "@/lib/queries";
import { parsePage } from "@/lib/validation";

export async function generateMetadata() {
  const t = await getT();
  return { title: t("admin.navAuthors") };
}

type SearchParams = Record<string, string | string[] | undefined>;

const one = (value: string | string[] | undefined) => (typeof value === "string" ? value : undefined);

async function AuthorsContent({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const t = await getT();
  await assertAdminPage();
  const sp = await searchParams;
  const q = one(sp.q)?.trim() ?? "";
  const page = parsePage(sp.page);
  const id = one(sp.id);
  const mergeId = one(sp.merge);
  const isNew = one(sp.new) === "1";

  const [list, suggestions, withoutAuthor, options, detail] = await Promise.all([
    listAuthors({ q, page }),
    listAuthorSuggestions(),
    countBooksWithoutAuthor(),
    listAuthorOptions(),
    id ? getAuthorById(id) : Promise.resolve(null),
  ]);
  const mergeOther = id && mergeId && mergeId !== id ? await getAuthorById(mergeId) : null;
  const baseHref = q ? `/admin/authors?q=${encodeURIComponent(q)}` : "/admin/authors";
  const hrefFor = (authorId: string, merge?: string) => {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    params.set("id", authorId);
    if (merge) params.set("merge", merge);
    return `/admin/authors?${params.toString()}`;
  };

  return (
    <div className="flex flex-col gap-5">
      {withoutAuthor > 0 && (
        <p className="rounded-2xl bg-tint/70 px-5 py-3 text-sm">
          <strong>{t("admin.booksWithoutAuthor", { n: withoutAuthor })}</strong> {t("admin.booksWithoutAuthorNote")}
        </p>
      )}

      {suggestions.map((suggestion) => (
        <div key={`${suggestion.a.id}:${suggestion.b.id}`} className="flex flex-wrap items-center gap-2 rounded-2xl bg-tint px-5 py-3">
          <span className="min-w-0 flex-1 font-medium text-orange-950 dark:text-orange-100">
            {t("admin.dupSuggestion", { a: suggestion.a.name, b: suggestion.b.name })}
          </span>
          <Link
            href={hrefFor(suggestion.a.id, suggestion.b.id)}
            className="inline-flex min-h-11 items-center rounded-full bg-ink px-4 text-sm font-semibold text-cream hover:bg-ink/80"
          >
            {t("admin.mergeOpen")}
          </Link>
        </div>
      ))}

      {detail && mergeOther && (
        <AuthorMergePanel key={`${detail.id}:${mergeOther.id}`} initialKeep={detail} other={mergeOther} />
      )}

      {/* On phones the open panel comes first, otherwise selecting an author appears to do nothing. */}
      <div className={`flex gap-4 lg:flex-row lg:items-start ${detail || isNew ? "flex-col-reverse" : "flex-col"}`}>
        <div className="flex min-w-0 flex-col gap-3 lg:flex-[3]">
          <Form action="/admin/authors" className="flex flex-col gap-1">
            <label htmlFor="author-q" className="text-sm font-semibold text-ink/70">{t("admin.searchAuthors")}</label>
            <div className="flex gap-2">
              <input
                id="author-q"
                name="q"
                type="search"
                defaultValue={q}
                placeholder={t("admin.authorSearchPlaceholder")}
                className="h-11 min-w-0 flex-1 rounded-xl border border-ink/15 bg-surface px-4 text-sm outline-none focus:border-brick focus:ring-4 focus:ring-brick/15"
              />
              <button type="submit" className="min-h-11 rounded-full bg-ink px-4 text-sm font-semibold text-cream">{t("common.search")}</button>
            </div>
          </Form>

          <p className="text-sm text-ink/60">{t("admin.authorCount", { n: list.total })}</p>
          {list.authors.length === 0 ? (
            <p className="rounded-2xl bg-surface/60 p-10 text-center text-ink/70">{t("admin.noAuthors")}</p>
          ) : (
            <ul className="flex flex-col overflow-hidden rounded-2xl bg-surface/70 ring-1 ring-ink/5">
              {list.authors.map((author) => {
                const selected = author.id === id;
                return (
                  <li key={author.id}>
                    <Link
                      href={hrefFor(author.id)}
                      aria-current={selected ? "true" : undefined}
                      className={`flex min-h-14 items-center justify-between gap-3 border-b border-ink/5 px-5 py-2 last:border-b-0 ${selected ? "bg-tint/70" : "hover:bg-tint/40"}`}
                    >
                      <span className="flex min-w-0 flex-col">
                        <span className="font-semibold">{author.name}</span>
                        <span className="text-sm text-ink/60">{author.lifespan || "—"}</span>
                      </span>
                      <span className="shrink-0 text-sm text-ink/70">{t("common.booksCount", { n: author.bookCount })}</span>
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
          <Pagination page={page} totalPages={list.totalPages} baseHref={baseHref} />
        </div>

        <div className="min-w-0 lg:flex-[2]">
          {isNew ? (
            <AuthorForm key="new" options={options} />
          ) : detail ? (
            // Keyed on content: after a merge the form must not keep (and later save back) stale values.
            <AuthorForm key={[detail.id, detail.name, detail.lifespan, detail.bio, ...detail.aliases].join("|")} author={detail} options={options} />
          ) : (
            <p className="rounded-2xl bg-surface/60 p-5 text-ink/70">
              {id ? t("admin.authorNotFound") : t("admin.authorPickPrompt")}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

export default async function AdminAuthorsPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const t = await getT();
  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-bold">{t("admin.navAuthors")}</h1>
        <Link href="/admin/authors?new=1" className="rounded-full bg-brick px-5 py-2.5 font-semibold text-white shadow hover:bg-orange-700">
          {t("admin.addAuthor")}
        </Link>
      </div>
      <Suspense fallback={<p className="text-ink/60">{t("common.loading")}</p>}>
        <AuthorsContent searchParams={searchParams} />
      </Suspense>
    </div>
  );
}
