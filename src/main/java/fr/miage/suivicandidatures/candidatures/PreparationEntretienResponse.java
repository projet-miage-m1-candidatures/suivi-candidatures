package fr.miage.suivicandidatures.candidatures;

import java.time.Instant;

public record PreparationEntretienResponse(
        Long candidatureId,
        String entreprise,
        String contenuMarkdown,
        Instant genereeLe,
        String modele,
        String avertissement) {

    public static PreparationEntretienResponse de(PreparationEntretien p, Candidature c) {
        return new PreparationEntretienResponse(
                p.getCandidatureId(),
                c.getEntreprise(),
                p.getContenu(),
                p.getGenereeLe(),
                p.getModele(),
                PreparationEntretienService.AVERTISSEMENT);
    }
}
