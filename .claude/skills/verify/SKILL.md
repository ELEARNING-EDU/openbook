---
name: verify
description: Use when verifying a change in OpenBook web by running the app, capturing screenshots or evidence for a PR, or when verified-loop needs this project's commands.
---

# Verify OpenBook web

Feature map: `docs/feature-map.md`. Corrections: `docs/corrections.md`.

## Hard checks (run all, in this order; same as CI)
1. `npx tsc --noEmit`
2. `npm run lint`
3. `npm test` (Vitest, in-memory MongoDB)

## Run the app against safe data
`node scripts/verify.mjs [id ...]`
- Starts an in-memory MongoDB, seeds it (including the sample EPUB book), runs `next dev` on port 3100 (`VERIFY_PORT` to change), and captures each feature-map id at 390×844 and 1280×800.
- Output: `test-results/verify/<id>-<mobile|desktop>.png` and `results.json` (git-ignored). Exit code 1 if any entry fails.
- Filter by id substring: `node scripts/verify.mjs book admin-books`. A full run takes about 3 minutes, a filtered one less.
- Open the PNGs for the entries you touched. "ok" only means status and console are clean; it does not mean the change is visible.
- Changed behavior that a screenshot can't show (clicks, forms): add a Playwright e2e test in `e2e/`.

## Logged-in / admin states
The script generates a throwaway `AUTH_SECRET`, starts the server with `ADMIN_EMAILS=admin@verify.local`, and forges Auth.js JWT cookies (`authjs.session-token`) for `who: "reader" | "admin"`. Real Google sign-in is never used.

## e2e (Playwright)
`npm run e2e` builds and serves on port 3200 and needs a seeded **local** DB. Run it only with `MONGODB_URI` overridden on the command line to a local instance, as described in README → Kiểm thử.

## Never
- `npm run seed` or `npm run e2e` with `.env.local`'s `MONGODB_URI` (Atlas): it deletes all books, categories and reviews. Use `scripts/verify.mjs` instead, or override `MONGODB_URI` to `127.0.0.1`.
- `npm run migrate:authors -- --apply` without the user's go-ahead and a backup.
- Deploying to Vercel or changing OAuth settings.

## Known gaps
- Real Google OAuth sign-in: manual only.
- `/admin/*` for a signed-in non-admin returns HTTP 200 with the 404 UI (`notFound()` in a streamed layout). The script asserts the UI.
- `next dev` logs a Cache Components "instant validation" console error on routes whose layout calls `redirect()`/`notFound()`. The script ignores it only on entries that expect a 404 or redirect.
- External EPUB sources (Gutenberg/Wikisource) and covers are live network calls; slow or offline sources show as missing covers, not failures.
- Reader page flipping and admin save flows are covered by e2e and `.superpowers`-era manual runs, not by this script.
