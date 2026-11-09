import { Suspense } from "react";
import { BookForm } from "@/components/admin/BookForm";
import { assertAdminPage } from "@/lib/auth-guard";
import { getT } from "@/lib/i18n/server";
import { listAuthorOptions, listCategories, listPublisherOptions, listSeriesOptions } from "@/lib/queries";

export async function generateMetadata() {
  const t = await getT();
  return { title: t("admin.newBook") };
}

async function NewBookForm() {
  await assertAdminPage();
  const [categories, authors, series, publishers] = await Promise.all([
    listCategories(),
    listAuthorOptions(),
    listSeriesOptions(),
    listPublisherOptions(),
  ]);
  return <BookForm categories={categories} authors={authors} series={series} publishers={publishers} />;
}

export default async function NewBookPage() {
  const t = await getT();
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">{t("admin.newBook")}</h1>
      <Suspense fallback={<p className="text-ink/60">{t("common.loading")}</p>}>
        <NewBookForm />
      </Suspense>
    </div>
  );
}
