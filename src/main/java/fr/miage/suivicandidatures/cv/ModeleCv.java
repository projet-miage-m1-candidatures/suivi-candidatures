package fr.miage.suivicandidatures.cv;

enum ModeleCv {

	CLASSIQUE(PoliceCv.SOURCE_SERIF_4, CouleurCv.NOIR),
	MODERNE(PoliceCv.INTER, CouleurCv.BLEU),
	COMPACT(PoliceCv.IBM_PLEX_SANS, CouleurCv.NOIR),
	ELEGANT(PoliceCv.EB_GARAMOND, CouleurCv.MARINE),
	EPURE(PoliceCv.LATO, CouleurCv.ANTHRACITE),
	CHRONOLOGIQUE(PoliceCv.GELASIO, CouleurCv.BLEU);

	private final PoliceCv policeParDefaut;
	private final CouleurCv couleurParDefaut;

	ModeleCv(PoliceCv policeParDefaut, CouleurCv couleurParDefaut) {
		this.policeParDefaut = policeParDefaut;
		this.couleurParDefaut = couleurParDefaut;
	}

	PoliceCv policeParDefaut() {
		return policeParDefaut;
	}

	CouleurCv couleurParDefaut() {
		return couleurParDefaut;
	}
}
