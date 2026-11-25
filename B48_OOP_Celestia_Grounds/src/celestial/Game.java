package celestial;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import celestial.story.Cutscene;
import celestial.story.Script;
import celestial.story.Story;
import celestial.ui.TitleScreen;
import celestial.world.Overworld;

/** Holds the screen stack, the running cutscene and the saved story state. */
public class Game {

	public static final int W = 1080, H = 720, TILE = 48;

	public final Input input = new Input();
	public Story story = new Story();
	public Overworld world;
	public long tick;

	private final List<Screen> screens = new ArrayList<>();
	private Cutscene cutscene;
	private final List<Script> queued = new ArrayList<>();

	// screen fade (0 = clear, 1 = black)
	public float fade = 0f, fadeTarget = 0f;
	public Color fadeColor = Color.BLACK;

	public Game() {
		push(new TitleScreen(this));
	}

	// ------------------------------------------------------------------ screens
	public void push(Screen s) {
		screens.add(s);
		s.enter();
	}

	public Screen top() {
		return screens.isEmpty() ? null : screens.get(screens.size() - 1);
	}

	public List<Screen> screens() { return screens; }

	/** Replace everything with a single base screen. */
	public void reset(Screen base) {
		screens.clear();
		cutscene = null;
		queued.clear();
		push(base);
	}

	// ------------------------------------------------------------------ cutscenes
	public void run(Script s) {
		if (cutscene == null) cutscene = new Cutscene(s);
		else queued.add(s);
	}

	public boolean inCutscene() { return cutscene != null; }

	public Cutscene cutscene() { return cutscene; }

	// ------------------------------------------------------------------ loop
	public void update() {
		tick++;
		input.beginFrame();
		Screen top = top();
		if (top != null) top.update();
		screens.removeIf(Screen::isClosed);
		if (cutscene != null && cutscene.update(this)) {
			cutscene = queued.isEmpty() ? null : new Cutscene(queued.remove(0));
		}
		screens.removeIf(Screen::isClosed);
		if (fade < fadeTarget) fade = Math.min(fadeTarget, fade + 0.05f);
		else if (fade > fadeTarget) fade = Math.max(fadeTarget, fade - 0.05f);
	}

	public void render(Graphics2D g) {
		Text.aa(g);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, W, H);
		int start = 0;
		for (int i = screens.size() - 1; i >= 0; i--) {
			if (screens.get(i).opaque()) { start = i; break; }
		}
		for (int i = start; i < screens.size(); i++) {
			screens.get(i).render(g);
		}
		if (fade > 0) {
			g.setColor(new Color(fadeColor.getRed(), fadeColor.getGreen(), fadeColor.getBlue(), (int) (fade * 255)));
			g.fillRect(0, 0, W, H);
		}
	}
}
