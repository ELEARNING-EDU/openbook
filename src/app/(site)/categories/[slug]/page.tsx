import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { BookGrid, BookGridSkeleton } from "@/components/BookGrid";
import { HeroSlider } from "@/components/HeroSlider";
import { Pagination } from "@/components/Pagination";
import { getCategoryBySlug, getTopDownloaded, listBooks } from "@/lib/queries";
import { parsePage } from "@/lib/validation";

type Props = PageProps<"/categories/[slug]">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const category = await getCategoryBySlug((await params).slug);
  return { title: category?.name ?? "Không tìm thấy" };
}

async function CategoryContent({ params, searchParams }: Props) {
  const [{ slug }, sp] = await Promise.all([params, searchParams]);
  const category = await getCategoryBySlug(slug);
  if (!category) notFound();
  const page = parsePage(sp.page);
  const [top, { books, total, totalPages }] = await Promise.all([
    getTopDownloaded({ categoryId: category.id, limit: 5 }),
    listBooks({ category: slug, page }),
  ]);
  return (
    <div className="space-y-10">
      <div>
        <p className="text-sm font-semibold tracking-wider text-brick uppercase">Thể loại</p>
        <h1 className="text-3xl font-bold md:text-5xl">{category.name}</h1>
        <p className="mt-1 text-ink/70">{total} cuốn sách</p>
      </div>
      <HeroSlider books={top} label={`Sách ${category.name} tải nhiều nhất`} />
      <section>
        <h2 className="mb-5 text-2xl font-bold">Tất cả sách {category.name}</h2>
        <BookGrid books={books} />
        <Pagination page={page} totalPages={totalPages} baseHref={`/categories/${slug}`} />
      </section>
    </div>
  );
}

export default function CategoryPage(props: Props) {
  return (
    <Suspense fallback={<BookGridSkeleton />}>
      <CategoryContent {...props} />
    </Suspense>
  );
}
