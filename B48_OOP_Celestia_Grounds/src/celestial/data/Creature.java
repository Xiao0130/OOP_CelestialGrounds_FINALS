package celestial.data;

/** One Hayop-Diwata with its level, HP and per-battle state. */
public class Creature {

	public final Species species;
	public int level;
	public int exp;
	public int hp;
	public boolean boss;          // Guardians get extra HP

	// per-battle state
	public int atkStage;          // Hindi Aatras
	public int accStage;          // lowered by Tinta
	public int burnStacks;        // Lason-Alab
	public boolean tiyagaUsed;
	public boolean fed;           // offered a Pandesal this battle

	// Bakunawa changes form between phases
	public Element elementOverride;
	public String spriteOverride;

	public Creature(Species species, int level) {
		this.species = species;
		this.level = level;
		this.hp = maxHp();
	}

	public static Creature boss(Species s, int level) {
		Creature c = new Creature(s, level);
		c.boss = true;
		c.hp = c.maxHp();
		return c;
	}

	public String name() { return species.label; }

	public Element element() { return elementOverride != null ? elementOverride : species.element; }

	public java.awt.image.BufferedImage image() {
		return spriteOverride != null ? celestial.Assets.cg("mon_" + spriteOverride + ".png") : species.image();
	}

	public int maxHp() {
		int v = species.hp * level / 40 + level + 12;
		return boss ? v * 125 / 100 : v;
	}

	public int atk() {
		int v = species.atk * 2 * level / 50 + 6;
		double mult = atkStage == 0 ? 1 : atkStage == 1 ? 1.3 : 1.6;
		return (int) Math.round(v * mult);
	}

	public int def() { return species.def * 2 * level / 50 + 6; }

	public int spd() { return species.spd * 2 * level / 50 + 6; }

	public Move[] moves() {
		if (species == Species.BAKUNAWA) return new Move[] { Move.ALON_NG_GUTOM };
		return new Move[] { species.signature, Move.TULAK };
	}

	public boolean fainted() { return hp <= 0; }

	public double hpFrac() { return Math.max(0, hp) / (double) maxHp(); }

	public void heal(int amount) { hp = Math.min(maxHp(), hp + amount); }

	public void fullHeal() { hp = maxHp(); }

	public void resetBattleState() {
		atkStage = 0;
		accStage = 0;
		burnStacks = 0;
		tiyagaUsed = false;
		fed = false;
	}

	public static int expToNext(int level) { return 10 * level; }

	/** Adds exp; returns number of levels gained. HP grows with the level. */
	public int gainExp(int amount) {
		int gained = 0;
		exp += amount;
		while (exp >= expToNext(level) && level < 30) {
			exp -= expToNext(level);
			int oldMax = maxHp();
			level++;
			hp += maxHp() - oldMax;
			gained++;
		}
		return gained;
	}

	public double expFrac() { return exp / (double) expToNext(level); }
}
