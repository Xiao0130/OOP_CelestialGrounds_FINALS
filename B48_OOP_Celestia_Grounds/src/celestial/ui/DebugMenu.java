package celestial.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Sound;
import celestial.Text;
import celestial.data.Species;
import celestial.story.Scenes;

/** F12: jump to any chapter with a ready-made team (for testing). */
public class DebugMenu extends Screen {

	private static final Font F = Assets.font(16f);
	private static final Species[] STARTERS = { Species.TARSI, Species.BINTURO, Species.DUGONG };
	private int sel, starter;

	public DebugMenu(Game game) {
		super(game);
	}

	@Override
	public void update() {
		int n = Scenes.CHAPTERS.length;
		if (game.input.pressed(Key.UP)) { sel = (sel + n - 1) % n; Sound.blip(); }
		if (game.input.pressed(Key.DOWN)) { sel = (sel + 1) % n; Sound.blip(); }
		if (game.input.pressed(Key.LEFT)) starter = (starter + 2) % 3;
		if (game.input.pressed(Key.RIGHT)) starter = (starter + 1) % 3;
		if (game.input.pressed(Key.CANCEL) || game.input.pressed(Key.DEBUG)) close();
		if (game.input.pressed(Key.CONFIRM)) Scenes.jump(game, sel, STARTERS[starter]);
	}

	@Override
	public void render(Graphics2D g) {
		Text.panel(g, 200, 60, 680, 600, new Color(20, 24, 50, 240), Text.GOLD);
		Text.draw(g, "DEBUG: jump to chapter", 230, 104, F, Text.GOLD);
		Text.draw(g, "Starter (Left/Right): " + STARTERS[starter].label, 230, 140, F, Color.WHITE);
		for (int i = 0; i < Scenes.CHAPTERS.length; i++)
			Text.draw(g, (i == sel ? "> " : "  ") + Scenes.CHAPTERS[i], 230, 190 + i * 40, F, i == sel ? Text.GOLD : Color.WHITE);
		Text.draw(g, "Enter: jump   Esc: close", 230, 630, F, new Color(180, 180, 210));
	}
}
