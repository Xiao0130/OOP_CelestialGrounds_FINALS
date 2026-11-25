package celestial.story;

import celestial.Game;
import celestial.battle.Battle;
import celestial.battle.BattleHooks;
import celestial.battle.BattleSetup;
import celestial.data.Creature;
import celestial.data.Move;
import celestial.data.Species;
import celestial.ui.RhythmScreen;
import celestial.world.Theme;

/** The battles written out in the game script, with their tutorial pop-ups and lines. */
public final class BattleScripts {

	private BattleScripts() {}

	// ------------------------------------------------------------------ BATTLE 1: Dali (tutorial)
	public static BattleSetup dali(Game g) {
		Creature rival = new Creature(g.story.rival, 1);
		rival.hp = (int) Math.ceil(rival.maxHp() * 0.6);
		BattleSetup s = BattleSetup.trainer("Dali", "dali", Theme.BARRIO, rival);
		s.noExp = true;
		return s.hooks(new BattleHooks() {
			boolean healTip, almost;

			public boolean intro(Battle b) {
				b.msg("Dali wants to battle!");
				b.say("DALI", "Prepare to lose, in style!");
				b.msg("Dali sent out [RIVAL PARTNER]!");
				b.msg("Go, [STARTER]!");
				b.announce();
				b.tutorial("LABAN lets you choose a move. Try your signature move!");
				return true;
			}

			public String enemySkip(Battle b, int turn) {
				if (turn != 1) return null;
				b.msg("[RIVAL PARTNER] is busy posing. It didn't move!");
				b.say("DALI", "Wait, I wasn't ready! That's a power pose!");
				b.tutorial("ELEMENT TRIANGLE: Fire beats Earth. Earth beats Water. Water beats Fire. Super effective hits do 1.5x damage.");
				return "";
			}

			public Boolean enemyFirst(Battle b, int turn) { return turn == 2 ? Boolean.TRUE : null; }

			public int fixedDamage(Battle b, Creature a, Creature d, Move m, boolean byPlayer) {
				if (byPlayer) return (int) Math.ceil(d.maxHp() * 0.3);
				return Math.max(0, Math.min((int) (d.maxHp() * 0.4), d.hp - 1));
			}

			public boolean preventFaint(Battle b, Creature c) { return c == b.enemy && b.turn < 3; }

			public void afterHit(Battle b, Creature a, Creature d, boolean byPlayer) {
				if (byPlayer && !almost && d.hp > 0 && d.hpFrac() <= 0.35) {
					almost = true;
					b.msg("...but [RIVAL PARTNER] is almost done!");
				}
				if (!byPlayer && !healTip) {
					healTip = true;
					b.msg("[STARTER] lost some HP.");
					b.tutorial("Dali's element beats yours. Use a Pandesal to heal!");
				}
			}

			public void onWin(Battle b) {
				b.act(() -> {
					Creature st = g.story.starterCreature();
					if (st != null && st.level < 4) {
						int old = st.maxHp();
						st.level = 4;
						st.exp = 0;
						st.hp += st.maxHp() - old;
					}
				});
				b.msg("[STARTER] grew to Lv.4!");
			}

			public String hint(Battle b) { return "Dali's partner beats yours, so keep a Pandesal ready!"; }
		});
	}

	// ------------------------------------------------------------------ BATTLE 2: wild Kalabaw (befriending tutorial)
	public static BattleSetup kalabawTutorial(Game g) {
		Creature k = new Creature(Species.KALABAW, 2);
		BattleSetup s = BattleSetup.wild(k, Theme.PALAYAN);
		return s.hooks(new BattleHooks() {
			boolean hungry;

			public boolean intro(Battle b) {
				b.msg("A wild Kalabaw appeared!");
				b.msg("Go, [STARTER]!");
				b.announce();
				b.tutorial("Wild Hayop-Diwata can be befriended. Weaken it, offer a Pandesal, then use the Banga.");
				g.story.set("befriend_tip");
				return true;
			}

			public boolean preventFaint(Battle b, Creature c) { return c == b.enemy; }

			public void afterHit(Battle b, Creature a, Creature d, boolean byPlayer) {
				if (byPlayer && !hungry && d.hpFrac() < 0.5) {
					hungry = true;
					b.msg("Kalabaw is slowing down. It looks hungry.");
					b.tutorial("Kalabaw is under 50% HP. Choose PANDESAL and offer it to Kalabaw, then use the BANGA.");
				}
				if (!byPlayer && d.hp <= 1) b.msg("Careful! Feed [STARTER] a Pandesal if it gets tired.");
			}

			public String[] befriendLines(Battle b) {
				return new String[] { "Tok... Tok... Tok! Kalabaw stepped inside.", "Kalabaw joined your party!" };
			}
		});
	}

	// ------------------------------------------------------------------ BATTLE 3: Kuya Gusmo
	public static BattleSetup gusmo(Game g) {
		BattleSetup s = BattleSetup.trainer("Kuya Gusmo", "gusmo", Theme.PALAYAN, new Creature(Species.KALABAW, 3));
		return s.hooks(new BattleHooks() {
			boolean tip, louder, ayan;

			public boolean intro(Battle b) {
				b.msg("Kuya Gusmo wants to battle!");
				b.say("GUSMO", "Isang kanta lang, pre!");
				b.msg("Kuya Gusmo sent out Kalabaw!");
				b.msg("Go, [STARTER]!");
				b.announce();
				b.msg("A videoke jingle plays. Kalabaw is vibing.");
				return true;
			}

			public void afterHit(Battle b, Creature a, Creature d, boolean byPlayer) {
				if (byPlayer && !tip) {
					tip = true;
					b.tutorial("Effectiveness depends on your starter. Fire: 'It's super effective!' Earth: no message. Water: 'It's not very effective...'");
				}
				if (!byPlayer && !louder) {
					louder = true;
					b.say("GUSMO", "Louder! More bass!");
				}
				if (byPlayer && d.tiyagaUsed && !ayan) {
					ayan = true;
					b.say("GUSMO", "Ayan! That's my Kalabaw!");
				}
			}

			public String hint(Battle b) { return "Kalabaw is Earth. Fire beats Earth! Watch out, its Tiyaga lets it hang on with 1 HP once."; }
		});
	}

	// ------------------------------------------------------------------ wild Tarsi in the Kawayan Maze
	public static BattleSetup wildTarsi(Game g) {
		BattleSetup s = BattleSetup.wild(new Creature(Species.TARSI, 3), Theme.KAWAYAN);
		return s.hooks(new BattleHooks() {
			public boolean intro(Battle b) {
				b.msg("A wild Tarsi appeared! It is staring at you without blinking.");
				b.msg("Go, " + b.ally.name() + "!");
				b.announce();
				b.tutorial("Tarsi's ability, MATA NG GABI: it shows the enemy's next move. Weaken it, offer a Pandesal, then use the Banga.");
				return true;
			}
		});
	}

	// ------------------------------------------------------------------ BATTLE 4: Tamaraw, Guardian of the Earth
	public static BattleSetup tamaraw(Game g) {
		BattleSetup s = BattleSetup.guardian("Tamaraw, Guardian of the Earth", new Creature(Species.TAMARAW, 5), Theme.TUKTOK);
		return s.hooks(new BattleHooks() {
			boolean tip, half, quarter;

			public boolean intro(Battle b) {
				b.msg("Tamaraw, Guardian of the Earth, wants to battle!");
				b.msg("Go, " + b.ally.name() + "!");
				b.announce();
				return true;
			}

			public void afterHit(Battle b, Creature a, Creature d, boolean byPlayer) {
				if (byPlayer && !tip) {
					tip = true;
					b.tutorial("Fire: 'It's super effective!' Water: 'It's not very effective...' Tala's hint: Fire is the best choice here.");
				}
				if (byPlayer && d.atkStage >= 1 && !half) {
					half = true;
					b.say("TAMARAW", "Hindi pa tapos!");
				}
				if (byPlayer && d.atkStage >= 2 && !quarter) {
					quarter = true;
					b.say("TAMARAW", "I will... not... back down!");
					b.say("TALA", "Finish it quickly, or offer a Pandesal to calm it!");
				}
			}

			public boolean onOffer(Battle b) {
				if (b.enemy.hpFrac() > 0.25) {
					b.msg("Tamaraw snorts. It's too angry to eat right now.");
					return true;
				}
				b.act(() -> g.story.pandesal--);
				b.msg("Tamaraw ate the Pandesal slowly. Its anger fades.");
				b.say("TAMARAW", "Hmph. Nobody has ever fed me in the middle of a fight.");
				b.calmWin();
				return true;
			}

			public String hint(Battle b) { return "Tamaraw gets stronger the weaker it feels. Fire is the best choice here. At 25 pct HP, a Pandesal can calm it!"; }
		});
	}

	// ------------------------------------------------------------------ BATTLE 5: Do\u00f1a Liwayway
	public static BattleSetup dona(Game g) {
		BattleSetup s = BattleSetup.trainer("Do\u00f1a Liwayway Ginhawa", "dona", Theme.CRATER,
				new Creature(Species.AHAS_BULKAN, 6), new Creature(Species.KALABAW, 6));
		return s.hooks(new BattleHooks() {
			public boolean intro(Battle b) {
				b.msg("Do\u00f1a Liwayway Ginhawa wants to battle!");
				b.say("DO\u00d1A", "I'm only keeping them safe.");
				b.msg("Do\u00f1a Liwayway Ginhawa sent out Ahas-Bulkan!");
				b.msg("Go, " + b.ally.name() + "!");
				b.announce();
				b.tutorial("Her team has two elements. Use Water on Ahas-Bulkan and Fire on Kalabaw.");
				return true;
			}

			public String hint(Battle b) {
				return b.enemy.species == Species.AHAS_BULKAN ? "Mama's Ahas-Bulkan is Fire. Use Water! Its burns stack, so don't let it hit you too often."
						: "Kalabaw is Earth. Use Fire! It can hang on with 1 HP once.";
			}
		});
	}

	// ------------------------------------------------------------------ BATTLE 6: Haribon, Guardian of the Fire
	public static BattleSetup haribon(Game g) {
		BattleSetup s = BattleSetup.guardian("Haribon, Guardian of the Fire", new Creature(Species.HARIBON, 8), Theme.CRATER);
		return s.hooks(new BattleHooks() {
			public boolean intro(Battle b) {
				b.msg("Haribon, Guardian of the Fire, wants to battle!");
				b.msg("Go, " + b.ally.name() + "!");
				b.announce();
				b.tutorial("Haribon always acts first. Water is super effective. Use Pandesal to recover between hits.");
				b.say("TALA", "It strikes first every turn, so plan around it!");
				return true;
			}

			public String hint(Battle b) { return "It strikes first every turn, so plan around it! Water beats Fire."; }
		});
	}

	// ------------------------------------------------------------------ BATTLE 7: Butanding, Guardian of the Water
	public static BattleSetup butanding(Game g) {
		BattleSetup s = BattleSetup.guardian("Butanding, Guardian of the Water", new Creature(Species.BUTANDING, 10), Theme.REEF);
		return s.hooks(new BattleHooks() {
			public boolean intro(Battle b) {
				b.msg("Butanding, Guardian of the Water, wants to battle!");
				b.msg("Go, " + b.ally.name() + "!");
				b.announce();
				b.tutorial("Butanding heals every turn. Earth is super effective. Hit hard, and finish quickly!");
				b.say("TALA", "It heals itself! Don't waste turns!");
				return true;
			}

			public String hint(Battle b) { return "It heals itself! Don't waste turns! Earth beats Water."; }
		});
	}

	// ------------------------------------------------------------------ BATTLE 8: Ang Bakunawa
	public static BattleSetup bakunawa(Game g) {
		Creature k = Creature.boss(Species.BAKUNAWA, 12);
		BattleSetup s = new BattleSetup(BattleSetup.Kind.FINAL, "Ang Bakunawa", Theme.CELESTIA, k);
		return s.hooks(new BattleHooks() {
			public boolean intro(Battle b) {
				b.msg("Ang Bakunawa rises from the sea!");
				b.msg("Go, " + b.ally.name() + "!");
				b.announce();
				b.tutorial("PHASE 1: Water form. Weak to Earth. When its tidal wave comes, press the arrow shown to dodge!");
				return true;
			}

			public String hint(Battle b) {
				switch (b.phase) {
					case 1: return "It's in Water form. Earth beats Water! Dodge the waves with the arrow keys.";
					case 2: return "Fire form now! Water beats Fire. Its moonbeams hit everyone, so heal with Pandesal.";
					default: return "Stone armor! Only Fire hits that are super effective can crack it.";
				}
			}

			public void finale(Battle b) {
				b.msg("The armor cracks. The Bakunawa roars. Bakunawa's LUNOK-LIWANAG! Every light goes out!");
				b.act(() -> b.blackout = true);
				b.say("TALA", "I can't see! I can't see anything!");
				b.say("PLAYER", "I'm scared too. I'm still scared.");
				b.say("PLAYER", "But I'm not alone. Listen. Find the beat.");
				b.act(() -> b.heroGlow = 0.02f);
				b.msg("A small, warm light appears in the player's hands. It is not the Hiyas. It is not the parol. It is the player's own light.");
				b.tutorial("Tap the beat 8 times. Each party member adds a sound and a glow: stomp, hiss, splash, and song.");
				b.screen(() -> new RhythmScreen(g));
				b.msg("Tok... Tok... Tok! Everyone lends their light!");
				b.say("PLAYER", "(to the Bakunawa) I'm afraid of the dark too. It's scary when you're alone in it. But you're not alone anymore.");
				b.msg("The Bakunawa's light grows soft and warm.");
				b.say("BAKUNAWA", "...Ang init. So warm. Salamat.");
				b.msg("Six moons rise from the sea and join the last one.");
			}
		});
	}
}
