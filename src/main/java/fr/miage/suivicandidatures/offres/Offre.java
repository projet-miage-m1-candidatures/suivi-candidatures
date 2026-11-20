package fr.miage.suivicandidatures.offres;

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
import java.util.Objects;

/**
 * Une offre d'emploi, de stage ou d'alternance.
 * <p>
 * Une offre vient d'une source externe : elle est la même pour tous les candidats et ne change pas après son import.
 * Ce que chaque candidat en fait (la garder ou la passer) est enregistré dans {@link TriOffre}.
 */
@Entity
@Table(name = "offre")
public class Offre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceOffre source;

    /** Identifiant dans la source : identifiant France Travail, ou URL pour un import par lien. */
    @Column(name = "id_externe", nullable = false, length = 2048)
    private String idExterne;

    @Column(length = 2048)
    private String url;

    @Column(nullable = false)
    private String titre;

    private String entreprise;

    private String lieu;

    @Column(name = "type_contrat", length = 100)
    private String typeContrat;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_publication")
    private LocalDate datePublication;

    @Column(name = "date_import", nullable = false)
    private Instant dateImport;

    /** Réservé à JPA. */
    protected Offre() {
    }

    private Offre(Builder builder) {
        this.source = Objects.requireNonNull(builder.source, "La source de l'offre est obligatoire");
        this.idExterne = exigerTexte(builder.idExterne, "L'identifiant de l'offre dans sa source est obligatoire");
        this.titre = exigerTexte(builder.titre, "Le titre de l'offre est obligatoire");
        this.url = builder.url;
        this.entreprise = builder.entreprise;
        this.lieu = builder.lieu;
        this.typeContrat = builder.typeContrat;
        this.description = builder.description;
        this.datePublication = builder.datePublication;
        this.dateImport = builder.dateImport != null ? builder.dateImport : Instant.now();
    }

    public static Builder builder(SourceOffre source, String idExterne, String titre) {
        return new Builder(source, idExterne, titre);
    }

    private static String exigerTexte(String valeur, String message) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return valeur.strip();
    }

    public Long getId() {
        return id;
    }

    public SourceOffre getSource() {
        return source;
    }

    public String getIdExterne() {
        return idExterne;
    }

    public String getUrl() {
        return url;
    }

    public String getTitre() {
        return titre;
    }

    public String getEntreprise() {
        return entreprise;
    }

    public String getLieu() {
        return lieu;
    }

    public String getTypeContrat() {
        return typeContrat;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDatePublication() {
        return datePublication;
    }

    public Instant getDateImport() {
        return dateImport;
    }

    /** Construit une offre étape par étape : seuls la source, l'identifiant externe et le titre sont obligatoires. */
    public static final class Builder {
        private final SourceOffre source;
        private final String idExterne;
        private final String titre;
        private String url;
        private String entreprise;
        private String lieu;
        private String typeContrat;
        private String description;
        private LocalDate datePublication;
        private Instant dateImport;

        private Builder(SourceOffre source, String idExterne, String titre) {
            this.source = source;
            this.idExterne = idExterne;
            this.titre = titre;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder entreprise(String entreprise) {
            this.entreprise = entreprise;
            return this;
        }

        public Builder lieu(String lieu) {
            this.lieu = lieu;
            return this;
        }

        public Builder typeContrat(String typeContrat) {
            this.typeContrat = typeContrat;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder datePublication(LocalDate datePublication) {
            this.datePublication = datePublication;
            return this;
        }

        public Builder dateImport(Instant dateImport) {
            this.dateImport = dateImport;
            return this;
        }

        public Offre build() {
            return new Offre(this);
        }
    }
}
