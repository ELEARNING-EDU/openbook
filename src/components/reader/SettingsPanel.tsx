"use client";

import { THEME_COLORS } from "@/components/reader/themes";
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
  const i = FONT_SIZES.indexOf(settings.fontSize);
  const chip = (on: boolean) =>
    `flex-1 rounded-xl px-3 py-2 text-sm font-semibold ring-1 transition ${on ? "ring-2 ring-brick" : "ring-black/10 hover:ring-black/30"}`;
  return (
    <div role="dialog" aria-label="Cài đặt hiển thị" className="absolute top-14 right-3 z-20 w-72 space-y-4 rounded-2xl bg-white p-4 text-ink shadow-2xl ring-1 ring-black/5">
      <div className="flex items-center gap-2">
        <button type="button" aria-label="Giảm cỡ chữ" disabled={i <= 0} onClick={() => onChange({ ...settings, fontSize: FONT_SIZES[i - 1] })} className={`${chip(false)} disabled:opacity-40`}>
          A−
        </button>
        <span className="w-12 text-center text-sm tabular-nums">{settings.fontSize}px</span>
        <button type="button" aria-label="Tăng cỡ chữ" disabled={i >= FONT_SIZES.length - 1} onClick={() => onChange({ ...settings, fontSize: FONT_SIZES[i + 1] })} className={`${chip(false)} text-lg disabled:opacity-40`}>
          A+
        </button>
      </div>
      <div className="flex gap-2">
        {THEMES.map((t) => (
          <button
            key={t}
            type="button"
            aria-pressed={settings.theme === t}
            onClick={() => onChange({ ...settings, theme: t })}
            className={chip(settings.theme === t)}
            style={{ background: THEME_COLORS[t].bg, color: THEME_COLORS[t].fg }}
          >
            {THEME_COLORS[t].label}
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
            {f === "curl" ? "Cong trang" : "Trượt"}
          </button>
        ))}
      </div>
      {reducedMotion && <p className="text-xs text-ink/60">Thiết bị đang bật giảm chuyển động nên dùng kiểu Trượt.</p>}
    </div>
  );
}
