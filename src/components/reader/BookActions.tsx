"use client";

import dynamic from "next/dynamic";
import { useEffect, useState, type MouseEvent } from "react";
import { createPortal } from "react-dom";
import { recordRead } from "@/actions/books";
import { useT } from "@/components/i18n/LocaleProvider";
import { AppPromptDialog } from "@/components/reader/AppPromptDialog";
import type { BookDTO } from "@/lib/queries";
import { isPhoneViewport } from "@/lib/reader/device";

function ReaderLoading() {
  const t = useT();
  return <p className="py-10 text-center text-ink/60">{t("reader.opening")}</p>;
}

const Reader = dynamic(() => import("@/components/reader/Reader"), {
  ssr: false,
  loading: () => <ReaderLoading />,
});

const pill =
  "inline-flex items-center gap-2 rounded-full px-6 py-3 font-semibold shadow-lg transition hover:scale-105 hover:shadow-xl";

export function BookActions({ book }: { book: BookDTO }) {
  const t = useT();
  const [promptOpen, setPromptOpen] = useState(false);
  const [readerOpen, setReaderOpen] = useState(false);
  useEffect(() => {
    if (readerOpen) document.getElementById("reader")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [readerOpen]);

  // readerOpen only turns true after a click, so document exists whenever this runs.
  const slot = readerOpen ? document.getElementById("reader-slot") : null;

  // One read per browser session per book; blocked storage still lets the reader open.
  const countRead = () => {
    const key = `read:${book.slug}`;
    try {
      if (sessionStorage.getItem(key)) return;
      sessionStorage.setItem(key, "1");
    } catch {
      // Storage blocked: fall through and let the server decide.
    }
    void recordRead(book.slug);
  };

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
              if (!readerOpen) countRead();
              setReaderOpen(true);
            }}
            className={`${pill} bg-ink text-cream`}
          >
            📖 {t("book.readOnline")}
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
          <section id="reader" aria-label={t("book.readTitle", { title: book.title })} className="scroll-mt-24">
            <Reader slug={book.slug} title={book.title} epubUrl={book.epubUrl} onClose={() => setReaderOpen(false)} />
          </section>,
          slot,
        )}
    </>
  );
}
