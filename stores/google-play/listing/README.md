# OpenBook — Google Play listing import

Everything the Console asks for, in the order it asks, with the exact text in `listing.json`.
Nothing here is uploaded yet.

Copy from `listing.json` — do not retype, and do not edit the JSON without re-running the length check
below, because Play rejects silently-shortened copy.

```sh
cd stores/google-play/listing
python3 - <<'EOF'
import json; d=json.load(open('listing.json'))
lim={'title':30,'shortDescription':80,'fullDescription':4000}
for loc,v in d['locales'].items():
    for k,m in lim.items():
        n=len(v[k]); print(f"{loc} {k} {n}/{m} {'OK' if n<=m else 'OVER'}")
EOF
```

Current pass: title 21/30 (en), 23/30 (vi) · short 62/80 or 59/80 · full 1470/4000 or 1263/4000.

## 1. Main store listing — text

| Console field | Limit | Where in `listing.json` |
|---|---|---|
| App name | 30 | `locales.<loc>.title` |
| Short description | 80 | `locales.<loc>.shortDescription` |
| Full description | 4000 | `locales.<loc>.fullDescription` |

Manage translations → add `vi-VN`, then fill the Vietnamese values. Default locale is `en-US`,
matching the package's default resources.

## 2. Main store listing — graphics

| Console field | Requirement | File |
|---|---|---|
| App icon | 512×512, 32-bit PNG, no alpha | `../exports/icon-512.png` |
| Feature graphic | 1024×500, no alpha | `../exports/feature-graphic-en.png`, `../exports/feature-graphic-vi.png` |
| Phone screenshots | 2–8, 16:9 to 9:16, min 1080 px | `../exports/screenshots-en-1…5` in the `en-US` listing |
| Phone screenshots | same | `../exports/screenshots-vi-1…5` in the `vi-VN` listing |

The feature graphic has no per-language slot in Console — it is one image per app. English
(`feature-graphic-en.png`) is the safer default since the app name is not translated; the Vietnamese
version is there if you prefer it.

Order the five screenshots 1→5; they are already numbered for that order.

## 3. Store settings

| Field | Answer |
|---|---|
| App or game | App |
| Free or paid | Free |
| Category | Books & Reference |
| Tags | Reading, Books, E-book reader, EPUB |
| Contact email | required — your support address |
| Website | optional — skip if none |
| Privacy policy | required URL — see blockers below |

## 4. Content rating (IARC questionnaire)

| Question | Answer |
|---|---|
| Category | Reference, News or Educational → **Reference** |
| Violence, sexuality, language, drugs, gambling | No to every question |
| User-generated content, chat, sharing between users | No — imported books stay on the device |
| Shares location | No |
| Allows purchases | No |
| Unrestricted web access | No |
| Digital purchases | No |

Expected outcome: Everyone / 3+. If the questionnaire returns 18+ something was answered wrongly.

## 5. Data safety

| Question | Answer | Why |
|---|---|---|
| Does your app collect or share required user data types? | **No** | No `INTERNET` permission; no analytics, ads or crash SDKs |
| Data collected | None | — |
| Data shared | None | — |
| Data encrypted in transit | N/A | nothing is transmitted |
| Users can request data deletion | N/A | nothing leaves the device |

Declared permissions to keep in mind when describing the app: `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MEDIA_PLAYBACK` (read-aloud), `POST_NOTIFICATIONS` (player notification).
None of them are permissions Data safety asks about.

## 6. App content

| Field | Answer |
|---|---|
| Ads | No ads |
| In-app purchases | No |
| App access | All functionality available without restrictions — no login |
| Target audience | 13+ — not designed for children |
| News app | No |
| COVID-19 tracing | No |
| Data safety | see step 5 |
| Government app | No |
| Financial features | No |
| Health | No |

## 7. Release track

| Field | Value |
|---|---|
| Release name | `20261028.00` |
| Package | `com.elearning.openbook` |
| Version code / name | `2026102800` / `20261028.00` |
| Upload | signed `.aab` — not built yet |

Version scheme (owner, 2026-10-28): `versionCode` = `YYYYMMDD` + two-digit build counter, `versionName`
= the same number as `YYYYMMDD.CC`. Today is `20261028`, so `2026102800`. Rebuilds on the same day bump
the counter (`…01`, `…02`) and `versionCode` must keep rising across releases. Play's ceiling is
`2100000000`, which this reaches in **2137** — after that drop to `YYMMDDCC`.

## Blockers before the first upload

1. **Signed bundle: done 2026-10-28.** `android/app/build/outputs/bundle/release/app-release.aab`
   (4.34 MB, versionCode 2026102800, upload key `keystore/openbook-release.jks`, alias `openbook`).
   Nothing is uploaded until the owner says so. Back up that `.jks` + `android/keystore.properties`
   together — lose them and every future update needs a Play upload-key reset.
2. **No privacy policy URL.** A hosted one-page policy is still required by Console even with no data
   collection; it can be a single page stating the app collects nothing and stores books locally.
3. **The namespace is still `com.example.epubreader`.** Deliberate: only `applicationId` was renamed,
   so ~90 Kotlin files keep their package. It is invisible to users and to Play, but a full rename is
   still worth doing before the code goes public.
4. Known UI defects carried in the screenshots: the English reader footer still reads
   `Trang 15 / 436`, and a long book title collides with the toolbar buttons. Neither blocks upload,
   but both show in the listing images.

Done on 2026-10-28: `applicationId` → `com.elearning.openbook`; `app_name` → `OpenBook` in both
locales; adaptive launcher icon redrawn from the approved mark (terracotta `#9E5932` background,
foreground at 0.76 of the 108 dp canvas so a circular mask does not clip it). Verified on
emulator-5554: app drawer shows the icon and the label, the app launches under the new package.

## What is deliberately not in the listing

The Store tab. It is a demo with no network and says so on screen, so it is not advertised as a
feature and appears in no screenshot.
