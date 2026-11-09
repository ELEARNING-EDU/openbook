// Pure calendar helpers for admin statistics. Day keys are YYYY-MM-DD in Asia/Ho_Chi_Minh.

const DAY_MS = 86_400_000;
const dayFormatter = new Intl.DateTimeFormat("en-CA", {
  timeZone: "Asia/Ho_Chi_Minh",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

export type DayStat = { date: string; reads: number; downloads: number };
export type PeriodStat = { start: string; end: string; reads: number; downloads: number };

export const STATS_RANGES = [7, 30, 90] as const;
export type StatsRange = (typeof STATS_RANGES)[number];
export type StatsSort = "reads" | "downloads";

export function dayKey(date: Date): string {
  const parts = dayFormatter.formatToParts(date);
  const value = (type: Intl.DateTimeFormatPartTypes) => parts.find((p) => p.type === type)?.value ?? "";
  return `${value("year")}-${value("month")}-${value("day")}`;
}

export function addDays(key: string, days: number): string {
  const [year, month, day] = key.split("-").map(Number);
  return new Date(Date.UTC(year, month - 1, day) + days * DAY_MS).toISOString().slice(0, 10);
}

export function fillDays(endKey: string, days: number, rows: DayStat[]): DayStat[] {
  const byDate = new Map(rows.map((row) => [row.date, row]));
  return Array.from({ length: days }, (_, i) => {
    const date = addDays(endKey, i - days + 1);
    const row = byDate.get(date);
    return row ? { date, reads: row.reads, downloads: row.downloads } : { date, reads: 0, downloads: 0 };
  });
}

// Groups into 7-day bars counted back from the newest day; the oldest bar may be shorter.
export function groupWeekly(days: DayStat[]): PeriodStat[] {
  const weeks: PeriodStat[] = [];
  for (let end = days.length - 1; end >= 0; end -= 7) {
    const chunk = days.slice(Math.max(end - 6, 0), end + 1);
    weeks.unshift({
      start: chunk[0].date,
      end: chunk[chunk.length - 1].date,
      reads: chunk.reduce((n, d) => n + d.reads, 0),
      downloads: chunk.reduce((n, d) => n + d.downloads, 0),
    });
  }
  return weeks;
}

export function parseRange(value: string | string[] | undefined): StatsRange {
  return value === "7" ? 7 : value === "90" ? 90 : 30;
}

export function parseSort(value: string | string[] | undefined): StatsSort {
  return value === "downloads" ? "downloads" : "reads";
}
