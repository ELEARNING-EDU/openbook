import { en, vi, type TKey } from "@/lib/i18n/dictionary";

export type Locale = "vi" | "en";
export const LOCALES: Locale[] = ["vi", "en"];
export const LOCALE_COOKIE = "locale";
export const DEFAULT_LOCALE: Locale = "vi";

export type Translate = (key: TKey, vars?: Record<string, string | number>) => string;

export const parseLocale = (value: string | undefined): Locale =>
  value === "en" || value === "vi" ? value : DEFAULT_LOCALE;

export const localeTag = (locale: Locale) => (locale === "vi" ? "vi-VN" : "en-US");

export function makeT(locale: Locale): Translate {
  const dict = locale === "en" ? en : vi;
  return (key, vars) =>
    vars ? dict[key].replace(/\{(\w+)\}/g, (_, name: string) => String(vars[name] ?? `{${name}}`)) : dict[key];
}
