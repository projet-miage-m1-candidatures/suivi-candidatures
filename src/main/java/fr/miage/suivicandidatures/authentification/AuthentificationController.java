package fr.miage.suivicandidatures.authentification;

import fr.miage.suivicandidatures.commun.CandidatCourant;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthentificationController {

	private final InscriptionService inscriptionService;
	private final CandidatCourant candidatCourant;

	AuthentificationController(InscriptionService inscriptionService, CandidatCourant candidatCourant) {
		this.inscriptionService = inscriptionService;
		this.candidatCourant = candidatCourant;
	}

	@GetMapping("/csrf")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void csrf(CsrfToken jeton) {
		// Charge le jeton pour que Spring Security le rende disponible dans le cookie.
		jeton.getToken();
	}

	@PostMapping("/inscription")
	@ResponseStatus(HttpStatus.CREATED)
	void inscrire(@Valid @RequestBody DemandeInscription demande) {
		inscriptionService.inscrire(demande);
	}

	@GetMapping("/moi")
	ProfilCandidat profil(@AuthenticationPrincipal CandidatAuthentifie candidat) {
		return new ProfilCandidat(candidatCourant.id(), candidat.getUsername(), candidat.prenom(), candidat.nom());
	}

	record ProfilCandidat(Long id, String email, String prenom, String nom) {
	}
}
