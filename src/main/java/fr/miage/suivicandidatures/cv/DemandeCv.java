package fr.miage.suivicandidatures.cv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record DemandeCv(
		@NotBlank(message = "Le titre du CV est obligatoire.") String titre,
		@NotNull(message = "La langue du CV est obligatoire.") LangueCv langue,
		@NotNull(message = "Le modèle du CV est obligatoire.") ModeleCv modele,
		PoliceCv police,
		@NotNull @Valid ContenuCv contenu) {

	PoliceCv policeEffective() {
		return police == null ? modele.policeParDefaut() : police;
	}
}
