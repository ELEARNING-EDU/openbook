#!/usr/bin/env python3
"""Google Play wants 24-bit PNG. Strip the alpha channel from the rendered exports."""
from pathlib import Path
from PIL import Image

for path in sorted(Path(__file__).parent.joinpath("exports").glob("*.png")):
    image = Image.open(path)
    if image.mode != "RGB":
        image.convert("RGB").save(path)
print("flattened exports to RGB")
