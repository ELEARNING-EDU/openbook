export const isTwoPage = (frameWidth: number) => frameWidth >= 1024;

export function pageForProgress(progress: number, pageCount: number): number {
  if (pageCount <= 0) return 0;
  return Math.min(pageCount - 1, Math.max(0, Math.round(progress * pageCount)));
}

export const progressForPage = (page: number, pageCount: number) => (pageCount <= 1 ? 0 : page / pageCount);
