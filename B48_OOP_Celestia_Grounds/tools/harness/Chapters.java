import celestial.*;
import celestial.data.Species;
import celestial.story.Scenes;

/** Jumps to each debug chapter, then lets the bot finish the game. */
public class Chapters {
	public static void main(String[] a) throws Exception {
		Sound.disable();
		int ch = Integer.parseInt(a[0]);
		Species st = new Species[] { Species.TARSI, Species.BINTURO, Species.DUGONG }[Integer.parseInt(a[1])];
		Bot.out = "/tmp/chap"; new java.io.File(Bot.out).mkdirs();
		Bot.g = new Game();
		Bot.g.update();
		Scenes.jump(Bot.g, ch, st);
		Bot.buildObjectives();
		Screen last = null;
		while (Bot.frame < 200000) {
			Screen top = Bot.g.top();
			if (top instanceof celestial.ui.TitleScreen) break;
			Bot.drive(top);
			Bot.g.update(); Bot.frame++;
		}
		System.out.println("chapter " + ch + " " + st.label + ": ended=" + Bot.g.story.has("ending") + " frames=" + Bot.frame + (Bot.g.story.has("ending") ? "" : " stuck at " + Bot.current()));
	}
}
