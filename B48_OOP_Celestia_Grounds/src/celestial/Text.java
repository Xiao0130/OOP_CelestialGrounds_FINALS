package celestial;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/** Text helpers: word-wrap, glyph-safe strings for the Pokemon GB font, panels. */
public final class Text {

	private Text() {}

	public static final Color INK = new Color(36, 32, 44);
	public static final Color PAPER = new Color(250, 246, 236);
	public static final Color GOLD = new Color(244, 196, 72);

	/** The GB font has no curly quotes, % or music notes. */
	public static String safe(String s) {
		return s.replace('\u201c', '\'').replace('\u201d', '\'').replace('"', '\'')
				.replace('\u2019', '\'').replace('\u2018', '\'').replace("\u2026", "...")
				.replace("\u2014", "-").replace("%", " pct");
	}

	public static List<String> wrap(String text, FontMetrics fm, int width) {
		List<String> lines = new ArrayList<>();
		for (String para : safe(text).split("\n")) {
			StringBuilder line = new StringBuilder();
			for (String word : para.split(" ")) {
				String trial = line.length() == 0 ? word : line + " " + word;
				if (fm.stringWidth(trial) > width && line.length() > 0) {
					lines.add(line.toString());
					line = new StringBuilder(word);
				} else {
					line = new StringBuilder(trial);
				}
			}
			lines.add(line.toString());
		}
		return lines;
	}

	public static void aa(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}

	public static void draw(Graphics2D g, String s, int x, int y, Font f, Color c) {
		g.setFont(f);
		g.setColor(c);
		g.drawString(safe(s), x, y);
	}

	public static void shadow(Graphics2D g, String s, int x, int y, Font f, Color c) {
		g.setFont(f);
		g.setColor(new Color(0, 0, 0, 160));
		g.drawString(safe(s), x + 2, y + 2);
		g.setColor(c);
		g.drawString(safe(s), x, y);
	}

	public static void center(Graphics2D g, String s, int cx, int y, Font f, Color c, boolean shadow) {
		g.setFont(f);
		int w = g.getFontMetrics().stringWidth(safe(s));
		if (shadow) shadow(g, s, cx - w / 2, y, f, c);
		else draw(g, s, cx - w / 2, y, f, c);
	}

	/** Rounded parchment panel with a dark border, used by every box in the game. */
	public static void panel(Graphics2D g, int x, int y, int w, int h, Color fill, Color border) {
		g.setColor(new Color(0, 0, 0, 90));
		g.fillRoundRect(x + 4, y + 5, w, h, 18, 18);
		g.setColor(border);
		g.fillRoundRect(x, y, w, h, 18, 18);
		g.setColor(fill);
		g.fillRoundRect(x + 5, y + 5, w - 10, h - 10, 12, 12);
	}

	public static void panel(Graphics2D g, int x, int y, int w, int h) {
		panel(g, x, y, w, h, PAPER, INK);
	}

	/** Little bouncing triangle that says "press Enter". */
	public static void nextArrow(Graphics2D g, int x, int y, long tick, Color c) {
		int bob = (int) ((tick / 8) % 2) * 3;
		g.setColor(c);
		g.fillPolygon(new int[] { x, x + 14, x + 7 }, new int[] { y + bob, y + bob, y + 9 + bob }, 3);
	}

	/** Draws a music note (the font has none). */
	public static void note(Graphics2D g, int x, int y, int size, Color c) {
		g.setColor(c);
		g.fillOval(x, y + size - size / 3, size / 2, size / 3);
		g.fillRect(x + size / 2 - 3, y, 3, size - size / 6);
		g.fillRect(x + size / 2 - 3, y, size / 3, 3);
	}
}
