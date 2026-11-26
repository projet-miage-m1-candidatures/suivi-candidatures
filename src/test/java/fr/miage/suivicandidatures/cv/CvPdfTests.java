package fr.miage.suivicandidatures.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class CvPdfTests {

	private final CvHtml html = new CvHtml(moteur());
	private final CvPdf pdf = new CvPdf(html);

	CvPdfTests() throws IOException {
	}

	@ParameterizedTest
	@MethodSource("modelesEtPolices")
	void genereUnPdfA4AvecDuTexteEtLesPolicesIntegrees(ModeleCv modele, PoliceCv police) throws IOException {
		Cv cv = cv(modele, police, LangueCv.FR, "Profil de test", List.of(experience(0)), List.of());
		byte[] resultat = pdf.generer(cv);
		assertThat(resultat).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
		try (PDDocument document = Loader.loadPDF(resultat)) {
			assertThat(document.getNumberOfPages()).isEqualTo(1);
			PDPage page = document.getPage(0);
			assertThat(page.getMediaBox().getWidth()).isCloseTo(595.28f, within(0.5f));
			assertThat(page.getMediaBox().getHeight()).isCloseTo(841.89f, within(0.5f));
			assertThat(new PDFTextStripper().getText(document)).contains("Élodie Dupont", "Profil de test");
			List<String> fontes = new ArrayList<>();
			for (var cle : page.getResources().getFontNames()) {
				PDFont fonte = page.getResources().getFont(cle);
				assertThat(fonte.isEmbedded()).as(fonte.getName()).isTrue();
				fontes.add(fonte.getName().replaceFirst("^[A-Z]{6}\\+", ""));
			}
			String famille = police.famille().replace(" ", "");
			String regular = police == PoliceCv.IBM_PLEX_SANS ? famille : famille + "-Regular";
			assertThat(fontes).contains(regular, famille + "-Bold");
			if (modele == ModeleCv.CLASSIQUE && police == PoliceCv.SOURCE_SERIF_4) {
				assertThat(fontes).anyMatch(nom -> nom.startsWith("SourceSerif4-It"));
			}
		}
	}

	@Test
	void echappeLesBalisesSaisiesEtImprimeLeurTexte() throws IOException {
		String profil = "<script>alert('test')</script> & du texte";
		Cv cv = cv(ModeleCv.MODERNE, PoliceCv.INTER, LangueCv.EN, profil, List.of(), List.of());
		assertThat(html.rendre(cv)).contains("&lt;script&gt;", "&lt;/script&gt;", "&amp; du texte", ">Profile</h2>")
				.doesNotContain("<script>");
		try (PDDocument document = Loader.loadPDF(pdf.generer(cv))) {
			assertThat(new PDFTextStripper().getText(document)).contains("Profile", profil);
		}
	}

	@Test
	void restitueToutesLesSectionsDansLOrdreAvecLesBonsLiens() throws IOException {
		Cv cv = cvComplet(LangueCv.FR);
		String gabarit = html.rendre(cv);
		List<String> rubriques = List.of("Profil", "Compétences", "Expériences", "Projets", "Formation",
				"Certifications", "Langues", "Centres d'intérêt");
		assertThat(Pattern.compile("<h2>([^<]+)</h2>").matcher(gabarit).results().map(resultat -> resultat.group(1)))
				.containsExactlyElementsOf(rubriques);
		assertThat(gabarit).contains("<p class=\"cv-description\">Maintenance de la plateforme.</p>",
				"<li>Déployé le pipeline.</li>", "href=\"https://github.com/elodie\"", ">github.com/elodie</a>",
				"href=\"https://example.test/projet\"", ">Entrepôt de données</a>",
				"href=\"http://example.test/badge\"", ">Certificat SQL</a>")
				.doesNotContain("javascript:", ">https://example.test/projet<", ">http://example.test/badge<");
		assertThat(gabarit.indexOf("Maintenance de la plateforme.")).isLessThan(gabarit.indexOf("Déployé le pipeline."));
		try (PDDocument document = Loader.loadPDF(pdf.generer(cv))) {
			String texte = new PDFTextStripper().getText(document);
			assertThat(texte.lines().map(String::strip).filter(rubriques::contains)).containsExactlyElementsOf(rubriques);
			assertThat(texte).contains("Élodie Dupont", "Permis B, BE", "Disponible à partir du 1er décembre 2026",
					"Savoir-être", "Rigueur", "Mention Bien", "Randonnée", "github.com/elodie", "Titre sans lien")
					.doesNotContain("javascript:", "example.test/projet", "example.test/badge");
			List<String> liens = new ArrayList<>();
			for (PDPage page : document.getPages()) {
				for (var annotation : page.getAnnotations()) {
					if (annotation instanceof PDAnnotationLink lien && lien.getAction() instanceof PDActionURI cible) {
						liens.add(cible.getURI());
					}
				}
			}
			assertThat(liens).containsExactlyInAnyOrder("https://github.com/elodie", "https://example.test/projet",
					"http://example.test/badge");
		}
	}

	@ParameterizedTest
	@EnumSource(LangueCv.class)
	void afficheUneExperienceSansFinCommeEnCours(LangueCv langue) throws IOException {
		try (PDDocument document = Loader.loadPDF(pdf.generer(cvComplet(langue)))) {
			String texte = new PDFTextStripper().getText(document);
			assertThat(texte).contains(langue == LangueCv.FR ? "avr. 2026 – aujourd'hui" : "Apr 2026 – Present");
			assertThat(texte).contains(langue == LangueCv.FR
					? "Disponible à partir du 1er décembre 2026" : "Available from December 1, 2026");
		}
	}

	@ParameterizedTest
	@EnumSource(ModeleCv.class)
	void gardeLesElementsEtLesTitresDeSectionEnsemble(ModeleCv modele) throws IOException {
		List<ContenuCv.Experience> experiences = IntStream.range(0, 18).mapToObj(CvPdfTests::experience).toList();
		List<ContenuCv.Formation> formations = IntStream.range(0, 12)
				.mapToObj(i -> new ContenuCv.Formation("Diplome-" + i + "-DEBUT",
						"Université de test, établissement de formation ".repeat(2) + "Diplome-" + i + "-FIN",
						"", "2024", "2026", false, ""))
				.toList();
		Cv cv = cv(modele, modele.policeParDefaut(), LangueCv.EN, "Profil de test", experiences, formations);
		List<String> pages = pagesPdf(cv);
		assertThat(pages).hasSizeGreaterThan(1);
		for (int i = 0; i < experiences.size(); i++) {
			String tag = "Poste-" + i + "-";
			assertThat(pageAvec(pages, tag + "DEBUT")).contains(tag + "FIN");
		}
		for (int i = 0; i < formations.size(); i++) {
			String tag = "Diplome-" + i + "-";
			assertThat(pageAvec(pages, tag + "DEBUT")).contains(tag + "FIN");
		}
		assertThat(titres(pageAvec(pages, "Profil de test"))).contains("PROFILE");
		assertThat(titres(pageAvec(pages, "Poste-0-DEBUT"))).contains("EXPERIENCE");
		assertThat(titres(pageAvec(pages, "Diplome-0-DEBUT"))).contains("EDUCATION");
	}

	@Test
	void deplaceLeTitreDeFormationQuandIlNeRestePasAssezDePlacePourLeDiplome() throws IOException {
		List<ContenuCv.Experience> experiences = IntStream.rangeClosed(1, 7)
				.mapToObj(i -> new ContenuCv.Experience("Mission " + i, "Nordlys", "Paris", "2025-04", "", true, "",
						IntStream.range(0, i == 7 ? 1 : 4).mapToObj(n -> "Réalisation " + n
								+ ". Développement et simplification d’un traitement de données dans l’entrepôt partagé.")
								.toList())).toList();
		Cv cv = cv(ModeleCv.CLASSIQUE, PoliceCv.SOURCE_SERIF_4, LangueCv.EN,
				"Je conçois des chaînes de traitement de données fiables et je documente leur fonctionnement.",
				experiences, List.of(new ContenuCv.Formation("Master MIAGE", "Université Paris Nanterre", "", "2025", "", true, "")));

		List<String> pages = pagesPdf(cv);
		assertThat(pages).hasSize(2);
		assertThat(titres(pageAvec(pages, "Master MIAGE"))).contains("EDUCATION");
	}

	@Test
	void gardeLeTitreCompactAvecLaPremiereExperienceLongue() throws IOException {
		List<String> realisations = IntStream.range(0, 23).mapToObj(n -> "Réalisation " + n
				+ ". Développement et simplification d’un traitement de données dans l’entrepôt partagé.").toList();
		ContenuCv.Experience experience = new ContenuCv.Experience("Mission longue", "Nordlys", "Paris",
				"2025-04", "", true, "", realisations);
		Cv cv = cv(ModeleCv.COMPACT, PoliceCv.IBM_PLEX_SANS, LangueCv.EN,
				"Je prépare des traitements de données complexes et je propose des solutions adaptées au besoin de chaque équipe.",
				List.of(experience), List.of());
		List<String> pages = pagesPdf(cv);
		assertThat(pages).hasSize(2);
		String page = pageAvec(pages, "Mission longue");
		assertThat(page).contains("Réalisation 22.");
		assertThat(titres(page)).contains("EXPERIENCE");
	}

	@Test
	void conserveToutLeTexteDuneExperiencePlusHauteQuunePage() throws IOException {
		List<String> realisations = IntStream.range(0, 25)
				.mapToObj(i -> "Repere-" + i + "-DEBUT " + "Réalisation documentée avec plusieurs résultats. ".repeat(8)
						+ "Repere-" + i + "-FIN")
				.toList();
		ContenuCv.Experience experience = new ContenuCv.Experience("Projet long", "Équipe", "Paris", "", "", false, "", realisations);
		List<String> pages = pagesPdf(cv(ModeleCv.MODERNE, PoliceCv.INTER, LangueCv.EN, "Profil de test",
				List.of(experience), List.of()));
		assertThat(pages).hasSizeGreaterThan(1);
		assertThat(pageAvec(pages, "Projet long")).contains("Experience", "Repere-0-DEBUT");
		String document = String.join("\n", pages);
		for (int i = 0; i < realisations.size(); i++) {
			assertThat(document).containsOnlyOnce("Repere-" + i + "-DEBUT").containsOnlyOnce("Repere-" + i + "-FIN");
		}
	}

	private List<String> pagesPdf(Cv cv) throws IOException {
		try (PDDocument document = Loader.loadPDF(pdf.generer(cv))) {
			List<String> pages = new ArrayList<>();
			PDFTextStripper lecteur = new PDFTextStripper();
			for (int page = 1; page <= document.getNumberOfPages(); page++) {
				lecteur.setStartPage(page);
				lecteur.setEndPage(page);
				pages.add(lecteur.getText(document));
			}
			return pages;
		}
	}

	private static String pageAvec(List<String> pages, String texte) {
		return pages.stream().filter(page -> page.contains(texte)).findFirst().orElseThrow(
				() -> new AssertionError("Texte absent du PDF : " + texte));
	}

	private static String titres(String page) {
		return page.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
	}

	private static ContenuCv.Experience experience(int index) {
		String poste = "Poste-" + index + "-";
		return new ContenuCv.Experience(poste + "DEBUT", "Nordlys", "Paris", "2026-04", "", true, "",
				List.of("Développé un pipeline de données qui transfère les résultats vers l'entrepôt partagé.",
						"Réalisé des tests de contrôle en production avec le reste de l'équipe.", poste + "FIN"));
	}

	private static Cv cv(ModeleCv modele, PoliceCv police, LangueCv langue, String profil,
			List<ContenuCv.Experience> experiences, List<ContenuCv.Formation> formations) {
		ContenuCv.Identite identite = new ContenuCv.Identite("Élodie", "Dupont", "Data engineer", "Paris",
				"elodie@example.test", "", List.of(), List.of(new ContenuCv.Lien("Site", "https://example.test/elodie")));
		ContenuCv contenu = new ContenuCv(identite, profil, List.of(), List.of(), List.of(), experiences, formations,
				List.of(), List.of(), List.of(), new ContenuCv.Disponibilite(false, ""));
		return new Cv(1L, new DemandeCv("CV de test", langue, modele, police, contenu), Instant.EPOCH);
	}

	private static Cv cvComplet(LangueCv langue) {
		ContenuCv.Identite identite = new ContenuCv.Identite("Élodie", "Dupont", "Data engineer", "Paris",
				"elodie@example.test", "06 00 00 00 00", List.of("B", "BE"),
				List.of(new ContenuCv.Lien("GitHub", "github.com/elodie"), new ContenuCv.Lien("Refusé", " javascript:alert(1)")));
		ContenuCv contenu = new ContenuCv(identite, "Profil de test",
				List.of(new ContenuCv.GroupeCompetences("Langages", List.of("SQL", "Python"))),
				List.of("Rigueur"), List.of("Randonnée"),
				List.of(new ContenuCv.Experience("Stage", "Nordlys", "Paris", "2026-04", "", false,
						"Maintenance de la plateforme.", List.of("Déployé le pipeline."))),
				List.of(new ContenuCv.Formation("Master MIAGE", "Paris Nanterre", "Bien", "2024", "2026", false, "Parcours données.")),
				List.of(new ContenuCv.Projet("Entrepôt de données", "example.test/projet", "Prototype d'entrepôt."),
						new ContenuCv.Projet("Titre sans lien", "jAvAsCrIpT:alert(2)", "")),
				List.of(new ContenuCv.Langue("Anglais", "C1")),
				List.of(new ContenuCv.Certification("Certificat SQL", "Organisme", "2026-05", "http://example.test/badge"),
						new ContenuCv.Certification("Titre sans lien", "", "", "javascript:alert(3)")),
				new ContenuCv.Disponibilite(false, "2026-12-01"));
		return new Cv(1L, new DemandeCv("CV de test", langue, ModeleCv.MODERNE, PoliceCv.INTER, contenu), Instant.EPOCH);
	}

	private static Stream<Arguments> modelesEtPolices() {
		return Arrays.stream(ModeleCv.values()).flatMap(modele ->
				Arrays.stream(PoliceCv.values()).map(police -> Arguments.of(modele, police)));
	}

	private static SpringTemplateEngine moteur() {
		ClassLoaderTemplateResolver gabarits = new ClassLoaderTemplateResolver();
		gabarits.setPrefix("templates/");
		gabarits.setSuffix(".html");
		gabarits.setTemplateMode(TemplateMode.HTML);
		gabarits.setCharacterEncoding(StandardCharsets.UTF_8.name());
		SpringTemplateEngine moteur = new SpringTemplateEngine();
		moteur.setTemplateResolver(gabarits);
		return moteur;
	}
}
