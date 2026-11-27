package fr.miage.suivicandidatures.commun.ia;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public interface AssistantIa {

	String generer(String consignes, String demande);

	default String genererAvecImages(String consignes, List<Partie> demande) {
		throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Le modèle d'IA configuré ne sait pas lire les images.");
	}

	sealed interface Partie permits Texte, Image {
	}

	record Texte(String contenu) implements Partie {
	}

	record Image(String typeMime, byte[] octets) implements Partie {
	}
}
