import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";
import { getSessionUser } from "@/lib/auth-guard";
import { getT } from "@/lib/i18n/server";
import { listMenuCategories } from "@/lib/queries";
import { MobileNav } from "@/components/MobileNav";
import { SearchBox } from "@/components/SearchBox";
import { LanguageToggle, ThemeToggle } from "@/components/ThemeToggle";
import { UserMenu } from "@/components/UserMenu";

export function Logo() {
  return (
    <Link href="/" className="flex shrink-0 items-center gap-2" aria-label="OpenBook">
      <span className="grid size-9 place-items-center rounded-xl bg-gradient-to-br from-brick to-rose-500 font-display text-lg font-bold text-white shadow-md">
        O
      </span>
      <span className="font-display text-xl font-bold tracking-tight">
        Open<span className="text-brick">Book</span>
      </span>
    </Link>
  );
}

async function HeaderDynamic() {
  await connection();
  const [user, categories] = await Promise.all([getSessionUser(), listMenuCategories()]);
  return (
    <>
      <div className="hidden items-center gap-1 md:flex">
        <ThemeToggle />
        <LanguageToggle />
        <UserMenu user={user} />
      </div>
      <MobileNav categories={categories} userSlot={<UserMenu user={user} />} />
    </>
  );
}

async function CategoryNav() {
  await connection();
  const [t, categories] = await Promise.all([getT(), listMenuCategories()]);
  return (
    <nav aria-label={t("category.label")} className="no-scrollbar hidden gap-1 overflow-x-auto md:flex">
      <Link href="/books" className="shrink-0 rounded-full px-3 py-1.5 text-sm font-medium hover:bg-tint">
        {t("common.all")}
      </Link>
      {categories.map((c) => (
        <Link key={c.id} href={`/categories/${c.slug}`} className="shrink-0 rounded-full px-3 py-1.5 text-sm hover:bg-tint hover:text-brick">
          {c.name}
        </Link>
      ))}
    </nav>
  );
}

export function Header() {
  return (
    <header className="sticky top-0 z-40 border-b border-ink/5 bg-cream/85 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-3 md:gap-6">
        <Logo />
        <SearchBox className="hidden max-w-md flex-1 md:block" />
        <div className="ml-auto flex items-center">
          <Suspense fallback={<div className="h-10 w-10 md:w-40" />}>
            <HeaderDynamic />
          </Suspense>
        </div>
      </div>
      <div className="mx-auto max-w-7xl px-4 pb-2">
        <Suspense fallback={<div className="hidden h-8 md:block" />}>
          <CategoryNav />
        </Suspense>
      </div>
    </header>
  );
}
