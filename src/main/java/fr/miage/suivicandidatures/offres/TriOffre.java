package fr.miage.suivicandidatures.offres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/** Le choix d'un candidat sur une offre : il la garde ou il la passe. Un seul choix par candidat et par offre. */
@Entity
@Table(name = "tri_offre")
public class TriOffre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant du candidat. Simple nombre pour l'instant : l'entité Utilisateur arrive avec l'authentification,
     * on remplacera alors ce champ par une relation.
     */
    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false)
    private Offre offre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChoixTri choix;

    @Column(name = "date_choix", nullable = false)
    private Instant dateChoix;

    /** Réservé à JPA. */
    protected TriOffre() {
    }

    public TriOffre(Long utilisateurId, Offre offre, ChoixTri choix) {
        this.utilisateurId = Objects.requireNonNull(utilisateurId, "Le candidat est obligatoire");
        this.offre = Objects.requireNonNull(offre, "L'offre est obligatoire");
        this.choix = Objects.requireNonNull(choix, "Le choix est obligatoire");
        this.dateChoix = Instant.now();
    }

    /** Le candidat change d'avis : on garde la même ligne et on met la date à jour. */
    public void changerChoix(ChoixTri nouveauChoix) {
        this.choix = Objects.requireNonNull(nouveauChoix, "Le choix est obligatoire");
        this.dateChoix = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public Offre getOffre() {
        return offre;
    }

    public ChoixTri getChoix() {
        return choix;
    }

    public Instant getDateChoix() {
        return dateChoix;
    }
}
