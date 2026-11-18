# OpenBook — App Store pack (iOS)

Version `2026.11.11` (build `20261111`), bundle `com.elearning.openbook`, team `KTKL66WRBK`, iPhone only.

| File | Use |
|---|---|
| `listing.md` | Copy-ready text for every App Store Connect field (VI + EN), category, age rating, App Privacy, review notes. |
| `creative/{vi,en}-header.png` | Product page **Header**, 21:9, 3840×1646, RGB, no alpha. Optional creative asset (iOS 27+). |
| `creative/{vi,en}-search.png` | **Search Results** creative, 3:2, 3840×2560, RGB, no alpha. Optional creative asset (iOS 27+). |
| `creative.py` | Rebuilds both creatives from `raw/`: `uv run --with pillow python3 creative.py`. The phrase per language is at the top. |
| `raw/` | Simulator captures (iPhone 18 Pro Max) and the hand-held EN page curl; source for `creative.py`. |

Privacy policy: `web/public/privacy.html` (VI `#vi`, EN `#en`), contact cdfx.dev@icloud.com.

## Screenshots (done)

Uploaded by the owner on 2026-11-11 at **1206×2622** — the required slot "iPhone with Dynamic Island (medium
display)". 1320×2868 was rejected in the large slot; the large slot is left to fall back. Sizes and pitfalls are
recorded in the `store-assets-skills` skill (`reference/sizes.md`).

## Creative assets rules (Apple, iOS 27)

Optional and separate from screenshots. Header 21:9 3840×1646 (or 16:9 5244×2950 PNG); Search Results 3:2,
1920×1280 to 3840×2560. JPEG/PNG, no alpha. A short phrase is allowed; no logos, prices, URLs or other
platforms. Keep the focal point centred — narrow screens crop the sides.

## Upload the build

The archive is `ios/build/OpenBook.xcarchive` (also copied to Xcode Organizer). Xcode → Window → Organizer →
Archives → OpenBook 2026.11.11 (20261111) → Distribute App → App Store Connect → Upload. Every new upload needs a
higher build number.
