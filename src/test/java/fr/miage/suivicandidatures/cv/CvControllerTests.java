package fr.miage.suivicandidatures.cv;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.miage.suivicandidatures.commun.UtilisateurRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CvControllerTests {

	private static final String API_CV = "/api/v1/cv";
	private static final String API_AUTH = "/api/v1/auth";
	private static final ContenuCv.Identite IDENTITE = new ContenuCv.Identite(
			"Alex Moreau", "Data engineer", "Paris", "alex@example.test", "06 00 00 00 00",
			"github.com/alex", "J'écris des pipelines de données.");

	private final List<Navigateur> navigateurs = new ArrayList<>();
	private final List<String> emails = new ArrayList<>();

	@LocalServerPort
	private int port;

	@Autowired
	private JsonMapper json;

	@Autowired
	private UtilisateurRepository utilisateurs;

	@Autowired
	private JdbcTemplate jdbc;

	@AfterEach
	void nettoyer() {
		navigateurs.forEach(Navigateur::close);
		emails.forEach(email -> utilisateurs.findByEmail(email).ifPresent(utilisateurs::delete));
	}

	@Test
	void creeEtRelitUnCvCompletStockeEnJsonb() throws Exception {
		Navigateur candidat = connecter();
		DemandeCv demande = exemple("Data engineer");

		HttpResponse<String> creation = candidat.envoyerCv("POST", "", demande);
		assertThat(creation.statusCode()).isEqualTo(201);
		JsonNode cv = json.readTree(creation.body());
		long id = cv.path("id").asLong();
		assertThat(id).isPositive();
		assertThat(creation.headers().firstValue("Location")).contains(API_CV + "/" + id);
		assertThat(cv.size()).isEqualTo(8);
		assertThat(cv.path("titre").asString()).isEqualTo(demande.titre());
		assertThat(cv.path("langue").asString()).isEqualTo("FR");
		assertThat(cv.path("modele").asString()).isEqualTo("MODERNE");
		assertThat(cv.path("police").asString()).isEqualTo("LATO");
		assertThat(date(cv, "dateCreation")).isEqualTo(date(cv, "dateModification"));
		JsonNode contenu = json.valueToTree(demande.contenu());
		assertThat(cv.path("contenu")).isEqualTo(contenu);

		String type = jdbc.queryForObject("SELECT pg_typeof(contenu)::text FROM cv WHERE id = ?", String.class, id);
		String document = jdbc.queryForObject("SELECT contenu::text FROM cv WHERE id = ?", String.class, id);
		assertThat(type).isEqualTo("jsonb");
		assertThat(json.readTree(document)).isEqualTo(contenu);
		assertThat(lireCv(candidat, id)).isEqualTo(cv);
		assertThat(listerCv(candidat)).containsExactly(cv);
	}

	@Test
	void remplaceLeCvEntierEtLeReplaceEnTeteAModification() throws Exception {
		Navigateur candidat = connecter();
		JsonNode premier = creerCv(candidat, exemple("Premier CV"));
		JsonNode second = creerCv(candidat, exemple("Second CV"));
		assertThat(listerCv(candidat)).containsExactly(second, premier);

		long id = premier.path("id").asLong();
		ObjectNode demande = ((ObjectNode) premier).deepCopy();
		demande.put("id", second.path("id").asLong());
		demande.put("dateCreation", "2000-01-01T00:00:00Z");
		demande.put("titre", "CV réécrit");
		demande.put("langue", "EN");
		JsonNode contenu = json.valueToTree(new ContenuCv(IDENTITE, List.of(), List.of(), List.of(), List.of()));
		demande.set("contenu", contenu);
		HttpResponse<String> miseAJour = candidat.envoyerCv("PUT", "/" + id, demande);
		assertThat(miseAJour.statusCode()).isEqualTo(200);
		JsonNode cv = json.readTree(miseAJour.body());
		assertThat(cv.path("id").asLong()).isEqualTo(id);
		assertThat(cv.path("titre").asString()).isEqualTo("CV réécrit");
		assertThat(cv.path("langue").asString()).isEqualTo("EN");
		assertThat(cv.path("contenu")).isEqualTo(contenu);
		assertThat(date(cv, "dateCreation")).isEqualTo(date(premier, "dateCreation"));
		assertThat(date(cv, "dateModification")).isAfter(date(second, "dateModification"));
		assertThat(listerCv(candidat)).containsExactly(cv, second);

		HttpResponse<String> memeContenu = candidat.envoyerCv("PUT", "/" + id, cv);
		assertThat(memeContenu.statusCode()).isEqualTo(200);
		JsonNode relu = lireCv(candidat, id);
		assertThat(date(relu, "dateModification")).isAfter(date(cv, "dateModification"));
		assertThat(relu.path("contenu")).isEqualTo(contenu);
	}

	@Test
	void dupliquePuisSupprimeLOriginalSansAffecterLaCopie() throws Exception {
		Navigateur candidat = connecter();
		JsonNode original = creerCv(candidat, exemple("Data engineer"));
		long id = original.path("id").asLong();
		JsonNode copie = dupliquerCv(candidat, id);
		long idCopie = copie.path("id").asLong();

		assertThat(idCopie).isNotEqualTo(id);
		assertThat(copie.path("titre").asString()).isEqualTo("Data engineer (copie)");
		for (String champ : List.of("langue", "modele", "police", "contenu")) {
			assertThat(copie.path(champ)).isEqualTo(original.path(champ));
		}
		assertThat(date(copie, "dateCreation")).isAfter(date(original, "dateCreation"));
		assertThat(date(copie, "dateModification")).isEqualTo(date(copie, "dateCreation"));
		HttpResponse<String> suppression = candidat.envoyerCv("DELETE", "/" + id, null);
		assertThat(suppression.statusCode()).isEqualTo(204);
		assertThat(suppression.body()).isEmpty();
		assertThat(probleme(candidat.envoyerCv("GET", "/" + id, null), 404)).isEqualTo("Ce CV n'existe pas.");
		assertThat(lireCv(candidat, idCopie)).isEqualTo(copie);
		assertThat(listerCv(candidat)).containsExactly(copie);
	}

	@Test
	void raccourcitUnTitreDe120CaracteresSansScinderLesAccentsOuLesEmojis() throws Exception {
		Navigateur candidat = connecter();
		String prefixe = "A".repeat(110);
		Map<String, String> titres = Map.of(
				"A".repeat(120), "A".repeat(112) + " (copie)",
				prefixe + " e\u0301" + "A".repeat(7), prefixe + " (copie)",
				prefixe + "A👩🏽‍💻" + "A".repeat(5), prefixe + "A (copie)");
		for (var cas : titres.entrySet()) {
			String titre = cas.getKey();
			assertThat(titre.codePointCount(0, titre.length())).isEqualTo(120);
			JsonNode original = creerCv(candidat, exemple(titre));
			JsonNode copie = dupliquerCv(candidat, original.path("id").asLong());
			assertThat(copie.path("titre").asString()).isEqualTo(cas.getValue());
		}
	}

	@Test
	void isoleLesCvDeDeuxComptes() throws Exception {
		Navigateur alex = connecter();
		Navigateur camille = connecter();
		JsonNode cvAlex = creerCv(alex, exemple("CV Alex"));
		JsonNode cvCamille = creerCv(camille, exemple("CV Camille"));
		String routeAlex = "/" + cvAlex.path("id").asLong();

		assertThat(listerCv(alex)).containsExactly(cvAlex);
		assertThat(listerCv(camille)).containsExactly(cvCamille);
		String detail = probleme(camille.envoyerCv("GET", routeAlex, null), 404);
		assertThat(probleme(camille.envoyerCv("PUT", routeAlex, exemple("Modification étrangère")), 404)).isEqualTo(detail);
		assertThat(probleme(camille.envoyerCv("POST", routeAlex + "/copie", null), 404)).isEqualTo(detail);
		assertThat(probleme(camille.envoyerCv("DELETE", routeAlex, null), 404)).isEqualTo(detail);
		assertThat(probleme(camille.envoyerCv("GET", "/0", null), 404)).isEqualTo(detail);
		assertThat(listerCv(alex)).containsExactly(cvAlex);
		assertThat(listerCv(camille)).containsExactly(cvCamille);
	}

	@Test
	void exigeUneSessionCandidat() throws Exception {
		Navigateur anonyme = navigateur();
		assertThat(anonyme.envoyerCv("GET", "", null).statusCode()).isEqualTo(401);
		assertThat(anonyme.envoyerCv("POST", "", exemple("CV anonyme")).statusCode()).isEqualTo(401);
	}

	@Test
	void renvoieLesErreursDeValidationEnProblemDetail() throws Exception {
		Navigateur candidat = connecter();
		assertThat(probleme(candidat.envoyerCv("POST", "", exemple("   ")), 400))
				.isEqualTo("Le titre du CV est obligatoire.");
		assertThat(probleme(candidat.envoyerCv("POST", "", exemple("A".repeat(121))), 400)).contains("120 caractères");

		ObjectNode sansNom = json.valueToTree(exemple("CV sans nom"));
		((ObjectNode) sansNom.path("contenu").path("identite")).remove("nom");
		assertThat(probleme(candidat.envoyerCv("POST", "", sansNom), 400)).isEqualTo("Le nom est obligatoire.");

		ObjectNode champsAbsents = sansNom.deepCopy();
		champsAbsents.remove(List.of("langue", "modele", "titre"));
		assertThat(probleme(candidat.envoyerCv("POST", "", champsAbsents), 400)).isEqualTo(
				"La langue du CV est obligatoire. Le modèle du CV est obligatoire. "
						+ "Le nom est obligatoire. Le titre du CV est obligatoire.");

		ObjectNode tropDeRealisations = json.valueToTree(exemple("CV trop long"));
		ArrayNode experiences = (ArrayNode) tropDeRealisations.path("contenu").path("experiences");
		ObjectNode experience = (ObjectNode) experiences.path(0);
		experience.set("realisations", json.valueToTree(Collections.nCopies(31, "Une réalisation")));
		String detail = probleme(candidat.envoyerCv("POST", "", tropDeRealisations), 400);
		assertThat(detail).contains("30").doesNotContain("contenu.", "realisations").endsWith(".");
		experiences.add(experience.deepCopy());
		assertThat(probleme(candidat.envoyerCv("POST", "", tropDeRealisations), 400)).isEqualTo(detail);

		ObjectNode modeleInvalide = json.valueToTree(exemple("CV invalide"));
		modeleInvalide.put("modele", "INCONNU");
		assertThat(probleme(candidat.envoyerCv("POST", "", modeleInvalide), 400)).contains("modèle");
		assertThat(probleme(candidat.envoyerCv("GET", "/invalide", null), 400)).contains("identifiant du CV");
		assertThat(listerCv(candidat)).isEmpty();
	}

	@Test
	void appliqueLaPoliceParDefautDuModeleALaCreationEtALaModification() throws Exception {
		Navigateur candidat = connecter();
		Map<ModeleCv, PoliceCv> polices = Map.of(ModeleCv.CLASSIQUE, PoliceCv.SOURCE_SERIF_4,
				ModeleCv.MODERNE, PoliceCv.INTER, ModeleCv.COMPACT, PoliceCv.IBM_PLEX_SANS);
		for (var choix : polices.entrySet()) {
			ObjectNode demande = json.valueToTree(exemple("CV sans police"));
			demande.put("modele", choix.getKey().name());
			demande.remove("police");
			JsonNode cv = creerCv(candidat, demande);
			assertThat(cv.path("police").asString()).isEqualTo(choix.getValue().name());
		}

		JsonNode cv = creerCv(candidat, exemple("CV avec police"));
		long id = cv.path("id").asLong();
		ObjectNode demande = ((ObjectNode) cv).deepCopy();
		demande.put("modele", "COMPACT");
		demande.remove("police");
		assertThat(candidat.envoyerCv("PUT", "/" + id, demande).statusCode()).isEqualTo(200);
		assertThat(lireCv(candidat, id).path("police").asString()).isEqualTo("IBM_PLEX_SANS");
	}

	private static DemandeCv exemple(String titre) {
		ContenuCv contenu = new ContenuCv(IDENTITE,
				List.of(new ContenuCv.Experience("Stagiaire", "Nordlys", "Paris", "04/2026", "En cours",
						List.of("Réduit les durées de 40 %", "Automatisé le chargement"))),
				List.of(new ContenuCv.Formation("Master MIAGE", "Paris Nanterre", "2026", "2027")),
				List.of(new ContenuCv.GroupeCompetences("Langages", List.of("SQL", "Python"))),
				List.of(new ContenuCv.Langue("Anglais", "C1")));
		return new DemandeCv(titre, LangueCv.FR, ModeleCv.MODERNE, PoliceCv.LATO, contenu);
	}

	private static Instant date(JsonNode cv, String champ) {
		return Instant.parse(cv.path(champ).asString());
	}

	private JsonNode creerCv(Navigateur candidat, Object demande) throws Exception {
		HttpResponse<String> reponse = candidat.envoyerCv("POST", "", demande);
		assertThat(reponse.statusCode()).as(reponse.body()).isEqualTo(201);
		return json.readTree(reponse.body());
	}

	private JsonNode dupliquerCv(Navigateur candidat, long id) throws Exception {
		HttpResponse<String> reponse = candidat.envoyerCv("POST", "/" + id + "/copie", null);
		assertThat(reponse.statusCode()).as(reponse.body()).isEqualTo(201);
		return json.readTree(reponse.body());
	}

	private JsonNode lireCv(Navigateur candidat, long id) throws Exception {
		HttpResponse<String> reponse = candidat.envoyerCv("GET", "/" + id, null);
		assertThat(reponse.statusCode()).isEqualTo(200);
		return json.readTree(reponse.body());
	}

	private JsonNode listerCv(Navigateur candidat) throws Exception {
		HttpResponse<String> reponse = candidat.envoyerCv("GET", "", null);
		assertThat(reponse.statusCode()).isEqualTo(200);
		return json.readTree(reponse.body());
	}

	private String probleme(HttpResponse<String> reponse, int statut) {
		assertThat(reponse.statusCode()).as(reponse.body()).isEqualTo(statut);
		assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
		JsonNode probleme = json.readTree(reponse.body());
		assertThat(probleme.path("status").asInt()).isEqualTo(statut);
		String detail = probleme.path("detail").asString();
		assertThat(detail).isNotBlank();
		return detail;
	}

	private Navigateur connecter() throws Exception {
		Navigateur navigateur = navigateur();
		String email = "candidat-cv-" + UUID.randomUUID() + "@example.test";
		String motDePasse = UUID.randomUUID().toString();
		emails.add(email);
		String inscription = json.writeValueAsString(Map.of(
				"prenom", "Alex", "nom", "Moreau", "email", email, "motDePasse", motDePasse));
		assertThat(navigateur.envoyer("POST", API_AUTH + "/inscription", "application/json", inscription).statusCode())
				.isEqualTo(201);

		String connexion = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&motDePasse=" + motDePasse;
		assertThat(navigateur.envoyer("POST", API_AUTH + "/connexion", "application/x-www-form-urlencoded", connexion)
				.statusCode()).isEqualTo(200);
		return navigateur;
	}

	private Navigateur navigateur() {
		Navigateur navigateur = new Navigateur();
		navigateurs.add(navigateur);
		return navigateur;
	}

	private class Navigateur implements AutoCloseable {

		private final CookieManager cookies = new CookieManager();
		private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();

		HttpResponse<String> envoyerCv(String methode, String route, Object corps) throws Exception {
			return envoyer(methode, API_CV + route, "application/json", corps == null ? null : json.writeValueAsString(corps));
		}

		HttpResponse<String> envoyer(String methode, String route, String type, String corps) throws Exception {
			HttpRequest.Builder requete = requete(route).method(methode, corps == null
					? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corps));
			if (corps != null) {
				requete.header("Content-Type", type);
			}
			if (!methode.equals("GET")) {
				requete.header("X-XSRF-TOKEN", obtenirJetonCsrf());
			}
			return client.send(requete.build(), HttpResponse.BodyHandlers.ofString());
		}

		private String obtenirJetonCsrf() throws Exception {
			assertThat(client.send(requete(API_AUTH + "/csrf").build(), HttpResponse.BodyHandlers.discarding()).statusCode())
					.isEqualTo(204);
			return cookies.getCookieStore().getCookies().stream()
					.filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
					.map(HttpCookie::getValue).findFirst().orElseThrow();
		}

		private HttpRequest.Builder requete(String route) {
			return HttpRequest.newBuilder(URI.create("http://localhost:" + port + route))
					.header("Accept-Language", "fr").timeout(Duration.ofSeconds(10));
		}

		@Override
		public void close() {
			client.close();
		}
	}
}
