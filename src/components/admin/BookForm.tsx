"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { checkEpubLink, deleteBook, saveBook } from "@/actions/books";
import { Cover } from "@/components/Cover";
import { ConfirmButton } from "@/components/admin/ConfirmButton";
import { useToast } from "@/components/admin/Toast";
import { useLocale, useT } from "@/components/i18n/LocaleProvider";
import { localeTag } from "@/lib/i18n";
import type { AdminBookDTO, AuthorOption, CategoryDTO, NamedTermDTO } from "@/lib/queries";
import type { BookStatus, ContributorRole } from "@/models/Book";

const input =
  "w-full rounded-xl border border-ink/15 bg-surface px-3.5 py-2.5 outline-none transition focus:border-brick focus:ring-4 focus:ring-brick/15";
const section = "flex flex-col gap-4 rounded-2xl bg-surface/70 p-5 ring-1 ring-ink/5";
const secondaryButton =
  "min-h-11 rounded-full border border-ink/15 bg-surface px-4 text-sm font-semibold transition hover:bg-tint disabled:opacity-50";

const ROLES: { value: ContributorRole; key: "form.roleTranslator" | "form.roleEditor" | "form.roleIllustrator" }[] = [
  { value: "translator", key: "form.roleTranslator" },
  { value: "editor", key: "form.roleEditor" },
  { value: "illustrator", key: "form.roleIllustrator" },
];

function FieldError({ errors }: { errors?: string[] }) {
  return errors?.[0] ? <p className="mt-1 text-sm text-red-700 dark:text-red-300">{errors[0]}</p> : null;
}

function formatSize(bytes: number) {
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function CheckItem({ ok, label }: { ok: boolean; label: string }) {
  return (
    <li className={`flex items-center gap-2 ${ok ? "text-moss dark:text-lime-400" : "text-red-700 dark:text-red-300"}`}>
      <span aria-hidden className="text-sm font-bold">
        {ok ? "✓" : "✗"}
      </span>
      {label}
    </li>
  );
}

export function BookForm({
  book,
  categories,
  authors,
  series: seriesOptions,
  publishers,
  stats,
}: {
  book?: AdminBookDTO;
  categories: CategoryDTO[];
  authors: AuthorOption[];
  series: NamedTermDTO[];
  publishers: NamedTermDTO[];
  stats?: { reads: number; downloads: number };
}) {
  const t = useT();
  const { locale } = useLocale();
  const router = useRouter();
  const toast = useToast();
  const [pending, start] = useTransition();
  const [errors, setErrors] = useState<Record<string, string[]>>({});
  const [title, setTitle] = useState(book?.title ?? "");
  const [author, setAuthor] = useState(book?.author ?? "");
  const [description, setDescription] = useState(book?.description ?? "");
  const [coverUrl, setCoverUrl] = useState(book?.coverUrl ?? "");
  const [epubUrl, setEpubUrl] = useState(book?.epubUrl ?? "");
  const [links, setLinks] = useState(book?.links.length ? book.links : [{ label: t("form.defaultLinkLabel"), url: "" }]);
  const [categoryIds, setCategoryIds] = useState<string[]>(book?.categories.map((c) => c.id) ?? []);
  const [featured, setFeatured] = useState(book?.featured ?? false);
  const [status, setStatus] = useState<BookStatus>(book?.status ?? "published");
  const [language, setLanguage] = useState(book?.language ?? "");
  const [isbn, setIsbn] = useState(book?.isbn ?? "");
  const [publishYear, setPublishYear] = useState(book?.publishYear != null ? String(book.publishYear) : "");
  const [series, setSeries] = useState(book?.series?.name ?? "");
  const [seriesNumber, setSeriesNumber] = useState(book?.seriesNumber != null ? String(book.seriesNumber) : "");
  const [publisher, setPublisher] = useState(book?.publisher?.name ?? "");
  const [contributors, setContributors] = useState<{ name: string; role: ContributorRole }[]>(
    book?.contributors.map((c) => ({ name: c.name, role: c.role })) ?? [],
  );
  const [checking, setChecking] = useState(false);
  const [checkResult, setCheckResult] = useState<{ status: "ok" | "tooLarge" | "unreachable"; bytes?: number } | null>(null);

  const setLink = (i: number, key: "label" | "url", value: string) =>
    setLinks((ls) => ls.map((l, j) => (j === i ? { ...l, [key]: value } : l)));
  const setContributor = (i: number, patch: Partial<{ name: string; role: ContributorRole }>) =>
    setContributors((people) => people.map((p, j) => (j === i ? { ...p, ...patch } : p)));

  const hasFile = Boolean(epubUrl.trim()) || links.some((l) => l.url.trim());
  const checks = [
    { ok: Boolean(title.trim()), label: t("form.checkTitle") },
    { ok: Boolean(author.trim()), label: t("form.checkAuthor") },
    { ok: categoryIds.length > 0, label: t("form.checkCategory") },
    { ok: hasFile, label: t("form.checkFile") },
  ];

  const check = () => {
    if (!epubUrl.trim()) return;
    setChecking(true);
    setCheckResult(null);
    start(async () => {
      const r = await checkEpubLink(epubUrl.trim());
      setChecking(false);
      if (r.ok) setCheckResult({ status: r.status, bytes: r.status === "ok" ? r.bytes : undefined });
      else toast(r.errors._form?.[0] ?? t("common.actionFailed"), "error");
    });
  };

  const checkMessage = !checkResult
    ? null
    : checkResult.status === "ok"
      ? t("form.checkLinkOk", { size: formatSize(checkResult.bytes ?? 0) })
      : checkResult.status === "tooLarge"
        ? t("form.checkLinkTooLarge")
        : t("form.checkLinkUnreachable");

  return (
    <div className="space-y-5">
      <Link href="/admin/books" className="inline-flex min-h-11 items-center font-semibold text-brick hover:underline dark:text-orange-400">
        {t("admin.navBooks")}
      </Link>
      <form
        noValidate
        className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_16rem]"
        onSubmit={(e) => {
          e.preventDefault();
          start(async () => {
            const r = await saveBook(book?.id ?? null, {
              title,
              author,
              description,
              coverUrl,
              epubUrl,
              links,
              categoryIds,
              featured,
              status,
              language: language as "" | "vi" | "en",
              isbn,
              publishYear: publishYear.trim() === "" ? null : Number(publishYear),
              series,
              seriesNumber: seriesNumber.trim() === "" ? null : Number(seriesNumber),
              publisher,
              contributors: contributors.filter((c) => c.name.trim()),
            });
            if (r.ok) {
              toast(t("form.saved"));
              router.push("/admin/books");
            } else {
              setErrors(r.errors);
              toast(r.errors._form?.[0] ?? t("form.checkFields"), "error");
            }
          });
        }}
      >
        <div className="space-y-5">
          {errors._form && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300">{errors._form[0]}</p>}

          <div className="flex flex-wrap justify-end gap-2">
            {book && status !== "draft" && (
              <Link href={`/books/${book.slug}`} className={secondaryButton}>
                {t("form.viewBook")}
              </Link>
            )}
            <button type="submit" disabled={pending} className="min-h-11 rounded-full bg-brick px-6 text-sm font-semibold text-white shadow transition hover:bg-orange-700 disabled:opacity-50">
              {pending ? t("common.saving") : book ? t("form.saveChanges") : t("form.saveBook")}
            </button>
          </div>

          <section aria-labelledby="s-info" className={section}>
            <h2 id="s-info" className="text-xl">{t("form.sectionInfo")}</h2>
            <div>
              <label htmlFor="title" className="mb-1 block text-sm font-semibold">{t("form.title")}</label>
              <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} className={input} />
              <FieldError errors={errors.title} />
            </div>
            <div>
              <label htmlFor="description" className="mb-1 block text-sm font-semibold">{t("form.description")}</label>
              <textarea id="description" rows={5} value={description} onChange={(e) => setDescription(e.target.value)} className={input} />
              <FieldError errors={errors.description} />
            </div>
          </section>

          <section aria-labelledby="s-people" className={section}>
            <h2 id="s-people" className="text-xl">{t("form.sectionPeople")}</h2>
            <div>
              <label htmlFor="author" className="mb-1 block text-sm font-semibold">{t("form.author")}</label>
              <input id="author" list="author-options" value={author} onChange={(e) => setAuthor(e.target.value)} className={input} />
              <FieldError errors={errors.author} />
            </div>
            {contributors.map((person, i) => (
              <div key={i} className="flex flex-wrap items-end gap-2">
                <div className="min-w-48 flex-1">
                  <label htmlFor={`contributor-${i}`} className="mb-1 block text-sm font-semibold">{t("form.personName")}</label>
                  <input
                    id={`contributor-${i}`}
                    list="author-options"
                    aria-label={t("form.personNameAria", { n: i + 1 })}
                    value={person.name}
                    onChange={(e) => setContributor(i, { name: e.target.value })}
                    className={input}
                  />
                </div>
                <div className="sm:w-44">
                  <label htmlFor={`contributor-role-${i}`} className="mb-1 block text-sm font-semibold">{t("form.personRole")}</label>
                  <select
                    id={`contributor-role-${i}`}
                    value={person.role}
                    onChange={(e) => setContributor(i, { role: e.target.value as ContributorRole })}
                    className={input}
                  >
                    {ROLES.map((role) => (
                      <option key={role.value} value={role.value}>{t(role.key)}</option>
                    ))}
                  </select>
                </div>
                <button
                  type="button"
                  aria-label={t("form.removePersonAria", { name: person.name })}
                  onClick={() => setContributors((people) => people.filter((_, j) => j !== i))}
                  className="grid size-11 place-items-center rounded-full text-ink/60 hover:bg-tint"
                >
                  ✕
                </button>
              </div>
            ))}
            <FieldError errors={errors.contributors} />
            <datalist id="author-options">
              {authors.map((a) => (
                <option key={a.id} value={a.name} />
              ))}
            </datalist>
            {contributors.length < 20 && (
              <button
                type="button"
                onClick={() => setContributors((people) => [...people, { name: "", role: "translator" }])}
                className="min-h-11 self-start rounded-xl border border-dashed border-ink/30 px-4 text-sm font-semibold hover:bg-tint"
              >
                {t("form.addPerson")}
              </button>
            )}
          </section>

          <section aria-labelledby="s-class" className={section}>
            <h2 id="s-class" className="text-xl">{t("form.sectionClass")}</h2>
            <fieldset>
              <legend className="mb-2 text-sm font-semibold">{t("form.categories")}</legend>
              {categories.length === 0 && <p className="text-sm text-ink/60">{t("form.noCategories")}</p>}
              <div className="flex flex-wrap gap-2">
                {categories.map((c) => {
                  const checked = categoryIds.includes(c.id);
                  return (
                    <label key={c.id} className={`cursor-pointer rounded-full border px-3 py-1.5 text-sm transition has-focus-visible:ring-4 has-focus-visible:ring-brick/20 ${checked ? "border-moss bg-moss text-white" : "border-ink/15 bg-surface hover:border-moss"}`}>
                      <input
                        type="checkbox"
                        className="sr-only"
                        checked={checked}
                        onChange={() => setCategoryIds((ids) => (checked ? ids.filter((x) => x !== c.id) : [...ids, c.id]))}
                      />
                      {c.name}
                    </label>
                  );
                })}
              </div>
              <FieldError errors={errors.categoryIds} />
            </fieldset>
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <label htmlFor="series" className="mb-1 block text-sm font-semibold">{t("form.series")}</label>
                <input id="series" list="series-options" placeholder={t("form.seriesPlaceholder")} value={series} onChange={(e) => setSeries(e.target.value)} className={input} />
                <FieldError errors={errors.series} />
              </div>
              <div>
                <label htmlFor="series-number" className="mb-1 block text-sm font-semibold">{t("form.seriesNumber")}</label>
                <input id="series-number" type="number" min="1" inputMode="numeric" value={seriesNumber} onChange={(e) => setSeriesNumber(e.target.value)} className={input} />
                <FieldError errors={errors.seriesNumber} />
              </div>
              <div>
                <label htmlFor="language" className="mb-1 block text-sm font-semibold">{t("form.language")}</label>
                <select id="language" value={language} onChange={(e) => setLanguage(e.target.value)} className={input}>
                  <option value="">{t("form.languageNone")}</option>
                  <option value="vi">{t("admin.langVi")}</option>
                  <option value="en">{t("admin.langEn")}</option>
                </select>
                <FieldError errors={errors.language} />
              </div>
              <div>
                <label htmlFor="publisher" className="mb-1 block text-sm font-semibold">{t("form.publisher")}</label>
                <input id="publisher" list="publisher-options" placeholder={t("form.publisherPlaceholder")} value={publisher} onChange={(e) => setPublisher(e.target.value)} className={input} />
                <FieldError errors={errors.publisher} />
              </div>
              <div>
                <label htmlFor="publish-year" className="mb-1 block text-sm font-semibold">{t("form.publishYear")}</label>
                <input id="publish-year" inputMode="numeric" placeholder={t("form.publishYearPlaceholder")} value={publishYear} onChange={(e) => setPublishYear(e.target.value)} className={input} />
                <FieldError errors={errors.publishYear} />
              </div>
              <div>
                <label htmlFor="isbn" className="mb-1 block text-sm font-semibold">{t("form.isbn")}</label>
                <input id="isbn" placeholder={t("form.isbnPlaceholder")} value={isbn} onChange={(e) => setIsbn(e.target.value)} className={input} />
                <FieldError errors={errors.isbn} />
              </div>
            </div>
            <datalist id="series-options">
              {seriesOptions.map((s) => (
                <option key={s.id} value={s.name} />
              ))}
            </datalist>
            <datalist id="publisher-options">
              {publishers.map((p) => (
                <option key={p.id} value={p.name} />
              ))}
            </datalist>
          </section>

          <section aria-labelledby="s-files" className={section}>
            <h2 id="s-files" className="text-xl">{t("form.sectionFiles")}</h2>
            <div className="flex flex-wrap items-end gap-2">
              <div className="min-w-0 flex-1 basis-80">
                <label htmlFor="epubUrl" className="mb-1 block text-sm font-semibold">{t("form.epubUrl")}</label>
                <input
                  id="epubUrl"
                  type="url"
                  placeholder="https://…/sach.epub"
                  value={epubUrl}
                  onChange={(e) => {
                    setEpubUrl(e.target.value);
                    setCheckResult(null);
                  }}
                  className={input}
                />
                <FieldError errors={errors.epubUrl} />
              </div>
              <button type="button" disabled={checking || !epubUrl.trim()} onClick={check} className={secondaryButton}>
                {checking ? t("form.checkLinkChecking") : t("form.checkLink")}
              </button>
            </div>
            {checkMessage && (
              <p
                role="status"
                className={`rounded-xl px-3.5 py-2.5 text-sm ${
                  checkResult?.status === "ok"
                    ? "bg-lime-100 text-lime-900 dark:bg-lime-900/40 dark:text-lime-200"
                    : "bg-red-50 text-red-800 dark:bg-red-950/40 dark:text-red-300"
                }`}
              >
                {checkMessage}
              </p>
            )}
            <div>
              <label htmlFor="coverUrl" className="mb-1 block text-sm font-semibold">{t("form.coverUrl")}</label>
              <input id="coverUrl" type="url" placeholder="https://…" value={coverUrl} onChange={(e) => setCoverUrl(e.target.value)} className={input} />
              <FieldError errors={errors.coverUrl} />
            </div>
            <fieldset>
              <legend className="mb-1 text-sm font-semibold">{t("form.links")}</legend>
              <div className="space-y-2">
                {links.map((l, i) => (
                  <div key={i} className="flex flex-col gap-2 rounded-xl bg-surface/60 p-2 sm:flex-row">
                    <input aria-label={t("form.linkLabelAria", { n: i + 1 })} placeholder={t("form.linkLabel")} value={l.label} onChange={(e) => setLink(i, "label", e.target.value)} className={`${input} sm:w-44`} />
                    <input aria-label={t("form.linkUrlAria", { n: i + 1 })} type="url" placeholder="https://…" value={l.url} onChange={(e) => setLink(i, "url", e.target.value)} className={input} />
                    {links.length > 1 && (
                      <button type="button" aria-label={t("form.removeLinkAria", { n: i + 1 })} onClick={() => setLinks((ls) => ls.filter((_, j) => j !== i))} className="rounded-xl px-3 text-red-700 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-950/40">
                        {t("common.delete")}
                      </button>
                    )}
                  </div>
                ))}
              </div>
              <FieldError errors={errors.links} />
              {links.length < 10 && (
                <button type="button" onClick={() => setLinks((ls) => [...ls, { label: "", url: "" }])} className="mt-2 text-sm font-semibold text-brick hover:underline dark:text-orange-400">
                  {t("form.addLink")}
                </button>
              )}
            </fieldset>
          </section>
        </div>

        <aside className="order-first space-y-5 lg:order-none">
          <section aria-labelledby="s-status" className={section}>
            <h2 id="s-status" className="text-lg">{t("form.sectionStatus")}</h2>
            <div>
              <label htmlFor="status" className="mb-1 block text-sm font-semibold">{t("form.status")}</label>
              <select id="status" value={status} onChange={(e) => setStatus(e.target.value as BookStatus)} className={input}>
                <option value="published">{t("admin.statusPublished")}</option>
                <option value="draft">{t("admin.statusDraft")}</option>
              </select>
              <FieldError errors={errors.status} />
            </div>
            <label className="flex min-h-11 items-center gap-2.5 text-sm font-medium">
              <input type="checkbox" checked={featured} onChange={(e) => setFeatured(e.target.checked)} className="size-[18px] accent-brick" />
              {t("form.featuredHome")}
            </label>
            <ul className="space-y-2 border-t border-ink/10 pt-3 text-sm">
              {checks.map((c) => (
                <CheckItem key={c.label} ok={c.ok} label={c.label} />
              ))}
            </ul>
          </section>

          <section aria-labelledby="s-cover" className={section}>
            <h2 id="s-cover" className="text-lg">{t("form.sectionCover")}</h2>
            <div className="w-40">
              <Cover key={coverUrl} src={coverUrl} alt={t("form.previewAlt")} className="rounded-lg shadow-lg" />
            </div>
          </section>

          {book && (
            <section aria-labelledby="s-stats" className={section}>
              <h2 id="s-stats" className="text-lg">{t("form.sectionStats")}</h2>
              <div className="flex justify-between gap-2 text-sm">
                <span className="text-ink/60">{t("admin.colReads")}</span>
                <span className="font-semibold tabular-nums">{(stats?.reads ?? 0).toLocaleString(localeTag(locale))}</span>
              </div>
              <div className="flex justify-between gap-2 text-sm">
                <span className="text-ink/60">{t("admin.colDownloads")}</span>
                <span className="font-semibold tabular-nums">{(stats?.downloads ?? 0).toLocaleString(localeTag(locale))}</span>
              </div>
              <p className="text-xs text-ink/60">{t("form.trackingNote")}</p>
            </section>
          )}

          {book && (
            <ConfirmButton
              action={() => deleteBook(book.id)}
              label={t("common.delete")}
              confirmText={t("admin.deleteBookConfirm", { title: book.title })}
              successText={t("admin.bookDeleted")}
              onSuccess={() => router.push("/admin/books")}
              className="w-full border border-red-300 py-2.5 text-red-700 hover:bg-red-50 dark:border-red-900 dark:text-red-300 dark:hover:bg-red-950/40"
            />
          )}
        </aside>
      </form>
    </div>
  );
}
