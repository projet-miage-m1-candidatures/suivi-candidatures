package fr.miage.suivicandidatures.candidatures;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "candidature")
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Candidat propriétaire : chacun ne voit que ses candidatures. */
    @Column(name = "utilisateur_id", nullable = false, updatable = false)
    private Long utilisateurId;

    @Column(nullable = false, length = 200)
    private String intitule;

    @Column(nullable = false, length = 200)
    private String entreprise;

    @Column(length = 200)
    private String lieu;

    @Column(name = "url_offre", length = 1000)
    private String urlOffre;

    @Column(name = "description_offre", length = 10000)
    private String descriptionOffre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutCandidature statut;

    @Column(name = "date_candidature", nullable = false)
    private LocalDate dateCandidature;

    @Column(name = "date_entretien")
    private LocalDate dateEntretien;

    @Column(name = "date_dernier_statut", nullable = false)
    private Instant dateDernierStatut;

    protected Candidature() {
        // requis par JPA
    }

    public Candidature(Long utilisateurId, String intitule, String entreprise, String lieu, String urlOffre,
                       String descriptionOffre) {
        this.utilisateurId = utilisateurId;
        this.intitule = intitule;
        this.entreprise = entreprise;
        this.lieu = lieu;
        this.urlOffre = urlOffre;
        this.descriptionOffre = descriptionOffre;
        this.statut = StatutCandidature.EN_ATTENTE;
        this.dateCandidature = LocalDate.now();
        this.dateDernierStatut = Instant.now();
    }

    /** Applique le nouveau statut. La validité de la transition est vérifiée par le service. */
    public void appliquerStatut(StatutCandidature cible, LocalDate dateEntretien) {
        this.statut = cible;
        this.dateDernierStatut = Instant.now();
        if (cible == StatutCandidature.ENTRETIEN) {
            this.dateEntretien = dateEntretien;
        }
    }

    public Long getId() { return id; }
    public Long getUtilisateurId() { return utilisateurId; }
    public String getIntitule() { return intitule; }
    public String getEntreprise() { return entreprise; }
    public String getLieu() { return lieu; }
    public String getUrlOffre() { return urlOffre; }
    public String getDescriptionOffre() { return descriptionOffre; }
    public StatutCandidature getStatut() { return statut; }
    public LocalDate getDateCandidature() { return dateCandidature; }
    public LocalDate getDateEntretien() { return dateEntretien; }
    public Instant getDateDernierStatut() { return dateDernierStatut; }
}
