"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import type { BookDTO } from "@/lib/queries";
import { Cover } from "@/components/Cover";
import { Stars } from "@/components/Stars";

const GRADIENTS = [
  "from-orange-200 via-rose-200 to-amber-100",
  "from-rose-200 via-amber-100 to-orange-200",
  "from-amber-200 via-orange-100 to-rose-200",
  "from-lime-100 via-amber-100 to-orange-200",
];
const INTERVAL_MS = 5000;

export function HeroSlider({ books, label = "Sách tải nhiều nhất" }: { books: BookDTO[]; label?: string }) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);

  const goTo = useCallback((i: number) => {
    const track = trackRef.current;
    const slide = track?.children[i] as HTMLElement | undefined;
    if (track && slide) track.scrollTo({ left: slide.offsetLeft, behavior: "smooth" });
  }, []);

  // Track the visible slide (covers swipe, buttons and autoplay alike).
  useEffect(() => {
    const track = trackRef.current;
    if (!track) return;
    const io = new IntersectionObserver(
      (entries) => {
        for (const e of entries) if (e.isIntersecting) setIndex(Number((e.target as HTMLElement).dataset.index));
      },
      { root: track, threshold: 0.6 },
    );
    Array.from(track.children).forEach((c) => io.observe(c));
    return () => io.disconnect();
  }, [books.length]);

  useEffect(() => {
    if (paused || books.length < 2) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const id = setInterval(() => {
      if (!document.hidden) goTo((index + 1) % books.length);
    }, INTERVAL_MS);
    return () => clearInterval(id);
  }, [paused, index, books.length, goTo]);

  if (books.length === 0) return null;

  const arrow =
    "grid size-11 place-items-center rounded-full bg-white/80 text-xl text-ink shadow-md backdrop-blur transition hover:bg-white hover:text-brick focus-visible:outline-2 focus-visible:outline-brick";

  return (
    <section
      aria-roledescription="carousel"
      aria-label={label}
      className="relative"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={(e) => !e.currentTarget.contains(e.relatedTarget) && setPaused(false)}
    >
      <div
        ref={trackRef}
        className="no-scrollbar flex snap-x snap-mandatory overflow-x-auto scroll-smooth rounded-[2rem] shadow-[0_24px_60px_-28px_rgba(194,65,12,0.55)]"
      >
        {books.map((b, i) => (
          <article
            key={b.id}
            data-index={i}
            aria-roledescription="slide"
            aria-label={`${i + 1} / ${books.length}: ${b.title}`}
            className={`relative w-full shrink-0 snap-center bg-gradient-to-br ${GRADIENTS[i % GRADIENTS.length]}`}
          >
            <div className="pointer-events-none absolute -top-16 -right-16 size-64 rounded-full bg-white/30 blur-2xl" />
            <div className="relative flex min-h-[420px] flex-col-reverse items-center gap-6 px-6 pt-8 pb-16 md:min-h-[480px] md:flex-row md:gap-12 md:px-16 md:py-12">
              <div className="flex-1 text-center md:text-left">
                <p className="inline-flex items-center gap-1.5 rounded-full bg-white/70 px-3 py-1 text-xs font-semibold text-brick">
                  🔥 #{i + 1} · {b.downloadCount.toLocaleString("vi-VN")} lượt tải
                </p>
                <h2 className="mt-3 text-3xl leading-tight font-bold md:text-5xl">{b.title}</h2>
                <p className="mt-2 text-ink/75 md:text-lg">{b.author}</p>
                <div className="mt-2">
                  <Stars value={b.ratingAvg} />
                </div>
                {b.description && (
                  <p className="mx-auto mt-3 line-clamp-3 max-w-xl text-sm text-ink/80 md:mx-0 md:text-base">
                    {b.description}
                  </p>
                )}
                <Link
                  href={`/books/${b.slug}`}
                  tabIndex={i === index ? 0 : -1}
                  className="mt-5 inline-flex items-center gap-2 rounded-full bg-gradient-to-r from-brick to-rose-600 px-6 py-3 font-semibold text-white shadow-lg transition hover:scale-105 hover:shadow-xl"
                >
                  Xem sách →
                </Link>
              </div>
              <div className="w-36 shrink-0 -rotate-3 transition duration-500 hover:rotate-0 md:w-56">
                <Cover src={b.coverUrl} alt={`Bìa sách ${b.title}`} className="rounded-2xl shadow-2xl ring-4 ring-white/60" />
              </div>
            </div>
          </article>
        ))}
      </div>

      {books.length > 1 && (
        <>
          <div className="absolute inset-y-0 left-3 hidden items-center md:flex">
            <button type="button" aria-label="Sách trước" className={arrow} onClick={() => goTo((index - 1 + books.length) % books.length)}>
              ‹
            </button>
          </div>
          <div className="absolute inset-y-0 right-3 hidden items-center md:flex">
            <button type="button" aria-label="Sách sau" className={arrow} onClick={() => goTo((index + 1) % books.length)}>
              ›
            </button>
          </div>
          <div className="absolute inset-x-0 bottom-5 flex justify-center gap-2">
            {books.map((b, i) => (
              <button
                key={b.id}
                type="button"
                aria-label={`Tới sách ${i + 1}`}
                aria-current={i === index ? "true" : undefined}
                onClick={() => goTo(i)}
                className={`h-2.5 rounded-full transition-all ${i === index ? "w-8 bg-brick" : "w-2.5 bg-ink/25 hover:bg-ink/40"}`}
              />
            ))}
          </div>
        </>
      )}
    </section>
  );
}
