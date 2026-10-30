import type { BookDTO } from "@/lib/queries";
import { BookCard } from "@/components/BookCard";

export function BookGrid({ books }: { books: BookDTO[] }) {
  if (books.length === 0) {
    return <p className="rounded-2xl bg-white/60 p-10 text-center text-ink/70">Không tìm thấy sách</p>;
  }
  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-3 md:gap-6 lg:grid-cols-4 xl:grid-cols-5">
      {books.map((b) => (
        <BookCard key={b.id} book={b} />
      ))}
    </div>
  );
}

export function BookGridSkeleton({ count = 10 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-3 md:gap-6 lg:grid-cols-4 xl:grid-cols-5" aria-hidden>
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="animate-pulse rounded-2xl bg-white/60 p-2.5">
          <div className="aspect-[2/3] rounded-xl bg-orange-100" />
          <div className="mt-3 h-4 w-3/4 rounded bg-orange-100" />
          <div className="mt-2 h-3 w-1/2 rounded bg-orange-100" />
        </div>
      ))}
    </div>
  );
}
