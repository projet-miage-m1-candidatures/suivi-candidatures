package fr.miage.suivicandidatures.cv;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record ContenuCv(
		@NotNull @Valid Identite identite,
		@NotNull @Size(max = 30) List<@NotNull @Valid Experience> experiences,
		@NotNull @Size(max = 30) List<@NotNull @Valid Formation> formations,
		@NotNull @Size(max = 30) List<@NotNull @Valid GroupeCompetences> competences,
		@NotNull @Size(max = 30) List<@NotNull @Valid Langue> langues) {

	record Identite(
			@NotBlank(message = "Le nom est obligatoire.") @Size(max = 160) String nom,
			@Size(max = 160) String titre,
			@Size(max = 120) String ville,
			@Size(max = 254) String email,
			@Size(max = 50) String telephone,
			@Size(max = 500) String lien,
			@Size(max = 3000) String profil) {
	}

	record Experience(
			@Size(max = 160) String poste,
			@Size(max = 160) String entreprise,
			@Size(max = 120) String lieu,
			@Size(max = 40) String debut,
			@Size(max = 40) String fin,
			@NotNull @Size(max = 30) List<@NotNull @Size(max = 1000) String> realisations) {
	}

	record Formation(
			@Size(max = 160) String diplome,
			@Size(max = 160) String ecole,
			@Size(max = 40) String debut,
			@Size(max = 40) String fin) {
	}

	record GroupeCompetences(
			@Size(max = 120) String groupe,
			@NotNull @Size(max = 30) List<@NotNull @Size(max = 120) String> elements) {
	}

	record Langue(@Size(max = 80) String langue, @Size(max = 40) String niveau) {
	}
}
