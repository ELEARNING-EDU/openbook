import { defineConfig, devices } from "@playwright/test";

// Requires a seeded database (`npm run seed`) and .env.local.
const PORT = 3200;

export default defineConfig({
  testDir: "e2e",
  use: { baseURL: `http://localhost:${PORT}` },
  webServer: { command: `npm run build && npx next start -p ${PORT}`, port: PORT, timeout: 300_000, reuseExistingServer: false },
  projects: [
    { name: "mobile", use: { ...devices["Desktop Chrome"], viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true } },
    { name: "desktop", use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 800 } } },
  ],
});
