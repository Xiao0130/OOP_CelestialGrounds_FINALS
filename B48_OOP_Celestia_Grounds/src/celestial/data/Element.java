package celestial.data;

import java.awt.Color;

/** Lupa (Earth), Apoy (Fire), Tubig (Water). Fire beats Earth, Earth beats Water, Water beats Fire. */
public enum Element {
	LUPA("Lupa", "Earth", new Color(176, 132, 72)),
	APOY("Apoy", "Fire", new Color(226, 86, 48)),
	TUBIG("Tubig", "Water", new Color(64, 140, 220)),
	WALA("-", "Normal", new Color(150, 150, 150));

	public final String tagalog, english;
	public final Color color;

	Element(String tagalog, String english, Color color) {
		this.tagalog = tagalog;
		this.english = english;
		this.color = color;
	}

	public boolean beats(Element other) {
		return (this == APOY && other == LUPA) || (this == LUPA && other == TUBIG) || (this == TUBIG && other == APOY);
	}

	/** 1.5x super effective, 0.5x not very effective. */
	public double against(Element defender) {
		if (this == WALA || defender == WALA) return 1.0;
		if (beats(defender)) return 1.5;
		if (defender.beats(this)) return 0.5;
		return 1.0;
	}

	/** The element that beats this one. */
	public Element weakness() {
		switch (this) {
			case LUPA: return APOY;
			case APOY: return TUBIG;
			case TUBIG: return LUPA;
			default: return WALA;
		}
	}

	public String label() {
		return english + " (" + tagalog + ")";
	}
}
