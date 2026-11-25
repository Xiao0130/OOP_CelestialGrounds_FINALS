package celestial.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Sound;
import celestial.Text;
import celestial.story.Story;
import celestial.world.CharSheet;
import celestial.world.Hud;
import celestial.world.Overworld;

/** Title, How to Play, and choosing Araw (boy) or Buwan (girl). */
public class TitleScreen extends Screen {

	private static final Font BIG = Assets.font(54f);
	private static final Font MID = Assets.font(22f);
	private static final Font SMALL = Assets.font(16f);
	private static final String[] MENU = { "SIMULAN (Start)", "PAANO MAGLARO (How to Play)" };

	private int sel;
	private int stage;   // 0 menu, 1 hero choice, 2 how to play

	public TitleScreen(Game game) {
		super(game);
	}

	@Override
	public void enter() { Sound.music("town"); }

	@Override
	public boolean opaque() { return true; }

	@Override
	public void update() {
		if (stage == 2) {
			if (game.input.pressed(Key.CONFIRM) || game.input.pressed(Key.CANCEL)) stage = 0;
			return;
		}
		int n = stage == 0 ? MENU.length : 2;
		if (game.input.pressed(Key.UP) || game.input.pressed(Key.LEFT)) { sel = (sel + n - 1) % n; Sound.blip(); }
		if (game.input.pressed(Key.DOWN) || game.input.pressed(Key.RIGHT)) { sel = (sel + 1) % n; Sound.blip(); }
		if (stage == 1 && game.input.pressed(Key.CANCEL)) { stage = 0; sel = 0; }
		if (!game.input.pressed(Key.CONFIRM)) return;
		Sound.tok();
		if (stage == 0) {
			if (sel == 0) { stage = 1; sel = 0; }
			else stage = 2;
		} else {
			start(sel == 0);
		}
	}

	/** Starts a new game as Araw (boy) or Buwan (girl). */
	public void start(boolean boy) {
		Story s = new Story();
		s.boy = boy;
		s.heroName = boy ? "Araw" : "Buwan";
		s.hasParol = true;
		game.story = s;
		Overworld w = new Overworld(game);
		game.reset(w);
		int[] p = celestial.world.MapGrids.POINTS.get("BARRIO.start");
		w.loadMap("BARRIO", p[0], p[1], "down");
	}

	@Override
	public void render(Graphics2D g) {
		g.setPaint(new GradientPaint(0, 0, new Color(8, 8, 32), 0, Game.H, new Color(40, 30, 80)));
		g.fillRect(0, 0, Game.W, Game.H);
		g.setPaint(null);
		for (int i = 0; i < 120; i++) {
			int x = (i * 211) % Game.W, y = (i * 97) % (Game.H - 160);
			boolean tw = (game.tick / 12 + i) % 9 == 0;
			g.setColor(new Color(255, 255, 255, tw ? 255 : 140));
			g.fillRect(x, y, tw ? 3 : 2, tw ? 3 : 2);
		}
		Hud.moon(g, Game.W / 2, 150, 70, 0, false, game.tick);
		// barrio silhouette
		g.setColor(new Color(12, 12, 26));
		g.fillRect(0, Game.H - 120, Game.W, 120);
		for (int i = 0; i < 7; i++) {
			int x = 40 + i * 150;
			g.fillPolygon(new int[] { x, x + 60, x + 120 }, new int[] { Game.H - 120, Game.H - 175, Game.H - 120 }, 3);
		}
		g.drawImage(Assets.scaled(Assets.cg("obj_parol.png"), 48, 48), 120, Game.H - 230, null);
		Text.center(g, "CELESTIAL GROUNDS", Game.W / 2, 300, BIG, new Color(255, 230, 160), true);
		Text.center(g, "Hayop-Diwata ng Lupa, Apoy at Tubig", Game.W / 2, 344, SMALL, new Color(200, 200, 240), false);

		if (stage == 0) {
			for (int i = 0; i < MENU.length; i++) {
				Color c = i == sel ? Text.GOLD : new Color(220, 220, 240);
				Text.center(g, (i == sel ? "> " : "") + MENU[i], Game.W / 2, 430 + i * 50, MID, c, true);
			}
			Text.center(g, "Arrows/WASD move   Enter/Z select   Esc back", Game.W / 2, Game.H - 30, SMALL, new Color(170, 170, 200), false);
		} else if (stage == 1) {
			Text.center(g, "Who are you?", Game.W / 2, 410, MID, Color.WHITE, true);
			String[] names = { "ARAW", "BUWAN" };
			String[] sheets = { "araw", "buwan" };
			for (int i = 0; i < 2; i++) {
				int cx = Game.W / 2 + (i == 0 ? -150 : 150);
				if (i == sel) {
					g.setColor(new Color(244, 196, 72, 90));
					g.fillRoundRect(cx - 90, 430, 180, 190, 20, 20);
				}
				g.drawImage(Assets.scaled(CharSheet.of(sheets[i]).frame("down", 0), 128, 128), cx - 64, 440, null);
				Text.center(g, names[i], cx, 600, MID, i == sel ? Text.GOLD : Color.WHITE, true);
			}
			Text.center(g, "Age 11. Afraid of the dark.", Game.W / 2, 660, SMALL, new Color(200, 200, 230), false);
		} else {
			Text.panel(g, 120, 380, Game.W - 240, 320, new Color(250, 246, 236, 245), Text.INK);
			String[] lines = {
					"Move: Arrow keys / WASD.  Interact: Enter / Z / Space.",
					"Team: P.   Hayop-Journal: J.   Back: Esc.",
					"Battle: LABAN fight, PANDESAL heal or calm,",
					"BANGA befriend, PALIT switch, TAKBO run, HINT (Tala).",
					"Fire beats Earth, Earth beats Water, Water beats Fire.",
					"To befriend: weaken below half, offer a Pandesal, use the Banga.",
					"Banga Shrines heal your team and refill Pandesal.",
					"Your light grows each time you face the dark (Tapang)." };
			for (int i = 0; i < lines.length; i++) Text.draw(g, lines[i], 150, 424 + i * 36, SMALL, Text.INK);
		}
	}
}
