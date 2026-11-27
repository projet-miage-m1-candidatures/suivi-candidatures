package fr.miage.suivicandidatures.cv;

enum CouleurCv {

	NOIR("#1b1f24", "#1b1f24"),
	MARINE("#1f2f4f", "#1b1f24"),
	BLEU("#1d4fa8", "#1b1f24"),
	BORDEAUX("#7a1f2b", "#1b1f24"),
	SAPIN("#1f4d3a", "#1b1f24"),
	ANTHRACITE("#2b2f36", "#4a4f57");

	private final String titres;
	private final String texte;

	CouleurCv(String titres, String texte) {
		this.titres = titres;
		this.texte = texte;
	}

	String titres() {
		return titres;
	}

	String texte() {
		return texte;
	}
}
