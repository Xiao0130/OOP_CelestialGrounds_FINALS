package celestial.data;

/** Every Hayop-Diwata knows Tulak (Push) plus its signature move. */
public enum Move {
	TULAK("Tulak", Element.WALA, 40, 100),
	LUKSO("Lukso", Element.LUPA, 60, 100),
	PUTIK_SLAM("Putik Slam", Element.LUPA, 60, 95),
	SUNGAY_CHARGE("Sungay Charge", Element.LUPA, 68, 95),
	ALAB_ARNIS("Alab-Arnis", Element.APOY, 60, 100),
	DURA_NG_APOY("Dura ng Apoy", Element.APOY, 50, 100),
	DAGIT_NG_ARAW("Dagit ng Araw", Element.APOY, 66, 95),
	ALON_AWIT("Alon-Awit", Element.TUBIG, 60, 100),
	GALAMAY_LASH("Galamay Lash", Element.TUBIG, 55, 100),
	LUNOK_ALON("Lunok-Alon", Element.TUBIG, 66, 95),
	// The Bakunawa's three forms
	ALON_NG_GUTOM("Alon ng Gutom", Element.TUBIG, 62, 100),
	APOY_NG_LUNGKOT("Apoy ng Lungkot", Element.APOY, 50, 100),
	BATO_NG_PAGKALIMOT("Bato ng Pagkalimot", Element.LUPA, 62, 100);

	public final String label;
	public final Element element;
	public final int power, accuracy;

	Move(String label, Element element, int power, int accuracy) {
		this.label = label;
		this.element = element;
		this.power = power;
		this.accuracy = accuracy;
	}
}
