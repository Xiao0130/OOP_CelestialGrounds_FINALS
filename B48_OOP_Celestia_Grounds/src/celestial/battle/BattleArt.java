package celestial.battle;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import celestial.Assets;
import celestial.Game;
import celestial.Text;
import celestial.data.Creature;
import celestial.data.Element;
import celestial.world.Theme;

/** Battle backgrounds and status boxes, drawn in code to match each area. */
public final class BattleArt {

	private BattleArt() {}

	private static final Font NAME = Assets.font(18f);
	private static final Font SMALL = Assets.font(13f);

	public static void background(Graphics2D g, Theme th, long tick) {
		Color skyTop, skyBottom, ground, ground2;
		switch (th) {
			case KAWAYAN: skyTop = new Color(30, 60, 50); skyBottom = new Color(90, 130, 90); ground = new Color(60, 100, 56); ground2 = new Color(44, 80, 44); break;
			case TUKTOK: skyTop = new Color(250, 150, 90); skyBottom = new Color(255, 214, 140); ground = new Color(130, 170, 80); ground2 = new Color(100, 140, 60); break;
			case BULKAN: case CRATER: skyTop = new Color(70, 26, 30); skyBottom = new Color(200, 90, 50); ground = new Color(90, 76, 72); ground2 = new Color(64, 54, 52); break;
			case DAGAT: skyTop = new Color(100, 180, 240); skyBottom = new Color(200, 236, 250); ground = new Color(60, 150, 200); ground2 = new Color(230, 214, 160); break;
			case TEMPLO: skyTop = new Color(10, 30, 40); skyBottom = new Color(30, 80, 90); ground = new Color(60, 90, 96); ground2 = new Color(44, 66, 72); break;
			case REEF: skyTop = new Color(10, 40, 90); skyBottom = new Color(40, 110, 160); ground = new Color(110, 150, 160); ground2 = new Color(80, 120, 136); break;
			case CELESTIA: skyTop = new Color(8, 8, 30); skyBottom = new Color(40, 40, 90); ground = new Color(60, 58, 100); ground2 = new Color(44, 42, 80); break;
			case BARRIO: skyTop = new Color(250, 190, 120); skyBottom = new Color(255, 236, 190); ground = new Color(110, 176, 80); ground2 = new Color(86, 150, 64); break;
			default: skyTop = new Color(110, 180, 240); skyBottom = new Color(220, 240, 250); ground = new Color(116, 182, 74); ground2 = new Color(90, 156, 60); break;
		}
		g.setPaint(new GradientPaint(0, 0, skyTop, 0, 330, skyBottom));
		g.fillRect(0, 0, Game.W, 330);
		g.setPaint(null);
		if (th == Theme.CELESTIA || th == Theme.TEMPLO) {
			g.setColor(new Color(255, 255, 255, 180));
			for (int i = 0; i < 60; i++) {
				int x = (i * 197) % Game.W, y = (i * 83) % 300;
				int tw = (int) ((tick / 10 + i) % 7) == 0 ? 3 : 2;
				g.fillRect(x, y, tw, tw);
			}
		}
		if (th == Theme.PALAYAN || th == Theme.BARRIO || th == Theme.TUKTOK) {
			g.setColor(new Color(90, 150, 90));
			for (int i = 0; i < 6; i++) g.fillOval(-80 + i * 220, 250, 300, 160);
		}
		if (th == Theme.KAWAYAN) {
			for (int i = 0; i < 26; i++) {
				int x = i * 44 + (i % 3) * 7;
				g.setColor(i % 2 == 0 ? new Color(70, 120, 60) : new Color(50, 96, 48));
				g.fillRect(x, 0, 12, 340);
				g.setColor(new Color(150, 190, 110));
				for (int y = 30 + i * 13 % 50; y < 340; y += 70) g.fillRect(x, y, 12, 3);
			}
		}
		if (th == Theme.BULKAN || th == Theme.CRATER) {
			g.setColor(new Color(50, 30, 34));
			g.fillPolygon(new int[] { 240, 520, 800 }, new int[] { 330, 60, 330 }, 3);
			g.setColor(new Color(255, 140, 50));
			g.fillPolygon(new int[] { 480, 520, 560 }, new int[] { 90, 60, 90 }, 3);
		}
		g.setColor(ground);
		g.fillRect(0, 330, Game.W, Game.H - 330);
		g.setColor(ground2);
		for (int y = 360; y < Game.H; y += 34) g.fillRect(0, y, Game.W, 6);
		// platforms
		g.setColor(new Color(0, 0, 0, 50));
		g.fillOval(640, 300, 330, 70);
		g.fillOval(130, 470, 360, 80);
		g.setColor(ground2.brighter());
		g.fillOval(650, 294, 310, 62);
		g.fillOval(140, 462, 340, 74);
	}

	public static Color hpColor(double f) {
		return f > 0.5 ? new Color(70, 200, 90) : f > 0.2 ? new Color(240, 196, 50) : new Color(230, 70, 60);
	}

	public static void elementBadge(Graphics2D g, Element e, int x, int y) {
		if (e == Element.WALA) return;
		g.setColor(e.color);
		g.fillRoundRect(x, y, 76, 22, 10, 10);
		Text.draw(g, e.tagalog.toUpperCase(), x + 8, y + 17, SMALL, Color.WHITE);
	}

	/** Name, level, element and HP bar. shownHp is the animated value. */
	public static void infoBox(Graphics2D g, Creature c, double shownHp, int x, int y, boolean showNumbers, String title) {
		int w = 400, h = showNumbers ? 112 : 92;
		Text.panel(g, x, y, w, h);
		Text.draw(g, title != null ? title : c.name(), x + 22, y + 36, NAME, Text.INK);
		Text.draw(g, "Lv." + c.level, x + w - 92, y + 36, NAME, Text.INK);
		elementBadge(g, c.element(), x + w - 96, y - 14);
		int bx = x + 70, by = y + 52, bw = w - 100;
		Text.draw(g, "HP", x + 24, by + 15, SMALL, Text.INK);
		g.setColor(new Color(60, 60, 70));
		g.fillRoundRect(bx - 3, by - 3, bw + 6, 20, 8, 8);
		double f = Math.max(0, shownHp) / c.maxHp();
		g.setColor(hpColor(f));
		g.fillRoundRect(bx, by, (int) (bw * f), 14, 6, 6);
		if (showNumbers) {
			Text.draw(g, Math.max(0, (int) Math.ceil(shownHp)) + " / " + c.maxHp(), x + w - 170, y + 94, SMALL, Text.INK);
			g.setColor(new Color(80, 140, 220));
			g.fillRect(x + 24, y + 98, (int) (150 * c.expFrac()), 5);
			g.setColor(new Color(0, 0, 0, 60));
			g.drawRect(x + 24, y + 98, 150, 5);
		}
	}
}
