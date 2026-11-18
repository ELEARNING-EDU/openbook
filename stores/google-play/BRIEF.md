# OpenBook — Google Play asset pack v2

Status: wordmark icon (owner, 2026-11-11), replaces the 2026-10-28 book mark. Ten screenshots and two feature graphics rendered
from real device captures. Nothing uploaded.

Palette: paper `#FBF7EE`, ink `#2A241D`, terracotta `#9E5932` (from Android `ShellTheme.kt`).
Type: Georgia Bold headings, Arial body.

## Icon

Wordmark "OpenBook" like the web logo: Playfair Display Bold, `Open` ink `#3B2A1A`, `Book` brick
`#C2410C`, on cream `#FBF6EC` (web tokens). Owner asked for white `Open`; it was unreadable on cream,
so owner picked ink (2026-11-11).

Source of truth: `stores/app-icon/make-icon.swift`. One run writes `stores/app-icon/icon.svg`,
the PNGs, the iOS `AppIcon` and the Android adaptive foreground. Then refresh this pack:

```sh
swift stores/app-icon/make-icon.swift
cp stores/app-icon/icon-512.png stores/google-play/exports/icon-512.png
cd stores/google-play && swift render.swift . && uv run --with pillow python3 flatten.py
```

Android background colour lives in `android/app/src/main/res/values/ic_launcher_background.xml`.

## Screenshots — 1080×1920, 24-bit

Five screens, one idea each, captions per store language. Settings screen and drawer are out
by owner request (2026-10-28).

| # | Screen | Vietnamese | English |
|---|--------|-----------|---------|
| 1 | Home | Mở ra là đọc tiếp | Pick up where you stopped |
| 2 | Library | Nhập EPUB, xem cả tủ | Your shelf in one grid |
| 3 | Reader | Một trang đọc yên | A quiet page, tuned |
| 4 | Read aloud | Để sách đọc thành tiếng | Let the book speak |
| 5 | Page turn | Lật trang như sách giấy | Pages that turn like paper |

The page-turn shot is a **diagonal** fold (owner request, 2026-10-28): drag from bottom-right to
top-left and screencap mid-gesture, e.g. for VI

```sh
( adb shell input swipe 1180 2200 620 1150 1750 & ); sleep 0.58; adb exec-out screencap -p > captures/vi-curl-raw.png
```

Earlier or later than ~0.6 s gives a corner tip or a flat vertical fold, so re-shoot if it does not
come out diagonal.

Inputs: `captures/{en,vi}-{home,library,reader,listen,curl}-raw.png`, captured from
`com.example.epubreader` on emulator-5554 (1344×2992), locale switched per language with
`adb shell cmd locale set-app-locales com.example.epubreader --locales vi-VN|en-US`.
English set reads Frankenstein; Vietnamese set reads *Sự sống là gì?*.

```sh
swift render.swift . && python3 flatten.py
```

## Feature graphic — 1024×500, 24-bit

`exports/feature-graphic-{vi,en}.png`: name, two-line tagline, feature line, approved icon.
Kept clear of the middle-right where Play's surfaces overlay controls.

## Known defects seen in the captures (fix before publishing)

1. Reader footer in the English UI still reads `Trang 15 / 436` — the string is Vietnamese
   regardless of locale. Visible in the three English reader shots (reader, listen, curl).
2. A long book title as the running head runs underneath the toolbar buttons (seen with
   *Sự sống là gì?* with chrome up). The Vietnamese shots avoid it by hiding the chrome.
3. The feature graphic shows "OpenBook" twice now (headline + wordmark icon).
