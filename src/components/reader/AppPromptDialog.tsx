"use client";

import { useEffect, useRef } from "react";
import { Cover } from "@/components/Cover";
import { useT } from "@/components/i18n/LocaleProvider";
import { appBookUrl, appStoreUrl, playStoreUrl } from "@/lib/reader/app-links";

export function AppPromptDialog({
  open,
  onClose,
  book,
}: {
  open: boolean;
  onClose: () => void;
  book: { slug: string; title: string; coverUrl: string };
}) {
  const t = useT();
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (open && !d.open) d.showModal();
    if (!open && d.open) d.close();
  }, [open]);

  const openInApp = appBookUrl(book.slug);
  const ios = appStoreUrl();
  const android = playStoreUrl();
  const btn = "flex w-full items-center justify-center rounded-full px-5 py-3 font-semibold transition";

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      aria-labelledby="app-prompt-title"
      className="m-auto w-[min(92vw,380px)] rounded-3xl bg-cream p-6 text-ink shadow-2xl backdrop:bg-ink/50"
    >
      <div className="flex flex-col items-center text-center">
        <div className="w-24">
          <Cover src={book.coverUrl} alt="" className="rounded-xl shadow-lg" />
        </div>
        <h2 id="app-prompt-title" className="mt-4 text-xl font-bold">
          {t("app.title")}
        </h2>
        <p className="mt-1 text-sm text-ink/70">{t("app.body", { title: book.title })}</p>
        <div className="mt-5 w-full space-y-2">
          {openInApp && (
            <a href={openInApp} className={`${btn} bg-gradient-to-r from-brick to-rose-600 text-white shadow-lg`}>
              {t("app.openInApp")}
            </a>
          )}
          <div className="flex gap-2">
            {ios && (
              <a href={ios} target="_blank" rel="noopener noreferrer" className={`${btn} bg-ink text-cream`}>
                App Store
              </a>
            )}
            {android && (
              <a href={android} target="_blank" rel="noopener noreferrer" className={`${btn} bg-ink text-cream`}>
                Google Play
              </a>
            )}
          </div>
          <button type="button" onClick={onClose} className={`${btn} hover:bg-tint`}>
            {t("common.close")}
          </button>
        </div>
      </div>
    </dialog>
  );
}
