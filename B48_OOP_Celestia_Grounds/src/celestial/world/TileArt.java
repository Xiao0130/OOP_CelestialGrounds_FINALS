package celestial.world;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Procedural pixel-art tiles. Each tile is painted at 24x24 and shown at 2x (48px),
 * the same pixel size as the character sprites. Neighbours are passed in so edges
 * (roof eaves, paddy dikes, shorelines) can be drawn.
 */
public final class TileArt {

	private TileArt() {}

	private static final int S = 24;

	/** Paints one tile into a 48x48 image. n = neighbour tiles: up, down, left, right. */
	public static BufferedImage paint(Tile t, Theme th, int x, int y, Tile up, Tile down, Tile left, Tile right) {
		int[] p = new int[S * S];
		Random r = new Random((x * 73856093L) ^ (y * 19349663L) ^ (t.ordinal() * 83492791L) ^ th.ordinal());
		switch (t) {
			case GROUND: ground(p, th, r); break;
			case FLOWERS: ground(p, th, r); flowers(p, r); break;
			case PATH: path(p, th, r); break;
			case TALL: ground(p, th, r); tall(p, th, r); break;
			case TREE: ground(p, th, r); tree(p, th, r); break;
			case BAMBOO: forest(p, r); bamboo(p, r); break;
			case ROCK: ground(p, th, r); rock(p, th, r, up != Tile.ROCK); break;
			case FENCE: ground(p, th, r); fence(p); break;
			case TUNNEL_WALL: basalt(p, r, true); break;
			case TUNNEL: basalt(p, r, false); break;
			case LAVA: lava(p, r); break;
			case COOLED: cooled(p, r); break;
			case WATER: water(p, th, r, up, down, left, right); break;
			case BOARD: water(p, th, r, Tile.WATER, Tile.WATER, Tile.WATER, Tile.WATER); board(p, r, left == Tile.BOARD || right == Tile.BOARD || (left != null && left.walkable) || (right != null && right.walkable)); break;
			case SAND: sand(p, th, r); break;
			case SEAGRASS: seagrass(p, th, r); break;
			case PADDY: paddy(p, r, up != Tile.PADDY, down != Tile.PADDY, left != Tile.PADDY, right != Tile.PADDY); break;
			case ROOF: roof(p, r, up != Tile.ROOF, down != Tile.ROOF, left != Tile.ROOF, right != Tile.ROOF); break;
			case WALL: wall(p, r, x); break;
			case DOOR: wall(p, r, 0); door(p); break;
			case PORCH: porch(p, r); break;
			case BENCH: ground(p, th, r); bench(p); break;
			case COOP: ground(p, th, r); coop(p); break;
			case SHRINE: ground(p, th, r); shrine(p); break;
			case SIGN: ground(p, th, r); sign(p); break;
			case GATEPOST: path(p, th, r); post(p); break;
			case IWALL: iwall(p, r, down); break;
			case WOOD: wood(p, r); break;
			case SHELF: iwall(p, r, Tile.WOOD); shelf(p, r); break;
			case TABLE: wood(p, r); table(p, up != Tile.TABLE && left != Tile.TABLE, left != Tile.TABLE, right != Tile.TABLE); break;
			case TEMPLE_WALL: templeWall(p, r, down != Tile.TEMPLE_WALL); break;
			case TEMPLE: case STATUE: templeFloor(p, r); break;
			case CORAL: seafloor(p, r); coral(p, r); break;
			case SEAFLOOR: seafloor(p, r); break;
			case STAR: star(p, r); break;
			case PILLAR: star(p, r); pillar(p); break;
			case CAGE: case JEEPNEY: case BOAT:
				if (th == Theme.CRATER || th == Theme.BULKAN) basalt(p, r, false); else path(p, th, r);
				break;
			default: ground(p, th, r);
		}
		BufferedImage im = new BufferedImage(S * 2, S * 2, BufferedImage.TYPE_INT_ARGB);
		for (int yy = 0; yy < S; yy++)
			for (int xx = 0; xx < S; xx++) {
				int c = p[yy * S + xx];
				im.setRGB(xx * 2, yy * 2, c);
				im.setRGB(xx * 2 + 1, yy * 2, c);
				im.setRGB(xx * 2, yy * 2 + 1, c);
				im.setRGB(xx * 2 + 1, yy * 2 + 1, c);
			}
		return im;
	}

	// ------------------------------------------------------------------ pixel helpers
	private static int rgb(int r, int g, int b) {
		return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
	}

	private static int rgb(Color c) { return c.getRGB() | 0xFF000000; }

	private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

	private static int shade(int c, int d) {
		return rgb(((c >> 16) & 255) + d, ((c >> 8) & 255) + d, (c & 255) + d);
	}

	private static void set(int[] p, int x, int y, int c) {
		if (x >= 0 && y >= 0 && x < S && y < S) p[y * S + x] = c;
	}

	private static void fill(int[] p, int x0, int y0, int x1, int y1, int c) {
		for (int y = y0; y <= y1; y++)
			for (int x = x0; x <= x1; x++) set(p, x, y, c);
	}

	private static void noise(int[] p, int base, Random r, int amount, int spread) {
		for (int i = 0; i < S * S; i++) p[i] = base;
		for (int i = 0; i < amount; i++) set(p, r.nextInt(S), r.nextInt(S), shade(base, r.nextInt(spread * 2 + 1) - spread));
	}

	private static void disc(int[] p, int cx, int cy, int rad, int c) {
		for (int y = -rad; y <= rad; y++)
			for (int x = -rad; x <= rad; x++)
				if (x * x + y * y <= rad * rad + rad / 2) set(p, cx + x, cy + y, c);
	}

	// ------------------------------------------------------------------ tiles
	private static void ground(int[] p, Theme th, Random r) {
		switch (th) {
			case DAGAT: sand(p, th, r); return;
			case REEF: seafloor(p, r); return;
			case CELESTIA: star(p, r); return;
			case LAB: wood(p, r); return;
			case TEMPLO: templeFloor(p, r); return;
			case BULKAN: case CRATER: basalt(p, r, false); ash(p, r); return;
			default:
		}
		int base = rgb(th.ground);
		noise(p, base, r, 70, 14);
		for (int i = 0; i < 6; i++) {
			int x = r.nextInt(S), y = r.nextInt(S - 2);
			set(p, x, y, shade(base, 26));
			set(p, x, y + 1, shade(base, -18));
		}
	}

	private static void ash(int[] p, Random r) {
		for (int i = 0; i < 8; i++) set(p, r.nextInt(S), r.nextInt(S), rgb(130, 112, 104));
	}

	private static void forest(int[] p, Random r) {
		noise(p, rgb(54, 94, 52), r, 80, 12);
		for (int i = 0; i < 5; i++) set(p, r.nextInt(S), r.nextInt(S), rgb(110, 92, 50));
	}

	private static void path(int[] p, Theme th, Random r) {
		if (th == Theme.BULKAN || th == Theme.CRATER) {
			noise(p, rgb(74, 64, 62), r, 60, 10);
			return;
		}
		if (th == Theme.KAWAYAN) {
			forest(p, r);
			return;
		}
		int base = rgb(th.path);
		noise(p, base, r, 60, 12);
		for (int i = 0; i < 4; i++) {
			int x = r.nextInt(S - 2), y = r.nextInt(S - 2);
			fill(p, x, y, x + 1, y, shade(base, -30));
			set(p, x, y - 1, shade(base, 20));
		}
	}

	private static void flowers(int[] p, Random r) {
		int[] cols = { rgb(240, 80, 90), rgb(250, 220, 80), rgb(250, 250, 250), rgb(220, 120, 220) };
		for (int i = 0; i < 4; i++) {
			int x = 2 + r.nextInt(S - 4), y = 2 + r.nextInt(S - 4);
			int c = cols[r.nextInt(cols.length)];
			set(p, x - 1, y, c); set(p, x + 1, y, c); set(p, x, y - 1, c); set(p, x, y + 1, c);
			set(p, x, y, rgb(250, 200, 60));
		}
	}

	private static void tall(int[] p, Theme th, Random r) {
		int dark, mid, light;
		if (th == Theme.BULKAN || th == Theme.CRATER) {
			dark = rgb(120, 92, 40); mid = rgb(170, 138, 64); light = rgb(214, 186, 100);
		} else if (th == Theme.KAWAYAN) {
			dark = rgb(28, 70, 36); mid = rgb(48, 110, 52); light = rgb(84, 150, 70);
		} else {
			dark = rgb(40, 110, 46); mid = rgb(62, 150, 58); light = rgb(120, 196, 90);
		}
		for (int row = 0; row < 4; row++) {
			for (int i = 0; i < 7; i++) {
				int x = i * 3 + (row % 2) + r.nextInt(2);
				int base = row * 6 + 6;
				int h = 4 + r.nextInt(3);
				for (int k = 0; k < h; k++) set(p, x, base - k, k == h - 1 ? light : k > h / 2 ? mid : dark);
				set(p, x + 1, base - 1, dark);
			}
		}
	}

	private static void tree(int[] p, Theme th, Random r) {
		int leaf = th == Theme.TUKTOK ? rgb(52, 120, 56) : rgb(46, 112, 58);
		fill(p, 10, 17, 13, 23, rgb(110, 76, 48));
		set(p, 10, 17, rgb(140, 100, 64));
		disc(p, 12, 10, 10, shade(leaf, -22));
		disc(p, 11, 9, 9, leaf);
		disc(p, 8, 6, 3, shade(leaf, 26));
		disc(p, 15, 8, 2, shade(leaf, 18));
		for (int i = 0; i < 8; i++) set(p, 3 + r.nextInt(17), 2 + r.nextInt(14), shade(leaf, -30));
	}

	private static void bamboo(int[] p, Random r) {
		int[] xs = { 2 + r.nextInt(3), 9 + r.nextInt(3), 16 + r.nextInt(3) };
		for (int x : xs) {
			for (int y = 0; y < S; y++) {
				set(p, x, y, rgb(120, 170, 70));
				set(p, x + 1, y, rgb(96, 146, 56));
				set(p, x + 2, y, rgb(70, 112, 44));
			}
			for (int y = r.nextInt(5); y < S; y += 6) {
				set(p, x, y, rgb(196, 214, 120)); set(p, x + 1, y, rgb(60, 100, 40)); set(p, x + 2, y, rgb(60, 100, 40));
			}
			set(p, x + 3, 4 + r.nextInt(14), rgb(70, 140, 60));
			set(p, x + 4, 4 + r.nextInt(14), rgb(90, 160, 70));
		}
	}

	private static void rock(int[] p, Theme th, Random r, boolean top) {
		boolean volcanic = th == Theme.BULKAN || th == Theme.CRATER;
		int base = volcanic ? rgb(60, 50, 52) : rgb(128, 120, 112);
		noise(p, base, r, 90, 14);
		if (top) fill(p, 0, 0, S - 1, 3, shade(base, 30));
		for (int i = 0; i < 3; i++) {
			int x = r.nextInt(S - 6), y = 5 + r.nextInt(S - 8);
			for (int k = 0; k < 5; k++) set(p, x + k, y + (k % 2), shade(base, -36));
		}
		if (volcanic && r.nextInt(3) == 0) {
			int x = r.nextInt(S - 4), y = r.nextInt(S - 4);
			set(p, x, y, rgb(220, 90, 40)); set(p, x + 1, y + 1, rgb(250, 140, 50));
		}
	}

	private static void fence(int[] p) {
		int wood = rgb(150, 106, 64);
		fill(p, 0, 8, S - 1, 9, wood);
		fill(p, 0, 15, S - 1, 16, wood);
		fill(p, 3, 5, 5, 20, shade(wood, -20));
		fill(p, 17, 5, 19, 20, shade(wood, -20));
	}

	private static void basalt(int[] p, Random r, boolean wall) {
		int base = wall ? rgb(42, 34, 38) : rgb(78, 68, 66);
		noise(p, base, r, 90, 10);
		if (wall) {
			for (int i = 0; i < 2; i++) {
				int x = r.nextInt(S - 4), y = r.nextInt(S);
				for (int k = 0; k < 4; k++) set(p, x + k, y + (k / 2), rgb(120, 44, 30));
			}
		}
	}

	private static void lava(int[] p, Random r) {
		noise(p, rgb(232, 86, 34), r, 120, 20);
		for (int i = 0; i < 5; i++) disc(p, r.nextInt(S), r.nextInt(S), 2, rgb(252, 182, 60));
		for (int i = 0; i < 3; i++) disc(p, r.nextInt(S), r.nextInt(S), 1, rgb(120, 40, 30));
	}

	private static void cooled(int[] p, Random r) {
		noise(p, rgb(70, 60, 60), r, 60, 10);
		for (int y = 0; y < S; y += 8) fill(p, 0, y, S - 1, y, rgb(210, 110, 50));
		for (int x = 0; x < S; x += 12) fill(p, x, 0, x, S - 1, rgb(160, 80, 40));
	}

	private static void water(int[] p, Theme th, Random r, Tile up, Tile down, Tile left, Tile right) {
		int base = rgb(th.water);
		noise(p, base, r, 40, 8);
		for (int i = 0; i < 3; i++) {
			int x = r.nextInt(S - 6), y = r.nextInt(S);
			for (int k = 0; k < 5; k++) set(p, x + k, y, shade(base, 36));
		}
		if (th == Theme.CELESTIA) {
			for (int i = 0; i < 2; i++) set(p, r.nextInt(S), r.nextInt(S), rgb(230, 230, 255));
		}
		int foam = rgb(220, 240, 250);
		if (up != null && up != Tile.WATER && up != Tile.BOARD) fill(p, 0, 0, S - 1, 1, foam);
		if (down != null && down != Tile.WATER && down != Tile.BOARD) fill(p, 0, S - 2, S - 1, S - 1, shade(base, -30));
		if (left != null && left != Tile.WATER && left != Tile.BOARD) fill(p, 0, 0, 1, S - 1, foam);
		if (right != null && right != Tile.WATER && right != Tile.BOARD) fill(p, S - 2, 0, S - 1, S - 1, foam);
	}

	private static void board(int[] p, Random r, boolean horizontal) {
		int wood = rgb(170, 122, 74);
		if (horizontal) {
			fill(p, 0, 4, S - 1, 19, wood);
			for (int x = 0; x < S; x += 6) fill(p, x, 4, x, 19, shade(wood, -40));
			fill(p, 0, 4, S - 1, 4, shade(wood, 30));
			fill(p, 0, 20, S - 1, 21, rgb(90, 60, 40));
		} else {
			fill(p, 4, 0, 19, S - 1, wood);
			for (int y = 0; y < S; y += 6) fill(p, 4, y, 19, y, shade(wood, -40));
			fill(p, 4, 0, 4, S - 1, shade(wood, 30));
		}
	}

	private static void sand(int[] p, Theme th, Random r) {
		int base = rgb(226, 208, 150);
		noise(p, base, r, 60, 12);
		for (int i = 0; i < 3; i++) set(p, r.nextInt(S), r.nextInt(S), rgb(250, 240, 220));
	}

	private static void seagrass(int[] p, Theme th, Random r) {
		int base = th == Theme.REEF ? rgb(70, 130, 150) : rgb(84, 168, 186);
		noise(p, base, r, 40, 8);
		for (int i = 0; i < 9; i++) {
			int x = r.nextInt(S), y = 6 + r.nextInt(S - 6);
			int h = 4 + r.nextInt(4);
			for (int k = 0; k < h; k++) set(p, x + (k / 3) % 2, y - k, k == h - 1 ? rgb(120, 200, 110) : rgb(50, 130, 80));
		}
	}

	private static void paddy(int[] p, Random r, boolean t, boolean b, boolean l, boolean rt) {
		int water = rgb(108, 168, 150);
		noise(p, water, r, 40, 8);
		for (int y = 4; y < S; y += 6)
			for (int x = 2 + (y % 4); x < S; x += 5) {
				set(p, x, y, rgb(150, 210, 80));
				set(p, x, y - 1, rgb(200, 230, 110));
				set(p, x - 1, y - 1, rgb(120, 190, 70));
			}
		int dike = rgb(124, 98, 62);
		if (t) fill(p, 0, 0, S - 1, 1, dike);
		if (b) fill(p, 0, S - 3, S - 1, S - 1, shade(dike, -10));
		if (l) fill(p, 0, 0, 1, S - 1, dike);
		if (rt) fill(p, S - 2, 0, S - 1, S - 1, dike);
	}

	private static void roof(int[] p, Random r, boolean top, boolean bottom, boolean left, boolean right) {
		int base = rgb(196, 160, 84);
		noise(p, base, r, 30, 10);
		for (int y = 1; y < S; y += 3)
			for (int x = 0; x < S; x++)
				if ((x + y) % 5 != 0) set(p, x, y, shade(base, -24));
		for (int i = 0; i < 12; i++) set(p, r.nextInt(S), r.nextInt(S), rgb(230, 200, 120));
		if (top) fill(p, 0, 0, S - 1, 2, rgb(150, 110, 60));
		if (bottom) {
			fill(p, 0, S - 4, S - 1, S - 1, rgb(130, 96, 50));
			for (int x = 0; x < S; x += 2) set(p, x, S - 1, rgb(90, 64, 34));
		}
		if (left) fill(p, 0, 0, 1, S - 1, rgb(140, 104, 56));
		if (right) fill(p, S - 2, 0, S - 1, S - 1, rgb(120, 88, 48));
	}

	private static void wall(int[] p, Random r, int x) {
		int base = rgb(206, 176, 120);
		for (int xx = 0; xx < S; xx++) {
			int c = (xx % 4 == 3) ? rgb(150, 116, 70) : shade(base, (xx % 4) * 6 - 6);
			for (int y = 0; y < S; y++) set(p, xx, y, c);
		}
		fill(p, 0, 0, S - 1, 2, rgb(110, 80, 50));
		fill(p, 0, S - 3, S - 1, S - 1, rgb(110, 80, 50));
		if (x % 3 == 1) {
			fill(p, 6, 6, 17, 15, rgb(90, 60, 40));
			fill(p, 8, 8, 15, 13, rgb(250, 214, 120));
			fill(p, 11, 8, 12, 13, rgb(90, 60, 40));
		}
	}

	private static void door(int[] p) {
		fill(p, 5, 3, 18, S - 1, rgb(96, 64, 40));
		fill(p, 7, 5, 16, S - 1, rgb(132, 90, 56));
		fill(p, 11, 5, 12, S - 1, rgb(96, 64, 40));
		set(p, 14, 14, rgb(240, 210, 120));
	}

	private static void porch(int[] p, Random r) {
		int wood = rgb(176, 130, 82);
		for (int y = 0; y < S; y++)
			for (int x = 0; x < S; x++) set(p, x, y, (y % 6 == 5) ? shade(wood, -40) : shade(wood, (x * 7 + y) % 3 * 4));
	}

	private static void bench(int[] p) {
		fill(p, 2, 9, 21, 13, rgb(150, 104, 62));
		fill(p, 2, 9, 21, 9, rgb(190, 140, 90));
		fill(p, 4, 14, 6, 20, rgb(110, 76, 46));
		fill(p, 17, 14, 19, 20, rgb(110, 76, 46));
	}

	private static void coop(int[] p) {
		fill(p, 2, 8, 21, 22, rgb(150, 104, 62));
		for (int x = 3; x < 21; x += 3) fill(p, x, 10, x, 21, rgb(110, 76, 46));
		fill(p, 0, 3, 23, 8, rgb(196, 160, 84));
		fill(p, 0, 7, 23, 8, rgb(130, 96, 50));
		fill(p, 9, 14, 14, 22, rgb(40, 30, 24));
	}

	private static void shrine(int[] p) {
		fill(p, 4, 16, 19, 22, rgb(150, 150, 160));
		fill(p, 4, 16, 19, 16, rgb(200, 200, 210));
		fill(p, 6, 14, 17, 15, rgb(120, 120, 130));
		disc(p, 12, 10, 5, rgb(176, 96, 60));
		fill(p, 10, 3, 14, 6, rgb(176, 96, 60));
		fill(p, 9, 2, 15, 3, rgb(140, 72, 44));
		fill(p, 7, 10, 17, 10, rgb(230, 200, 150));
		set(p, 3, 12, rgb(255, 230, 140)); set(p, 3, 13, rgb(255, 180, 60)); fill(p, 3, 14, 3, 15, rgb(240, 240, 230));
		set(p, 20, 12, rgb(255, 230, 140)); set(p, 20, 13, rgb(255, 180, 60)); fill(p, 20, 14, 20, 15, rgb(240, 240, 230));
	}

	private static void sign(int[] p) {
		fill(p, 11, 12, 12, 22, rgb(110, 76, 46));
		fill(p, 3, 4, 20, 13, rgb(170, 122, 74));
		fill(p, 3, 4, 20, 4, rgb(200, 150, 100));
		fill(p, 3, 13, 20, 13, rgb(110, 76, 46));
		for (int x = 5; x < 19; x += 3) fill(p, x, 7, x + 1, 7, rgb(90, 60, 40));
		for (int x = 6; x < 18; x += 4) fill(p, x, 10, x + 2, 10, rgb(90, 60, 40));
	}

	private static void post(int[] p) {
		fill(p, 7, 2, 16, S - 1, rgb(120, 82, 50));
		fill(p, 7, 2, 8, S - 1, rgb(160, 116, 70));
		fill(p, 5, 0, 18, 3, rgb(90, 60, 36));
	}

	private static void iwall(int[] p, Random r, Tile below) {
		noise(p, rgb(232, 220, 196), r, 30, 6);
		fill(p, 0, 15, S - 1, S - 1, rgb(140, 100, 66));
		fill(p, 0, 15, S - 1, 15, rgb(100, 70, 46));
		for (int x = 3; x < S; x += 6) fill(p, x, 16, x, S - 1, rgb(120, 86, 56));
	}

	private static void wood(int[] p, Random r) {
		int base = rgb(176, 132, 86);
		for (int y = 0; y < S; y++)
			for (int x = 0; x < S; x++) set(p, x, y, (y % 8 == 7) ? shade(base, -36) : shade(base, ((x / 6 + y / 8) % 2) * 8));
		for (int i = 0; i < 6; i++) set(p, r.nextInt(S), r.nextInt(S), shade(base, -20));
	}

	private static void shelf(int[] p, Random r) {
		fill(p, 1, 2, 22, 14, rgb(120, 84, 52));
		fill(p, 2, 8, 21, 8, rgb(90, 62, 38));
		int[] jar = { rgb(120, 200, 180), rgb(230, 170, 80), rgb(170, 120, 210), rgb(200, 90, 80) };
		for (int x = 3; x < 20; x += 5) {
			fill(p, x, 3, x + 3, 7, jar[r.nextInt(4)]);
			fill(p, x + 1, 9, x + 3, 13, jar[r.nextInt(4)]);
		}
	}

	private static void table(int[] p, boolean topEdge, boolean l, boolean rt) {
		fill(p, 0, 4, S - 1, 17, rgb(150, 104, 62));
		fill(p, 0, 4, S - 1, 5, rgb(190, 140, 90));
		fill(p, 0, 18, S - 1, 19, rgb(100, 70, 44));
		if (l) fill(p, 2, 20, 4, S - 1, rgb(100, 70, 44));
		if (rt) fill(p, 19, 20, 21, S - 1, rgb(100, 70, 44));
	}

	private static void templeWall(int[] p, Random r, boolean face) {
		int base = rgb(40, 58, 66);
		noise(p, base, r, 60, 8);
		for (int y = 0; y < S; y += 8) fill(p, 0, y, S - 1, y, shade(base, -18));
		for (int y = 0; y < S; y += 8) fill(p, (y / 8 % 2) * 12, y, (y / 8 % 2) * 12, y + 7, shade(base, -18));
		if (face) {
			fill(p, 0, S - 6, S - 1, S - 1, rgb(30, 44, 50));
			fill(p, 4, S - 4, 6, S - 4, rgb(90, 200, 190));
			fill(p, 10, S - 4, 13, S - 4, rgb(90, 200, 190));
			fill(p, 17, S - 4, 19, S - 4, rgb(90, 200, 190));
		}
	}

	private static void templeFloor(int[] p, Random r) {
		int base = rgb(74, 100, 104);
		noise(p, base, r, 40, 8);
		fill(p, 0, 0, S - 1, 0, shade(base, -20));
		fill(p, 0, 0, 0, S - 1, shade(base, -20));
		fill(p, 12, 0, 12, 11, shade(base, -14));
		fill(p, 0, 12, S - 1, 12, shade(base, -14));
	}

	private static void seafloor(int[] p, Random r) {
		int base = rgb(116, 150, 162);
		noise(p, base, r, 60, 10);
		for (int y = 3; y < S; y += 7)
			for (int x = 0; x < S; x++) if ((x + y) % 6 < 3) set(p, x, y + (x % 3 == 0 ? 1 : 0), shade(base, 18));
	}

	private static void coral(int[] p, Random r) {
		int c = r.nextBoolean() ? rgb(240, 110, 120) : rgb(250, 160, 70);
		int x0 = 12;
		for (int y = 22; y > 6; y--) set(p, x0 + (y % 4 == 0 ? 1 : 0), y, c);
		for (int k = 0; k < 4; k++) {
			int by = 10 + k * 3, dir = k % 2 == 0 ? 1 : -1;
			for (int i = 0; i < 6; i++) set(p, x0 + dir * i, by - i / 2, c);
			disc(p, x0 + dir * 6, by - 3, 1, shade(c, 30));
		}
		disc(p, 6, 19, 3, rgb(160, 90, 190));
		disc(p, 18, 20, 2, rgb(250, 220, 120));
	}

	private static void star(int[] p, Random r) {
		int base = rgb(58, 56, 98);
		noise(p, base, r, 50, 10);
		fill(p, 0, 0, S - 1, 0, shade(base, -20));
		fill(p, 0, 0, 0, S - 1, shade(base, -20));
		for (int i = 0; i < 3; i++) set(p, r.nextInt(S), r.nextInt(S), rgb(200, 210, 255));
		if (r.nextInt(4) == 0) {
			int x = 4 + r.nextInt(16), y = 4 + r.nextInt(16);
			set(p, x, y, rgb(255, 250, 200)); set(p, x - 1, y, rgb(160, 160, 220)); set(p, x + 1, y, rgb(160, 160, 220));
			set(p, x, y - 1, rgb(160, 160, 220)); set(p, x, y + 1, rgb(160, 160, 220));
		}
	}

	private static void pillar(int[] p) {
		fill(p, 6, 2, 17, 23, rgb(120, 118, 160));
		fill(p, 6, 2, 8, 23, rgb(160, 158, 200));
		fill(p, 4, 0, 19, 3, rgb(90, 88, 130));
		disc(p, 12, 0, 3, rgb(250, 240, 180));
	}
}
