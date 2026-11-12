#!/usr/bin/env python3
"""Draws the Dom Client logo and writes every icon size the project uses."""
import pathlib
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parent.parent
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
TOP_LEFT = (139, 92, 246)     # violet
BOTTOM_RIGHT = (37, 99, 235)  # blue


def logo(size: int) -> Image.Image:
    s = size * 4  # supersample, then shrink for smooth edges
    grad = Image.new("RGB", (s, s))
    px = grad.load()
    for y in range(s):
        for x in range(s):
            t = (x + y) / (2 * (s - 1))
            px[x, y] = tuple(int(a + (b - a) * t) for a, b in zip(TOP_LEFT, BOTTOM_RIGHT))
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, s - 1, s - 1), radius=int(s * 0.22), fill=255)
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    img.paste(grad, (0, 0), mask)

    d = ImageDraw.Draw(img)
    font = ImageFont.truetype(FONT, int(s * 0.62))
    text = "D"
    l, t, r, b = d.textbbox((0, 0), text, font=font)
    cx = (s - (r - l)) / 2 - l - s * 0.07
    cy = (s - (b - t)) / 2 - t
    shadow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).text((cx + s * 0.015, cy + s * 0.025), text, font=font, fill=(0, 0, 0, 90))
    img = Image.alpha_composite(img, shadow.filter(ImageFilter.GaussianBlur(s * 0.02)))
    d = ImageDraw.Draw(img)
    d.text((cx, cy), text, font=font, fill=(255, 255, 255, 255))

    # Two "voice" arcs to the right of the letter.
    w = max(4, int(s * 0.045))
    for i, rad in enumerate((0.17, 0.27)):
        r_px = s * rad
        ox, oy = s * 0.62, s * 0.5
        box = (ox - r_px, oy - r_px, ox + r_px, oy + r_px)
        d.arc(box, start=-45, end=45, fill=(255, 255, 255, 235 - i * 60), width=w)
    return img.resize((size, size), Image.LANCZOS)


def main():
    big = logo(256)
    (ROOT / "installer" / "assets").mkdir(parents=True, exist_ok=True)
    (ROOT / "installer" / "payload").mkdir(parents=True, exist_ok=True)
    big.save(ROOT / "installer" / "assets" / "logo.png")
    big.save(ROOT / "installer" / "assets" / "icon.ico",
             sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    logo(128).save(ROOT / "installer" / "payload" / "profile-icon.png")
    mod_icon = ROOT / "mod" / "src" / "main" / "resources" / "assets" / "domclient" / "icon.png"
    mod_icon.parent.mkdir(parents=True, exist_ok=True)
    logo(128).save(mod_icon)
    print("icons written")


if __name__ == "__main__":
    main()
