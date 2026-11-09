"use client";

import { useRouter } from "next/navigation";
import { createContext, useContext, useMemo, useTransition, type ReactNode } from "react";
import { makeT, type Locale, type Translate } from "@/lib/i18n";

type Value = { locale: Locale; setLocale(locale: Locale): void };

const LocaleContext = createContext<Value>({ locale: "vi", setLocale: () => {} });

export function LocaleProvider({ locale, children }: { locale: Locale; children: ReactNode }) {
  const router = useRouter();
  const [, startTransition] = useTransition();
  const value = useMemo<Value>(
    () => ({
      locale,
      setLocale(next) {
        // Cookie (not localStorage) so server components render the new language too.
        document.cookie = `locale=${next}; path=/; max-age=31536000; samesite=lax`;
        startTransition(() => router.refresh());
      },
    }),
    [locale, router],
  );
  return <LocaleContext value={value}>{children}</LocaleContext>;
}

export const useLocale = () => useContext(LocaleContext);

export function useT(): Translate {
  const { locale } = useLocale();
  return useMemo(() => makeT(locale), [locale]);
}
