package celestial.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Sound;
import celestial.Text;
import celestial.data.Creature;
import celestial.data.Element;

/**
 * The Bayanihan Drum: tap the beat 8 times in the dark. Each tap borrows a sound and glow
 * from a party member (stomp, hiss, splash) and every fourth beat is the song. No fail state.
 */
public class RhythmScreen extends Screen {

	public static final int BEAT = 40;          // frames per beat (90 BPM)
	private static final int WINDOW = 11;
	private static final Font BIG = Assets.font(24f);
	private static final Font SMALL = Assets.font(15f);

	private int t;
	private int hits;
	private int lastBeatHit = -1;
	private int doneTimer = -1;
	private String feedback = "";
	private int feedbackT;
	private final List<double[]> rings = new ArrayList<>();   // x, y, r, life, rgb

	public RhythmScreen(Game game) {
		super(game);
	}

	private int phaseInBeat() { return t % BEAT; }

	/** True when a tap now would land on the beat (used by the test bot). */
	public boolean nearBeat() {
		int p = phaseInBeat();
		return Math.min(p, BEAT - p) <= 3;
	}

	public int hits() { return hits; }

	@Override
	public void update() {
		t++;
		if (phaseInBeat() == 0) Sound.tone(110, 90, 0, 0.25, Sound.Wave.DRUM);
		if (feedbackT > 0) feedbackT--;
		for (double[] r : rings) { r[2] += 4; r[3] -= 1; }
		rings.removeIf(r -> r[3] <= 0);
		if (doneTimer >= 0) {
			if (++doneTimer > 70) close();
			return;
		}
		if (game.input.pressed(Key.CONFIRM)) {
			int p = phaseInBeat();
			int dist = Math.min(p, BEAT - p);
			int beatIndex = (t + BEAT / 2) / BEAT;
			if (dist <= WINDOW && beatIndex != lastBeatHit) {
				lastBeatHit = beatIndex;
				onHit();
			} else {
				feedback = "Listen... find the beat.";
				feedbackT = 40;
			}
		}
	}

	private void onHit() {
		hits++;
		List<Creature> party = game.story.party;
		Color c;
		String sound;
		if (hits % 4 == 0 || party.isEmpty()) {
			c = new Color(255, 236, 160);
			sound = "song";
			Sound.tone(523 + hits * 20, 300, 0, 0.3, Sound.Wave.CHIME);
		} else {
			Creature cr = party.get((hits - 1) % party.size());
			Element e = cr.element();
			c = e.color;
			if (e == Element.LUPA) { sound = "stomp"; Sound.tone(70, 160, 0, 0.5, Sound.Wave.DRUM); }
			else if (e == Element.APOY) { sound = "hiss"; Sound.tone(0, 220, 0.3, 0.25, Sound.Wave.NOISE); }
			else { sound = "splash"; Sound.tone(0, 260, -0.3, 0.3, Sound.Wave.NOISE); }
			sound = cr.name() + ": " + sound;
		}
		feedback = sound + "!";
		feedbackT = 30;
		rings.add(new double[] { Game.W / 2.0, 300, 20, 50, c.getRGB() & 0xFFFFFF });
		if (hits >= 8) doneTimer = 0;
	}

	@Override
	public void render(Graphics2D g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, Game.W, 528);
		double glow = 0.15 + hits / 8.0 * 0.85;
		int gr = (int) (60 + 260 * glow);
		g.setPaint(new RadialGradientPaint(new Point2D.Double(Game.W / 2.0, 300), gr, new float[] { 0f, 1f },
				new Color[] { new Color(255, 230, 160, (int) (230 * glow)), new Color(255, 230, 160, 0) }));
		g.fillOval(Game.W / 2 - gr, 300 - gr, gr * 2, gr * 2);
		g.setPaint(null);
		// incoming beat ring shrinks onto the light
		int p = phaseInBeat();
		int ringR = 30 + (BEAT - p) * 4;
		g.setStroke(new BasicStroke(4f));
		g.setColor(new Color(255, 255, 255, 160));
		g.drawOval(Game.W / 2 - ringR, 300 - ringR, ringR * 2, ringR * 2);
		g.setColor(new Color(255, 236, 170));
		g.drawOval(Game.W / 2 - 30, 270, 60, 60);
		for (int i = 0; i < rings.size(); i++) {
			double[] r = rings.get(i);
			Color c = new Color((int) r[4]);
			g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.max(0, Math.min(255, r[3] * 5))));
			g.drawOval((int) (r[0] - r[2]), (int) (r[1] - r[2]), (int) (r[2] * 2), (int) (r[2] * 2));
		}
		g.setStroke(new BasicStroke(1f));
		Text.center(g, "Tap the beat! (Enter)", Game.W / 2, 70, BIG, Color.WHITE, true);
		for (int i = 0; i < 8; i++) {
			g.setColor(i < hits ? new Color(255, 220, 120) : new Color(60, 60, 80));
			g.fillOval(Game.W / 2 - 150 + i * 40, 100, 22, 22);
		}
		if (feedbackT > 0) Text.center(g, feedback, Game.W / 2, 470, SMALL, new Color(255, 240, 200), true);
	}
}
