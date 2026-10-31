"use client";

import type { TocItem } from "@/lib/epub/parse";

export function TocDrawer({ toc, current, onPick, onClose }: { toc: TocItem[]; current: number; onPick(chapter: number): void; onClose(): void }) {
  const list = (items: TocItem[], depth: number) => (
    <ul className={depth ? "ml-4 border-l border-black/10 pl-2" : ""}>
      {items.map((t, i) => (
        <li key={`${depth}-${i}`}>
          {t.chapter >= 0 ? (
            <button
              type="button"
              aria-current={t.chapter === current ? "true" : undefined}
              onClick={() => onPick(t.chapter)}
              className={`w-full rounded-lg px-3 py-2 text-left text-sm transition hover:bg-black/5 ${t.chapter === current ? "font-bold text-brick" : ""}`}
            >
              {t.label}
            </button>
          ) : (
            <p className="px-3 py-2 text-sm font-semibold">{t.label}</p>
          )}
          {t.children.length > 0 && list(t.children, depth + 1)}
        </li>
      ))}
    </ul>
  );
  return (
    <div className="absolute inset-0 z-20 flex">
      <nav aria-label="Mục lục" className="h-full w-80 max-w-[85%] overflow-y-auto bg-white p-4 text-ink shadow-2xl">
        <div className="mb-3 flex items-center justify-between">
          <p className="font-display text-lg font-bold">Mục lục</p>
          <button type="button" aria-label="Đóng mục lục" onClick={onClose} className="grid size-9 place-items-center rounded-full hover:bg-black/5">
            ✕
          </button>
        </div>
        {list(toc, 0)}
      </nav>
      <button type="button" aria-label="Đóng mục lục" className="flex-1 bg-black/30" onClick={onClose} />
    </div>
  );
}
