package celestial.story;

import celestial.Game;

/** One step of a cutscene. update() returns true when the step is finished. */
public interface Cmd {
	default void start(Game g) {}

	boolean update(Game g);
}
