"""Builds the Celestial Grounds tile maps and writes src/celestial/world/MapGrids.java.

Tile legend (see celestial.world.Tile):
  .  ground          ,  path            "  tall grass (encounters)   f flowers
  T  tree            B  bamboo          R  rock / cliff             F fence
  V  tunnel wall     v  tunnel floor    L  lava                     K cooled lava / stone bridge
  ~  water           =  boardwalk       s  sand                     q seagrass shallows (encounters)
  p  rice paddy      ^  nipa roof       #  house wall               D door
  *  porch           n  bench           C  rooster coop             S Banga Shrine
  O  sign            G  gate post       X  interior wall            o wood floor
  J  shelves / jars  k  lab table       x  temple wall              t temple floor
  c  coral           u  sea floor       E  Babaylan statue          a starstone floor
  A  starstone pillar Q cage            Y  jeepney                  Z boat (blocking)
Run: python3 tools/build_maps.py <PlayPokemon dir>
"""
import os
import random
import sys
from collections import deque

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
WALK = set('.,"fvKs=q*otuaD')   # D handled as warp; walking onto a door warps


class Grid:
    def __init__(self, name, w, h, fill):
        self.name, self.w, self.h = name, w, h
        self.g = [[fill] * w for _ in range(h)]
        self.points = {}
        self.zones = []

    def put(self, x, y, ch):
        assert 0 <= x < self.w and 0 <= y < self.h, (self.name, x, y)
        self.g[y][x] = ch

    def get(self, x, y):
        return self.g[y][x]

    def rect(self, x0, y0, x1, y1, ch):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, ch)

    def border(self, ch, t=1):
        for y in range(self.h):
            for x in range(self.w):
                if x < t or y < t or x >= self.w - t or y >= self.h - t:
                    self.put(x, y, ch)

    def rows(self, y0, rows):
        for i, r in enumerate(rows):
            assert len(r) == self.w, (self.name, y0 + i, len(r), r)
            for x, ch in enumerate(r):
                self.put(x, y0 + i, ch)

    def house(self, x, y, w, doorx):
        self.rect(x, y, x + w - 1, y + 1, '^')
        self.rect(x, y + 2, x + w - 1, y + 2, '#')
        self.put(doorx, y + 2, 'D')

    def mark(self, name, x, y):
        self.points[name] = (x, y)

    def zone(self, name, x0, y0, x1, y1):
        self.zones.append((name, x0, y0, x1, y1))

    def scatter(self, ch, x0, y0, x1, y1, n, seed, on='.'):
        r = random.Random(seed)
        for _ in range(n * 10):
            if n <= 0:
                break
            x, y = r.randint(x0, x1), r.randint(y0, y1)
            if self.g[y][x] == on:
                self.put(x, y, ch)
                n -= 1

    def reach(self, start, extra_walk=''):
        ok = WALK | set(extra_walk)
        seen = {start}
        dq = deque([start])
        while dq:
            x, y = dq.popleft()
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < self.w and 0 <= ny < self.h and (nx, ny) not in seen and self.g[ny][nx] in ok:
                    seen.add((nx, ny))
                    if self.g[ny][nx] != 'D':
                        dq.append((nx, ny))
        return seen

    def check(self, start, targets, extra_walk=''):
        seen = self.reach(self.points[start], extra_walk)
        for t in targets:
            p = self.points[t] if isinstance(t, str) else t
            assert p in seen, f"{self.name}: {t} {p} unreachable from {start}"

    def adjacent_ok(self, start, targets):
        """target is a blocking tile (NPC / object): need a walkable neighbour reachable."""
        seen = self.reach(self.points[start])
        for t in targets:
            x, y = self.points[t]
            assert any((x + dx, y + dy) in seen for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))), f"{self.name}: {t} not adjacent-reachable"


def maze(w, h, seed, loops=6):
    """Perfect maze on odd cells, '.' corridors, '#' walls, plus a few loops."""
    r = random.Random(seed)
    g = [['#'] * w for _ in range(h)]
    cw, ch = (w - 1) // 2, (h - 1) // 2
    stack = [(cw // 2, ch - 1)]
    seen = {stack[0]}
    g[2 * stack[0][1] + 1][2 * stack[0][0] + 1] = '.'
    while stack:
        cx, cy = stack[-1]
        nbrs = [(cx + dx, cy + dy, dx, dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                if 0 <= cx + dx < cw and 0 <= cy + dy < ch and (cx + dx, cy + dy) not in seen]
        if not nbrs:
            stack.pop()
            continue
        nx, ny, dx, dy = r.choice(nbrs)
        g[2 * cy + 1 + dy][2 * cx + 1 + dx] = '.'
        g[2 * ny + 1][2 * nx + 1] = '.'
        seen.add((nx, ny))
        stack.append((nx, ny))
    for _ in range(loops):
        for _t in range(100):
            x, y = r.randrange(1, w - 1), r.randrange(1, h - 1)
            if g[y][x] == '#' and ((x % 2 == 0 and y % 2 == 1) or (x % 2 == 1 and y % 2 == 0)):
                if (g[y][x - 1] == '.' and g[y][x + 1] == '.') or (g[y - 1][x] == '.' and g[y + 1][x] == '.'):
                    g[y][x] = '.'
                    break
    return g


maps = []

# ------------------------------------------------------------------ Barrio Tulog
b = Grid("BARRIO", 32, 21, '.')
b.border('T')
b.rect(1, 1, 30, 1, 'T')
b.rect(14, 0, 17, 19, ',')
b.put(13, 1, 'G'); b.put(18, 1, 'G')
b.rect(5, 8, 27, 8, ',')
b.rect(6, 17, 25, 17, ',')
b.house(3, 4, 5, 5); b.put(5, 7, ',')
b.house(23, 4, 7, 27); b.put(27, 7, ',')
b.put(25, 7, 'O')                       # lab sign
b.put(10, 11, 'S')                      # Banga Shrine
b.put(21, 12, 'O')                      # barrio sign
b.house(3, 13, 6, 6)                    # player's bahay kubo, door (6,15)
b.rect(3, 16, 7, 16, '*')               # porch
b.put(2, 16, 'n')                       # bench
b.put(8, 16, 'C')                       # rooster coop
b.house(22, 13, 5, 24); b.put(24, 16, ',')
b.rect(8, 11, 9, 13, '.')
b.scatter('f', 2, 2, 12, 19, 7, 1)
b.scatter('f', 19, 2, 29, 19, 6, 2)
b.mark("start", 5, 16); b.mark("door_home", 6, 15); b.mark("lola_porch", 6, 16)
b.mark("lab_door", 27, 6); b.mark("lab_out", 27, 7); b.mark("shrine", 10, 11)
b.mark("villager", 12, 9); b.mark("goat", 21, 3); b.mark("dali_gate", 16, 2)
b.mark("from_palayan", 15, 1); b.mark("bench", 2, 16); b.mark("coop", 8, 16)
b.mark("sign_lab", 25, 7); b.mark("sign_barrio", 21, 12)
b.mark("house_nw_door", 5, 6); b.mark("house_se_door", 24, 15)
b.check("start", ["lab_out", "from_palayan", "villager"])
b.adjacent_ok("start", ["shrine", "sign_lab", "sign_barrio", "bench", "coop"])
maps.append(b)

# ------------------------------------------------------------------ Propesor's lab
lab = Grid("LAB", 13, 9, 'o')
lab.border('X')
lab.rect(1, 1, 4, 1, 'J'); lab.rect(8, 1, 11, 1, 'J')
lab.rect(2, 4, 10, 4, 'k')
lab.put(6, 8, 'D')
lab.mark("entry", 6, 7); lab.mark("propesor", 6, 2); lab.mark("door", 6, 8)
lab.mark("tub_tarsi", 3, 4); lab.mark("tub_binturo", 6, 4); lab.mark("tub_dugong", 9, 4)
lab.check("entry", ["propesor"])
maps.append(lab)

# ------------------------------------------------------------------ Map 1 Earth: Palayan Trail
p = Grid("PALAYAN", 30, 30, '.')
p.border('T')
p.rect(1, 1, 28, 1, 'T'); p.rect(1, 28, 28, 28, 'T')
p.rect(13, 0, 16, 29, ',')
for (x0, y0, x1, y1) in [(1, 2, 8, 3), (21, 2, 28, 3), (1, 5, 6, 6), (24, 5, 28, 6), (1, 10, 4, 11),
                         (21, 9, 28, 10), (1, 15, 7, 16), (22, 15, 28, 16), (1, 23, 7, 24), (22, 23, 28, 24)]:
    p.rect(x0, y0, x1, y1, 'p')
p.rect(1, 19, 13, 19, 'p'); p.rect(15, 19, 28, 19, 'p'); p.put(14, 19, ',')
p.rect(1, 13, 5, 14, 'B')
p.rect(17, 8, 28, 8, ',')
for (x0, y0, x1, y1) in [(10, 3, 12, 5), (17, 3, 18, 5), (3, 7, 5, 9), (21, 12, 24, 13), (7, 17, 8, 18),
                         (20, 17, 22, 18), (24, 21, 25, 22), (9, 24, 11, 25), (18, 25, 20, 26)]:
    p.rect(x0, y0, x1, y1, '"')
p.put(12, 21, 'O')
p.put(6, 22, 'S')
p.scatter('f', 2, 20, 11, 27, 4, 3)
p.mark("from_barrio", 14, 27); p.mark("to_barrio_row", 14, 29)
p.mark("kalabaw", 14, 19); p.mark("sign", 12, 21); p.mark("shrine", 6, 22)
p.mark("tala_spawn", 6, 12); p.mark("meet", 14, 12)
p.mark("tricycle_start", 28, 8); p.mark("tricycle_stop", 19, 8); p.mark("gusmo_spawn", 19, 9)
p.mark("from_kawayan", 14, 2)
p.zone("kalabaw_trigger", 12, 20, 17, 20)
p.zone("tala_trigger", 13, 12, 16, 12)
# with Kalabaw moved away, top and bottom connect
p.check("from_barrio", ["from_kawayan", "meet", "tricycle_stop"])
p.adjacent_ok("from_barrio", ["sign", "shrine"])
maps.append(p)

# ------------------------------------------------------------------ Map 1 Earth: Kawayan Maze
KW, KH = 27, 23
kg = maze(KW, KH, seed=7, loops=8)
k = Grid("KAWAYAN", KW, KH, 'B')
for y in range(KH):
    for x in range(KW):
        k.put(x, y, '.' if kg[y][x] == '.' else 'B')
k.put(13, 0, '.'); k.put(13, KH - 1, '.')
k.put(13, 1, '.'); k.put(13, KH - 2, '.')
# undergrowth in dead ends
dead = []
for y in range(1, KH - 1):
    for x in range(1, KW - 1):
        if k.get(x, y) == '.':
            n = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if k.get(x + dx, y + dy) != 'B')
            if n == 1:
                dead.append((x, y))
for (x, y) in dead:
    k.put(x, y, '"')
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if k.get(x + dx, y + dy) == '.' and random.Random(x * 31 + y).random() < 0.6:
            k.put(x + dx, y + dy, '"')
k.put(13, KH - 2, '.'); k.put(13, 1, '.')
k.mark("entry", 13, KH - 2); k.mark("exit", 13, 1)
k.check("entry", ["exit"])
maps.append(k)

# ------------------------------------------------------------------ Map 1 Earth: Tuktok ng Palayan + jeepney stop
t = Grid("TUKTOK", 30, 18, 'T')
t.rows(0, [
    "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTT",
    "TTTTTTTTTRRRRRRRRRRRTTTTTTTTTT",
    "TTTTTTTRR...........RRTTTTTTTT",
    "TTTTTTR...............RTTTTTTT",
    "TTTTTR.................RTTTTTT",
    "TTTTR...................RTTTTT",
    "TTTTR...................RTTTTT",
    "TTTTR....................,,,,T",
    "TTTTTR.......,,..........,,,,T",
    "TTTTTTR......,,.........R,,,,T",
    "TTTTTTTR.....,,........RT,,,,T",
    "TTpppppp.....,,.....ppppT,,,,T",
    "TTpppppp.....,,.....ppppT,,,,T",
    "T............,,..........,,,,T",
    "T...S........,,..........,,,,T",
    "Tpppppppppppp,,ppppppppppppppT",
    "TTTTTTTTTTTTT,,TTTTTTTTTTTTTTT",
    "TTTTTTTTTTTTT,,TTTTTTTTTTTTTTT",
])
t.rect(26, 9, 28, 10, 'Y')
t.mark("entry", 13, 15); t.mark("tamaraw", 14, 4); t.mark("shrine", 4, 14)
t.mark("conductor", 25, 11); t.mark("jeepney", 26, 9); t.mark("stop", 25, 12)
t.zone("tamaraw_trigger", 5, 6, 23, 7)
t.check("entry", ["stop", (14, 5)])
t.adjacent_ok("entry", ["shrine", "conductor"])
maps.append(t)

# ------------------------------------------------------------------ Map 1 Fire: Volcano Trail
v = Grid("BULKAN", 32, 28, 'R')
v.rows(0, [
    "VVVVVVVVVVVVVVVVVVVVVVVVvvVVVVVV",
    "VVVVVVVVVVVVVVVVVVVVVVVVvvVVVVVV",
    "VVVVvvvvvvvvvvVVVVVVVvvvvvVVVVVV",
    "VVVVvVVVVVVVVvVVVVVVVvVVVVVVVVVV",
    "VVVVvVVVVVVVVvvvvvvvvvVVVVVVVVVV",
    "VVVVvvvvvVVVVVVVVVVVVVVVVVVVVVVV",
    "VVVVVVVVvVVVVVVVVVVVVVVVVVVVVVVV",
    "VVVVVVVVvvvvvvvvVVVVVVVVVVVVVVVV",
    "VVVVVVVVVVVVVVVvVVVVVVVVVVVVVVVV",
    "RRRRRRRRRRRRRRRvRRRRRRRRRRRRRRRR",
    'RR....""""....,,......""""....RR',
    'RR..""""......,,....""""......RR',
    'RR..""""..R...,,...R..""""....RR',
    "RR..........RR,,RR............RR",
    "RR............,,..............RR",
    "RRLLLLLLLLLLLLLLLLLLLLLLLLLLLLRR",
    "RRLLLLLLLLLLLLLLLLLLLLLLLLLLLLRR",
    "RR............,,..............RR",
    'RR..""""......,,......""""....RR',
    'RR..""""......,,......""""....RR',
    "RR............,,..............RR",
    "RRRRRR........,,..........RRRRRR",
    "RR....S.......,,.............RRR",
    "RR............,,..............RR",
    "RR....,,,,,,,,,,..............RR",
    "RR....,.......................RR",
    "RR............................RR",
    "RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR",
])
v.rect(2, 24, 4, 25, 'Y')
v.mark("arrive", 5, 25); v.mark("jeepney", 2, 24); v.mark("shrine", 6, 22)
v.mark("lava_a", 14, 16); v.mark("lava_b", 15, 16); v.mark("bank", 14, 17)
v.mark("tunnel_mouth", 15, 10); v.mark("exit", 24, 1); v.mark("from_crater", 24, 1)
v.zone("lava_note", 2, 17, 29, 17)
v.zone("tunnel_trigger", 14, 10, 15, 10)
v.zone("dark_tunnel", 0, 0, 31, 9)
v.check("arrive", ["bank"])
for x in (14, 15):
    v.put(x, 15, 'K'); v.put(x, 16, 'K')
v.check("arrive", ["exit", "tunnel_mouth"])
for x in (14, 15):
    v.put(x, 15, 'L'); v.put(x, 16, 'L')
v.adjacent_ok("arrive", ["shrine"])
maps.append(v)

# ------------------------------------------------------------------ Map 1 Fire: Crater
c = Grid("CRATER", 30, 18, 'R')
c.rows(0, [
    "RRRRRRRRRRRRRRRRRRRRRRRRRRRRRR",
    "RRRRRRRRRR..........RRRRRR~~~~",
    "RRRRRRRR....QQQQ......RRRR~~~~",
    "RRRRRRR.....QQQQ.......RRR~~~~",
    "RRRRRR......QQQQ........RR~~~~",
    "RRRRR....................R~~~~",
    "RRRR......................=~~~",
    "RRRR.......LLLLLL.........==~~",
    "RRRR......LLLLLLLL........=~~~",
    "RRRR......LLLLLLLL........R~~~",
    "RRRR.......LLLLLL.........R~~~",
    "RRRRR.....................RR~~",
    'RRRRR..""""......""""....RRR~~',
    'RRRRRR.""""......""""...RRRR~~',
    "RRRRRR..........S.......RRRR~~",
    "RRRRRRRR......,,......RRRRRR~~",
    "RRRRRRRRRRRR..,,..RRRRRRRRRR~~",
    "RRRRRRRRRRRRRR,,RRRRRRRRRRRRRR",
])
c.mark("entry", 14, 16); c.mark("cage", 12, 2); c.mark("dona", 14, 5); c.mark("shrine", 16, 14)
c.mark("dock", 27, 7); c.mark("captain", 26, 8); c.mark("haribon_land", 14, 5)
c.zone("dona_trigger", 6, 5, 22, 6)
c.check("entry", ["dock", (14, 6)])
c.adjacent_ok("entry", ["shrine", "captain"])
maps.append(c)

# ------------------------------------------------------------------ Map 2: Dagat Tala
d = Grid("DAGAT", 38, 18, '~')
d.rect(1, 7, 7, 14, 's')
d.rect(0, 11, 0, 11, '=')
d.rect(8, 11, 12, 11, '=')
d.rect(13, 6, 20, 14, 's')
d.rect(14, 7, 19, 9, 'q'); d.rect(14, 12, 17, 13, 'q')
d.rect(21, 12, 25, 12, '=')
d.rect(26, 8, 33, 15, 's')
d.rect(27, 12, 32, 14, 'q')
d.rect(29, 5, 29, 7, '=')
d.rect(25, 3, 34, 4, 's')
d.rect(26, 0, 33, 2, 'x')
d.put(29, 2, 'D'); d.put(30, 2, 'D')
d.rect(20, 1, 23, 2, 'q'); d.rect(24, 3, 24, 3, 'q')
d.put(3, 8, 'S')
d.mark("arrive", 2, 11); d.mark("shrine", 3, 8); d.mark("captain", 1, 12)
d.mark("temple_door", 29, 2); d.mark("from_temple", 29, 3)
d.check("arrive", ["from_temple", (30, 3)])
d.adjacent_ok("arrive", ["shrine", "captain"])
maps.append(d)

# ------------------------------------------------------------------ Map 2: Sunken Temple
TW, TH = 27, 21
tg = maze(TW, TH, seed=21, loops=5)
tm = Grid("TEMPLO", TW, TH, 'x')
for y in range(TH):
    for x in range(TW):
        tm.put(x, y, 't' if tg[y][x] == '.' else 'x')
tm.rect(9, 1, 17, 3, 't')               # statue chamber
tm.put(13, 1, 'E')
tm.put(16, 0, 'x')                      # sealed reef door (opens later)
tm.put(16, 1, 't')
tm.put(13, TH - 1, 'D'); tm.put(13, TH - 2, 't')
tm.mark("entry", 13, TH - 2); tm.mark("statue", 13, 1); tm.mark("chamber", 13, 3)
tm.mark("reef_door", 16, 0); tm.mark("reef_in", 16, 1)
tm.zone("chamber_trigger", 9, 2, 17, 3)
tm.check("entry", ["chamber", "reef_in"])
maps.append(tm)

# ------------------------------------------------------------------ Map 2: Butanding's Reef
r = Grid("REEF", 28, 20, 'u')
r.border('c')
r.put(13, 19, 'u'); r.put(13, 0, 'u'); r.put(14, 0, 'u')
r.scatter('c', 1, 1, 26, 18, 30, 5, on='u')
for y in range(0, 20):
    for x in (13, 14):
        r.put(x, y, 'u')
r.rect(9, 5, 18, 11, 'u')
r.rect(3, 14, 6, 17, 'u')
r.rect(20, 3, 24, 6, 'q')
r.put(4, 15, 'S')
r.mark("entry", 13, 18); r.mark("butanding", 14, 7); r.mark("shrine", 4, 15)
r.mark("exit", 13, 1)
r.zone("butanding_trigger", 9, 10, 18, 11)
r.check("entry", ["exit", (13, 11)])
r.adjacent_ok("entry", ["shrine"])
maps.append(r)

# ------------------------------------------------------------------ Map 2 end: Tuktok ng Celestia
ce = Grid("CELESTIA", 24, 20, '~')
ce.rect(4, 3, 19, 18, 'a')
ce.rect(11, 18, 12, 19, 'a')
for (x, y) in [(5, 4), (18, 4), (5, 12), (18, 12), (8, 7), (15, 7)]:
    ce.put(x, y, 'A')
ce.rect(4, 3, 7, 3, '~'); ce.rect(16, 3, 19, 3, '~')
ce.put(6, 16, 'S')
ce.mark("entry", 11, 17); ce.mark("shrine", 6, 16); ce.mark("altar", 11, 9)
ce.mark("bakunawa", 11, 1)
ce.zone("altar_trigger", 8, 9, 15, 11)
ce.check("entry", ["altar"])
ce.adjacent_ok("entry", ["shrine"])
maps.append(ce)


# ------------------------------------------------------------------ export
def jstr(s):
    return '"' + s.replace('\\', '\\\\').replace('"', '\\"') + '"'


out = ["// GENERATED by tools/build_maps.py - edit the Python and re-run, not this file.",
       "package celestial.world;", "", "import java.util.HashMap;", "import java.util.Map;", "",
       "public final class MapGrids {", "\tprivate MapGrids() {}", ""]
for m in maps:
    out.append(f"\tpublic static final String[] {m.name} = {{")
    for row in m.g:
        out.append("\t\t" + jstr(''.join(row)) + ",")
    out.append("\t};")
out.append("")
out.append("\t/** Named tile coordinates, key = MAPNAME.point */")
out.append("\tpublic static final Map<String, int[]> POINTS = new HashMap<>();")
out.append("\t/** Named rectangles {x0,y0,x1,y1}, key = MAPNAME.zone */")
out.append("\tpublic static final Map<String, int[]> ZONES = new HashMap<>();")
out.append("\tstatic {")
for m in maps:
    for k_, (x, y) in m.points.items():
        out.append(f'\t\tPOINTS.put("{m.name}.{k_}", new int[]{{{x}, {y}}});')
    for (n, x0, y0, x1, y1) in m.zones:
        out.append(f'\t\tZONES.put("{m.name}.{n}", new int[]{{{x0}, {y0}, {x1}, {y1}}});')
out.append("\t}")
out.append("}")
path = os.path.join(ROOT, "src", "celestial", "world", "MapGrids.java")
os.makedirs(os.path.dirname(path), exist_ok=True)
open(path, "w").write("\n".join(out) + "\n")
for m in maps:
    print(f"{m.name:9s} {m.w}x{m.h}")
    if m.name in ("KAWAYAN", "TEMPLO"):
        print("\n".join(''.join(r) for r in m.g))
print("ok ->", path)
