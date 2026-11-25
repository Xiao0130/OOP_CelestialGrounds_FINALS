package celestial.world;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;

import celestial.Assets;
import celestial.Game;
import celestial.Text;
import celestial.story.Story;

/** Moon (cracked, brightening with each Hiyas), Hiyas gems, Tapang parol meter, Pandesal count. */
public final class Hud {

	private Hud() {}

	private static final Font SMALL = Assets.font(14f);
	public static final Color[] HIYAS = { new Color(200, 150, 60), new Color(236, 84, 52), new Color(70, 150, 236) };

	public static void draw(Graphics2D g, Game game, String area) {
		Story s = game.story;
		// area name
		g.setFont(SMALL);
		int aw = g.getFontMetrics().stringWidth(Text.safe(area)) + 30;
		g.setColor(new Color(10, 12, 30, 150));
		g.fillRoundRect(12, 12, aw, 34, 12, 12);
		Text.draw(g, area, 27, 35, SMALL, new Color(240, 232, 210));

		int x = Game.W - 316, y = 12;
		g.setColor(new Color(10, 12, 30, 150));
		g.fillRoundRect(x, y, 304, 64, 14, 14);
		moon(g, x + 34, y + 32, 22, s.hiyas, s.has("seven_moons"), game.tick);
		// Hiyas gems
		for (int i = 0; i < 3; i++) gem(g, x + 72 + i * 24, y + 32, 9, i < s.hiyas ? HIYAS[i] : null);
		// Tapang parol meter
		if (s.hasParol) {
			g.drawImage(Assets.scaled(Assets.cg("obj_parol.png"), 30, 30), x + 146, y + 16, null);
			for (int i = 0; i < 4; i++) {
				boolean on = i < s.tapang;
				g.setColor(on ? (s.heroLight ? new Color(255, 236, 150) : new Color(255, 170, 70)) : new Color(80, 80, 100));
				g.fillRoundRect(x + 180 + i * 14, y + 24, 10, 18, 4, 4);
			}
		}
		if (s.hasBanga) {
			g.drawImage(Assets.scaled(Assets.cg("obj_pandesal.png"), 28, 28), x + 240, y + 18, null);
			Text.draw(g, "x" + s.pandesal, x + 268, y + 40, SMALL, Color.WHITE);
		}
		if (s.starter != null && !game.inCutscene()) {
			String hint = "P Team   J Journal";
			g.setFont(SMALL);
			int hw = g.getFontMetrics().stringWidth(hint) + 24;
			g.setColor(new Color(10, 12, 30, 120));
			g.fillRoundRect(12, Game.H - 42, hw, 30, 10, 10);
			Text.draw(g, hint, 24, Game.H - 21, SMALL, new Color(220, 220, 230));
		}
	}

	/** The cracked moon. Each Hiyas brightens one slice; the ending shows it whole. */
	public static void moon(Graphics2D g, int cx, int cy, int r, int slices, boolean whole, long tick) {
		g.setColor(new Color(255, 250, 220, 50));
		g.fillOval(cx - r - 6, cy - r - 6, (r + 6) * 2, (r + 6) * 2);
		g.setColor(new Color(150, 150, 170));
		g.fillOval(cx - r, cy - r, r * 2, r * 2);
		g.setColor(new Color(250, 244, 210));
		if (whole) {
			g.fillOval(cx - r, cy - r, r * 2, r * 2);
		} else {
			for (int i = 0; i < slices; i++) g.fillArc(cx - r, cy - r, r * 2, r * 2, 90 + i * 120, 120);
			g.setColor(new Color(60, 50, 70));
			g.setStroke(new BasicStroke(2f));
			g.drawPolyline(new int[] { cx - r + 3, cx - 6, cx + 2, cx - 3, cx + r - 4 },
					new int[] { cy - 6, cy - 2, cy - 9, cy + 4, cy + 2 }, 5);
			g.setStroke(new BasicStroke(1f));
		}
		g.setColor(new Color(200, 196, 170));
		g.fillOval(cx - 9, cy + 5, 6, 6);
		g.fillOval(cx + 6, cy - 12, 5, 5);
	}

	public static void gem(Graphics2D g, int cx, int cy, int r, Color c) {
		Polygon p = new Polygon(new int[] { cx, cx + r, cx, cx - r }, new int[] { cy - r - 2, cy, cy + r + 2, cy }, 4);
		if (c != null) {
			g.setColor(c);
			g.fillPolygon(p);
			g.setColor(new Color(255, 255, 255, 180));
			g.fillPolygon(new int[] { cx, cx + r / 2, cx }, new int[] { cy - r, cy - 2, cy }, 3);
		}
		g.setColor(c == null ? new Color(120, 120, 140) : Color.WHITE);
		g.drawPolygon(p);
	}
}
