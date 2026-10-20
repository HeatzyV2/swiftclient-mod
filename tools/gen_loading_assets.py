"""Builds the loading screen textures (halo, ring, glyph atlases). They are read straight from the jar, before
the game's own resources and fonts exist, so text is drawn from these atlases instead of a Minecraft font.
Run from the repository root: python tools/gen_loading_assets.py"""
import json
import math
from PIL import Image, ImageDraw, ImageFont

ROOT = 'src/main/resources/assets/swiftclient/'
OUT = ROOT + 'textures/gui/loading/'
CHARS = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789%·.,-'!?:/()ÀÂÄÉÈÊËÎÏÔÖÙÛÜÇ"


def glow(size=256):
    im = Image.new('RGBA', (size, size))
    px = im.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / c
            a = max(0.0, 1.0 - d) ** 2.2
            px[x, y] = (255, 255, 255, round(255 * a))
    im.save(OUT + 'glow.png')


def ring(size=512, width=2.2, ss=4):
    big = size * ss
    im = Image.new('L', (big, big))
    d = ImageDraw.Draw(im)
    r = big / 2 - 2 * ss
    w = width * ss
    d.ellipse((big / 2 - r, big / 2 - r, big / 2 + r, big / 2 + r), outline=255, width=round(w))
    a = im.resize((size, size), Image.LANCZOS)
    out = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    out.putalpha(a)
    out.save(OUT + 'ring.png')


def atlas(name, ttf, px=48, cols=16):
    font = ImageFont.truetype(ROOT + 'font/ui/' + ttf, px)
    ascent, descent = font.getmetrics()
    cell_w = max(math.ceil(font.getlength(ch)) for ch in CHARS) + 4
    cell_h = ascent + descent + 4
    rows = math.ceil(len(CHARS) / cols)
    im = Image.new('RGBA', (cols * cell_w, rows * cell_h), (255, 255, 255, 0))
    alpha = Image.new('L', im.size)
    d = ImageDraw.Draw(alpha)
    for i, ch in enumerate(CHARS):
        d.text(((i % cols) * cell_w + 2, (i // cols) * cell_h + 2), ch, font=font, fill=255)
    im.putalpha(alpha)
    im.save(OUT + name + '.png')
    meta = {'chars': CHARS, 'cols': cols, 'cellW': cell_w, 'cellH': cell_h, 'capTop': ascent - font.getbbox('H')[3] + font.getbbox('H')[1],
            'capHeight': font.getbbox('H')[3] - font.getbbox('H')[1], 'width': im.size[0], 'height': im.size[1],
            'advance': [round(font.getlength(ch), 2) for ch in CHARS]}
    meta['capTop'] = font.getbbox('H')[1] + 2
    with open(OUT + name + '.json', 'w', encoding='utf-8') as f:
        json.dump(meta, f, ensure_ascii=False)


glow()
ring()
atlas('title', 'semibold.ttf')
atlas('text', 'medium.ttf')
