// @vitest-environment jsdom
import { beforeEach, describe, expect, it } from "vitest";
import { isPhoneViewport } from "@/lib/reader/device";
import { isTwoPage, pageForProgress, progressForPage } from "@/lib/reader/paging";
import { DEFAULT_SETTINGS, loadPosition, loadSettings, savePosition, saveSettings } from "@/lib/reader/storage";

beforeEach(() => localStorage.clear());

describe("settings", () => {
  it("mặc định, lưu/đọc, dữ liệu hỏng", () => {
    expect(loadSettings()).toEqual(DEFAULT_SETTINGS);
    expect(DEFAULT_SETTINGS).toEqual({ fontSize: 18, theme: "sepia", flip: "curl" });
    saveSettings({ fontSize: 22, theme: "dark", flip: "slide" });
    expect(loadSettings()).toEqual({ fontSize: 22, theme: "dark", flip: "slide" });
    localStorage.setItem("openbook:reader:settings", "{bad");
    expect(loadSettings()).toEqual(DEFAULT_SETTINGS);
    localStorage.setItem("openbook:reader:settings", JSON.stringify({ fontSize: 99, theme: "dark", flip: "slide" }));
    expect(loadSettings()).toEqual(DEFAULT_SETTINGS);
  });
});

describe("position", () => {
  it("mặc định, hợp lệ, vượt phạm vi, hỏng", () => {
    expect(loadPosition("a", 3)).toEqual({ chapter: 0, progress: 0 });
    savePosition("a", { chapter: 2, progress: 0.4 });
    expect(loadPosition("a", 3)).toEqual({ chapter: 2, progress: 0.4 });
    expect(localStorage.getItem("openbook:reader:pos:a")).toBe(JSON.stringify({ chapter: 2, progress: 0.4 }));
    savePosition("a", { chapter: 5, progress: 0.5 });
    expect(loadPosition("a", 3)).toEqual({ chapter: 0, progress: 0 });
    savePosition("a", { chapter: 1, progress: 1.7 });
    expect(loadPosition("a", 3)).toEqual({ chapter: 0, progress: 0 });
    localStorage.setItem("openbook:reader:pos:a", "nope");
    expect(loadPosition("a", 3)).toEqual({ chapter: 0, progress: 0 });
  });
});

describe("device & paging", () => {
  it("isPhoneViewport", () => {
    expect(isPhoneViewport(390, 844)).toBe(true);
    expect(isPhoneViewport(844, 390)).toBe(true);
    expect(isPhoneViewport(768, 1024)).toBe(false);
    expect(isPhoneViewport(1280, 800)).toBe(false);
  });
  it("pageForProgress / progressForPage / isTwoPage", () => {
    expect(pageForProgress(0.5, 10)).toBe(5);
    expect(pageForProgress(1, 10)).toBe(9);
    expect(pageForProgress(0.3, 0)).toBe(0);
    expect(progressForPage(5, 10)).toBe(0.5);
    expect(progressForPage(0, 1)).toBe(0);
    expect(isTwoPage(1024)).toBe(true);
    expect(isTwoPage(1023)).toBe(false);
  });
});
