package celestial.world;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import celestial.Assets;

/** 96x128 walking sheet (rows down, up, left, right; 3 frames) shown at 2x. */
public class CharSheet {

	private static final Map<String, CharSheet> cache = new HashMap<>();
	private final BufferedImage[][] frames = new BufferedImage[4][3];

	private CharSheet(String name) {
		BufferedImage src = Assets.cg("char_" + name + ".png");
		for (int r = 0; r < 4; r++)
			for (int c = 0; c < 3; c++) {
				BufferedImage cell = src.getSubimage(c * 32, r * 32, 32, 32);
				frames[r][c] = Assets.scaled(cell, 64, 64);
			}
	}

	public static CharSheet of(String name) {
		return cache.computeIfAbsent(name, CharSheet::new);
	}

	public static int row(String dir) {
		switch (dir) {
			case "up": return 1;
			case "left": return 2;
			case "right": return 3;
			default: return 0;
		}
	}

	/** frame 0 = standing, 1/2 = steps. */
	public BufferedImage frame(String dir, int f) {
		return frames[row(dir)][f];
	}
}
