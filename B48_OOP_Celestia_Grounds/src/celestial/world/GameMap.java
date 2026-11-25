package celestial.world;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import celestial.Game;
import celestial.data.Species;
import celestial.story.Script;

/** One area: tiles plus warps, triggers, NPC spawns, encounters and lighting. */
public class GameMap {

	public final String id;
	public final String name;      // banner text
	public final Theme theme;
	public final char[][] tiles;
	public final int w, h;

	public final List<Warp> warps = new ArrayList<>();
	public final List<Trigger> triggers = new ArrayList<>();
	public final Map<Long, Function<Game, Script>> interactions = new HashMap<>();
	public final List<Spawn> spawns = new ArrayList<>();
	public final List<Encounter> encounters = new ArrayList<>();
	public Predicate<Game> encountersOn = g -> true;
	public final List<Light> lights = new ArrayList<>();
	public final List<DarkZone> darkZones = new ArrayList<>();
	public ToDoubleFunction<Game> darkness = g -> 0;
	public Color tint;              // e.g. sunset orange
	public Function<Game, Script> onEnter;
	public Function<Game, Script> onStep;     // after every step (distance-based scenes)
	public String music = "town";

	private BufferedImage baked;

	public GameMap(String id, String name, Theme theme, String[] rows) {
		this.id = id;
		this.name = name;
		this.theme = theme;
		this.h = rows.length;
		this.w = rows[0].length();
		tiles = new char[h][w];
		for (int y = 0; y < h; y++) {
			if (rows[y].length() != w) throw new IllegalStateException(id + " row " + y + " has width " + rows[y].length());
			tiles[y] = rows[y].toCharArray();
		}
	}

	// ------------------------------------------------------------------ data types
	public static class Warp {
		public final int x, y;
		public final String map;
		public final int tx, ty;
		public final String dir;
		public Predicate<Game> allowed = g -> true;
		public Function<Game, Script> denied;

		public Warp(int x, int y, String map, int tx, int ty, String dir) {
			this.x = x; this.y = y; this.map = map; this.tx = tx; this.ty = ty; this.dir = dir;
		}
	}

	public static class Trigger {
		public final Rectangle area;
		public final String once;               // story flag set when it fires (null = every time)
		public final Predicate<Game> when;
		public final Function<Game, Script> script;

		public Trigger(Rectangle area, String once, Predicate<Game> when, Function<Game, Script> script) {
			this.area = area; this.once = once; this.when = when; this.script = script;
		}
	}

	public static class Spawn {
		public final Function<Game, Entity> make;
		public final Predicate<Game> when;

		public Spawn(Function<Game, Entity> make, Predicate<Game> when) {
			this.make = make; this.when = when;
		}
	}

	public static class Encounter {
		public final Species species;
		public final int weight, minLevel, maxLevel;

		public Encounter(Species species, int weight, int minLevel, int maxLevel) {
			this.species = species; this.weight = weight; this.minLevel = minLevel; this.maxLevel = maxLevel;
		}
	}

	public static class Light {
		public final double x, y, radius;  // tiles
		public final Color color;
		public Predicate<Game> when = g -> true;
		public boolean flicker;

		public Light(double x, double y, double radius, Color color) {
			this.x = x; this.y = y; this.radius = radius; this.color = color;
		}
	}

	public static class DarkZone {
		public final Rectangle area;
		public final ToDoubleFunction<Game> level;

		public DarkZone(Rectangle area, ToDoubleFunction<Game> level) {
			this.area = area; this.level = level;
		}
	}

	// ------------------------------------------------------------------ queries
	public Tile tile(int x, int y) {
		if (x < 0 || y < 0 || x >= w || y >= h) return Tile.ROCK;
		return Tile.of(tiles[y][x]);
	}

	public void setTile(int x, int y, char c) {
		tiles[y][x] = c;
		baked = null;
	}

	public Warp warpAt(int x, int y) {
		for (Warp wp : warps) if (wp.x == x && wp.y == y) return wp;
		return null;
	}

	public static long key(int x, int y) { return ((long) x << 32) | (y & 0xffffffffL); }

	public void interact(int x, int y, Function<Game, Script> f) { interactions.put(key(x, y), f); }

	public static Rectangle zone(int[] z) { return new Rectangle(z[0], z[1], z[2] - z[0] + 1, z[3] - z[1] + 1); }

	// ------------------------------------------------------------------ drawing
	public BufferedImage baked() {
		if (baked == null) {
			baked = new BufferedImage(w * Game.TILE, h * Game.TILE, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = baked.createGraphics();
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++) {
					Tile t = tile(x, y);
					Tile up = y > 0 ? tile(x, y - 1) : null, down = y < h - 1 ? tile(x, y + 1) : null;
					Tile left = x > 0 ? tile(x - 1, y) : null, right = x < w - 1 ? tile(x + 1, y) : null;
					g.drawImage(TileArt.paint(t, theme, x, y, up, down, left, right), x * Game.TILE, y * Game.TILE, null);
				}
			g.dispose();
		}
		return baked;
	}
}
