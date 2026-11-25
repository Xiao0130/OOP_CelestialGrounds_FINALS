package celestial.story;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import celestial.Game;
import celestial.Screen;
import celestial.battle.Battle;
import celestial.battle.BattleSetup;
import celestial.ui.ChoiceBox;
import celestial.ui.Popup;
import celestial.ui.PromptScreen;
import celestial.ui.TextBox;
import celestial.world.Entity;

/** Fluent builder for cutscenes. Every scene in Scenes.java is written with this. */
public class Script {

	final List<Cmd> cmds = new ArrayList<>();

	public Script add(Cmd c) {
		cmds.add(c);
		return this;
	}

	public Script append(Script other) {
		if (other != null) cmds.addAll(other.cmds);
		return this;
	}

	public boolean isEmpty() { return cmds.isEmpty(); }

	// ------------------------------------------------------------------ helpers
	/** Pushes a screen and waits until it closes. */
	public Script screen(Function<Game, Screen> make) {
		return add(new Cmd() {
			Screen s;

			public void start(Game g) {
				s = make.apply(g);
				g.push(s);
			}

			public boolean update(Game g) { return s.isClosed(); }
		});
	}

	public Script say(String speaker, String text) {
		return screen(g -> new TextBox(g, speaker, text, TextBox.Style.SPEECH));
	}

	/** Stage directions from the script, shown as narration. */
	public Script narrate(String text) {
		return screen(g -> new TextBox(g, null, text, TextBox.Style.NARRATION));
	}

	public Script system(String text) {
		return screen(g -> new TextBox(g, "SYSTEM", text, TextBox.Style.SYSTEM));
	}

	public Script popup(String title, String text) {
		return screen(g -> new Popup(g, title, text));
	}

	public Script tutorial(String text) { return popup("TUTORIAL", text); }

	public Script prompt(String text) {
		return screen(g -> new PromptScreen(g, text));
	}

	/** A choice whose answer picks the next part of the script. */
	public Script choice(String question, String[] options, IntFunction<Script> after) {
		final int[] picked = { -1 };
		screen(g -> new ChoiceBox(g, question, options, i -> picked[0] = i));
		return then(g -> after.apply(picked[0]));
	}

	public Script run(Consumer<Game> action) {
		return add(g -> {
			action.accept(g);
			return true;
		});
	}

	/** Builds more script when this point is reached (for branches that depend on state). */
	public Script then(Function<Game, Script> more) {
		return add(new Cmd() {
			public boolean update(Game g) {
				Script s = more.apply(g);
				if (s != null && g.cutscene() != null) g.cutscene().insert(s);
				return true;
			}
		});
	}

	public Script when(Predicate<Game> cond, Supplier<Script> yes) {
		return then(g -> cond.test(g) ? yes.get() : null);
	}

	public Script wait(int frames) {
		return add(new Cmd() {
			int t;

			public void start(Game g) { t = 0; }

			public boolean update(Game g) { return ++t >= frames; }
		});
	}

	public Script fadeOut() {
		return add(new Cmd() {
			public void start(Game g) { g.fadeTarget = 1f; }

			public boolean update(Game g) { return g.fade >= 1f; }
		});
	}

	public Script fadeIn() {
		return add(new Cmd() {
			public void start(Game g) { g.fadeTarget = 0f; }

			public boolean update(Game g) { return g.fade <= 0f; }
		});
	}

	// ------------------------------------------------------------------ world
	public Script walk(String id, String dir, int steps) {
		return add(new Cmd() {
			public void start(Game g) {
				Entity e = g.world.find(id);
				if (e != null) e.queueSteps(dir, steps);
			}

			public boolean update(Game g) {
				Entity e = g.world.find(id);
				return e == null || !e.busy();
			}
		});
	}

	public Script walkAsync(String id, String dir, int steps) {
		return run(g -> {
			Entity e = g.world.find(id);
			if (e != null) e.queueSteps(dir, steps);
		});
	}

	/** Walks along x first then y to the target tile (no pathfinding; for open ground). */
	public Script walkTo(String id, int tx, int ty) {
		return then(g -> {
			Entity e = g.world.find(id);
			Script s = new Script();
			if (e == null) return s;
			int dx = tx - e.tx, dy = ty - e.ty;
			if (dx != 0) s.walk(id, dx > 0 ? "right" : "left", Math.abs(dx));
			if (dy != 0) s.walk(id, dy > 0 ? "down" : "up", Math.abs(dy));
			return s;
		});
	}

	public Script face(String id, String dir) {
		return run(g -> {
			Entity e = g.world.find(id);
			if (e != null) e.dir = dir;
		});
	}

	/** Makes two entities look at each other. */
	public Script faceEach(String a, String b) {
		return run(g -> {
			Entity ea = g.world.find(a), eb = g.world.find(b);
			if (ea != null && eb != null) {
				ea.lookAt(eb);
				eb.lookAt(ea);
			}
		});
	}

	public Script spawn(Supplier<Entity> make) {
		return run(g -> g.world.spawn(make.get()));
	}

	public Script despawn(String id) {
		return run(g -> g.world.remove(id));
	}

	public Script teleport(String map, int x, int y, String dir) {
		return fadeOut().run(g -> g.world.loadMap(map, x, y, dir)).fadeIn();
	}

	public Script banner(String text) {
		return run(g -> g.world.banner(text));
	}

	public Script shake(int frames) {
		return run(g -> g.world.shake(frames));
	}

	// ------------------------------------------------------------------ battle
	public Script battle(Supplier<BattleSetup> setup, Function<Battle.Result, Script> after) {
		final Battle[] holder = new Battle[1];
		screen(g -> holder[0] = new Battle(g, setup.get()));
		return then(g -> after == null ? null : after.apply(holder[0].result()));
	}

	// ------------------------------------------------------------------ story
	public Script set(String flag) { return run(g -> g.story.set(flag)); }

	public Script tapang(int level, String message) {
		run(g -> {
			g.story.tapang = Math.max(g.story.tapang, level);
			g.world.lightPulse();
			celestial.Sound.tone(523, 160, 0, 0.3, celestial.Sound.Wave.CHIME);
			celestial.Sound.tone(784, 300, 0, 0.3, celestial.Sound.Wave.CHIME);
		});
		return system(message);
	}
}
