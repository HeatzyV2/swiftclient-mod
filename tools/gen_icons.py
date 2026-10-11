"""Generates the mod's UI icons (src/main/resources/assets/swiftclient/textures/gui/icons/*.png).

The icons are the pixelarticons set already used by the launcher, rasterised as white 64x64 masks: the game tints them
at draw time (dim when idle, white when active), so they follow the Swift palette instead of being colour emojis.

    python tools/gen_icons.py <path to pixelarticons icons.json>

icons.json lives in node_modules/@iconify-json/pixelarticons of the launcher repository.
"""
import json
import re
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/swiftclient/textures/gui/icons"
MCMETA = '{\n  "texture": {\n    "blur": true,\n    "clamp": true\n  }\n}\n'
TILE = 64
SS = 6  # supersampling for smooth edges
MAX_SCALE = 3.1  # pixels per source unit: keeps the original padding feeling, never bigger

# mod icon name -> pixelarticons name
MAP = {
    "activity": "chart-line", "autojump": "arrow-big-up", "back": "arrow-left", "bell": "bell", "blockoverlay": "box",
    "bomb": "bomb", "bossbar": "battery-full", "calendar": "calendar", "cape": "flag", "chat": "message-text",
    "check": "check", "chevron_down": "chevron-down", "chevron_right": "chevron-right", "chevron_up": "chevron-up",
    "chip": "cpu", "click": "mouse", "clock": "clock", "close": "close", "cloud": "cloud", "combo": "zap",
    "compass": "compass", "crosshair": "bullseye", "fire": "fire", "flask": "test-tube", "fog": "waves",
    "foliage": "tree", "freelook": "eye", "friends": "users", "fullbright": "sun", "gapple": "heart",
    "gauge": "dashboard", "gear": "settings-cog", "glass": "window-frame", "globe": "globe", "grass": "leaf",
    "grid": "grid-3x3", "hitbox": "frame", "hitcolor": "drop", "host": "server", "hourglass": "hourglass",
    "hud": "layout", "items2d": "card-stack", "keyboard": "keyboard", "leaf": "leaf", "list": "list", "map": "map",
    "menu": "menu", "mods": "blocks", "motionblur": "speed-fast", "music": "music", "nametag": "label",
    "nickhider": "eye-off", "norain": "sun-alt", "palette": "colors-swatch", "physics": "circle-pile",
    "pickup": "download", "pin": "pin", "profiles": "sliders-2", "reach": "arrows-horizontal", "retro": "gamepad",
    "saturation": "fill-half", "screenshot": "camera", "shield": "shield", "shirt": "shirt", "signal": "signal",
    "snaplook": "crop", "sparkles": "sparkles", "stack": "files", "sunrise": "sun", "sword": "sword",
    "tabanim": "tab", "timer": "alarm-clock", "titles": "heading", "togglesneak": "human",
    "togglesprint": "human-run", "users": "user", "vanillaui": "app-windows", "widertab": "chevrons-horizontal",
    "worldedit": "edit", "zoom": "search",
    # new icons used by the redesigned screens
    "undo": "undo", "redo": "redo", "copy": "copy", "trash": "trash", "plus": "plus", "minus": "minus",
    "lock": "lock", "unlock": "lock-open", "reload": "reload", "move": "move", "expand": "expand",
    "eye": "eye", "coins": "coins", "cart": "shopping-bag", "star": "star", "pet": "dog", "heart": "heart",
    "crown": "crown", "hand": "hand", "pointer": "pointer", "layers": "layout-sidebar-right", "target": "target",
    "alignh": "align-center-horizontal", "alignv": "align-center-vertical", "link": "link", "play": "play",
    "stop": "stop", "send": "send", "user_plus": "user-plus", "wifi": "wifi", "lightning": "zap",
    "ruler": "proportions", "magnet": "target", "wand": "wand", "info": "circle-info", "warning": "alert",
    "key": "key", "image": "image", "swap": "repeat", "filter": "filter", "sort": "sort", "save": "save",
}


def parse_path(d):
    """pixelarticons paths are made of axis-aligned moves: M/m L/l H/h V/v Z/z. Returns a list of polygons."""
    tokens = re.findall(r"[MmLlHhVvZz]|-?\d*\.?\d+", d)
    polys, cur, x, y, i, cmd = [], [], 0.0, 0.0, 0, None
    start = (0.0, 0.0)
    while i < len(tokens):
        t = tokens[i]
        if re.match(r"[A-Za-z]", t):
            cmd = t
            i += 1
            if cmd in "Zz":
                if cur:
                    polys.append(cur)
                cur = []
                x, y = start
            continue
        if cmd in "Mm":
            nx, ny = float(tokens[i]), float(tokens[i + 1])
            i += 2
            x, y = (nx, ny) if cmd == "M" else (x + nx, y + ny)
            if cur:
                polys.append(cur)
            cur = [(x, y)]
            start = (x, y)
            cmd = "L" if cmd == "M" else "l"
        elif cmd in "Ll":
            nx, ny = float(tokens[i]), float(tokens[i + 1])
            i += 2
            x, y = (nx, ny) if cmd == "L" else (x + nx, y + ny)
            cur.append((x, y))
        elif cmd in "Hh":
            nx = float(tokens[i])
            i += 1
            x = nx if cmd == "H" else x + nx
            cur.append((x, y))
        elif cmd in "Vv":
            ny = float(tokens[i])
            i += 1
            y = ny if cmd == "V" else y + ny
            cur.append((x, y))
        else:
            i += 1
    if cur:
        polys.append(cur)
    return polys


def render(body):
    paths = re.findall(r'\sd="([^"]+)"', body)
    polys = [p for d in paths for p in parse_path(d) if len(p) >= 3]
    if not polys:
        return None
    xs = [px for p in polys for px, _ in p]
    ys = [py for p in polys for _, py in p]
    bx0, bx1, by0, by1 = min(xs), max(xs), min(ys), max(ys)
    bw, bh = max(bx1 - bx0, 1), max(by1 - by0, 1)
    scale = min(MAX_SCALE, 58.0 / max(bw, bh))
    ox = TILE / 2 - (bx0 + bw / 2) * scale
    oy = TILE / 2 - (by0 + bh / 2) * scale
    mask = Image.new("1", (TILE * SS, TILE * SS), 0)
    # Even-odd fill: each sub-path is XORed into the mask
    for p in polys:
        layer = Image.new("1", mask.size, 0)
        ImageDraw.Draw(layer).polygon([((px * scale + ox) * SS, (py * scale + oy) * SS) for px, py in p], fill=1)
        mask = ImageChops.logical_xor(mask, layer)
    mask = mask.convert("L")
    mask = mask.resize((TILE, TILE), Image.LANCZOS)
    img = Image.new("RGBA", (TILE, TILE), (255, 255, 255, 0))
    img.putalpha(mask)
    return img


def main():
    data = json.loads(Path(sys.argv[1]).read_text(encoding="utf8"))
    icons = data["icons"]
    OUT.mkdir(parents=True, exist_ok=True)
    made, missing = 0, []
    for name, src in sorted(MAP.items()):
        if src not in icons:
            missing.append((name, src))
            continue
        img = render(icons[src]["body"])
        if img is None:
            missing.append((name, src))
            continue
        img.save(OUT / f"{name}.png")
        (OUT / f"{name}.png.mcmeta").write_text(MCMETA, encoding="utf8", newline="\n")
        made += 1
    print(f"{made} icons written, missing: {missing}")


if __name__ == "__main__":
    main()
