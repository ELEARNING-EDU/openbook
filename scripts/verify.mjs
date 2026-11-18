// Evidence run for agents: in-memory MongoDB + seed + `next dev`, then one screenshot per
// feature-map entry and viewport. Never reads or writes the database in .env.local.
// Usage: node scripts/verify.mjs [id-substring ...]   → test-results/verify/
import { spawn } from "node:child_process";
import { randomBytes } from "node:crypto";
import { mkdirSync, writeFileSync } from "node:fs";
import { chromium } from "@playwright/test";
import { MongoMemoryServer } from "mongodb-memory-server";
import { encode } from "next-auth/jwt";

const PORT = Number(process.env.VERIFY_PORT ?? 3100);
const BASE = `http://localhost:${PORT}`;
const OUT = "test-results/verify";
const SECRET = randomBytes(32).toString("base64");
const ADMIN = "admin@verify.local";
const COOKIE = "authjs.session-token";
const VIEWPORTS = {
  mobile: { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true },
  desktop: { viewport: { width: 1280, height: 800 } },
};
// Keep ids in sync with docs/feature-map.md. who: guest | reader | admin.
const PAGES = [
  { id: "home", path: "/" },
  { id: "books", path: "/books" },
  { id: "books-filter", path: "/books?category=triet-hoc" },
  { id: "search-special", path: "/books?q=(" },
  { id: "category", path: "/categories/van-hoc-nuoc-ngoai" },
  { id: "book", path: "/books/pride-and-prejudice" },
  { id: "book-reader", path: "/books/sach-mau-openbook" },
  { id: "book-signed-in", path: "/books/pride-and-prejudice", who: "reader" },
  { id: "not-found", path: "/khong-co-trang-nay", status: 404 },
  { id: "privacy", path: "/privacy.html" },
  { id: "admin-guest", path: "/admin/books", url: /\/api\/auth\/signin/ },
  // notFound() in the streamed admin layout renders the 404 UI with HTTP 200, so assert the UI.
  { id: "admin-reader", path: "/admin/books", who: "reader", text: "Không tìm thấy trang" },
  { id: "admin", path: "/admin", who: "admin" },
  { id: "admin-stats", path: "/admin/stats", who: "admin" },
  { id: "admin-books", path: "/admin/books", who: "admin" },
  { id: "admin-book-new", path: "/admin/books/new", who: "admin" },
  { id: "admin-categories", path: "/admin/categories", who: "admin" },
  { id: "admin-authors", path: "/admin/authors", who: "admin" },
  { id: "admin-reviews", path: "/admin/reviews", who: "admin" },
];
const ERROR_TEXT = ["Đã có lỗi xảy ra", "Something went wrong"];
// Expected on entries that intentionally 404/redirect: the 404 document itself, and next dev's
// Cache Components "instant" validation complaining that redirect()/notFound() stopped the segment.
const EXPECTED_NOISE = [/Failed to load resource: .* 404/, /validate `?instant/];

const filters = process.argv.slice(2);
const pages = filters.length ? PAGES.filter((p) => filters.some((f) => p.id.includes(f))) : PAGES;
if (!pages.length) throw new Error(`No feature-map id matches: ${filters.join(", ")}`);

function run(cmd, args, env, { detached = false } = {}) {
  const child = spawn(cmd, args, { env: { ...process.env, ...env }, stdio: ["ignore", "pipe", "pipe"], detached });
  let log = "";
  child.stdout.on("data", (d) => (log += d));
  child.stderr.on("data", (d) => (log += d));
  const done = new Promise((resolve) => child.on("exit", (code) => resolve({ code, log })));
  return { child, done, log: () => log };
}

const mongo = await MongoMemoryServer.create();
const MONGODB_URI = mongo.getUri("openbook");
// The env var wins over .env.local (process.loadEnvFile and Next.js never override), so this guard is the whole safety story.
if (!/^mongodb:\/\/127\.0\.0\.1:\d+\//.test(MONGODB_URI)) throw new Error(`Refusing non-local DB: ${MONGODB_URI}`);
const env = { MONGODB_URI, AUTH_SECRET: SECRET, ADMIN_EMAILS: ADMIN, AUTH_TRUST_HOST: "true", NEXT_TELEMETRY_DISABLED: "1" };

let server;
let failed = 0;
try {
  const seed = await run("npx", ["tsx", "scripts/seed.ts"], { ...env, SEED_BASE_URL: BASE }).done;
  if (seed.code !== 0) throw new Error(`seed failed:\n${seed.log}`);

  // next dev sometimes exits right after printing an upgrade notice; a restart gets past it.
  for (let attempt = 1; ; attempt++) {
    server = run("npx", ["next", "dev", "-p", String(PORT)], env, { detached: true });
    const deadline = Date.now() + 180_000;
    let up = false;
    while (!up && server.child.exitCode === null && Date.now() < deadline) {
      up = await fetch(BASE).then((r) => r.status < 500, () => false);
      if (!up) await new Promise((r) => setTimeout(r, 1000));
    }
    if (up) break;
    if (attempt === 3 || server.child.exitCode === null) throw new Error(`next dev did not start:\n${server.log()}`);
  }

  const cookieFor = async (who) => {
    if (!who || who === "guest") return [];
    const email = who === "admin" ? ADMIN : "reader@verify.local";
    const value = await encode({ salt: COOKIE, secret: SECRET, token: { sub: `verify-${who}`, email, name: `Verify ${who}`, picture: null } });
    return [{ name: COOKIE, value, url: BASE }];
  };

  mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch();
  const results = [];
  for (const p of pages) {
    for (const [vpName, vp] of Object.entries(VIEWPORTS)) {
      const ctx = await browser.newContext(vp);
      await ctx.addCookies(await cookieFor(p.who));
      const page = await ctx.newPage();
      const errors = [];
      page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
      page.on("console", (m) => m.type() === "error" && errors.push(`console: ${m.text()}`));
      // next dev compiles each route on first hit, which can exceed Playwright's 30s default.
      const res = await page.goto(BASE + p.path, { waitUntil: "domcontentloaded", timeout: 120_000 }).catch((e) => {
        errors.push(`navigation: ${e.message}`);
        return null;
      });
      await page.waitForLoadState("networkidle", { timeout: 15_000 }).catch(() => {});
      // Scroll-reveal sections stay transparent until observed; force them visible for the screenshot.
      // The next dev badge covers content; console errors are still captured above.
      await page.evaluate(() => {
        document.querySelectorAll(".reveal").forEach((e) => e.classList.add("is-visible"));
        document.querySelectorAll("nextjs-portal").forEach((e) => e.remove());
      });
      await page.waitForTimeout(400);
      const file = `${OUT}/${p.id}-${vpName}.png`;
      await page.screenshot({ path: file, fullPage: true });

      const problems = [];
      const status = res?.status();
      if (p.url ? !p.url.test(page.url()) : status !== (p.status ?? 200)) problems.push(`status ${status} at ${page.url()}`);
      if (p.text && !(await page.getByText(p.text).count())) problems.push(`missing text "${p.text}"`);
      for (const t of ERROR_TEXT) if (await page.getByText(t, { exact: false }).count()) problems.push(`error text "${t}"`);
      const expectsError = p.url || p.status || p.text;
      problems.push(
        ...errors
          .map((e) => e.split("\n")[0])
          .filter((e) => !(expectsError && EXPECTED_NOISE.some((r) => r.test(e)))),
      );
      if (problems.length) failed++;
      results.push({ id: p.id, viewport: vpName, file, status, ok: !problems.length, problems });
      console.log(`${problems.length ? "FAIL" : "ok  "} ${p.id}-${vpName}${problems.length ? "\n     " + problems.join("\n     ") : ""}`);
      await ctx.close();
    }
  }
  await browser.close();
  writeFileSync(`${OUT}/results.json`, JSON.stringify(results, null, 2));
  console.log(`\n${results.length - failed}/${results.length} ok → ${OUT}/`);
} finally {
  if (server) {
    try { process.kill(-server.child.pid, "SIGTERM"); } catch {}
  }
  await mongo.stop();
}
process.exitCode = failed ? 1 : 0;
