package fr.miage.suivicandidatures.authentification;

import java.util.Locale;

final class AdresseEmail {

	private AdresseEmail() {
	}

	static String normaliser(String email) {
		return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
	}
}
