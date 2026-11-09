"use client";

import { useEffect, useRef, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { deletePublisher, savePublisher } from "@/actions/publishers";
import { deleteSeries, saveSeries } from "@/actions/series";
import { ConfirmButton } from "@/components/admin/ConfirmButton";
import { useToast } from "@/components/admin/Toast";
import { useT } from "@/components/i18n/LocaleProvider";
import type { AdminTermDTO } from "@/lib/queries";

const input = "h-11 w-full rounded-xl border border-ink/15 bg-surface px-3.5 text-sm outline-none focus:border-brick focus:ring-4 focus:ring-brick/15";
const primaryButton = "min-h-11 rounded-full bg-brick px-5 text-sm font-semibold text-white transition hover:bg-orange-700 disabled:opacity-50";
const secondaryButton = "min-h-11 rounded-full border border-ink/15 bg-surface px-4 text-sm font-semibold transition hover:bg-tint disabled:opacity-50";

export type TermKind = "series" | "publisher";

export function TermManager({ kind, terms }: { kind: TermKind; terms: AdminTermDTO[] }) {
  const t = useT();
  const router = useRouter();
  const toast = useToast();
  const [pending, start] = useTransition();
  const [draft, setDraft] = useState("");
  const [editing, setEditing] = useState<string | null>(null);
  const [editName, setEditName] = useState("");
  const [deleting, setDeleting] = useState<AdminTermDTO | null>(null);
  const [moveTo, setMoveTo] = useState("");
  const [error, setError] = useState<string>();
  const panel = useRef<HTMLElement>(null);
  const deletingId = deleting?.id;

  const save = (id: string | null, name: string, onDone: () => void) =>
    start(async () => {
      const r = id ? (kind === "series" ? await saveSeries(id, { name }) : await savePublisher(id, { name })) : kind === "series" ? await saveSeries(null, { name }) : await savePublisher(null, { name });
      if (r.ok) {
        toast(t("admin.termSaved"));
        setError(undefined);
        onDone();
        router.refresh();
      } else {
        setError(r.errors.name?.[0] ?? r.errors._form?.[0] ?? t("common.actionFailed"));
      }
    });

  const remove = (term: AdminTermDTO, target?: string) =>
    start(async () => {
      const r = kind === "series" ? await deleteSeries(term.id, target || undefined) : await deletePublisher(term.id, target || undefined);
      if (r.ok) {
        toast(t("admin.termDeleted"));
        setDeleting(null);
        router.refresh();
      } else toast(r.errors._form?.[0] ?? t("common.actionFailed"), "error");
    });

  // The delete button can sit far below the panel, so bring the panel into view.
  useEffect(() => {
    if (!deletingId) return;
    panel.current?.scrollIntoView({ block: "center" });
    panel.current?.focus({ preventScroll: true });
  }, [deletingId]);

  const addLabel = kind === "series" ? t("admin.addSeriesLabel") : t("admin.addPublisherLabel");
  const emptyText = kind === "series" ? t("admin.emptySeries") : t("admin.emptyPublishers");

  return (
    <div className="space-y-6">
      <form
        className="flex flex-wrap items-end gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          const name = draft.trim();
          if (!name) return;
          save(null, name, () => setDraft(""));
        }}
      >
        <label className="min-w-0 flex-1 basis-72 text-sm font-semibold text-ink/70">
          {addLabel}
          <input value={draft} onChange={(e) => setDraft(e.target.value)} placeholder={addLabel} className={`mt-1 ${input}`} />
        </label>
        <button type="submit" disabled={pending} className={primaryButton}>
          {t("common.add")}
        </button>
        {error && <p className="basis-full text-sm text-red-700 dark:text-red-300">{error}</p>}
      </form>

      {deleting && (
        <section ref={panel} tabIndex={-1} aria-labelledby="delete-term-h" className="space-y-3 rounded-2xl border-2 border-ink bg-surface p-5 outline-none">
          <h2 id="delete-term-h" className="text-xl">
            {t("admin.deleteTermTitle", { name: deleting.name })}
          </h2>
          <p>{t("admin.deleteTermBody", { count: deleting.bookCount })}</p>
          <label className="flex max-w-xs flex-col gap-1 text-xs font-semibold text-ink/70">
            {t("admin.moveTermTo")}
            <select value={moveTo} onChange={(e) => setMoveTo(e.target.value)} className="h-11 rounded-xl border border-ink/15 bg-surface px-3 text-sm font-normal text-ink">
              <option value="">{t("admin.moveTermNone")}</option>
              {terms
                .filter((term) => term.id !== deleting.id)
                .map((term) => (
                  <option key={term.id} value={term.id}>{term.name}</option>
                ))}
            </select>
          </label>
          <div className="flex flex-wrap gap-2">
            <button type="button" disabled={pending} onClick={() => remove(deleting, moveTo)} className="min-h-11 rounded-full bg-red-700 px-5 text-sm font-semibold text-white transition hover:bg-red-800 disabled:opacity-50">
              {t("admin.deleteTermConfirm")}
            </button>
            <button type="button" onClick={() => setDeleting(null)} className={secondaryButton}>
              {t("common.cancel")}
            </button>
          </div>
        </section>
      )}

      <div className="relative overflow-x-auto rounded-2xl bg-surface/70 ring-1 ring-ink/5">
        <table className="w-full min-w-[560px] text-left text-sm">
          <thead className="bg-tint/60 text-xs tracking-wider text-ink/60 uppercase">
            <tr>
              <th scope="col" className="px-4 py-3 sm:px-6">{t("admin.colName")}</th>
              <th scope="col" className="px-3 py-3">{t("admin.colSlug")}</th>
              <th scope="col" className="px-3 py-3 text-right">{t("admin.colBookCount")}</th>
              <th scope="col" className="px-6 py-3"><span className="sr-only">{t("admin.colActions")}</span></th>
            </tr>
          </thead>
          <tbody>
            {terms.map((term) => (
              <tr key={term.id} className="border-t border-ink/5">
                {editing === term.id ? (
                  <td colSpan={4} className="p-3">
                    <form
                      className="flex flex-wrap items-center gap-2"
                      onSubmit={(e) => {
                        e.preventDefault();
                        save(term.id, editName.trim(), () => setEditing(null));
                      }}
                    >
                      <div className="min-w-48 flex-1">
                        <input aria-label={t("admin.colName")} value={editName} onChange={(e) => setEditName(e.target.value)} className={input} />
                        {error && <p className="mt-1 text-sm text-red-700 dark:text-red-300">{error}</p>}
                      </div>
                      <button type="submit" disabled={pending} className={primaryButton}>{t("common.save")}</button>
                      <button type="button" onClick={() => setEditing(null)} className={secondaryButton}>{t("common.cancel")}</button>
                    </form>
                  </td>
                ) : (
                  <>
                    <td className="px-4 py-2 font-semibold sm:px-6">{term.name}</td>
                    <td className="px-3 py-2 text-ink/60">{term.slug}</td>
                    <td className="px-3 py-2 text-right tabular-nums">{term.bookCount}</td>
                    <td className="px-6 py-2 text-right whitespace-nowrap">
                      <button
                        type="button"
                        onClick={() => {
                          setError(undefined);
                          setEditName(term.name);
                          setEditing(term.id);
                        }}
                        className="min-h-11 rounded-full px-3 text-sm font-semibold text-brick hover:bg-tint dark:text-orange-400"
                      >
                        {t("common.edit")}
                      </button>
                      {term.bookCount > 0 ? (
                        <button
                          type="button"
                          onClick={() => {
                            setMoveTo("");
                            setDeleting(term);
                          }}
                          className="min-h-11 rounded-full px-3 text-sm font-semibold text-red-700 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-950/40"
                        >
                          {t("common.delete")}
                        </button>
                      ) : (
                        <ConfirmButton
                          action={() => (kind === "series" ? deleteSeries(term.id) : deletePublisher(term.id))}
                          label={t("common.delete")}
                          confirmText={t("admin.deleteTermTitle", { name: term.name })}
                          successText={t("admin.termDeleted")}
                          onSuccess={() => router.refresh()}
                          className="text-red-700 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-950/40"
                        />
                      )}
                    </td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>
        {terms.length === 0 && <p className="p-8 text-center text-ink/60">{emptyText}</p>}
      </div>
    </div>
  );
}
