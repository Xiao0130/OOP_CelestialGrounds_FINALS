package celestial.story;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import celestial.data.Creature;
import celestial.data.Element;
import celestial.data.Species;

/** Everything the player has done so far: party, items, Tapang, Hiyas, story flags. */
public class Story {

	public static final int PARTY_LIMIT = 3;

	public boolean boy = true;
	public String heroName = "Araw";

	public Species starter;
	public Species rival;

	public final List<Creature> party = new ArrayList<>();
	public final List<Creature> banga = new ArrayList<>();   // befriended creatures resting in the Banga

	public int pandesal;
	public boolean hasBanga, hasParol, hasJournal;

	/** Tapang (courage) 1-4, 5 = full light at the finale. */
	public int tapang = 1;
	public boolean parolLit = true;
	public boolean heroLight;            // from Scene 14: light comes from the hero, not the parol
	public int hiyas;

	public final Set<String> flags = new HashSet<>();
	public final EnumSet<Species> seen = EnumSet.noneOf(Species.class);
	public final EnumSet<Species> befriended = EnumSet.noneOf(Species.class);

	public String shrineMap = "BARRIO";
	public int shrineX = 10, shrineY = 12;   // Barrio Tulog shrine

	public boolean talaFollowing;

	// ------------------------------------------------------------------ flags
	public boolean has(String flag) { return flags.contains(flag); }

	public void set(String flag) { flags.add(flag); }

	public void clear(String flag) { flags.remove(flag); }

	// ------------------------------------------------------------------ party
	/** Returns true if the creature joined the active party, false if it rests in the Banga. */
	public boolean addCreature(Creature c) {
		seen.add(c.species);
		befriended.add(c.species);
		c.resetBattleState();
		if (party.size() < PARTY_LIMIT) {
			party.add(c);
			return true;
		}
		banga.add(c);
		return false;
	}

	public Creature firstAlive() {
		for (Creature c : party) if (!c.fainted()) return c;
		return null;
	}

	public void healAll() {
		for (Creature c : party) c.fullHeal();
		for (Creature c : banga) c.fullHeal();
	}

	public boolean hasElementInParty(Element e) {
		for (Creature c : party) if (c.element() == e) return true;
		return false;
	}

	public Creature partyMember(Element e) {
		for (Creature c : party) if (c.element() == e && !c.fainted()) return c;
		return null;
	}

	public int maxPartyLevel() {
		int m = 1;
		for (Creature c : party) m = Math.max(m, c.level);
		return m;
	}

	public Creature starterCreature() {
		for (Creature c : party) if (c.species == starter) return c;
		for (Creature c : banga) if (c.species == starter) return c;
		return party.isEmpty() ? null : party.get(0);
	}

	public Species bituinForm() { return boy ? Species.APOLAKI : Species.MAYARI; }

	// ------------------------------------------------------------------ light
	/** Radius of the light circle in tiles. Tapang 1 = 2 tiles, 2 = 3, 3 = 4, then the hero's own light. */
	public double lightRadius() {
		if (heroLight) return tapang >= 5 ? 6.5 : 5.0;
		if (!hasParol || !parolLit) return 0.0;
		return tapang + 1;
	}

	// ------------------------------------------------------------------ text
	/** Fills the script's [PLACEHOLDERS]. */
	public String sub(String s) {
		if (s == null) return null;
		String out = s.replace("[PLAYER NAME]", heroName).replace("[HERO]", heroName);
		if (starter != null) {
			out = out.replace("[STARTER MOVE]", starter.signature.label).replace("[STARTER]", starter.label);
		}
		if (rival != null) {
			out = out.replace("[RIVAL MOVE]", rival.signature.label).replace("[RIVAL PARTNER]", rival.label);
		}
		out = out.replace("[BITUIN FORM]", bituinForm().label.toUpperCase());
		return out;
	}
}
