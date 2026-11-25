package celestial.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.List;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Text;

/** Full-screen story cards: the seven-moon sky, the closing quote, the credits. */
public class CardScreen extends Screen {

	public enum Kind { SKY, QUOTE, CREDITS, TRAVEL }

	private static final Font BIG = Assets.font(24f);
	private static final Font MID = Assets.font(18f);
	private final Kind kind;
	private final String text;
	private int t;

	public CardScreen(Game game, Kind kind, String text) {
		super(game);
		this.kind = kind;
		this.text = text == null ? null : game.story.sub(text);
	}

	private float prevFade, prevTarget;

	@Override
	public boolean opaque() { return true; }

	/** Cards fade in on their own, so lift any black fade while they are shown. */
	@Override
	public void enter() {
		prevFade = game.fade;
		prevTarget = game.fadeTarget;
		game.fade = 0;
		game.fadeTarget = 0;
	}

	@Override
	public void update() {
		t++;
		if (t > 70 && game.input.pressed(Key.CONFIRM)) {
			game.fade = prevFade;
			game.fadeTarget = prevTarget;
			close();
		}
	}

	@Override
	public void render(Graphics2D g) {
		float a = Math.min(1f, t / 60f);
		if (kind == Kind.SKY) {
			g.setPaint(new GradientPaint(0, 0, new Color(6, 8, 30), 0, Game.H, new Color(30, 30, 70)));
			g.fillRect(0, 0, Game.W, Game.H);
			g.setPaint(null);
			for (int i = 0; i < 140; i++) {
				g.setColor(new Color(255, 255, 255, 120 + (i * 37) % 120));
				g.fillRect((i * 173) % Game.W, (i * 89) % 460, 2, 2);
			}
			int[][] moons = { { 160, 170, 34 }, { 330, 110, 26 }, { 500, 150, 44 }, { 660, 90, 30 }, { 820, 160, 36 }, { 950, 230, 22 }, { 420, 260, 20 } };
			for (int i = 0; i < moons.length; i++) {
				int rise = (int) Math.max(0, (1 - Math.min(1, (t - i * 12) / 60.0)) * 200);
				int[] m = moons[i];
				int gr = m[2] * 2;
				g.setPaint(new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Double(m[0], m[1] + rise), gr, new float[] { 0f, 1f },
						new Color[] { new Color(255, 248, 210, 110), new Color(255, 248, 210, 0) }));
				g.fillOval(m[0] - gr, m[1] - gr + rise, gr * 2, gr * 2);
				g.setPaint(null);
				g.setColor(new Color(252, 246, 214));
				g.fillOval(m[0] - m[2], m[1] - m[2] + rise, m[2] * 2, m[2] * 2);
			}
			// barrio roofs and the porch
			g.setColor(new Color(12, 12, 24));
			g.fillRect(0, Game.H - 170, Game.W, 170);
			for (int i = 0; i < 6; i++) {
				int x = 20 + i * 180;
				g.fillPolygon(new int[] { x, x + 80, x + 160 }, new int[] { Game.H - 170, Game.H - 240, Game.H - 170 }, 3);
			}
			g.drawImage(Assets.scaled(Assets.cg("obj_parol.png"), 40, 40), 300, Game.H - 120, null);
		} else {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, Game.W, Game.H);
			if (kind == Kind.QUOTE) {
				int r = 6 + (int) (Math.sin(t / 20.0) * 2);
				g.setColor(new Color(255, 230, 160, (int) (200 * a)));
				g.fillOval(Game.W / 2 - r, 200 - r, r * 2, r * 2);
			}
		}
		if (text != null) {
			Font f = kind == Kind.QUOTE ? BIG : MID;
			g.setFont(f);
			List<String> lines = Text.wrap(text, g.getFontMetrics(), Game.W - 220);
			int y0 = kind == Kind.SKY ? Game.H - 60 - lines.size() * 36 : Game.H / 2 - lines.size() * 22 + 20;
			if (kind == Kind.SKY) {
				g.setColor(new Color(0, 0, 0, 140));
				g.fillRect(0, y0 - 40, Game.W, lines.size() * 40 + 30);
			}
			for (int i = 0; i < lines.size(); i++)
				Text.center(g, lines.get(i), Game.W / 2, y0 + i * (kind == Kind.QUOTE ? 46 : 38), f,
						new Color(255, 240, 210, (int) (255 * a)), false);
		}
		if (t > 70) Text.nextArrow(g, Game.W - 60, Game.H - 50, game.tick, new Color(200, 200, 220));
	}
}
