package fr.miage.suivicandidatures.cv;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record ContenuCv(
		@NotNull @Valid Identite identite,
		@Size(max = 3000) String profil,
		@NotNull @Size(max = 30) List<@NotNull @Valid GroupeCompetences> competences,
		@NotNull @Size(max = 30) List<@NotNull @Size(max = 120) String> savoirEtre,
		@NotNull @Size(max = 30) List<@NotNull @Size(max = 120) String> centresInteret,
		@NotNull @Size(max = 30) List<@NotNull @Valid Experience> experiences,
		@NotNull @Size(max = 30) List<@NotNull @Valid Formation> formations,
		@NotNull @Size(max = 30) List<@NotNull @Valid Projet> projets,
		@NotNull @Size(max = 30) List<@NotNull @Valid Langue> langues,
		@NotNull @Size(max = 30) List<@NotNull @Valid Certification> certifications,
		@NotNull @Valid Disponibilite disponibilite) {

	// Mêmes caractères blancs que String.trim() dans le rendu du navigateur.
	private static final String ESPACES = "[\\p{Zs}\\t\\n\\r\\f\\x0B\\u2028\\u2029\\uFEFF]";
	private static final java.util.regex.Pattern BORDS = java.util.regex.Pattern.compile(
			"\\A" + ESPACES + "+|" + ESPACES + "+\\z");

	static String sansEspacesAuxBords(String texte) {
		return texte == null ? "" : BORDS.matcher(texte).replaceAll("");
	}

	record Identite(
			@NotBlank(message = "Le prénom est obligatoire.") @Size(max = 160) String prenom,
			@NotBlank(message = "Le nom est obligatoire.") @Size(max = 160) String nom,
			@Size(max = 160) String titre,
			@Size(max = 120) String ville,
			@Size(max = 254) String email,
			@Size(max = 50) String telephone,
			@JsonInclude(JsonInclude.Include.NON_NULL) @Size(max = 160) String mobilite,
			@NotNull @Size(max = 30) List<@NotNull @Pattern(regexp = "AM|A1|A2|A|B|BE|C1|C1E|C|CE|D1|D1E|D|DE",
					message = "Le code du permis de conduire est invalide.") String> permis,
			@NotNull @Size(max = 30) List<@NotNull @Valid Lien> liens) {

		String nomComplet() {
			return (sansEspacesAuxBords(prenom) + " " + sansEspacesAuxBords(nom)).strip();
		}
	}

	record Lien(@Size(max = 120) String libelle, @Size(max = 500) String url) {
	}

	record Experience(
			@Size(max = 160) String poste,
			@Size(max = 160) String entreprise,
			@Size(max = 120) String lieu,
			@Pattern(regexp = DatesCv.ANNEE_MOIS, message = DatesCv.MESSAGE_DEBUT) String debut,
			@Pattern(regexp = DatesCv.ANNEE_MOIS, message = DatesCv.MESSAGE_FIN) String fin,
			boolean enCours,
			@Size(max = 3000) String description,
			@NotNull @Size(max = 30) List<@NotNull @Size(max = 1000) String> realisations) {
	}

	record Formation(
			@Size(max = 160) String diplome,
			@Size(max = 160) String etablissement,
			@Size(max = 120) String mention,
			@Pattern(regexp = DatesCv.ANNEE_MOIS, message = DatesCv.MESSAGE_DEBUT) String debut,
			@Pattern(regexp = DatesCv.ANNEE_MOIS, message = DatesCv.MESSAGE_FIN) String fin,
			boolean enCours,
			@Size(max = 3000) String description) {
	}

	record Projet(@Size(max = 160) String titre, @Size(max = 500) String lien,
			@Size(max = 3000) String description) {
	}

	record Certification(@Size(max = 160) String nom, @Size(max = 160) String organisme,
			@Pattern(regexp = DatesCv.ANNEE_MOIS, message = "La date de certification doit être au format AAAA ou AAAA-MM.") String date,
			@Size(max = 500) String lien) {
	}

	record Disponibilite(boolean immediate, String date) {

		@JsonIgnore
		@AssertTrue(message = "La date de disponibilité doit être une date valide au format AAAA-MM-JJ.")
		public boolean isDateValide() {
			return date == null || date.isEmpty() || DatesCv.jourValide(date);
		}
	}

	record GroupeCompetences(
			@Size(max = 120) String groupe,
			@NotNull @Size(max = 30) List<@NotNull @Size(max = 120) String> elements) {
	}

	record Langue(@Size(max = 80) String langue, @Size(max = 40) String niveau) {
	}
}
