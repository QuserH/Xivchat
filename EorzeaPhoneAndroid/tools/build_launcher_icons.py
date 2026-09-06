"""Reframe the existing artwork and preview standard Android launcher masks."""

import argparse
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
SOURCE = ROOT / "tools/assets/launcher-source.png"
BACKGROUND = (23, 27, 25, 255)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview-dir", type=Path, required=True)
    args = parser.parse_args()
    if not SOURCE.exists():
        SOURCE.parent.mkdir(parents=True, exist_ok=True)
        with Image.open(RES / "mipmap-xxxhdpi/ic_launcher.png") as original:
            original.save(SOURCE)

    with Image.open(SOURCE) as original:
        # The old 192px artwork includes a pale backdrop and an outer mockup bezel.
        art = original.convert("RGBA").crop((22, 18, 174, 178))
    art_mask = Image.new("L", art.size)
    ImageDraw.Draw(art_mask).rounded_rectangle((0, 0, 151, 159), radius=26, fill=255)
    art.putalpha(art_mask)
    square = Image.new("RGBA", (432, 432), BACKGROUND)
    # Keep the phone and companion inside the circle's safe area, not just a square.
    art = art.resize((345, 363), Image.Resampling.LANCZOS)
    square.alpha_composite(art, (43, 34))
    art_path = RES / "drawable-nodpi/launcher_art.png"
    square.save(art_path)

    for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        icon = square.resize((size, size), Image.Resampling.LANCZOS)
        icon.save(RES / f"mipmap-{density}/ic_launcher.png")
        mask = Image.new("L", (432, 432))
        ImageDraw.Draw(mask).ellipse((0, 0, 431, 431), fill=255)
        icon.putalpha(mask.resize((size, size), Image.Resampling.LANCZOS))
        icon.save(RES / f"mipmap-{density}/ic_launcher_round.png")

    args.preview_dir.mkdir(parents=True, exist_ok=True)
    preview = Image.new("RGB", (700, 254), "#e5e7eb")
    for index, shape in enumerate(("circle", "rounded", "square")):
        mask = Image.new("L", (432, 432))
        draw = ImageDraw.Draw(mask)
        if shape == "circle":
            draw.ellipse((0, 0, 431, 431), fill=255)
        elif shape == "rounded":
            draw.rounded_rectangle((0, 0, 431, 431), radius=100, fill=255)
        else:
            draw.rectangle((0, 0, 431, 431), fill=255)
        shaped = square.copy()
        shaped.putalpha(mask)
        shaped = shaped.resize((192, 192), Image.Resampling.LANCZOS)
        preview.paste(shaped, (22 + index * 230, 20), shaped)
        ImageDraw.Draw(preview).text((22 + index * 230, 224), shape, fill="black")
    preview.save(args.preview_dir / "launcher-masks.png")
    print(f"Generated launcher icons; preview: {args.preview_dir / 'launcher-masks.png'}")


if __name__ == "__main__":
    main()
