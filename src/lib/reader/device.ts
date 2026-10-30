/** Phones are excluded from the in-page reader; works in either orientation. */
export const isPhoneViewport = (w: number, h: number) => Math.min(w, h) < 600;
