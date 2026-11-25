package celestial.world;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Sound;
import celestial.Text;
import celestial.story.Scenes;
import celestial.story.Script;
import celestial.ui.DebugMenu;
import celestial.ui.JournalScreen;
import celestial.ui.PartyScreen;

/** The walking-around screen: map, hero, NPCs, light and darkness, HUD. */
public class Overworld extends Screen {

	public final Map<String, GameMap> maps;
	public GameMap map;
	public final List<Entity> entities = new ArrayList<>();
	public Entity player;
	public Entity follower;

	private final Lighting lighting = new Lighting();
	private final Random rng = new Random();
	private double darkness;
	public double forceDark = -1;        // scenes can force a darkness level
	public Color night = new Color(6, 6, 22);
	private int camX, camY;
	private int holdFrames;
	private boolean turning;
	private int shake;
	private double pulse;
	private String banner;
	private int bannerT;
	private int safeSteps;

	public Overworld(Game game) {
		super(game);
		game.world = this;
		maps = Maps.build(game);
	}

	@Override
	public boolean opaque() { return true; }

	// ------------------------------------------------------------------ map loading
	public void loadMap(String id, int x, int y, String dir) {
		map = maps.get(id);
		entities.clear();
		String sheet = game.story.boy ? "araw" : "buwan";
		if (player == null) player = Entity.person("player", sheet, x, y, dir);
		player.sheet = CharSheet.of(sheet);
		player.place(x, y);
		player.dir = dir;
		player.solid = false;
		entities.add(player);
		for (GameMap.Spawn s : map.spawns) {
			if (s.when.test(game)) {
				Entity e = s.make.apply(game);
				if (e != null) entities.add(e);
			}
		}
		follower = null;
		if (game.story.talaFollowing) {
			follower = Entity.person("tala", "tala", x, y, dir);
			follower.solid = false;
			follower.light = 1.3;
			follower.lightColor = new Color(255, 236, 150);
			int bx = x - Entity.dx(dir), by = y - Entity.dy(dir);
			if (map.tile(bx, by).walkable) follower.place(bx, by);
			entities.add(follower);
		}
		// the last Banga Shrine you passed is where you rest if your team gets too tired
		for (int yy = 0; yy < map.h; yy++)
			for (int xx = 0; xx < map.w; xx++)
				if (map.tile(xx, yy) == Tile.SHRINE && map.tile(xx, yy + 1).walkable) {
					game.story.shrineMap = map.id;
					game.story.shrineX = xx;
					game.story.shrineY = yy + 1;
				}
		darkness = targetDarkness();
		banner(map.name);
		Sound.music(map.music);
		safeSteps = 4;
		if (map.onEnter != null) {
			Script s = map.onEnter.apply(game);
			if (s != null && !s.isEmpty()) game.run(s);
		}
	}

	public Entity find(String id) {
		for (Entity e : entities) if (e.id.equals(id)) return e;
		return null;
	}

	public void spawn(Entity e) {
		remove(e.id);
		entities.add(e);
	}

	public void remove(String id) {
		entities.removeIf(e -> e.id.equals(id));
		if (follower != null && follower.id.equals(id)) follower = null;
	}

	public void startFollower() {
		Entity t = find("tala");
		if (t != null) {
			follower = t;
			t.solid = false;
			t.onTalk = null;
			t.light = 1.3;
			t.lightColor = new Color(255, 236, 150);
		}
	}

	public void banner(String text) {
		banner = text;
		bannerT = 200;
	}

	public void shake(int frames) { shake = frames; }

	public void lightPulse() { pulse = 2.0; }

	// ------------------------------------------------------------------ update
	@Override
	public void update() {
		for (Entity e : new ArrayList<>(entities)) {
			boolean arrived = e.update();
			if (arrived && e == player) onArrive();
		}
		Scenes.tick(game);
		if (bannerT > 0) bannerT--;
		if (shake > 0) shake--;
		if (pulse > 0) pulse = Math.max(0, pulse - 0.02);
		double target = targetDarkness();
		darkness += (target - darkness) * 0.06;

		if (game.inCutscene() || game.top() != this) return;

		if (game.input.pressed(Key.DEBUG)) { game.push(new DebugMenu(game)); return; }
		if (game.story.starter != null && !player.moving()) {
			if (game.input.pressed(Key.PARTY)) { game.push(new PartyScreen(game, false)); return; }
			if (game.input.pressed(Key.JOURNAL) && game.story.hasJournal) { game.push(new JournalScreen(game)); return; }
		}
		if (player.moving()) return;

		if (game.input.pressed(Key.CONFIRM)) {
			interact();
			return;
		}
		String d = heldDir();
		if (d == null) { holdFrames = 0; turning = false; return; }
		if (!d.equals(player.dir)) {
			player.dir = d;          // a quick tap only turns the hero
			holdFrames = 1;
			turning = true;
			return;
		}
		holdFrames++;
		if (turning && holdFrames < 7) return;
		turning = false;
		tryStep(d);
	}

	private String heldDir() {
		if (game.input.held(Key.UP)) return "up";
		if (game.input.held(Key.DOWN)) return "down";
		if (game.input.held(Key.LEFT)) return "left";
		if (game.input.held(Key.RIGHT)) return "right";
		return null;
	}

	public boolean blocked(int x, int y) {
		if (!map.tile(x, y).walkable) return true;
		for (Entity e : entities) if (e != player && e != follower && e.solid && e.visible && e.tx == x && e.ty == y) return true;
		return false;
	}

	private void tryStep(String d) {
		int nx = player.tx + Entity.dx(d), ny = player.ty + Entity.dy(d);
		Script block = Scenes.blockMove(game, map.id, player.tx, player.ty, nx, ny);
		if (block != null) {
			game.run(block);
			return;
		}
		GameMap.Warp w = map.warpAt(nx, ny);
		if (w != null && !map.tile(nx, ny).walkable) {      // doors warp without stepping on them
			useWarp(w);
			return;
		}
		if (nx < 0 || ny < 0 || nx >= map.w || ny >= map.h || blocked(nx, ny)) return;
		int ox = player.tx, oy = player.ty;
		player.stepNow(d);
		if (follower != null) {
			int fdx = ox - follower.tx, fdy = oy - follower.ty;
			if (Math.abs(fdx) + Math.abs(fdy) == 1) follower.queueSteps(fdx > 0 ? "right" : fdx < 0 ? "left" : fdy > 0 ? "down" : "up", 1);
			else if (fdx != 0 || fdy != 0) follower.place(ox, oy);
		}
	}

	private void useWarp(GameMap.Warp w) {
		if (!w.allowed.test(game)) {
			if (w.denied != null) game.run(w.denied.apply(game));
			return;
		}
		Sound.tone(330, 80, 0, 0.2, Sound.Wave.SINE);
		game.run(new Script().teleport(w.map, w.tx, w.ty, w.dir));
	}

	private void onArrive() {
		if (follower != null && follower.ty == player.ty && follower.tx == player.tx) follower.place(follower.tx, follower.ty);
		GameMap.Warp w = map.warpAt(player.tx, player.ty);
		if (w != null) { useWarp(w); return; }
		for (GameMap.Trigger t : map.triggers) {
			if (t.area.contains(player.tx, player.ty) && (t.once == null || !game.story.has(t.once)) && t.when.test(game)) {
				if (t.once != null) game.story.set(t.once);
				game.run(t.script.apply(game));
				return;
			}
		}
		if (map.onStep != null) {
			Script s = map.onStep.apply(game);
			if (s != null && !s.isEmpty()) { game.run(s); return; }
		}
		if (safeSteps > 0) safeSteps--;
		if (map.tile(player.tx, player.ty).encounters() && safeSteps == 0 && !map.encounters.isEmpty()
				&& map.encountersOn.test(game) && rng.nextDouble() < 0.15) {
			GameMap.Encounter enc = pickEncounter();
			if (enc != null) {
				safeSteps = 3;
				int lv = enc.minLevel + rng.nextInt(enc.maxLevel - enc.minLevel + 1);
				game.run(Scenes.wild(game, enc.species, lv));
			}
		}
	}

	private GameMap.Encounter pickEncounter() {
		int total = 0;
		for (GameMap.Encounter e : map.encounters) total += e.weight;
		int roll = rng.nextInt(total);
		for (GameMap.Encounter e : map.encounters) {
			roll -= e.weight;
			if (roll < 0) return e;
		}
		return null;
	}

	private void interact() {
		int fx = player.tx + Entity.dx(player.dir), fy = player.ty + Entity.dy(player.dir);
		for (Entity e : entities) {
			if (e != player && e.visible && e.onTalk != null && e.tx == fx && e.ty == fy) {
				if (e.sheet != null) e.lookAt(player);
				Script s = e.onTalk.apply(game);
				if (s != null) game.run(s);
				return;
			}
		}
		Function<Game, Script> f = map.interactions.get(GameMap.key(fx, fy));
		if (f != null) {
			Script s = f.apply(game);
			if (s != null) game.run(s);
			return;
		}
		Script s = Scenes.tileMessage(game, map.tile(fx, fy));
		if (s != null) game.run(s);
	}

	public void respawnPlayer(String mapId, int x, int y) {
		loadMap(mapId, x, y, "down");
	}

	private double targetDarkness() {
		if (map == null) return 0;
		if (forceDark >= 0) return forceDark;
		double t = map.darkness.applyAsDouble(game);
		if (player != null)
			for (GameMap.DarkZone z : map.darkZones)
				if (z.area.contains(player.tx, player.ty)) t = Math.max(t, z.level.applyAsDouble(game));
		return t;
	}

	// ------------------------------------------------------------------ render
	@Override
	public void render(Graphics2D g) {
		if (map == null) return;
		int mapW = map.w * Game.TILE, mapH = map.h * Game.TILE;
		int cx = (int) player.px + Game.TILE / 2 - Game.W / 2;
		int cy = (int) player.py + Game.TILE / 2 - Game.H / 2;
		camX = mapW <= Game.W ? -(Game.W - mapW) / 2 : Math.max(0, Math.min(mapW - Game.W, cx));
		camY = mapH <= Game.H ? -(Game.H - mapH) / 2 : Math.max(0, Math.min(mapH - Game.H, cy));
		if (shake > 0) {
			camX += rng.nextInt(9) - 4;
			camY += rng.nextInt(9) - 4;
		}
		BufferedImage base = map.baked();
		g.drawImage(base, -camX, -camY, null);
		animateTiles(g);

		List<Entity> sorted = new ArrayList<>(entities);
		sorted.sort(Comparator.comparingDouble(Entity::footY));
		for (Entity e : sorted) if (!e.overhead) e.draw(g, camX, camY, game.tick);
		grassOverlay(g, player);
		if (follower != null) grassOverlay(g, follower);
		for (Entity e : sorted) if (e.overhead) e.draw(g, camX, camY, game.tick);
		if (map.tint != null) {
			g.setColor(map.tint);
			g.fillRect(0, 0, Game.W, Game.H);
		}
		drawLights(g);
		Scenes.overlay(game, g, camX, camY);
		Hud.draw(g, game, map.name);
		drawBanner(g);
	}

	private void animateTiles(Graphics2D g) {
		int x0 = Math.max(0, camX / Game.TILE), y0 = Math.max(0, camY / Game.TILE);
		int x1 = Math.min(map.w - 1, (camX + Game.W) / Game.TILE), y1 = Math.min(map.h - 1, (camY + Game.H) / Game.TILE);
		for (int y = y0; y <= y1; y++)
			for (int x = x0; x <= x1; x++) {
				Tile t = map.tile(x, y);
				int sx = x * Game.TILE - camX, sy = y * Game.TILE - camY;
				if (t == Tile.WATER || t == Tile.SEAGRASS) {
					int ph = (int) ((game.tick / 6 + x * 7 + y * 13) % 48);
					g.setColor(new Color(255, 255, 255, 70));
					g.fillRect(sx + ph % 40, sy + 10 + (y * 17 + x) % 26, 8, 2);
				} else if (t == Tile.LAVA) {
					int a = (int) (40 + 40 * Math.sin((game.tick + x * 11 + y * 7) / 12.0));
					g.setColor(new Color(255, 230, 90, Math.max(0, a)));
					g.fillRect(sx, sy, Game.TILE, Game.TILE);
				}
			}
		if (map.theme == Theme.REEF || map.theme == Theme.TEMPLO) {
			g.setColor(new Color(200, 240, 255, 90));
			for (int i = 0; i < 18; i++) {
				int bx = (int) ((i * 137 + 40) % Game.W);
				int by = (int) (Game.H - ((game.tick * (1 + i % 3) + i * 97) % (Game.H + 40)));
				g.drawOval(bx + (int) (Math.sin((game.tick + i * 30) / 20.0) * 6), by, 6 + i % 4, 6 + i % 4);
			}
		}
	}

	/** Lower body hidden in tall grass. */
	private void grassOverlay(Graphics2D g, Entity e) {
		if (e == null || !e.visible) return;
		Tile t = map.tile(e.tx, e.ty);
		if (!t.encounters() || e.moving()) return;
		int sx = e.tx * Game.TILE - camX, sy = e.ty * Game.TILE - camY;
		BufferedImage base = map.baked();
		g.drawImage(base, sx, sy + 26, sx + Game.TILE, sy + Game.TILE, e.tx * Game.TILE, e.ty * Game.TILE + 26, e.tx * Game.TILE + Game.TILE, e.ty * Game.TILE + Game.TILE, null);
	}

	private void drawLights(Graphics2D g) {
		if (darkness <= 0.01) return;
		lighting.clear();
		double r = game.story.lightRadius();
		boolean lit = r > 0;
		if (!lit) r = 0.7;
		r += pulse;
		Color pc = game.story.heroLight ? new Color(255, 236, 170) : new Color(255, 170, 80);
		double flick = game.story.heroLight ? 0 : Math.sin(game.tick / 7.0) * 0.06;
		lighting.add(player.px - camX + 24, player.py - camY + 20, (r + 0.5 + flick) * Game.TILE, lit ? pc : null, lit ? 1.0 : 0.45);
		for (Entity e : entities) {
			if (e == player || !e.visible || e.light <= 0) continue;
			double lr = e.light;
			if (e == follower && game.story.has("bituin_faint")) lr = 0.8;
			lighting.add(e.px - camX + 24, e.py - camY + 16, lr * Game.TILE, e.lightColor, e == follower && game.story.has("bituin_faint") ? 0.5 : 0.95);
		}
		for (GameMap.Light l : map.lights) {
			if (!l.when.test(game)) continue;
			double lr = l.radius;
			double str = 0.95;
			if (l.flicker) {
				double f = Math.sin(game.tick / 9.0 + l.x * 3 + l.y * 5);
				lr *= 0.8 + 0.2 * f;
				str = 0.7 + 0.25 * f;
			}
			lighting.add(l.x * Game.TILE - camX + 24, l.y * Game.TILE - camY + 24, lr * Game.TILE, l.color, str);
		}
		lighting.render(g, darkness, night);
		// fireflies / lamp sparks drawn on top of the dark
		for (GameMap.Light l : map.lights) {
			if (!l.flicker || !l.when.test(game)) continue;
			int fx = (int) (l.x * Game.TILE - camX + 24 + Math.sin(game.tick / 20.0 + l.y) * 8);
			int fy = (int) (l.y * Game.TILE - camY + 20 + Math.cos(game.tick / 17.0 + l.x) * 6);
			g.setColor(new Color(230, 255, 140, 220));
			g.fillOval(fx - 2, fy - 2, 5, 5);
		}
	}

	private void drawBanner(Graphics2D g) {
		if (bannerT <= 0 || banner == null) return;
		Font f = Assets.font(20f);
		g.setFont(f);
		int w = g.getFontMetrics().stringWidth(Text.safe(banner)) + 60;
		int slide = bannerT > 180 ? (200 - bannerT) * 4 : bannerT < 20 ? bannerT * 4 : 80;
		int y = -80 + slide + 70;
		Text.panel(g, Game.W / 2 - w / 2, y, w, 58, new Color(30, 30, 60, 235), Text.GOLD);
		Text.center(g, banner, Game.W / 2, y + 38, f, new Color(255, 240, 200), false);
	}

	public int camX() { return camX; }

	public int camY() { return camY; }

	public double darknessNow() { return darkness; }

	/** For tests. */
	public void setDarknessNow(double d) { darkness = d; }

}
