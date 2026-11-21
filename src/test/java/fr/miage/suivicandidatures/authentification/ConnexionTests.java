package fr.miage.suivicandidatures.authentification;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import fr.miage.suivicandidatures.commun.UtilisateurRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConnexionTests {

	private final String email = "alex-" + UUID.randomUUID() + "@example.test";
	private final String motDePasse = UUID.randomUUID().toString();
	private final CookieManager cookies = new CookieManager();
	private final HttpClient navigateur = HttpClient.newBuilder().cookieHandler(cookies).build();

	@LocalServerPort
	private int port;

	@Autowired
	private UtilisateurRepository utilisateurs;

	@Autowired
	private JsonMapper json;

	@AfterEach
	void supprimerLeCompteDeTest() {
		navigateur.close();
		utilisateurs.findByEmail(email).ifPresent(utilisateurs::delete);
	}

	@Test
	void ouvreUneSessionRenouvelleLesJetonsEtInvalideLeCookieALaDeconnexion() throws Exception {
		inscrireAlex(motDePasse);
		assertThat(lireProfil().statusCode()).isEqualTo(401);
		String jetonInitial = obtenirJetonCsrf();

		String emailSaisi = "  " + email.toUpperCase(Locale.ROOT) + "  ";
		assertThat(seConnecter(emailSaisi, motDePasse, jetonInitial).statusCode()).isEqualTo(200);
		String session = cookie("JSESSIONID");
		String jetonConnecte = obtenirJetonCsrf();
		assertThat(jetonConnecte).isNotEqualTo(jetonInitial);

		HttpResponse<String> reponseProfil = lireProfil();
		assertThat(reponseProfil.statusCode()).isEqualTo(200);
		JsonNode profil = json.readTree(reponseProfil.body());
		assertThat(profil.size()).isEqualTo(4);
		assertThat(profil.path("id").asLong()).isPositive();
		assertThat(profil.path("prenom").asString()).isEqualTo("Alex");
		assertThat(profil.path("nom").asString()).isEqualTo("Martin");
		assertThat(profil.path("email").asString()).isEqualTo(email);

		assertThat(seDeconnecter(null).statusCode()).isEqualTo(403);
		assertThat(lireProfil().statusCode()).isEqualTo(200);

		// Envoie explicitement le même identifiant de session avant et après la déconnexion.
		HttpRequest requeteAncienneSession = requete("/moi").header("Cookie", "JSESSIONID=" + session).build();
		try (HttpClient autreNavigateur = HttpClient.newHttpClient()) {
			assertThat(autreNavigateur.send(requeteAncienneSession, HttpResponse.BodyHandlers.discarding())
					.statusCode()).isEqualTo(200);
			assertThat(seDeconnecter(jetonConnecte).statusCode()).isEqualTo(204);
			assertThat(autreNavigateur.send(requeteAncienneSession, HttpResponse.BodyHandlers.discarding())
					.statusCode()).isEqualTo(401);
		}
		assertThat(cookies.getCookieStore().getCookies()).noneMatch(cookie -> cookie.getName().equals("JSESSIONID"));

		String jetonDeconnecte = obtenirJetonCsrf();
		assertThat(jetonDeconnecte).isNotEqualTo(jetonConnecte);
		assertThat(seConnecter(email, motDePasse, jetonDeconnecte).statusCode()).isEqualTo(200);
		assertThat(lireProfil().statusCode()).isEqualTo(200);
	}

	@Test
	void renvoieLaMemeErreurPourUneAdresseInconnueEtUnMauvaisMotDePasse() throws Exception {
		inscrireAlex(motDePasse);
		String jeton = obtenirJetonCsrf();

		HttpResponse<String> motDePasseInvalide = seConnecter(email, UUID.randomUUID().toString(), jeton);
		HttpResponse<String> adresseInconnue = seConnecter("autre-" + email, motDePasse, jeton);
		assertThat(motDePasseInvalide.statusCode()).isEqualTo(401);
		assertThat(adresseInconnue.statusCode()).isEqualTo(401);
		assertThat(adresseInconnue.body()).isEqualTo(motDePasseInvalide.body()).isNotBlank();
		assertThat(lireProfil().statusCode()).isEqualTo(401);
	}

	@Test
	void exigeUnJetonCsrfValidePourLaConnexion() throws Exception {
		assertThat(seConnecter(email, motDePasse, null).statusCode()).isEqualTo(403);
		obtenirJetonCsrf();
		assertThat(seConnecter(email, motDePasse, "invalide").statusCode()).isEqualTo(403);
	}

	@Test
	void refuseUnMotDePasseAuDelaDeLaLimiteBCrypt() throws Exception {
		String motDePasseMaximal = motDePasse.repeat(2);
		inscrireAlex(motDePasseMaximal);
		String jeton = obtenirJetonCsrf();

		assertThat(seConnecter(email, motDePasseMaximal + "é", jeton).statusCode()).isEqualTo(401);
		assertThat(seConnecter(email, motDePasseMaximal, jeton).statusCode()).isEqualTo(200);
	}

	private void inscrireAlex(String motDePasseInscription) throws Exception {
		String corps = json.writeValueAsString(Map.of(
				"prenom", "Alex", "nom", "Martin", "email", email, "motDePasse", motDePasseInscription));
		assertThat(poster("/inscription", corps, "application/json", obtenirJetonCsrf()).statusCode())
				.isEqualTo(201);
	}

	private HttpResponse<String> seConnecter(String adresse, String motDePasseSaisi, String jeton) throws Exception {
		String corps = "email=" + URLEncoder.encode(adresse, StandardCharsets.UTF_8)
				+ "&motDePasse=" + URLEncoder.encode(motDePasseSaisi, StandardCharsets.UTF_8);
		return poster("/connexion", corps, "application/x-www-form-urlencoded", jeton);
	}

	private HttpResponse<String> seDeconnecter(String jeton) throws Exception {
		return poster("/deconnexion", "", "application/x-www-form-urlencoded", jeton);
	}

	private HttpResponse<String> lireProfil() throws Exception {
		return navigateur.send(requete("/moi").build(), HttpResponse.BodyHandlers.ofString());
	}

	private String obtenirJetonCsrf() throws Exception {
		assertThat(navigateur.send(requete("/csrf").build(), HttpResponse.BodyHandlers.discarding()).statusCode())
				.isEqualTo(204);
		return cookie("XSRF-TOKEN");
	}

	private String cookie(String nom) {
		return cookies.getCookieStore().getCookies().stream()
				.filter(cookie -> cookie.getName().equals(nom))
				.map(HttpCookie::getValue)
				.findFirst().orElseThrow(() -> new AssertionError("Cookie absent : " + nom));
	}

	private HttpResponse<String> poster(String route, String corps, String type, String jeton) throws Exception {
		HttpRequest.Builder requete = requete(route).header("Content-Type", type)
				.POST(HttpRequest.BodyPublishers.ofString(corps));
		if (jeton != null) {
			requete.header("X-XSRF-TOKEN", jeton);
		}
		return navigateur.send(requete.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpRequest.Builder requete(String route) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth" + route))
				.timeout(Duration.ofSeconds(10));
	}
}
