package celestial.data;

import java.awt.image.BufferedImage;

import celestial.Assets;

/** The nine Hayop-Diwata plus the story creatures (Bituin, Apolaki, Mayari, Bakunawa). */
public enum Species {
	//         name          real animal              element        ability                    signature            hp  atk def spd  sprite        rare
	TARSI("Tarsi", "Philippine Tarsier", Element.LUPA, Ability.MATA_NG_GABI, Move.LUKSO, 46, 52, 46, 70, "tarsi", true,
			"Tarsi sees tomorrow, mostly because it never blinks."),
	KALABAW("Kalabaw", "Carabao", Element.LUPA, Ability.TIYAGA, Move.PUTIK_SLAM, 72, 54, 60, 30, "kalabaw", false,
			"Nothing rushes it. The carabao has feelings."),
	TAMARAW("Tamaraw", "Tamaraw", Element.LUPA, Ability.HINDI_AATRAS, Move.SUNGAY_CHARGE, 66, 50, 52, 46, "tamaraw", false,
			"Guardian of the Earth. Stubborn as a mountain."),
	BINTURO("Binturo", "Binturong", Element.APOY, Ability.AMOY_POPCORN, Move.ALAB_ARNIS, 56, 55, 48, 52, "binturo", true,
			"Smells like popcorn. It is not a snack."),
	AHAS_BULKAN("Ahas-Bulkan", "Philippine Cobra", Element.APOY, Ability.LASON_ALAB, Move.DURA_NG_APOY, 50, 58, 44, 60, "ahas", false,
			"Spits tiny flames and looks very proud of itself."),
	HARIBON("Haribon", "Philippine Eagle", Element.APOY, Ability.HARI_NG_HIMPAPAWID, Move.DAGIT_NG_ARAW, 72, 64, 56, 80, "haribon", false,
			"Guardian of the Fire. It always strikes first."),
	DUGONG("Dugong", "Dugong", Element.TUBIG, Ability.KUNDIMAN, Move.ALON_AWIT, 64, 48, 54, 40, "dugong", true,
			"Sings off-key, but it sings for you."),
	PUGITA("Pugita", "Octopus", Element.TUBIG, Ability.TINTA, Move.GALAMAY_LASH, 54, 54, 50, 58, "pugita", false,
			"Squirts ink and looks completely innocent."),
	BUTANDING("Butanding", "Whale Shark", Element.TUBIG, Ability.BITUIN_BATIK, Move.LUNOK_ALON, 80, 58, 58, 30, "butanding", false,
			"Guardian of the Water. Spots like constellations."),
	// story creatures (not among the nine)
	BITUIN("Bituin", "Baby star", Element.WALA, Ability.LIWANAG, Move.TULAK, 30, 10, 10, 10, "bituin", false,
			"Glows brighter when someone is scared."),
	APOLAKI("Apolaki", "Sun spirit", Element.WALA, Ability.LIWANAG, Move.TULAK, 90, 70, 70, 70, "apolaki", false,
			"The light the Bakunawa lost, shining like the sun."),
	MAYARI("Mayari", "Moon spirit", Element.WALA, Ability.LIWANAG, Move.TULAK, 90, 70, 70, 70, "mayari", false,
			"The light the Bakunawa lost, shining like the moon."),
	BAKUNAWA("Bakunawa", "Moon-eating serpent", Element.TUBIG, Ability.GUTOM, Move.ALON_NG_GUTOM, 56, 52, 56, 50, "bakunawa", false,
			"The first light-keeper, who lost its light and is afraid of the dark.");

	public static final Species[] NINE = { TARSI, KALABAW, TAMARAW, BINTURO, AHAS_BULKAN, HARIBON, DUGONG, PUGITA, BUTANDING };

	public final String label, animal, sprite, flavor;
	public final Element element;
	public final Ability ability;
	public final Move signature;
	public final int hp, atk, def, spd;
	public final boolean rare;

	Species(String label, String animal, Element element, Ability ability, Move signature,
			int hp, int atk, int def, int spd, String sprite, boolean rare, String flavor) {
		this.label = label;
		this.animal = animal;
		this.element = element;
		this.ability = ability;
		this.signature = signature;
		this.hp = hp;
		this.atk = atk;
		this.def = def;
		this.spd = spd;
		this.sprite = sprite;
		this.rare = rare;
		this.flavor = flavor;
	}

	public BufferedImage image() {
		return Assets.cg("mon_" + sprite + ".png");
	}

	public boolean isStarter() {
		return this == TARSI || this == BINTURO || this == DUGONG;
	}

	public static Species starterFor(Element e) {
		switch (e) {
			case LUPA: return TARSI;
			case APOY: return BINTURO;
			default: return DUGONG;
		}
	}
}
