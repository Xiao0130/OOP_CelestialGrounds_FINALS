package celestial.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Text;

/** Centered TUTORIAL / notice card. */
public class Popup extends Screen {

	private static final Font TITLE = Assets.font(20f);
	private static final Font BODY = Assets.font(17f);
	private final String title, text;
	private List<String> lines;
	private int age;

	public Popup(Game game, String title, String text) {
		super(game);
		this.title = title;
		this.text = game.story.sub(text);
	}

	@Override
	public void update() {
		age++;
		if (age > 12 && (game.input.pressed(Key.CONFIRM) || game.input.pressed(Key.CANCEL))) close();
	}

	@Override
	public void render(Graphics2D g) {
		int w = 760;
		g.setFont(BODY);
		if (lines == null) lines = Text.wrap(text, g.getFontMetrics(), w - 80);
		int h = 110 + lines.size() * 34;
		int x = (Game.W - w) / 2, y = Math.max(40, (Game.H - h) / 2 - 60);
		g.setColor(new Color(0, 0, 0, 90));
		g.fillRect(0, 0, Game.W, Game.H);
		Text.panel(g, x, y, w, h, new Color(255, 250, 230), new Color(150, 96, 30));
		g.setColor(Text.GOLD);
		g.fillRoundRect(x + 5, y + 5, w - 10, 50, 12, 12);
		Text.center(g, title, Game.W / 2, y + 40, TITLE, Text.INK, false);
		for (int i = 0; i < lines.size(); i++) Text.draw(g, lines.get(i), x + 40, y + 94 + i * 34, BODY, Text.INK);
		if (age > 12) Text.nextArrow(g, x + w - 40, y + h - 30, game.tick, Text.INK);
	}
}
