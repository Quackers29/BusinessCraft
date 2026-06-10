#!/usr/bin/env python3
"""Generate tourist_basic.png from vanilla villager UV layout (MC 1.20.1)."""
from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "docs" / "reference" / "villager.png"
OUT = ROOT / "common" / "src" / "main" / "resources" / "assets" / "businesscraft" / "textures" / "entity" / "tourist_basic.png"


def fill_box(img: Image.Image, box: tuple[int, int, int, int], color: tuple[int, int, int, int]) -> None:
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            if img.getpixel((x, y))[3] > 0:
                img.putpixel((x, y), color)


def fill_box_pattern(
    img: Image.Image,
    box: tuple[int, int, int, int],
    base: tuple[int, int, int, int],
    accent: tuple[int, int, int, int],
    step: int = 5,
) -> None:
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            if img.getpixel((x, y))[3] == 0:
                continue
            c = accent if (x + y) % step == 0 else base
            img.putpixel((x, y), c)


def draw_pixel(img: Image.Image, x: int, y: int, color: tuple[int, int, int, int]) -> None:
    if 0 <= x < img.width and 0 <= y < img.height and img.getpixel((x, y))[3] > 0:
        img.putpixel((x, y), color)


def main() -> None:
    if not BASE.exists():
        raise SystemExit(f"Missing base texture: {BASE}")

    img = Image.open(BASE).convert("RGBA")
    if img.size != (64, 64):
        raise SystemExit(f"Expected 64x64 base, got {img.size}")

    # Tier 1 basic tourist palette.
    polo = (48, 128, 220, 255)
    polo_dark = (32, 92, 175, 255)
    collar = (245, 245, 245, 255)
    khaki = (205, 176, 112, 255)
    khaki_dark = (165, 138, 82, 255)
    shoe = (242, 242, 242, 255)
    shoe_accent = (210, 48, 48, 255)
    hair = (96, 58, 32, 255)
    skin = (194, 152, 108, 255)
    glasses = (20, 20, 20, 255)
    strap = (30, 30, 30, 255)
    camera = (58, 58, 58, 255)
    lens = (220, 220, 220, 255)
    backpack = (188, 44, 44, 255)

    # --- Clothing: replace villager robe panels (opaque UV islands only). ---
    # Upper torso / crossed arms robe (front, back, sides).
    torso_boxes = [
        (20, 20, 28, 32),  # front
        (32, 20, 40, 32),  # back
        (16, 20, 20, 32),  # right side
        (28, 20, 32, 32),  # left side
        (40, 20, 56, 32),  # folded arm robe panels
    ]
    for box in torso_boxes:
        fill_box_pattern(img, box, polo, polo_dark, step=6)

    # White polo collar on chest front.
    fill_box(img, (21, 20, 27, 22), collar)

    # Lower robe -> khaki shorts.
    leg_boxes = [
        (4, 20, 8, 32),
        (8, 20, 12, 32),
        (0, 20, 4, 32),
        (12, 20, 16, 32),
        (20, 52, 24, 64),
        (24, 52, 28, 64),
        (16, 52, 20, 64),
        (28, 52, 32, 64),
        (20, 32, 28, 40),
        (32, 32, 40, 40),
    ]
    for box in leg_boxes:
        fill_box_pattern(img, box, khaki, khaki_dark, step=4)

    # Sneakers on lowest leg rows.
    shoe_boxes = [
        (4, 28, 8, 32),
        (8, 28, 12, 32),
        (0, 28, 4, 32),
        (12, 28, 16, 32),
        (20, 60, 24, 64),
        (24, 60, 28, 64),
        (16, 60, 20, 64),
        (28, 60, 32, 64),
    ]
    for box in shoe_boxes:
        x0, y0, x1, y1 = box
        for y in range(y0, y1):
            for x in range(x0, x1):
                if img.getpixel((x, y))[3] > 0:
                    img.putpixel((x, y), shoe_accent if x % 2 == 0 else shoe)

    # --- Head: keep villager face shape, refresh hair + tourist details. ---
    # Hair panels (top + back of head).
    fill_box(img, (8, 0, 24, 8), hair)
    fill_box(img, (16, 8, 24, 16), hair)
    # Side hair strips.
    fill_box(img, (0, 8, 8, 12), hair)
    fill_box(img, (24, 8, 32, 12), hair)

    # Face skin refresh (front head), preserve nose bump separately.
    fill_box(img, (8, 12, 16, 16), skin)
    fill_box(img, (9, 8, 15, 12), skin)

    # Sunglasses on forehead.
    for x in range(9, 15):
        draw_pixel(img, x, 9, glasses)
        draw_pixel(img, x, 10, glasses)

    # Friendly eyes (simple dark pupils on white sclera kept from base where possible).
    draw_pixel(img, 10, 12, (255, 255, 255, 255))
    draw_pixel(img, 13, 12, (255, 255, 255, 255))
    draw_pixel(img, 11, 12, (30, 30, 30, 255))
    draw_pixel(img, 14, 12, (30, 30, 30, 255))

    # --- Accessories ---
    # Camera strap + body on chest front.
    for x in range(21, 27):
        draw_pixel(img, x, 21, strap)
    for y in range(23, 27):
        for x in range(22, 25):
            draw_pixel(img, x, y, camera)
    draw_pixel(img, 23, 24, lens)
    draw_pixel(img, 24, 24, lens)

    # Red daypack on back torso.
    fill_box(img, (34, 22, 38, 29), backpack)

    # Watch on visible arm skin panel.
    for x in range(44, 48):
        draw_pixel(img, x, 26, strap)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print(f"Wrote {OUT} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
