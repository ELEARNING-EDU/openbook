import { notFound } from "next/navigation";
import { Suspense } from "react";
import { BookForm } from "@/components/admin/BookForm";
import { assertAdminPage } from "@/lib/auth-guard";
import { getBookById, listCategories } from "@/lib/queries";

export const metadata = { title: "Sửa sách" };

async function EditBookForm({ params }: Pick<PageProps<"/admin/books/[id]">, "params">) {
  await assertAdminPage();
  const { id } = await params;
  const [book, categories] = await Promise.all([getBookById(id), listCategories()]);
  if (!book) notFound();
  return <BookForm book={book} categories={categories} />;
}

export default function EditBookPage({ params }: PageProps<"/admin/books/[id]">) {
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">Sửa sách</h1>
      <Suspense fallback={<p className="text-ink/60">Đang tải…</p>}>
        <EditBookForm params={params} />
      </Suspense>
    </div>
  );
}
