package fr.miage.suivicandidatures.cv;

import static java.util.stream.Collectors.joining;
import static fr.miage.suivicandidatures.cv.ContenuCv.sansEspacesAuxBords;

import java.io.IOException;
import java.net.URI;
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

	private static final Pattern SCHEMA = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:");
	private static final Pattern HTTP = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);

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
		contexte.setVariable("nom", siRenseigne(identite.nomComplet()));
		contexte.setVariable("posteVise", siRenseigne(identite.titre()));
		contexte.setVariable("profil", siRenseigne(contenu.profil()));
		contexte.setVariable("contact", contact(identite));
		contexte.setVariable("complements", complements(contenu, anglais));
		contexte.setVariable("experiences", contenu.experiences().stream()
				.filter(e -> renseigne(e.poste()) || renseigne(e.entreprise()))
				.map(e -> new Experience(e.poste(), siRenseigne(e.entreprise()), siRenseigne(e.lieu()),
						periode(e.debut(), e.fin(), e.enCours(), true, anglais),
						siRenseigne(e.description()), e.realisations().stream().map(ContenuCv::sansEspacesAuxBords)
						.filter(r -> !r.isEmpty()).toList()))
				.toList());
		contexte.setVariable("projets", contenu.projets().stream().filter(p -> renseigne(p.titre()))
				.map(p -> new Projet(p.titre(), adresseWeb(p.lien()), siRenseigne(p.description()))).toList());
		contexte.setVariable("formations", contenu.formations().stream().filter(f -> renseigne(f.diplome()))
				.map(f -> new Formation(f.diplome(), siRenseigne(f.etablissement()), siRenseigne(f.mention()),
						periode(f.debut(), f.fin(), f.enCours(), false, anglais), siRenseigne(f.description())))
				.toList());
		contexte.setVariable("certifications", contenu.certifications().stream().filter(c -> renseigne(c.nom()))
				.map(c -> new Certification(c.nom(), siRenseigne(c.organisme()),
						DatesCv.formaterDate(sansEspacesAuxBords(c.date()), anglais), adresseWeb(c.lien())))
				.toList());
		contexte.setVariable("competences", contenu.competences().stream().filter(g -> !g.elements().isEmpty())
				.map(g -> new Groupe(siRenseigne(g.groupe()), String.join(", ", g.elements())))
				.toList());
		contexte.setVariable("savoirEtre", contenu.savoirEtre().stream().filter(CvHtml::renseigne).collect(joining(", ")));
		contexte.setVariable("langues", contenu.langues().stream().filter(l -> renseigne(l.langue()))
				.map(l -> renseigne(l.niveau()) ? l.langue() + " (" + l.niveau() + ")" : l.langue())
				.collect(joining(" · ")));
		contexte.setVariable("interets", contenu.centresInteret().stream().filter(CvHtml::renseigne).collect(joining(" · ")));
		return moteur.process("cv/cv", contexte);
	}

	private static String periode(String debut, String fin, boolean enCours, boolean ouverte, boolean anglais) {
		return DatesCv.periode(sansEspacesAuxBords(debut), sansEspacesAuxBords(fin), enCours, ouverte, anglais);
	}

	private static List<Contact> contact(ContenuCv.Identite identite) {
		return Stream.concat(
				Stream.of(identite.ville(), identite.email(), identite.telephone()).filter(CvHtml::renseigne)
						.map(texte -> new Contact(texte, null)),
				identite.liens().stream().map(lien -> new Contact(sansEspacesAuxBords(lien.url()), adresseWeb(lien.url())))
						.filter(lien -> lien.url() != null)).toList();
	}

	private static String complements(ContenuCv contenu, boolean anglais) {
		List<String> permis = contenu.identite().permis();
		String conduite = permis.isEmpty() ? "" : (anglais ? "Driving licence " : "Permis ") + String.join(", ", permis);
		ContenuCv.Disponibilite disponibilite = contenu.disponibilite();
		String disponible = "";
		if (disponibilite.immediate()) {
			disponible = anglais ? "Available immediately" : "Disponible immédiatement";
		} else if (renseigne(disponibilite.date())) {
			disponible = (anglais ? "Available from " : "Disponible à partir du ")
					+ DatesCv.formaterJour(disponibilite.date(), anglais);
		}
		return Stream.of(conduite, disponible).filter(CvHtml::renseigne).collect(joining(" · "));
	}

	private static String adresseWeb(String valeur) {
		String adresse = sansEspacesAuxBords(valeur);
		boolean http = HTTP.matcher(adresse).find();
		if (adresse.isEmpty() || (!http && SCHEMA.matcher(adresse).find())) {
			return null;
		}
		String url = http ? adresse : "https://" + adresse;
		try {
			return URI.create(url).getRawAuthority() == null ? null : url;
		} catch (IllegalArgumentException erreur) {
			return null;
		}
	}

	private static String siRenseigne(String texte) {
		return renseigne(texte) ? texte : null;
	}

	private static boolean renseigne(String texte) {
		return !sansEspacesAuxBords(texte).isEmpty();
	}

	record Contact(String texte, String url) {
	}

	record Experience(String poste, String entreprise, String lieu, String periode, String description,
			List<String> realisations) {
	}

	record Formation(String diplome, String etablissement, String mention, String periode, String description) {
	}

	record Projet(String titre, String url, String description) {
	}

	record Certification(String nom, String organisme, String date, String url) {
	}

	record Groupe(String nom, String elements) {
	}
}
