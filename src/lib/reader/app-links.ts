// NEXT_PUBLIC_* values are inlined at build time; empty means "hide that button".
export function appBookUrl(slug: string): string | null {
  const tpl = process.env.NEXT_PUBLIC_APP_BOOK_URL;
  return tpl ? tpl.replaceAll("{slug}", encodeURIComponent(slug)) : null;
}

export const appStoreUrl = () => process.env.NEXT_PUBLIC_APP_STORE_URL || null;
export const playStoreUrl = () => process.env.NEXT_PUBLIC_PLAY_STORE_URL || null;
