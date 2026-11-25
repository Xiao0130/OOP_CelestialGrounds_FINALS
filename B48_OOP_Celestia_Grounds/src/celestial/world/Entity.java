package celestial.world;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.function.Function;

import celestial.Assets;
import celestial.Game;
import celestial.story.Script;

/** Anything standing on the map: the hero, NPCs, overworld creatures and props. */
public class Entity {

	public final String id;
	public int tx, ty;             // tile position
	public double px, py;          // pixel position (world)
	public String dir = "down";
	public CharSheet sheet;        // walking character
	public BufferedImage image;    // or a still image (creature, prop)
	public int imgW, imgH;         // draw size for still images
	public boolean solid = true;
	public boolean visible = true;
	public boolean flipX;
	public boolean bob;            // gentle floating (Bituin, Haribon, Bakunawa)
	public boolean noShadow;
	public double speed = 4;       // pixels per frame
	public Function<Game, Script> onTalk;
	public double light;           // light radius in tiles (0 = none)
	public Color lightColor = new Color(255, 200, 120);
	public int drawOffsetY;        // e.g. Dali on the jeepney roof
	public int drawOffsetX;
	public boolean overhead;       // drawn above everything (gate arch)
	public float alpha = 1f;

	private final LinkedList<String> steps = new LinkedList<>();
	private int stepPixels;
	private int animTick;
	private String movingDir;

	public Entity(String id, int tx, int ty) {
		this.id = id;
		place(tx, ty);
	}

	public static Entity person(String id, String sheet, int tx, int ty, String dir) {
		Entity e = new Entity(id, tx, ty);
		e.sheet = CharSheet.of(sheet);
		e.dir = dir;
		return e;
	}

	public static Entity still(String id, BufferedImage img, int w, int h, int tx, int ty) {
		Entity e = new Entity(id, tx, ty);
		e.image = img;
		e.imgW = w;
		e.imgH = h;
		return e;
	}

	public Entity talk(Function<Game, Script> f) {
		onTalk = f;
		return this;
	}

	public void place(int x, int y) {
		tx = x;
		ty = y;
		px = x * Game.TILE;
		py = y * Game.TILE;
		steps.clear();
		movingDir = null;
	}

	public void queueSteps(String d, int n) {
		for (int i = 0; i < n; i++) steps.add(d);
	}

	public boolean busy() {
		return movingDir != null || !steps.isEmpty();
	}

	public boolean moving() { return movingDir != null; }

	/** Starts a single step (used for the hero after collision checks). */
	public void stepNow(String d) {
		dir = d;
		movingDir = d;
		stepPixels = 0;
		tx += dx(d);
		ty += dy(d);
	}

	/** @return true on the frame a step finishes. */
	public boolean update() {
		animTick++;
		if (movingDir == null && !steps.isEmpty()) stepNow(steps.poll());
		if (movingDir == null) return false;
		double sp = speed;
		stepPixels += sp;
		px += dx(movingDir) * sp;
		py += dy(movingDir) * sp;
		if (stepPixels >= Game.TILE) {
			px = tx * Game.TILE;
			py = ty * Game.TILE;
			movingDir = null;
			if (!steps.isEmpty()) stepNow(steps.poll());
			return true;
		}
		return false;
	}

	public void lookAt(Entity o) {
		int ddx = o.tx - tx, ddy = o.ty - ty;
		if (Math.abs(ddx) > Math.abs(ddy)) dir = ddx > 0 ? "right" : "left";
		else if (ddy != 0) dir = ddy > 0 ? "down" : "up";
	}

	public static int dx(String d) { return "left".equals(d) ? -1 : "right".equals(d) ? 1 : 0; }

	public static int dy(String d) { return "up".equals(d) ? -1 : "down".equals(d) ? 1 : 0; }

	public static String opposite(String d) {
		switch (d) {
			case "up": return "down";
			case "down": return "up";
			case "left": return "right";
			default: return "left";
		}
	}

	/** Bottom of the sprite in world pixels, used for y-sorting. */
	public double footY() { return py + Game.TILE; }

	public void draw(Graphics2D g, int camX, int camY, long tick) {
		if (!visible) return;
		int x = (int) px - camX + drawOffsetX, y = (int) py - camY + drawOffsetY;
		java.awt.Composite oldC = g.getComposite();
		if (alpha < 1f) g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
		int bobY = bob ? (int) Math.round(Math.sin((tick + id.hashCode() % 50) / 14.0) * 4) : 0;
		if (!noShadow) {
			g.setColor(new Color(0, 0, 0, 60));
			int sw = sheet != null ? 34 : Math.min(imgW - 8, 90);
			g.fillOval(x + Game.TILE / 2 - sw / 2, y + Game.TILE - 10, sw, 10);
		}
		if (sheet != null) {
			int f = 0;
			if (movingDir != null) f = ((animTick / 8) % 2) + 1;
			g.drawImage(sheet.frame(dir, f), x - 8, y - 18 + bobY, null);
		} else if (image != null) {
			BufferedImage im = Assets.scaled(image, imgW, imgH);
			if (flipX) im = Assets.flipped(im);
			g.drawImage(im, x + Game.TILE / 2 - imgW / 2, y + Game.TILE - imgH + bobY, null);
		}
		g.setComposite(oldC);
	}
}
