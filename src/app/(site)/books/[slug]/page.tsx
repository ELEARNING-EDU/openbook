import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { BookRow } from "@/components/BookRow";
import { Cover } from "@/components/Cover";
import { BookActions } from "@/components/reader/BookActions";
import { ReviewForm } from "@/components/ReviewForm";
import { ReviewList } from "@/components/ReviewList";
import { Stars } from "@/components/Stars";
import { SignInButton } from "@/components/UserMenu";
import { getSessionUser } from "@/lib/auth-guard";
import { getLocale, getT } from "@/lib/i18n/server";
import { localeTag } from "@/lib/i18n";
import { getBookBySlug, getRelatedBooks, getUserReview, getVisibleReviews, type BookDTO } from "@/lib/queries";

type Props = PageProps<"/books/[slug]">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const t = await getT();
  const book = await getBookBySlug((await params).slug);
  if (!book) return { title: t("book.notFound") };
  return { title: book.title, description: book.description.slice(0, 160) || `${book.title} — ${book.author}` };
}

async function ReviewSection({ book }: { book: BookDTO }) {
  const t = await getT();
  const [user, reviews] = await Promise.all([getSessionUser(), getVisibleReviews(book.id)]);
  const mine = user ? await getUserReview(book.id, user.id) : null;
  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
      <div>
        {user ? (
          <ReviewForm bookId={book.id} initial={mine ? { rating: mine.rating, comment: mine.comment } : undefined} />
        ) : (
          <div className="rounded-2xl bg-surface/70 p-5 ring-1 ring-ink/5">
            <p className="mb-3 text-ink/80">{t("common.signInToReview")}</p>
            <SignInButton label={t("common.signInToReview")} callbackUrl={`/books/${book.slug}`} />
          </div>
        )}
      </div>
      <ReviewList reviews={reviews} />
    </div>
  );
}

async function BookDetail({ params }: Pick<Props, "params">) {
  const t = await getT();
  const locale = await getLocale();
  const book = await getBookBySlug((await params).slug);
  if (!book) notFound();
  const related = await getRelatedBooks(book);
  return (
    <article className="space-y-14">
      <div className="grid gap-8 md:grid-cols-[minmax(0,1fr)_minmax(0,2fr)] md:gap-12">
        <div className="mx-auto w-56 md:w-full md:max-w-xs">
          <Cover src={book.coverUrl} alt={`${t("common.coverAlt", { title: book.title })}`} className="rounded-2xl shadow-[0_24px_50px_-20px_rgba(59,42,26,0.5)]" />
        </div>
        <div>
          <div className="flex flex-wrap gap-2">
            {book.categories.map((c) => (
              <Link key={c.id} href={`/categories/${c.slug}`} className="rounded-full bg-moss/10 px-3 py-1 text-xs font-semibold text-moss hover:bg-moss/20">
                {c.name}
              </Link>
            ))}
          </div>
          <h1 className="mt-3 text-3xl leading-tight font-bold md:text-5xl">{book.title}</h1>
          <p className="mt-2 text-lg text-ink/75">{book.author}</p>
          <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-ink/70">
            <span className="flex items-center gap-2">
              <Stars value={book.ratingAvg} size="lg" />
              <span>{t("book.ratingCount", { n: book.ratingCount })}</span>
            </span>
            <span>🔥 {t("common.downloads", { n: book.downloadCount.toLocaleString(localeTag(locale)) })}</span>
          </div>
          <BookActions book={book} />
          {book.description && <p className="mt-8 max-w-prose whitespace-pre-line leading-relaxed text-ink/85">{book.description}</p>}
        </div>
      </div>
      <div id="reader-slot" />
      {related.length > 0 && <BookRow title={t("book.related")} books={related} horizontal />}
      <section>
        <h2 className="mb-6 text-2xl font-bold md:text-3xl">{t("book.reviews")}</h2>
        <Suspense fallback={<p className="text-ink/60">{t("book.reviewsLoading")}</p>}>
          <ReviewSection book={book} />
        </Suspense>
      </section>
    </article>
  );
}

export default function BookPage({ params }: Props) {
  return (
    <Suspense fallback={<div className="h-96 animate-pulse rounded-2xl bg-surface/50" />}>
      <BookDetail params={params} />
    </Suspense>
  );
}
