import type { Metadata } from "next";
import { Suspense } from "react";
import { BookGrid, BookGridSkeleton } from "@/components/BookGrid";
import { Pagination } from "@/components/Pagination";
import { listBooks } from "@/lib/queries";
import { parsePage } from "@/lib/validation";

export const metadata: Metadata = { title: "Tất cả sách" };

async function Results({ searchParams }: { searchParams: PageProps<"/books">["searchParams"] }) {
  const sp = await searchParams;
  const q = typeof sp.q === "string" ? sp.q.trim() : "";
  const page = parsePage(sp.page);
  const { books, total, totalPages } = await listBooks({ q, page });
  return (
    <>
      <h1 className="text-3xl font-bold md:text-4xl">{q ? `Kết quả cho “${q}”` : "Tất cả sách"}</h1>
      <p className="mt-1 mb-6 text-ink/70">{total} cuốn sách</p>
      <BookGrid books={books} />
      <Pagination page={page} totalPages={totalPages} baseHref={q ? `/books?q=${encodeURIComponent(q)}` : "/books"} />
    </>
  );
}

export default function BooksPage(props: PageProps<"/books">) {
  return (
    <Suspense fallback={<BookGridSkeleton />}>
      <Results searchParams={props.searchParams} />
    </Suspense>
  );
}
