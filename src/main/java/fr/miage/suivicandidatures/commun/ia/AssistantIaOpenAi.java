package fr.miage.suivicandidatures.commun.ia;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AssistantIaOpenAi implements AssistantIa {

	private static final Logger LOG = LoggerFactory.getLogger(AssistantIaOpenAi.class);
	private static final JsonMapper LECTEUR_JSON = JsonMapper.builder().build();
	private static final Pattern IDENTIFIANT_ERREUR = Pattern.compile("[a-z][a-z0-9_]{0,79}");

	private final ProprietesIa proprietes;
	private final RestClient client;

	AssistantIaOpenAi(ProprietesIa proprietes, RestClient.Builder constructeur) {
		this.proprietes = proprietes;
		this.client = constructeur.baseUrl(proprietes.url()).build();
	}

	@Override
	public String generer(String consignes, String demande) {
		return generer(consignes, demande, false);
	}

	@Override
	public String genererAvecImages(String consignes, List<Partie> demande) {
		List<Map<String, ?>> contenu = demande.stream().map(AssistantIaOpenAi::coderPartie).toList();
		return generer(consignes, contenu, true);
	}

	private static Map<String, ?> coderPartie(Partie partie) {
		return switch (partie) {
			case Texte texte -> Map.of("type", "text", "text", texte.contenu());
			case Image image -> Map.of("type", "image_url", "image_url", Map.of("url",
					"data:" + image.typeMime() + ";base64," + Base64.getEncoder().encodeToString(image.octets())));
		};
	}

	private String generer(String consignes, Object demande, boolean avecImages) {
		if (proprietes.cle() == null || proprietes.cle().isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "L'assistant IA n'est pas configuré");
		}

		Requete corps = new Requete(proprietes.modele(), List.of(
				Map.of("role", "developer", "content", consignes),
				Map.of("role", "user", "content", demande)));

		JsonNode reponse;
		try {
			reponse = client.post()
					.uri("/v1/chat/completions")
					.headers(enTetes -> enTetes.setBearerAuth(proprietes.cle()))
					.contentType(MediaType.APPLICATION_JSON)
					.body(corps)
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientResponseException erreur) {
			int statutHttp = erreur.getStatusCode().value();
			journaliserErreurHttp(statutHttp, erreur.getResponseBodyAsString());
			if (statutHttp == 429) {
				throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
						"Limite de l'assistant IA atteinte, réessayez plus tard");
			}
			if (avecImages && (statutHttp == 400 || statutHttp == 415 || statutHttp == 422)) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
						"Le modèle d'IA configuré ne sait pas lire les images.");
			}
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "L'assistant IA a refusé la requête");
		} catch (RestClientException erreur) {
			LOG.warn("Appel OpenAI impossible ({})", erreur.getClass().getSimpleName());
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "L'assistant IA est indisponible");
		}

		String texte = reponse == null ? null : reponse.path("choices").path(0)
				.path("message").path("content").stringValue(null);
		if (texte == null || texte.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "L'assistant IA a renvoyé une réponse vide");
		}
		return texte;
	}

	private static void journaliserErreurHttp(int statutHttp, String corps) {
		try {
			JsonNode erreur = LECTEUR_JSON.readTree(corps).path("error");
			String code = erreur.path("code").stringValue("");
			String type = erreur.path("type").stringValue("");
			if (IDENTIFIANT_ERREUR.matcher(code).matches() && IDENTIFIANT_ERREUR.matcher(type).matches()) {
				LOG.warn("OpenAI HTTP {} (error.code={}, error.type={})", statutHttp, code, type);
				return;
			}
		} catch (JacksonException ignoree) {
			// Le corps peut être vide ou provenir d'un intermédiaire HTTP.
		}
		LOG.warn("OpenAI HTTP {}", statutHttp);
	}

	private record Requete(String model, List<Map<String, Object>> messages) {

		@Override
		public String toString() {
			// RestClient affiche cette représentation quand ses logs DEBUG sont activés.
			return "Requête de génération IA";
		}
	}
}
