package fr.miage.suivicandidatures.cv;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

record ReponseImportCv(LangueCv langue, ContenuCv contenu) {

	private static final Pattern PERIODE = Pattern.compile(DatesCv.ANNEE_MOIS);
	private static final Pattern PERMIS = Pattern.compile(ContenuCv.CODES_PERMIS);

	static ReponseImportCv lire(JsonMapper json, String reponse) {
		if (reponse == null) {
			throw reponseIllisible();
		}
		String texte = reponse.strip();
		if (texte.startsWith("```") && texte.endsWith("```") && texte.indexOf('\n') >= 0) {
			texte = texte.substring(texte.indexOf('\n') + 1, texte.length() - 3).strip();
		}
		try {
			JsonNode racine = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(texte);
			JsonNode contenu = racine.path("contenu");
			if (!contenu.isObject()) {
				throw reponseIllisible();
			}
			LangueCv langue = LangueCv.valueOf(chaine(racine.path("langue")).toUpperCase(Locale.ROOT));
			return new ReponseImportCv(langue, nettoyer(contenu));
		} catch (JacksonException | IllegalArgumentException erreur) {
			throw reponseIllisible();
		}
	}

	private static ContenuCv nettoyer(JsonNode cv) {
		JsonNode identite = cv.path("identite");
		JsonNode disponibilite = cv.path("disponibilite");
		return new ContenuCv(
				new ContenuCv.Identite(texte(identite, "prenom", 160), texte(identite, "nom", 160),
						texte(identite, "titre", 160), texte(identite, "ville", 120), texte(identite, "email", 254),
						texte(identite, "telephone", 50), texte(identite, "mobilite", 160),
						liste(identite.path("permis"), noeud -> PERMIS.matcher(chaine(noeud)).matches(),
								ReponseImportCv::chaine),
						objets(identite, "liens", lien -> new ContenuCv.Lien(texte(lien, "libelle", 120), texte(lien, "url", 500)))),
				texte(cv, "profil", 3000),
				objets(cv, "competences", groupe -> new ContenuCv.GroupeCompetences(texte(groupe, "groupe", 120),
						textes(groupe, "elements", 120))),
				textes(cv, "savoirEtre", 120),
				textes(cv, "centresInteret", 120),
				objets(cv, "experiences", ReponseImportCv::experience),
				objets(cv, "formations", formation -> new ContenuCv.Formation(texte(formation, "diplome", 160),
						texte(formation, "etablissement", 160), texte(formation, "mention", 120),
						periode(formation, "debut"), periode(formation, "fin"), indicateur(formation, "enCours"),
						texte(formation, "description", 3000))),
				objets(cv, "projets", projet -> new ContenuCv.Projet(texte(projet, "titre", 160),
						texte(projet, "lien", 500), texte(projet, "description", 3000))),
				objets(cv, "langues", langue -> new ContenuCv.Langue(texte(langue, "langue", 80), texte(langue, "niveau", 40))),
				objets(cv, "certifications", certification -> new ContenuCv.Certification(texte(certification, "nom", 160),
						texte(certification, "organisme", 160), periode(certification, "date"), texte(certification, "lien", 500))),
				disponibilite(disponibilite));
	}

	private static ContenuCv.Experience experience(JsonNode experience) {
		boolean enCours = indicateur(experience, "enCours");
		return new ContenuCv.Experience(texte(experience, "poste", 160), texte(experience, "entreprise", 160),
				texte(experience, "lieu", 120), periode(experience, "debut"), enCours ? "" : periode(experience, "fin"),
				enCours, texte(experience, "description", 3000), textes(experience, "realisations", 1000));
	}

	private static ContenuCv.Disponibilite disponibilite(JsonNode disponibilite) {
		boolean immediate = indicateur(disponibilite, "immediate");
		String date = chaine(disponibilite.path("date"));
		if (date.length() == 7 && PERIODE.matcher(date).matches()) {
			date += "-01";
		}
		return new ContenuCv.Disponibilite(immediate, !immediate && DatesCv.jourValide(date) ? date : "");
	}

	private static String periode(JsonNode objet, String champ) {
		String date = chaine(objet.path(champ));
		return PERIODE.matcher(date).matches() ? date : "";
	}

	private static boolean indicateur(JsonNode objet, String champ) {
		JsonNode valeur = objet.path(champ);
		return valeur.isBoolean() && valeur.booleanValue();
	}

	private static String chaine(JsonNode noeud) {
		return ContenuCv.sansEspacesAuxBords(noeud.stringValue(""));
	}

	private static String texte(JsonNode objet, String champ, int maximum) {
		return raccourcir(chaine(objet.path(champ)), maximum);
	}

	private static String raccourcir(String texte, int maximum) {
		if (texte.length() <= maximum) {
			return texte;
		}
		int fin = Character.isHighSurrogate(texte.charAt(maximum - 1)) ? maximum - 1 : maximum;
		return texte.substring(0, fin);
	}

	private static List<String> textes(JsonNode objet, String champ, int maximum) {
		return liste(objet.path(champ), JsonNode::isString, noeud -> raccourcir(chaine(noeud), maximum));
	}

	private static <T> List<T> objets(JsonNode objet, String champ, Function<JsonNode, T> convertir) {
		return liste(objet.path(champ), JsonNode::isObject, convertir);
	}

	private static <T> List<T> liste(JsonNode champ, Predicate<JsonNode> accepter, Function<JsonNode, T> convertir) {
		if (!champ.isArray()) {
			return List.of();
		}
		List<T> resultat = new ArrayList<>();
		for (JsonNode element : champ) {
			if (accepter.test(element)) {
				resultat.add(convertir.apply(element));
				if (resultat.size() == 30) {
					break;
				}
			}
		}
		return List.copyOf(resultat);
	}

	private static ResponseStatusException reponseIllisible() {
		return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
				"L'IA a renvoyé une réponse illisible, réessayez.");
	}
}
