package fr.miage.suivicandidatures.candidatures;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record CandidatureResponse(
        Long id,
        String intitule,
        String entreprise,
        String lieu,
        String urlOffre,
        String descriptionOffre,
        StatutCandidature statut,
        String statutLibelle,
        LocalDate dateCandidature,
        LocalDate dateEntretien,
        Instant dateDernierStatut,
        Set<StatutCandidature> transitionsPossibles) {

    public static CandidatureResponse de(Candidature c) {
        return new CandidatureResponse(
                c.getId(),
                c.getIntitule(),
                c.getEntreprise(),
                c.getLieu(),
                c.getUrlOffre(),
                c.getDescriptionOffre(),
                c.getStatut(),
                c.getStatut().getLibelle(),
                c.getDateCandidature(),
                c.getDateEntretien(),
                c.getDateDernierStatut(),
                c.getStatut().transitionsAutorisees());
    }
}
