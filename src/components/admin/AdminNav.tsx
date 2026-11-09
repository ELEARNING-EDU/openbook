"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useT } from "@/components/i18n/LocaleProvider";

const ITEMS = [
  { href: "/admin", key: "admin.navOverview", icon: "🧭" },
  { href: "/admin/stats", key: "admin.navStats", icon: "📊" },
  { href: "/admin/books", key: "admin.navBooks", icon: "📚" },
  { href: "/admin/authors", key: "admin.navAuthors", icon: "✍️" },
  { href: "/admin/categories", key: "admin.navCategories", icon: "🏷️" },
  { href: "/admin/reviews", key: "admin.navReviews", icon: "💬" },
  { href: "/", key: "admin.navHome", icon: "🏠" },
] as const;

export function AdminNav() {
  const t = useT();
  const pathname = usePathname();
  // Overview is only active on exactly /admin; the other admin items cover their subpages.
  const active = (href: string) => (href === "/" ? false : href === "/admin" ? pathname === href : pathname.startsWith(href));
  return (
    <>
      <nav aria-label={t("admin.navLabel")} className="hidden w-56 shrink-0 md:block">
        <ul className="sticky top-6 space-y-1">
          {ITEMS.map((item) => (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={active(item.href) ? "page" : undefined}
                className={`flex items-center gap-3 rounded-xl px-4 py-2.5 font-medium transition ${active(item.href) ? "bg-brick text-white shadow" : "hover:bg-tint"}`}
              >
                <span aria-hidden>{item.icon}</span>
                {t(item.key)}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
      <nav aria-label={t("admin.navLabel")} className="fixed inset-x-0 bottom-0 z-40 border-t border-ink/10 bg-cream/95 backdrop-blur md:hidden">
        <ul className="flex w-full overflow-x-auto">
          {ITEMS.map((item) => (
            <li key={item.href} className="min-w-16 flex-1 shrink-0">
              <Link
                href={item.href}
                aria-current={active(item.href) ? "page" : undefined}
                className={`flex min-h-11 flex-col items-center justify-center gap-0.5 px-1 py-1 text-center text-[10px] leading-tight font-medium break-words ${active(item.href) ? "text-brick dark:text-orange-400" : "text-ink/70"}`}
              >
                <span aria-hidden className="text-base">{item.icon}</span>
                {t(item.key)}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </>
  );
}
