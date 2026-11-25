import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import celestial.*;
import celestial.Input.Key;

/** Renders the game headless to PNGs. Usage: Shots <outdir> */
public class Shots {
	static Game g; static String out;
	static void step(int n){ for(int i=0;i<n;i++) g.update(); }
	static void press(Key k){ g.input.inject(k); step(1); }
	static void shot(String name) throws Exception {
		BufferedImage im = new BufferedImage(Game.W, Game.H, BufferedImage.TYPE_INT_RGB);
		Graphics2D gr = im.createGraphics(); g.render(gr); gr.dispose();
		ImageIO.write(im, "png", new File(out, name + ".png"));
	}
	public static void main(String[] a) throws Exception {
		Sound.disable(); out = a[0]; new File(out).mkdirs();
		g = new Game(); step(5); shot("00_title");
		press(Key.CONFIRM); step(3); shot("01_hero");
		press(Key.CONFIRM); step(30); shot("02_scene1_narration");
		for (int i=0;i<12;i++){ press(Key.CONFIRM); step(25);} shot("03_porch");
	}
}
