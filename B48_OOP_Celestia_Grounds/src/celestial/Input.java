package celestial;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.EnumSet;

/** Keyboard state, polled once per frame. Tests can call {@link #inject(Key)}. */
public class Input implements KeyListener {

	public enum Key { UP, DOWN, LEFT, RIGHT, CONFIRM, CANCEL, PARTY, JOURNAL, DEBUG }

	private final EnumSet<Key> held = EnumSet.noneOf(Key.class);
	private final EnumSet<Key> pending = EnumSet.noneOf(Key.class);
	private final EnumSet<Key> pressed = EnumSet.noneOf(Key.class);
	private final EnumSet<Key> injectedHeld = EnumSet.noneOf(Key.class);

	public static Key map(int code) {
		switch (code) {
			case KeyEvent.VK_UP: case KeyEvent.VK_W: return Key.UP;
			case KeyEvent.VK_DOWN: case KeyEvent.VK_S: return Key.DOWN;
			case KeyEvent.VK_LEFT: case KeyEvent.VK_A: return Key.LEFT;
			case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: return Key.RIGHT;
			case KeyEvent.VK_ENTER: case KeyEvent.VK_SPACE: case KeyEvent.VK_Z: case KeyEvent.VK_X: return Key.CONFIRM;
			case KeyEvent.VK_ESCAPE: case KeyEvent.VK_BACK_SPACE: case KeyEvent.VK_C: return Key.CANCEL;
			case KeyEvent.VK_P: return Key.PARTY;
			case KeyEvent.VK_J: return Key.JOURNAL;
			case KeyEvent.VK_F12: return Key.DEBUG;
			default: return null;
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		Key k = map(e.getKeyCode());
		if (k != null) {
			if (!held.contains(k)) pending.add(k);
			held.add(k);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		Key k = map(e.getKeyCode());
		if (k != null) held.remove(k);
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	/** Called at the start of every logic frame. */
	public void beginFrame() {
		pressed.clear();
		pressed.addAll(pending);
		pending.clear();
	}

	public boolean pressed(Key k) { return pressed.contains(k); }

	public boolean held(Key k) { return held.contains(k) || injectedHeld.contains(k); }

	/** Swallow the current presses so a screen change doesn't double-fire. */
	public void consume() { pressed.clear(); }

	// --- for automated tests -------------------------------------------
	public void inject(Key k) { pending.add(k); }
	public void injectHold(Key k, boolean down) { if (down) injectedHeld.add(k); else injectedHeld.remove(k); }
	public void releaseAll() { held.clear(); injectedHeld.clear(); }
}
