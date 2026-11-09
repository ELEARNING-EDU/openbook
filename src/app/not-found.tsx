import Link from "next/link";
import { getT } from "@/lib/i18n/server";

export default async function NotFound() {
  const t = await getT();
  return (
    <main className="mx-auto flex max-w-lg flex-1 flex-col items-center justify-center px-4 py-24 text-center">
      <p className="font-display text-7xl font-bold text-brick">404</p>
      <h1 className="mt-4 text-2xl font-bold">{t("error.notFoundTitle")}</h1>
      <p className="mt-2 text-ink/70">{t("error.notFoundBody")}</p>
      <Link href="/" className="mt-6 rounded-full bg-brick px-6 py-3 font-semibold text-white hover:bg-orange-700">
        {t("error.backHome")}
      </Link>
    </main>
  );
}
