package celestial.world;

import java.awt.Color;

/** Colour palette for an area. Also picks the battle background. */
public enum Theme {
	BARRIO(new Color(104, 170, 76), new Color(186, 152, 102), new Color(60, 128, 200)),
	LAB(new Color(176, 132, 86), new Color(176, 132, 86), new Color(60, 128, 200)),
	PALAYAN(new Color(114, 180, 72), new Color(190, 158, 100), new Color(80, 150, 190)),
	KAWAYAN(new Color(54, 94, 52), new Color(92, 84, 58), new Color(60, 110, 120)),
	TUKTOK(new Color(124, 168, 78), new Color(192, 156, 104), new Color(80, 150, 190)),
	BULKAN(new Color(92, 80, 78), new Color(64, 56, 56), new Color(60, 110, 160)),
	CRATER(new Color(100, 84, 80), new Color(70, 60, 58), new Color(40, 104, 170)),
	DAGAT(new Color(226, 208, 150), new Color(200, 180, 130), new Color(34, 124, 186)),
	TEMPLO(new Color(70, 96, 100), new Color(70, 96, 100), new Color(30, 80, 110)),
	REEF(new Color(120, 150, 160), new Color(120, 150, 160), new Color(20, 70, 120)),
	CELESTIA(new Color(58, 56, 98), new Color(58, 56, 98), new Color(16, 24, 64));

	public final Color ground, path, water;

	Theme(Color ground, Color path, Color water) {
		this.ground = ground;
		this.path = path;
		this.water = water;
	}
}
