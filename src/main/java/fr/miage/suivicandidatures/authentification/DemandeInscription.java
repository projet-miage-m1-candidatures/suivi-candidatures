package fr.miage.suivicandidatures.authentification;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record DemandeInscription(
		@NotBlank String prenom,
		@NotBlank String nom,
		@NotBlank @Email String email,
		@NotNull String motDePasse) {

	DemandeInscription {
		if (email != null) {
			email = email.strip().toLowerCase(Locale.ROOT);
		}
	}

	@Override
	public String toString() {
		return "DemandeInscription";
	}
}
