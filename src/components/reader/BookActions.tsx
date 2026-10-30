"use client";

import dynamic from "next/dynamic";
import { useEffect, useState, type MouseEvent } from "react";
import { createPortal } from "react-dom";
import { recordRead } from "@/actions/books";
import { AppPromptDialog } from "@/components/reader/AppPromptDialog";
import type { BookDTO } from "@/lib/queries";
import { isPhoneViewport } from "@/lib/reader/device";

const Reader = dynamic(() => import("@/components/reader/Reader"), {
  ssr: false,
  loading: () => <p className="py-10 text-center text-ink/60">Đang mở sách…</p>,
});

const pill =
  "inline-flex items-center gap-2 rounded-full px-6 py-3 font-semibold shadow-lg transition hover:scale-105 hover:shadow-xl";

export function BookActions({ book }: { book: BookDTO }) {
  const [promptOpen, setPromptOpen] = useState(false);
  const [readerOpen, setReaderOpen] = useState(false);
  useEffect(() => {
    if (readerOpen) document.getElementById("reader")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [readerOpen]);

  // readerOpen only turns true after a click, so document exists whenever this runs.
  const slot = readerOpen ? document.getElementById("reader-slot") : null;

  // Phones never get the in-page reader or direct links: everything goes through the app.
  const phoneGuard = (e: MouseEvent) => {
    if (!isPhoneViewport(window.innerWidth, window.innerHeight)) return false;
    e.preventDefault();
    setPromptOpen(true);
    return true;
  };

  return (
    <>
      <div className="mt-6 flex flex-wrap gap-3">
        {book.epubUrl && (
          <button
            type="button"
            aria-expanded={readerOpen}
            aria-controls="reader"
            onClick={(e) => {
              if (phoneGuard(e)) return;
              if (!readerOpen) void recordRead(book.slug);
              setReaderOpen(true);
            }}
            className={`${pill} bg-ink text-cream`}
          >
            📖 Đọc online
          </button>
        )}
        {book.links.map((l, i) => (
          // Plain <a>: a prefetching <Link> would hit the counter route.
          <a
            key={i}
            href={`/books/${book.slug}/go/${i}`}
            target="_blank"
            rel="noopener noreferrer"
            onClick={phoneGuard}
            className={`${pill} bg-gradient-to-r from-brick to-rose-600 text-white`}
          >
            ⬇ {l.label}
          </a>
        ))}
      </div>
      <AppPromptDialog open={promptOpen} onClose={() => setPromptOpen(false)} book={book} />
      {slot &&
        createPortal(
          <section id="reader" aria-label={`Đọc ${book.title}`} className="scroll-mt-24">
            <Reader slug={book.slug} title={book.title} epubUrl={book.epubUrl} onClose={() => setReaderOpen(false)} />
          </section>,
          slot,
        )}
    </>
  );
}
