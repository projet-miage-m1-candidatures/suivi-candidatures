package fr.miage.suivicandidatures.authentification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record DemandeInscription(
		@NotBlank String prenom,
		@NotBlank String nom,
		@NotBlank @Email String email,
		@NotNull String motDePasse) {

	DemandeInscription {
		email = AdresseEmail.normaliser(email);
	}

	@Override
	public String toString() {
		return "DemandeInscription";
	}
}
