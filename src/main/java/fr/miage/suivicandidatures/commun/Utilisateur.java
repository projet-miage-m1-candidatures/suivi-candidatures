package fr.miage.suivicandidatures.commun;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String email;

	@Column(name = "mot_de_passe_hache", nullable = false)
	private String motDePasseHache;

	@Column(nullable = false)
	private String prenom;

	@Column(nullable = false)
	private String nom;

	protected Utilisateur() {
	}

	public Utilisateur(String email, String motDePasseHache, String prenom, String nom) {
		this.email = email;
		this.motDePasseHache = motDePasseHache;
		this.prenom = prenom;
		this.nom = nom;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getMotDePasseHache() {
		return motDePasseHache;
	}

	public String getPrenom() {
		return prenom;
	}

	public String getNom() {
		return nom;
	}
}
