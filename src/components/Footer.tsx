import { Logo } from "@/components/Header";
import { getT } from "@/lib/i18n/server";

export async function Footer() {
  const t = await getT();
  return (
    <footer className="mt-20 border-t border-ink/10 bg-gradient-to-b from-transparent to-tint/60">
      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-10 md:flex-row md:items-center md:justify-between">
        <Logo />
        <p className="text-sm text-ink/70">{t("footer.tagline")}</p>
      </div>
    </footer>
  );
}
