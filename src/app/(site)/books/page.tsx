import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { BookGrid, BookGridSkeleton } from "@/components/BookGrid";
import { Pagination } from "@/components/Pagination";
import { listBooks, listCategories } from "@/lib/queries";
import { parsePage } from "@/lib/validation";

export const metadata: Metadata = { title: "Tất cả sách" };

async function Results({ searchParams }: { searchParams: PageProps<"/books">["searchParams"] }) {
  const sp = await searchParams;
  const q = typeof sp.q === "string" ? sp.q.trim() : "";
  const page = parsePage(sp.page);
  const category = typeof sp.category === "string" ? sp.category : "";
  const [{ books, total, totalPages }, categories] = await Promise.all([listBooks({ q, category, page }), listCategories()]);
  const href = (slug: string) => {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (slug) params.set("category", slug);
    const query = params.toString();
    return query ? `/books?${query}` : "/books";
  };
  return (
    <>
      <h1 className="text-3xl font-bold md:text-4xl">{q ? `Kết quả cho “${q}”` : "Tất cả sách"}</h1>
      <p className="mt-1 mb-6 text-ink/70">{total} cuốn sách</p>
      <nav aria-label="Lọc theo thể loại" className="mb-6 flex flex-wrap gap-2">
        {[{ slug: "", name: "Tất cả" }, ...categories].map((c) => (
          <Link
            key={c.slug}
            href={href(c.slug)}
            aria-current={c.slug === category ? "page" : undefined}
            className={`rounded-full px-4 py-2 text-sm font-semibold transition ${c.slug === category ? "bg-brick text-white" : "bg-white/70 hover:bg-orange-100"}`}
          >
            {c.name}
          </Link>
        ))}
      </nav>
      <BookGrid books={books} />
      <Pagination page={page} totalPages={totalPages} baseHref={href(category)} />
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
