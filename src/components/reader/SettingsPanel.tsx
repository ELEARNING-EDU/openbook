"use client";

import { THEME_COLORS } from "@/components/reader/themes";
import { useT } from "@/components/i18n/LocaleProvider";
import { FONT_SIZES, THEMES, type ReaderSettings } from "@/lib/reader/storage";

export function SettingsPanel({
  settings,
  reducedMotion,
  onChange,
}: {
  settings: ReaderSettings;
  reducedMotion: boolean;
  onChange(s: ReaderSettings): void;
}) {
  const t = useT();
  const themeLabel = { white: t("reader.themeWhite"), sepia: t("reader.themeSepia"), dark: t("reader.themeDark") };
  const i = FONT_SIZES.indexOf(settings.fontSize);
  const chip = (on: boolean) =>
    `flex-1 rounded-xl px-3 py-2 text-sm font-semibold ring-1 transition ${on ? "ring-2 ring-brick" : "ring-black/10 hover:ring-black/30"}`;
  return (
    <div role="dialog" aria-label={t("reader.settings")} className="absolute top-14 right-3 z-20 w-72 space-y-4 rounded-2xl bg-surface p-4 text-ink shadow-2xl ring-1 ring-black/5">
      <div className="flex items-center gap-2">
        <button type="button" aria-label={t("reader.decreaseFont")} disabled={i <= 0} onClick={() => onChange({ ...settings, fontSize: FONT_SIZES[i - 1] })} className={`${chip(false)} disabled:opacity-40`}>
          A−
        </button>
        <span className="w-12 text-center text-sm tabular-nums">{settings.fontSize}px</span>
        <button type="button" aria-label={t("reader.increaseFont")} disabled={i >= FONT_SIZES.length - 1} onClick={() => onChange({ ...settings, fontSize: FONT_SIZES[i + 1] })} className={`${chip(false)} text-lg disabled:opacity-40`}>
          A+
        </button>
      </div>
      <div className="flex gap-2">
        {THEMES.map((theme) => (
          <button
            key={theme}
            type="button"
            aria-pressed={settings.theme === theme}
            onClick={() => onChange({ ...settings, theme })}
            className={chip(settings.theme === theme)}
            style={{ background: THEME_COLORS[theme].bg, color: THEME_COLORS[theme].fg }}
          >
            {themeLabel[theme]}
          </button>
        ))}
      </div>
      <div className="flex gap-2">
        {(["curl", "slide"] as const).map((f) => (
          <button
            key={f}
            type="button"
            aria-pressed={settings.flip === f}
            disabled={reducedMotion && f === "curl"}
            onClick={() => onChange({ ...settings, flip: f })}
            className={`${chip(settings.flip === f)} disabled:opacity-40`}
          >
            {f === "curl" ? t("reader.flipCurl") : t("reader.flipSlide")}
          </button>
        ))}
      </div>
      {reducedMotion && <p className="text-xs text-ink/60">{t("reader.reducedMotion")}</p>}
    </div>
  );
}
