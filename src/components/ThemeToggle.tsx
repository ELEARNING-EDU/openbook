"use client";

import { useLocale, useT } from "@/components/i18n/LocaleProvider";

const btn = "grid size-10 place-items-center rounded-full text-lg hover:bg-tint";

export function ThemeToggle() {
  const t = useT();
  const toggle = () => {
    const next = !document.documentElement.classList.contains("dark");
    document.documentElement.classList.toggle("dark", next);
    document.cookie = `theme=${next ? "dark" : "light"}; path=/; max-age=31536000; samesite=lax`;
  };
  return (
    <button type="button" onClick={toggle} aria-label={t("common.themeToggle")} className={btn}>
      {/* Both icons are always rendered; CSS picks the one matching the html class (no hydration mismatch). */}
      <span aria-hidden className="hidden dark:inline">☀️</span>
      <span aria-hidden className="inline dark:hidden">🌙</span>
    </button>
  );
}

export function LanguageToggle() {
  const t = useT();
  const { locale, setLocale } = useLocale();
  return (
    <button
      type="button"
      onClick={() => setLocale(locale === "vi" ? "en" : "vi")}
      aria-label={t("common.languageToggle")}
      className={`${btn} text-xs font-bold tracking-wide`}
    >
      {locale === "vi" ? "EN" : "VI"}
    </button>
  );
}
