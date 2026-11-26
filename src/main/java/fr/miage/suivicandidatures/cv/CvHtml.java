package fr.miage.suivicandidatures.cv;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
class CvHtml {

	// Mêmes caractères d'espacement que String.trim() en JavaScript, y compris les espaces insécables.
	private static final String ESPACES = "[\\p{Zs}\\t\\n\\r\\f\\x0B\\u2028\\u2029\\uFEFF]";
	private static final Pattern BORDS = Pattern.compile("\\A" + ESPACES + "+|" + ESPACES + "+\\z");

	private final SpringTemplateEngine moteur;
	private final String css;

	CvHtml(SpringTemplateEngine moteur) throws IOException {
		this.moteur = moteur;
		css = new ClassPathResource("static/cv-modeles.css").getContentAsString(StandardCharsets.UTF_8);
	}

	String rendre(Cv cv) {
		ContenuCv contenu = cv.getContenu();
		ContenuCv.Identite identite = contenu.identite();
		boolean anglais = cv.getLangue() == LangueCv.EN;
		Context contexte = new Context(anglais ? Locale.ENGLISH : Locale.FRENCH);
		contexte.setVariable("anglais", anglais);
		contexte.setVariable("langue", anglais ? "en" : "fr");
		contexte.setVariable("titreCv", cv.getTitre());
		contexte.setVariable("classeCv", "cv modele-" + cv.getModele().name().toLowerCase(Locale.ROOT));
		contexte.setVariable("styleCv", "--cv-police: " + cv.getPolice().familleCss() + ";");
		contexte.setVariable("css", css);
		contexte.setVariable("nom", siRenseigne(identite.nom()));
		contexte.setVariable("posteVise", siRenseigne(identite.titre()));
		contexte.setVariable("profil", siRenseigne(identite.profil()));
		contexte.setVariable("contact", Stream.of(identite.ville(), identite.email(), identite.telephone(), identite.lien())
				.filter(CvHtml::renseigne).collect(joining(" · ")));
		contexte.setVariable("experiences", contenu.experiences().stream()
				.filter(e -> renseigne(e.poste()) || renseigne(e.entreprise()))
				.map(e -> new Experience(e.poste(), siRenseigne(e.entreprise()), siRenseigne(e.lieu()),
						periode(e.debut(), e.fin()), e.realisations().stream().map(CvHtml::sansEspacesAuxBords)
						.filter(r -> !r.isEmpty()).toList()))
				.toList());
		contexte.setVariable("formations", contenu.formations().stream().filter(f -> renseigne(f.diplome()))
				.map(f -> new Formation(f.diplome(), siRenseigne(f.ecole()), periode(f.debut(), f.fin())))
				.toList());
		contexte.setVariable("competences", contenu.competences().stream().filter(g -> !g.elements().isEmpty())
				.map(g -> new Groupe(siRenseigne(g.groupe()), String.join(", ", g.elements())))
				.toList());
		contexte.setVariable("langues", contenu.langues().stream().filter(l -> renseigne(l.langue()))
				.map(l -> renseigne(l.niveau()) ? l.langue() + " (" + l.niveau() + ")" : l.langue())
				.collect(joining(" · ")));
		return moteur.process("cv/cv", contexte);
	}

	private static String periode(String debut, String fin) {
		return Stream.of(debut, fin).map(CvHtml::sansEspacesAuxBords)
				.filter(texte -> !texte.isEmpty()).collect(joining(" – "));
	}

	private static String siRenseigne(String texte) {
		return renseigne(texte) ? texte : null;
	}

	private static boolean renseigne(String texte) {
		return !sansEspacesAuxBords(texte).isEmpty();
	}

	private static String sansEspacesAuxBords(String texte) {
		return texte == null ? "" : BORDS.matcher(texte).replaceAll("");
	}

	record Experience(String poste, String entreprise, String lieu, String periode, List<String> realisations) {
	}

	record Formation(String diplome, String ecole, String periode) {
	}

	record Groupe(String nom, String elements) {
	}
}
