package celestial.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Text;

/** "PROMPT: Press Enter to ..." moments: a pulsing line over the world, waits for Enter. */
public class PromptScreen extends Screen {

	private static final Font FONT = Assets.font(18f);
	private final String text;
	private int age;

	public PromptScreen(Game game, String text) {
		super(game);
		this.text = game.story.sub(text);
	}

	@Override
	public void update() {
		age++;
		if (age > 20 && game.input.pressed(Key.CONFIRM)) close();
	}

	@Override
	public void render(Graphics2D g) {
		int a = (int) (150 + 100 * Math.sin(game.tick / 10.0));
		g.setColor(new Color(10, 10, 30, 190));
		g.fillRoundRect(140, Game.H - 120, Game.W - 280, 64, 20, 20);
		Text.center(g, text, Game.W / 2, Game.H - 78, FONT, new Color(255, 236, 170, Math.max(0, Math.min(255, a))), false);
	}
}
