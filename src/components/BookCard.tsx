import Link from "next/link";
import type { BookDTO } from "@/lib/queries";
import { Cover } from "@/components/Cover";
import { Stars } from "@/components/Stars";

export function BookCard({ book }: { book: BookDTO }) {
  return (
    <Link
      href={`/books/${book.slug}`}
      className="group block rounded-2xl bg-white/70 p-2.5 shadow-[0_6px_20px_-8px_rgba(59,42,26,0.25)] ring-1 ring-ink/5 transition duration-300 hover:-translate-y-1 hover:-rotate-1 hover:shadow-[0_18px_36px_-12px_rgba(194,65,12,0.4)] focus-visible:outline-2 focus-visible:outline-brick"
    >
      <Cover src={book.coverUrl} alt={`Bìa sách ${book.title}`} className="rounded-xl" />
      <div className="px-1 pt-3 pb-1">
        <h3 className="line-clamp-2 font-display text-base leading-snug font-semibold group-hover:text-brick">
          {book.title}
        </h3>
        <p className="mt-1 truncate text-sm text-ink/70">{book.author}</p>
        <div className="mt-1.5 flex items-center gap-1.5">
          <Stars value={book.ratingAvg} />
          <span className="text-xs text-ink/60">({book.ratingCount})</span>
        </div>
      </div>
    </Link>
  );
}
