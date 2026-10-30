import { Suspense } from "react";
import { BookForm } from "@/components/admin/BookForm";
import { assertAdminPage } from "@/lib/auth-guard";
import { listCategories } from "@/lib/queries";

export const metadata = { title: "Thêm sách" };

async function NewBookForm() {
  await assertAdminPage();
  return <BookForm categories={await listCategories()} />;
}

export default function NewBookPage() {
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">Thêm sách</h1>
      <Suspense fallback={<p className="text-ink/60">Đang tải…</p>}>
        <NewBookForm />
      </Suspense>
    </div>
  );
}
