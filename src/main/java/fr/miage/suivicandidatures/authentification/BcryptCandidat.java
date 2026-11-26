package fr.miage.suivicandidatures.authentification;

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

final class BcryptCandidat extends BCryptPasswordEncoder {

	static boolean longueurCompatible(String motDePasse) {
		return motDePasse.getBytes(StandardCharsets.UTF_8).length <= 72;
	}

	@Override
	protected boolean matchesNonNull(String motDePasse, String hachage) {
		return longueurCompatible(motDePasse) && super.matchesNonNull(motDePasse, hachage);
	}
}
