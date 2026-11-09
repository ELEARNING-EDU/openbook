"use client";

import { useT } from "@/components/i18n/LocaleProvider";
import type { TocItem } from "@/lib/epub/parse";

export function TocDrawer({ toc, current, onPick, onClose }: { toc: TocItem[]; current: number; onPick(chapter: number): void; onClose(): void }) {
  const t = useT();
  const list = (items: TocItem[], depth: number) => (
    <ul className={depth ? "ml-4 border-l border-black/10 pl-2" : ""}>
      {items.map((item, i) => (
        <li key={`${depth}-${i}`}>
          {item.chapter >= 0 ? (
            <button
              type="button"
              aria-current={item.chapter === current ? "true" : undefined}
              onClick={() => onPick(item.chapter)}
              className={`w-full rounded-lg px-3 py-2 text-left text-sm transition hover:bg-black/5 ${item.chapter === current ? "font-bold text-brick" : ""}`}
            >
              {item.label}
            </button>
          ) : (
            <p className="px-3 py-2 text-sm font-semibold">{item.label}</p>
          )}
          {item.children.length > 0 && list(item.children, depth + 1)}
        </li>
      ))}
    </ul>
  );
  return (
    <div className="absolute inset-0 z-20 flex">
      <nav aria-label={t("reader.toc")} className="h-full w-80 max-w-[85%] overflow-y-auto bg-surface p-4 text-ink shadow-2xl">
        <div className="mb-3 flex items-center justify-between">
          <p className="font-display text-lg font-bold">{t("reader.toc")}</p>
          <button type="button" aria-label={t("reader.closeToc")} onClick={onClose} className="grid size-9 place-items-center rounded-full hover:bg-black/5">
            ✕
          </button>
        </div>
        {list(toc, 0)}
      </nav>
      <button type="button" aria-label={t("reader.closeToc")} className="flex-1 bg-black/30" onClick={onClose} />
    </div>
  );
}
