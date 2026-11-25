package celestial.world;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import celestial.Game;

/** Darkness overlay with soft holes cut out for every light (the Tapang light circle, fireflies, lamps). */
public class Lighting {

	public static class Glow {
		final double x, y, r;
		final Color color;
		final double strength;

		public Glow(double x, double y, double r, Color color, double strength) {
			this.x = x; this.y = y; this.r = r; this.color = color; this.strength = strength;
		}
	}

	private final BufferedImage overlay = new BufferedImage(Game.W, Game.H, BufferedImage.TYPE_INT_ARGB);
	private final List<Glow> glows = new ArrayList<>();

	public void clear() { glows.clear(); }

	public void add(double screenX, double screenY, double radiusPx, Color c, double strength) {
		if (radiusPx > 1) glows.add(new Glow(screenX, screenY, radiusPx, c, strength));
	}

	public void render(Graphics2D g, double darkness, Color night) {
		if (darkness <= 0.01) return;
		Graphics2D o = overlay.createGraphics();
		o.setComposite(AlphaComposite.Src);
		o.setColor(new Color(night.getRed(), night.getGreen(), night.getBlue(), (int) (Math.min(1, darkness) * 255)));
		o.fillRect(0, 0, Game.W, Game.H);
		o.setComposite(AlphaComposite.DstOut);
		for (Glow gl : glows) {
			float a = (float) Math.max(0, Math.min(1, gl.strength));
			RadialGradientPaint paint = new RadialGradientPaint(new Point2D.Double(gl.x, gl.y), (float) gl.r,
					new float[] { 0f, 0.55f, 1f },
					new Color[] { new Color(0, 0, 0, a), new Color(0, 0, 0, a * 0.8f), new Color(0, 0, 0, 0f) });
			o.setPaint(paint);
			o.fillOval((int) (gl.x - gl.r), (int) (gl.y - gl.r), (int) (gl.r * 2), (int) (gl.r * 2));
		}
		o.dispose();
		g.drawImage(overlay, 0, 0, null);
		// warm colour wash inside each light
		Composite old = g.getComposite();
		for (Glow gl : glows) {
			if (gl.color == null) continue;
			RadialGradientPaint paint = new RadialGradientPaint(new Point2D.Double(gl.x, gl.y), (float) gl.r,
					new float[] { 0f, 1f },
					new Color[] { new Color(gl.color.getRed(), gl.color.getGreen(), gl.color.getBlue(), (int) (60 * Math.min(1, darkness + 0.2))),
							new Color(gl.color.getRed(), gl.color.getGreen(), gl.color.getBlue(), 0) });
			g.setPaint(paint);
			g.fillOval((int) (gl.x - gl.r), (int) (gl.y - gl.r), (int) (gl.r * 2), (int) (gl.r * 2));
		}
		g.setComposite(old);
	}
}
