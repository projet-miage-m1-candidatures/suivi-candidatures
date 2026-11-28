package fr.miage.suivicandidatures.candidatures;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Fiche de préparation à l'entretien générée par l'IA, une par candidature. */
@Entity
@Table(name = "preparation_entretien")
public class PreparationEntretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidature_id", nullable = false, unique = true)
    private Long candidatureId;

    @Column(nullable = false, length = 30000)
    private String contenu;

    @Column(name = "generee_le", nullable = false)
    private Instant genereeLe;

    @Column(nullable = false, length = 100)
    private String modele;

    protected PreparationEntretien() {
        // requis par JPA
    }

    public PreparationEntretien(Long candidatureId) {
        this.candidatureId = candidatureId;
    }

    public void remplacer(String contenu, String modele) {
        this.contenu = contenu;
        this.modele = modele;
        this.genereeLe = Instant.now();
    }

    public Long getId() { return id; }
    public Long getCandidatureId() { return candidatureId; }
    public String getContenu() { return contenu; }
    public Instant getGenereeLe() { return genereeLe; }
    public String getModele() { return modele; }
}
