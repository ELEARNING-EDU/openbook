import { cookies } from "next/headers";
import { DEFAULT_LOCALE, makeT, parseLocale, LOCALE_COOKIE, type Locale, type Translate } from "@/lib/i18n";

export async function getLocale(): Promise<Locale> {
  try {
    return parseLocale((await cookies()).get(LOCALE_COOKIE)?.value);
  } catch {
    // Outside a request scope (unit tests) there are no cookies; fall back to the default.
    return DEFAULT_LOCALE;
  }
}

export async function getT(): Promise<Translate> {
  return makeT(await getLocale());
}
