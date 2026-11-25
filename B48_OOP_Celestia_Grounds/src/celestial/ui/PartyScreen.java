package celestial.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import celestial.Assets;
import celestial.Game;
import celestial.Input.Key;
import celestial.Screen;
import celestial.Sound;
import celestial.Text;
import celestial.battle.BattleArt;
import celestial.data.Creature;
import celestial.data.Element;
import celestial.data.Move;
import celestial.story.Story;

/** Team of 3 plus the creatures resting in the Banga. Select one, then another, to swap them. */
public class PartyScreen extends Screen {

	private static final Font TITLE = Assets.font(22f);
	private static final Font MID = Assets.font(17f);
	private static final Font SMALL = Assets.font(14f);

	private final boolean finalPick;
	private int cursor;
	private int picked = -1;
	private boolean warned;
	private String note;

	public PartyScreen(Game game, boolean finalPick) {
		super(game);
		this.finalPick = finalPick;
	}

	private List<Creature> all() {
		List<Creature> l = new ArrayList<>(game.story.party);
		l.addAll(game.story.banga);
		return l;
	}

	private Element missing() {
		for (Element e : new Element[] { Element.LUPA, Element.APOY, Element.TUBIG })
			if (!game.story.hasElementInParty(e)) return e;
		return null;
	}

	@Override
	public void update() {
		Story s = game.story;
		List<Creature> l = all();
		if (l.isEmpty()) { close(); return; }
		if (game.input.pressed(Key.UP)) { cursor = (cursor + l.size() - 1) % l.size(); Sound.blip(); }
		if (game.input.pressed(Key.DOWN)) { cursor = (cursor + 1) % l.size(); Sound.blip(); }
		if (game.input.pressed(Key.CONFIRM)) {
			Sound.tok();
			if (picked < 0) {
				picked = cursor;
			} else {
				swap(picked, cursor);
				picked = -1;
			}
			warned = false;
		}
		if (game.input.pressed(Key.CANCEL) || game.input.pressed(Key.PARTY)) {
			if (picked >= 0) { picked = -1; return; }
			if (finalPick && missing() != null && !warned && s.party.size() + s.banga.size() >= 3) {
				warned = true;
				note = "No " + missing().english + " creature in your team! Esc again to go anyway.";
				return;
			}
			close();
		}
	}

	private void swap(int a, int b) {
		Story s = game.story;
		if (a == b) return;
		int np = s.party.size();
		Creature ca = all().get(a), cb = all().get(b);
		boolean aParty = a < np, bParty = b < np;
		if (aParty && bParty) {
			s.party.set(a, cb);
			s.party.set(b, ca);
		} else if (aParty != bParty) {
			int pi = aParty ? a : b, bi = (aParty ? b : a) - np;
			Creature p = s.party.get(pi), q = s.banga.get(bi);
			s.party.set(pi, q);
			s.banga.set(bi, p);
		} else {
			s.banga.set(a - np, cb);
			s.banga.set(b - np, ca);
		}
		note = null;
	}

	@Override
	public void render(Graphics2D g) {
		Story s = game.story;
		g.setColor(new Color(10, 12, 30, 225));
		g.fillRect(0, 0, Game.W, Game.H);
		Text.draw(g, finalPick ? "Choose your final team of 3. Bring one of each element!" : "Your team", 40, 52, finalPick ? MID : TITLE, Text.GOLD);
		// element coverage
		int ex = Game.W - 290;
		int ey = finalPick ? 70 : 30;
		for (Element e : new Element[] { Element.LUPA, Element.APOY, Element.TUBIG }) {
			boolean have = s.hasElementInParty(e);
			g.setColor(have ? e.color : new Color(70, 70, 90));
			g.fillRoundRect(ex, ey, 80, 28, 10, 10);
			Text.draw(g, e.tagalog.toUpperCase(), ex + 10, ey + 20, SMALL, have ? Color.WHITE : new Color(150, 150, 170));
			ex += 88;
		}
		List<Creature> l = all();
		int np = s.party.size();
		Text.draw(g, "TEAM (" + np + "/" + Story.PARTY_LIMIT + ")", 40, 100, SMALL, new Color(200, 200, 230));
		Text.draw(g, "RESTING IN THE BANGA", 40, 112 + Story.PARTY_LIMIT * 74 + 24, SMALL, new Color(200, 200, 230));
		for (int i = 0; i < l.size(); i++) {
			Creature c = l.get(i);
			int y = i < np ? 110 + i * 74 : 112 + Story.PARTY_LIMIT * 74 + 36 + (i - np) * 60;
			int h = i < np ? 66 : 54;
			Color fill = i == picked ? new Color(120, 200, 140) : i == cursor ? Text.GOLD : new Color(60, 64, 104);
			g.setColor(fill);
			g.fillRoundRect(40, y, 470, h, 14, 14);
			Color ink = (i == cursor || i == picked) ? Text.INK : Color.WHITE;
			g.drawImage(Assets.scaled(c.image(), h - 6, h - 6), 48, y + 3, null);
			Text.draw(g, c.name() + "  Lv." + c.level, 48 + h + 6, y + 28, MID, ink);
			BattleArt.elementBadge(g, c.element(), 380, y + 10);
			g.setColor(new Color(40, 40, 50));
			g.fillRect(48 + h + 6, y + h - 20, 180, 8);
			g.setColor(BattleArt.hpColor(c.hpFrac()));
			g.fillRect(48 + h + 6, y + h - 20, (int) (180 * c.hpFrac()), 8);
			Text.draw(g, c.hp + "/" + c.maxHp(), 48 + h + 196, y + h - 12, SMALL, ink);
		}
		// details
		if (!l.isEmpty()) {
			Creature c = l.get(Math.min(cursor, l.size() - 1));
			Text.panel(g, 540, 110, 500, 500);
			g.drawImage(Assets.scaled(c.image(), 176, 176), 702, 120, null);
			Text.draw(g, c.name(), 570, 330, TITLE, Text.INK);
			Text.draw(g, c.species.animal, 570, 360, SMALL, new Color(90, 90, 110));
			BattleArt.elementBadge(g, c.element(), 930, 312);
			Text.draw(g, "Ability: " + c.species.ability.label, 570, 400, MID, Text.INK);
			g.setFont(SMALL);
			List<String> d = Text.wrap(c.species.ability.description, g.getFontMetrics(), 440);
			for (int i = 0; i < d.size(); i++) Text.draw(g, d.get(i), 570, 428 + i * 24, SMALL, new Color(70, 70, 90));
			int y = 490;
			for (Move m : c.moves()) {
				Text.draw(g, m.label + "  (" + m.element.english + ", " + m.power + ")", 570, y, SMALL, Text.INK);
				y += 28;
			}
			Text.draw(g, "EXP " + c.exp + "/" + Creature.expToNext(c.level), 570, y + 10, SMALL, new Color(70, 70, 90));
		}
		String help = picked >= 0 ? "Pick another to swap places.  Esc: cancel" : "Enter: pick to swap   Esc: close";
		Text.draw(g, help, 40, Game.H - 40, SMALL, new Color(200, 200, 230));
		if (note != null) Text.draw(g, note, 40, Game.H - 74, SMALL, new Color(255, 170, 120));
	}
}
