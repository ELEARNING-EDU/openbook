import { beforeEach, describe, expect, it, vi } from "vitest";

const auth = vi.fn();
vi.mock("@/auth", () => ({ auth }));
vi.mock("next/server", () => ({ connection: async () => {} }));

const { requireAdmin, requireUser, getSessionUser } = await import("@/lib/auth-guard");

const session = (email: string) => ({ user: { id: "g-1", name: "Tên", email, image: "https://x/a.png" } });

beforeEach(() => {
  process.env.ADMIN_EMAILS = "boss@x.com";
  auth.mockReset();
});

describe("auth guards", () => {
  it("chưa đăng nhập → UNAUTHORIZED", async () => {
    auth.mockResolvedValue(null);
    expect(await getSessionUser()).toBeNull();
    await expect(requireUser()).rejects.toThrow("UNAUTHORIZED");
    await expect(requireAdmin()).rejects.toThrow("UNAUTHORIZED");
  });
  it("người đọc → user không phải admin, requireAdmin FORBIDDEN", async () => {
    auth.mockResolvedValue(session("reader@x.com"));
    await expect(requireUser()).resolves.toMatchObject({ id: "g-1", isAdmin: false });
    await expect(requireAdmin()).rejects.toThrow("FORBIDDEN");
  });
  it("admin không phân biệt hoa thường", async () => {
    auth.mockResolvedValue(session("Boss@X.com"));
    await expect(requireAdmin()).resolves.toMatchObject({ isAdmin: true, email: "Boss@X.com" });
  });
  it("session thiếu id → coi như chưa đăng nhập", async () => {
    auth.mockResolvedValue({ user: { email: "boss@x.com" } });
    await expect(requireUser()).rejects.toThrow("UNAUTHORIZED");
  });
});
