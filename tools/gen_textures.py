#!/usr/bin/env python3
"""Generates the mod's pixel-art textures procedurally (run from the repository root)."""
import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.join("src", "main", "resources", "assets", "imperium", "textures")
rnd = random.Random(1234)


def out(path, img):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def noise_fill(img, box, base, amount=0.12, seed=None):
    r = random.Random(seed) if seed is not None else rnd
    x0, y0, x1, y1 = box
    for x in range(x0, x1):
        for y in range(y0, y1):
            img.putpixel((x, y), shade(base, 1 + (r.random() - 0.5) * amount * 2))


def from_mask(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), palette[ch])
    return img


# ---------------------------------------------------------------- items
STEEL = {"a": hexc("2f353d"), "b": hexc("e3e8ee"), "c": hexc("aab4c0"), "d": hexc("76818f"), "e": hexc("c9d1db")}

INGOT = [
    "................",
    "................",
    "................",
    "................",
    "......aaaaa.....",
    "....aabbbbbaa...",
    "..aabbbbbbbbbaa.",
    ".aecbbbbbbbbbcca",
    ".adcccbbbbbcccda",
    ".adddcccccccddda",
    "..aadddddddddaa.",
    "....aaddddddaa..",
    "......aaaaaa....",
    "................",
    "................",
    "................",
]
NUGGET = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......aaa.......",
    ".....abbea......",
    "....abbccda.....",
    "....acccdda.aa..",
    ".....adddaa.aba.",
    "......aaa..acda.",
    "...aa......aaa..",
    "..abca..........",
    "..adda..........",
    "...aa...........",
    "................",
]
HELMET = [
    "................",
    "................",
    "................",
    "....aaaaaaaa....",
    "...abbbbbbbba...",
    "..abeeccccceba..",
    "..abcccccccccba.",
    "..accaaaaaaacca.",
    "..aca......acda.",
    "..ada......ada..",
    "..aaa......aaa..",
    "................",
    "................",
    "................",
    "................",
    "................",
]
CHEST = [
    "................",
    "..aaa......aaa..",
    ".abba......abba.",
    ".abcbaaaaaabcba.",
    ".abccbbbbbbccba.",
    ".aacccceeccccaa.",
    "...acccccccca...",
    "...acccccccca...",
    "...accceeccca...",
    "...acccccccca...",
    "...accddddcca...",
    "...adddddddda...",
    "...aaaaaaaaaa...",
    "................",
    "................",
    "................",
]
LEGS = [
    "................",
    "................",
    "...aaaaaaaaaa...",
    "...abbbbbbbba...",
    "...accceeccca...",
    "...accca.acca...",
    "...accca.acca...",
    "...acca..acca...",
    "...acca..acca...",
    "...acca..acca...",
    "...adda..adda...",
    "...adda..adda...",
    "...aaaa..aaaa...",
    "................",
    "................",
    "................",
]
BOOTS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...aaa....aaa...",
    "...aba....aba...",
    "...aca....aca...",
    "...aca....aca...",
    "..abca....acba..",
    ".abccda..adccba.",
    ".addddd..ddddda.",
    ".aaaaaa..aaaaaa.",
    "................",
    "................",
    "................",
]


def line_px(img, p0, p1, color):
    d = ImageDraw.Draw(img)
    d.line([p0, p1], fill=color, width=1)


def outline(img, color):
    w, h = img.size
    src = img.copy()
    for x in range(w):
        for y in range(h):
            if src.getpixel((x, y))[3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src.getpixel((nx, ny))[3] != 0 and src.getpixel((nx, ny)) != color:
                    img.putpixel((x, y), color)
                    break
    return img


def tool(kind):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wood_l, wood_d = hexc("8a5a2b"), hexc("5a3a1a")
    light, mid, dark = STEEL["b"], STEEL["c"], STEEL["d"]
    if kind == "sword":
        for i in range(10):
            x, y = 4 + i, 11 - i
            img.putpixel((x, y), light)
            img.putpixel((x + 1, y), mid)
            if i < 9:
                img.putpixel((x + 1, y + 1), dark)
        for i in range(5):
            img.putpixel((2 + i, 8 + i), hexc("3d4652"))
        img.putpixel((3, 12), wood_l)
        img.putpixel((2, 13), wood_d)
        img.putpixel((1, 14), hexc("3d4652"))
    else:
        for i in range(10):
            x, y = 3 + i, 13 - i
            img.putpixel((x, y), wood_l if i % 2 else wood_d)
        if kind == "pickaxe":
            pts = [(4, 3), (5, 2), (6, 2), (7, 2), (8, 2), (9, 2), (10, 3), (11, 3), (12, 4), (13, 5), (13, 6), (14, 7), (14, 8), (14, 9)]
            for j, (x, y) in enumerate(pts):
                img.putpixel((x, y), light if j % 3 else mid)
                if y + 1 < 16 and img.getpixel((x, y + 1))[3] == 0:
                    img.putpixel((x, y + 1), dark)
        elif kind == "axe":
            for x in range(8, 14):
                for y in range(1, 7):
                    if (x - 8) + (6 - y) < 8 and not (x < 10 and y > 4):
                        img.putpixel((x, y), light if x > 11 else mid)
            for y in range(1, 6):
                img.putpixel((13, y), dark)
        elif kind == "shovel":
            for x in range(10, 15):
                for y in range(1, 6):
                    if (x - 12) ** 2 + (y - 3) ** 2 <= 5:
                        img.putpixel((x, y), light if x + y < 16 else mid)
            img.putpixel((14, 4), dark)
            img.putpixel((13, 5), dark)
    return outline(img, hexc("23282e"))


def gen_items():
    out("item/steel_ingot.png", from_mask(INGOT, STEEL))
    out("item/steel_nugget.png", from_mask(NUGGET, STEEL))
    out("item/steel_helmet.png", from_mask(HELMET, STEEL))
    out("item/steel_chestplate.png", from_mask(CHEST, STEEL))
    out("item/steel_leggings.png", from_mask(LEGS, STEEL))
    out("item/steel_boots.png", from_mask(BOOTS, STEEL))
    for k in ("sword", "pickaxe", "axe", "shovel"):
        out("item/steel_%s.png" % k, tool(k))


# ---------------------------------------------------------------- blocks
def bevel(img, light=1.18, dark=0.78):
    w, h = img.size
    for i in range(w):
        img.putpixel((i, 0), shade(img.getpixel((i, 0)), light))
        img.putpixel((0, i), shade(img.getpixel((0, i)), light))
        img.putpixel((i, h - 1), shade(img.getpixel((i, h - 1)), dark))
        img.putpixel((w - 1, i), shade(img.getpixel((w - 1, i)), dark))


def stone_bricks(base="7d7d7d", seed=1):
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, (0, 0, 16, 16), hexc(base), 0.08, seed)
    mortar = hexc("4e4e4e")
    for y in (3, 7, 11, 15):
        for x in range(16):
            img.putpixel((x, y), mortar)
    for row, y0 in enumerate((0, 4, 8, 12)):
        off = 0 if row % 2 == 0 else 4
        for x in (off, off + 8):
            for y in range(y0, y0 + 3):
                img.putpixel((x % 16, y), mortar)
    return img


def gen_blocks():
    # steel block: brushed metal with rivets
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        base = shade(hexc("9aa5b1"), 0.94 + 0.12 * rnd.random())
        for x in range(16):
            img.putpixel((x, y), shade(base, 1 + (rnd.random() - 0.5) * 0.06))
    bevel(img, 1.25, 0.7)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel((x, y), hexc("e8eef4"))
        img.putpixel((x + 1, y + 1), hexc("5d6875"))
    out("block/steel_block.png", img)

    # province core: golden frame with a glowing crystal
    img = stone_bricks("6b6b70", 7)
    gold, gold_d = hexc("f2c230"), hexc("a77a12")
    for i in range(2, 14):
        for j in (2, 13):
            img.putpixel((i, j), gold if j == 2 else gold_d)
            img.putpixel((j, i), gold if j == 2 else gold_d)
    for x in range(3, 13):
        for y in range(3, 13):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d <= 4.5:
                t = d / 4.5
                c = (int(120 + 135 * (1 - t)), int(230 - 40 * t), 255, 255)
                img.putpixel((x, y), c)
            else:
                img.putpixel((x, y), hexc("2a2a33"))
    img.putpixel((6, 6), hexc("ffffff"))
    img.putpixel((7, 6), hexc("ffffff"))
    out("block/province_core.png", img)

    # construction site: wooden scaffold with a blueprint
    def scaffold(center):
        img = Image.new("RGBA", (16, 16))
        noise_fill(img, (0, 0, 16, 16), hexc("9c7a48"), 0.1, 3)
        dark = hexc("5c4022")
        for i in range(16):
            img.putpixel((i, 0), dark)
            img.putpixel((i, 15), dark)
            img.putpixel((0, i), dark)
            img.putpixel((15, i), dark)
        for i in range(1, 15):
            img.putpixel((i, i), hexc("7a5a30"))
            img.putpixel((15 - i, i), hexc("7a5a30"))
        center(img)
        return img

    def blueprint(img):
        for x in range(4, 12):
            for y in range(4, 12):
                img.putpixel((x, y), hexc("2c5aa0"))
        for x in range(5, 11):
            img.putpixel((x, 6), hexc("dfe8ff"))
            img.putpixel((x, 10), hexc("dfe8ff"))
        for y in range(6, 11):
            img.putpixel((5, y), hexc("dfe8ff"))
            img.putpixel((10, y), hexc("dfe8ff"))
        img.putpixel((7, 5), hexc("dfe8ff"))
        img.putpixel((8, 5), hexc("dfe8ff"))

    def emblem(img):
        for x in range(4, 12):
            for y in range(4, 12):
                img.putpixel((x, y), hexc("2e7d32"))
        for x in range(5, 11):
            for y in range(5, 11):
                if abs(x - 7.5) + abs(y - 7.5) <= 3.5:
                    img.putpixel((x, y), hexc("ffd54f"))
        img.putpixel((7, 7), hexc("fff8e1"))

    out("block/construction_site.png", scaffold(blueprint))
    out("block/construction_site_active.png", scaffold(emblem))

    # treasury
    side = Image.new("RGBA", (16, 16))
    noise_fill(side, (0, 0, 16, 16), hexc("5b3a1e"), 0.12, 11)
    iron = hexc("8c939b")
    for y in (1, 14):
        for x in range(16):
            side.putpixel((x, y), iron)
    for x in (1, 14):
        for y in range(16):
            side.putpixel((x, y), iron)
    for x in range(6, 10):
        for y in range(6, 10):
            side.putpixel((x, y), hexc("f2c230") if (x + y) % 3 else hexc("c8961c"))
    side.putpixel((7, 9), hexc("3b2a12"))
    bevel(side)
    out("block/treasury_side.png", side)
    top = Image.new("RGBA", (16, 16))
    noise_fill(top, (0, 0, 16, 16), hexc("5b3a1e"), 0.12, 12)
    for x in range(2, 14):
        for y in range(2, 14):
            if rnd.random() < 0.55:
                top.putpixel((x, y), hexc("f2c230") if rnd.random() < 0.7 else hexc("fff2a0"))
            else:
                top.putpixel((x, y), hexc("b8860b"))
    for i in range(16):
        top.putpixel((i, 0), iron)
        top.putpixel((i, 15), iron)
        top.putpixel((0, i), iron)
        top.putpixel((15, i), iron)
    out("block/treasury_top.png", top)

    # steel furnace
    sidef = stone_bricks("6f6f74", 21)
    for y in range(16):
        sidef.putpixel((0, y), hexc("8c939b"))
        sidef.putpixel((15, y), hexc("8c939b"))
    for x in range(16):
        sidef.putpixel((x, 0), hexc("8c939b"))
    out("block/steel_furnace_side.png", sidef)
    topf = Image.new("RGBA", (16, 16))
    noise_fill(topf, (0, 0, 16, 16), hexc("7a7a80"), 0.08, 22)
    for x in range(5, 11):
        for y in range(5, 11):
            topf.putpixel((x, y), hexc("2a2a2a"))
    bevel(topf)
    out("block/steel_furnace_top.png", topf)
    for lit in (False, True):
        f = stone_bricks("6f6f74", 23)
        for x in range(3, 13):
            for y in range(6, 14):
                if lit:
                    t = (y - 6) / 8
                    f.putpixel((x, y), (255, int(200 - 120 * t + rnd.random() * 30), int(40 + rnd.random() * 40), 255))
                else:
                    f.putpixel((x, y), hexc("1d1d1d"))
        for x in range(2, 14):
            f.putpixel((x, 5), hexc("8c939b"))
            f.putpixel((x, 14), hexc("8c939b"))
        for y in range(5, 15):
            f.putpixel((2, y), hexc("8c939b"))
            f.putpixel((13, y), hexc("8c939b"))
        out("block/steel_furnace_front%s.png" % ("_on" if lit else ""), f)


# ---------------------------------------------------------------- armor layers
def gen_armor():
    for layer in (1, 2):
        img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        regions = [(0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32)] if layer == 1 else [(16, 16, 40, 32), (0, 16, 16, 32)]
        for (x0, y0, x1, y1) in regions:
            for x in range(x0, x1):
                for y in range(y0, y1):
                    base = hexc("a9b3bf")
                    if (y - y0) % 4 == 0:
                        base = hexc("cdd5de")
                    if (x - x0) % 8 == 7:
                        base = hexc("7c8794")
                    img.putpixel((x, y), shade(base, 1 + (rnd.random() - 0.5) * 0.1))
        # visor slit on the helmet front
        if layer == 1:
            for x in range(9, 15):
                img.putpixel((x, 12), hexc("1d2228"))
        out("models/armor/steel_layer_%d.png" % layer, img)


# ---------------------------------------------------------------- skins
FACES = {"front": None}


def box(img, u, v, w, h, d, faces):
    """Fills a cube's UV unwrap. faces: dict with keys top,bottom,right,front,left,back -> callable(img,x,y,w,h)."""
    rects = {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }
    for k, (x, y, ww, hh) in rects.items():
        f = faces.get(k) or faces.get("all")
        if f:
            f(img, x, y, ww, hh)


def solid(color, amount=0.06):
    def f(img, x, y, w, h):
        for i in range(x, x + w):
            for j in range(y, y + h):
                img.putpixel((i, j), shade(color, 1 + (rnd.random() - 0.5) * amount * 2))
    return f


def layered(top_color, top_rows, bottom_color):
    def f(img, x, y, w, h):
        for i in range(x, x + w):
            for j in range(y, y + h):
                c = top_color if j - y < top_rows else bottom_color
                img.putpixel((i, j), shade(c, 1 + (rnd.random() - 0.5) * 0.1))
    return f


def bands(colors):
    """colors: list of (rows, color) from top."""
    def f(img, x, y, w, h):
        for j in range(h):
            acc = 0
            col = colors[-1][1]
            for rows, c in colors:
                if j < acc + rows:
                    col = c
                    break
                acc += rows
            for i in range(x, x + w):
                img.putpixel((i, y + j), shade(col, 1 + (rnd.random() - 0.5) * 0.1))
    return f


def face(skin, hair, eyes, female=False, beard=None, hood=None):
    def f(img, x, y, w, h):
        solid(skin, 0.03)(img, x, y, w, h)
        top = hood or hair
        for i in range(x, x + w):
            img.putpixel((i, y), top)
            if female or hood:
                img.putpixel((i, y + 1), top)
        if female:
            for j in range(y, y + 6):
                img.putpixel((x, j), hair)
                img.putpixel((x + w - 1, j), hair)
        if hood:
            for j in range(y, y + h):
                img.putpixel((x, j), hood)
                img.putpixel((x + w - 1, j), hood)
        ey = y + 4
        img.putpixel((x + 1, ey), (255, 255, 255, 255))
        img.putpixel((x + 2, ey), eyes)
        img.putpixel((x + 5, ey), eyes)
        img.putpixel((x + 6, ey), (255, 255, 255, 255))
        img.putpixel((x + 3, y + 5), shade(skin, 0.85))
        img.putpixel((x + 4, y + 5), shade(skin, 0.85))
        if beard:
            for i in range(x + 1, x + w - 1):
                img.putpixel((i, y + 7), beard)
            img.putpixel((x + 2, y + 6), beard)
            img.putpixel((x + 5, y + 6), beard)
        else:
            img.putpixel((x + 3, y + 6), shade(skin, 0.7))
            img.putpixel((x + 4, y + 6), shade(skin, 0.7))
    return f


def head_side(skin, hair, female, hood=None):
    def f(img, x, y, w, h):
        solid(skin, 0.03)(img, x, y, w, h)
        rows = 6 if female else 3
        col = hood or hair
        for j in range(y, y + (h if hood else rows)):
            for i in range(x, x + w):
                if hood or j - y < rows:
                    img.putpixel((i, j), shade(col, 1 + (rnd.random() - 0.5) * 0.1))
    return f


def skin_image(skin, hair, eyes, shirt, pants, shoes, female=False, beard=None, belt=None, hood=None, apron=None, chain=False):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    hair_all = hood or hair
    # head
    box(img, 0, 0, 8, 8, 8, {
        "top": solid(hair_all, 0.1), "bottom": solid(skin), "front": face(skin, hair, eyes, female, beard, hood),
        "right": head_side(skin, hair, female, hood), "left": head_side(skin, hair, female, hood),
        "back": solid(hair_all, 0.1)})
    body_bands = [(10, shirt), (2, belt or shade(shirt, 0.6))]

    def body_front(img_, x, y, w, h):
        bands(body_bands)(img_, x, y, w, h)
        if chain:
            for i in range(x, x + w):
                for j in range(y, y + 10):
                    if (i + j) % 2 == 0:
                        img_.putpixel((i, j), shade(shirt, 1.25))
        if apron:
            for i in range(x + 1, x + w - 1):
                for j in range(y + 3, y + h):
                    img_.putpixel((i, j), shade(apron, 1 + (rnd.random() - 0.5) * 0.1))
        # collar
        img_.putpixel((x + 3, y), shade(shirt, 0.7))
        img_.putpixel((x + 4, y), shade(shirt, 0.7))

    box(img, 16, 16, 8, 12, 4, {"all": bands(body_bands), "front": body_front, "top": solid(shirt), "bottom": solid(pants)})
    arm = bands([(4, shirt), (6, shirt if not female else shirt), (2, skin)])
    arm_short = bands([(5, shirt), (7, skin)])
    box(img, 40, 16, 4, 12, 4, {"all": arm_short if female else arm, "top": solid(shirt), "bottom": solid(skin)})
    box(img, 32, 48, 4, 12, 4, {"all": arm_short if female else arm, "top": solid(shirt), "bottom": solid(skin)})
    leg = bands([(9, pants), (3, shoes)])
    if female:
        leg = bands([(8, pants), (1, shade(pants, 0.8)), (3, shoes)])
    box(img, 0, 16, 4, 12, 4, {"all": leg, "top": solid(pants), "bottom": solid(shoes)})
    box(img, 16, 48, 4, 12, 4, {"all": leg, "top": solid(pants), "bottom": solid(shoes)})
    if female:
        # long hair on the hat layer, back of the head
        box(img, 32, 0, 8, 8, 8, {"back": solid(hair, 0.1)})
    return img


SKIN_TONES = ["f1c27d", "e0ac69", "c68642", "ffdbac", "8d5524", "f5d0b0"]
HAIRS = ["3b2314", "6a4e23", "1c1c1c", "c9a14a", "8e3b1f", "b8b8b8"]
SHIRTS_M = ["7b5e3b", "4a6b3a", "5b6e8c", "8c3b3b", "c2b280", "4b4b4b"]
SHIRTS_F = ["8e4f7a", "3b6b8c", "b5653b", "5f8a4e", "c9a3d6", "a33b3b"]
PANTS = ["4e3b2a", "3b4b5e", "5a4a3a", "2f3a2f", "6b5b45", "3a3a3a"]


def gen_skins():
    for i in range(6):
        sk, hr = hexc(SKIN_TONES[i]), hexc(HAIRS[i])
        man = skin_image(sk, hr, hexc("2b4a7a"), hexc(SHIRTS_M[i]), hexc(PANTS[i]), hexc("2b1d12"),
                         beard=hexc(HAIRS[i]) if i % 2 == 0 else None, belt=hexc("3b2a1a"))
        out("entity/citizen/man_%d.png" % i, man)
        woman = skin_image(sk, hexc(HAIRS[(i + 2) % 6]), hexc("3a6b3a"), hexc(SHIRTS_F[i]), hexc(SHIRTS_F[i]), hexc("2b1d12"),
                           female=True, belt=hexc("e8dcc0"), apron=hexc("ece4d0") if i % 2 == 0 else None)
        out("entity/citizen/woman_%d.png" % i, woman)
    sk = hexc("e0ac69")
    out("entity/soldier/militia.png", skin_image(sk, hexc("4a3220"), hexc("2b4a7a"), hexc("9c7a4c"), hexc("5a4630"), hexc("2b1d12"),
                                                 beard=hexc("4a3220"), belt=hexc("3b2a1a")))
    out("entity/soldier/swordsman.png", skin_image(hexc("f1c27d"), hexc("1c1c1c"), hexc("3a3a3a"), hexc("8a8f96"), hexc("4b4b52"),
                                                   hexc("2a2a2a"), belt=hexc("5a3a1e"), chain=True))
    out("entity/soldier/archer.png", skin_image(hexc("c68642"), hexc("2b1d12"), hexc("2b4a7a"), hexc("3f6b2f"), hexc("4e3b2a"),
                                                hexc("2b1d12"), belt=hexc("6a4a2a"), hood=hexc("2f5524")))
    out("entity/soldier/knight.png", skin_image(hexc("ffdbac"), hexc("c9a14a"), hexc("2b4a7a"), hexc("aab4c0"), hexc("8a939e"),
                                                hexc("5d6875"), belt=hexc("3b2a1a"), chain=True))
    out("entity/soldier/sapper.png", skin_image(hexc("8d5524"), hexc("1c1c1c"), hexc("2b2b2b"), hexc("6b4a2b"), hexc("3b3b3b"),
                                                hexc("1d1d1d"), beard=hexc("1c1c1c"), belt=hexc("2a1a0a"), apron=hexc("4a3420")))

    # tabard: white cloth on the jacket layer, tinted with the nation colour in game
    tab = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    def cloth(img, x, y, w, h):
        for i in range(x + 1, x + w - 1):
            for j in range(y, y + h - 1):
                v = 235 if (i - x) % 3 else 210
                if j == y + h - 2:
                    v = 180
                img.putpixel((i, j), (v, v, v, 255))
        # emblem
        cx = x + w // 2
        img.putpixel((cx - 1, y + 4), (150, 150, 150, 255))
        img.putpixel((cx, y + 4), (150, 150, 150, 255))
        img.putpixel((cx - 1, y + 5), (150, 150, 150, 255))
        img.putpixel((cx, y + 5), (150, 150, 150, 255))
    box(tab, 16, 32, 8, 12, 4, {"front": cloth, "back": cloth})
    # shoulder pieces on the sleeves
    def shoulder(img, x, y, w, h):
        for i in range(x, x + w):
            for j in range(y, y + 3):
                img.putpixel((i, j), (225, 225, 225, 255))
    box(tab, 40, 32, 4, 12, 4, {"all": shoulder, "top": solid((230, 230, 230, 255), 0)})
    box(tab, 48, 48, 4, 12, 4, {"all": shoulder, "top": solid((230, 230, 230, 255), 0)})
    out("entity/tabard.png", tab)

    # sash for citizens: diagonal band across the jacket layer
    sash = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    def band(img, x, y, w, h):
        for k in range(h):
            i = x + int(k * w / h)
            for t in (0, 1):
                if x <= i + t < x + w:
                    img.putpixel((i + t, y + k), (230, 230, 230, 255))
    box(sash, 16, 32, 8, 12, 4, {"front": band, "back": band})
    out("entity/sash.png", sash)


def gen_pack():
    img = Image.new("RGBA", (64, 64), hexc("1b1e24"))
    d = ImageDraw.Draw(img)
    d.polygon([(32, 6), (56, 20), (56, 44), (32, 58), (8, 44), (8, 20)], fill=hexc("2e7d32"), outline=hexc("f2c230"))
    d.polygon([(32, 16), (46, 32), (32, 48), (18, 32)], fill=hexc("f2c230"))
    d.rectangle([29, 26, 35, 38], fill=hexc("fff8e1"))
    path = os.path.join("src", "main", "resources", "imperium_logo.png")
    img.resize((128, 128), Image.NEAREST).save(path)


if __name__ == "__main__":
    gen_items()
    gen_blocks()
    gen_armor()
    gen_skins()
    gen_pack()
    print("textures generated")
