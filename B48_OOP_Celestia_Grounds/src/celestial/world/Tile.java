package celestial.world;

/** Tile types, keyed by the characters used in MapGrids. */
public enum Tile {
	GROUND('.', true), PATH(',', true), TALL('"', true), FLOWERS('f', true),
	TREE('T', false), BAMBOO('B', false), ROCK('R', false), FENCE('F', false),
	TUNNEL_WALL('V', false), TUNNEL('v', true), LAVA('L', false), COOLED('K', true),
	WATER('~', false), BOARD('=', true), SAND('s', true), SEAGRASS('q', true),
	PADDY('p', false), ROOF('^', false), WALL('#', false), DOOR('D', false),
	PORCH('*', true), BENCH('n', false), COOP('C', false), SHRINE('S', false), SIGN('O', false), GATEPOST('G', false),
	IWALL('X', false), WOOD('o', true), SHELF('J', false), TABLE('k', false),
	TEMPLE_WALL('x', false), TEMPLE('t', true), CORAL('c', false), SEAFLOOR('u', true), STATUE('E', false),
	STAR('a', true), PILLAR('A', false), CAGE('Q', false), JEEPNEY('Y', false), BOAT('Z', false);

	public final char ch;
	public final boolean walkable;

	Tile(char ch, boolean walkable) {
		this.ch = ch;
		this.walkable = walkable;
	}

	private static final Tile[] BY_CHAR = new Tile[128];
	static {
		for (Tile t : values()) BY_CHAR[t.ch] = t;
	}

	public static Tile of(char c) {
		Tile t = c < 128 ? BY_CHAR[c] : null;
		return t == null ? GROUND : t;
	}

	/** Tall grass and seagrass hide wild Hayop-Diwata. */
	public boolean encounters() {
		return this == TALL || this == SEAGRASS;
	}
}
