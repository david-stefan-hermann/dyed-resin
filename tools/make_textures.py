#!/usr/bin/env python3
"""The 16 dyed resin clump textures (item and block) from the vanilla ones.

    python3 tools/make_textures.py [--preview out.png]

Reads textures/{item,block}/resin_clump.png from the loom cache's minecraft-client.jar and recolours each shade:
the shade keeps its distance in hue, saturation and brightness from the main orange, measured from the dye's own
colour instead. Writes src/main/resources/assets/dyedresin/textures/{item,block}/<colour>_resin_clump.png.
"""
import argparse
import colorsys
import io
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAR = Path.home() / '.gradle/caches/fabric-loom/26.2/minecraft-client.jar'
OUT = ROOT / 'src/main/resources/assets/dyedresin/textures'

# The colour each clump is built around: the dye's colour, nudged where resin needs it (light shades of white and
# yellow would clip, the darks of black would vanish).
COLOURS = {
    'white': (0xE4, 0xE8, 0xE8),
    'orange': (0xF0, 0x76, 0x13),
    'magenta': (0xC2, 0x44, 0xB8),
    'light_blue': (0x3A, 0xAF, 0xD9),
    'yellow': (0xF0, 0xC0, 0x24),
    'lime': (0x76, 0xC0, 0x18),
    'pink': (0xEE, 0x86, 0xA6),
    'gray': (0x52, 0x5A, 0x5E),
    'light_gray': (0x9E, 0x9E, 0x98),
    'cyan': (0x16, 0x98, 0x99),
    'purple': (0x84, 0x2F, 0xB6),
    'blue': (0x3C, 0x46, 0xB0),
    'brown': (0x84, 0x52, 0x2E),
    'green': (0x5A, 0x7A, 0x14),
    'red': (0xB8, 0x2C, 0x24),
    'black': (0x34, 0x34, 0x3C),
}
BASE = (224, 85, 4)  # the main orange of the vanilla clump
HUE_SHIFT = 0.6  # how much of vanilla's warm-to-yellow hue ramp a dyed clump keeps


def hsv(rgb):
    return colorsys.rgb_to_hsv(*(c / 255 for c in rgb))


def recolour(pixel, target):
    h0, s0, v0 = hsv(BASE)
    h, s, v = hsv(pixel[:3])
    th, ts, tv = hsv(target)
    dh = ((h - h0 + 0.5) % 1.0) - 0.5
    nh = (th + dh * HUE_SHIFT * min(1.0, ts * 2)) % 1.0
    ns = max(0.0, min(1.0, ts * (s / s0)))
    nv = max(0.0, min(1.0, tv + (v - v0) * (0.75 + 0.25 * tv)))
    r, g, b = colorsys.hsv_to_rgb(nh, ns, nv)
    return round(r * 255), round(g * 255), round(b * 255), pixel[3]


def dyed(image, target):
    out = image.copy()
    out.putdata([recolour(p, target) if p[3] else p for p in image.getdata()])
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--preview', help='also write a sheet of all clumps, 8x enlarged')
    args = parser.parse_args()
    with zipfile.ZipFile(JAR) as jar:
        vanilla = {kind: Image.open(io.BytesIO(jar.read('assets/minecraft/textures/%s/resin_clump.png' % kind)))
                   .convert('RGBA') for kind in ('item', 'block')}
    sheet = Image.new('RGBA', (17 * 16, 2 * 16), (40, 40, 40, 255))
    for kind, image in vanilla.items():
        sheet.paste(image, (0, 16 if kind == 'block' else 0))
    for i, (name, colour) in enumerate(COLOURS.items(), start=1):
        for kind, image in vanilla.items():
            texture = dyed(image, colour)
            (OUT / kind).mkdir(parents=True, exist_ok=True)
            texture.save(OUT / kind / ('%s_resin_clump.png' % name))
            sheet.paste(texture, (i * 16, 16 if kind == 'block' else 0), texture)
    if args.preview:
        sheet.resize((sheet.width * 8, sheet.height * 8), Image.NEAREST).save(args.preview)
    print('%d colours written to %s' % (len(COLOURS), OUT))


if __name__ == '__main__':
    main()
