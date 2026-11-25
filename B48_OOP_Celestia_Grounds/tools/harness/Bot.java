import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import javax.imageio.ImageIO;

import celestial.*;
import celestial.Input.Key;
import celestial.battle.Battle;
import celestial.data.*;
import celestial.story.Story;
import celestial.ui.*;
import celestial.world.*;

/** Plays Celestial Grounds from the title screen to the credits, headless. */
public class Bot {
	public static Game g;
	public static String out;
	static int starterIdx;
	static boolean boy;
	public static long frame;
	static int shotN;
	static Random rnd = new Random(1);
	static boolean cheatLevels;
	static Set<String> shotsTaken = new HashSet<>();
	static int battles, wins, losses, befriends;
	static String lastMap = "";
	static Battle lastBattle;

	static class Obj {
		String name; Predicate<Game> done; Runnable act; String map;
		Obj(String n, Predicate<Game> d, Runnable a) { name = n; done = d; act = a; }
	}
	static Obj o(String map, String n, Predicate<Game> d, Runnable a) { Obj x = new Obj(n, d, a); x.map = map; return x; }
	static List<Obj> objs = new ArrayList<>();

	static void shot(String name) {
		try {
			BufferedImage im = new BufferedImage(Game.W, Game.H, BufferedImage.TYPE_INT_RGB);
			Graphics2D gr = im.createGraphics(); g.render(gr); gr.dispose();
			ImageIO.write(im, "png", new File(out, String.format("%03d_%s.png", shotN++, name)));
		} catch (Exception e) { throw new RuntimeException(e); }
	}
	static void shotOnce(String key) { if (shotsTaken.add(key)) shot(key); }

	public static void main(String[] a) throws Exception {
		Sound.disable();
		out = a[0]; starterIdx = Integer.parseInt(a[1]); boy = a.length < 3 || a[2].equals("boy");
		cheatLevels = a.length > 3 && a[3].equals("cheat");
		new File(out).mkdirs();
		g = new Game();
		tick(3); shot("title");
		press(Key.CONFIRM); tick(2);
		if (!boy) press(Key.RIGHT);
		press(Key.CONFIRM); tick(2);
		buildObjectives();
		long limit = 260000;
		Screen lastTop = null;
		while (frame < limit) {
			Screen top = g.top();
			if (top instanceof TitleScreen && g.story.has("ending")) { shot("back_to_title"); break; }
			if (top != lastTop) { onNewTop(top); lastTop = top; }
			drive(top);
			tick(1);
		}
		Story s = g.story;
		System.out.println("frames=" + frame + " (" + (frame / 3600) + " min game time)");
		System.out.println("ended=" + s.has("ending") + " hiyas=" + s.hiyas + " tapang=" + s.tapang + " battles=" + battles + " losses=" + losses + " befriended=" + s.befriended);
		for (Creature c : s.party) System.out.println("  party " + c.name() + " L" + c.level + " " + c.hp + "/" + c.maxHp());
		for (Creature c : s.banga) System.out.println("  banga " + c.name() + " L" + c.level);
		if (!s.has("ending")) { System.out.println("STUCK at objective: " + current()); shot("stuck"); }
	}

	public static String current() { for (Obj o : objs) if (!o.done.test(g)) return o.name; return "none"; }

	static void tick(int n) { for (int i = 0; i < n; i++) { g.update(); frame++; } }
	static void press(Key k) { g.input.inject(k); tick(1); }

	static void onNewTop(Screen top) {
		if (lastBattle != null && lastBattle.isClosed()) {
			if (lastBattle.result() == Battle.Result.LOSE) { losses++; System.out.println("LOST vs " + lastBattle.enemy.name() + " at " + current()); }
			lastBattle = null;
		}
		if (top instanceof Battle) {
			Battle b = (Battle) top;
			lastBattle = b;
			if (b.setup.kind != celestial.battle.BattleSetup.Kind.WILD) {
				StringBuilder sb = new StringBuilder("BATTLE " + b.setup.trainer + " L" + b.enemy.level + " vs");
				for (Creature c : g.story.party) sb.append(" ").append(c.name()).append(" L").append(c.level).append("(").append(c.hp).append(")");
				sb.append(" pandesal=").append(g.story.pandesal);
				System.out.println(sb);
			}
			battles++;
			shotOnce("battle_" + b.setup.kind + "_" + b.enemy.name());
		}
		if (top instanceof Popup) shotOnce("popup_" + g.world.map.id + "_" + (shotN));
		if (top instanceof TextBox) {
			String t = ((TextBox) top).text();
			String[] keys = { "The door creaks open", "Choose your partner", "DALI MAGTANGGOL blocks", "TALA trips", "Suddenly every firefly", "fireflies relight",
					"Sunset. A wide clearing", "Best seat in the house", "Doña retreats", "The trail enters a cooled lava", "The paper is soaked",
					"the Banga in the bag begins to glow", "Pito ang buwan", "He glows brighter and brighter", "I remember now", "The sea rises",
					"Seven moons light the sky", "Night outside the player's house", "I'm still afraid sometimes", "Tala joined you", "Hiyas ng Lupa rises" };
			for (String k : keys) if (t.contains(k) && shotsTaken.add(k)) { tick(t.length() / 2 + 8); shot("moment_" + k.replaceAll("[^A-Za-z]", "").substring(0, Math.min(18, k.replaceAll("[^A-Za-z]", "").length()))); }
		}
		if (top instanceof Battle && battles % 1 == 0) { }
		if (top instanceof RhythmScreen) shotOnce("rhythm");
		if (top instanceof CardScreen) { tick(90); shot("card"); }
		if (top instanceof PartyScreen) shot("party_final");
	}

	// ------------------------------------------------------------------ UI driving
	static int lastConfirm;
	public static void drive(Screen top) {
		g.input.releaseAll();
		if (g.world != null && g.world.map != null && !g.world.map.id.equals(lastMap)) {
			lastMap = g.world.map.id;
			tick(40);
			shot("map_" + lastMap + (g.story.has("ending") ? "_end" : ""));
		}
		if (top instanceof TextBox || top instanceof Popup || top instanceof PromptScreen || top instanceof CardScreen) {
			if (frame - lastConfirm > 5) { g.input.inject(Key.CONFIRM); lastConfirm = (int) frame; }
			return;
		}
		if (top instanceof ChoiceBox) {
			ChoiceBox c = (ChoiceBox) top;
			String[] o = c.options();
			if (o[0].startsWith("Tarsi") && o.length == 3) { for (int i = 0; i < starterIdx; i++) press(Key.DOWN); }
			if (o[o.length - 1].equals("Not yet") && o[0].equals("Board") == false && o[0].startsWith("Sakay") == false && !o[0].equals("Yes")) {}
			g.input.inject(Key.CONFIRM);
			return;
		}
		if (top instanceof PartyScreen) {
			arrangeFinalTeam();
			g.input.inject(Key.CANCEL); tick(1); g.input.inject(Key.CANCEL);
			return;
		}
		if (top instanceof RhythmScreen) {
			if (((RhythmScreen) top).nearBeat()) g.input.inject(Key.CONFIRM);
			return;
		}
		if (top instanceof Battle) { battle((Battle) top); return; }
		if (top instanceof Overworld && !g.inCutscene()) walkObjectives();
	}

	static void arrangeFinalTeam() {
		Story s = g.story;
		List<Creature> all = new ArrayList<>(s.party); all.addAll(s.banga);
		List<Creature> team = new ArrayList<>();
		for (Element e : new Element[] { Element.LUPA, Element.APOY, Element.TUBIG }) {
			Creature best = null;
			for (Creature c : all) if (c.element() == e && (best == null || c.level > best.level)) best = c;
			if (best != null) team.add(best);
		}
		for (Creature c : all) if (team.size() < 3 && !team.contains(c)) team.add(c);
		s.party.clear(); s.banga.clear();
		s.party.addAll(team);
		for (Creature c : all) if (!team.contains(c)) s.banga.add(c);
	}

	// ------------------------------------------------------------------ battle AI
	static void battle(Battle b) {
		String mode = b.modeName();
		if (b.qteDirection() != null) {
			g.input.inject(b.qteDirection().equals("left") ? Key.LEFT : Key.RIGHT);
			return;
		}
		Story s = g.story;
		Creature me = b.ally, en = b.enemy;
		switch (mode) {
			case "QUEUE":
				if (frame % 4 == 0) g.input.inject(Key.CONFIRM);
				return;
			case "MENU": {
				int want;
				boolean wild = b.isWild();
				boolean befriendable = wild && en.species != s.starter && !s.befriended.contains(en.species);
				if (me.hpFrac() < 0.35 && s.pandesal > 0) { pandesalTarget = 0; want = 1; }
				else if (befriendable && (en.hpFrac() < 0.5 || en.species == Species.DUGONG) && !en.fed && s.pandesal > 0) { pandesalTarget = 1; want = 1; }
				else if (befriendable && en.fed && (en.hpFrac() < 0.5 || en.species == Species.DUGONG)) want = 2;
				else if (wild && !befriendable) want = 4;
				else if (wild && en.species == Species.DUGONG) { want = s.pandesal > 0 ? 1 : 4; pandesalTarget = 1; }
				else if (shouldSwitch(b) >= 0) want = 3;
				else want = 0;
				if (b.setup.kind == celestial.battle.BattleSetup.Kind.GUARDIAN && en.species == Species.TAMARAW && en.hpFrac() <= 0.25 && s.pandesal > 1) { pandesalTarget = 1; want = 1; }
				navGrid(b.selection(), want);
				return;
			}
			case "MOVES": {
				Move[] ms = me.moves();
				int best = 0; double bestV = -1;
				for (int i = 0; i < ms.length; i++) {
					double v = ms[i].power * ms[i].element.against(en.element());
					if (b.isWild() && en.species != s.starter && !s.befriended.contains(en.species)) v = -v; // be gentle: we want to befriend it
					if (v > bestV) { bestV = v; best = i; }
				}
				navList(b.selection(), best);
				return;
			}
			case "PANDESAL": {
				List<String> o = b.pandesalChoices();
				int want = 0;
				if (pandesalTarget == 1) for (int i = 0; i < o.size(); i++) if (o.get(i).startsWith("Offer")) want = i;
				navList(b.selection(), want);
				return;
			}
			case "SWITCH": case "FORCED_SWITCH": {
				int want = mode.equals("SWITCH") ? shouldSwitch(b) : -1;
				if (want < 0) for (int i = 0; i < s.party.size(); i++) if (!s.party.get(i).fainted() && s.party.get(i) != me) { want = i; break; }
				if (want < 0) want = s.party.size();
				navList(b.selection(), want);
				return;
			}
			default:
		}
	}
	static int pandesalTarget;

	static int shouldSwitch(Battle b) {
		Creature me = b.ally, en = b.enemy;
		double mine = me.species.signature.element.against(en.element());
		Story s = g.story;
		for (int i = 0; i < s.party.size(); i++) {
			Creature c = s.party.get(i);
			if (c == me || c.fainted() || c.hpFrac() < 0.4) continue;
			if (c.species.signature.element.against(en.element()) > mine && mine <= 1) return i;
		}
		return -1;
	}

	static void navGrid(int sel, int want) {
		if (sel == want) { g.input.inject(Key.CONFIRM); return; }
		int sr = sel / 3, sc = sel % 3, wr = want / 3, wc = want % 3;
		if (sr < wr) g.input.inject(Key.DOWN); else if (sr > wr) g.input.inject(Key.UP);
		else if (sc < wc) g.input.inject(Key.RIGHT); else g.input.inject(Key.LEFT);
	}
	static void navList(int sel, int want) {
		if (sel == want) g.input.inject(Key.CONFIRM);
		else g.input.inject(sel < want ? Key.DOWN : Key.UP);
	}

	// ------------------------------------------------------------------ overworld objectives
	static int[] P(String k) { return MapGrids.POINTS.get(k); }
	static boolean on(String map) { return g.world.map.id.equals(map); }
	static boolean flag(String f) { return g.story.has(f); }

	public static void buildObjectives() {
		objs.add(o("BARRIO", "porch: open door", x -> flag("lola_out"), () -> interact("BARRIO", 6, 16, "up")));
		objs.add(o("LAB", "lab: starter", x -> g.story.starter != null, () -> {}));
		objs.add(o("BARRIO", "gate: Dali", x -> flag("dali_gate"), () -> walk("BARRIO", 15, 3)));
		objs.add(o("PALAYAN", "kalabaw", x -> flag("kalabaw_tut"), () -> { lv(4); walk("PALAYAN", 14, 20); }));
		objs.add(o("PALAYAN", "tala & gusmo", x -> flag("tala_joined"), () -> { lv(5); if (flag("tala_met")) interact("PALAYAN", 16, 12, "left"); else walk("PALAYAN", 14, 12); }));
		objs.add(o("KAWAYAN", "kawayan dark", x -> flag("kawayan_dark"), () -> walk("KAWAYAN", 13, 1)));
		objs.add(o("TUKTOK", "tamaraw", x -> flag("hiyas_lupa"), () -> { lv(6); if (flag("tamaraw_talk")) interact("TUKTOK", 14, 5, "up"); else walk("TUKTOK", 14, 7); }));
		objs.add(o("TUKTOK", "jeepney", x -> flag("jeepney"), () -> interact("TUKTOK", 25, 12, "up")));
		objs.add(o("BULKAN", "befriend fire", x -> g.story.befriended.contains(Species.AHAS_BULKAN) || g.story.starter == Species.BINTURO || grassTries > 1500,
				() -> wanderGrass("BULKAN", 4, 18, 7, 19)));
		objs.add(o("BULKAN", "lava puzzle", x -> flag("lava_bridge"), () -> interact("BULKAN", 14, 17, "up")));
		objs.add(o("BULKAN", "tunnel", x -> flag("tunnel"), () -> walk("BULKAN", 15, 10)));
		objs.add(o("CRATER", "dona", x -> flag("dona_beaten"), () -> { lv(8); if (g.world.find("dona") != null && flag("dona_talk")) interact("CRATER", 14, 6, "up"); else walk("CRATER", 14, 6); }));
		objs.add(o("CRATER", "haribon", x -> flag("hiyas_apoy"), () -> interact("CRATER", 14, 6, "up")));
		objs.add(o("DAGAT", "befriend water", x -> g.story.befriended.contains(Species.PUGITA) || grassTries2 > 1500,
				() -> wanderGrass2("DAGAT", 14, 7, 19, 9)));
		objs.add(o("TEMPLO", "temple chamber", x -> flag("babaylan"), () -> walk("TEMPLO", 13, 3)));
		objs.add(o("REEF", "butanding", x -> flag("hiyas_tubig"), () -> { lv(10); if (flag("butanding_talk")) interact("REEF", 14, 8, "up"); else walk("REEF", 13, 11); }));
		objs.add(o("CELESTIA", "bakunawa", x -> flag("bakunawa_calmed"), () -> { lv(12); if (g.story.has("bituin_evolved")) interact("CELESTIA", 11, 7, "up"); else walk("CELESTIA", 11, 10); }));
	}

	static int grassTries, grassTries2;
	static void wanderGrass(String map, int x0, int y0, int x1, int y1) { grassTries++; wander(map, x0, y0, x1, y1); }
	static void wanderGrass2(String map, int x0, int y0, int x1, int y1) { grassTries2++; wander(map, x0, y0, x1, y1); }
	static void wander(String map, int x0, int y0, int x1, int y1) {
		Entity p = g.world.player;
		int tx = (p.tx == x0 ? x1 : x0), ty = p.ty < y0 || p.ty > y1 ? y0 : p.ty;
		walk(map, tx, ty);
	}

	static void lv(int L) {
		if (!cheatLevels) return;
		for (Creature c : g.story.party) if (c.level < L) { c.level = L; c.fullHeal(); }
	}

	static boolean healing;
	static void walkObjectives() {
		for (Obj o : objs) {
			if (!o.done.test(g)) {
				if (o.map != null && !on(o.map)) { healing = false; goToMap(o.map); return; }
				// like a sensible player: rest at the shrine before a story battle
				boolean hurt = false;
				for (Creature c : g.story.party) if (c.hpFrac() < 0.7) hurt = true;
				if (g.story.starter != null && g.story.has("tala_joined") && (hurt || g.story.pandesal < 2) && shrineHere() != null && !healing) healing = true;
				if (healing) {
					int[] sh = shrineHere();
					if (sh == null) { healing = false; } else {
						Entity p = g.world.player;
						if (p.tx == sh[0] && p.ty == sh[1] + 1 && p.dir.equals("up") && !hurt && g.story.pandesal >= 2) { healing = false; }
						else { interact(g.world.map.id, sh[0], sh[1] + 1, "up"); return; }
					}
				}
				o.act.run();
				return;
			}
		}
	}
	static int[] shrineHere() {
		GameMap m = g.world.map;
		for (int y = 0; y < m.h; y++) for (int x = 0; x < m.w; x++) if (m.tile(x, y) == Tile.SHRINE && m.tile(x, y + 1).walkable) return new int[] { x, y };
		return null;
	}

	// map graph: warps plus the jeepney and ferry rides
	static String[][] RIDES = { { "TUKTOK", "25", "12", "up", "BULKAN" }, { "BULKAN", "5", "25", "left", "TUKTOK" },
			{ "CRATER", "25", "8", "right", "DAGAT" }, { "DAGAT", "2", "12", "left", "CRATER" } };

	static void goToMap(String target) {
		Map<String, GameMap> maps = g.world.maps;
		String start = g.world.map.id;
		Map<String, String[]> firstHop = new HashMap<>();
		ArrayDeque<String> q = new ArrayDeque<>();
		q.add(start);
		Set<String> seen = new HashSet<>(); seen.add(start);
		while (!q.isEmpty()) {
			String m = q.poll();
			List<String[]> edges = new ArrayList<>();
			for (GameMap.Warp w : maps.get(m).warps) if (w.allowed.test(g)) edges.add(new String[] { m, "" + w.x, "" + w.y, "warp", w.map });
			for (String[] r : RIDES) if (r[0].equals(m)) edges.add(r);
			for (String[] e : edges) {
				if (seen.contains(e[4])) continue;
				seen.add(e[4]);
				firstHop.put(e[4], m.equals(start) ? e : firstHop.get(m));
				q.add(e[4]);
			}
		}
		String[] hop = firstHop.get(target);
		if (hop == null) return;
		int x = Integer.parseInt(hop[1]), y = Integer.parseInt(hop[2]);
		if (hop[3].equals("warp")) {
			GameMap m = g.world.map;
			if (m.tile(x, y).walkable) walk(start, x, y);
			else {
				for (String d : new String[] { "down", "up", "left", "right" }) {
					int ax = x - Entity.dx(d), ay = y - Entity.dy(d);
					if (m.tile(ax, ay).walkable) { door(start, ax, ay, d); return; }
				}
			}
		} else {
			interact(start, x, y, hop[3]);
		}
	}

	static void walk(String map, int tx, int ty) {
		if (!on(map)) return;
		Entity p = g.world.player;
		if (p.moving()) return;
		if (p.tx == tx && p.ty == ty) return;
		String d = path(tx, ty);
		if (d != null) g.input.injectHold(dirKey(d), true);
	}

	static void interact(String map, int sx, int sy, String face) {
		if (!on(map)) return;
		Entity p = g.world.player;
		if (p.moving()) return;
		if (p.tx != sx || p.ty != sy) { walk(map, sx, sy); return; }
		if (!p.dir.equals(face)) { g.input.injectHold(dirKey(face), true); return; }
		if (frame - lastConfirm > 10) { g.input.inject(Key.CONFIRM); lastConfirm = (int) frame; }
	}

	static void door(String map, int sx, int sy, String dir) {
		if (!on(map)) return;
		Entity p = g.world.player;
		if (p.moving()) return;
		if (p.tx != sx || p.ty != sy) { walk(map, sx, sy); return; }
		g.input.injectHold(dirKey(dir), true);
	}

	static Key dirKey(String d) {
		switch (d) { case "up": return Key.UP; case "down": return Key.DOWN; case "left": return Key.LEFT; default: return Key.RIGHT; }
	}

	/** BFS from the player to the target; returns the first direction. */
	static String path(int tx, int ty) {
		Overworld w = g.world;
		GameMap m = w.map;
		Entity p = w.player;
		int[][] prev = new int[m.h * m.w][];
		boolean[] seen = new boolean[m.h * m.w];
		ArrayDeque<int[]> q = new ArrayDeque<>();
		q.add(new int[] { p.tx, p.ty });
		seen[p.ty * m.w + p.tx] = true;
		int[][] dirs = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };
		while (!q.isEmpty()) {
			int[] c = q.poll();
			if (c[0] == tx && c[1] == ty) {
				int[] cur = c;
				while (prev[cur[1] * m.w + cur[0]] != null && !(prev[cur[1] * m.w + cur[0]][0] == p.tx && prev[cur[1] * m.w + cur[0]][1] == p.ty)) cur = prev[cur[1] * m.w + cur[0]];
				int dx = cur[0] - p.tx, dy = cur[1] - p.ty;
				return dx > 0 ? "right" : dx < 0 ? "left" : dy > 0 ? "down" : "up";
			}
			for (int[] d : dirs) {
				int nx = c[0] + d[0], ny = c[1] + d[1];
				if (nx < 0 || ny < 0 || nx >= m.w || ny >= m.h || seen[ny * m.w + nx]) continue;
				boolean ok = !w.blocked(nx, ny) || (nx == tx && ny == ty && m.tile(nx, ny).walkable);
				if (!ok) continue;
				// avoid stepping on warps that aren't the goal
				if (m.warpAt(nx, ny) != null && !(nx == tx && ny == ty)) continue;
				seen[ny * m.w + nx] = true;
				prev[ny * m.w + nx] = c;
				q.add(new int[] { nx, ny });
			}
		}
		return null;
	}
}
