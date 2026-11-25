package celestial.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import celestial.data.Creature;
import celestial.world.Theme;

/** Who you are fighting, where, and any scripted behaviour (hooks). */
public class BattleSetup {

	public enum Kind { WILD, TRAINER, GUARDIAN, FINAL }

	public final Kind kind;
	public final String trainer;              // "Dali", "Kuya Gusmo", "Tamaraw, Guardian of the Earth"...
	public final List<Creature> enemies;
	public final Theme theme;
	public BattleHooks hooks = new BattleHooks() {};
	public String trainerSprite;              // char sheet shown at the start of trainer battles
	public boolean noExp;

	public BattleSetup(Kind kind, String trainer, Theme theme, Creature... enemies) {
		this.kind = kind;
		this.trainer = trainer;
		this.theme = theme;
		this.enemies = new ArrayList<>(Arrays.asList(enemies));
	}

	public static BattleSetup wild(Creature c, Theme theme) {
		return new BattleSetup(Kind.WILD, null, theme, c);
	}

	public static BattleSetup trainer(String name, String sprite, Theme theme, Creature... cs) {
		BattleSetup s = new BattleSetup(Kind.TRAINER, name, theme, cs);
		s.trainerSprite = sprite;
		return s;
	}

	public static BattleSetup guardian(String title, Creature c, Theme theme) {
		c.boss = true;
		c.hp = c.maxHp();
		return new BattleSetup(Kind.GUARDIAN, title, theme, c);
	}

	public BattleSetup hooks(BattleHooks h) {
		hooks = h;
		return this;
	}
}
