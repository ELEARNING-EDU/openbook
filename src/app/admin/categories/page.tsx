import Link from "next/link";
import { Suspense } from "react";
import { CategoryManager } from "@/components/admin/CategoryManager";
import { TermManager } from "@/components/admin/TermManager";
import { assertAdminPage } from "@/lib/auth-guard";
import { getT } from "@/lib/i18n/server";
import { listAdminCategories, listAdminPublishers, listAdminSeries } from "@/lib/queries";

export async function generateMetadata() {
  const t = await getT();
  return { title: t("admin.navCategories") };
}

type Tab = "categories" | "series" | "publishers";
const parseTab = (value: string | undefined): Tab =>
  value === "series" || value === "publishers" ? value : "categories";
const one = (value: string | string[] | undefined) => (typeof value === "string" ? value : undefined);

async function Taxonomy({ searchParams }: { searchParams: PageProps<"/admin/categories">["searchParams"] }) {
  const t = await getT();
  await assertAdminPage();
  const tab = parseTab(one((await searchParams).tab));
  const [categories, series, publishers] = await Promise.all([
    listAdminCategories(),
    listAdminSeries(),
    listAdminPublishers(),
  ]);
  const tabs: { id: Tab; label: string; count: number }[] = [
    { id: "categories", label: t("admin.colCategory"), count: categories.length },
    { id: "series", label: t("admin.tabSeries"), count: series.length },
    { id: "publishers", label: t("admin.tabPublishers"), count: publishers.length },
  ];

  return (
    <div className="space-y-6">
      <nav aria-label={t("admin.navCategories")} className="flex flex-wrap gap-2">
        {tabs.map((item) => {
          const active = item.id === tab;
          return (
            <Link
              key={item.id}
              href={item.id === "categories" ? "/admin/categories" : `/admin/categories?tab=${item.id}`}
              aria-current={active ? "page" : undefined}
              className={`inline-flex min-h-11 items-center rounded-full border px-4 text-sm font-semibold transition ${
                active ? "border-ink bg-ink text-cream" : "border-ink/20 bg-surface hover:bg-tint"
              }`}
            >
              {item.label} ({item.count})
            </Link>
          );
        })}
      </nav>

      {tab === "categories" ? (
        <CategoryManager categories={categories} />
      ) : (
        <TermManager key={tab} kind={tab === "series" ? "series" : "publisher"} terms={tab === "series" ? series : publishers} />
      )}
    </div>
  );
}

export default async function AdminCategoriesPage(props: PageProps<"/admin/categories">) {
  const t = await getT();
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">{t("admin.navCategories")}</h1>
      <Suspense fallback={<p className="text-ink/60">{t("common.loading")}</p>}>
        <Taxonomy searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}
