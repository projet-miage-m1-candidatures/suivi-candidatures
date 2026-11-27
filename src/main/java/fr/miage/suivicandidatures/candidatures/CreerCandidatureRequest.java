package fr.miage.suivicandidatures.candidatures;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de POST /api/v1/candidatures (bouton « J'ai postulé »). */
public record CreerCandidatureRequest(
        @NotBlank @Size(max = 200) String intitule,
        @NotBlank @Size(max = 200) String entreprise,
        @Size(max = 200) String lieu,
        @Size(max = 1000) String urlOffre,
        @Size(max = 10000) String descriptionOffre) {
}
