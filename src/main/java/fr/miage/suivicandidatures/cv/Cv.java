package fr.miage.suivicandidatures.cv;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cv")
public class Cv {

	static final int LONGUEUR_MAX_TITRE = 120;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "utilisateur_id", nullable = false, updatable = false)
	private Long utilisateurId;

	@Column(nullable = false, length = LONGUEUR_MAX_TITRE)
	private String titre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 2)
	private LangueCv langue;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ModeleCv modele;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PoliceCv police;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CouleurCv couleur;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TailleCv taille;

	@JdbcTypeCode(SqlTypes.JSON)
	@Convert(converter = ContenuCvJson.class)
	@Column(nullable = false)
	private ContenuCv contenu;

	@Column(name = "date_creation", nullable = false, updatable = false)
	private Instant dateCreation;

	@Column(name = "date_modification", nullable = false)
	private Instant dateModification;

	protected Cv() {
	}

	Cv(Long utilisateurId, DemandeCv demande, Instant maintenant) {
		this.utilisateurId = utilisateurId;
		this.dateCreation = maintenant;
		modifier(demande, maintenant);
	}

	void modifier(DemandeCv demande, Instant maintenant) {
		titre = demande.titre();
		langue = demande.langue();
		modele = demande.modele();
		police = demande.policeEffective();
		couleur = demande.couleurEffective();
		taille = demande.tailleEffective();
		contenu = demande.contenu();
		dateModification = maintenant;
	}

	Long getId() {
		return id;
	}

	String getTitre() {
		return titre;
	}

	LangueCv getLangue() {
		return langue;
	}

	ModeleCv getModele() {
		return modele;
	}

	PoliceCv getPolice() {
		return police;
	}

	CouleurCv getCouleur() {
		return couleur;
	}

	TailleCv getTaille() {
		return taille;
	}

	ContenuCv getContenu() {
		return contenu;
	}

	Instant getDateCreation() {
		return dateCreation;
	}

	Instant getDateModification() {
		return dateModification;
	}
}
