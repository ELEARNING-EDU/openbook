"""App Store creative assets (iOS 27+, optional): product page Header 21:9 and Search Results 3:2.
Paper background, one short phrase, real app screens from raw/. No logo, price or URL (Apple's rules).
Run from stores/app-store:  uv run --with pillow python3 creative.py   → creative/{vi,en}-{header,search}.png
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).parent
PAPER, INK, MUTED = (251, 247, 238), (42, 36, 29), (110, 100, 89)
SERIF = "/System/Library/Fonts/Supplemental/Georgia Bold.ttf"
PHRASE = {"vi": "Đọc, nghe và ghi chú sách", "en": "Read, listen and take notes"}
# (canvas, phrase size, phrase top, phone width, phones top, screens)
LAYOUT = {
    "header": ((3840, 1646), 120, 150, 560, 470, ["1-home", "3-reader", "4-listen", "7-search"]),
    "search": ((3840, 2560), 150, 200, 900, 560, ["3-reader", "4-listen", "7-search"]),
}


def phone(shot: Image.Image, width: int) -> Image.Image:
    """The screen in a rounded ink frame, like the screenshot set."""
    border, radius = round(width * 0.012), round(width * 0.075)
    h = round(width * shot.height / shot.width)
    screen = shot.resize((width, h), Image.LANCZOS)
    frame = Image.new("RGB", (width + 2 * border, h + 2 * border), PAPER)
    d = ImageDraw.Draw(frame)
    d.rounded_rectangle((0, 0, frame.width - 1, frame.height - 1), radius + border, fill=INK)
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width - 1, h - 1), radius, fill=255)
    frame.paste(screen, (border, border), mask)
    outer = Image.new("L", frame.size, 0)
    ImageDraw.Draw(outer).rounded_rectangle((0, 0, frame.width - 1, frame.height - 1), radius + border, fill=255)
    return frame, outer


def render(lang: str, kind: str) -> Path:
    (w, h), size, top, pw, ptop, screens = LAYOUT[kind]
    canvas = Image.new("RGB", (w, h), PAPER)
    d = ImageDraw.Draw(canvas)
    font = ImageFont.truetype(SERIF, size)
    text = PHRASE[lang]
    tw = d.textlength(text, font=font)
    d.text(((w - tw) / 2, top), text, font=font, fill=INK)

    gap = round(pw * 0.14)
    frames = [phone(Image.open(ROOT / f"raw/{lang}-{s}.png").convert("RGB"), pw) for s in screens]
    total = sum(f.width for f, _ in frames) + gap * (len(frames) - 1)
    x = (w - total) // 2  # centred: Apple crops the sides on narrow screens
    for frame, mask in frames:
        canvas.paste(frame, (x, ptop), mask)  # phones run off the bottom edge on purpose
        x += frame.width + gap

    out = ROOT / f"creative/{lang}-{kind}.png"
    out.parent.mkdir(exist_ok=True)
    canvas.save(out)
    return out


if __name__ == "__main__":
    for lang in ("vi", "en"):
        for kind in LAYOUT:
            p = render(lang, kind)
            im = Image.open(p)
            assert im.mode == "RGB" and im.size == LAYOUT[kind][0], (p, im.mode, im.size)
            print(p.relative_to(ROOT), im.size, im.mode)
