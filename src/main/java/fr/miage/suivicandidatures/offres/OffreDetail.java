package fr.miage.suivicandidatures.offres;

import java.time.LocalDate;

/**
 * Ce que l'API renvoie pour une offre. On n'expose pas l'entité JPA directement : le format de l'API reste stable
 * même si la table change.
 */
public record OffreDetail(
        Long id,
        SourceOffre source,
        String url,
        String titre,
        String entreprise,
        String lieu,
        String typeContrat,
        String description,
        LocalDate datePublication
) {

    static OffreDetail depuis(Offre offre) {
        return new OffreDetail(
                offre.getId(),
                offre.getSource(),
                offre.getUrl(),
                offre.getTitre(),
                offre.getEntreprise(),
                offre.getLieu(),
                offre.getTypeContrat(),
                offre.getDescription(),
                offre.getDatePublication()
        );
    }
}
