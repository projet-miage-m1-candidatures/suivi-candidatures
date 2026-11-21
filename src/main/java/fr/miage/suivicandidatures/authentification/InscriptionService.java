package fr.miage.suivicandidatures.authentification;

import java.nio.charset.StandardCharsets;

import fr.miage.suivicandidatures.commun.Utilisateur;
import fr.miage.suivicandidatures.commun.UtilisateurRepository;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class InscriptionService {

	private final UtilisateurRepository utilisateurs;
	private final PasswordEncoder passwordEncoder;

	InscriptionService(UtilisateurRepository utilisateurs, PasswordEncoder passwordEncoder) {
		this.utilisateurs = utilisateurs;
		this.passwordEncoder = passwordEncoder;
	}

	void inscrire(DemandeInscription demande) {
		String motDePasse = demande.motDePasse();
		if (motDePasse.codePointCount(0, motDePasse.length()) < 15
				|| motDePasse.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Le mot de passe doit contenir au moins 15 caractères et au plus 72 octets en UTF-8");
		}

		String email = demande.email();
		if (utilisateurs.findByEmail(email).isPresent()) {
			throw emailDejaUtilise();
		}

		String hachage = passwordEncoder.encode(motDePasse);
		try {
			utilisateurs.saveAndFlush(new Utilisateur(email, hachage, demande.prenom(), demande.nom()));
		} catch (DataIntegrityViolationException erreur) {
			if (erreur.getCause() instanceof ConstraintViolationException violation
					&& "uk_utilisateur_email".equals(violation.getConstraintName())) {
				throw emailDejaUtilise();
			}
			throw erreur;
		}
	}

	private ResponseStatusException emailDejaUtilise() {
		return new ResponseStatusException(HttpStatus.CONFLICT, "Adresse e-mail déjà utilisée");
	}
}
