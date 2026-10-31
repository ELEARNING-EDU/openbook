"use client";

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
  return (
    <div
      className={`flex items-center gap-1 px-3 py-2 transition-opacity duration-300 ${p.visible ? "opacity-100" : "pointer-events-none opacity-0"}`}
    >
      <button type="button" aria-label="Mục lục" onClick={p.onToc} className={btn}>
        ☰
      </button>
      <p className="mx-2 min-w-0 flex-1 truncate text-center font-display text-sm font-semibold">{p.chapterTitle}</p>
      <button type="button" aria-label="Cài đặt hiển thị" onClick={p.onSettings} className={`${btn} font-display font-bold`}>
        Aa
      </button>
      <button type="button" aria-label={p.fullscreen ? "Thoát toàn màn hình" : "Toàn màn hình"} onClick={p.onFullscreen} className={btn}>
        ⛶
      </button>
      <button type="button" aria-label="Đóng trình đọc" onClick={p.onClose} className={btn}>
        ✕
      </button>
    </div>
  );
}
