import type { Metadata } from "next";
import { Suspense } from "react";
import { AdminNav } from "@/components/admin/AdminNav";
import { ToastProvider } from "@/components/admin/Toast";
import { Logo } from "@/components/Header";
import { assertAdminPage } from "@/lib/auth-guard";

export const metadata: Metadata = { title: { default: "Quản trị", template: "%s · Quản trị OpenBook" }, robots: { index: false } };

// Children render only after the server-side admin check passes.
async function AdminGate({ children }: { children: React.ReactNode }) {
  const user = await assertAdminPage();
  return (
    <>
      <p className="mb-6 text-sm text-ink/60">
        Xin chào, <span className="font-semibold text-ink">{user.name}</span>
      </p>
      {children}
    </>
  );
}

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    <ToastProvider>
      <header className="border-b border-ink/5 bg-cream/85 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center gap-3 px-4 py-3">
          <Logo />
          <span className="rounded-full bg-moss px-2.5 py-0.5 text-xs font-semibold text-white">Quản trị</span>
        </div>
      </header>
      <div className="mx-auto flex w-full max-w-7xl flex-1 gap-8 px-4 py-6 pb-24 md:py-10">
        <Suspense fallback={<div className="hidden w-56 shrink-0 md:block" />}>
          <AdminNav />
        </Suspense>
        <main className="min-w-0 flex-1">
          <Suspense fallback={<p className="text-ink/60">Đang kiểm tra quyền truy cập…</p>}>
            <AdminGate>{children}</AdminGate>
          </Suspense>
        </main>
      </div>
    </ToastProvider>
  );
}
