"use client";

import { useT } from "@/components/i18n/LocaleProvider";

const btn = "grid size-10 place-items-center rounded-full text-lg transition hover:bg-black/10 focus-visible:outline-2 focus-visible:outline-brick";

export function ReaderToolbar(p: {
  chapterTitle: string;
  visible: boolean;
  fullscreen: boolean;
  onToc(): void;
  onSettings(): void;
  onFullscreen(): void;
  onClose(): void;
}) {
  const t = useT();
  return (
    <div
      className={`flex items-center gap-1 px-3 py-2 transition-opacity duration-300 ${p.visible ? "opacity-100" : "pointer-events-none opacity-0"}`}
    >
      <button type="button" aria-label={t("reader.toc")} onClick={p.onToc} className={btn}>
        ☰
      </button>
      <p className="mx-2 min-w-0 flex-1 truncate text-center font-display text-sm font-semibold">{p.chapterTitle}</p>
      <button type="button" aria-label={t("reader.settings")} onClick={p.onSettings} className={`${btn} font-display font-bold`}>
        Aa
      </button>
      <button type="button" aria-label={p.fullscreen ? t("reader.exitFullscreen") : t("reader.fullscreen")} onClick={p.onFullscreen} className={btn}>
        ⛶
      </button>
      <button type="button" aria-label={t("reader.closeReader")} onClick={p.onClose} className={btn}>
        ✕
      </button>
    </div>
  );
}
