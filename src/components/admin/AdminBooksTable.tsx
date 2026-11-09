"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { bulkAddCategory, bulkDeleteBooks, bulkSetBookStatus, deleteBook, toggleFeatured } from "@/actions/books";
import { ConfirmButton } from "@/components/admin/ConfirmButton";
import { Cover } from "@/components/Cover";
import { Stars } from "@/components/Stars";
import { useT } from "@/components/i18n/LocaleProvider";
import { useToast } from "@/components/admin/Toast";
import type { ActionResult } from "@/actions/types";
import type { AdminBookRowDTO, CategoryDTO } from "@/lib/queries";

const barButton = "min-h-11 rounded-lg px-3 text-sm font-medium text-cream transition hover:bg-white/10 disabled:opacity-40";
const statusPill = (status: "published" | "draft") =>
  status === "published"
    ? "bg-lime-100 text-lime-900 dark:bg-lime-900/40 dark:text-lime-200"
    : "bg-tint text-ink/70";

export function AdminBooksTable({
  books,
  categories,
  localeTag,
}: {
  books: AdminBookRowDTO[];
  categories: CategoryDTO[];
  localeTag: string;
}) {
  const t = useT();
  const router = useRouter();
  const toast = useToast();
  const [pending, start] = useTransition();
  const [selected, setSelected] = useState<string[]>([]);
  const [categoryId, setCategoryId] = useState("");

  const allSelected = books.length > 0 && books.every((b) => selected.includes(b.id));
  const toggle = (id: string) =>
    setSelected((ids) => (ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]));
  const toggleAll = () => setSelected(allSelected ? [] : books.map((b) => b.id));

  const run = (action: () => Promise<ActionResult>, success: string) =>
    start(async () => {
      const r = await action();
      if (r.ok) {
        toast(success);
        setSelected([]);
        setCategoryId("");
        router.refresh();
      } else toast(r.errors._form?.[0] ?? t("common.actionFailed"), "error");
    });

  const remove = () => {
    if (!window.confirm(t("admin.bulkDeleteConfirm", { n: selected.length }))) return;
    run(() => bulkDeleteBooks(selected), t("admin.bulkDeleted", { n: selected.length }));
  };

  const featured = (book: AdminBookRowDTO) => (
    <ConfirmButton
      action={() => toggleFeatured(book.id)}
      label={book.featured ? t("admin.featuredOn") : t("admin.featuredOff")}
      successText={t("common.updated")}
      onSuccess={() => {
        setSelected([]);
        router.refresh();
      }}
      className={book.featured ? "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200" : "hover:bg-tint"}
    />
  );
  const editLink = (book: AdminBookRowDTO) => (
    <Link href={`/admin/books/${book.id}`} className="inline-flex min-h-11 items-center rounded-full px-3 text-sm font-semibold text-brick hover:bg-tint dark:text-orange-400">
      {t("common.edit")}
    </Link>
  );
  const deleteButton = (book: AdminBookRowDTO) => (
    <ConfirmButton
      action={() => deleteBook(book.id)}
      label={t("common.delete")}
      confirmText={t("admin.deleteBookConfirm", { title: book.title })}
      successText={t("admin.bookDeleted")}
      onSuccess={() => {
        setSelected([]);
        router.refresh();
      }}
      className="text-red-700 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-950/40"
    />
  );
  const statusCell = (book: AdminBookRowDTO) => (
    <div className="flex flex-wrap gap-1">
      <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap ${statusPill(book.status)}`}>
        {book.status === "draft" ? t("admin.statusDraft") : t("admin.statusPublished")}
      </span>
      {book.issues.map((issue) => (
        <span key={issue} className="inline-block rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap text-red-800 dark:bg-red-950/40 dark:text-red-300">
          {t(issue)}
        </span>
      ))}
    </div>
  );

  return (
    <div className="space-y-4">
      {selected.length > 0 && (
        <div role="region" aria-label={t("admin.bulkRegion")} className="sticky top-2 z-30 flex flex-wrap items-center gap-2 rounded-2xl bg-ink px-4 py-2 text-cream">
          <span className="mr-auto font-semibold">{t("admin.bulkSelected", { n: selected.length })}</span>
          <label className="sr-only" htmlFor="bulk-category">{t("admin.bulkPickCategory")}</label>
          <select
            id="bulk-category"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            className="min-h-11 rounded-lg border border-cream/25 bg-ink px-2 text-sm text-cream"
          >
            <option value="">{t("admin.bulkPickCategory")}</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
          <button
            type="button"
            disabled={pending || !categoryId}
            onClick={() => run(() => bulkAddCategory(selected, categoryId), t("admin.bulkUpdated", { n: selected.length }))}
            className={barButton}
          >
            {t("admin.bulkCategory")}
          </button>
          <button type="button" disabled={pending} onClick={() => run(() => bulkSetBookStatus(selected, "published"), t("admin.bulkUpdated", { n: selected.length }))} className={barButton}>
            {t("admin.bulkPublish")}
          </button>
          <button type="button" disabled={pending} onClick={() => run(() => bulkSetBookStatus(selected, "draft"), t("admin.bulkUpdated", { n: selected.length }))} className={barButton}>
            {t("admin.bulkDraft")}
          </button>
          <button type="button" disabled={pending} onClick={remove} className="min-h-11 rounded-lg px-3 text-sm font-medium text-red-200 transition hover:bg-white/10 disabled:opacity-40">
            {t("common.delete")}
          </button>
          <button type="button" aria-label={t("admin.bulkClear")} onClick={() => setSelected([])} className="grid size-11 place-items-center rounded-lg text-cream hover:bg-white/10">
            ✕
          </button>
        </div>
      )}

      <div className="relative overflow-x-auto rounded-2xl bg-surface/70 ring-1 ring-ink/5">
        <table className="hidden w-full min-w-[880px] text-left text-sm md:table">
          <thead className="bg-tint/60 text-xs tracking-wider text-ink/60 uppercase">
            <tr>
              <th scope="col" className="w-11 px-3 py-3">
                <input type="checkbox" aria-label={t("admin.selectAll")} checked={allSelected} onChange={toggleAll} className="size-[18px] accent-brick" />
              </th>
              <th className="p-3">{t("admin.colBook")}</th>
              <th className="p-3">{t("admin.colCategory")}</th>
              <th className="p-3">{t("admin.colStatus")}</th>
              <th className="p-3 text-right">{t("admin.colReads")}</th>
              <th className="p-3 text-right">{t("admin.colDownloads")}</th>
              <th className="p-3 text-right">{t("admin.colRating")}</th>
              <th className="p-3"><span className="sr-only">{t("admin.colActions")}</span></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-ink/5">
            {books.map((book) => (
              <tr key={book.id} className={selected.includes(book.id) ? "bg-orange-50/70 dark:bg-orange-950/20" : "hover:bg-tint/40"}>
                <td className="px-3 py-2">
                  <input
                    type="checkbox"
                    aria-label={t("admin.selectBook", { title: book.title })}
                    checked={selected.includes(book.id)}
                    onChange={() => toggle(book.id)}
                    className="size-[18px] accent-brick"
                  />
                </td>
                <td className="p-3">
                  <div className="flex items-center gap-3">
                    <div className="w-9 shrink-0"><Cover src={book.coverUrl} alt="" className="rounded-md" /></div>
                    <div className="min-w-0">
                      <Link href={`/admin/books/${book.id}`} className="font-semibold hover:text-brick">{book.title}</Link>
                      <p className="text-ink/60">{book.author}</p>
                    </div>
                  </div>
                </td>
                <td className="p-3 text-ink/70">{book.categories.map((c) => c.name).join(", ") || "—"}</td>
                <td className="p-3">{statusCell(book)}</td>
                <td className="p-3 text-right tabular-nums">{book.reads.toLocaleString(localeTag)}</td>
                <td className="p-3 text-right tabular-nums">{book.downloads.toLocaleString(localeTag)}</td>
                <td className="p-3 text-right">
                  {book.ratingCount > 0 ? (
                    <span className="inline-flex items-center gap-1">
                      <Stars value={book.ratingAvg} />
                      <span className="text-xs text-ink/60">({book.ratingCount})</span>
                    </span>
                  ) : (
                    "—"
                  )}
                </td>
                <td className="p-3">
                  <div className="flex flex-wrap items-center justify-end gap-1">
                    {featured(book)}
                    {editLink(book)}
                    {deleteButton(book)}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <ul className="divide-y divide-ink/5 md:hidden">
          {books.map((book) => (
            <li key={book.id} className={`flex gap-3 p-3 ${selected.includes(book.id) ? "bg-orange-50/70 dark:bg-orange-950/20" : ""}`}>
              <input
                type="checkbox"
                aria-label={t("admin.selectBook", { title: book.title })}
                checked={selected.includes(book.id)}
                onChange={() => toggle(book.id)}
                className="mt-1 size-[18px] shrink-0 accent-brick"
              />
              <div className="w-16 shrink-0"><Cover src={book.coverUrl} alt="" className="rounded-lg" /></div>
              <div className="min-w-0 flex-1">
                <Link href={`/admin/books/${book.id}`} className="font-semibold hover:text-brick">{book.title}</Link>
                <p className="text-sm text-ink/60">{book.author}</p>
                <div className="mt-1">{statusCell(book)}</div>
                <p className="mt-1 text-xs text-ink/60">
                  {t("admin.colReads")} {book.reads.toLocaleString(localeTag)} · {t("admin.colDownloads")} {book.downloads.toLocaleString(localeTag)} · ★ {book.ratingAvg.toFixed(1)}
                </p>
                <div className="mt-2 flex flex-wrap gap-1">
                  {featured(book)}
                  {editLink(book)}
                  {deleteButton(book)}
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
