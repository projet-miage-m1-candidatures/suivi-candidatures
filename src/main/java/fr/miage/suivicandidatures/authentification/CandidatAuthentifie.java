package fr.miage.suivicandidatures.authentification;

import java.util.List;

import fr.miage.suivicandidatures.commun.CandidatCourant;
import fr.miage.suivicandidatures.commun.Utilisateur;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

final class CandidatAuthentifie extends User implements CandidatCourant.Identite {

	private final Long id;
	private final String prenom;
	private final String nom;

	CandidatAuthentifie(Utilisateur utilisateur) {
		super(utilisateur.getEmail(), utilisateur.getMotDePasseHache(),
				List.of(new SimpleGrantedAuthority("ROLE_CANDIDAT")));
		this.id = utilisateur.getId();
		this.prenom = utilisateur.getPrenom();
		this.nom = utilisateur.getNom();
	}

	@Override
	public Long id() {
		return id;
	}

	String prenom() {
		return prenom;
	}

	String nom() {
		return nom;
	}
}
