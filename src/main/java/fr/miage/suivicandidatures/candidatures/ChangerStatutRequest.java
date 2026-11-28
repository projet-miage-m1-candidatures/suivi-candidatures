package fr.miage.suivicandidatures.candidatures;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Corps de PATCH /api/v1/candidatures/{id}/statut. */
public record ChangerStatutRequest(
        @NotNull StatutCandidature statut,
        LocalDate dateEntretien) {
}
