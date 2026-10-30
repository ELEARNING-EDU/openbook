"use client";

import Link from "next/link";
import { useEffect, useState, type ReactNode } from "react";
import type { CategoryDTO } from "@/lib/queries";

export function MobileNav({ categories, userSlot }: { categories: CategoryDTO[]; userSlot: ReactNode }) {
  const [open, setOpen] = useState(false);
  // Close on Escape; link clicks close it below (routes stay mounted under Activity).
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open]);

  return (
    <div className="md:hidden">
      <button
        type="button"
        aria-label="Mở menu"
        aria-expanded={open}
        aria-controls="mobile-drawer"
        onClick={() => setOpen(true)}
        className="grid size-10 place-items-center rounded-full hover:bg-orange-100"
      >
        <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
          <path d="M4 7h16M4 12h16M4 17h16" />
        </svg>
      </button>
      {open && (
        <div className="fixed inset-0 z-50">
          <button type="button" aria-label="Đóng menu" className="absolute inset-0 bg-ink/40" onClick={() => setOpen(false)} />
          <nav
            id="mobile-drawer"
            aria-label="Menu"
            onClick={(e) => (e.target as HTMLElement).closest("a") && setOpen(false)}
            className="absolute inset-y-0 right-0 flex w-80 max-w-[85vw] flex-col gap-6 overflow-y-auto bg-cream p-6 shadow-2xl"
          >
            <div className="flex items-center justify-between">
              <span className="font-display text-xl font-bold">Menu</span>
              <button type="button" aria-label="Đóng menu" onClick={() => setOpen(false)} className="grid size-10 place-items-center rounded-full text-2xl hover:bg-orange-100">
                ×
              </button>
            </div>
            <div>{userSlot}</div>
            <div>
              <p className="mb-2 text-xs font-semibold tracking-wider text-ink/60 uppercase">Khám phá</p>
              <ul className="space-y-1">
                <li>
                  <Link href="/books" className="block rounded-xl px-3 py-2 font-medium hover:bg-orange-100">
                    Tất cả sách
                  </Link>
                </li>
                {categories.map((c) => (
                  <li key={c.id}>
                    <Link href={`/categories/${c.slug}`} className="block rounded-xl px-3 py-2 hover:bg-orange-100">
                      {c.name}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </nav>
        </div>
      )}
    </div>
  );
}
