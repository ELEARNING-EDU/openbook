export const FONT_SIZES = [16, 18, 20, 22, 24] as const;
export const THEMES = ["white", "sepia", "dark"] as const;
export const FLIPS = ["curl", "slide"] as const;

export type ReaderSettings = {
  fontSize: (typeof FONT_SIZES)[number];
  theme: (typeof THEMES)[number];
  flip: (typeof FLIPS)[number];
};
export type ReaderPosition = { chapter: number; progress: number };

export const DEFAULT_SETTINGS: ReaderSettings = { fontSize: 18, theme: "sepia", flip: "curl" };
const SETTINGS_KEY = "openbook:reader:settings";
const posKey = (slug: string) => `openbook:reader:pos:${slug}`;

function read(key: string): Record<string, unknown> | null {
  try {
    const v = JSON.parse(localStorage.getItem(key) ?? "null");
    return v && typeof v === "object" ? v : null;
  } catch {
    return null;
  }
}

const write = (key: string, value: unknown) => {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // storage full or blocked (private mode): reading still works without persistence
  }
};

const oneOf = <T>(list: readonly T[], v: unknown): v is T => list.includes(v as T);

export function loadSettings(): ReaderSettings {
  const s = read(SETTINGS_KEY);
  if (s && oneOf(FONT_SIZES, s.fontSize) && oneOf(THEMES, s.theme) && oneOf(FLIPS, s.flip))
    return { fontSize: s.fontSize, theme: s.theme, flip: s.flip };
  return DEFAULT_SETTINGS;
}

export const saveSettings = (s: ReaderSettings) => write(SETTINGS_KEY, s);

export function loadPosition(slug: string, chapterCount: number): ReaderPosition {
  const p = read(posKey(slug));
  const { chapter, progress } = p ?? {};
  if (Number.isInteger(chapter) && (chapter as number) >= 0 && (chapter as number) < chapterCount && typeof progress === "number" && progress >= 0 && progress <= 1)
    return { chapter: chapter as number, progress };
  return { chapter: 0, progress: 0 };
}

export const savePosition = (slug: string, pos: ReaderPosition) => write(posKey(slug), pos);
