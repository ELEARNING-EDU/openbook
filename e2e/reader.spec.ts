import { expect, test } from "@playwright/test";

test("trang chủ có slider và sách nổi bật", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Sách nổi bật" })).toBeVisible();
  const carousel = page.locator('[aria-roledescription="carousel"]');
  await expect(carousel).toBeVisible();
  await carousel.getByRole("button", { name: "Tới sách 2" }).click();
  await expect(carousel.getByRole("button", { name: "Tới sách 2" })).toHaveAttribute("aria-current", "true");
});

test("mở chi tiết sách từ thẻ sách", async ({ page }) => {
  await page.goto("/books");
  await page.locator("main a[href^='/books/']").first().click();
  await expect(page).toHaveURL(/\/books\/[a-z0-9-]+$/);
  const read = page.locator("a[href*='/go/0']");
  await expect(read).toHaveAttribute("target", "_blank");
  await expect(read).toHaveAttribute("rel", /noopener/);
});

test("tìm kiếm với ký tự đặc biệt không lỗi", async ({ page }) => {
  const res = await page.goto("/books?q=(");
  expect(res?.status()).toBe(200);
  await expect(page.getByText("Không tìm thấy sách")).toBeVisible();
  await expect(page.getByText("Đã có lỗi xảy ra")).toHaveCount(0);
});

test("trang thể loại có slider", async ({ page }) => {
  await page.goto("/");
  const href = await page.locator("a[href^='/categories/']").first().getAttribute("href");
  await page.goto(href!);
  await expect(page.locator('[aria-roledescription="carousel"]')).toBeVisible();
});

test("link tải sai chỉ số → 404", async ({ request, page }) => {
  await page.goto("/books");
  const href = await page.locator("main a[href^='/books/']").first().getAttribute("href");
  expect((await request.get(`${href}/go/99`, { maxRedirects: 0 })).status()).toBe(404);
});

test("/admin khi chưa đăng nhập chuyển tới trang đăng nhập", async ({ page }) => {
  await page.goto("/admin/books");
  await expect(page).toHaveURL(/\/api\/auth\/signin/);
});

test("menu hamburger chỉ hiện trên mobile", async ({ page, isMobile }) => {
  await page.goto("/");
  const burger = page.getByRole("button", { name: "Mở menu" });
  if (isMobile) {
    await expect(burger).toBeVisible();
    await burger.click();
    await expect(page.locator("#mobile-drawer")).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(page.locator("#mobile-drawer")).toHaveCount(0);
  } else {
    await expect(burger).toBeHidden();
  }
});
