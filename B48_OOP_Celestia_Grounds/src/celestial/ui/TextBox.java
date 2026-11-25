package celestial.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Sound;
import celestial.Text;

/** Dialogue box with typewriter text, speaker tag and pages of three lines. */
public class TextBox extends Screen {

	public enum Style { SPEECH, NARRATION, SYSTEM }

	private static final int X = 24, Y = Game.H - 196, BW = Game.W - 48, BH = 176;
	private static final Font FONT = celestial.Assets.font(18f);
	private static final Font TAG = celestial.Assets.font(16f);

	private final String speaker;
	private final Style style;
	private final String text;
	private List<String> lines;
	private int page;
	private int shown;
	private int age;

	public TextBox(Game game, String speaker, String text, Style style) {
		super(game);
		this.style = style;
		String sp = speaker;
		if ("PLAYER".equals(sp)) sp = game.story.heroName.toUpperCase();
		this.speaker = sp;
		this.text = game.story.sub(text);
	}

	/** The full text (used by tests). */
	public String text() { return text; }

	private List<String> lines(Graphics2D g) {
		if (lines == null) {
			g.setFont(FONT);
			lines = Text.wrap(text, g.getFontMetrics(), BW - 70);
		}
		return lines;
	}

	private String pageText() {
		if (lines == null) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = page * 3; i < Math.min(lines.size(), page * 3 + 3); i++) sb.append(lines.get(i)).append('\n');
		return sb.toString();
	}

	@Override
	public void update() {
		age++;
		if (lines == null) {
			BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = scratch.createGraphics();
			lines(g);
			g.dispose();
		}
		int total = pageText().length();
		if (shown < total) {
			shown = Math.min(total, shown + 2);
			if (style == Style.SPEECH && age % 4 == 0) Sound.blip();
		}
		if (game.input.pressed(Key.CONFIRM) || game.input.pressed(Key.CANCEL)) {
			if (shown < total) {
				shown = total;
			} else if ((page + 1) * 3 < lines.size()) {
				page++;
				shown = 0;
			} else {
				close();
			}
		}
	}

	@Override
	public void render(Graphics2D g) {
		List<String> ls = lines(g);
		Color fill, border, ink;
		switch (style) {
			case NARRATION: fill = new Color(20, 22, 44, 230); border = new Color(120, 120, 170); ink = new Color(230, 222, 190); break;
			case SYSTEM: fill = new Color(28, 60, 110, 240); border = new Color(150, 200, 255); ink = new Color(236, 244, 255); break;
			default: fill = Text.PAPER; border = Text.INK; ink = Text.INK; break;
		}
		Text.panel(g, X, Y, BW, BH, fill, border);
		if (speaker != null) {
			g.setFont(TAG);
			int tw = g.getFontMetrics().stringWidth(Text.safe(speaker)) + 34;
			Text.panel(g, X + 20, Y - 38, tw, 46, style == Style.SYSTEM ? new Color(60, 110, 190) : Text.GOLD, border);
			Text.draw(g, speaker, X + 37, Y - 8, TAG, style == Style.SYSTEM ? Color.WHITE : Text.INK);
		}
		int remaining = shown;
		for (int i = 0; i < 3; i++) {
			int li = page * 3 + i;
			if (li >= ls.size()) break;
			String l = ls.get(li);
			String part = l.substring(0, Math.max(0, Math.min(l.length(), remaining)));
			remaining -= l.length() + 1;
			Text.draw(g, part, X + 34, Y + 52 + i * 40, FONT, ink);
			if (remaining < 0) break;
		}
		if (shown >= pageText().length()) Text.nextArrow(g, X + BW - 44, Y + BH - 36, game.tick, ink);
	}
}
