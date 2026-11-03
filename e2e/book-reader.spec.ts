import { expect, test } from "@playwright/test";

// Requires `SEED_BASE_URL=http://localhost:3200 npm run seed` (adds the "sach-mau-openbook" sample book).
const BOOK = "/books/sach-mau-openbook";

test("route epub trả file / 404", async ({ request }) => {
  const ok = await request.get(`${BOOK}/epub`);
  expect(ok.status()).toBe(200);
  expect(ok.headers()["content-type"]).toBe("application/epub+zip");
  expect((await request.get("/books/khong-co/epub")).status()).toBe(404);
});

test("máy tính: mở trình đọc, lật trang, đổi nền, nhớ vị trí", async ({ page, isMobile }) => {
  test.skip(isMobile, "chỉ cho máy tính/iPad");
  await page.goto(BOOK);
  await page.getByRole("button", { name: "📖 Đọc online" }).click();
  const reader = page.locator("#reader");
  await expect(reader.getByText("Chương 1, đoạn 1.").filter({ visible: true }).first()).toBeVisible();
  const live = reader.locator("[aria-live]");
  await expect(live).toHaveText(/^Trang 1 \//);

  await reader.getByRole("button", { name: "Cài đặt hiển thị" }).click();
  await reader.getByRole("button", { name: "Tối" }).click();
  await expect(reader.locator(":scope > div").first()).toHaveCSS("background-color", "rgb(28, 28, 30)");
  await page.keyboard.press("Escape");

  await page.keyboard.press("ArrowRight");
  await expect(live).not.toHaveText(/^Trang 1 \//);
  const where = await live.textContent();

  await page.reload();
  await page.getByRole("button", { name: "📖 Đọc online" }).click();
  await expect(page.locator("#reader [aria-live]")).toHaveText(where!);
});

test("máy tính: thanh công cụ tự mờ và hiện lại khi rê chuột", async ({ page, isMobile }) => {
  test.skip(isMobile, "chỉ cho máy tính/iPad");
  await page.goto(BOOK);
  await page.getByRole("button", { name: "📖 Đọc online" }).click();
  const reader = page.locator("#reader");
  const toolbar = reader.getByRole("button", { name: "Mục lục" }).locator("xpath=..");
  await expect(reader.locator("[aria-live]")).toHaveText(/^Trang 1 \//);
  await expect(toolbar).toHaveCSS("opacity", "0", { timeout: 6000 });
  await reader.hover();
  await expect(toolbar).toHaveCSS("opacity", "1");
});

test("máy tính: sách quá lớn báo đúng lỗi", async ({ page, isMobile }) => {
  test.skip(isMobile, "chỉ cho máy tính/iPad");
  await page.route("**/books/*/epub", (route) => route.fulfill({ status: 413, body: "Sách quá lớn" }));
  await page.goto(BOOK);
  await page.getByRole("button", { name: "📖 Đọc online" }).click();
  await expect(page.getByText("Sách quá lớn để đọc trực tiếp. Hãy mở bản gốc.")).toBeVisible();
});

test("điện thoại: mời mở ứng dụng, không mở trình đọc", async ({ page, isMobile }) => {
  test.skip(!isMobile, "chỉ cho điện thoại");
  await page.goto(BOOK);
  await page.getByRole("button", { name: "📖 Đọc online" }).click();
  await expect(page.getByText("Đọc sách này trên ứng dụng OpenBook")).toBeVisible();
  await expect(page.locator("#reader")).toHaveCount(0);
});
