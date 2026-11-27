package fr.miage.suivicandidatures.candidatures;

import java.util.EnumSet;
import java.util.Set;

/**
 * Statut d'une candidature et transitions autorisées (machine à états).
 *
 * EN_ATTENTE -> ENTRETIEN | REFUSEE
 * ENTRETIEN  -> REFUSEE
 * REFUSEE    -> (aucune : état final)
 */
public enum StatutCandidature {

    EN_ATTENTE("En attente"),
    ENTRETIEN("Entretien à préparer"),
    REFUSEE("Refusée");

    private final String libelle;

    StatutCandidature(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public Set<StatutCandidature> transitionsAutorisees() {
        return switch (this) {
            case EN_ATTENTE -> EnumSet.of(ENTRETIEN, REFUSEE);
            case ENTRETIEN -> EnumSet.of(REFUSEE);
            case REFUSEE -> EnumSet.noneOf(StatutCandidature.class);
        };
    }

    public boolean peutPasserA(StatutCandidature cible) {
        return transitionsAutorisees().contains(cible);
    }
}
