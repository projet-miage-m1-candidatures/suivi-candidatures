package fr.miage.suivicandidatures.cv;

import com.fasterxml.jackson.annotation.JsonCreator;

enum InterligneCv {
	SERRE, NORMAL, AERE;

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	static InterligneCv depuisJson(String valeur) {
		return valueOf(valeur);
	}
}
