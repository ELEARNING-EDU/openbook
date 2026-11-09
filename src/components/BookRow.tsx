import Link from "next/link";
import type { BookDTO } from "@/lib/queries";
import { BookCard } from "@/components/BookCard";
import { getT } from "@/lib/i18n/server";

export async function BookRow({
  title,
  href,
  books,
  horizontal = false,
}: {
  title: string;
  href?: string;
  books: BookDTO[];
  horizontal?: boolean;
}) {
  const t = await getT();
  return (
    <section>
      <div className="mb-4 flex items-end justify-between gap-4">
        <h2 className="text-2xl font-bold md:text-3xl">{title}</h2>
        {href && (
          <Link href={href} className="shrink-0 text-sm font-semibold text-brick hover:underline">
            {t("common.seeAll")}
          </Link>
        )}
      </div>
      <div
        className={
          horizontal
            ? "no-scrollbar -mx-4 flex snap-x gap-4 overflow-x-auto px-4 pb-2"
            : "no-scrollbar -mx-4 flex snap-x gap-4 overflow-x-auto px-4 pb-2 md:mx-0 md:grid md:grid-cols-3 md:gap-6 md:overflow-visible md:px-0 lg:grid-cols-4 xl:grid-cols-5"
        }
      >
        {books.map((b) => (
          <div key={b.id} className={horizontal ? "w-40 shrink-0 snap-start sm:w-44" : "w-40 shrink-0 snap-start sm:w-44 md:w-auto"}>
            <BookCard book={b} still={horizontal} />
          </div>
        ))}
      </div>
    </section>
  );
}
