"""Generates Celestial Grounds sprites into res/celestial/.

Characters are palette swaps of the trainer sheets already in PlayPokemon
(char1.png, npc1.png, sprites/mom.png), normalised to a 96x128 sheet:
4 rows (down, up, left, right) x 3 columns (stand, step A, step B), 32x32 cells.

Creatures are drawn as pixel art on small canvases, then shaded and outlined.
Run:  python3 tools/make_sprites.py <PlayPokemon project dir>
"""
import math
import os
import sys

from PIL import Image, ImageDraw

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
OUT = os.path.join(ROOT, "res", "celestial")
os.makedirs(OUT, exist_ok=True)


# ---------------------------------------------------------------- characters
def load_rgba(p):
    return Image.open(os.path.join(ROOT, p)).convert("RGBA")


def norm_char1(im):
    """char1/npc1: 96x128, rows down, up, RIGHT, LEFT (see the original Moving.java).
    Swap the last two rows so every sheet is down, up, left, right."""
    out = im.copy()
    out.paste(im.crop((0, 64, 96, 96)), (0, 96))
    out.paste(im.crop((0, 96, 96, 128)), (0, 64))
    return out


def norm_mom(im):
    """mom.png: 256x256 at 2x, 4x4 cells of 64. Rows: down, left, right, up.
    Columns: stand, stepA, stand, stepB."""
    small = im.resize((128, 128), Image.NEAREST)
    out = Image.new("RGBA", (96, 128), (0, 0, 0, 0))
    row_map = {0: 0, 1: 3, 2: 1, 3: 2}  # our row -> mom row
    col_map = [0, 1, 3]
    for r in range(4):
        for c in range(3):
            cell = small.crop((col_map[c] * 32, row_map[r] * 32, col_map[c] * 32 + 32, row_map[r] * 32 + 32))
            out.alpha_composite(cell, (c * 32, r * 32))
    return out


def recolor(im, mapping):
    px = im.load()
    out = im.copy()
    po = out.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            key = (r, g, b)
            if key in mapping:
                nr, ng, nb = mapping[key]
                po[x, y] = (nr, ng, nb, a)
    return out


def ramp(keys, colors):
    return {k: c for k, c in zip(keys, colors)}


CH1_CAP = [(79, 124, 194), (79, 124, 193), (58, 83, 137), (59, 84, 138), (58, 84, 137)]
CH1_SHIRT = [(253, 166, 0), (187, 119, 0)]
CH1_HAIR = [(120, 64, 64), (121, 64, 64), (104, 61, 57)]
NPC_HAIR = [(224, 128, 152), (152, 88, 104), (88, 64, 80)]
NPC_CLOTH = [(168, 168, 216), (96, 96, 144), (120, 120, 160)]
MOM_HAIR = [(103, 54, 54), (159, 85, 85), (52, 27, 27), (183, 119, 119), (148, 84, 82), (172, 108, 106)]
MOM_SHIRT = [(255, 155, 176), (236, 125, 144), (236, 117, 117)]
MOM_JEANS = [(86, 114, 145), (120, 150, 183), (119, 161, 192), (86, 108, 117)]


def cap(c1, c2):
    return ramp(CH1_CAP, [c1, c1, c2, c2, c2])


char1 = norm_char1(load_rgba("res/char1.png"))
npc1 = norm_char1(load_rgba("res/npc1.png"))
mom = norm_mom(load_rgba("res/sprites/mom.png"))

sheets = {
    "araw": char1,
    "buwan": npc1,
    # Dali: straw salakot + red shirt
    "dali": recolor(char1, {**cap((226, 190, 96), (168, 128, 52)), **ramp(CH1_SHIRT, [(222, 62, 52), (150, 32, 30)])}),
    # Propesor Bantay: grey hair, white lab coat
    "propesor": recolor(char1, {**cap((112, 112, 118), (70, 70, 76)), **ramp(CH1_SHIRT, [(238, 238, 238), (176, 176, 190)])}),
    # Kuya Gusmo: black cap, white shirt
    "gusmo": recolor(char1, {**cap((52, 52, 58), (28, 28, 32)), **ramp(CH1_SHIRT, [(245, 245, 245), (190, 190, 196)])}),
    # Team Tambay grunt: orange bandana, grey tank top
    "tambay": recolor(char1, {**cap((236, 120, 40), (170, 80, 20)), **ramp(CH1_SHIRT, [(150, 150, 150), (96, 96, 96)])}),
    # Jeepney conductor: green cap, sky-blue shirt
    "conductor": recolor(char1, {**cap((60, 150, 80), (30, 96, 50)), **ramp(CH1_SHIRT, [(140, 200, 240), (80, 140, 190)])}),
    # Ferry captain: white cap, navy shirt
    "captain": recolor(char1, {**cap((240, 240, 240), (180, 180, 190)), **ramp(CH1_SHIRT, [(40, 60, 120), (24, 36, 80)])}),
    # Barrio villager: black hair, teal clothes
    "villager": recolor(npc1, {**ramp(NPC_HAIR, [(60, 50, 56), (36, 30, 34), (24, 20, 24)]),
                                **ramp(NPC_CLOTH, [(90, 190, 170), (40, 120, 110), (60, 150, 140)])}),
    # Tala: black hair, sunny yellow top, green skirt
    "tala": recolor(mom, {**ramp(MOM_HAIR, [(46, 40, 50), (78, 68, 84), (20, 16, 22), (104, 94, 110), (70, 62, 76), (90, 80, 96)]),
                          **ramp(MOM_SHIRT, [(250, 212, 84), (214, 168, 40), (214, 168, 40)]),
                          **ramp(MOM_JEANS, [(56, 128, 84), (92, 168, 112), (92, 168, 112), (56, 120, 80)])}),
    # Lola: grey hair, cream baro, maroon saya
    "lola": recolor(mom, {**ramp(MOM_HAIR, [(150, 150, 158), (204, 204, 212), (96, 96, 104), (228, 228, 234), (170, 170, 178), (190, 190, 198)]),
                          **ramp(MOM_SHIRT, [(244, 238, 222), (210, 200, 178), (210, 200, 178)]),
                          **ramp(MOM_JEANS, [(122, 40, 58), (168, 70, 90), (168, 70, 90), (110, 36, 52)])}),
    # Dona Liwayway: dark hair, gold terno
    "dona": recolor(mom, {**ramp(MOM_HAIR, [(58, 40, 30), (92, 64, 46), (30, 20, 14), (120, 88, 64), (80, 56, 40), (100, 74, 54)]),
                          **ramp(MOM_SHIRT, [(246, 206, 70), (204, 154, 32), (204, 154, 32)]),
                          **ramp(MOM_JEANS, [(220, 176, 52), (246, 214, 104), (246, 214, 104), (196, 150, 40)])}),
}

# Babaylan statue: mom sheet turned to carved stone
stone = mom.copy()
sp = stone.load()
for y in range(stone.height):
    for x in range(stone.width):
        r, g, b, a = sp[x, y]
        if a:
            v = int(0.3 * r + 0.55 * g + 0.15 * b)
            v = 70 + int(v * 0.62)
            sp[x, y] = (v, v + 4, v + 14, a)
sheets["babaylan"] = stone

for name, im in sheets.items():
    im.save(os.path.join(OUT, f"char_{name}.png"))


# ---------------------------------------------------------------- pixel art kit
class Art:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def ell(self, cx, cy, rx, ry, col):
        self.d.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=col)

    def circ(self, cx, cy, r, col):
        self.ell(cx, cy, r, r, col)

    def poly(self, pts, col):
        self.d.polygon(pts, fill=col)

    def rect(self, x0, y0, x1, y1, col):
        self.d.rectangle((x0, y0, x1, y1), fill=col)

    def line(self, pts, col, w=1):
        self.d.line(pts, fill=col, width=w)

    def px(self, x, y, col):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.im.putpixel((x, y), col if len(col) == 4 else col + (255,))

    def finish(self, outline=(24, 20, 30), shade=True):
        im = self.im
        w, h = im.size
        src = im.load()
        out = im.copy()
        o = out.load()
        if shade:
            for y in range(h):
                for x in range(w):
                    r, g, b, a = src[x, y]
                    if a == 0:
                        continue

                    def empty(xx, yy):
                        return not (0 <= xx < w and 0 <= yy < h) or src[xx, yy][3] == 0 or src[xx, yy][:3] != (r, g, b)

                    if empty(x - 1, y - 1) and empty(x, y - 1):
                        o[x, y] = (min(255, r + 34), min(255, g + 34), min(255, b + 30), a)
                    elif empty(x + 1, y + 1) and empty(x, y + 1):
                        o[x, y] = (int(r * 0.72), int(g * 0.72), int(b * 0.78), a)
        src2 = out.copy().load()
        for y in range(h):
            for x in range(w):
                if src2[x, y][3] != 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < w and 0 <= yy < h and src2[xx, yy][3] != 0:
                        o[x, y] = outline + (255,)
                        break
        return out


def save(art_img, name, scale=1):
    if scale != 1:
        art_img = art_img.resize((art_img.width * scale, art_img.height * scale), Image.NEAREST)
    art_img.save(os.path.join(OUT, name))


def eye(a, x, y, r=2, iris=(30, 24, 30), shine=True):
    a.circ(x, y, r, iris)
    if shine:
        a.px(x - 1, y - 1, (255, 255, 255))


# ---------------------------------------------------------------- creatures (64x64)
def tarsi():
    a = Art(64, 64)
    fur, fur2, belly = (150, 112, 80), (118, 86, 60), (206, 176, 136)
    a.line([(40, 50), (52, 52), (58, 44), (56, 36)], fur2, 3)          # long tail
    a.circ(56, 34, 3, fur)
    a.ell(32, 46, 13, 12, fur)                                       # body
    a.ell(32, 49, 8, 7, belly)
    a.circ(13, 15, 7, fur2); a.circ(51, 15, 7, fur2)                 # ears
    a.circ(13, 15, 4, (220, 150, 140)); a.circ(51, 15, 4, (220, 150, 140))
    a.ell(32, 26, 19, 15, fur)                                       # head
    a.circ(23, 25, 8, (250, 196, 60)); a.circ(41, 25, 8, (250, 196, 60))  # huge eyes
    a.circ(23, 25, 5, (26, 20, 24)); a.circ(41, 25, 5, (26, 20, 24))
    a.circ(21, 23, 1, (255, 255, 255)); a.circ(39, 23, 1, (255, 255, 255))
    a.poly([(30, 33), (34, 33), (32, 35)], (90, 50, 50))             # nose
    for x in (19, 22, 25):                                           # long fingers
        a.line([(x, 52), (x - 1, 58)], fur2, 2)
    for x in (39, 42, 45):
        a.line([(x, 52), (x + 1, 58)], fur2, 2)
    return a.finish()


def kalabaw():
    a = Art(64, 64)
    hide, hide2, horn = (92, 96, 108), (66, 70, 82), (214, 206, 186)
    a.ell(38, 38, 22, 14, hide)                                      # body
    for x in (22, 30, 44, 52):
        a.rect(x - 3, 44, x + 3, 58, hide2)                          # legs
        a.rect(x - 3, 56, x + 3, 58, (40, 40, 46))
    a.line([(58, 34), (62, 44)], hide2, 2)
    a.ell(17, 32, 11, 10, hide)                                      # head
    a.ell(13, 38, 7, 5, (130, 126, 128))                             # muzzle
    a.px(10, 38, (30, 30, 30)); a.px(15, 38, (30, 30, 30))
    a.line([(10, 25), (2, 22), (0, 14), (4, 8)], horn, 3)            # sweeping horns
    a.line([(24, 25), (32, 20), (36, 12), (32, 7)], horn, 3)
    eye(a, 12, 30, 1); eye(a, 22, 30, 1)
    a.ell(40, 33, 10, 4, (112, 116, 128))
    return a.finish()


def tamaraw():
    a = Art(64, 64)
    hide, hide2 = (52, 44, 44), (36, 30, 32)
    a.ell(32, 42, 22, 14, hide)
    for x in (16, 26, 38, 48):
        a.rect(x - 3, 48, x + 3, 60, hide2)
        a.rect(x - 3, 54, x + 3, 60, (220, 214, 200))                # white socks
    a.ell(32, 28, 13, 12, hide)                                      # head (front)
    a.poly([(22, 20), (14, 2), (19, 4), (27, 18)], (196, 190, 176))  # V horns
    a.poly([(42, 20), (50, 2), (45, 4), (37, 18)], (196, 190, 176))
    a.ell(32, 35, 7, 5, (90, 80, 78))
    a.px(30, 35, (20, 20, 20)); a.px(34, 35, (20, 20, 20))
    a.circ(26, 26, 2, (230, 80, 60)); a.circ(38, 26, 2, (230, 80, 60))
    a.line([(28, 20), (36, 20)], (210, 200, 190), 1)                 # white blaze
    return a.finish()


def binturo():
    a = Art(64, 64)
    fur, fur2, cream = (46, 42, 48), (70, 64, 72), (214, 196, 160)
    a.line([(42, 50), (54, 54), (60, 44), (58, 32)], fur2, 5)        # bushy tail
    a.poly([(54, 30), (62, 26), (60, 34)], (255, 150, 40))          # flame tip
    a.poly([(56, 31), (60, 22), (61, 32)], (255, 220, 80))
    a.ell(32, 44, 16, 13, fur)                                       # shaggy body
    for i in range(7):
        x = 18 + i * 4
        a.poly([(x, 52), (x + 2, 58), (x + 4, 52)], fur)
    a.ell(32, 24, 16, 13, fur)                                       # head
    a.poly([(16, 14), (14, 2), (22, 12)], fur); a.poly([(48, 14), (50, 2), (42, 12)], fur)  # tufted ears
    a.line([(14, 2), (12, 0)], cream, 1); a.line([(50, 2), (52, 0)], cream, 1)
    a.ell(32, 28, 9, 7, cream)                                       # face mask
    a.circ(26, 22, 3, (240, 180, 60)); a.circ(38, 22, 3, (240, 180, 60))
    a.circ(26, 22, 1, (20, 16, 18)); a.circ(38, 22, 1, (20, 16, 18))
    a.circ(32, 30, 2, (40, 30, 30))
    for dy in (-1, 1, 3):                                            # whiskers
        a.line([(26, 30 + dy), (14, 28 + dy * 2)], (240, 240, 240), 1)
        a.line([(38, 30 + dy), (50, 28 + dy * 2)], (240, 240, 240), 1)
    return a.finish()


def ahas():
    a = Art(64, 64)
    sc, sc2, band = (222, 92, 40), (176, 58, 30), (90, 34, 24)
    a.ell(32, 52, 22, 9, sc2)                                        # coil
    a.ell(30, 48, 17, 7, sc)
    for x in (18, 28, 38):
        a.rect(x, 44, x + 2, 56, band)
    a.poly([(28, 46), (24, 30), (30, 22), (36, 30), (34, 46)], sc)   # neck
    a.ell(32, 18, 15, 13, sc)                                        # hood
    a.ell(32, 20, 9, 9, (250, 196, 90))
    a.poly([(28, 18), (32, 14), (36, 18), (32, 24)], band)           # hood mark
    a.ell(32, 9, 7, 5, sc2)                                          # head
    eye(a, 29, 8, 1, (250, 230, 60)); eye(a, 35, 8, 1, (250, 230, 60))
    a.line([(32, 13), (32, 17)], (255, 70, 40), 1)
    a.poly([(30, 17), (32, 21), (34, 17)], (255, 200, 60))           # tiny flame
    return a.finish()


def haribon():
    a = Art(64, 64)
    br, br2, cream = (134, 92, 56), (98, 64, 38), (238, 226, 200)
    a.poly([(30, 30), (2, 14), (0, 30), (8, 40), (26, 42)], br2)     # wings
    a.poly([(34, 30), (62, 14), (64, 30), (56, 40), (38, 42)], br2)
    for i in range(4):
        a.line([(4 + i * 5, 34 + i), (8 + i * 5, 42)], br, 1)
        a.line([(60 - i * 5, 34 + i), (56 - i * 5, 42)], br, 1)
    a.ell(32, 40, 11, 14, br)                                        # body
    a.ell(32, 42, 7, 10, cream)
    a.rect(28, 52, 30, 58, (230, 200, 70)); a.rect(34, 52, 36, 58, (230, 200, 70))
    a.ell(32, 20, 10, 10, cream)                                     # head
    for i in range(6):                                               # shaggy crest
        x = 22 + i * 4
        a.poly([(x, 16), (x + 2, 4 + (i % 2) * 3), (x + 4, 16)], br)
    a.poly([(30, 22), (40, 22), (34, 30), (30, 27)], (110, 128, 150))  # blue-grey beak
    eye(a, 27, 19, 1, (40, 30, 20)); eye(a, 37, 19, 1, (40, 30, 20))
    return a.finish()


def dugong():
    a = Art(64, 64)
    sk, sk2, belly = (128, 150, 176), (96, 116, 142), (196, 210, 222)
    a.poly([(50, 40), (62, 32), (60, 44), (62, 54), (50, 46)], sk2)  # fluke
    a.ell(32, 40, 22, 14, sk)                                        # body
    a.ell(30, 46, 15, 6, belly)
    a.ell(24, 52, 6, 3, sk2); a.ell(40, 52, 6, 3, sk2)               # flippers
    a.ell(18, 34, 12, 10, sk)                                        # head
    a.ell(12, 40, 8, 5, belly)                                       # snout
    for i in range(3):
        a.px(8 + i * 3, 42, (70, 80, 90))
    eye(a, 14, 31, 2); eye(a, 23, 31, 2)
    a.ell(10, 34, 2, 1, (240, 160, 170)); a.ell(27, 34, 2, 1, (240, 160, 170))
    a.line([(36, 12), (34, 18)], (250, 250, 255), 1)                 # musical note
    a.circ(33, 19, 2, (250, 250, 255))
    return a.finish()


def pugita():
    a = Art(64, 64)
    pk, pk2, sucker = (196, 92, 168), (150, 60, 130), (246, 186, 220)
    for i, x in enumerate((10, 20, 30, 40, 50, 56)):
        sway = 6 if i % 2 else -4
        a.line([(32, 36), (x, 48), (x + sway, 58), (x + sway - 3, 62)], pk2, 4)
        a.px(x, 49, sucker); a.px(x + sway // 2, 54, sucker)
    a.ell(32, 22, 17, 17, pk)                                        # mantle
    a.ell(26, 14, 5, 4, (220, 130, 196))
    a.circ(25, 28, 5, (255, 255, 255)); a.circ(39, 28, 5, (255, 255, 255))
    a.circ(26, 29, 3, (30, 20, 40)); a.circ(40, 29, 3, (30, 20, 40))
    a.ell(32, 36, 3, 2, pk2)
    a.circ(56, 10, 2, (40, 30, 60))                                  # ink drop
    return a.finish()


def butanding():
    a = Art(64, 64)
    sk, sk2, belly = (62, 92, 132), (42, 66, 102), (210, 222, 230)
    a.poly([(52, 28), (64, 14), (60, 32), (64, 50), (52, 38)], sk2)  # tail
    a.poly([(30, 18), (38, 6), (42, 20)], sk2)                       # dorsal fin
    a.ell(30, 32, 26, 15, sk)                                        # body
    a.ell(26, 40, 20, 6, belly)
    a.poly([(20, 42), (10, 56), (28, 46)], sk2)                      # pectoral fin
    a.rect(6, 34, 18, 36, (30, 40, 56))                              # wide mouth
    for (x, y) in [(14, 24), (22, 22), (30, 20), (38, 24), (46, 26), (18, 30), (28, 28), (36, 31), (44, 32), (24, 35), (40, 37)]:
        a.circ(x, y, 1, (236, 240, 250))                             # star spots
    eye(a, 10, 28, 1)
    return a.finish()


def star_creature(size, body, body2, face=True, crown=None):
    a = Art(size, size)
    cx = cy = size // 2
    R, r = size * 0.44, size * 0.2
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        rad = R if i % 2 == 0 else r
        pts.append((cx + rad * math.cos(ang), cy + 2 + rad * math.sin(ang)))
    a.poly(pts, body)
    a.ell(cx, cy + 4, size * 0.16, size * 0.12, body2)
    if face:
        eye(a, cx - size // 10, cy + 1, max(1, size // 32))
        eye(a, cx + size // 10, cy + 1, max(1, size // 32))
        a.px(cx, cy + size // 10, (180, 80, 60))
    return a


def bituin():
    a = star_creature(32, (255, 226, 100), (255, 244, 180))
    return a.finish(outline=(120, 90, 30))


def apolaki():
    a = Art(64, 64)
    for i in range(12):                                              # sun rays
        ang = i * math.pi / 6
        a.line([(32, 30), (32 + 30 * math.cos(ang), 30 + 30 * math.sin(ang))], (255, 190, 60), 3)
    a.circ(32, 30, 18, (255, 214, 90))
    a.ell(32, 46, 10, 12, (240, 170, 50))                            # warrior body
    a.poly([(22, 50), (14, 62), (26, 58)], (240, 170, 50)); a.poly([(42, 50), (50, 62), (38, 58)], (240, 170, 50))
    a.line([(46, 36), (60, 20)], (250, 250, 250), 2)                 # sun spear
    a.poly([(58, 16), (63, 18), (60, 23)], (255, 255, 200))
    eye(a, 26, 28, 2); eye(a, 38, 28, 2)
    a.rect(30, 35, 34, 36, (190, 90, 50))
    return a.finish(outline=(110, 60, 20))


def mayari():
    a = Art(64, 64)
    a.circ(32, 30, 20, (186, 206, 255))
    a.circ(40, 24, 17, (0, 0, 0, 0))                                 # crescent crown
    a.ell(32, 34, 13, 14, (214, 226, 250))                           # face/body
    a.ell(32, 52, 12, 10, (150, 170, 230))
    a.poly([(20, 52), (10, 62), (26, 60)], (150, 170, 230)); a.poly([(44, 52), (54, 62), (38, 60)], (150, 170, 230))
    for (x, y) in [(8, 8), (56, 10), (52, 44), (10, 40), (58, 30)]:
        a.px(x, y, (255, 255, 255)); a.px(x + 1, y, (200, 220, 255)); a.px(x, y + 1, (200, 220, 255))
    eye(a, 27, 33, 2); eye(a, 37, 33, 2)
    a.rect(30, 39, 34, 40, (120, 110, 170))
    return a.finish(outline=(50, 60, 110))


def bakunawa(body, body2, crack, glow):
    a = Art(96, 96)
    a.ell(48, 78, 40, 14, body2)                                     # lower coils
    a.ell(44, 72, 32, 10, body)
    a.line([(70, 74), (86, 60), (90, 44), (82, 34)], body2, 9)       # tail
    a.poly([(80, 34), (92, 26), (88, 40)], glow)
    a.line([(30, 70), (22, 50), (30, 32), (44, 24)], body, 14)       # neck
    for i in range(5):
        y = 66 - i * 9
        a.line([(18 + (i % 2) * 4, y), (28 + (i % 2) * 4, y + 2)], crack, 2)
    a.ell(54, 22, 20, 14, body)                                      # head
    a.poly([(60, 26), (88, 30), (86, 40), (62, 36)], body)           # open jaw
    a.poly([(64, 30), (84, 32), (82, 36), (64, 34)], (60, 10, 30))
    for x in range(66, 84, 4):
        a.poly([(x, 31), (x + 2, 35), (x + 4, 31)], (250, 250, 250))
    a.poly([(42, 12), (30, 0), (48, 8)], body2)                      # horns
    a.poly([(56, 10), (54, 0), (62, 8)], body2)
    for i in range(6):                                               # mane spikes
        a.poly([(34 + i * 4, 30 + i * 6), (26 + i * 3, 34 + i * 6), (34 + i * 4, 36 + i * 6)], body2)
    a.circ(58, 18, 4, glow); a.circ(58, 18, 2, (255, 255, 255))      # eye
    for (x, y) in [(46, 24), (60, 26), (38, 74), (54, 80), (64, 70)]:  # light cracks
        a.line([(x, y), (x + 4, y + 3), (x + 2, y + 6)], crack, 1)
    return a.finish(outline=(10, 6, 20))


def goat():
    a = Art(32, 32)
    a.ell(18, 18, 10, 6, (236, 232, 222))
    for x in (11, 15, 21, 25):
        a.rect(x, 22, x + 1, 28, (150, 140, 130))
    a.ell(8, 13, 5, 4, (236, 232, 222))
    a.line([(6, 9), (3, 4)], (150, 130, 100), 1); a.line([(9, 9), (9, 4)], (150, 130, 100), 1)
    a.line([(6, 17), (6, 20)], (210, 205, 195), 2)                   # beard
    eye(a, 7, 12, 0)
    return a.finish()


def rooster():
    a = Art(32, 32)
    a.ell(16, 20, 8, 7, (200, 90, 40))
    a.poly([(22, 18), (30, 8), (28, 20)], (40, 70, 60))              # tail
    a.ell(11, 11, 4, 4, (220, 120, 50))
    a.poly([(9, 6), (11, 3), (13, 6)], (230, 40, 40))
    a.poly([(6, 11), (3, 12), (6, 13)], (240, 200, 60))
    a.rect(14, 26, 14, 30, (240, 200, 60)); a.rect(18, 26, 18, 30, (240, 200, 60))
    a.line([(9, 10), (11, 10)], (30, 20, 20), 1)                     # sleepy eye
    return a.finish()


def parol():
    a = star_creature(24, (250, 80, 70), (255, 220, 120), face=False)
    a.line([(12, 18), (8, 23)], (250, 220, 120), 1)
    a.line([(12, 18), (16, 23)], (250, 220, 120), 1)
    return a.finish(outline=(90, 30, 30))


def banga():
    a = Art(24, 24)
    a.ell(12, 15, 9, 8, (176, 96, 60))
    a.rect(8, 4, 16, 8, (176, 96, 60))
    a.rect(7, 3, 17, 4, (140, 72, 44))
    a.line([(4, 14), (20, 14)], (230, 200, 150), 1)
    return a.finish(outline=(60, 30, 20))


def pandesal():
    a = Art(24, 24)
    a.ell(12, 13, 9, 7, (214, 150, 80))
    a.ell(12, 11, 7, 4, (236, 186, 110))
    for x in (8, 12, 16):
        a.px(x, 9, (250, 240, 220))
    return a.finish(outline=(90, 50, 20))


def jeepney():
    a = Art(96, 56)
    a.rect(6, 14, 90, 44, (230, 230, 236))                           # body
    a.rect(6, 30, 90, 34, (220, 40, 50))
    a.rect(6, 34, 90, 36, (40, 90, 200))
    for x in range(14, 80, 13):
        a.rect(x, 18, x + 9, 27, (90, 140, 180))                     # windows
    a.rect(4, 8, 92, 13, (250, 200, 40))                             # roof rack
    a.poly([(70, 8), (78, 0), (86, 8)], (230, 60, 60))               # horse ornament-ish
    a.circ(22, 46, 7, (30, 30, 34)); a.circ(74, 46, 7, (30, 30, 34))
    a.circ(22, 46, 3, (180, 180, 190)); a.circ(74, 46, 3, (180, 180, 190))
    a.rect(88, 32, 94, 38, (255, 240, 160))                          # headlight
    return a.finish(shade=False)


def tricycle():
    a = Art(64, 48)
    a.rect(4, 10, 38, 34, (40, 150, 90))                             # sidecar
    a.rect(8, 14, 34, 22, (120, 200, 230))
    a.rect(40, 18, 58, 30, (200, 50, 50))                            # motorbike
    a.rect(44, 4, 60, 16, (40, 40, 44))                              # videoke speaker
    a.circ(52, 10, 4, (90, 90, 100)); a.circ(52, 10, 2, (20, 20, 20))
    for x in (12, 32, 54):
        a.circ(x, 38, 6, (30, 30, 34)); a.circ(x, 38, 2, (170, 170, 180))
    return a.finish(shade=False)


def boat(gold=False):
    a = Art(96, 48)
    hull = (226, 182, 60) if gold else (200, 70, 60)
    a.poly([(4, 24), (92, 24), (80, 40), (14, 40)], hull)
    a.rect(30, 12, 64, 24, (240, 240, 240) if not gold else (250, 220, 120))
    a.rect(34, 15, 40, 20, (90, 140, 180)); a.rect(46, 15, 52, 20, (90, 140, 180))
    a.line([(0, 30), (16, 30)], (120, 80, 50), 2); a.line([(80, 30), (96, 30)], (120, 80, 50), 2)  # outriggers
    a.line([(4, 34), (92, 34)], (250, 250, 250), 1)
    return a.finish(shade=False)


creatures = {
    "tarsi": tarsi(), "kalabaw": kalabaw(), "tamaraw": tamaraw(),
    "binturo": binturo(), "ahas": ahas(), "haribon": haribon(),
    "dugong": dugong(), "pugita": pugita(), "butanding": butanding(),
    "apolaki": apolaki(), "mayari": mayari(),
}
for k, v in creatures.items():
    save(v, f"mon_{k}.png")
save(bituin(), "mon_bituin.png")
save(bakunawa((54, 44, 104), (34, 28, 70), (250, 240, 190), (250, 240, 160)), "mon_bakunawa.png")
save(bakunawa((40, 110, 170), (26, 74, 126), (180, 240, 255), (140, 230, 255)), "mon_bakunawa_tubig.png")
save(bakunawa((200, 72, 44), (140, 40, 30), (255, 220, 120), (255, 200, 80)), "mon_bakunawa_apoy.png")
save(bakunawa((120, 96, 70), (86, 66, 48), (230, 210, 160), (240, 220, 140)), "mon_bakunawa_lupa.png")
for k, f in {"goat": goat, "rooster": rooster, "parol": parol, "banga": banga, "pandesal": pandesal}.items():
    save(f(), f"obj_{k}.png")
save(jeepney(), "obj_jeepney.png")
save(tricycle(), "obj_tricycle.png")
save(boat(False), "obj_ferry.png")
save(boat(True), "obj_goldboat.png")
print("wrote", len(os.listdir(OUT)), "files to", OUT)
