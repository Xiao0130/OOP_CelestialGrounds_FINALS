package celestial.battle;

import celestial.data.Creature;
import celestial.data.Move;

/** Scripted behaviour for a specific battle (tutorial pop-ups, boss lines...). All optional. */
public interface BattleHooks {

	/** Replace the standard opening ("A wild X appeared!" / "X wants to battle!"). */
	default boolean intro(Battle b) { return false; }

	/** Runs once at the start of each turn, before the menu opens. */
	default void beforeMenu(Battle b, int turn) {}

	/** Return a message to make the enemy skip its action this turn (e.g. Dali's partner posing). */
	default String enemySkip(Battle b, int turn) { return null; }

	/** Return a fixed damage amount (>= 0) to keep tutorials on script; -1 = normal damage. */
	default int fixedDamage(Battle b, Creature attacker, Creature defender, Move m, boolean byPlayer) { return -1; }

	/** Return TRUE/FALSE to force who acts first this turn; null = speed decides. */
	default Boolean enemyFirst(Battle b, int turn) { return null; }

	/** Multiply damage. */
	default double damageScale(Battle b, Creature attacker, Creature defender, Move m, boolean byPlayer) { return 1.0; }

	/** Return true to keep a creature at 1 HP instead of fainting (befriending tutorial). */
	default boolean preventFaint(Battle b, Creature c) { return false; }

	/** After any hit lands. */
	default void afterHit(Battle b, Creature attacker, Creature defender, boolean byPlayer) {}

	/** Return true if offering a Pandesal to the enemy was handled here (Tamaraw calm). */
	default boolean onOffer(Battle b) { return false; }

	/** Custom text when a wild creature eats your Pandesal. */
	default String[] offerLines(Battle b) { return null; }

	/** Custom lines for a successful befriend. */
	default String[] befriendLines(Battle b) { return null; }

	/** Tala's advice for the HINT button. */
	default String hint(Battle b) { return null; }

	/** After "You won the battle!". */
	default void onWin(Battle b) {}

	/** Final boss: runs when the third phase is emptied. */
	default void finale(Battle b) {}
}
