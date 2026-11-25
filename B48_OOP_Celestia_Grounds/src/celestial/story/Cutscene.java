package celestial.story;

import java.util.LinkedList;

import celestial.Game;

/** Runs a Script one command at a time. Commands can insert more commands (branches, battle results). */
public class Cutscene {

	private final LinkedList<Cmd> queue = new LinkedList<>();
	private Cmd current;

	public Cutscene(Script s) {
		queue.addAll(s.cmds);
	}

	/** Puts a script's commands right after the current one. */
	public void insert(Script s) {
		queue.addAll(0, s.cmds);
	}

	/** @return true when everything has finished. */
	public boolean update(Game g) {
		int guard = 0;
		while (guard++ < 200) {
			if (current == null) {
				if (queue.isEmpty()) return true;
				current = queue.poll();
				current.start(g);
			}
			if (current.update(g)) {
				current = null;
				continue;
			}
			return false;
		}
		return false;
	}
}
