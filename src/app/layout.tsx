import type { Metadata } from "next";
import { cookies } from "next/headers";
import { Be_Vietnam_Pro, Playfair_Display } from "next/font/google";
import { LocaleProvider } from "@/components/i18n/LocaleProvider";
import { getT } from "@/lib/i18n/server";
import { LOCALE_COOKIE, parseLocale } from "@/lib/i18n";
import { parseTheme, THEME_COOKIE } from "@/lib/theme";
import "./globals.css";

// Theme and language come from cookies, so the shell cannot be prerendered without them.
export const instant = false;

const playfair = Playfair_Display({
  variable: "--font-playfair",
  subsets: ["latin", "vietnamese"],
});

const beVietnam = Be_Vietnam_Pro({
  variable: "--font-be-vietnam",
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600", "700"],
});

export async function generateMetadata(): Promise<Metadata> {
  const t = await getT();
  return {
    title: { default: "OpenBook", template: "%s · OpenBook" },
    description: t("meta.description"),
  };
}

export default async function RootLayout({ children }: LayoutProps<"/">) {
  const store = await cookies();
  const locale = parseLocale(store.get(LOCALE_COOKIE)?.value);
  const dark = parseTheme(store.get(THEME_COOKIE)?.value) === "dark";
  const htmlClass = [dark && "dark", playfair.variable, beVietnam.variable, "h-full"].filter(Boolean).join(" ");
  return (
    <html lang={locale} className={htmlClass}>
      <body className="min-h-full flex flex-col">
        <LocaleProvider locale={locale}>{children}</LocaleProvider>
      </body>
    </html>
  );
}
