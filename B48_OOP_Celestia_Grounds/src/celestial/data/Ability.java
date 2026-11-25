package celestial.data;

public enum Ability {
	MATA_NG_GABI("Mata ng Gabi", "Shows the enemy's next move."),
	TIYAGA("Tiyaga", "Survives one lethal hit with 1 HP, once per battle."),
	HINDI_AATRAS("Hindi Aatras", "Attack rises as its HP drops."),
	AMOY_POPCORN("Amoy Popcorn", "The enemy may lose its turn, distracted by the smell."),
	LASON_ALAB("Lason-Alab", "Burn damage stacks with each hit."),
	HARI_NG_HIMPAPAWID("Hari ng Himpapawid", "Always acts first."),
	KUNDIMAN("Kundiman", "Heals the party a little each turn."),
	TINTA("Tinta", "Ink lowers the enemy's accuracy."),
	BITUIN_BATIK("Bituin-Batik", "Heals itself each turn."),
	LIWANAG("Liwanag", "A light that grows when someone is scared."),
	GUTOM("Gutom", "Lost its light. Changes form when hurt.");

	public final String label, description;

	Ability(String label, String description) {
		this.label = label;
		this.description = description;
	}
}
