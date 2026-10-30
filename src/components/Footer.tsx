import { Logo } from "@/components/Header";

export function Footer() {
  return (
    <footer className="mt-20 border-t border-ink/10 bg-gradient-to-b from-transparent to-orange-100/60">
      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-10 md:flex-row md:items-center md:justify-between">
        <Logo />
        <p className="text-sm text-ink/70">Thư viện sách mở — đọc và tải sách miễn phí, mãi mãi.</p>
      </div>
    </footer>
  );
}
