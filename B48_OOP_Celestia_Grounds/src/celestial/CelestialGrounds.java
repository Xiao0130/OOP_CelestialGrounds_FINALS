package celestial;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * CELESTIAL GROUNDS - entry point.
 * Runs inside the original PlayPokemon Eclipse project; launch this class (or main.Main).
 * Working directory must be the project folder so that res/ is found.
 */
@SuppressWarnings("serial")
public class CelestialGrounds extends JPanel {

	private final Game game = new Game();

	public CelestialGrounds() {
		setPreferredSize(new Dimension(Game.W, Game.H));
		setFocusable(true);
		addKeyListener(game.input);
		setDoubleBuffered(true);
		Timer timer = new Timer(16, e -> {
			game.update();
			repaint();
		});
		timer.setCoalesce(true);
		timer.start();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		game.render((Graphics2D) g);
	}

	public static void main(String[] args) {
		Sound.init();
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Celestial Grounds");
			CelestialGrounds panel = new CelestialGrounds();
			frame.setContentPane(panel);
			frame.setResizable(false);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			panel.requestFocusInWindow();
		});
	}
}
