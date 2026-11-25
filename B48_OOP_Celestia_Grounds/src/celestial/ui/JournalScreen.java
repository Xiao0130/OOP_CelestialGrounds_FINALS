package celestial.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Sound;
import celestial.Text;
import celestial.battle.BattleArt;
import celestial.data.Species;
import celestial.story.Story;

/** The Hayop-Journal: every animal you have met (seen) and befriended. */
public class JournalScreen extends Screen {

	private static final Font TITLE = Assets.font(22f);
	private static final Font MID = Assets.font(17f);
	private static final Font SMALL = Assets.font(14f);
	private int sel;

	public JournalScreen(Game game) {
		super(game);
	}

	@Override
	public void update() {
		if (game.input.pressed(Key.LEFT)) { sel = (sel + 8) % 9; Sound.blip(); }
		if (game.input.pressed(Key.RIGHT)) { sel = (sel + 1) % 9; Sound.blip(); }
		if (game.input.pressed(Key.UP)) { sel = (sel + 6) % 9; Sound.blip(); }
		if (game.input.pressed(Key.DOWN)) { sel = (sel + 3) % 9; Sound.blip(); }
		if (game.input.pressed(Key.CANCEL) || game.input.pressed(Key.JOURNAL) || game.input.pressed(Key.CONFIRM)) close();
	}

	@Override
	public void render(Graphics2D g) {
		Story s = game.story;
		g.setColor(new Color(60, 40, 24));
		g.fillRect(0, 0, Game.W, Game.H);
		Text.panel(g, 20, 20, Game.W - 40, Game.H - 40, new Color(244, 232, 200), new Color(110, 70, 40));
		int seen = 0, friends = 0;
		for (Species sp : Species.NINE) {
			if (s.seen.contains(sp)) seen++;
			if (s.befriended.contains(sp)) friends++;
		}
		Text.draw(g, "HAYOP-JOURNAL", 60, 72, TITLE, new Color(110, 70, 40));
		Text.draw(g, "Seen " + seen + "/9   Befriended " + friends + "/9", 600, 72, MID, Text.INK);
		for (int i = 0; i < 9; i++) {
			Species sp = Species.NINE[i];
			int x = 60 + (i % 3) * 160, y = 100 + (i / 3) * 170;
			g.setColor(i == sel ? Text.GOLD : new Color(226, 210, 170));
			g.fillRoundRect(x, y, 148, 158, 16, 16);
			boolean known = s.seen.contains(sp);
			if (known) g.drawImage(Assets.scaled(sp.image(), 112, 112), x + 18, y + 8, null);
			else g.drawImage(Assets.silhouette(Assets.scaled(sp.image(), 112, 112), 0xFF6A5A48), x + 18, y + 8, null);
			Text.center(g, known ? sp.label : "???", x + 74, y + 140, SMALL, Text.INK, false);
			if (s.befriended.contains(sp)) g.drawImage(Assets.scaled(Assets.cg("obj_banga.png"), 26, 26), x + 116, y + 6, null);
		}
		Species sp = Species.NINE[sel];
		int dx = 570;
		Text.panel(g, dx, 100, 470, 500, new Color(252, 244, 220), new Color(160, 120, 80));
		if (s.seen.contains(sp)) {
			Text.draw(g, sp.label, dx + 30, 150, TITLE, Text.INK);
			Text.draw(g, "(" + sp.animal + ")", dx + 30, 180, SMALL, new Color(90, 80, 70));
			BattleArt.elementBadge(g, sp.element, dx + 360, 130);
			g.setFont(MID);
			List<String> fl = Text.wrap(sp.flavor, g.getFontMetrics(), 410);
			for (int i = 0; i < fl.size(); i++) Text.draw(g, fl.get(i), dx + 30, 230 + i * 30, MID, Text.INK);
			{
				Text.draw(g, "Ability: " + sp.ability.label, dx + 30, 360, MID, Text.INK);
				g.setFont(SMALL);
				List<String> ab = Text.wrap(sp.ability.description, g.getFontMetrics(), 410);
				for (int i = 0; i < ab.size(); i++) Text.draw(g, ab.get(i), dx + 30, 390 + i * 24, SMALL, new Color(80, 70, 60));
				Text.draw(g, "Signature move: " + sp.signature.label, dx + 30, 470, SMALL, Text.INK);
				Text.draw(g, "Also knows: Tulak (Push)", dx + 30, 498, SMALL, Text.INK);
			}
			Text.draw(g, s.befriended.contains(sp) ? "Befriended!" : "Not befriended yet", dx + 30, 560, MID,
					s.befriended.contains(sp) ? new Color(40, 130, 60) : new Color(150, 90, 60));
		} else {
			Text.draw(g, "Not yet met.", dx + 30, 150, MID, new Color(120, 100, 80));
		}
		Text.draw(g, "Esc / J: close", 60, Game.H - 50, SMALL, new Color(110, 70, 40));
	}
}
