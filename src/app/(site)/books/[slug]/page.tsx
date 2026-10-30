import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { Cover } from "@/components/Cover";
import { BookActions } from "@/components/reader/BookActions";
import { ReviewForm } from "@/components/ReviewForm";
import { ReviewList } from "@/components/ReviewList";
import { Stars } from "@/components/Stars";
import { SignInButton } from "@/components/UserMenu";
import { getSessionUser } from "@/lib/auth-guard";
import { getBookBySlug, getUserReview, getVisibleReviews, type BookDTO } from "@/lib/queries";

type Props = PageProps<"/books/[slug]">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const book = await getBookBySlug((await params).slug);
  if (!book) return { title: "Không tìm thấy sách" };
  return { title: book.title, description: book.description.slice(0, 160) || `${book.title} — ${book.author}` };
}

async function ReviewSection({ book }: { book: BookDTO }) {
  const [user, reviews] = await Promise.all([getSessionUser(), getVisibleReviews(book.id)]);
  const mine = user ? await getUserReview(book.id, user.id) : null;
  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
      <div>
        {user ? (
          <ReviewForm bookId={book.id} initial={mine ? { rating: mine.rating, comment: mine.comment } : undefined} />
        ) : (
          <div className="rounded-2xl bg-white/70 p-5 ring-1 ring-ink/5">
            <p className="mb-3 text-ink/80">Đăng nhập để chấm điểm và bình luận về cuốn sách này.</p>
            <SignInButton label="Đăng nhập để đánh giá" callbackUrl={`/books/${book.slug}`} />
          </div>
        )}
      </div>
      <ReviewList reviews={reviews} />
    </div>
  );
}

async function BookDetail({ params }: Pick<Props, "params">) {
  const book = await getBookBySlug((await params).slug);
  if (!book) notFound();
  return (
    <article className="space-y-14">
      <div className="grid gap-8 md:grid-cols-[minmax(0,1fr)_minmax(0,2fr)] md:gap-12">
        <div className="mx-auto w-56 md:w-full md:max-w-xs">
          <Cover src={book.coverUrl} alt={`Bìa sách ${book.title}`} className="rounded-2xl shadow-[0_24px_50px_-20px_rgba(59,42,26,0.5)]" />
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
              <span>
                {book.ratingAvg.toFixed(1)} ({book.ratingCount} đánh giá)
              </span>
            </span>
            <span>🔥 {book.downloadCount.toLocaleString("vi-VN")} lượt tải</span>
          </div>
          <BookActions book={book} />
          {book.description && <p className="mt-8 max-w-prose whitespace-pre-line leading-relaxed text-ink/85">{book.description}</p>}
        </div>
      </div>
      <div id="reader-slot" />
      <section>
        <h2 className="mb-6 text-2xl font-bold md:text-3xl">Đánh giá & bình luận</h2>
        <Suspense fallback={<p className="text-ink/60">Đang tải đánh giá…</p>}>
          <ReviewSection book={book} />
        </Suspense>
      </section>
    </article>
  );
}

export default function BookPage({ params }: Props) {
  return (
    <Suspense fallback={<div className="h-96 animate-pulse rounded-2xl bg-white/50" />}>
      <BookDetail params={params} />
    </Suspense>
  );
}
