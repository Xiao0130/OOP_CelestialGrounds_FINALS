package celestial.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import java.util.function.IntConsumer;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Sound;
import celestial.Text;

/** A question in the dialogue box with a list of answers on the right. */
public class ChoiceBox extends Screen {

	private static final Font FONT = Assets.font(18f);
	private final String question;
	private final String[] options;
	private final IntConsumer onPick;
	private int sel;
	private List<String> qLines;

	public ChoiceBox(Game game, String question, String[] options, IntConsumer onPick) {
		super(game);
		this.question = question == null ? null : game.story.sub(question);
		this.options = options;
		for (int i = 0; i < options.length; i++) options[i] = game.story.sub(options[i]);
		this.onPick = onPick;
	}

	@Override
	public void update() {
		if (game.input.pressed(Key.UP)) { sel = (sel + options.length - 1) % options.length; Sound.blip(); }
		if (game.input.pressed(Key.DOWN)) { sel = (sel + 1) % options.length; Sound.blip(); }
		if (game.input.pressed(Key.CONFIRM)) {
			Sound.tok();
			onPick.accept(sel);
			close();
		}
	}

	@Override
	public void render(Graphics2D g) {
		g.setFont(FONT);
		int ow = 0;
		for (String o : options) ow = Math.max(ow, g.getFontMetrics().stringWidth(Text.safe(o)));
		ow += 80;
		int oh = options.length * 42 + 30;
		int ox = Game.W - ow - 30;
		int oy = Game.H - 210 - oh;
		if (question != null) {
			Text.panel(g, 24, Game.H - 196, Game.W - 48, 176);
			if (qLines == null) {
				qLines = Text.wrap(question, g.getFontMetrics(), Game.W - 120);
			}
			for (int i = 0; i < Math.min(3, qLines.size()); i++)
				Text.draw(g, qLines.get(i), 58, Game.H - 144 + i * 40, FONT, Text.INK);
		} else {
			oy = Game.H - oh - 30;
		}
		Text.panel(g, ox, oy, ow, oh);
		for (int i = 0; i < options.length; i++) {
			int y = oy + 46 + i * 42;
			if (i == sel) {
				g.setColor(new Color(244, 196, 72, 120));
				g.fillRoundRect(ox + 12, y - 28, ow - 24, 38, 10, 10);
				g.setColor(Text.INK);
				g.fillPolygon(new int[] { ox + 20, ox + 20, ox + 32 }, new int[] { y - 20, y - 2, y - 11 }, 3);
			}
			Text.draw(g, options[i], ox + 44, y, FONT, Text.INK);
		}
	}

	/** Used by tests. */
	public String[] options() { return options; }

}
