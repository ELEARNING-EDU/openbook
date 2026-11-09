"use client";

import { useEffect, useRef, useState, useTransition } from "react";
import { deleteCategory, saveCategory, saveCategoryMenu } from "@/actions/categories";
import { ConfirmButton } from "@/components/admin/ConfirmButton";
import { useT } from "@/components/i18n/LocaleProvider";
import { useToast } from "@/components/admin/Toast";
import type { AdminCategoryDTO } from "@/lib/queries";

const input = "h-11 w-full rounded-full border border-ink/15 bg-surface px-4 text-sm outline-none focus:border-brick focus:ring-4 focus:ring-brick/15";
const primaryButton = "min-h-11 rounded-full bg-brick px-5 text-sm font-semibold text-white transition hover:bg-orange-700 disabled:opacity-50";
const secondaryButton = "min-h-11 rounded-full border border-ink/15 bg-surface px-5 text-sm font-semibold transition hover:bg-tint disabled:opacity-50";
const iconButton = "grid size-11 place-items-center rounded-full text-ink transition hover:bg-tint disabled:opacity-40 disabled:hover:bg-transparent";

type MenuItem = { id: string; name: string; showInMenu: boolean };

const toMenu = (categories: AdminCategoryDTO[]): MenuItem[] => categories.map((c) => ({ id: c.id, name: c.name, showInMenu: c.showInMenu }));

function NameForm({ id, initial, submitLabel, onDone }: { id: string | null; initial: string; submitLabel: string; onDone?: () => void }) {
  const t = useT();
  const [name, setName] = useState(initial);
  const [error, setError] = useState<string>();
  const [pending, start] = useTransition();
  const toast = useToast();
  return (
    <form
      className="flex flex-1 flex-wrap items-start gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        start(async () => {
          const r = await saveCategory(id, { name });
          if (r.ok) {
            toast(t("form.categorySaved"));
            setError(undefined);
            if (!id) setName("");
            onDone?.();
          } else setError(r.errors.name?.[0] ?? r.errors._form?.[0]);
        });
      }}
    >
      <div className="min-w-48 flex-1">
        <input aria-label={t("form.categoryName")} placeholder={t("form.categoryName")} value={name} onChange={(e) => setName(e.target.value)} className={input} />
        {error && <p className="mt-1 px-4 text-sm text-red-700 dark:text-red-300">{error}</p>}
      </div>
      <button type="submit" disabled={pending} className={primaryButton}>
        {submitLabel}
      </button>
    </form>
  );
}

function Row({ category, onAskDelete }: { category: AdminCategoryDTO; onAskDelete: () => void }) {
  const t = useT();
  const [editing, setEditing] = useState(false);
  if (editing) {
    return (
      <tr className="border-t border-ink/5">
        <td colSpan={4} className="p-3">
          <div className="flex flex-wrap items-center gap-2">
            <NameForm id={category.id} initial={category.name} submitLabel={t("common.save")} onDone={() => setEditing(false)} />
            <button type="button" onClick={() => setEditing(false)} className={secondaryButton}>
              {t("common.cancel")}
            </button>
          </div>
        </td>
      </tr>
    );
  }
  return (
    <tr className="border-t border-ink/5">
      <td className="py-1 pr-1 pl-4 font-semibold sm:px-6">{category.name}</td>
      <td className="hidden px-3 py-1 text-ink/60 sm:table-cell">/{category.slug}</td>
      <td className="px-3 py-1 text-right tabular-nums">{category.bookCount}</td>
      <td className="py-1 pr-2 text-right whitespace-nowrap sm:px-6">
        <button type="button" onClick={() => setEditing(true)} className="min-h-11 rounded-full px-3 text-sm font-semibold text-brick hover:bg-tint">
          {t("common.edit")}
        </button>
        {category.bookCount > 0 ? (
          <button type="button" onClick={onAskDelete} className="min-h-11 rounded-full px-3 text-sm font-semibold text-red-700 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-950/40">
            {t("common.delete")}
          </button>
        ) : (
          <ConfirmButton
            action={deleteCategory.bind(null, category.id)}
            label={t("common.delete")}
            confirmText={t("form.deleteCategory", { name: category.name })}
            successText={t("form.categoryDeleted")}
            className="text-red-700 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-950/40"
          />
        )}
      </td>
    </tr>
  );
}

export function CategoryManager({ categories }: { categories: AdminCategoryDTO[] }) {
  const t = useT();
  const toast = useToast();
  const [pending, start] = useTransition();
  const [serverCategories, setServerCategories] = useState(categories);
  const [menu, setMenu] = useState<MenuItem[]>(() => toMenu(categories));
  const [deleting, setDeleting] = useState<AdminCategoryDTO | null>(null);
  const [moveTo, setMoveTo] = useState("");
  const deletePanel = useRef<HTMLElement>(null);
  const deletingId = deleting?.id;

  // The delete button can sit far below the panel, so bring the panel into view.
  useEffect(() => {
    if (!deletingId) return;
    deletePanel.current?.scrollIntoView({ block: "center" });
    deletePanel.current?.focus({ preventScroll: true });
  }, [deletingId]);

  // Server data wins after renames, additions, deletes and menu saves.
  if (serverCategories !== categories) {
    setServerCategories(categories);
    setMenu(toMenu(categories));
  }

  const move = (index: number, by: number) =>
    setMenu((items) => {
      const next = [...items];
      const target = index + by;
      if (target < 0 || target >= next.length) return items;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });

  const saveMenu = () =>
    start(async () => {
      const r = await saveCategoryMenu(menu.map(({ id, showInMenu }) => ({ id, showInMenu })));
      toast(r.ok ? t("admin.menuSaved") : (r.errors._form?.[0] ?? t("common.actionFailed")), r.ok ? "ok" : "error");
    });

  const confirmDelete = () => {
    if (!deleting) return;
    start(async () => {
      const r = await deleteCategory(deleting.id, moveTo || undefined);
      if (r.ok) {
        setDeleting(null);
        toast(t("form.categoryDeleted"));
      } else toast(r.errors._form?.[0] ?? t("common.actionFailed"), "error");
    });
  };

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-tint/60 p-4">
        <NameForm id={null} initial="" submitLabel={t("form.addCategory")} />
      </div>

      {deleting && (
        <section ref={deletePanel} tabIndex={-1} aria-labelledby="delete-category-h" className="space-y-3 rounded-2xl border-2 border-ink bg-surface p-5 outline-none">
          <h2 id="delete-category-h" className="text-xl">
            {t("admin.deleteCategoryTitle", { name: deleting.name })}
          </h2>
          <p>{t("admin.deleteCategoryBody", { count: deleting.bookCount })}</p>
          <label className="flex max-w-xs flex-col gap-1 text-xs font-semibold text-ink/70">
            {t("admin.moveBooksTo")}
            <select value={moveTo} onChange={(e) => setMoveTo(e.target.value)} className="h-11 rounded-xl border border-ink/15 bg-surface px-3 text-sm font-normal text-ink">
              <option value="">{t("admin.moveBooksNone")}</option>
              {categories
                .filter((c) => c.id !== deleting.id)
                .map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
            </select>
          </label>
          <div className="flex flex-wrap gap-2">
            <button type="button" disabled={pending} onClick={confirmDelete} className="min-h-11 rounded-full bg-red-700 px-5 text-sm font-semibold text-white transition hover:bg-red-800 disabled:opacity-50">
              {t("admin.deleteCategoryConfirm")}
            </button>
            <button type="button" onClick={() => setDeleting(null)} className={secondaryButton}>
              {t("common.cancel")}
            </button>
          </div>
        </section>
      )}

      <section aria-labelledby="menu-h" className="space-y-3 rounded-2xl bg-surface/70 p-5 ring-1 ring-ink/5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 id="menu-h" className="text-xl">
            {t("admin.menuTitle")}
          </h2>
          <button type="button" disabled={pending} onClick={saveMenu} className={primaryButton}>
            {t("admin.menuSave")}
          </button>
        </div>
        <div className="flex flex-col gap-1.5 rounded-xl bg-cream p-3">
          <span className="text-xs font-semibold text-ink/60">{t("admin.menuPreview")}</span>
          <div className="flex flex-wrap gap-x-4 gap-y-1">
            <span className="font-semibold">{t("common.all")}</span>
            {menu
              .filter((m) => m.showInMenu)
              .map((m) => (
                <span key={m.id}>{m.name}</span>
              ))}
          </div>
        </div>
        {menu.length === 0 ? (
          <p className="text-ink/60">{t("form.noCategories")}</p>
        ) : (
          <ul>
            {menu.map((m, i) => (
              <li key={m.id} className="flex flex-wrap items-center gap-x-3 border-t border-ink/5">
                <label className="flex min-h-11 flex-1 basis-56 cursor-pointer items-center gap-2.5 font-medium">
                  <input
                    type="checkbox"
                    checked={m.showInMenu}
                    onChange={() => setMenu((items) => items.map((x) => (x.id === m.id ? { ...x, showInMenu: !x.showInMenu } : x)))}
                    className="size-[18px] accent-brick"
                  />
                  {m.name}
                </label>
                <div className="flex gap-1">
                  <button type="button" disabled={i === 0} aria-label={t("admin.menuMoveUp", { name: m.name })} onClick={() => move(i, -1)} className={iconButton}>
                    <svg aria-hidden width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M6 15l6-6 6 6" />
                    </svg>
                  </button>
                  <button type="button" disabled={i === menu.length - 1} aria-label={t("admin.menuMoveDown", { name: m.name })} onClick={() => move(i, 1)} className={iconButton}>
                    <svg aria-hidden width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M6 9l6 6 6-6" />
                    </svg>
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <div className="relative overflow-x-auto rounded-2xl bg-surface/70 ring-1 ring-ink/5">
        <table className="w-full text-left text-sm">
          <thead className="bg-tint/60 text-xs tracking-wider text-ink/60 uppercase">
            <tr>
              <th scope="col" className="py-3 pr-1 pl-4 sm:px-6">{t("admin.colName")}</th>
              <th scope="col" className="hidden px-3 py-3 sm:table-cell">{t("admin.colSlug")}</th>
              <th scope="col" className="px-3 py-3 text-right">{t("admin.colBookCount")}</th>
              <th scope="col" className="px-6 py-3 text-right"><span className="sr-only">{t("admin.colActions")}</span></th>
            </tr>
          </thead>
          <tbody>
            {categories.map((c) => (
              <Row
                key={c.id}
                category={c}
                onAskDelete={() => {
                  setMoveTo("");
                  setDeleting(c);
                }}
              />
            ))}
          </tbody>
        </table>
        {categories.length === 0 && <p className="p-8 text-center text-ink/60">{t("form.noCategories")}</p>}
      </div>
    </div>
  );
}
