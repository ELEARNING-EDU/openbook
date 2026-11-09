"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, useTransition } from "react";
import { markNotDuplicate, mergeAuthors } from "@/actions/authors";
import { useToast } from "@/components/admin/Toast";
import { useT } from "@/components/i18n/LocaleProvider";
import type { AdminAuthorDetail } from "@/lib/queries";

const primaryButton =
  "min-h-11 rounded-full bg-brick px-5 text-sm font-semibold text-white transition hover:bg-orange-700 disabled:opacity-50";
const secondaryButton =
  "min-h-11 rounded-full border border-ink/15 bg-surface px-5 text-sm font-semibold transition hover:bg-tint disabled:opacity-50";

export function AuthorMergePanel({ initialKeep, other }: { initialKeep: AdminAuthorDetail; other: AdminAuthorDetail }) {
  const t = useT();
  const router = useRouter();
  const toast = useToast();
  const [keepId, setKeepId] = useState(initialKeep.id);
  const [pending, start] = useTransition();
  const panel = useRef<HTMLElement>(null);
  const keep = keepId === initialKeep.id ? initialKeep : other;
  const remove = keepId === initialKeep.id ? other : initialKeep;
  const lostBio = !!remove.bio.trim() && !!keep.bio.trim();
  const lostLifespan = !!remove.lifespan.trim() && !!keep.lifespan.trim();

  useEffect(() => {
    panel.current?.scrollIntoView({ block: "center" });
    panel.current?.focus({ preventScroll: true });
  }, []);

  const confirm = () =>
    start(async () => {
      const r = await mergeAuthors(keep.id, remove.id);
      if (r.ok) {
        toast(t("admin.merged", { removed: remove.name, kept: keep.name }));
        router.push(`/admin/authors?id=${keep.id}`);
      } else toast(r.errors._form?.[0] ?? t("common.actionFailed"), "error");
    });

  const different = () =>
    start(async () => {
      const r = await markNotDuplicate(keep.id, remove.id);
      if (r.ok) {
        toast(t("admin.notDuplicate"));
        router.push(`/admin/authors?id=${keep.id}`);
      } else toast(r.errors._form?.[0] ?? t("common.actionFailed"), "error");
    });

  return (
    <section
      ref={panel}
      tabIndex={-1}
      aria-labelledby="merge-authors-h"
      className="flex flex-col gap-4 rounded-2xl border-2 border-ink bg-surface p-5 outline-none"
    >
      <h2 id="merge-authors-h" className="text-2xl">{t("admin.mergeTitle")}</h2>
      <fieldset className="m-0 grid gap-3 border-0 p-0 sm:grid-cols-2">
        <legend className="mb-2 font-semibold">{t("admin.mergeKeep")}</legend>
        {[initialKeep, other].map((option) => {
          const on = option.id === keepId;
          return (
            <label
              key={option.id}
              className={`flex cursor-pointer gap-3 rounded-xl border p-3.5 ${on ? "border-brick bg-tint/60" : "border-ink/15 bg-surface hover:border-brick/50"}`}
            >
              <input type="radio" name="merge-keep" checked={on} onChange={() => setKeepId(option.id)} className="mt-0.5 size-[18px] shrink-0 accent-brick" />
              <span className="flex min-w-0 flex-col">
                <span className="font-semibold">{option.name}</span>
                <span className="text-sm text-ink/60">
                  {t("common.booksCount", { n: option.bookCount })}
                  {option.lifespan ? ` · ${option.lifespan}` : ""}
                </span>
              </span>
            </label>
          );
        })}
      </fieldset>
      <ul className="list-disc space-y-1 pl-5">
        <li>{t("admin.mergeAliasNote")}</li>
        <li>{t("admin.mergeBooks", { n: remove.bookCount, from: remove.name, to: keep.name })}</li>
        <li>{t("admin.mergeFields")}</li>
        {lostBio && <li className="font-semibold text-red-700 dark:text-red-300">{t("admin.mergeLossBio", { name: remove.name })}</li>}
        {lostLifespan && <li className="font-semibold text-red-700 dark:text-red-300">{t("admin.mergeLossLifespan", { name: remove.name })}</li>}
      </ul>
      <p className="text-ink/60">{t("admin.mergeUndo")}</p>
      <div className="flex flex-wrap items-center gap-2">
        <button type="button" disabled={pending} onClick={confirm} className={primaryButton}>{t("admin.mergeConfirm")}</button>
        <button type="button" onClick={() => router.push(`/admin/authors?id=${initialKeep.id}`)} className={secondaryButton}>{t("common.cancel")}</button>
        <button type="button" disabled={pending} onClick={different} className="min-h-11 rounded-full px-3 text-sm text-ink/60 hover:bg-tint">
          {t("admin.mergeDifferent")}
        </button>
      </div>
    </section>
  );
}
