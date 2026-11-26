package fr.miage.suivicandidatures.cv;

enum PoliceCv {

	INTER("Inter", "inter", "Inter", "Arial, sans-serif"),
	LATO("Lato", "lato", "Lato", "Arial, sans-serif"),
	IBM_PLEX_SANS("IBM Plex Sans", "ibm-plex-sans", "IBMPlexSans", "Arial, sans-serif"),
	SOURCE_SERIF_4("Source Serif 4", "source-serif-4", "SourceSerif4", "Georgia, serif"),
	LORA("Lora", "lora", "Lora", "Georgia, serif");

	private final String famille;
	private final String dossier;
	private final String baseFichier;
	private final String secours;

	PoliceCv(String famille, String dossier, String baseFichier, String secours) {
		this.famille = famille;
		this.dossier = dossier;
		this.baseFichier = baseFichier;
		this.secours = secours;
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
}
