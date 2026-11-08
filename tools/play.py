"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/*.jpg (1080 px wide phone screenshots of the demo house, see tools/shots.py)
and writes the 512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen is cut short under its caption.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
SERIF = ROOT / "app/src/main/res/font/lora_bold.ttf"
SANS = ROOT / "app/src/main/res/font/nunito_bold.ttf"

# The evening theme: the room in lamp light.
BG = (0x22, 0x30, 0x2B)
DEEP = (0x16, 0x1F, 0x1B)
INK = (0xF3, 0xEA, 0xD8)
MUTED = (0xA9, 0xB3, 0xA6)
AMBER = (0xF0, 0xB7, 0x5E)

# The order they appear in the store, with the caption over each: a light line and an amber one.
PAGES = [
    ("1-home", ("Write it down once,", "the hob remembers"), ("Запишите один раз —", "домовой запомнит")),
    ("4-find", ("Where is it?", "Just ask"), ("Где лежит?", "Просто спросите")),
    ("2-room", ("Rooms, shelves", "and numbered boxes"), ("Комнаты, полки", "и коробки с номерами")),
    ("3-thing", ("Lent, needed soon,", "back on the shelf"), ("Одолжили, скоро нужно,", "вернули на полку")),
    ("6-kid", ("When a child", "grows into it"), ("Когда ребёнок", "дорастёт до вещи")),
    ("5-people", ("What's lent out", "and what to give back"), ("Что одолжили", "и что пора вернуть")),
    ("8-clear", ("A clear-out", "in five minutes"), ("Разбор", "за пять минут")),
    ("7-hob", ("The cosier,", "the better"), ("Чем уютнее,", "тем лучше")),
]


def font(path, size):
    return ImageFont.truetype(str(path), size)


def fit(path, text, size, width):
    while font(path, size).getlength(text) > width:
        size -= 2
    return font(path, size)


def board(w, h):
    """The dark green of the app, with lamp light glowing from the top."""
    g = Image.new("RGB", (w, h), BG).convert("RGBA")
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    r = max(w, h) * 0.55
    ImageDraw.Draw(glow).ellipse((w / 2 - r, -r * 1.1, w / 2 + r, r * 0.5), fill=AMBER + (46,))
    g.alpha_composite(glow.filter(ImageFilter.GaussianBlur(r / 3)))
    return g


def rounded(im, radius):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, radius=44):
    """A screen on the board: rounded like a phone, with a soft shadow under it."""
    sh = Image.new("RGBA", (card.width + 120, card.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((60, 76, 60 + card.width, 76 + card.height), radius, fill=DEEP + (200,))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(26)), (x - 60, y - 60))
    g.alpha_composite(rounded(card, radius), (x, y))
    edge = Image.new("RGBA", g.size, (0, 0, 0, 0))
    ImageDraw.Draw(edge).rounded_rectangle((x - 1, y - 1, x + card.width, y + card.height), radius, outline=(255, 255, 255, 34), width=3)
    g.alpha_composite(edge)


def shot(lang, name):
    return Image.open(SHOTS / lang / f"{name}.jpg").convert("RGB")


def screenshot(lang, name, caption):
    W, H = 1080, 1920
    g = board(W, H)
    d = ImageDraw.Draw(g)
    size = min(fit(SERIF, line, 84, W - 140).size for line in caption)
    title = font(SERIF, size)
    d.text((W / 2, 140), caption[0], font=title, fill=INK, anchor="mm")
    d.text((W / 2, 240), caption[1], font=title, fill=AMBER, anchor="mm")
    im = shot(lang, name)
    w = 860
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 340
    # The screen runs off the bottom edge: the house goes on.
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def feature(lang):
    W, H = 1024, 500
    g = board(W, H)
    d = ImageDraw.Draw(g)
    # The home screen with its rooms, tilted into the right half.
    home = shot(lang, "1-home")
    home = home.resize((360, round(home.height * 360 / home.width)), Image.LANCZOS).crop((0, 0, 360, 760))
    lay(g, home, W - 360 - 70, 56, radius=30)
    logo = Image.open(ROOT / "docs/logo.png").convert("RGB").resize((132, 132), Image.LANCZOS)
    g.alpha_composite(rounded(logo, 30), (56, 70))
    name, lines = ("Hob", ["Remembers where", "everything lies"]) if lang == "en" else ("Домовой", ["Помнит,", "где что лежит"])
    d.text((56, 230), name, font=fit(SERIF, name, 88, 470), fill=INK)
    sub = fit(SANS, max(lines, key=lambda l: font(SANS, 44).getlength(l)), 44, 470)
    for i, l in enumerate(lines):
        d.text((58, 350 + i * 54), l, font=sub, fill=AMBER if i == 1 else MUTED)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def icon():
    """The README logo is already a full square, so Play's mask finds no corners."""
    Image.open(ROOT / "docs/logo.png").convert("RGB").resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(lang, name, en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
