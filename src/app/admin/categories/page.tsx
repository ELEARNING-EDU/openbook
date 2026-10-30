import { Suspense } from "react";
import { CategoryManager } from "@/components/admin/CategoryManager";
import { assertAdminPage } from "@/lib/auth-guard";
import { listCategories } from "@/lib/queries";

export const metadata = { title: "Thể loại" };

async function Categories() {
  await assertAdminPage();
  return <CategoryManager categories={await listCategories()} />;
}

export default function AdminCategoriesPage() {
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">Thể loại</h1>
      <Suspense fallback={<p className="text-ink/60">Đang tải…</p>}>
        <Categories />
      </Suspense>
    </div>
  );
}
