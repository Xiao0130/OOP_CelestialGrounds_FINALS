import java.util.*;
import celestial.*;
import celestial.battle.*;
import celestial.data.*;
import celestial.story.*;

/** Runs boss battles many times with the bot's battle AI. */
public class Sim {
	public static void main(String[] a) throws Exception {
		Sound.disable();
		String[] bosses = a.length > 0 ? a : new String[] { "tamaraw", "dona", "haribon", "butanding", "bakunawa", "bakunawa_nowater" };
		Species[] starters = { Species.TARSI, Species.BINTURO, Species.DUGONG };
		for (String boss : bosses) {
			for (Species st : starters) {
				int wins = 0, n = 60; long turns = 0, pd = 0; int[] ph = new int[4];
				for (int i = 0; i < n; i++) {
					Game g = new Game();
					Bot.g = g;
					Story s = g.story;
					s.starter = st; s.hasBanga = true; s.pandesal = 3; s.set("tala_joined");
					party(s, boss, st);
					Battle b = new Battle(g, setup(g, boss));
					g.reset(b);
					int f = 0;
					while (!b.isClosed() && f < 60000) { Bot.drive(b); g.update(); Bot.frame++; f++; }
					if (b.result() == Battle.Result.WIN) wins++;
					turns += b.turn; pd += 3 - s.pandesal; if (b.result() != Battle.Result.WIN) ph[b.phase]++;
				}
				System.out.printf("%-10s %-8s win %3d%%  turns %.1f  pandesal used %.1f  lost in phase %s%n", boss, st.label, wins * 100 / n, turns / (double) n, pd / (double) n, java.util.Arrays.toString(ph));
			}
		}
	}
	static void party(Story s, String boss, Species st) {
		int L; Species third;
		switch (boss) {
			case "tamaraw": L = 5; break;
			case "dona": case "haribon": L = 8; break;
			case "butanding": L = 10; break;
			default: L = 12;
		}
		s.addCreature(new Creature(st, L));
		s.addCreature(new Creature(Species.KALABAW, L - 1));
		if (boss.equals("tamaraw")) { if (st != Species.TARSI) s.addCreature(new Creature(Species.TARSI, L - 2)); return; }
		third = st == Species.BINTURO ? (boss.equals("bakunawa") || boss.equals("butanding") ? Species.PUGITA : Species.AHAS_BULKAN) : Species.AHAS_BULKAN;
		if (boss.equals("bakunawa") && st == Species.DUGONG) third = Species.AHAS_BULKAN;
		if (boss.equals("bakunawa") && st == Species.TARSI) { s.party.remove(1); s.addCreature(new Creature(Species.PUGITA, L - 1)); }
		if (boss.equals("bakunawa_nowater")) { s.party.clear(); s.addCreature(new Creature(Species.TARSI, L)); s.addCreature(new Creature(Species.KALABAW, L)); s.addCreature(new Creature(Species.BINTURO, L - 3)); return; }
		s.addCreature(new Creature(third, L - 1));
	}
	static BattleSetup setup(Game g, String boss) {
		switch (boss) {
			case "tamaraw": return BattleScripts.tamaraw(g);
			case "dona": return BattleScripts.dona(g);
			case "haribon": return BattleScripts.haribon(g);
			case "butanding": return BattleScripts.butanding(g);
			default: return BattleScripts.bakunawa(g);
		}
	}
}
