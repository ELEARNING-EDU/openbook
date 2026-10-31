"use client";

import { useEffect, useLayoutEffect, useRef, type CSSProperties, type PointerEvent, type RefObject } from "react";
import { PAD } from "@/components/reader/themes";

type Props = {
  html: string;
  width: number; // stage width (one or two pages)
  height: number;
  twoPage: boolean;
  page: number; // first visible page of the chapter (even in two-page mode)
  pageCount: number;
  fontSize: number;
  colors: { bg: string; fg: string };
  mode: "curl" | "slide";
  animate: boolean;
  onMeasured(count: number): void;
  onPage(page: number): void; // curl: page-flip moved inside the chapter
  onNext(): void;
  onPrev(): void;
  onChapterLink(chapter: number): void;
  /** Curl mode: lets the reader turn pages through page-flip; returns false at a chapter edge. */
  controlRef: RefObject<{ next(): boolean; prev(): boolean } | null>;
};

export function pageLayout(width: number, height: number, twoPage: boolean) {
  const pageW = Math.floor(width / (twoPage ? 2 : 1));
  return { pageW, textW: pageW - 2 * PAD, textH: height - 2 * PAD };
}

/** Page count of `html` laid out like PageStage does, using an off-screen box. */
export function measureHtml(html: string, layout: ReturnType<typeof pageLayout>, fontSize: number): number {
  const box = document.createElement("div");
  box.className = "reader-columns";
  Object.assign(box.style, {
    position: "fixed",
    left: "-100000px",
    top: "0",
    visibility: "hidden",
    width: `${layout.textW}px`,
    height: `${layout.textH}px`,
    columnWidth: `${layout.textW}px`,
    columnGap: `${2 * PAD}px`,
    columnFill: "auto",
    fontSize: `${fontSize}px`,
  });
  box.innerHTML = html;
  document.body.append(box);
  const count = Math.max(1, Math.round((box.scrollWidth + 2 * PAD) / layout.pageW));
  box.remove();
  return count;
}

/**
 * Lays one chapter out in CSS columns: each column (+2×PAD gap) is exactly one page wide,
 * so page k is the column box shifted left by k × pageWidth. The same box measures the page count.
 */
export function PageStage(p: Props) {
  const perView = p.twoPage ? 2 : 1;
  const { pageW, textW, textH } = pageLayout(p.width, p.height, p.twoPage);
  const boxRef = useRef<HTMLDivElement>(null);
  const curlRef = useRef<HTMLDivElement>(null);
  const swipe = useRef<number | null>(null);
  const cb = useRef(p);
  // Layout effect (not effect) so the measuring layout effect below sees this render's callbacks.
  useLayoutEffect(() => {
    cb.current = p;
  });

  const columns: CSSProperties = {
    width: textW,
    height: textH,
    columnWidth: textW,
    columnGap: 2 * PAD,
    columnFill: "auto",
    fontSize: p.fontSize,
  };

  // Measure after layout whenever anything affecting line breaks changes.
  useLayoutEffect(() => {
    const box = boxRef.current;
    if (!box || textW <= 0 || textH <= 0) return;
    const count = Math.max(1, Math.round((box.scrollWidth + 2 * PAD) / pageW));
    cb.current.onMeasured(count);
  }, [p.html, textW, textH, pageW, p.fontSize]);

  // Curl: page-flip owns these nodes, so they are built outside React.
  useEffect(() => {
    const host = curlRef.current;
    if (p.mode !== "curl" || !host || p.pageCount < 1) return;
    const book = document.createElement("div");
    host.append(book);
    const pages = Array.from({ length: p.pageCount }, (_, k) => {
      const el = document.createElement("div");
      el.className = "reader-page";
      el.style.cssText = `background:${p.colors.bg};color:${p.colors.fg};overflow:hidden;`;
      // page-flip rewrites the page's own style, so an opaque layer keeps the back of a turning page solid.
      const bg = document.createElement("div");
      bg.style.cssText = `position:absolute;inset:0;background:${p.colors.bg};`;
      el.append(bg);
      const inner = document.createElement("div");
      inner.className = "reader-columns";
      Object.assign(inner.style, {
        position: "absolute",
        left: `${PAD}px`,
        top: `${PAD}px`,
        width: `${textW}px`,
        height: `${textH}px`,
        columnWidth: `${textW}px`,
        columnGap: `${2 * PAD}px`,
        columnFill: "auto",
        fontSize: `${p.fontSize}px`,
        transform: `translateX(${-k * pageW}px)`,
      });
      // shortcut: every page holds a copy of the (sanitized) chapter, fine for typical chapters; virtualize if huge books feel slow
      inner.innerHTML = p.html;
      el.append(inner);
      return el;
    });
    let flip: import("page-flip/dist/js/page-flip.module.js").PageFlip | null = null;
    let cancelled = false;
    import("page-flip/dist/js/page-flip.module.js")
      .then(({ PageFlip }) => {
        if (cancelled) return;
        flip = new PageFlip(book, {
          width: pageW,
          height: p.height,
          size: "fixed",
          usePortrait: !p.twoPage,
          showCover: false,
          drawShadow: true,
          maxShadowOpacity: 0.5,
          flippingTime: 700,
          mobileScrollSupport: false,
          startPage: Math.min(p.page, p.pageCount - 1),
        });
        flip.loadFromHTML(pages);
        flip.on("flip", (e) => cb.current.onPage(Number(e.data)));
        const f = flip;
        const perPage = p.twoPage ? 2 : 1;
        p.controlRef.current = {
          next: () => (f.getCurrentPageIndex() + perPage < p.pageCount ? (f.flipNext(), true) : false),
          prev: () => (f.getCurrentPageIndex() > 0 ? (f.flipPrev(), true) : false),
        };
      })
      .catch((err) => console.error("page-flip failed", err));
    return () => {
      cancelled = true;
      p.controlRef.current = null;
      flip?.destroy();
      book.remove();
      host.replaceChildren();
    };
    // p.page is only the start page; later moves come from page-flip itself.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [p.mode, p.html, p.pageCount, pageW, p.height, p.twoPage, p.fontSize, p.colors.bg, p.colors.fg, textW, textH]);

  const atStart = p.page === 0;
  const atEnd = p.page + perView >= p.pageCount;

  const onClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const link = (e.target as HTMLElement).closest("[data-chapter]");
    if (link) {
      e.preventDefault();
      p.onChapterLink(Number(link.getAttribute("data-chapter")));
      return;
    }
    if ((e.target as HTMLElement).closest("a")) return;
    const x = e.clientX - e.currentTarget.getBoundingClientRect().left;
    const zone = x < p.width * 0.2 ? "prev" : x > p.width * 0.8 ? "next" : null;
    // In curl mode page-flip handles taps inside the chapter; we only cross chapter boundaries.
    if (p.mode === "curl" && !((zone === "next" && atEnd) || (zone === "prev" && atStart))) return;
    if (zone === "next") p.onNext();
    if (zone === "prev") p.onPrev();
  };

  const onPointerDown = (e: PointerEvent) => {
    if (p.mode === "slide") swipe.current = e.clientX;
  };
  const onPointerUp = (e: PointerEvent) => {
    if (swipe.current === null) return;
    const dx = e.clientX - swipe.current;
    swipe.current = null;
    if (dx < -50) p.onNext();
    else if (dx > 50) p.onPrev();
  };

  return (
    <div
      className="relative mx-auto select-none"
      style={{ width: pageW * perView, height: p.height, background: p.colors.bg, color: p.colors.fg }}
      onClick={onClick}
      onPointerDown={onPointerDown}
      onPointerUp={onPointerUp}
    >
      {/* Slide view (also the measuring box in curl mode, then kept invisible). */}
      <div
        className="absolute inset-0 overflow-hidden"
        style={{ visibility: p.mode === "slide" ? "visible" : "hidden" }}
        aria-hidden={p.mode !== "slide"}
      >
        <div
          ref={boxRef}
          className="reader-columns absolute"
          style={{
            ...columns,
            left: PAD,
            top: PAD,
            transform: `translateX(${-p.page * pageW}px)`,
            transition: p.animate && p.mode === "slide" ? "transform 300ms ease" : "none",
          }}
          dangerouslySetInnerHTML={{ __html: p.html }}
        />
      </div>
      {p.mode === "curl" && <div ref={curlRef} className="absolute inset-0" />}
      {p.twoPage && (
        <div
          aria-hidden
          className="pointer-events-none absolute inset-y-0 left-1/2 w-12 -translate-x-1/2"
          style={{ background: "linear-gradient(90deg, transparent, rgba(0,0,0,0.12) 50%, transparent)" }}
        />
      )}
    </div>
  );
}
