package fr.miage.suivicandidatures.commun;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CandidatCourant {

	public Long id() {
		Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
		if (authentification != null && authentification.isAuthenticated()
				&& authentification.getPrincipal() instanceof Identite candidat) {
			return candidat.id();
		}
		throw new AuthenticationCredentialsNotFoundException("Aucun candidat connecté");
	}

	public interface Identite {

		Long id();
	}
}
