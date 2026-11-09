import type { ReaderSettings } from "@/lib/reader/storage";

export const THEME_COLORS: Record<ReaderSettings["theme"], { bg: string; fg: string }> = {
  white: { bg: "#ffffff", fg: "#1f1f1f" },
  sepia: { bg: "#f6efe0", fg: "#5b4636" },
  dark: { bg: "#1c1c1e", fg: "#e5e5e7" },
};

/** Inner margin of a page; columns use 2×PAD as gap so one page = one column step. */
export const PAD = 40;
