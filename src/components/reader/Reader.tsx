"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { measureHtml, PageStage, pageLayout } from "@/components/reader/PageStage";
import { ReaderToolbar } from "@/components/reader/ReaderToolbar";
import { SettingsPanel } from "@/components/reader/SettingsPanel";
import { THEME_COLORS } from "@/components/reader/themes";
import { TocDrawer } from "@/components/reader/TocDrawer";
import { useT } from "@/components/i18n/LocaleProvider";
import { openEpub, type EpubBook, type TocItem } from "@/lib/epub/parse";
import { sanitizeChapter } from "@/lib/epub/sanitize";
import { isTwoPage, pageForProgress, progressForPage } from "@/lib/reader/paging";
import { loadPosition, loadSettings, savePosition, saveSettings, type ReaderSettings } from "@/lib/reader/storage";
import "./reader.css";

const MIME: Record<string, string> = { png: "image/png", jpg: "image/jpeg", jpeg: "image/jpeg", gif: "image/gif", webp: "image/webp", svg: "image/svg+xml" };
const CHROME_HIDE_MS = 3000;
const SINGLE_PAGE_MAX = 720;
const SPREAD_MAX = 1400;

const flatten = (items: TocItem[]): TocItem[] => items.flatMap((t) => [t, ...flatten(t.children)]);

type Props = { slug: string; title: string; epubUrl: string; onClose(): void };

export default function Reader({ slug, title, epubUrl, onClose }: Props) {
  const t = useT();
  const tRef = useRef(t);
  useEffect(() => {
    tRef.current = t;
  }, [t]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [attempt, setAttempt] = useState(0);
  const [tooLarge, setTooLarge] = useState(false);
  const [book, setBook] = useState<{ epub: EpubBook; htmls: string[] } | null>(null);
  const [settings, setSettings] = useState<ReaderSettings>(loadSettings);
  const [chapter, setChapter] = useState(0);
  const [page, setPage] = useState(0);
  const [counts, setCounts] = useState<(number | undefined)[]>([]);
  const [size, setSize] = useState({ w: 0, h: 0 });
  const [panel, setPanel] = useState<"toc" | "settings" | null>(null);
  const [chrome, setChrome] = useState(true);
  const [fullscreen, setFullscreen] = useState(false);
  const [reducedMotion] = useState(() => window.matchMedia("(prefers-reduced-motion: reduce)").matches);

  const rootRef = useRef<HTMLDivElement>(null);
  const stageRef = useRef<HTMLDivElement>(null);
  const controlRef = useRef<{ next(): boolean; prev(): boolean } | null>(null);
  const pending = useRef<number | null>(null); // progress to restore after the next measurement
  // Load, parse and sanitize every chapter once (all chapters are measured anyway).
  useEffect(() => {
    let cancelled = false;
    const urls: string[] = [];
    (async () => {
      try {
        const res = await fetch(`/books/${slug}/epub`);
        if (res.status === 413 && !cancelled) setTooLarge(true);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const parsed = openEpub(await res.arrayBuffer(), (i) => tRef.current("reader.chapter", { n: i + 1 }));
        if (cancelled) return;
        const images = new Map<string, string>();
        const imageUrl = (path: string) => {
          if (images.has(path)) return images.get(path);
          const data = parsed.resource(path);
          if (!data) return undefined;
          const type = MIME[path.split(".").pop()?.toLowerCase() ?? ""] ?? "application/octet-stream";
          const url = URL.createObjectURL(new Blob([data as BlobPart], { type }));
          images.set(path, url);
          urls.push(url);
          return url;
        };
        const htmls = parsed.chapters.map((c, i) => {
          try {
            return sanitizeChapter(parsed.chapterHtml(i), { baseHref: c.href, imageUrl, chapterIndexOf: parsed.chapterIndexOf });
          } catch (err) {
            console.error(err);
            return "";
          }
        });
        const pos = loadPosition(slug, parsed.chapters.length);
        pending.current = pos.progress;
        setBook({ epub: parsed, htmls });
        setCounts(parsed.chapters.map(() => undefined));
        setChapter(pos.chapter);
        setStatus("ready");
      } catch (err) {
        console.error(err);
        if (!cancelled) setStatus("error");
      }
    })();
    return () => {
      cancelled = true;
      urls.forEach((u) => URL.revokeObjectURL(u));
    };
  }, [slug, attempt]);

  const html = book?.htmls[chapter] ?? "";

  // Stage size → layout.
  const twoPage = isTwoPage(size.w);
  const perView = twoPage ? 2 : 1;
  const count = counts[chapter] ?? 0;
  const stageW = Math.min(size.w, twoPage ? SPREAD_MAX : SINGLE_PAGE_MAX);
  const align = useCallback((pg: number) => pg - (pg % perView), [perView]);

  // Exact page counts for every chapter (for "Trang X / Y"), redone whenever the layout changes.
  const layoutKey = `${stageW}:${size.h}:${twoPage}:${settings.fontSize}`;
  const [allCounts, setAllCounts] = useState<{ key: string; counts: number[] } | null>(null);
  useEffect(() => {
    if (status !== "ready" || !book || !stageW || !size.h) return;
    const id = setTimeout(() => {
      const layout = pageLayout(stageW, size.h, twoPage);
      // shortcut: measures all chapters in one go; chunk with requestIdleCallback if very long books stutter
      setAllCounts({ key: layoutKey, counts: book.htmls.map((h) => measureHtml(h, layout, settings.fontSize)) });
    }, 50);
    return () => clearTimeout(id);
  }, [status, book, stageW, size.h, twoPage, settings.fontSize, layoutKey]);
  const exact = allCounts?.key === layoutKey ? allCounts.counts : null;

  const keepPlace = useCallback(() => {
    if (count) pending.current = progressForPage(page, count);
  }, [count, page]);

  const keepPlaceRef = useRef(keepPlace);
  useEffect(() => {
    keepPlaceRef.current = keepPlace;
  }, [keepPlace]);
  useEffect(() => {
    const el = stageRef.current;
    if (!el) return;
    const ro = new ResizeObserver(([e]) => {
      keepPlaceRef.current(); // remember the reading spot before re-pagination
      setSize({ w: Math.floor(e.contentRect.width), h: Math.floor(e.contentRect.height) });
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, [status]);

  const onMeasured = useCallback(
    (n: number) => {
      setCounts((c) => (c[chapter] === n ? c : Object.assign([...c], { [chapter]: n })));
      // Read/clear outside the updater: updaters must stay pure (StrictMode runs them twice).
      const target = pending.current !== null ? pageForProgress(pending.current, n) : null;
      pending.current = null;
      setPage((pg) => align(target ?? Math.min(pg, n - 1)));
    },
    [chapter, align],
  );

  const goTo = useCallback((ch: number, progress: number) => {
    pending.current = progress;
    setChapter(ch);
    setPanel(null);
  }, []);

  const chapterCount = book?.epub.chapters.length ?? 0;
  const next = useCallback(() => {
    if (controlRef.current?.next()) return;
    if (page + perView < count) setPage(page + perView);
    else if (chapter < chapterCount - 1) goTo(chapter + 1, 0);
  }, [page, perView, count, chapter, chapterCount, goTo]);
  const prev = useCallback(() => {
    if (controlRef.current?.prev()) return;
    if (page > 0) setPage(Math.max(0, page - perView));
    else if (chapter > 0) goTo(chapter - 1, 1);
  }, [page, perView, chapter, goTo]);

  // Persist position.
  useEffect(() => {
    if (status === "ready" && count) savePosition(slug, { chapter, progress: progressForPage(page, count) });
  }, [slug, status, chapter, page, count]);

  // Keyboard.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.target as HTMLElement).closest("input, textarea, select")) return;
      if (e.key === "ArrowRight") next();
      else if (e.key === "ArrowLeft") prev();
      else if (e.key === "Escape") setPanel(null);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [next, prev]);

  // Fullscreen.
  useEffect(() => {
    const onFs = () => setFullscreen(document.fullscreenElement === rootRef.current);
    document.addEventListener("fullscreenchange", onFs);
    return () => document.removeEventListener("fullscreenchange", onFs);
  }, []);
  const toggleFullscreen = () => {
    if (document.fullscreenElement) void document.exitFullscreen();
    else void rootRef.current?.requestFullscreen?.();
  };

  // Chrome auto-hide.
  const hideTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const wake = useCallback(() => {
    setChrome(true);
    clearTimeout(hideTimer.current);
    hideTimer.current = setTimeout(() => setChrome(false), CHROME_HIDE_MS);
  }, []);
  useEffect(() => () => clearTimeout(hideTimer.current), []);
  useEffect(() => {
    if (status !== "ready") return;
    clearTimeout(hideTimer.current);
    hideTimer.current = setTimeout(() => setChrome(false), CHROME_HIDE_MS);
  }, [status]);

  const updateSettings = (s: ReaderSettings) => {
    if (s.fontSize !== settings.fontSize) keepPlace();
    setSettings(s);
    saveSettings(s);
  };

  // Global page numbers; chapters not laid out yet use the average of measured ones.
  const known = counts.filter((c): c is number => !!c);
  const avg = known.length ? known.reduce((a, b) => a + b, 0) / known.length : 1;
  const est = (i: number) => exact?.[i] ?? counts[i] ?? avg;
  const startOf = (ch: number) => Array.from({ length: ch }, (_, i) => est(i)).reduce((a, b) => a + b, 0);
  const total = Math.max(1, Math.round(startOf(chapterCount)));
  const current = Math.min(total, Math.round(startOf(chapter)) + page + 1);
  const seek = (v: number) => {
    let ch = 0;
    while (ch < chapterCount - 1 && startOf(ch + 1) <= v) ch++;
    const progress = Math.min(1, Math.max(0, (v - startOf(ch)) / est(ch)));
    if (ch === chapter && count) setPage(align(pageForProgress(progress, count)));
    else goTo(ch, progress);
  };

  const chapterTitle = useMemo(() => {
    const items = book ? flatten(book.epub.toc).filter((t) => t.chapter >= 0 && t.chapter <= chapter) : [];
    return items.length ? items[items.length - 1].label : title;
  }, [book, chapter, title]);

  const colors = THEME_COLORS[settings.theme];
  const flipMode = reducedMotion ? "slide" : settings.flip;

  return (
    <div
      ref={rootRef}
      onPointerMove={wake}
      onPointerDown={wake}
      className="relative flex flex-col overflow-hidden rounded-[1.75rem] shadow-[0_30px_80px_-30px_rgba(59,42,26,0.6)] ring-1 ring-black/5"
      style={{ height: fullscreen ? "100vh" : "85vh", background: colors.bg, color: colors.fg }}
      data-theme={settings.theme}
    >
      <ReaderToolbar
        chapterTitle={chapterTitle}
        visible={chrome || panel !== null || status !== "ready"}
        fullscreen={fullscreen}
        onToc={() => setPanel(panel === "toc" ? null : "toc")}
        onSettings={() => setPanel(panel === "settings" ? null : "settings")}
        onFullscreen={toggleFullscreen}
        onClose={() => {
          if (document.fullscreenElement) void document.exitFullscreen();
          onClose();
        }}
      />

      <div ref={stageRef} className="relative min-h-0 flex-1 px-2 md:px-6">
        {status === "loading" && (
          <div className="flex h-full flex-col items-center justify-center gap-4">
            <div className="h-3/5 w-2/5 max-w-xs animate-pulse rounded-xl bg-current opacity-10" />
            <p className="text-sm opacity-70">{t("reader.opening")}</p>
          </div>
        )}
        {status === "error" && (
          <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
            <p className="font-display text-xl font-bold">{t("reader.openFailed")}</p>
            <p className="max-w-sm text-sm opacity-70">
              {tooLarge ? t("reader.tooLarge") : t("reader.invalid")}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => {
                  setStatus("loading");
                  setTooLarge(false);
                  setAttempt((a) => a + 1);
                }}
                className="rounded-full bg-brick px-5 py-2.5 font-semibold text-white"
              >
                {t("error.retry")}
              </button>
              <a href={epubUrl} target="_blank" rel="noopener noreferrer" className="rounded-full px-5 py-2.5 font-semibold underline-offset-4 hover:underline">
                {t("reader.openOriginal")}
              </a>
            </div>
          </div>
        )}
        {status === "ready" && size.w > 0 && size.h > 0 && (
          <PageStage
            html={html}
            width={stageW}
            height={size.h}
            twoPage={twoPage}
            page={page}
            pageCount={count}
            fontSize={settings.fontSize}
            colors={colors}
            mode={flipMode}
            animate={!reducedMotion}
            onMeasured={onMeasured}
            onPage={(pg) => setPage(align(pg))}
            onNext={next}
            onPrev={prev}
            onChapterLink={(ch) => goTo(ch, 0)}
            controlRef={controlRef}
          />
        )}
        {panel === "settings" && <SettingsPanel settings={settings} reducedMotion={reducedMotion} onChange={updateSettings} />}
        {panel === "toc" && book && <TocDrawer toc={book.epub.toc} current={chapter} onPick={(ch) => goTo(ch, 0)} onClose={() => setPanel(null)} />}
      </div>

      <div className={`flex items-center gap-3 px-5 py-3 text-xs transition-opacity duration-300 ${chrome || status !== "ready" ? "opacity-80" : "opacity-40"}`}>
        <span className="shrink-0 tabular-nums" aria-live="polite">
          {t("reader.page", { current, total })}
        </span>
        <input
          type="range"
          aria-label={t("reader.progress")}
          min={0}
          max={total - 1}
          value={current - 1}
          disabled={status !== "ready"}
          onChange={(e) => seek(Number(e.target.value))}
          className="min-w-0 flex-1 accent-brick"
        />
        <span className="hidden max-w-48 truncate sm:inline">{chapterTitle}</span>
      </div>
    </div>
  );
}
