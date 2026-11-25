package celestial.story;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import celestial.Assets;
import celestial.Game;
import celestial.Sound;
import celestial.Text;
import celestial.battle.Battle;
import celestial.battle.BattleHooks;
import celestial.battle.BattleSetup;
import celestial.data.Creature;
import celestial.data.Element;
import celestial.data.Species;
import celestial.ui.CardScreen;
import celestial.ui.PartyScreen;
import celestial.ui.TitleScreen;
import celestial.world.Entity;
import celestial.world.GameMap;
import celestial.world.Hud;
import celestial.world.Maps;
import celestial.world.Overworld;
import celestial.world.Tile;

/**
 * CELESTIAL GROUNDS - the game script, scene by scene.
 * Dialogue follows "Celestial Grounds Game Script: Adventure and Battle".
 * [PLACEHOLDERS] are filled by Story.sub().
 */
public final class Scenes {

	private Scenes() {}

	static int[] P(String key) { return Maps.P(key); }

	// =====================================================================================
	// Shared helpers
	// =====================================================================================

	public static Script sign(String text) {
		return new Script().narrate("The sign reads: '" + text + "'");
	}

	/** Losing: "You rest at the last Banga Shrine. Lola's voice says 'Kumain ka muna, apo.'" */
	public static Script lose(Game g) {
		return new Script()
				.fadeOut()
				.run(gg -> {
					gg.story.healAll();
					gg.story.pandesal = Math.max(gg.story.pandesal, 3);
					gg.world.forceDark = -1;
					gg.world.respawnPlayer(gg.story.shrineMap, gg.story.shrineX, gg.story.shrineY);
				})
				.fadeIn()
				.say("LOLA (voice)", "Kumain ka muna, apo.")
				.system("You rest at the last Banga Shrine. Your team feels better.");
	}

	public static Script shrine(Game g) {
		return new Script()
				.run(gg -> {
					Entity pl = gg.world.player;
					gg.story.shrineMap = gg.world.map.id;
					gg.story.shrineX = pl.tx;
					gg.story.shrineY = pl.ty;
					gg.story.healAll();
					Sound.tone(523, 200, 0, 0.3, Sound.Wave.CHIME);
					Sound.tone(659, 300, 0, 0.3, Sound.Wave.CHIME);
				})
				.system("You rest at the Banga Shrine. Your team is fully healed.")
				.then(gg -> {
					if (gg.story.hasBanga && gg.story.pandesal < 3) {
						gg.story.pandesal = 3;
						return new Script().narrate("Someone left warm Pandesal at the shrine. (Pandesal: 3)");
					}
					return null;
				});
	}

	/** Default text for tiles you face and press Enter on. */
	public static Script tileMessage(Game g, Tile t) {
		switch (t) {
			case SHRINE: return shrine(g);
			case WATER: return new Script().narrate("The water is deep and cool.");
			case LAVA: return new Script().narrate("The lava is too hot to cross.");
			case PADDY: return new Script().narrate("Rice sprouts sway in the flooded paddy.");
			case BAMBOO: return g.world.map.id.equals("KAWAYAN") && !g.story.has("ending")
					? new Script().narrate("The bamboo creaks softly in the wind.") : null;
			case STATUE: return talkStatue(g);
			default: return null;
		}
	}

	/** Story-based walls (the dark yard in Scene 1, leaving town without a partner). */
	public static Script blockMove(Game g, String map, int fx, int fy, int tx, int ty) {
		Story s = g.story;
		if (map.equals("BARRIO")) {
			if (!s.has("off_porch") && g.world.map.tile(fx, fy) == Tile.PORCH && g.world.map.tile(tx, ty) != Tile.PORCH
					&& g.world.map.tile(tx, ty).walkable) {
				s.set("looked_yard");
				return new Script().say("PLAYER", "Too dark. Maybe later.").then(Scenes::maybeLola);
			}
			if (s.starter == null && ty <= 3 && tx >= 13 && tx <= 18) {
				return new Script().say("VILLAGER", "Don't enter the tall grass without a partner! Propesor Bantay's lab is the house with the sign, to the east.");
			}
		}
		return null;
	}

	/** A wild encounter in tall grass or seagrass. */
	public static Script wild(Game g, Species sp, int level) {
		Story s = g.story;
		if (sp == s.starter) {
			return new Script().run(gg -> gg.story.pandesal++)
					.narrate("Something rustles in the grass... a bundle with a free Pandesal! (Your partner's friends must have left it.)");
		}
		BattleSetup setup = BattleSetup.wild(new Creature(sp, level), g.world.map.theme);
		final String flag = "met_" + sp.name();
		final boolean first = !s.has(flag);
		s.set(flag);
		setup.hooks(new BattleHooks() {
			public boolean intro(Battle b) {
				switch (sp) {
					case AHAS_BULKAN:
						b.msg("A wild Ahas-Bulkan appeared! It is spitting tiny flames and looks very proud of itself.");
						go(b);
						if (first) b.tutorial("Fire is weak to Water. Ahas-Bulkan's LASON-ALAB makes burn damage stack with every hit.");
						return true;
					case BINTURO:
						b.msg("A wild Binturo appeared! The air smells like popcorn.");
						go(b);
						if (first) {
							b.say("PLAYER", "Why am I suddenly hungry?");
							b.say("TALA", "That's its ability, Amoy Popcorn. Don't let it distract you!");
						}
						return true;
					case PUGITA:
						b.msg("A wild Pugita appeared! It squirts ink and looks completely innocent.");
						go(b);
						if (first) b.tutorial("Pugita's ability, TINTA, lowers your accuracy. Earth beats Water.");
						return true;
					case DUGONG:
						b.msg("A shy Dugong peeks above the waves.");
						go(b);
						if (first) b.tutorial("Dugong is shy. Attacking it makes it flee. Offer a Pandesal first!");
						return true;
					case TARSI:
						b.msg("A wild Tarsi appeared! It is staring at you without blinking.");
						go(b);
						if (first) b.tutorial("Tarsi's ability, MATA NG GABI: it shows the enemy's next move. Weaken it, offer a Pandesal, then use the Banga.");
						return true;
					default:
						if (s.has("befriend_tip")) return false;
						s.set("befriend_tip");
						b.msg("A wild " + sp.label + " appeared!");
						go(b);
						b.tutorial("Wild Hayop-Diwata can be befriended. Weaken it, offer a Pandesal, then use the Banga.");
						return true;
				}
			}

			private void go(Battle b) {
				b.msg("Go, " + b.ally.name() + "!");
				b.announce();
			}
		});
		return new Script().battle(() -> setup, r -> afterWild(g, sp, r));
	}

	private static Script afterWild(Game g, Species sp, Battle.Result r) {
		if (r == Battle.Result.LOSE) return lose(g);
		if (r == Battle.Result.BEFRIENDED && !g.story.has("tip_" + sp.name())) {
			g.story.set("tip_" + sp.name());
			return new Script().tutorial(sp.label + "'s ability, " + sp.ability.label.toUpperCase() + ": " + sp.ability.description);
		}
		return null;
	}

	/** Hiyas flies into the sky: drawn by overlay(). */
	private static int hiyasAnim = -1, hiyasColor;

	private static Script hiyas(int index, String name) {
		return new Script()
				.run(g -> {
					g.story.hiyas = Math.max(g.story.hiyas, index + 1);
					hiyasAnim = 0;
					hiyasColor = index;
					Sound.tone(659, 200, 0, 0.3, Sound.Wave.CHIME);
					Sound.tone(880, 260, 0, 0.3, Sound.Wave.CHIME);
					Sound.tone(1175, 400, 0, 0.3, Sound.Wave.CHIME);
				})
				.wait(70);
	}

	/** Called every overworld frame. */
	public static void tick(Game g) {
		if (hiyasAnim >= 0 && ++hiyasAnim > 110) hiyasAnim = -1;
		Overworld w = g.world;
		if (w.map.id.equals("TEMPLO") && g.story.has("temple_dark") && !g.story.has("babaylan") && Maps.temploDist != null && g.tick % 80 == 0) {
			int[] target = chimeTarget(w.player.tx, w.player.ty);
			if (target != null) Sound.chime(Math.max(-1, Math.min(1, (target[0] - w.player.tx) / 4.0)));
		}
	}

	/** Extra drawing on top of the overworld: rising Hiyas, temple chimes. */
	public static void overlay(Game g, Graphics2D gr, int camX, int camY) {
		Overworld w = g.world;
		if (hiyasAnim >= 0) {
			int x = (int) w.player.px - camX + 24;
			int y = (int) w.player.py - camY - hiyasAnim * 5;
			Hud.gem(gr, x, y, 16, Hud.HIYAS[hiyasColor]);
			gr.setColor(new Color(255, 255, 255, Math.max(0, 160 - hiyasAnim * 2)));
			gr.fillOval(x - 30, y - 30, 60, 60);
		}
		if (w.map.id.equals("TEMPLO") && g.story.has("temple_dark") && !g.story.has("babaylan") && Maps.temploDist != null) {
			int[] target = chimeTarget(w.player.tx, w.player.ty);
			if (target != null) {
				int sx = target[0] * Game.TILE - camX + 12, sy = target[1] * Game.TILE - camY + 4;
				int a = (int) (90 + 80 * Math.sin(g.tick / 12.0));
				Text.note(gr, sx, sy, 24, new Color(160, 230, 255, Math.max(0, Math.min(255, a))));
			}
		}
	}

	private static int[] chimeTarget(int x, int y) {
		int[][] d = Maps.temploDist;
		if (d[y][x] < 0) return null;
		int[] cur = { x, y };
		for (int i = 0; i < 4; i++) {
			int[] n = Maps.stepToward(d, cur[0], cur[1]);
			if (n == null) break;
			cur = n;
		}
		return cur;
	}

	// =====================================================================================
	// PROLOGUE: BARRIO TULOG
	// =====================================================================================

	public static Script enterBarrio(Game g) {
		if (g.story.has("intro_done")) return null;
		g.story.set("intro_done");
		// SCENE 1: Outside the House
		return new Script()
				.banner("Barrio Tulog, before sunrise")
				.narrate("Before sunrise. The PLAYER, age 11, stands on the porch steps of a small bahay kubo in Barrio Tulog, holding a paper PAROL (star lantern). Past the last lit step, the yard is dark. Above, the moon has a thin crack running across it.")
				.say("PLAYER", "(whispering) It's just a crack. It's just a crack. The moon will be fine.")
				.tutorial("Move with the arrow keys. Press Enter to interact. Look at the moon from the bench, the dark yard, the rooster coop, and the porch door.");
	}

	public static Script lookMoon(Game g) {
		g.story.set("looked_moon");
		return new Script().narrate("You look up at the moon. A thin crack runs across it.")
				.say("PLAYER", "It wasn't like that yesterday.").then(Scenes::maybeLola);
	}

	public static Script lookCoop(Game g) {
		if (g.story.has("ending")) return new Script().narrate("The rooster is asleep. Tomorrow, it will crow.");
		g.story.set("looked_coop");
		return new Script().narrate("A sleeping rooster. It refuses to crow.").then(Scenes::maybeLola);
	}

	public static Script homeDoor(Game g) {
		if (!g.story.has("lola_out")) return lolaComesOut(g);
		if (g.story.has("ending")) return new Script().narrate("Lola's sinangag smells amazing. Maybe later.");
		return new Script().narrate("The smell of garlic rice drifts from the kitchen. The Propesor is waiting, though.");
	}

	/** After looking at two things, Lola comes out on her own. */
	static Script maybeLola(Game g) {
		Story s = g.story;
		if (s.has("lola_out")) return null;
		int looked = (s.has("looked_moon") ? 1 : 0) + (s.has("looked_coop") ? 1 : 0) + (s.has("looked_yard") ? 1 : 0);
		return looked >= 2 ? lolaComesOut(g) : null;
	}

	static Script lolaComesOut(Game g) {
		g.story.set("lola_out");
		int[] door = P("BARRIO.door_home");
		return new Script()
				.narrate("The door creaks open. LOLA steps out, holding a clay pot wrapped in cloth.")
				.spawn(() -> Entity.person("lola", "lola", door[0], door[1], "down").talk(Scenes::talkLola))
				.faceEach("player", "lola")
				.say("LOLA", "Apo! Why are you standing out here in the dark?")
				.say("PLAYER", "I couldn't sleep. The moon, Lola. It's cracked.")
				.say("LOLA", "I know. Even the rooster is too scared to crow. Propesor Bantay is waiting for you at the lab.")
				.say("PLAYER", "But it's still dark.")
				.say("LOLA", "Then it's a good time to start. The sun comes up anyway.")
				.say("LOLA", "First, this is your Banga. It has been in our family for six generations. Don't dent it. It's an antique and it's sacred.")
				.say("PLAYER", "Can something be both?")
				.say("LOLA", "In this family, everything is both. Now... kumain ka na ba?")
				.narrate("Lola hands over 3 Pandesal.")
				.say("PLAYER", "(holding the parol tight) Lola... will the light be enough?")
				.say("LOLA", "A little light never hurt anybody. Ingat, apo.")
				.run(gg -> {
					gg.story.hasBanga = true;
					gg.story.hasParol = true;
					gg.story.pandesal = 3;
					Sound.tok();
				})
				.system("You received a Banga, 3 Pandesal, and a Parol. The Parol lights dark places. Its glow is small now, but it may grow. (Tapang 1)")
				.prompt("Press Enter to take your first step off the porch.")
				.face("player", "down")
				.run(gg -> gg.story.set("off_porch"))
				.walk("player", "down", 1)
				.run(gg -> gg.world.lightPulse())
				.narrate("The player steps into the yard, and the parol lights a small circle around them. The adventure begins.")
				.tutorial("Propesor Bantay's lab is the house with the sign to the north-east. Banga Shrines (clay pots on stone) heal your team.");
	}

	public static Script talkLola(Game g) {
		if (g.story.starter == null) return new Script().say("LOLA", "The Propesor's lab is the house with the sign. Go on, apo. The sun comes up anyway.");
		return new Script().say("LOLA", "Kumain ka na ba? Take care of [STARTER], and let it take care of you.");
	}

	public static Script talkVillager(Game g) {
		if (g.story.starter == null) return villagerWarns(g);
		if (g.story.hiyas >= 1) return new Script().say("VILLAGER", "Did the moon just get a little brighter? Or am I still dreaming?");
		return new Script().say("VILLAGER", "Oh, you have a partner now! Then the tall grass is all yours. Ingat!");
	}

	public static Script villagerWarns(Game g) {
		return new Script()
				.narrate("The player walks through the sleepy barrio.")
				.say("VILLAGER", "Don't enter the tall grass without a partner!");
	}

	public static Script sleepyHouse(Game g) {
		return new Script().narrate(g.story.has("ending") ? "Laughter and the clatter of pots come from inside." : "Zzz... Everyone inside is still asleep.");
	}

	// ------------------------------------------------------------------ SCENE 2: Propesor Bantay's Lab
	public static Script enterLab(Game g) {
		if (g.story.starter != null) return null;
		return new Script()
				.narrate("The lab is a small house covered in jars and a hanging sign: 'Wala pong signal.' PROPESOR BANTAY stands on a ladder holding his phone up to the ceiling.")
				.say("PROPESOR BANTAY", "Wala pa ring signal. Ah! You're here! ...Is that a parol?")
				.say("PLAYER", "...It's for the dark.")
				.say("PROPESOR BANTAY", "Afraid of the dark, ha? Good. People who are afraid look carefully before every step. That makes you a fine Tagapag-alaga.")
				.say("PLAYER", "(quietly) Thank you.")
				.shake(30)
				.say("PROPESOR BANTAY", "Now, last night the moon cracked. The old stories say the Bakunawa, the moon-eating serpent, is waking up. Someone must carry that Banga to the Babaylan at the Tuktok ng Celestia, and befriend Hayop-Diwata along the way.")
				.say("PLAYER", "Me? But I'm afraid of a lot of things.")
				.say("PROPESOR BANTAY", "Then you're perfectly qualified. Everyone is afraid of something. Here, choose a partner.")
				.narrate("Three Hayop-Diwata wait on the lab table: TARSI on a lamp, BINTURO under a chair, and DUGONG in a washtub the Propesor calls 'its aquarium.'")
				.say("PROPESOR BANTAY", "Tarsi sees tomorrow, mostly because it never blinks. Binturo smells like popcorn. I promise it is not a snack. Dugong sings off-key, but it sings for you.")
				.append(chooseStarter());
	}

	private static final Species[] STARTERS = { Species.TARSI, Species.BINTURO, Species.DUGONG };

	private static Script chooseStarter() {
		return new Script().choice("Choose your partner.", new String[] { "Tarsi (Earth)", "Binturo (Fire)", "Dugong (Water)" },
				i -> new Script().choice("Befriend " + STARTERS[i].label + "?", new String[] { "Yes", "No" },
						j -> j == 0 ? pickStarter(STARTERS[i]) : chooseStarter()));
	}

	private static Script pickStarter(Species sp) {
		return new Script()
				.run(g -> {
					Story s = g.story;
					s.starter = sp;
					s.rival = sp == Species.TARSI ? Species.BINTURO : sp == Species.BINTURO ? Species.DUGONG : Species.TARSI;
					s.addCreature(new Creature(sp, 3));
					s.seen.add(Species.TARSI);
					s.seen.add(Species.BINTURO);
					s.seen.add(Species.DUGONG);
					g.world.remove("table_" + sp.sprite);
					Sound.tok();
				})
				.narrate("[STARTER] sniffs the Banga, hops in, and the lid closes. Tok.")
				.say("PROPESOR BANTAY", "And take this Hayop-Journal. It records every animal you meet. Fill it up, and maybe the moon will fill up too.")
				.run(g -> g.story.hasJournal = true)
				.system("[STARTER] joined your party! You received the Hayop-Journal.")
				.tutorial("Press P to see your team and J to open the Hayop-Journal. Each partner knows its signature move and Tulak (Push).");
	}

	public static Script talkPropesor(Game g) {
		if (g.story.starter == null) return enterLab(g);
		if (g.story.hiyas >= 3) return new Script().say("PROPESOR BANTAY", "Three Hiyas! Still no signal, but I can feel it.");
		return new Script().say("PROPESOR BANTAY", "The Babaylan waits at the Tuktok ng Celestia. Go through the barrio gate to the north. And stay out of the tall grass at night... unless you want to make friends.");
	}

	public static Script tableCreature(Game g, Species sp) {
		if (g.story.starter == null) return enterLab(g);
		return new Script().narrate(sp.label + " watches you go. It looks a little proud of you.");
	}

	// ------------------------------------------------------------------ SCENE 3: Dali at the Gate
	public static Script scene3(Game g) {
		int[] gate = P("BARRIO.dali_gate");
		return new Script()
				.spawn(() -> {
					Entity d = Entity.person("dali", "dali", gate[0], gate[1], "down");
					return d;
				})
				.faceEach("player", "dali")
				.narrate("As the player steps through the barrio gate, the first sunlight spills over the rice fields. DALI MAGTANGGOL blocks the path, wearing a salakot like a crown. His partner sits on his shoulder. A goat grazes nearby, and Dali edges away from it.")
				.say("DALI", "Look who finally woke up. (sniffs) Is that a parol? Still need a night-light? Ha!")
				.narrate("The player hides the parol behind their back.")
				.say("PLAYER", "It's... it's not a night-light.")
				.say("DALI", "Sure it isn't. Anyway! I chose first. I took the strongest one, obviously.")
				.say("PLAYER", "You took the one that beats mine.")
				.say("DALI", "Pure coincidence. Totally planned. Same thing. Battle me!")
				.battle(() -> BattleScripts.dali(g), r -> {
					if (r == Battle.Result.LOSE) {
						g.story.clear("dali_gate");
						return new Script().despawn("dali").append(lose(g));
					}
					return new Script()
							.say("DALI", "That doesn't count! The lag was terrible.")
							.say("PLAYER", "We're outdoors.")
							.say("DALI", "Outdoor lag. It exists.")
							.run(gg -> Sound.tone(300, 300, 0.4, 0.3, Sound.Wave.NOISE))
							.narrate("The goat bleats. Dali jumps and hides behind his partner.")
							.face("dali", "right")
							.say("DALI", "I'm... tactically repositioning.")
							.narrate("Dali storms off, taking a very wide detour around the goat.")
							.walk("dali", "left", 1)
							.walk("dali", "up", 2)
							.despawn("dali")
							.narrate("The player looks at the parol, then at the long trail ahead, and walks on.");
				});
	}

	// =====================================================================================
	// MAP 1, EARTH ZONE: KAPATAGAN NG ALINGAWNGAW
	// =====================================================================================

	public static Script enterPalayan(Game g) {
		if (g.story.has("palayan_intro")) return null;
		g.story.set("palayan_intro");
		return new Script()
				.narrate("Rice terraces glow gold in the morning light. A wooden sign reads: 'Please do not startle the carabao. The carabao has feelings.' The trail is blocked by a large KALABAW. Nothing rushes it.");
	}

	// ------------------------------------------------------------------ SCENE 4: Palayan Trail
	public static Script scene4(Game g) {
		if (g.story.has("kalabaw_tut")) return null;
		g.story.set("kalabaw_busy");
		int[] k = P("PALAYAN.kalabaw");
		return new Script()
				.walkTo("player", k[0], k[1] + 1)
				.face("player", "up")
				.say("PLAYER", "Excuse me. Hello. Kalabaw?")
				.narrate("Kalabaw chews slowly and does not move.")
				.battle(() -> BattleScripts.kalabawTutorial(g), r -> {
					g.story.clear("kalabaw_busy");
					if (r == Battle.Result.LOSE) return lose(g);
					if (r != Battle.Result.BEFRIENDED) {
						return new Script().walk("player", "down", 1)
								.narrate("Kalabaw is still blocking the trail. Weaken it, offer a Pandesal, then use the Banga.");
					}
					return new Script()
							.despawn("kalabaw")
							.set("kalabaw_tut")
							.tutorial("Kalabaw's ability, TIYAGA: it survives one lethal hit with 1 HP, once per battle.")
							.say("PLAYER", "Salamat, Kalabaw. You're heavier than you look.")
							.narrate("Kalabaw chews. The path is clear.");
				});
	}

	// ------------------------------------------------------------------ SCENE 5: Tala and Bituin
	public static Script scene5(Game g) {
		int[] m = P("PALAYAN.meet"), ts = P("PALAYAN.tala_spawn");
		return new Script()
				.walkTo("player", m[0], m[1])
				.face("player", "left")
				.spawn(() -> Entity.person("tala", "tala", ts[0] - 1, ts[1] + 1, "right"))
				.walk("tala", "up", 1)
				.walk("tala", "right", m[0] - ts[0])
				.narrate("Halfway down the trail, a girl bursts out of the bamboo clutching a woven bayong that glows from inside. TALA trips and falls. Out floats BITUIN, a baby star-creature the size of a rice ball.")
				.spawn(() -> {
					Entity b = Entity.still("bituin", Species.BITUIN.image(), 40, 40, m[0] - 1, m[1] - 1);
					b.bob = true;
					b.solid = false;
					b.light = 1.2;
					return b;
				})
				.run(gg -> Sound.tone(1046, 300, 0, 0.25, Sound.Wave.CHIME))
				.say("TALA", "Please don't be scared. He's just shy. And glowing. A lot.")
				.say("PLAYER", "What is that?")
				.say("TALA", "Bituin. He glows brighter every time the moon cracks. Please, don't tell anyone.")
				.spawn(() -> {
					Entity t = Entity.still("tricycle", Assets.cg("obj_tricycle.png"), 96, 72, P("PALAYAN.tricycle_start")[0], P("PALAYAN.tricycle_start")[1]);
					t.speed = 8;
					t.solid = false;
					return t;
				})
				.run(gg -> Sound.tone(90, 600, 0.6, 0.4, Sound.Wave.DRUM))
				.walk("tricycle", "left", P("PALAYAN.tricycle_start")[0] - P("PALAYAN.tricycle_stop")[0])
				.shake(12)
				.narrate("A tricycle with a giant videoke speaker skids onto the path. KUYA GUSMO of TEAM TAMBAY hops off, wearing flip-flops and a shirt that reads 'BAHALA NA.' Two grunts lean on the tricycle, doing nothing professionally.")
				.spawn(() -> Entity.person("gusmo", "gusmo", P("PALAYAN.gusmo_spawn")[0], P("PALAYAN.gusmo_spawn")[1], "down"))
				.spawn(() -> Entity.person("grunt1", "tambay", 21, 9, "left"))
				.spawn(() -> Entity.person("grunt2", "tambay", 22, 8, "left"))
				.walk("gusmo", "down", m[1] - P("PALAYAN.gusmo_spawn")[1])
				.walk("gusmo", "left", P("PALAYAN.gusmo_spawn")[0] - m[0] - 1)
				.faceEach("player", "gusmo")
				.say("GUSMO", "Pre, bawal tumakbo. Do\u00f1a Ginhawa pays us in load and lugaw. Give us the star.")
				.say("TALA", "His name is Bituin.")
				.say("GUSMO", "Okay. Give us Bituin.")
				.say("PLAYER", "Hindi.")
				.say("GUSMO", "Hay naku. Videoke battle, then. Loudest bass wins.")
				.append(scene6(g));
	}

	// ------------------------------------------------------------------ SCENE 6: Battle: Kuya Gusmo
	public static Script scene6(Game g) {
		return new Script().battle(() -> BattleScripts.gusmo(g), r -> {
			if (r == Battle.Result.LOSE) return lose(g);
			int[] stop = P("PALAYAN.tricycle_stop");
			return new Script()
					.run(gg -> gg.story.set("gusmo_beaten"))
					.say("GUSMO", "Aray ko po. You hit harder than my landlord.")
					.say("GUSMO", "Fine, fine. Do\u00f1a Ginhawa is at Bundok Alab. She's... nice? She gives out lugaw. Don't tell her I told you.")
					.narrate("Gusmo leaves on the tricycle. He returns for his speaker. He leaves again.")
					.then(gg -> {
						Entity gu = gg.world.find("gusmo");
						Script s = new Script();
						if (gu != null) s.walkTo("gusmo", stop[0], stop[1] + 1);
						return s;
					})
					.despawn("gusmo").despawn("grunt1").despawn("grunt2")
					.then(gg -> gg.world.find("tricycle") == null ? new Script().spawn(() -> {
						Entity t = Entity.still("tricycle", Assets.cg("obj_tricycle.png"), 96, 72, stop[0], stop[1]);
						t.speed = 8;
						t.solid = false;
						return t;
					}) : null)
					.walk("tricycle", "right", 9)
					.wait(30)
					.walk("tricycle", "left", 4)
					.run(gg -> Sound.tone(80, 400, 0.5, 0.4, Sound.Wave.DRUM))
					.wait(20)
					.walk("tricycle", "right", 4)
					.despawn("tricycle")
					.then(gg -> gg.world.find("tala") == null
							? new Script().spawn(() -> Entity.person("tala", "tala", gg.world.player.tx - 1, gg.world.player.ty, "right")) : null)
					.faceEach("player", "tala")
					.say("TALA", "Thank you. I can't battle, but Bituin and I need a place to hide. May I come with you?")
					.say("PLAYER", "Tara na.")
					.despawn("bituin")
					.run(gg -> {
						gg.story.talaFollowing = true;
						gg.story.set("tala_joined");
						gg.world.startFollower();
					})
					.system("Tala joined you as a guide. Tap HINT in battle to hear her advice.");
		});
	}

	public static Script rematchGusmo(Game g) {
		return new Script().say("GUSMO", "Round two, pre? Loudest bass wins.").append(scene6(g));
	}

	// ------------------------------------------------------------------ SCENE 7: Kawayan Maze (First Dark Path)
	public static Script enterKawayan(Game g) {
		if (g.story.has("kawayan_intro")) return null;
		g.story.set("kawayan_intro");
		return new Script()
				.narrate("Deeper in the maze, the bamboo closes overhead and the sunlight fades to twilight. The player lifts the parol. Fireflies mark a hidden path, and the bamboo seems to whisper the player's name.")
				.say("BAMBOO", "(creaking) ...[PLAYER NAME]... [PLAYER NAME]...")
				.say("PLAYER", "Okay, that is not creepy at all.")
				.tutorial("Follow the fireflies through the bamboo.");
	}

	/** The dark moment happens halfway along the fireflies' path. */
	public static Script kawayanStep(Game g) {
		if (g.story.has("kawayan_dark")) return null;
		Entity p = g.world.player;
		int d = Maps.kawayanDist[p.ty][p.tx];
		if (d < 0 || d > Maps.kawayanLen / 2) return null;
		g.story.set("kawayan_dark");
		return new Script()
				.run(gg -> {
					gg.story.set("fireflies_out");
					gg.world.forceDark = 0.96;
					Sound.tone(220, 500, 0, 0.2, Sound.Wave.SINE);
				})
				.wait(40)
				.narrate("Suddenly every firefly goes out. The screen darkens to a small circle of light around the parol. The player freezes and hugs the lantern.")
				.say("TALA", "Hey. You're shaking.")
				.say("PLAYER", "I'm not scared.")
				.narrate("A pause.")
				.say("PLAYER", "...I'm a little scared. I've always been afraid of the dark.")
				.say("TALA", "Me too, sometimes. That's why Bituin glows when I'm scared. See?")
				.run(gg -> {
					if (gg.world.follower != null) gg.world.follower.light = 2.4;
					Sound.tone(1046, 400, 0, 0.25, Sound.Wave.CHIME);
				})
				.narrate("Bituin glows softly. The path ahead becomes faintly visible.")
				.say("TALA", "We can go together. Just one step.")
				.prompt("Press Enter to take one step into the dark.")
				.then(gg -> {
					Entity pl = gg.world.player;
					int[] n = Maps.stepToward(Maps.kawayanDist, pl.tx, pl.ty);
					Script s = new Script();
					if (n != null) {
						String dir = n[0] > pl.tx ? "right" : n[0] < pl.tx ? "left" : n[1] > pl.ty ? "down" : "up";
						s.walk("player", dir, 1);
					}
					return s;
				})
				.run(gg -> {
					gg.story.clear("fireflies_out");
					gg.world.forceDark = -1;
					if (gg.world.follower != null) gg.world.follower.light = 1.3;
				})
				.narrate("The player steps forward. The fireflies relight, one by one.")
				.tapang(2, "Tapang increased! (Tapang 2). The light circle around you grows.")
				.then(gg -> gg.story.starter != Species.TARSI
						? new Script().battle(() -> BattleScripts.wildTarsi(gg), r -> {
							if (r == Battle.Result.LOSE) return lose(gg);
							return new Script().say("TALA", "Tarsi sees in the dark. Maybe it will show us the way.");
						})
						: new Script()
								.narrate("A bag of 3 Pandesal hangs from a bamboo stalk with a note in Lola's handwriting: 'Kumain ka na ba?'")
								.run(x -> x.story.pandesal += 3)
								.system("You received 3 Pandesal.")
								.say("TALA", "Tarsi sees in the dark. Maybe it will show us the way."));
	}

	// ------------------------------------------------------------------ SCENE 8: Guardian Battle: Tamaraw
	public static Script scene8(Game g) {
		return new Script()
				.narrate("Sunset. A wide clearing at the top of the terraces. TAMARAW, a compact, black-horned buffalo, stands in the center, stubborn as a mountain.")
				.say("TAMARAW", "Hindi ako aatras. I will not back down.")
				.say("PLAYER", "We're not asking you to.")
				.say("TAMARAW", "Then why do you stand in my field?")
				.say("PLAYER", "The moon is cracking. We need your Hiyas to restore it.")
				.say("TAMARAW", "The Hiyas is not given. It is earned. Prove that you walk beside others, not above them.")
				.say("TALA", "Tamaraw gets stronger the weaker it feels. Don't let this drag on!")
				.append(tamarawBattle(g));
	}

	private static Script tamarawBattle(Game g) {
		return new Script().battle(() -> BattleScripts.tamaraw(g), r -> {
			if (r == Battle.Result.LOSE) return lose(g);
			return new Script()
					.say("TAMARAW", "You fight like someone who has friends. And you carry a light, small but steady. Do not hide it. Take the Hiyas.")
					.append(hiyas(0, "Lupa"))
					.narrate("The Hiyas ng Lupa rises, glowing brown-gold, and flies into the sky. One slice of the moon brightens.")
					.set("hiyas_lupa")
					.despawn("tamaraw")
					.say("TALA", "It's warmer...")
					.narrate("Bituin hums. In the distance, Bundok Alab glows orange. A jeepney horn honks.")
					.run(gg -> Sound.tone(392, 250, 0.6, 0.3, Sound.Wave.SINE))
					.system("Hiyas ng Lupa obtained! (1 of 3)");
		});
	}

	public static Script rematchTamaraw(Game g) {
		if (!g.story.has("tamaraw_talk")) return scene8(g);
		return new Script().say("TAMARAW", "Hindi ako aatras. Again, then.").append(tamarawBattle(g));
	}

	// =====================================================================================
	// MAP 1, FIRE ZONE: BUNDOK ALAB
	// =====================================================================================

	// ------------------------------------------------------------------ SCENE 9: Jeepney to Bundok Alab
	public static Script scene9(Game g) {
		if (!g.story.has("hiyas_lupa"))
			return new Script().say("CONDUCTOR", "Bundok Alab? We leave once the Guardian of the Earth is satisfied. It's up in the clearing.");
		int[] j = P("TUKTOK.jeepney");
		if (g.story.has("jeepney")) {
			return new Script().choice("Ride the jeepney to Bundok Alab?", new String[] { "Sakay na (Ride)", "Not yet" },
					i -> i == 0 ? new Script().say("CONDUCTOR", "Bayad po! Bundok Alab!").teleport("BULKAN", P("BULKAN.arrive")[0], P("BULKAN.arrive")[1], "right") : null);
		}
		return new Script()
				.say("CONDUCTOR", "Bayad po! Sukli po! Bundok Alab, wala pong signal!")
				.spawn(() -> {
					Entity d = Entity.person("dali", "dali", j[0] + 1, j[1], "left");
					d.drawOffsetY = -46;
					d.solid = false;
					return d;
				})
				.say("DALI", "(from the roof) I'm not following you. The jeepney is just going my way.")
				.say("PLAYER", "You're on the roof.")
				.say("DALI", "Best seat in the house. (the jeepney lurches) Ow.")
				.shake(20)
				.set("jeepney")
				.fadeOut()
				.despawn("dali")
				.screen(gg -> new CardScreen(gg, CardScreen.Kind.TRAVEL, "The jeepney rattles toward Bundok Alab. Bayad po! Sukli po!"))
				.run(gg -> gg.world.loadMap("BULKAN", P("BULKAN.arrive")[0], P("BULKAN.arrive")[1], "right"))
				.fadeIn();
	}

	public static Script jeepneyBack(Game g) {
		return new Script().choice("Ride the jeepney back to Tuktok ng Palayan?", new String[] { "Sakay na (Ride)", "Stay" },
				i -> i == 0 ? new Script().teleport("TUKTOK", P("TUKTOK.stop")[0], P("TUKTOK.stop")[1], "left") : null);
	}

	// ------------------------------------------------------------------ SCENE 10: Volcano Trail and the Dark Tunnel
	public static Script enterBulkan(Game g) {
		if (g.story.has("bulkan_intro")) return null;
		g.story.set("bulkan_intro");
		return new Script()
				.narrate("Black rock, glowing lava rivers, heat shimmering in the air.")
				.say("TALA", "Bituin is sweating. Is that even possible?");
	}

	public static Script lavaNote(Game g) {
		return new Script().tutorial("PUZZLE: a lava river blocks the path. Face the lava and press Enter to send out an Earth or Water creature. Earth stomps a stone bridge. Water cools the lava into a walkable path.");
	}

	public static Script lavaPuzzle(Game g) {
		if (g.story.has("lava_bridge")) return new Script().narrate("The lava is too hot to cross. Use the path you made.");
		List<Creature> helpers = new ArrayList<>();
		for (Creature c : g.story.party) if (c.element() == Element.LUPA || c.element() == Element.TUBIG) helpers.add(c);
		for (Creature c : g.story.banga) if (c.element() == Element.LUPA || c.element() == Element.TUBIG) helpers.add(c);
		if (helpers.isEmpty())
			return new Script().narrate("A lava river blocks the path.").say("TALA", "We need an Earth or Water Hayop-Diwata to get across this!");
		String[] opts = new String[helpers.size() + 1];
		for (int i = 0; i < helpers.size(); i++) opts[i] = helpers.get(i).name() + " (" + helpers.get(i).element().english + ")";
		opts[helpers.size()] = "Not now";
		return new Script().narrate("A lava river blocks the path.").choice("Send out which Hayop-Diwata?", opts, i -> {
			if (i >= helpers.size()) return null;
			Creature c = helpers.get(i);
			boolean earth = c.element() == Element.LUPA;
			int[] b = P("BULKAN.bank");
			return new Script()
					.spawn(() -> {
						Entity e = Entity.still("helper", c.image(), 72, 72, b[0] + 1, b[1]);
						e.solid = false;
						return e;
					})
					.shake(earth ? 30 : 10)
					.run(gg -> {
						Sound.tone(earth ? 70 : 0, 500, 0, 0.5, earth ? Sound.Wave.DRUM : Sound.Wave.NOISE);
						for (int x = 14; x <= 15; x++) {
							gg.world.map.setTile(x, 15, 'K');
							gg.world.map.setTile(x, 16, 'K');
						}
						gg.story.set("lava_bridge");
					})
					.narrate(earth ? c.name() + " stomps the ground. Stones rise from the lava and form a bridge!"
							: c.name() + " sprays water. The lava hisses and cools into a walkable path!")
					.despawn("helper");
		});
	}

	public static Script tunnel(Game g) {
		boolean fire = g.story.party.stream().anyMatch(c -> c.element() == Element.APOY);
		Script s = new Script()
				.walkTo("player", 15, 10)
				.face("player", "up")
				.narrate("The trail enters a cooled lava tunnel. The orange glow fades. It is nearly pitch dark. Tala stops at the entrance.")
				.say("TALA", "I... I can't see the end of it.")
				.say("PLAYER", "(a deep breath) I'll go first.")
				.say("TALA", "You? But you're...")
				.say("PLAYER", "Still scared. But I'd rather be scared and walking than scared and standing still.")
				.narrate("The player holds up the parol and steps in.")
				.walk("player", "up", 1);
		if (fire) s.narrate("Fire creatures on your team make the tunnel walls glow.");
		return s.tapang(3, "Tapang increased! (Tapang 3). The light circle around you grows.");
	}

	// ------------------------------------------------------------------ SCENE 11: Do\u00f1a Liwayway and the Cage
	public static Script scene11(Game g) {
		return new Script()
				.walkTo("player", 14, 6)
				.face("player", "up")
				.narrate("A gold cage the size of a house hangs over the crater. Inside, HARIBON perches with its wings tucked and its eyes sharp. DO\u00d1A LIWAYWAY GINHAWA stands before it in a gold terno.")
				.say("DO\u00d1A", "Tala, hija. Come home.")
				.say("TALA", "Mama, let him go.")
				.say("DO\u00d1A", "I am keeping him safe. Para sa kanilang kaligtasan.")
				.say("TALA", "That's not safe, Mama. That's a cage.")
				.say("DO\u00d1A", "Everything precious must be kept. The star. The eagle. You.")
				.say("PLAYER", "Hindi lahat ng mahalaga ay kailangang ikulong.")
				.say("DO\u00d1A", "Then prove it.")
				.append(donaBattle(g));
	}

	private static Script donaBattle(Game g) {
		return new Script().battle(() -> BattleScripts.dona(g), r -> {
			if (r == Battle.Result.LOSE) return lose(g);
			return new Script()
					.say("DO\u00d1A", "...Impossible.")
					.spawn(() -> Entity.person("dali", "dali", 16, 1, "down"))
					.walk("dali", "down", 1)
					.run(gg -> Sound.tok())
					.shake(14)
					.run(gg -> {
						gg.story.set("dona_beaten");
						gg.world.remove("cage");
						gg.world.remove("haribon_caged");
						for (GameMap.Spawn sp : gg.world.map.spawns) {
							Entity e = sp.when.test(gg) ? sp.make.apply(gg) : null;
							if (e != null && (e.id.equals("cage") || e.id.equals("haribon"))) gg.world.spawn(e);
						}
					})
					.narrate("DALI swings down from a rope and smacks the cage latch with his salakot. The door swings open.")
					.say("DALI", "It's not heroics. I just hate locks.")
					.say("DO\u00d1A", "Hindi pa ito tapos.")
					.narrate("Do\u00f1a retreats to a golden boat and sails toward the sea.")
					.walk("dona", "right", 11)
					.despawn("dona")
					.spawn(() -> {
						Entity bt = Entity.still("goldboat", Assets.cg("obj_goldboat.png"), 144, 72, 28, 6);
						bt.solid = false;
						bt.noShadow = true;
						bt.speed = 3;
						return bt;
					})
					.walk("goldboat", "right", 4)
					.despawn("goldboat")
					.walk("dali", "up", 2)
					.despawn("dali")
					.append(scene12(g));
		});
	}

	public static Script rematchDona(Game g) {
		return new Script().say("DO\u00d1A", "Back again? I'm only keeping them safe.").append(donaBattle(g));
	}

	// ------------------------------------------------------------------ SCENE 12: Guardian Battle: Haribon
	public static Script scene12(Game g) {
		return new Script()
				.narrate("The freed Haribon soars up, then dives down. The air thickens with heat.")
				.shake(16)
				.say("HARIBON", "I strike first. I always strike first.")
				.say("PLAYER", "We're not here for a cage. We're here for the Hiyas.")
				.say("HARIBON", "Then show me you can stand your ground when everything strikes first.")
				.append(haribonBattle(g));
	}

	private static Script haribonBattle(Game g) {
		return new Script().battle(() -> BattleScripts.haribon(g), r -> {
			if (r == Battle.Result.LOSE) return lose(g);
			return new Script()
					.say("HARIBON", "You stood. Take the Hiyas.")
					.append(hiyas(1, "Apoy"))
					.set("hiyas_apoy")
					.despawn("haribon")
					.system("Hiyas ng Apoy obtained! (2 of 3). The moon glows two slices brighter.")
					.run(gg -> {
						for (GameMap.Spawn sp : gg.world.map.spawns) {
							if (!sp.when.test(gg)) continue;
							Entity e = sp.make.apply(gg);
							if (e != null && (e.id.equals("ferry") || e.id.equals("captain"))) gg.world.spawn(e);
						}
					})
					.say("TALA", "Look, a ferry at the crater's edge! The sea is that way.");
		});
	}

	public static Script rematchHaribon(Game g) {
		return new Script().say("HARIBON", "I strike first. Stand your ground.").append(haribonBattle(g));
	}

	// =====================================================================================
	// MAP 2: DAGAT TALA (WATER)
	// =====================================================================================

	// ------------------------------------------------------------------ SCENE 13: Ferry and Reef Routes
	public static Script scene13(Game g) {
		return new Script()
				.say("FERRY CAPTAIN", "Hold on to your pandesal, ha. The waves are in a mood.")
				.choice("Board the ferry to Dagat Tala?", new String[] { "Board", "Not yet" }, i -> i != 0 ? null
						: new Script()
								.fadeOut()
								.screen(gg -> new CardScreen(gg, CardScreen.Kind.TRAVEL, "The ferry rolls over the waves toward Dagat Tala."))
								.run(gg -> {
									gg.world.loadMap("DAGAT", P("DAGAT.arrive")[0], P("DAGAT.arrive")[1], "right");
								})
								.fadeIn());
	}

	public static Script enterDagat(Game g) {
		if (g.story.has("dagat_intro")) return null;
		g.story.set("dagat_intro");
		return new Script()
				.narrate("Sea routes and sandbars stretch across Dagat Tala. Far to the north-east, a temple glows under the water.")
				.say("TALA", "The seagrass is full of Hayop-Diwata. Pugita is Water, so Earth beats it!");
	}

	public static Script ferryBack(Game g) {
		return new Script().say("FERRY CAPTAIN", "Back to Bundok Alab? The waves are still in a mood.")
				.choice("Ride the ferry back?", new String[] { "Ride", "Stay" }, i -> i != 0 ? null
						: new Script().teleport("CRATER", P("CRATER.captain")[0] - 1, P("CRATER.captain")[1], "left"));
	}

	// ------------------------------------------------------------------ SCENE 14: The Sunken Temple (Total Darkness)
	public static Script enterTemple(Game g) {
		if (g.story.has("temple_dark")) return null;
		return new Script()
				.narrate("An air-bubble path leads into a glowing temple. Water drips onto the player's parol. The paper is soaked, and the flame goes out. Total darkness. Bituin's glow is faint, like a distant star.")
				.run(gg -> {
					gg.story.parolLit = false;
					gg.story.set("bituin_faint");
					Sound.tone(200, 600, 0, 0.3, Sound.Wave.NOISE);
				})
				.say("TALA", "I can't see anything! Bituin is too weak!")
				.say("PLAYER", "(trembling) I can't see either.")
				.narrate("The screen is almost entirely black. Only the sound of the player's breathing is heard.")
				.say("PLAYER", "I'm scared. I'm really scared. But we can't stay here.")
				.say("PLAYER", "Tala, hold my hand. We'll walk together. One step at a time.")
				.run(gg -> gg.story.set("temple_dark"))
				.tutorial("PUZZLE: walk by sound. Soft chimes ring from the correct direction. If you can't hear them, follow the faint music notes.");
	}

	public static Script templeStep(Game g) {
		if (!g.story.has("temple_dark") || g.story.has("banga_glow")) return null;
		Entity p = g.world.player;
		int d = Maps.temploDist[p.ty][p.tx];
		if (d < 0 || d > Maps.temploLen / 2) return null;
		return bangaGlow(g);
	}

	private static Script bangaGlow(Game g) {
		g.story.set("banga_glow");
		return new Script()
				.narrate("As the player walks, the Banga in the bag begins to glow, a warm light that comes from the player's own courage.")
				.run(gg -> {
					gg.story.heroLight = true;
					gg.world.lightPulse();
				})
				.say("BABAYLAN", "(voice) You walk in the dark, small one. And yet you glow.")
				.say("PLAYER", "I... what?")
				.say("BABAYLAN", "The light was never in the parol. It was in you all along.")
				.tapang(4, "Tapang increased! (Tapang 4). The Banga glows with the hero's own light.");
	}

	public static Script babaylan(Game g) {
		Script s = new Script();
		if (!g.story.has("banga_glow")) s.append(bangaGlow(g));
		int[] door = P("TEMPLO.reef_door");
		return s
				.run(gg -> Sound.tone(784, 800, 0, 0.3, Sound.Wave.CHIME))
				.narrate("A Babaylan statue lights up and speaks.")
				.say("BABAYLAN", "Pito ang buwan noon. Seven moons lit the sky.")
				.say("BABAYLAN", "The first light-keeper lost its own light. In its hunger, it ate six moons. The people called it Bakunawa, and forgot why it was hungry.")
				.say("TALA", "So it isn't a monster. It's lonely.")
				.say("PLAYER", "And scared. It's dark where it is, and nobody's there.")
				.say("PLAYER", "Then we don't defeat it. We remember it.")
				.run(gg -> {
					gg.story.clear("bituin_faint");
					gg.world.map.setTile(door[0], door[1], 'D');
					gg.world.shake(20);
				})
				.narrate("Bituin brightens and draws a tiny constellation in the air. A door opens toward the open reef.");
	}

	public static Script talkStatue(Game g) {
		if (!g.story.has("babaylan")) return new Script().narrate("A carved stone figure. It feels like it's listening.");
		return new Script().say("BABAYLAN", "Pito ang buwan noon. Remember it, small one, and it will remember you.");
	}

	// ------------------------------------------------------------------ SCENE 15: Guardian Battle: Butanding
	public static Script scene15(Game g) {
		return new Script()
				.narrate("A vast shape glides through the water. BUTANDING, a whale shark with spots like constellations, circles you slowly.")
				.say("BUTANDING", "Nagmamadali ka ba? No? Then sit. Eat. We talk first.")
				.say("PLAYER", "We don't have time.")
				.say("BUTANDING", "Nobody ever has time. That is why the moon is cracked. Fine. Fight me, quickly.")
				.append(butandingBattle(g));
	}

	private static Script butandingBattle(Game g) {
		return new Script().battle(() -> BattleScripts.butanding(g), r -> {
			if (r == Battle.Result.LOSE) return lose(g);
			return new Script()
					.say("BUTANDING", "Good. You rushed, but you didn't forget the others. Take the Hiyas.")
					.append(hiyas(2, "Tubig"))
					.set("hiyas_tubig")
					.despawn("butanding")
					.system("Hiyas ng Tubig obtained! (3 of 3).")
					.say("TALA", "The current is pulling upward... to the Tuktok ng Celestia!");
		});
	}

	public static Script rematchButanding(Game g) {
		if (!g.story.has("butanding_talk")) {
			g.story.set("butanding_talk");
			return scene15(g);
		}
		return new Script().say("BUTANDING", "Back so soon? Sit. Eat. Then fight me, quickly.").append(butandingBattle(g));
	}

	// =====================================================================================
	// FINAL BOSS ARENA: TUKTOK NG CELESTIA
	// =====================================================================================

	public static Script enterCelestia(Game g) {
		if (g.story.has("celestia_intro")) return null;
		g.story.set("celestia_intro");
		return new Script().narrate("The stone path rises out of the sea to the Tuktok ng Celestia. The cracked moon hangs close enough to touch.")
				.tutorial("Rest at the Banga Shrine first. Then walk to the center of the summit.");
	}

	// ------------------------------------------------------------------ SCENE 16: Bituin Evolves
	public static Script scene16(Game g) {
		int[] a = P("CELESTIA.altar");
		return new Script()
				.spawn(() -> {
					Entity b = Entity.still("bituin", Species.BITUIN.image(), 48, 48, a[0] + 3, a[1] - 1);
					b.bob = true;
					b.solid = false;
					b.light = 1.5;
					b.lightColor = new Color(255, 240, 160);
					return b;
				})
				.run(gg -> { if (gg.world.follower != null) gg.world.follower.lookAt(gg.world.player); })
				.narrate("The three Hiyas orbit Bituin. He glows brighter and brighter.")
				.run(gg -> {
					gg.fadeColor = Color.WHITE;
					Sound.tone(523, 300, 0, 0.3, Sound.Wave.CHIME);
					Sound.tone(659, 400, 0, 0.3, Sound.Wave.CHIME);
					Sound.tone(784, 500, 0, 0.3, Sound.Wave.CHIME);
				})
				.fadeOut()
				.despawn("bituin")
				.run(gg -> {
					gg.story.set("bituin_evolved");
					for (GameMap.Spawn sp : gg.world.map.spawns) {
						if (!sp.when.test(gg)) continue;
						Entity e = sp.make.apply(gg);
						if (e != null && e.id.equals("bituin_form")) gg.world.spawn(e);
					}
				})
				.fadeIn()
				.run(gg -> gg.fadeColor = Color.BLACK)
				.narrate(g.story.boy ? "Araw route: he becomes APOLAKI." : "Buwan route: he becomes MAYARI.")
				.say("[BITUIN FORM]", "I remember now. I am the light the Bakunawa lost. I came back to find it. And you carried a light too, little one. You only needed to find it.")
				.say("TALA", "Bituin...?")
				.system("Choose your final team of 3. Bring one creature of each element!")
				.then(Scenes::missingElementHint)
				.screen(gg -> new PartyScreen(gg, true))
				.shake(40)
				.run(gg -> Sound.tone(60, 1200, 0, 0.5, Sound.Wave.DRUM))
				.run(gg -> {
					for (GameMap.Spawn sp : gg.world.map.spawns) {
						if (!sp.when.test(gg)) continue;
						Entity e = sp.make.apply(gg);
						if (e != null && e.id.equals("bakunawa")) gg.world.spawn(e);
					}
				})
				.narrate("The sea rises. The Bakunawa emerges, a serpent made of broken light.")
				.append(scene17(g));
	}

	// ------------------------------------------------------------------ SCENE 17: Final Boss: Ang Bakunawa
	public static Script scene17(Game g) {
		return new Script()
				.say("BAKUNAWA", "Walang naghintay sa akin. No one waited for me.")
				.say("PLAYER", "Kami. Kami ang naghintay. We did.")
				.run(gg -> gg.story.tapang = 5)
				.append(bakunawaBattle(g));
	}

	/** Tala points out a missing element (and where its Common creature lives). */
	static Script missingElementHint(Game g) {
		boolean[] have = new boolean[3];
		List<Creature> all = new ArrayList<>(g.story.party);
		all.addAll(g.story.banga);
		for (Creature c : all) {
			if (c.element() == Element.LUPA) have[0] = true;
			if (c.element() == Element.APOY) have[1] = true;
			if (c.element() == Element.TUBIG) have[2] = true;
		}
		String[] where = { "Kalabaw grazes in the Palayan Trail's tall grass", "Ahas-Bulkan hides in the Volcano Trail's cogon grass",
				"Pugita lives in the seagrass of Dagat Tala" };
		String[] el = { "Earth", "Fire", "Water" };
		for (int i = 0; i < 3; i++)
			if (!have[i])
				return new Script().say("TALA", "We don't have a " + el[i] + " friend yet... " + where[i] + ". We can still go back down and befriend one!");
		return null;
	}

	private static Script bakunawaBattle(Game g) {
		return new Script().battle(() -> BattleScripts.bakunawa(g), r -> {
			if (r == Battle.Result.LOSE) return lose(g).then(Scenes::missingElementHint);
			return new Script().set("bakunawa_calmed").append(scene18(g));
		});
	}

	public static Script rematchBakunawa(Game g) {
		return new Script().choice("Face the Bakunawa again?", new String[] { "Yes", "Not yet (P: change team)" },
				i -> i == 0 ? new Script().say("BAKUNAWA", "Walang naghintay sa akin.").say("PLAYER", "Kami ang naghintay. We did.").append(bakunawaBattle(g)) : null);
	}

	// ------------------------------------------------------------------ SCENE 18: Ending and the Light Within
	public static Script scene18(Game g) {
		int[] porch = P("BARRIO.start");
		return new Script()
				.fadeOut()
				.run(gg -> {
					Story s = gg.story;
					s.set("ending");
					s.set("seven_moons");
					s.talaFollowing = false;
					s.parolLit = false;
					s.heroLight = false;
					gg.world.loadMap("BARRIO", 15, 10, "up");
					spawnCelebration(gg);
				})
				.fadeIn()
				.narrate("Seven moons light the sky. The villagers bang pots and pans in celebration. Lola arrives on a carabao.")
				.run(gg -> {
					for (int i = 0; i < 6; i++) Sound.tone(900 + i * 70, 80, (i % 2) * 2 - 1, 0.2, Sound.Wave.WOOD);
				})
				.walk("lola_ride", "down", 2)
				.walk("carabao", "down", 2)
				.say("LOLA", "Kumain na ba kayo?")
				.say("DALI", "I would like to formally announce that I won a battle.")
				.say("PLAYER", "Against who?")
				.say("DALI", "(shuddering) ...A goat.")
				.run(gg -> Sound.tone(300, 300, 0.4, 0.3, Sound.Wave.NOISE))
				.narrate("A goat bleats proudly in the background. It is Dali's greatest fear, and he beat it.")
				.narrate("Tala hugs her mother. Do\u00f1a Liwayway looks at the sky and sighs: 'The moon looks safe without a cage.' Kuya Gusmo opens a sari-sari store. The Bakunawa becomes the sea's gentle guardian.")
				.fadeOut()
				.run(gg -> {
					gg.world.loadMap("BARRIO", porch[0], porch[1], "down");
					int i = 0;
					List<Creature> all = new ArrayList<>(gg.story.party);
					all.addAll(gg.story.banga);
					for (Creature c : all) {
						Entity e = Entity.still("pal" + i, c.image(), 56, 56, 2 + i * 2, 18 + (i % 2));
						e.solid = false;
						gg.world.spawn(e);
						i++;
					}
				})
				.fadeIn()
				.narrate("Night outside the player's house, where the story began. The player sits on the porch steps with the parol beside them, unlit. The yard is dark, and the player looks out at the seven moons and smiles.")
				.screen(gg -> new CardScreen(gg, CardScreen.Kind.SKY, null))
				.say("PLAYER", "(narration) I'm still afraid sometimes. But the dark isn't empty anymore.")
				.fadeOut()
				.screen(gg -> new CardScreen(gg, CardScreen.Kind.QUOTE, "\"Darkness may swallow us, as our fears once did, yet there is light in us. We only need to find it.\""))
				.screen(gg -> new CardScreen(gg, CardScreen.Kind.CREDITS, "Maraming salamat sa paglalaro.\n(Thank you for playing.)\n\nCELESTIAL GROUNDS"))
				.run(gg -> {
					gg.fadeTarget = 0;
					gg.fade = 0;
					gg.reset(new TitleScreen(gg));
				});
	}

	private static void spawnCelebration(Game g) {
		Overworld w = g.world;
		Entity carabao = Entity.still("carabao", Species.KALABAW.image(), 80, 80, 15, 5);
		w.spawn(carabao);
		Entity lola = Entity.person("lola_ride", "lola", 15, 5, "down");
		lola.drawOffsetY = -26;
		lola.solid = false;
		w.spawn(lola);
		w.spawn(Entity.person("dali", "dali", 17, 9, "left"));
		Entity goat = Entity.still("goat", Assets.cg("obj_goat.png"), 64, 64, 19, 9);
		goat.flipX = true;
		w.spawn(goat);
		w.spawn(Entity.person("tala", "tala", 12, 9, "right"));
		w.spawn(Entity.person("dona", "dona", 11, 9, "right"));
		w.spawn(Entity.person("gusmo", "gusmo", 23, 16, "down"));
		w.spawn(Entity.person("propesor", "propesor", 13, 11, "up"));
		w.spawn(Entity.person("villager1", "villager", 9, 12, "right"));
		w.spawn(Entity.person("villager2", "villager", 20, 12, "left"));
		w.spawn(Entity.person("tambay1", "tambay", 22, 17, "up"));
	}

	// =====================================================================================
	// Debug chapter select (F12) - for the team to test any scene quickly
	// =====================================================================================

	public static final String[] CHAPTERS = {
			"Scene 1: Outside the House", "Scene 4: Palayan Trail", "Scene 7: Kawayan Maze", "Scene 8: Tamaraw",
			"Scene 10: Volcano Trail", "Scene 11: Crater (Dona, Haribon)", "Scene 13: Dagat Tala", "Scene 14: Sunken Temple",
			"Scene 15: Butanding", "Scene 16: Tuktok ng Celestia" };

	public static void jump(Game g, int chapter, Species starter) {
		Story s = new Story();
		s.boy = g.story.boy;
		s.heroName = g.story.heroName;
		s.hasParol = true;
		g.story = s;
		String[] flags;
		String map;
		int[] at;
		switch (chapter) {
			case 0: map = "BARRIO"; at = P("BARRIO.start"); flags = new String[0]; break;
			case 1: map = "PALAYAN"; at = P("PALAYAN.from_barrio"); flags = f(1); break;
			case 2: map = "KAWAYAN"; at = P("KAWAYAN.entry"); flags = f(2); break;
			case 3: map = "TUKTOK"; at = P("TUKTOK.entry"); flags = f(3); break;
			case 4: map = "BULKAN"; at = P("BULKAN.arrive"); flags = f(4); break;
			case 5: map = "CRATER"; at = P("CRATER.entry"); flags = f(5); break;
			case 6: map = "DAGAT"; at = P("DAGAT.arrive"); flags = f(6); break;
			case 7: map = "TEMPLO"; at = P("TEMPLO.entry"); flags = f(7); break;
			case 8: map = "REEF"; at = P("REEF.entry"); flags = f(8); break;
			default: map = "CELESTIA"; at = P("CELESTIA.entry"); flags = f(9); break;
		}
		for (String fl : flags) s.set(fl);
		if (chapter > 0) {
			s.starter = starter;
			s.rival = starter == Species.TARSI ? Species.BINTURO : starter == Species.BINTURO ? Species.DUGONG : Species.TARSI;
			s.hasBanga = s.hasJournal = true;
			s.pandesal = 5;
			int[] lv = { 0, 4, 5, 5, 7, 7, 9, 9, 10, 12 };
			int L = lv[chapter];
			s.addCreature(new Creature(starter, L));
			if (chapter >= 2) s.addCreature(new Creature(Species.KALABAW, Math.max(3, L - 1)));
			if (chapter >= 4) {
				Species other = starter == Species.BINTURO ? Species.DUGONG : starter == Species.DUGONG ? Species.AHAS_BULKAN : Species.AHAS_BULKAN;
				s.addCreature(new Creature(other, L - 1));
			}
			if (chapter >= 6) s.addCreature(new Creature(Species.PUGITA, L - 1));
			if (chapter >= 2) { s.talaFollowing = true; }
			s.tapang = chapter >= 7 ? 3 : chapter >= 5 ? 3 : chapter >= 3 ? 2 : 1;
			s.hiyas = chapter >= 9 ? 3 : chapter >= 6 ? 2 : chapter >= 4 ? 1 : 0;
			if (chapter >= 8) { s.heroLight = true; s.tapang = 4; }
		}
		Overworld w = new Overworld(g);
		g.reset(w);
		if (s.has("babaylan")) w.maps.get("TEMPLO").setTile(16, 0, 'D');
		if (s.has("lava_bridge")) for (int x = 14; x <= 15; x++) { w.maps.get("BULKAN").setTile(x, 15, 'K'); w.maps.get("BULKAN").setTile(x, 16, 'K'); }
		w.loadMap(map, at[0], at[1], "up");
	}

	private static String[] f(int ch) {
		List<String> l = new ArrayList<>();
		String[][] steps = {
				{},
				{ "intro_done", "lola_out", "off_porch", "villager_warned", "dali_gate" },
				{ "palayan_intro", "kalabaw_tut", "tala_met", "gusmo_beaten", "tala_joined" },
				{ "kawayan_intro", "kawayan_dark" },
				{ "tamaraw_talk", "hiyas_lupa", "jeepney" },
				{ "bulkan_intro", "lava_note", "lava_bridge", "tunnel" },
				{ "dona_talk", "dona_beaten", "hiyas_apoy" },
				{ "dagat_intro" },
				{ "temple_dark", "banga_glow", "babaylan" },
				{ "butanding_talk", "hiyas_tubig" } };
		for (int i = 0; i <= ch && i < steps.length; i++) for (String x : steps[i]) l.add(x);
		return l.toArray(new String[0]);
	}
}
