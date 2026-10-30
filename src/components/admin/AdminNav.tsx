"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const ITEMS = [
  { href: "/admin/books", label: "Sách", icon: "📚" },
  { href: "/admin/categories", label: "Thể loại", icon: "🏷️" },
  { href: "/admin/reviews", label: "Bình luận", icon: "💬" },
  { href: "/", label: "Trang chủ", icon: "🏠" },
];

export function AdminNav() {
  const pathname = usePathname();
  const active = (href: string) => href !== "/" && pathname.startsWith(href);
  return (
    <>
      <nav aria-label="Quản trị" className="hidden w-56 shrink-0 md:block">
        <ul className="sticky top-6 space-y-1">
          {ITEMS.map((i) => (
            <li key={i.href}>
              <Link
                href={i.href}
                aria-current={active(i.href) ? "page" : undefined}
                className={`flex items-center gap-3 rounded-xl px-4 py-2.5 font-medium transition ${active(i.href) ? "bg-brick text-white shadow" : "hover:bg-orange-100"}`}
              >
                <span aria-hidden>{i.icon}</span>
                {i.label}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
      <nav aria-label="Quản trị" className="fixed inset-x-0 bottom-0 z-40 border-t border-ink/10 bg-cream/95 backdrop-blur md:hidden">
        <ul className="grid grid-cols-4">
          {ITEMS.map((i) => (
            <li key={i.href}>
              <Link
                href={i.href}
                aria-current={active(i.href) ? "page" : undefined}
                className={`flex flex-col items-center gap-0.5 py-2 text-xs font-medium ${active(i.href) ? "text-brick" : "text-ink/70"}`}
              >
                <span aria-hidden className="text-lg">{i.icon}</span>
                {i.label}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </>
  );
}
