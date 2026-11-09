import { describe, expect, it } from "vitest";
import { addDays, dayKey, fillDays, groupWeekly, parseRange, parseSort } from "@/lib/stats";

describe("dayKey", () => {
  it("dùng ngày theo giờ Việt Nam", () => {
    expect(dayKey(new Date("2026-11-03T16:59:59Z"))).toBe("2026-11-03");
    expect(dayKey(new Date("2026-11-03T17:00:00Z"))).toBe("2026-11-04");
  });
});

describe("addDays", () => {
  it("qua tháng và năm", () => {
    expect(addDays("2026-12-31", 1)).toBe("2027-01-01");
    expect(addDays("2026-03-01", -1)).toBe("2026-02-28");
  });
});

describe("fillDays", () => {
  it("điền ngày thiếu bằng 0, giữ thứ tự cũ → mới", () => {
    const days = fillDays("2026-11-04", 3, [{ date: "2026-11-03", reads: 2, downloads: 1 }]);
    expect(days).toEqual([
      { date: "2026-11-02", reads: 0, downloads: 0 },
      { date: "2026-11-03", reads: 2, downloads: 1 },
      { date: "2026-11-04", reads: 0, downloads: 0 },
    ]);
  });
});

describe("groupWeekly", () => {
  it("gom 7 ngày tính từ ngày mới nhất, nhóm cũ nhất có thể thiếu ngày", () => {
    const weeks = groupWeekly(fillDays("2026-11-10", 10, []));
    expect(weeks).toHaveLength(2);
    expect(weeks[0]).toMatchObject({ start: "2026-11-01", end: "2026-11-03" });
    expect(weeks[1]).toMatchObject({ start: "2026-11-04", end: "2026-11-10" });
  });
  it("cộng dồn lượt đọc và lượt tải", () => {
    const days = [
      { date: "2026-11-01", reads: 1, downloads: 2 },
      { date: "2026-11-02", reads: 3, downloads: 4 },
    ];
    expect(groupWeekly(days)).toEqual([{ start: "2026-11-01", end: "2026-11-02", reads: 4, downloads: 6 }]);
  });
});

describe("parseRange/parseSort", () => {
  it("giá trị lạ rơi về mặc định", () => {
    expect(parseRange("7")).toBe(7);
    expect(parseRange("90")).toBe(90);
    expect(parseRange("8")).toBe(30);
    expect(parseRange(undefined)).toBe(30);
    expect(parseSort("downloads")).toBe("downloads");
    expect(parseSort("x")).toBe("reads");
  });
});
