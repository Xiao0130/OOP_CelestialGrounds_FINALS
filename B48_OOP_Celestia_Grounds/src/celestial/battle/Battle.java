package celestial.battle;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Sound;
import celestial.Text;
import celestial.data.Ability;
import celestial.data.Creature;
import celestial.data.Element;
import celestial.data.Move;
import celestial.data.Species;
import celestial.story.Story;
import celestial.ui.Popup;

/**
 * Turn-based battle following the script's rules:
 * LABAN (fight) | PANDESAL (heal or calm) | BANGA (befriend) | PALIT (switch) | TAKBO (run) | HINT (Tala).
 * Fire beats Earth, Earth beats Water, Water beats Fire (1.5x / 0.5x).
 */
public class Battle extends Screen {

	public enum Result { WIN, LOSE, RAN, BEFRIENDED, FLED }

	private enum Mode { QUEUE, MENU, MOVES, PANDESAL, SWITCH, FORCED_SWITCH, END }

	/** One queued battle beat: a message, an animation, an action. */
	public interface Step {
		default void start() {}

		boolean update();
	}

	private static final Font MSG = Assets.font(19f);
	private static final Font MENU = Assets.font(18f);
	private static final Font SMALL = Assets.font(14f);
	private static final String[] MAIN = { "LABAN", "PANDESAL", "BANGA", "PALIT", "TAKBO", "HINT" };

	public final BattleSetup setup;
	public final Story story;
	public Creature ally, enemy;
	public int enemyIndex;
	public int turn = 1;
	private Result result;
	private final Random rng = new Random();

	private Mode mode = Mode.QUEUE;
	private int sel;
	private final LinkedList<Step> queue = new LinkedList<>();
	private Step current;
	private List<Step> collecting;
	private int menuPreparedTurn = 0;
	private Move enemyPlanned;
	private String pendingPlayerAction;  // for building the turn

	// display
	private String message = "", speaker;
	private int shown, msgAge;
	private double allyHpShown, enemyHpShown;
	private int allyLunge, enemyLunge, allyFlash, enemyFlash, allyFaint, enemyFaint;
	private boolean enemyHidden, allyHidden;
	private int bangaShake = -1;
	private int sparkle;
	private int introSlide = 60;

	// final boss
	public int phase = 1;
	public int armor;
	public boolean blackout;
	public float heroGlow;
	private int qteTimer = -1;
	private String qteDir;
	private double qteMult = 1;
	private boolean qteDone;

	public Battle(Game game, BattleSetup setup) {
		super(game);
		this.setup = setup;
		this.story = game.story;
	}

	@Override
	public boolean opaque() { return true; }

	public Result result() { return result; }

	public boolean isWild() { return setup.kind == BattleSetup.Kind.WILD; }

	// ------------------------------------------------------------------ queue API (used by hooks)
	private void push(Step s) {
		if (collecting != null) collecting.add(s);
		else queue.add(s);
	}

	public void msg(String text) {
		final String t = story.sub(text);
		push(new Step() {
			public void start() { showMessage(t, null); }

			public boolean update() { return messageDone(false); }
		});
	}

	public void say(String who, String text) {
		final String t = story.sub(text);
		final String w = "PLAYER".equals(who) ? story.heroName.toUpperCase() : who;
		push(new Step() {
			public void start() { showMessage(t, w); }

			public boolean update() { return messageDone(true); }
		});
	}

	public void popup(String title, String text) {
		screen(() -> new Popup(game, title, text));
	}

	public void tutorial(String text) { popup("TUTORIAL", text); }

	public void act(Runnable r) {
		push(() -> {
			r.run();
			return true;
		});
	}

	public void wait(int frames) {
		push(new Step() {
			int t;

			public boolean update() { return ++t >= frames; }
		});
	}

	public void screen(Supplier<Screen> make) {
		push(new Step() {
			Screen s;

			public void start() {
				s = make.get();
				game.push(s);
			}

			public boolean update() { return s.isClosed(); }
		});
	}

	private void waitBars() {
		push(() -> Math.abs(allyHpShown - Math.max(0, ally.hp)) < 0.5 && Math.abs(enemyHpShown - Math.max(0, enemy.hp)) < 0.5);
	}

	private void showMessage(String t, String who) {
		message = t;
		speaker = who;
		shown = 0;
		msgAge = 0;
	}

	private boolean messageDone(boolean needConfirm) {
		msgAge++;
		if (shown < message.length()) {
			shown = Math.min(message.length(), shown + 2);
			if (game.input.pressed(Key.CONFIRM)) {
				shown = message.length();
				game.input.consume();
			}
			return false;
		}
		if (game.input.pressed(Key.CONFIRM)) {
			game.input.consume();
			return true;
		}
		return !needConfirm && msgAge > message.length() / 2 + 70;
	}

	// ------------------------------------------------------------------ setup
	@Override
	public void enter() {
		Sound.music("battle");
		for (Creature c : story.party) c.resetBattleState();
		for (Creature c : setup.enemies) c.resetBattleState();
		enemy = setup.enemies.get(0);
		ally = story.firstAlive();
		allyHpShown = ally.hp;
		enemyHpShown = enemy.hp;
		story.seen.add(enemy.species);
		if (setup.kind == BattleSetup.Kind.FINAL) setPhase(1, false);
		if (!setup.hooks.intro(this)) {
			defaultIntro();
			announce();
		}
	}

	/** "Haribon's HARI NG HIMPAPAWID!" / "Tarsi's MATA NG GABI!" when they enter. */
	public void announce() {
		announceEntry(enemy, false);
		announceEntry(ally, true);
	}

	public void defaultIntro() {
		switch (setup.kind) {
			case WILD: msg("A wild " + enemy.name() + " appeared!"); break;
			case TRAINER:
				msg(setup.trainer + " wants to battle!");
				msg(setup.trainer + " sent out " + enemy.name() + "!");
				break;
			default: msg(setup.trainer + " wants to battle!"); break;
		}
		msg("Go, " + ally.name() + "!");
	}

	private void announceEntry(Creature c, boolean mine) {
		if (c.species.ability == Ability.HARI_NG_HIMPAPAWID)
			msg(c.name() + "'s HARI NG HIMPAPAWID! It moves first!");
		if (mine && c.species.ability == Ability.MATA_NG_GABI)
			msg(c.name() + "'s MATA NG GABI! It can see the enemy's next move.");
	}

	// ------------------------------------------------------------------ loop
	@Override
	public void update() {
		if (introSlide > 0) introSlide -= 3;
		tweenBars();
		if (allyLunge > 0) allyLunge--;
		if (enemyLunge > 0) enemyLunge--;
		if (allyFlash > 0) allyFlash--;
		if (enemyFlash > 0) enemyFlash--;
		if (sparkle > 0) sparkle--;
		if (allyFaint > 0 && allyFaint < 30) allyFaint++;
		if (enemyFaint > 0 && enemyFaint < 30) enemyFaint++;
		if (bangaShake >= 0) bangaShake++;
		if (blackout && heroGlow > 0 && heroGlow < 1) heroGlow = Math.min(1, heroGlow + 0.01f);

		switch (mode) {
			case QUEUE: runQueue(); break;
			case MENU: menuInput(); break;
			case MOVES: movesInput(); break;
			case PANDESAL: pandesalInput(); break;
			case SWITCH: case FORCED_SWITCH: switchInput(); break;
			case END: close(); Sound.music("town"); break;
		}
	}

	private void tweenBars() {
		double at = Math.max(0, ally.hp), et = Math.max(0, enemy.hp);
		allyHpShown += Math.signum(at - allyHpShown) * Math.min(Math.abs(at - allyHpShown), Math.max(0.4, ally.maxHp() / 60.0));
		enemyHpShown += Math.signum(et - enemyHpShown) * Math.min(Math.abs(et - enemyHpShown), Math.max(0.4, enemy.maxHp() / 60.0));
	}

	private void runQueue() {
		int guard = 0;
		while (guard++ < 50) {
			if (current == null) {
				if (queue.isEmpty()) {
					if (result != null) { mode = Mode.END; return; }
					prepareMenu();
					return;
				}
				current = queue.poll();
				collecting = new ArrayList<>();
				current.start();
				queue.addAll(0, collecting);
				collecting = null;
			}
			collecting = new ArrayList<>();
			boolean done = current.update();
			queue.addAll(0, collecting);
			collecting = null;
			if (!done) return;
			current = null;
			if (mode != Mode.QUEUE) return;
		}
	}

	private void prepareMenu() {
		if (menuPreparedTurn != turn) {
			menuPreparedTurn = turn;
			enemyPlanned = chooseEnemyMove();
			setup.hooks.beforeMenu(this, turn);
			if (!queue.isEmpty()) return;
		}
		mode = Mode.MENU;
		message = "What will " + ally.name() + " do?";
		speaker = null;
		shown = message.length();
	}

	// ------------------------------------------------------------------ menus
	private boolean hintAvailable() { return story.has("tala_joined"); }

	private void menuInput() {
		int cols = 3, n = hintAvailable() ? 6 : 5;
		if (game.input.pressed(Key.LEFT) && sel % cols > 0) { sel--; Sound.blip(); }
		if (game.input.pressed(Key.RIGHT) && sel % cols < cols - 1 && sel + 1 < n) { sel++; Sound.blip(); }
		if (game.input.pressed(Key.UP) && sel >= cols) { sel -= cols; Sound.blip(); }
		if (game.input.pressed(Key.DOWN) && sel + cols < n) { sel += cols; Sound.blip(); }
		if (!game.input.pressed(Key.CONFIRM)) return;
		Sound.tok();
		switch (sel) {
			case 0: mode = Mode.MOVES; sel = 0; break;
			case 1:
				if (story.pandesal <= 0) { mode = Mode.QUEUE; msg("You're out of Pandesal!"); sel = 1; }
				else { mode = Mode.PANDESAL; sel = 0; }
				break;
			case 2: mode = Mode.QUEUE; doBanga(); sel = 0; break;
			case 3: mode = Mode.SWITCH; sel = 0; break;
			case 4: mode = Mode.QUEUE; doRun(); sel = 0; break;
			case 5:
				mode = Mode.QUEUE;
				String h = setup.hooks.hint(this);
				say("TALA", h != null ? h : genericHint());
				sel = 0;
				break;
			default:
		}
	}

	private void movesInput() {
		Move[] ms = ally.moves();
		int n = ms.length + 1;
		if (game.input.pressed(Key.UP) || game.input.pressed(Key.LEFT)) { sel = (sel + n - 1) % n; Sound.blip(); }
		if (game.input.pressed(Key.DOWN) || game.input.pressed(Key.RIGHT)) { sel = (sel + 1) % n; Sound.blip(); }
		if (game.input.pressed(Key.CANCEL)) { mode = Mode.MENU; sel = 0; return; }
		if (game.input.pressed(Key.CONFIRM)) {
			Sound.tok();
			if (sel == ms.length) { mode = Mode.MENU; sel = 0; return; }
			Move m = ms[sel];
			mode = Mode.QUEUE;
			sel = 0;
			takeTurn(() -> attack(ally, enemy, m, true), true);
		}
	}

	private List<String> pandesalOptions() {
		List<String> o = new ArrayList<>();
		o.add("Feed " + ally.name());
		if (isWild() || (setup.kind == BattleSetup.Kind.GUARDIAN)) o.add("Offer to " + enemy.name());
		o.add("Back");
		return o;
	}

	private void pandesalInput() {
		List<String> o = pandesalOptions();
		if (game.input.pressed(Key.UP)) { sel = (sel + o.size() - 1) % o.size(); Sound.blip(); }
		if (game.input.pressed(Key.DOWN)) { sel = (sel + 1) % o.size(); Sound.blip(); }
		if (game.input.pressed(Key.CANCEL)) { mode = Mode.MENU; sel = 1; return; }
		if (!game.input.pressed(Key.CONFIRM)) return;
		Sound.tok();
		String pick = o.get(sel);
		mode = Mode.QUEUE;
		sel = 0;
		if (pick.startsWith("Feed")) {
			if (ally.hp >= ally.maxHp()) { msg(ally.name() + " is already full!"); return; }
			takeTurn(() -> {
				story.pandesal--;
				ally.heal(Math.max(12, ally.maxHp() / 2));
				msg(ally.name() + " ate the Pandesal. It looks happier! HP restored.");
				waitBars();
			}, false);
		} else if (pick.startsWith("Offer")) {
			offerToEnemy();
		} else {
			mode = Mode.MENU;
			sel = 1;
		}
	}

	private void offerToEnemy() {
		if (setup.hooks.onOffer(this)) return;
		if (!isWild()) {
			msg(enemy.name() + " isn't interested in food right now.");
			return;
		}
		takeTurn(() -> {
			story.pandesal--;
			enemy.fed = true;
			String[] lines = setup.hooks.offerLines(this);
			if (lines == null) {
				if (enemy.species == Species.TARSI) lines = new String[] { "Tarsi nibbled the Pandesal and blinked. Once." };
				else if (enemy.species == Species.KALABAW) lines = new String[] { "You offered a Pandesal. Kalabaw sniffed it and chewed it slowly.", "Kalabaw trusts you a little more." };
				else if (enemy.species == Species.DUGONG) {
					lines = new String[] { "Dugong sang a soft kundiman. Everyone feels better." };
					for (Creature c : story.party) if (!c.fainted()) c.heal(c.maxHp() / 5);
				} else lines = new String[] { enemy.name() + " ate the Pandesal. It looks happier!" };
			}
			for (String l : lines) msg(l);
		}, false);
	}

	private List<Creature> switchable() {
		List<Creature> list = new ArrayList<>();
		for (Creature c : story.party) list.add(c);
		return list;
	}

	private void switchInput() {
		List<Creature> list = switchable();
		int n = list.size() + (mode == Mode.SWITCH ? 1 : 0);
		if (game.input.pressed(Key.UP)) { sel = (sel + n - 1) % n; Sound.blip(); }
		if (game.input.pressed(Key.DOWN)) { sel = (sel + 1) % n; Sound.blip(); }
		if (mode == Mode.SWITCH && game.input.pressed(Key.CANCEL)) { mode = Mode.MENU; sel = 3; return; }
		if (!game.input.pressed(Key.CONFIRM)) return;
		if (sel == list.size()) { mode = Mode.MENU; sel = 3; return; }
		Creature pick = list.get(sel);
		if (pick.fainted()) { Sound.tone(200, 120, 0, 0.3, Sound.Wave.SINE); return; }
		if (pick == ally) { Sound.tone(200, 120, 0, 0.3, Sound.Wave.SINE); return; }
		Sound.tok();
		boolean forced = mode == Mode.FORCED_SWITCH;
		mode = Mode.QUEUE;
		sel = 0;
		if (forced) {
			sendOut(pick);
		} else {
			takeTurn(() -> {
				msg("Come back, " + ally.name() + "!");
				act(() -> allyHidden = true);
				wait(12);
				sendOut(pick);
			}, false);
		}
	}

	private void sendOut(Creature c) {
		act(() -> {
			ally = c;
			allyHpShown = c.hp;
			allyHidden = false;
			allyFaint = 0;
		});
		msg("Go, " + c.name() + "!");
		if (c.species.ability == Ability.MATA_NG_GABI) msg(c.name() + "'s MATA NG GABI! It can see the enemy's next move.");
	}

	// ------------------------------------------------------------------ turn building
	/** Queues the player's action and the enemy's, in speed order, then end-of-turn effects. */
	private void takeTurn(Runnable playerAction, boolean isAttack) {
		boolean enemyFirst = false;
		Boolean forced = setup.hooks.enemyFirst(this, turn);
		if (forced != null) {
			enemyFirst = forced;
		} else if (isAttack) {
			boolean enemyPriority = enemy.species.ability == Ability.HARI_NG_HIMPAPAWID;
			boolean allyPriority = ally.species.ability == Ability.HARI_NG_HIMPAPAWID;
			if (enemyPriority && !allyPriority) enemyFirst = true;
			else if (allyPriority == enemyPriority && enemy.spd() > ally.spd()) enemyFirst = true;
		}
		final Creature myAlly = ally;
		final Creature myEnemy = enemy;
		if (enemyFirst) {
			act(this::enemyAction);
			act(() -> {
				if (result == null && ally == myAlly && !ally.fainted()) playerAction.run();
			});
		} else {
			act(playerAction);
			act(() -> {
				if (result == null && enemy == myEnemy && !enemy.fainted() && !ally.fainted()) enemyAction();
			});
		}
		act(this::endOfTurn);
	}

	private void enemyAction() {
		if (result != null || enemy.fainted()) return;
		String skip = setup.hooks.enemySkip(this, turn);
		if (skip != null) {
			if (!skip.isEmpty()) msg(skip);
			return;
		}
		if (isWild() && enemy.fed && rng.nextDouble() < 0.5) {
			msg(enemy.name() + " is watching you curiously.");
			return;
		}
		Move m = enemyPlanned != null ? enemyPlanned : chooseEnemyMove();
		attack(enemy, ally, m, false);
	}

	private Move chooseEnemyMove() {
		if (setup.kind == BattleSetup.Kind.FINAL) {
			return phase == 1 ? Move.ALON_NG_GUTOM : phase == 2 ? Move.APOY_NG_LUNGKOT : Move.BATO_NG_PAGKALIMOT;
		}
		Move sig = enemy.species.signature;
		if (ally != null && sig.element.against(ally.element()) < 1 && rng.nextDouble() < 0.5) return Move.TULAK;
		double sigChance = setup.kind == BattleSetup.Kind.WILD ? 0.7 : setup.kind == BattleSetup.Kind.TRAINER ? 0.7 : 0.6;
		return rng.nextDouble() < sigChance ? sig : Move.TULAK;
	}

	// ------------------------------------------------------------------ attacking
	private void attack(Creature atk, Creature def, Move m, boolean byPlayer) {
		if (atk.fainted() || result != null) return;
		// Binturo's Amoy Popcorn distracts whoever faces it
		if (def.species.ability == Ability.AMOY_POPCORN && rng.nextDouble() < 0.2) {
			msg(def.name() + "'s AMOY POPCORN! " + atk.name() + " is distracted by the smell and loses its turn!");
			return;
		}
		msg(atk.name() + " used " + m.label + "!");
		act(() -> {
			if (byPlayer) allyLunge = 12; else enemyLunge = 12;
			Sound.hit();
		});
		wait(10);
		double accMult = 1 - 0.12 * atk.accStage;
		if (rng.nextInt(100) >= m.accuracy * accMult) {
			msg(atk.name() + "'s attack missed!");
			return;
		}
		if (!byPlayer && m == Move.ALON_NG_GUTOM) {
			msg("Bakunawa's ALON NG GUTOM! A wave of hunger crashes down!");
			startQte();
		}
		if (!byPlayer && m == Move.APOY_NG_LUNGKOT) msg("Bakunawa's APOY NG LUNGKOT! The sky burns with sorrow!");
		act(() -> applyHit(atk, def, m, byPlayer));
	}

	private int damage(Creature atk, Creature def, Move m, boolean byPlayer, double eff) {
		int fixed = setup.hooks.fixedDamage(this, atk, def, m, byPlayer);
		if (fixed >= 0) return fixed;
		double base = ((2.0 * atk.level / 5 + 2) * m.power * atk.atk() / (double) def.def()) / 50.0 + 2;
		double rand = 0.85 + rng.nextDouble() * 0.15;
		double dmg = base * eff * rand * setup.hooks.damageScale(this, atk, def, m, byPlayer);
		return Math.max(1, (int) Math.round(dmg));
	}

	private void applyHit(Creature atk, Creature def, Move m, boolean byPlayer) {
		double eff = m.element.against(def.element());
		int dmg = damage(atk, def, m, byPlayer, eff);
		boolean armored = setup.kind == BattleSetup.Kind.FINAL && byPlayer && phase == 3 && armor > 0;
		if (armored && eff <= 1) dmg = Math.max(1, (int) (dmg * 0.55));
		if (!byPlayer && qteDone) {
			dmg = Math.max(1, (int) (dmg * qteMult));
			qteDone = false;
		}
		// Tiyaga: survive one lethal hit with 1 HP
		boolean tiyaga = false;
		if (dmg >= def.hp && def.hp > 1 && def.species.ability == Ability.TIYAGA && !def.tiyagaUsed) {
			def.tiyagaUsed = true;
			dmg = def.hp - 1;
			tiyaga = true;
		}
		if (dmg >= def.hp && setup.hooks.preventFaint(this, def)) dmg = Math.max(0, def.hp - 1);
		def.hp = Math.max(0, def.hp - dmg);
		if (byPlayer) enemyFlash = 18; else allyFlash = 18;
		waitBars();
		if (eff > 1) msg("It's super effective!");
		else if (eff < 1) msg("It's not very effective...");
		if (tiyaga) msg(def.name() + "'s TIYAGA! It held on with 1 HP!");
		if (armored) {
			if (eff > 1) {
				armor--;
				msg(armor > 0 ? "The stone armor cracked! (" + armor + " left)" : "The stone armor shatters!");
			} else {
				msg("The stone armor absorbed part of the hit!");
			}
		}
		// Bakunawa's sorrow burns the whole team
		if (!byPlayer && m == Move.APOY_NG_LUNGKOT) {
			boolean any = false;
			for (Creature c : story.party) {
				if (c != def && !c.fainted()) {
					c.hp = Math.max(1, c.hp - Math.max(1, dmg / 6));
					any = true;
				}
			}
			if (any) msg("Burning moonbeams fall on your whole team!");
		}
		if (atk.species.ability == Ability.LASON_ALAB && !def.fainted()) {
			def.burnStacks++;
			msg(def.burnStacks == 1 ? def.name() + " was burned! (LASON-ALAB)" : "LASON-ALAB! " + def.name() + "'s burn grows stronger!");
		}
		if (atk.species.ability == Ability.TINTA && !def.fainted() && def.accStage < 3) {
			def.accStage++;
			msg(atk.name() + "'s TINTA! " + def.name() + "'s accuracy fell!");
		}
		if (def.species.ability == Ability.HINDI_AATRAS && !def.fainted()) {
			if (def.hpFrac() <= 0.25 && def.atkStage < 2) {
				def.atkStage = 2;
				msg(def.name() + "'s HINDI AATRAS! Its Attack rose sharply!");
			} else if (def.hpFrac() <= 0.5 && def.atkStage < 1) {
				def.atkStage = 1;
				msg(def.name() + "'s HINDI AATRAS! Its Attack rose!");
			}
		}
		setup.hooks.afterHit(this, atk, def, byPlayer);
		// shy Dugong flees when attacked
		if (byPlayer && isWild() && def.species == Species.DUGONG && !def.fainted()) {
			msg("Dugong dove under the waves and swam away!");
			act(() -> { enemyHidden = true; result = Result.FLED; });
			return;
		}
		act(() -> checkFaint(def));
	}

	// ------------------------------------------------------------------ fainting / phases
	private void checkFaint(Creature c) {
		if (!c.fainted() || result != null) return;
		if (c == enemy) {
			if (setup.kind == BattleSetup.Kind.FINAL) { nextPhase(); return; }
			String owner = setup.kind == BattleSetup.Kind.TRAINER ? possessive(setup.trainer) + enemy.name() : enemy.name();
			msg(owner + " is too tired to battle!");
			act(() -> enemyFaint = 1);
			wait(24);
			if (!setup.noExp) giveExp();
			if (enemyIndex + 1 < setup.enemies.size()) {
				act(() -> {
					enemyIndex++;
					enemy = setup.enemies.get(enemyIndex);
					enemyHpShown = enemy.hp;
					enemyFaint = 0;
					story.seen.add(enemy.species);
				});
				act(() -> msg(setup.trainer + " sent out " + enemy.name() + "!"));
			} else {
				win();
			}
		} else {
			msg(c.name() + " is too tired to battle!");
			act(() -> allyFaint = 1);
			wait(20);
			act(() -> {
				if (story.firstAlive() == null) {
					msg("Your team is too tired to go on...");
					act(() -> result = Result.LOSE);
				} else {
					mode = Mode.FORCED_SWITCH;
					message = "Who will you send out?";
					speaker = null;
					shown = message.length();
					sel = 0;
				}
			});
		}
	}

	private static String possessive(String name) {
		return name + "'s ";
	}

	public void win() {
		if (setup.kind != BattleSetup.Kind.WILD) msg("You won the battle!");
		setup.hooks.onWin(this);
		act(() -> { if (result == null) result = Result.WIN; });
	}

	/** Forces a win (Tamaraw calmed with Pandesal). */
	public void calmWin() {
		act(() -> enemyHidden = false);
		if (!setup.noExp) giveExp();
		win();
	}

	private void giveExp() {
		act(() -> {
			int per = setup.kind == BattleSetup.Kind.WILD ? 8 : setup.kind == BattleSetup.Kind.TRAINER ? 10 : 12;
			int exp = per * enemy.level;
			int top = story.maxPartyLevel();
			msg("Your team gained " + exp + " EXP.");
			for (Creature c : story.party) {
				if (c.fainted()) continue;
				int gain = c.level < top - 1 ? exp * 2 : exp;
				int lv = c.gainExp(gain);
				if (lv > 0) msg(c.name() + " grew to Lv." + c.level + "!");
			}
		});
	}

	private void setPhase(int p, boolean announce) {
		phase = p;
		Element[] els = { Element.TUBIG, Element.APOY, Element.LUPA };
		String[] spr = { "bakunawa_tubig", "bakunawa_apoy", "bakunawa_lupa" };
		enemy.elementOverride = els[p - 1];
		enemy.spriteOverride = spr[p - 1];
		enemy.hp = enemy.maxHp();
		enemyHpShown = enemy.hp;
		enemy.burnStacks = 0;
	}

	private void nextPhase() {
		if (phase < 3) {
			act(() -> { for (Creature c : story.party) if (!c.fainted()) c.heal(c.maxHp() * 3 / 10); });
			msg("The three Hiyas pulse with light. Your team feels a little stronger.");
			waitBars();
		}
		if (phase == 1) {
			msg("The Bakunawa's scales turn from sea-blue to burning red!");
			act(() -> { setPhase(2, true); enemyFlash = 30; });
			tutorial("PHASE 2: Fire form. Weak to Water. Survive the burning moonbeams! They hit your whole team.");
		} else if (phase == 2) {
			msg("The flames cool into stone...");
			act(() -> { setPhase(3, true); armor = 2; enemyFlash = 30; });
			msg("Bakunawa's BATO NG PAGKALIMOT! Ancient stone armor rises!");
			tutorial("PHASE 3: Earth form. Weak to Fire. Break its stone armor with super-effective hits!");
		} else {
			act(() -> enemy.hp = 0);
			setup.hooks.finale(this);
			act(() -> { if (result == null) result = Result.WIN; });
		}
	}

	private void endOfTurn() {
		if (result != null) return;
		for (Creature c : new Creature[] { ally, enemy }) {
			if (c.burnStacks > 0 && !c.fainted()) {
				int d = Math.max(1, (int) Math.round(c.maxHp() * 0.04 * c.burnStacks));
				if (d >= c.hp && c == enemy && setup.hooks.preventFaint(this, c)) d = c.hp - 1;
				c.hp = Math.max(0, c.hp - d);
				msg(c.name() + " is hurt by its burn!");
				waitBars();
				final Creature cc = c;
				act(() -> checkFaint(cc));
			}
		}
		act(() -> {
			if (result != null) return;
			if (ally.species.ability == Ability.KUNDIMAN && !ally.fainted()) {
				boolean healed = false;
				for (Creature c : story.party) if (!c.fainted() && c.hp < c.maxHp()) { c.heal(Math.max(1, c.maxHp() / 14)); healed = true; }
				if (healed) { msg(ally.name() + "'s KUNDIMAN! Its team feels a little better."); waitBars(); }
			}
			if (enemy.species.ability == Ability.KUNDIMAN && !enemy.fainted() && enemy.hp < enemy.maxHp()) {
				enemy.heal(Math.max(1, enemy.maxHp() / 14));
				msg(enemy.name() + "'s KUNDIMAN! It feels a little better.");
				waitBars();
			}
			if (enemy.species.ability == Ability.BITUIN_BATIK && !enemy.fainted() && enemy.hp < enemy.maxHp()) {
				enemy.heal(Math.max(1, enemy.maxHp() / 16));
				msg(enemy.name() + "'s BITUIN-BATIK! It recovered some HP.");
				waitBars();
			}
		});
		act(() -> turn++);
	}

	// ------------------------------------------------------------------ banga / run
	private void doBanga() {
		if (setup.kind != BattleSetup.Kind.WILD) {
			msg(setup.kind == BattleSetup.Kind.TRAINER ? "That Hayop-Diwata already has a friend. You can't befriend it."
					: "A Guardian can't be befriended.");
			return;
		}
		boolean ready = enemy.fed && (enemy.hpFrac() < 0.5 || enemy.species == Species.DUGONG);
		boolean accepts = ready && (!enemy.species.rare || rng.nextDouble() < 0.5);
		takeTurn(() -> {
			msg("You held out the Banga...");
			act(() -> { bangaShake = 0; enemyHidden = true; Sound.tok(); });
			wait(26);
			act(Sound::tok);
			wait(26);
			act(Sound::tok);
			wait(26);
			if (accepts) {
				act(() -> { sparkle = 60; bangaShake = -1; });
				String[] lines = setup.hooks.befriendLines(this);
				if (lines == null) lines = new String[] { "Tok... Tok... Tok! " + enemy.name() + " joined your party!" };
				for (String l : lines) msg(l);
				act(() -> {
					boolean inParty = story.addCreature(enemy);
					if (!inParty) msg(enemy.name() + " will rest in the Banga for now. Press P to change your team.");
				});
				if (!setup.noExp) giveExp();
				act(() -> result = Result.BEFRIENDED);
			} else {
				act(() -> { bangaShake = -1; enemyHidden = false; });
				msg(enemy.name() + " sniffed the Banga and stepped back. Not ready yet.");
				if (!ready && story.has("tala_joined")) {
					say("TALA", enemy.fed ? "Weaken it below half its HP first, then try the Banga again!"
							: "Offer it a Pandesal first, so it trusts you!");
				}
			}
		}, false);
	}

	private void doRun() {
		if (isWild()) {
			msg("You got away safely!");
			act(() -> result = Result.RAN);
		} else {
			msg("There's no running from this battle!");
		}
	}

	private String genericHint() {
		Element e = enemy.element();
		if (e == Element.WALA) return "Just keep going. We're with you!";
		Element w = e.weakness();
		Creature best = story.partyMember(w);
		String s = enemy.name() + " is " + e.english + ". " + w.english + " beats " + e.english + "!";
		if (best != null && best != ally) s += " Switch to " + best.name() + "!";
		else if (best == ally) s += " Use " + ally.species.signature.label + "!";
		if (isWild()) s += " To befriend it: weaken it, offer a Pandesal, then use the Banga.";
		return s;
	}

	// ------------------------------------------------------------------ QTE (dodge the tidal wave)
	private void startQte() {
		push(new Step() {
			public void start() {
				qteDir = rng.nextBoolean() ? "left" : "right";
				qteTimer = 70;
				qteMult = 1;
			}

			public boolean update() {
				qteTimer--;
				boolean hit = (qteDir.equals("left") && game.input.pressed(Key.LEFT)) || (qteDir.equals("right") && game.input.pressed(Key.RIGHT));
				boolean wrong = (qteDir.equals("left") && game.input.pressed(Key.RIGHT)) || (qteDir.equals("right") && game.input.pressed(Key.LEFT));
				if (hit) {
					qteMult = 0.35;
					qteDone = true;
					qteTimer = -1;
					Sound.tone(990, 120, qteDir.equals("left") ? -0.6 : 0.6, 0.3, Sound.Wave.SINE);
					msg("You dodged the wave! " + ally.name() + " only got splashed.");
					return true;
				}
				if (wrong || qteTimer <= 0) {
					qteMult = 1;
					qteDone = true;
					qteTimer = -1;
					return true;
				}
				return false;
			}
		});
	}

	// ------------------------------------------------------------------ read-only state (used by the test bot)
	public String modeName() { return mode.name(); }

	public int selection() { return sel; }

	public String qteDirection() { return qteTimer > 0 ? qteDir : null; }

	public List<String> pandesalChoices() { return pandesalOptions(); }

	// ------------------------------------------------------------------ render
	@Override
	public void render(Graphics2D g) {
		if (blackout) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, Game.W, Game.H);
			if (heroGlow > 0) {
				int r = (int) (60 + 220 * heroGlow);
				g.setPaint(new RadialGradientPaint(new Point2D.Double(Game.W / 2.0, 300), r, new float[] { 0f, 1f },
						new Color[] { new Color(255, 230, 160, (int) (200 * heroGlow)), new Color(255, 230, 160, 0) }));
				g.fillOval(Game.W / 2 - r, 300 - r, r * 2, r * 2);
				g.setPaint(null);
			}
		} else {
			BattleArt.background(g, setup.theme, game.tick);
			drawCreatures(g);
			BattleArt.infoBox(g, enemy, enemyHpShown, 40 - introSlide * 6, 34, false, enemyTitle());
			BattleArt.infoBox(g, ally, allyHpShown, Game.W - 440 + introSlide * 6, 392, true, null);
			drawExtras(g);
		}
		drawBottom(g);
		if (qteTimer > 0) drawQte(g);
	}

	private String enemyTitle() {
		if (setup.kind == BattleSetup.Kind.TRAINER) return enemy.name();
		if (setup.kind == BattleSetup.Kind.WILD) return "Wild " + enemy.name();
		return enemy.name();
	}

	private void drawCreatures(Graphics2D g) {
		int es = enemy.boss ? (setup.kind == BattleSetup.Kind.FINAL ? 300 : 240) : 192;
		int ex = 805 - es / 2 - enemyLunge * 2 + introSlide * 6, ey = 330 - es;
		if (!enemyHidden && (enemyFlash / 3) % 2 == 0) {
			BufferedImage im = Assets.scaled(enemy.image(), es, es);
			int dy = enemyFaint * 4;
			if (enemyFaint > 0) {
				java.awt.Composite c = g.getComposite();
				g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, 1f - enemyFaint / 30f)));
				g.drawImage(im, ex, ey + dy, null);
				g.setComposite(c);
			} else {
				int bob = enemy.boss ? (int) (Math.sin(game.tick / 16.0) * 4) : 0;
				g.drawImage(im, ex, ey + bob, null);
			}
		}
		if (bangaShake >= 0) {
			int wob = (bangaShake % 26) < 8 ? (int) (Math.sin(bangaShake * 1.2) * 8) : 0;
			g.drawImage(Assets.scaled(Assets.cg("obj_banga.png"), 96, 96), 760 + wob, 230, null);
		}
		if (sparkle > 0) {
			g.setColor(new Color(255, 240, 160, Math.min(255, sparkle * 5)));
			for (int i = 0; i < 10; i++) {
				double a = i * Math.PI / 5 + sparkle / 10.0;
				int r = 40 + (60 - sparkle);
				g.fillOval(808 + (int) (Math.cos(a) * r), 278 + (int) (Math.sin(a) * r), 8, 8);
			}
		}
		int as = 192;
		int ax = 310 - as / 2 + allyLunge * 3 - introSlide * 6, ay = 510 - as;
		if (!allyHidden && (allyFlash / 3) % 2 == 0) {
			BufferedImage im = Assets.flipped(Assets.scaled(ally.image(), as, as));
			if (allyFaint > 0) {
				java.awt.Composite c = g.getComposite();
				g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, 1f - allyFaint / 30f)));
				g.drawImage(im, ax, ay + allyFaint * 4, null);
				g.setComposite(c);
			} else {
				g.drawImage(im, ax, ay, null);
			}
		}
	}

	private void drawExtras(Graphics2D g) {
		// Mata ng Gabi shows the enemy's next move
		if (ally.species.ability == Ability.MATA_NG_GABI && !ally.fainted() && enemyPlanned != null && mode != Mode.QUEUE) {
			Text.panel(g, 40, 134, 400, 46, new Color(40, 30, 60, 235), new Color(250, 196, 60));
			Text.draw(g, "MATA NG GABI: next " + enemyPlanned.label, 58, 164, SMALL, new Color(255, 230, 150));
		}
		if (setup.kind == BattleSetup.Kind.FINAL) {
			Text.shadow(g, "Phase " + phase + "/3", 300, 152, SMALL, Color.WHITE);
			if (phase == 3 && armor > 0) {
				for (int i = 0; i < armor; i++) {
					g.setColor(new Color(140, 120, 90));
					g.fillRect(64 + i * 30, 140, 24, 12);
					g.setColor(new Color(60, 50, 40));
					g.drawRect(64 + i * 30, 140, 24, 12);
				}
			}
		}
		if (enemy.burnStacks > 0) Text.shadow(g, "BURN x" + enemy.burnStacks, 460, 70, SMALL, new Color(255, 120, 80));
		if (ally.burnStacks > 0) Text.draw(g, "BURN x" + ally.burnStacks, Game.W - 160, 392 + 36, SMALL, new Color(220, 70, 40));
		if (ally.accStage > 0) Text.draw(g, "INK -" + ally.accStage, Game.W - 160, 392 + 64, SMALL, new Color(90, 60, 120));
		if (story.hasBanga) {
			g.drawImage(Assets.scaled(Assets.cg("obj_pandesal.png"), 30, 30), Game.W - 430, 350, null);
			Text.draw(g, "x" + story.pandesal, Game.W - 398, 372, SMALL, Color.WHITE);
		}
	}

	private void drawBottom(Graphics2D g) {
		int y = 528;
		Text.panel(g, 12, y, Game.W - 24, Game.H - y - 12, new Color(36, 40, 70), new Color(240, 220, 160));
		boolean menuOpen = mode != Mode.QUEUE && mode != Mode.END;
		int textW = menuOpen ? 540 : Game.W - 90;
		g.setFont(MSG);
		List<String> lines = Text.wrap(message, g.getFontMetrics(), textW);
		int left = shown;
		int ty = y + 50;
		if (speaker != null) {
			Text.draw(g, speaker, 44, ty, MSG, Text.GOLD);
			ty += 36;
		}
		for (int i = 0; i < Math.min(lines.size(), speaker != null ? 3 : 4); i++) {
			String l = lines.get(i);
			String part = l.substring(0, Math.max(0, Math.min(l.length(), left)));
			left -= l.length() + 1;
			Text.draw(g, part, 44, ty + i * 36, MSG, Color.WHITE);
		}
		if (mode == Mode.QUEUE && shown >= message.length() && speaker != null)
			Text.nextArrow(g, Game.W - 70, Game.H - 50, game.tick, Color.WHITE);
		if (mode == Mode.MENU) drawGrid(g, y);
		if (mode == Mode.MOVES) drawMoves(g, y);
		if (mode == Mode.PANDESAL) drawList(g, y, pandesalOptions());
		if (mode == Mode.SWITCH || mode == Mode.FORCED_SWITCH) drawSwitch(g);
	}

	private void drawGrid(Graphics2D g, int y) {
		int n = hintAvailable() ? 6 : 5;
		int x0 = 610, w = 146, h = 66;
		for (int i = 0; i < n; i++) {
			int cx = x0 + (i % 3) * (w + 8), cy = y + 22 + (i / 3) * (h + 10);
			boolean on = i == sel;
			g.setColor(on ? Text.GOLD : new Color(70, 76, 120));
			g.fillRoundRect(cx, cy, w, h, 14, 14);
			g.setColor(on ? Text.INK : new Color(200, 200, 230));
			g.drawRoundRect(cx, cy, w, h, 14, 14);
			g.setFont(MENU);
			int tw = g.getFontMetrics().stringWidth(MAIN[i]);
			Text.draw(g, MAIN[i], cx + (w - tw) / 2, cy + 42, MENU, on ? Text.INK : Color.WHITE);
		}
	}

	private void drawMoves(Graphics2D g, int y) {
		Move[] ms = ally.moves();
		for (int i = 0; i <= ms.length; i++) {
			int cy = y + 20 + i * 52;
			boolean on = i == sel;
			g.setColor(on ? Text.GOLD : new Color(70, 76, 120));
			g.fillRoundRect(600, cy, 450, 46, 12, 12);
			if (i < ms.length) {
				Move m = ms[i];
				Text.draw(g, m.label, 618, cy + 31, MENU, on ? Text.INK : Color.WHITE);
				BattleArt.elementBadge(g, m.element, 870, cy + 12);
				Text.draw(g, "PWR " + m.power, 958, cy + 30, SMALL, on ? Text.INK : Color.WHITE);
			} else {
				Text.draw(g, "Back", 618, cy + 31, MENU, on ? Text.INK : Color.WHITE);
			}
		}
		if (sel < ms.length) {
			double eff = ms[sel].element.against(enemy.element());
			String note = eff > 1 ? "Super effective!" : eff < 1 ? "Not very effective..." : "";
			if (!note.isEmpty()) Text.draw(g, note, 44, Game.H - 40, SMALL, eff > 1 ? new Color(140, 240, 140) : new Color(240, 170, 140));
		}
	}

	private void drawList(Graphics2D g, int y, List<String> opts) {
		for (int i = 0; i < opts.size(); i++) {
			int cy = y + 22 + i * 50;
			boolean on = i == sel;
			g.setColor(on ? Text.GOLD : new Color(70, 76, 120));
			g.fillRoundRect(640, cy, 410, 44, 12, 12);
			Text.draw(g, opts.get(i), 660, cy + 30, MENU, on ? Text.INK : Color.WHITE);
		}
	}

	private void drawSwitch(Graphics2D g) {
		List<Creature> list = switchable();
		int n = list.size() + (mode == Mode.SWITCH ? 1 : 0);
		int h = n * 70 + 30;
		int x = 250, y = 70;
		Text.panel(g, x, y, 580, h);
		for (int i = 0; i < n; i++) {
			int cy = y + 20 + i * 70;
			boolean on = i == sel;
			if (on) {
				g.setColor(new Color(244, 196, 72, 140));
				g.fillRoundRect(x + 14, cy, 552, 62, 12, 12);
			}
			if (i < list.size()) {
				Creature c = list.get(i);
				g.drawImage(Assets.scaled(c.image(), 56, 56), x + 24, cy + 3, null);
				String tag = c == ally ? " (out)" : c.fainted() ? " (tired)" : "";
				Text.draw(g, c.name() + "  Lv." + c.level + tag, x + 92, cy + 28, MENU, Text.INK);
				BattleArt.elementBadge(g, c.element(), x + 92, cy + 36);
				g.setColor(new Color(60, 60, 70));
				g.fillRect(x + 190, cy + 42, 240, 10);
				g.setColor(BattleArt.hpColor(c.hpFrac()));
				g.fillRect(x + 190, cy + 42, (int) (240 * c.hpFrac()), 10);
				Text.draw(g, Math.max(0, c.hp) + "/" + c.maxHp(), x + 448, cy + 52, SMALL, Text.INK);
			} else {
				Text.draw(g, "Back", x + 92, cy + 38, MENU, Text.INK);
			}
		}
	}

	private void drawQte(Graphics2D g) {
		g.setColor(new Color(20, 60, 140, 120));
		g.fillRect(0, 0, Game.W, 528);
		boolean left = "left".equals(qteDir);
		int cx = Game.W / 2 + (left ? -160 : 160);
		g.setColor(new Color(255, 255, 255, 230));
		if (left) g.fillPolygon(new int[] { cx - 70, cx + 50, cx + 50 }, new int[] { 260, 190, 330 }, 3);
		else g.fillPolygon(new int[] { cx + 70, cx - 50, cx - 50 }, new int[] { 260, 190, 330 }, 3);
		Text.center(g, "DODGE! Press " + (left ? "LEFT" : "RIGHT") + "!", Game.W / 2, 120, Assets.font(26f), Color.WHITE, true);
		g.setColor(Color.WHITE);
		g.fillRect(Game.W / 2 - 150, 420, (int) (300 * qteTimer / 70.0), 10);
	}
}
