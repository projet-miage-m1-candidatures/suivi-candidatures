package fr.miage.suivicandidatures.cv;

enum ModeleCv {

	CLASSIQUE(PoliceCv.SOURCE_SERIF_4),
	MODERNE(PoliceCv.INTER),
	COMPACT(PoliceCv.IBM_PLEX_SANS);

	private final PoliceCv policeParDefaut;

	ModeleCv(PoliceCv policeParDefaut) {
		this.policeParDefaut = policeParDefaut;
	}

	PoliceCv policeParDefaut() {
		return policeParDefaut;
	}
}
