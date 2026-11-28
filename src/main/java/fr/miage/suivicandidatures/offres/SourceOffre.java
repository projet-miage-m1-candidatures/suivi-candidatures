package fr.miage.suivicandidatures.offres;

/** D'où vient une offre. */
public enum SourceOffre {
    /** Offre trouvée par l'API « Offres d'emploi v2 » de France Travail. */
    FRANCE_TRAVAIL,
    /** Offre importée depuis le lien d'un autre site. */
    LIEN
}
