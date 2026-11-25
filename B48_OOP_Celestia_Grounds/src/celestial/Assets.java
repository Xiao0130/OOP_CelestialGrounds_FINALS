package celestial;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/** Loads and caches images and fonts. All paths are relative to the project folder (like the rest of PlayPokemon). */
public final class Assets {

	private static final Map<String, BufferedImage> images = new HashMap<>();
	private static final Map<Float, Font> fonts = new HashMap<>();
	private static Font base;
	private static final Map<String, BufferedImage> scaled = new HashMap<>();

	private Assets() {}

	public static BufferedImage img(String path) {
		BufferedImage im = images.get(path);
		if (im == null) {
			try {
				BufferedImage raw = ImageIO.read(new File(path));
				im = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = im.createGraphics();
				g.drawImage(raw, 0, 0, null);
				g.dispose();
			} catch (Exception e) {
				System.err.println("[Celestial] missing image " + path);
				im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			}
			images.put(path, im);
		}
		return im;
	}

	/** Image from res/celestial. */
	public static BufferedImage cg(String name) {
		return img("res/celestial/" + name);
	}

	/** Nearest-neighbour scaled copy, cached. */
	public static BufferedImage scaled(BufferedImage src, int w, int h) {
		String key = System.identityHashCode(src) + ":" + w + "x" + h;
		BufferedImage out = scaled.get(key);
		if (out == null) {
			out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = out.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.drawImage(src, 0, 0, w, h, null);
			g.dispose();
			scaled.put(key, out);
		}
		return out;
	}

	public static BufferedImage flipped(BufferedImage src) {
		String key = "flip:" + System.identityHashCode(src);
		BufferedImage out = scaled.get(key);
		if (out == null) {
			out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = out.createGraphics();
			g.drawImage(src, src.getWidth(), 0, -src.getWidth(), src.getHeight(), null);
			g.dispose();
			scaled.put(key, out);
		}
		return out;
	}

	/** Solid-colour silhouette (used for unseen journal entries and the befriend flash). */
	public static BufferedImage silhouette(BufferedImage src, int argb) {
		String key = "sil:" + System.identityHashCode(src) + ":" + argb;
		BufferedImage out = scaled.get(key);
		if (out == null) {
			out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < src.getHeight(); y++)
				for (int x = 0; x < src.getWidth(); x++)
					if ((src.getRGB(x, y) >>> 24) > 0) out.setRGB(x, y, argb);
			scaled.put(key, out);
		}
		return out;
	}

	public static Font font(float size) {
		Font f = fonts.get(size);
		if (f == null) {
			if (base == null) {
				try {
					base = Font.createFont(Font.TRUETYPE_FONT, new File("res/PokemonGb-RAeo.ttf"));
					GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(base);
				} catch (Exception e) {
					base = new Font(Font.MONOSPACED, Font.BOLD, 12);
				}
			}
			f = base.deriveFont(size);
			fonts.put(size, f);
		}
		return f;
	}
}
