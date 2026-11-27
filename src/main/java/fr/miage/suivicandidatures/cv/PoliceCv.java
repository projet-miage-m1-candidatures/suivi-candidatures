package fr.miage.suivicandidatures.cv;

enum PoliceCv {

	INTER("Inter", "inter", "Inter", "Arial, sans-serif"),
	LATO("Lato", "lato", "Lato", "Arial, sans-serif"),
	IBM_PLEX_SANS("IBM Plex Sans", "ibm-plex-sans", "IBMPlexSans", "Arial, sans-serif"),
	SOURCE_SERIF_4("Source Serif 4", "source-serif-4", "SourceSerif4", "Georgia, serif", "It"),
	LORA("Lora", "lora", "Lora", "Georgia, serif"),
	CARLITO("Carlito", "carlito", "Carlito", "Calibri, Arial, sans-serif", "Italic"),
	OPEN_SANS("Open Sans", "open-sans", "OpenSans", "Arial, sans-serif", "Italic"),
	ROBOTO("Roboto", "roboto", "Roboto", "Arial, sans-serif", "Italic"),
	EB_GARAMOND("EB Garamond", "eb-garamond", "EBGaramond", "Garamond, Georgia, serif", "Italic"),
	GELASIO("Gelasio", "gelasio", "Gelasio", "Georgia, serif", "Italic");

	private final String famille;
	private final String dossier;
	private final String baseFichier;
	private final String secours;
	private final String italique;

	PoliceCv(String famille, String dossier, String baseFichier, String secours) {
		this(famille, dossier, baseFichier, secours, null);
	}

	PoliceCv(String famille, String dossier, String baseFichier, String secours, String italique) {
		this.famille = famille;
		this.dossier = dossier;
		this.baseFichier = baseFichier;
		this.secours = secours;
		this.italique = italique;
	}

	String famille() {
		return famille;
	}

	String familleCss() {
		return "'" + famille + "', " + secours;
	}

	String fichier(String style) {
		return "fonts/" + dossier + "/" + baseFichier + "-" + style + ".ttf";
	}

	String italique() {
		return italique;
	}
}
