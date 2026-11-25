package celestial.world;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import celestial.Assets;
import celestial.Game;
import celestial.data.Element;
import celestial.data.Species;
import celestial.story.Scenes;
import celestial.world.GameMap.DarkZone;
import celestial.world.GameMap.Encounter;
import celestial.world.GameMap.Light;
import celestial.world.GameMap.Spawn;
import celestial.world.GameMap.Trigger;
import celestial.world.GameMap.Warp;

/** Builds every area of Celestial Grounds and connects it to the scenes. */
public final class Maps {

	private Maps() {}

	public static int[][] kawayanDist, temploDist;
	public static int kawayanLen, temploLen;

	public static int[] P(String key) {
		int[] p = MapGrids.POINTS.get(key);
		if (p == null) throw new IllegalArgumentException("no point " + key);
		return p;
	}

	static Rectangle Z(String key) {
		return GameMap.zone(MapGrids.ZONES.get(key));
	}

	private static Warp warp(GameMap m, int x, int y, String map, int tx, int ty, String dir) {
		Warp w = new Warp(x, y, map, tx, ty, dir);
		m.warps.add(w);
		return w;
	}

	/** BFS distance from a tile to every walkable tile. */
	public static int[][] dist(GameMap m, int tx, int ty) {
		int[][] d = new int[m.h][m.w];
		for (int[] row : d) Arrays.fill(row, -1);
		ArrayDeque<int[]> q = new ArrayDeque<>();
		d[ty][tx] = 0;
		q.add(new int[] { tx, ty });
		while (!q.isEmpty()) {
			int[] c = q.poll();
			for (int[] dd : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
				int nx = c[0] + dd[0], ny = c[1] + dd[1];
				if (nx < 0 || ny < 0 || nx >= m.w || ny >= m.h || d[ny][nx] >= 0) continue;
				if (!m.tile(nx, ny).walkable) continue;
				d[ny][nx] = d[c[1]][c[0]] + 1;
				q.add(new int[] { nx, ny });
			}
		}
		return d;
	}

	/** Next tile toward the target of a distance field. */
	public static int[] stepToward(int[][] d, int x, int y) {
		int best = d[y][x];
		int[] out = null;
		for (int[] dd : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
			int nx = x + dd[0], ny = y + dd[1];
			if (ny < 0 || nx < 0 || ny >= d.length || nx >= d[0].length) continue;
			if (d[ny][nx] >= 0 && d[ny][nx] < best) {
				best = d[ny][nx];
				out = new int[] { nx, ny };
			}
		}
		return out;
	}

	private static final Color WARM = new Color(255, 190, 110);

	public static Map<String, GameMap> build(Game game) {
		Map<String, GameMap> maps = new HashMap<>();

		// ================================================================ BARRIO TULOG
		GameMap b = new GameMap("BARRIO", "Barrio Tulog", Theme.BARRIO, MapGrids.BARRIO);
		b.darkness = g -> g.story.has("ending") ? 0.74 : g.story.starter == null ? 0.82 : !g.story.has("dali_gate") ? 0.32 : 0;
		b.lights.add(new Light(5.5, 15.4, 2.3, WARM));
		b.lights.add(new Light(24, 6, 1.6, WARM));
		b.lights.add(new Light(4, 6, 1.3, WARM));
		b.lights.add(new Light(23, 15, 1.3, WARM));
		b.lights.add(new Light(10, 11, 1.6, new Color(255, 230, 160)));
		Light porch2 = new Light(5.5, 16.5, 3.2, WARM);
		porch2.when = g -> g.story.has("ending");
		b.lights.add(porch2);
		for (int i = 0; i < 6; i++) {
			Light lan = new Light(8 + i * 3.5, 9 + (i % 2) * 6, 1.8, new Color(255, 200, 120));
			lan.when = g -> g.story.has("ending");
			lan.flicker = true;
			b.lights.add(lan);
		}
		for (int x = 14; x <= 17; x++) {
			Warp w = warp(b, x, 0, "PALAYAN", x - 1, 27, "up");
			w.allowed = g -> g.story.has("dali_gate");
		}
		Warp lab = warp(b, P("BARRIO.lab_door")[0], P("BARRIO.lab_door")[1], "LAB", P("LAB.entry")[0], P("LAB.entry")[1], "up");
		lab.allowed = g -> g.story.has("off_porch");
		b.spawns.add(new Spawn(g -> Entity.person("lola", "lola", P("BARRIO.door_home")[0], P("BARRIO.door_home")[1], "down").talk(Scenes::talkLola),
				g -> g.story.has("lola_out") && !g.story.has("ending")));
		b.spawns.add(new Spawn(g -> Entity.person("villager", "villager", P("BARRIO.villager")[0], P("BARRIO.villager")[1], "right").talk(Scenes::talkVillager),
				g -> !g.story.has("ending")));
		b.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("goat", Assets.cg("obj_goat.png"), 64, 64, P("BARRIO.goat")[0], P("BARRIO.goat")[1]);
			e.talk(gg -> new celestial.story.Script().narrate("Meeeh. The goat looks very pleased with itself."));
			return e;
		}, g -> !g.story.has("ending")));
		b.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("rooster", Assets.cg("obj_rooster.png"), 44, 44, P("BARRIO.coop")[0], P("BARRIO.coop")[1]);
			e.solid = false;
			e.drawOffsetY = -14;
			e.noShadow = true;
			return e;
		}, g -> true));
		b.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("arch", arch(), 6 * Game.TILE, 2 * Game.TILE, 15, 1);
			e.overhead = true;
			e.solid = false;
			e.noShadow = true;
			e.drawOffsetX = Game.TILE / 2;
			e.drawOffsetY = -Game.TILE / 2;
			return e;
		}, g -> true));
		b.interact(P("BARRIO.bench")[0], P("BARRIO.bench")[1], Scenes::lookMoon);
		b.interact(P("BARRIO.coop")[0], P("BARRIO.coop")[1], Scenes::lookCoop);
		b.interact(P("BARRIO.door_home")[0], P("BARRIO.door_home")[1], Scenes::homeDoor);
		b.interact(P("BARRIO.sign_lab")[0], P("BARRIO.sign_lab")[1], g -> Scenes.sign("Laboratoryo ni Propesor Bantay. Wala pong signal."));
		b.interact(P("BARRIO.sign_barrio")[0], P("BARRIO.sign_barrio")[1], g -> Scenes.sign("BARRIO TULOG. Home of Lola's famous pandesal. Mag-ingat sa kambing (beware of the goat)."));
		b.interact(P("BARRIO.house_nw_door")[0], P("BARRIO.house_nw_door")[1], Scenes::sleepyHouse);
		b.interact(P("BARRIO.house_se_door")[0], P("BARRIO.house_se_door")[1], Scenes::sleepyHouse);
		b.triggers.add(new Trigger(new Rectangle(14, 3, 4, 1), "dali_gate", g -> g.story.starter != null, Scenes::scene3));
		b.triggers.add(new Trigger(new Rectangle(11, 7, 8, 3), "villager_warned", g -> g.story.starter == null && g.story.has("off_porch"), Scenes::villagerWarns));
		b.onEnter = Scenes::enterBarrio;
		maps.put(b.id, b);

		// ================================================================ LAB
		GameMap lb = new GameMap("LAB", "Propesor Bantay's Lab", Theme.LAB, MapGrids.LAB);
		warp(lb, P("LAB.door")[0], P("LAB.door")[1], "BARRIO", P("BARRIO.lab_out")[0], P("BARRIO.lab_out")[1], "down");
		lb.spawns.add(new Spawn(g -> Entity.person("propesor", "propesor", P("LAB.propesor")[0], P("LAB.propesor")[1], "down").talk(Scenes::talkPropesor), g -> true));
		String[] tubs = { "tub_tarsi", "tub_binturo", "tub_dugong" };
		Species[] starters = { Species.TARSI, Species.BINTURO, Species.DUGONG };
		for (int i = 0; i < 3; i++) {
			final Species sp = starters[i];
			final int[] pt = P("LAB." + tubs[i]);
			lb.spawns.add(new Spawn(g -> {
				Entity e = Entity.still("table_" + sp.sprite, sp.image(), 56, 56, pt[0], pt[1]);
				e.solid = false;
				e.noShadow = true;
				e.drawOffsetY = -10;
				e.talk(gg -> Scenes.tableCreature(gg, sp));
				return e;
			}, g -> g.story.starter != sp));
		}
		lb.onEnter = Scenes::enterLab;
		maps.put(lb.id, lb);

		// ================================================================ MAP 1 EARTH: PALAYAN TRAIL
		GameMap p = new GameMap("PALAYAN", "Map 1: Kapatagan ng Alingawngaw - Palayan Trail", Theme.PALAYAN, MapGrids.PALAYAN);
		for (int x = 13; x <= 16; x++) warp(p, x, 29, "BARRIO", x + 1, 1, "down");
		for (int x = 13; x <= 16; x++) warp(p, x, 0, "KAWAYAN", P("KAWAYAN.entry")[0], P("KAWAYAN.entry")[1], "up");
		p.encounters.add(new Encounter(Species.KALABAW, 100, 2, 3));
		p.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("kalabaw", Species.KALABAW.image(), 80, 80, P("PALAYAN.kalabaw")[0], P("PALAYAN.kalabaw")[1]);
			e.talk(Scenes::scene4);
			return e;
		}, g -> !g.story.has("kalabaw_tut")));
		p.spawns.add(new Spawn(g -> Entity.person("gusmo", "gusmo", 15, 12, "left").talk(Scenes::rematchGusmo),
				g -> g.story.has("tala_met") && !g.story.has("gusmo_beaten")));
		p.spawns.add(new Spawn(g -> Entity.person("tala", "tala", 13, 12, "right"),
				g -> g.story.has("tala_met") && !g.story.has("tala_joined")));
		p.interact(P("PALAYAN.sign")[0], P("PALAYAN.sign")[1], g -> Scenes.sign("Please do not startle the carabao. The carabao has feelings."));
		p.triggers.add(new Trigger(Z("PALAYAN.kalabaw_trigger"), null, g -> !g.story.has("kalabaw_tut") && !g.story.has("kalabaw_busy"), Scenes::scene4));
		p.triggers.add(new Trigger(Z("PALAYAN.tala_trigger"), "tala_met", g -> g.story.has("kalabaw_tut"), Scenes::scene5));
		p.onEnter = Scenes::enterPalayan;
		maps.put(p.id, p);

		// ================================================================ MAP 1 EARTH: KAWAYAN MAZE
		GameMap k = new GameMap("KAWAYAN", "Map 1: Kawayan Maze", Theme.KAWAYAN, MapGrids.KAWAYAN);
		int[] ke = P("KAWAYAN.entry"), kx = P("KAWAYAN.exit");
		warp(k, ke[0], ke[1] + 1, "PALAYAN", 14, 2, "down");
		warp(k, kx[0], kx[1] - 1, "TUKTOK", P("TUKTOK.entry")[0], P("TUKTOK.entry")[1], "up");
		kawayanDist = dist(k, kx[0], kx[1]);
		kawayanLen = kawayanDist[ke[1]][ke[0]];
		k.darkness = g -> 0.72;
		k.encounters.add(new Encounter(Species.KALABAW, 85, 3, 4));
		k.encounters.add(new Encounter(Species.TARSI, 15, 3, 4));
		// fireflies mark the hidden path
		int[] cur = { ke[0], ke[1] };
		int step = 0;
		while (cur != null) {
			if (step % 3 == 1) {
				Light f = new Light(cur[0], cur[1], 1.1, new Color(200, 255, 120));
				f.flicker = true;
				f.when = g -> !g.story.has("fireflies_out");
				k.lights.add(f);
			}
			cur = stepToward(kawayanDist, cur[0], cur[1]);
			step++;
		}
		k.onStep = Scenes::kawayanStep;
		k.onEnter = Scenes::enterKawayan;
		maps.put(k.id, k);

		// ================================================================ MAP 1 EARTH: TUKTOK NG PALAYAN
		GameMap t = new GameMap("TUKTOK", "Map 1: Tuktok ng Palayan", Theme.TUKTOK, MapGrids.TUKTOK);
		t.tint = new Color(255, 120, 40, 34);
		warp(t, 13, 17, "KAWAYAN", kx[0], kx[1], "down");
		warp(t, 14, 17, "KAWAYAN", kx[0], kx[1], "down");
		t.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("tamaraw", Species.TAMARAW.image(), 112, 112, P("TUKTOK.tamaraw")[0], P("TUKTOK.tamaraw")[1]);
			e.talk(Scenes::rematchTamaraw);
			return e;
		}, g -> !g.story.has("hiyas_lupa")));
		t.spawns.add(new Spawn(g -> jeepney(P("TUKTOK.jeepney")[0] + 1, P("TUKTOK.jeepney")[1] + 1), g -> true));
		t.spawns.add(new Spawn(g -> Entity.person("conductor", "conductor", P("TUKTOK.conductor")[0], P("TUKTOK.conductor")[1], "left").talk(Scenes::scene9), g -> true));
		for (int x = 26; x <= 28; x++) for (int y = 9; y <= 10; y++) t.interact(x, y, Scenes::scene9);
		t.triggers.add(new Trigger(Z("TUKTOK.tamaraw_trigger"), "tamaraw_talk", g -> !g.story.has("hiyas_lupa"), Scenes::scene8));
		maps.put(t.id, t);

		// ================================================================ MAP 1 FIRE: VOLCANO TRAIL
		GameMap v = new GameMap("BULKAN", "Map 1: Bundok Alab - Volcano Trail", Theme.BULKAN, MapGrids.BULKAN);
		v.darkness = g -> 0.12;
		v.darkZones.add(new DarkZone(Z("BULKAN.dark_tunnel"), g -> g.story.party.stream().anyMatch(c -> c.element() == Element.APOY) ? 0.72 : 0.93));
		warp(v, 24, 0, "CRATER", P("CRATER.entry")[0], P("CRATER.entry")[1], "up");
		warp(v, 25, 0, "CRATER", P("CRATER.entry")[0] + 1, P("CRATER.entry")[1], "up");
		v.encounters.add(new Encounter(Species.AHAS_BULKAN, 85, 5, 6));
		v.encounters.add(new Encounter(Species.BINTURO, 15, 5, 6));
		v.spawns.add(new Spawn(g -> jeepney(P("BULKAN.jeepney")[0] + 1, P("BULKAN.jeepney")[1] + 1), g -> true));
		for (int x = 2; x <= 4; x++) for (int y = 24; y <= 25; y++) v.interact(x, y, Scenes::jeepneyBack);
		for (int x = 2; x <= 29; x++) {
			v.interact(x, 16, Scenes::lavaPuzzle);
			v.interact(x, 15, Scenes::lavaPuzzle);
		}
		// tunnel walls glow when a Fire creature is in the team
		int[][] tunnel = { { 15, 8 }, { 12, 7 }, { 8, 6 }, { 6, 5 }, { 4, 3 }, { 8, 2 }, { 12, 2 }, { 13, 4 }, { 17, 4 }, { 21, 3 }, { 23, 2 }, { 24, 1 } };
		for (int[] tp : tunnel) {
			Light l = new Light(tp[0], tp[1], 1.6, new Color(255, 120, 50));
			l.when = g -> g.story.party.stream().anyMatch(c -> c.element() == Element.APOY);
			v.lights.add(l);
		}
		v.triggers.add(new Trigger(Z("BULKAN.lava_note"), "lava_note", g -> !g.story.has("lava_bridge"), Scenes::lavaNote));
		v.triggers.add(new Trigger(Z("BULKAN.tunnel_trigger"), "tunnel", g -> true, Scenes::tunnel));
		v.onEnter = Scenes::enterBulkan;
		maps.put(v.id, v);

		// ================================================================ MAP 1 FIRE: CRATER
		GameMap c = new GameMap("CRATER", "Map 1: Bundok Alab - Crater", Theme.CRATER, MapGrids.CRATER);
		c.darkness = g -> 0.1;
		warp(c, 14, 17, "BULKAN", 24, 1, "down");
		warp(c, 15, 17, "BULKAN", 25, 1, "down");
		c.encounters.add(new Encounter(Species.AHAS_BULKAN, 85, 6, 7));
		c.encounters.add(new Encounter(Species.BINTURO, 15, 6, 7));
		c.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("cage", cage(g.story.has("dona_beaten")), 4 * Game.TILE, 4 * Game.TILE, 13, 4);
			e.drawOffsetX = Game.TILE / 2;
			e.solid = false;
			e.noShadow = true;
			return e;
		}, g -> true));
		c.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("haribon_caged", Species.HARIBON.image(), 96, 96, 13, 3);
			e.drawOffsetX = Game.TILE / 2;
			e.solid = false;
			return e;
		}, g -> !g.story.has("dona_beaten")));
		c.spawns.add(new Spawn(g -> Entity.person("dona", "dona", P("CRATER.dona")[0], P("CRATER.dona")[1], "down").talk(Scenes::rematchDona),
				g -> !g.story.has("dona_beaten")));
		c.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("haribon", Species.HARIBON.image(), 112, 112, P("CRATER.haribon_land")[0], P("CRATER.haribon_land")[1]);
			e.bob = true;
			e.talk(Scenes::rematchHaribon);
			return e;
		}, g -> g.story.has("dona_beaten") && !g.story.has("hiyas_apoy")));
		c.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("ferry", Assets.cg("obj_ferry.png"), 144, 72, 28, 7);
			e.solid = false;
			e.noShadow = true;
			return e;
		}, g -> g.story.has("hiyas_apoy")));
		c.spawns.add(new Spawn(g -> Entity.person("captain", "captain", P("CRATER.captain")[0], P("CRATER.captain")[1], "left").talk(Scenes::scene13),
				g -> g.story.has("hiyas_apoy")));
		c.triggers.add(new Trigger(Z("CRATER.dona_trigger"), "dona_talk", g -> !g.story.has("dona_beaten"), Scenes::scene11));
		maps.put(c.id, c);

		// ================================================================ MAP 2: DAGAT TALA
		GameMap d = new GameMap("DAGAT", "Map 2: Dagat Tala - Ferry and Reef", Theme.DAGAT, MapGrids.DAGAT);
		for (int x = 29; x <= 30; x++) warp(d, x, 2, "TEMPLO", P("TEMPLO.entry")[0], P("TEMPLO.entry")[1], "up");
		d.encounters.add(new Encounter(Species.PUGITA, 85, 7, 8));
		d.encounters.add(new Encounter(Species.DUGONG, 15, 7, 8));
		d.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("ferry", Assets.cg("obj_ferry.png"), 144, 72, 0, 12);
			e.solid = false;
			e.noShadow = true;
			e.drawOffsetX = -60;
			return e;
		}, g -> true));
		d.spawns.add(new Spawn(g -> Entity.person("captain", "captain", P("DAGAT.captain")[0], P("DAGAT.captain")[1], "right").talk(Scenes::ferryBack), g -> true));
		d.onEnter = Scenes::enterDagat;
		maps.put(d.id, d);

		// ================================================================ MAP 2: SUNKEN TEMPLE
		GameMap tm = new GameMap("TEMPLO", "Map 2: The Sunken Temple", Theme.TEMPLO, MapGrids.TEMPLO);
		int[] te = P("TEMPLO.entry"), tc = P("TEMPLO.chamber");
		warp(tm, te[0], te[1] + 1, "DAGAT", P("DAGAT.from_temple")[0], P("DAGAT.from_temple")[1], "down");
		Warp reef = warp(tm, P("TEMPLO.reef_door")[0], P("TEMPLO.reef_door")[1], "REEF", P("REEF.entry")[0], P("REEF.entry")[1], "up");
		reef.allowed = g -> g.story.has("babaylan");
		temploDist = dist(tm, tc[0], tc[1]);
		temploLen = temploDist[te[1]][te[0]];
		tm.darkness = g -> 0.97;
		Light statue = new Light(13, 1.5, 3.2, new Color(140, 220, 255));
		statue.when = g -> g.story.has("babaylan");
		tm.lights.add(statue);
		tm.spawns.add(new Spawn(g -> {
			Entity e = Entity.person("statue", "babaylan", P("TEMPLO.statue")[0], P("TEMPLO.statue")[1], "down");
			e.talk(Scenes::talkStatue);
			return e;
		}, g -> true));
		tm.triggers.add(new Trigger(Z("TEMPLO.chamber_trigger"), "babaylan", g -> true, Scenes::babaylan));
		tm.onStep = Scenes::templeStep;
		tm.onEnter = Scenes::enterTemple;
		maps.put(tm.id, tm);

		// ================================================================ MAP 2: BUTANDING'S REEF
		GameMap r = new GameMap("REEF", "Map 2: Butanding's Reef", Theme.REEF, MapGrids.REEF);
		r.darkness = g -> 0.3;
		warp(r, 13, 19, "TEMPLO", P("TEMPLO.reef_in")[0], P("TEMPLO.reef_in")[1], "down");
		for (int x = 13; x <= 14; x++) {
			Warp up = warp(r, x, 0, "CELESTIA", P("CELESTIA.entry")[0], P("CELESTIA.entry")[1], "up");
			up.allowed = g -> g.story.has("hiyas_tubig");
			up.denied = g -> new celestial.story.Script().say("TALA", "The current is too strong up there. Let's talk to the Butanding first.");
		}
		r.encounters.add(new Encounter(Species.PUGITA, 85, 8, 9));
		r.encounters.add(new Encounter(Species.DUGONG, 15, 8, 9));
		for (int[] lp : new int[][] { { 4, 4 }, { 22, 9 }, { 6, 10 }, { 20, 15 }, { 10, 16 } }) r.lights.add(new Light(lp[0], lp[1], 1.4, new Color(120, 240, 230)));
		r.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("butanding", Species.BUTANDING.image(), 176, 176, P("REEF.butanding")[0], P("REEF.butanding")[1]);
			e.bob = true;
			e.talk(Scenes::rematchButanding);
			return e;
		}, g -> !g.story.has("hiyas_tubig")));
		r.triggers.add(new Trigger(Z("REEF.butanding_trigger"), "butanding_talk", g -> !g.story.has("hiyas_tubig"), Scenes::scene15));
		maps.put(r.id, r);

		// ================================================================ MAP 2 END: TUKTOK NG CELESTIA
		GameMap ce = new GameMap("CELESTIA", "Final Arena: Tuktok ng Celestia", Theme.CELESTIA, MapGrids.CELESTIA);
		ce.darkness = g -> 0.45;
		for (int x = 11; x <= 12; x++) warp(ce, x, 19, "REEF", 13, 1, "down");
		for (int[] lp : new int[][] { { 5, 4 }, { 18, 4 }, { 5, 12 }, { 18, 12 }, { 8, 7 }, { 15, 7 } }) ce.lights.add(new Light(lp[0], lp[1] - 0.6, 2.0, new Color(255, 240, 190)));
		ce.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("bakunawa", Assets.cg("mon_bakunawa.png"), 200, 200, P("CELESTIA.altar")[0], P("CELESTIA.altar")[1] - 3);
			e.bob = true;
			e.solid = true;
			e.drawOffsetX = Game.TILE / 2;
			e.talk(Scenes::rematchBakunawa);
			return e;
		}, g -> g.story.has("bituin_evolved") && !g.story.has("bakunawa_calmed")));
		ce.spawns.add(new Spawn(g -> {
			Entity e = Entity.still("bituin_form", g.story.bituinForm().image(), 96, 96, P("CELESTIA.altar")[0] + 3, P("CELESTIA.altar")[1] - 1);
			e.bob = true;
			e.light = 2.5;
			e.lightColor = new Color(255, 240, 180);
			e.talk(gg -> new celestial.story.Script().say(gg.story.bituinForm().label.toUpperCase(), "We face it together. Bring one friend of each element."));
			return e;
		}, g -> g.story.has("bituin_evolved") && !g.story.has("bakunawa_calmed")));
		ce.triggers.add(new Trigger(Z("CELESTIA.altar_trigger"), "bituin_evolved", g -> g.story.has("hiyas_tubig"), Scenes::scene16));
		ce.onEnter = Scenes::enterCelestia;
		maps.put(ce.id, ce);

		return maps;
	}

	// ------------------------------------------------------------------ props
	private static Entity jeepney(int ax, int ay) {
		Entity e = Entity.still("jeepney", Assets.cg("obj_jeepney.png"), 3 * Game.TILE, 84, ax, ay);
		e.solid = false;
		return e;
	}

	/** Wooden arch over the barrio gate. */
	private static BufferedImage arch() {
		BufferedImage im = new BufferedImage(6 * Game.TILE, 2 * Game.TILE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = im.createGraphics();
		g.setColor(new Color(110, 74, 44));
		g.fillRect(8, 30, im.getWidth() - 16, 30);
		g.setColor(new Color(150, 106, 64));
		g.fillRect(8, 30, im.getWidth() - 16, 6);
		g.setColor(new Color(196, 160, 84));
		g.fillPolygon(new int[] { 0, im.getWidth() / 2, im.getWidth() }, new int[] { 32, 0, 32 }, 3);
		g.setColor(new Color(240, 226, 190));
		g.setFont(Assets.font(14f));
		String s = "BARRIO TULOG";
		int w = g.getFontMetrics().stringWidth(s);
		g.drawString(s, im.getWidth() / 2 - w / 2, 54);
		g.dispose();
		return im;
	}

	/** Do\u00f1a Liwayway's golden cage (closed or open). */
	private static BufferedImage cage(boolean open) {
		BufferedImage im = new BufferedImage(4 * Game.TILE, 4 * Game.TILE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = im.createGraphics();
		int w = im.getWidth(), h = im.getHeight();
		g.setColor(new Color(0, 0, 0, 60));
		g.fillOval(10, h - 30, w - 20, 26);
		g.setColor(new Color(230, 184, 60));
		g.setStroke(new BasicStroke(5f));
		g.drawArc(8, 6, w - 16, 80, 0, 180);
		g.fillRect(8, h - 24, w - 16, 10);
		g.fillRect(8, 44, w - 16, 8);
		for (int x = 14; x < w - 8; x += 22) {
			if (open && x > w / 2 - 30 && x < w / 2 + 30) continue;
			g.drawLine(x, 46, x, h - 20);
		}
		g.fillOval(w / 2 - 8, 0, 16, 16);
		if (open) {
			g.setColor(new Color(200, 150, 40));
			g.drawLine(w / 2 + 30, 50, w / 2 + 70, h - 40);
		}
		g.dispose();
		return im;
	}
}
