package celestial;

import java.awt.Graphics2D;

/** A layer on the screen stack (overworld, battle, dialogue box, menus...). */
public abstract class Screen {

	protected final Game game;
	private boolean closed;

	protected Screen(Game game) {
		this.game = game;
	}

	/** Called once when pushed. */
	public void enter() {}

	/** Only the top screen is updated. */
	public abstract void update();

	public abstract void render(Graphics2D g);

	/** If true, screens below this one are not drawn. */
	public boolean opaque() { return false; }

	public void close() {
		closed = true;
	}

	public boolean isClosed() { return closed; }
}
