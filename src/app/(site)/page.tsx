import { connection } from "next/server";
import { Suspense } from "react";
import { BookGridSkeleton } from "@/components/BookGrid";
import { BookRow } from "@/components/BookRow";
import { HeroSlider } from "@/components/HeroSlider";
import { Reveal } from "@/components/Reveal";
import { SearchBox } from "@/components/SearchBox";
import { getT } from "@/lib/i18n/server";
import { getCategorySections, getFeaturedBooks, getLatestBooks, getTopDownloaded } from "@/lib/queries";

async function TopSlider() {
  await connection();
  return <HeroSlider books={await getTopDownloaded({ limit: 8 })} />;
}

async function HomeSections() {
  await connection();
  const t = await getT();
  const [featured, latest, sections] = await Promise.all([getFeaturedBooks(), getLatestBooks(), getCategorySections()]);
  return (
    <div className="space-y-14 md:space-y-20">
      {featured.length > 0 && (
        <Reveal>
          <BookRow title={t("home.featured")} books={featured} horizontal />
        </Reveal>
      )}
      <Reveal>
        <BookRow title={t("home.latest")} books={latest} horizontal />
      </Reveal>
      {sections.map((s) => (
        <Reveal key={s.category.id}>
          <BookRow title={s.category.name} href={`/categories/${s.category.slug}`} books={s.books} horizontal />
        </Reveal>
      ))}
    </div>
  );
}

async function HomeQuestion() {
  const t = await getT();
  return (
    <div className="mx-auto max-w-2xl text-center">
      <p className="font-display text-2xl font-semibold md:text-3xl">{t("home.question")}</p>
      <SearchBox size="lg" className="mt-4" />
    </div>
  );
}

export default function HomePage() {
  return (
    <div className="space-y-10 md:space-y-14">
      <Suspense fallback={<div className="min-h-[420px] animate-pulse rounded-[2rem] bg-gradient-to-br from-tint to-surface md:min-h-[480px]" />}>
        <TopSlider />
      </Suspense>
      <Suspense fallback={<div className="mx-auto h-24 max-w-2xl" />}>
        <HomeQuestion />
      </Suspense>
      <Suspense fallback={<BookGridSkeleton />}>
        <HomeSections />
      </Suspense>
    </div>
  );
}
