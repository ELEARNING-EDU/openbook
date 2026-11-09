import { notFound } from "next/navigation";
import { Suspense } from "react";
import { BookForm } from "@/components/admin/BookForm";
import { assertAdminPage } from "@/lib/auth-guard";
import { getT } from "@/lib/i18n/server";
import {
  getBookAllTimeStats,
  getBookById,
  listAuthorOptions,
  listCategories,
  listPublisherOptions,
  listSeriesOptions,
} from "@/lib/queries";

export async function generateMetadata() {
  const t = await getT();
  return { title: t("admin.editBook") };
}

async function EditBookForm({ params }: Pick<PageProps<"/admin/books/[id]">, "params">) {
  await assertAdminPage();
  const { id } = await params;
  const [book, categories, authors, series, publishers] = await Promise.all([
    getBookById(id),
    listCategories(),
    listAuthorOptions(),
    listSeriesOptions(),
    listPublisherOptions(),
  ]);
  if (!book) notFound();
  const stats = await getBookAllTimeStats(book.id);
  return <BookForm book={book} categories={categories} authors={authors} series={series} publishers={publishers} stats={stats} />;
}

export default async function EditBookPage({ params }: PageProps<"/admin/books/[id]">) {
  const t = await getT();
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">{t("admin.editBook")}</h1>
      <Suspense fallback={<p className="text-ink/60">{t("common.loading")}</p>}>
        <EditBookForm params={params} />
      </Suspense>
    </div>
  );
}
