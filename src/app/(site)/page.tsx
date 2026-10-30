import { connection } from "next/server";
import { Suspense } from "react";
import { BookGrid, BookGridSkeleton } from "@/components/BookGrid";
import { BookRow } from "@/components/BookRow";
import { HeroSlider } from "@/components/HeroSlider";
import { Reveal } from "@/components/Reveal";
import { SearchBox } from "@/components/SearchBox";
import { getCategorySections, getFeaturedBooks, getLatestBooks, getTopDownloaded } from "@/lib/queries";

async function TopSlider() {
  await connection();
  return <HeroSlider books={await getTopDownloaded({ limit: 8 })} />;
}

async function HomeSections() {
  await connection();
  const [featured, latest, sections] = await Promise.all([getFeaturedBooks(), getLatestBooks(), getCategorySections()]);
  return (
    <div className="space-y-14 md:space-y-20">
      {featured.length > 0 && (
        <Reveal>
          <h2 className="mb-5 text-2xl font-bold md:text-3xl">Sách nổi bật</h2>
          <BookGrid books={featured} />
        </Reveal>
      )}
      <Reveal>
        <h2 className="mb-5 text-2xl font-bold md:text-3xl">Mới cập nhật</h2>
        <BookGrid books={latest} />
      </Reveal>
      {sections.map((s) => (
        <Reveal key={s.category.id}>
          <BookRow title={s.category.name} href={`/categories/${s.category.slug}`} books={s.books} />
        </Reveal>
      ))}
    </div>
  );
}

export default function HomePage() {
  return (
    <div className="space-y-10 md:space-y-14">
      <Suspense fallback={<div className="min-h-[420px] animate-pulse rounded-[2rem] bg-gradient-to-br from-orange-100 to-rose-100 md:min-h-[480px]" />}>
        <TopSlider />
      </Suspense>
      <div className="mx-auto max-w-2xl text-center">
        <p className="font-display text-2xl font-semibold md:text-3xl">Hôm nay bạn muốn đọc gì?</p>
        <SearchBox size="lg" className="mt-4" />
      </div>
      <Suspense fallback={<BookGridSkeleton />}>
        <HomeSections />
      </Suspense>
    </div>
  );
}
