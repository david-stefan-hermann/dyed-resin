#!/usr/bin/env python3
"""The mod's logo (icon.png, 128 x 128) and the guide book's item texture, from the dyed clump textures.

    python3 tools/make_textures.py && python3 tools/make_art.py

The logo is the sixteen dyed clumps, four by four, in dye order. The book is vanilla's book with its leather in
resin orange and a light blue clump on the cover, like the other mods' books.
"""
import io
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / 'src/main/resources/assets/dyedresin'
JAR = Path.home() / '.gradle/caches/fabric-loom/26.2/minecraft-client.jar'
ORDER = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
         'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black')
COVER = (0xD9, 0x62, 0x0F)  # resin orange, the guide book's panel colour
# the logo on the cover: a light blue clump, as on the creative tab
CLUMP = {
    (0xA8, 0xE4, 0xF4): [(7, 3), (8, 3)],
    (0x3A, 0xAF, 0xD9): [(6, 4), (7, 4), (8, 4), (9, 4), (6, 5), (7, 5), (10, 5)],
    (0x1F, 0x7C, 0xA6): [(8, 5), (9, 5), (7, 6), (8, 6), (10, 6), (11, 6)],
    (0x14, 0x55, 0x78): [(9, 6), (11, 7)],
}


def logo():
    out = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    for i, name in enumerate(ORDER):
        clump = Image.open(ASSETS / 'textures/item' / ('%s_resin_clump.png' % name)).convert('RGBA')
        clump = clump.resize((32, 32), Image.NEAREST)
        out.paste(clump, ((i % 4) * 32, (i // 4) * 32), clump)
    out.save(ASSETS / 'icon.png')


def book():
    with zipfile.ZipFile(JAR) as jar:
        image = Image.open(io.BytesIO(jar.read('assets/minecraft/textures/item/book.png'))).convert('RGBA')
    pixels = []
    for r, g, b, a in image.getdata():
        if a and max(r, g, b) - min(r, g, b) >= 16:  # the leather; the pages are grey
            f = r / 0x65  # 0x65: the red of the vanilla cover's main brown
            r, g, b = (min(255, round(c * f)) for c in COVER)
        pixels.append((r, g, b, a))
    image.putdata(pixels)
    for colour, points in CLUMP.items():
        for point in points:
            image.putpixel(point, colour + (255,))
    image.save(ASSETS / 'textures/item/guide_book.png')

if __name__ == '__main__':
    logo()
    book()
