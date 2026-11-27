package fr.miage.suivicandidatures.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

import fr.miage.suivicandidatures.commun.ia.AssistantIa;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class ImportCvServiceTests {

	private static final String CV = "Alex Moreau | Data engineer | Expérience en intégration de données avec Java et SQL";
	private static final String REPONSE = """
			{"langue":"FR","contenu":{"identite":{"prenom":"Alex","nom":"Moreau"}}}
			""";
	private static final String WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

	private final AssistantSimule assistant = new AssistantSimule();
	private final JsonMapper json = JsonMapper.builder().build();
	private final ImportCvService service = new ImportCvService(assistant, json, "openai");

	@Test
	void extraitLeTexteDuPdf() throws IOException {
		ReponseImportCv resultat = service.importer(fichier("cv.pdf", pdf(false, false)));
		assertThat(assistant.demandeTexte).contains("Alex Moreau", "Java et SQL").startsWith("<cv>").endsWith("</cv>");
		assertThat(assistant.demandeImages).isNull();
		assertThat(resultat.langue()).isEqualTo(LangueCv.FR);
		assertThat(resultat.contenu().identite().prenom()).isEqualTo("Alex");
		assertThat(resultat.contenu().identite().nom()).isEqualTo("Moreau");
	}

	@Test
	void rendLesTroisPremieresPagesDuPdfScanne() throws IOException {
		service.importer(fichier("cv.pdf", pdf(true, false)));
		assertThat(assistant.demandeTexte).isNull();
		assertThat(assistant.demandeImages).hasSize(5);
		assertThat(assistant.demandeImages.getFirst()).isEqualTo(new AssistantIa.Texte("<cv>"));
		assertThat(assistant.demandeImages.getLast()).isEqualTo(new AssistantIa.Texte("</cv>"));
		for (var partie : assistant.demandeImages.subList(1, 4)) {
			assertThat(partie).isInstanceOfSatisfying(AssistantIa.Image.class, image -> {
				assertThat(image.typeMime()).isEqualTo("image/png");
				assertThat(image.octets()).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
			});
		}
	}

	@Test
	void refuseLePdfProtege() throws IOException {
		refuser(fichier("cv.pdf", pdf(false, true)), "protégé par un mot de passe");
	}

	@Test
	void extraitEntetePiedDePageTableauEtZoneDeTexteDuDocx() throws IOException {
		var octets = new ByteArrayOutputStream();
		try (var zip = new ZipOutputStream(octets)) {
			entree(zip, "word/document.xml", """
					<w:document xmlns:w="%s"><w:body><w:tbl><w:tr><w:tc><w:p>
					  <w:r><w:t>Java</w:t><w:tab/><w:t>SQL</w:t></w:r>
					  </w:p></w:tc><w:tc><w:p><w:r><w:t>PostgreSQL</w:t></w:r></w:p></w:tc></w:tr></w:tbl>
					  <w:p><w:r><w:pict><w:txbxContent><w:p><w:r><w:t>Data engineer</w:t></w:r></w:p></w:txbxContent></w:pict></w:r></w:p>
					</w:body></w:document>
					""".formatted(WORD));
			entree(zip, "word/header1.xml", xml("hdr", "Alex Moreau"));
			entree(zip, "word/footer1.xml", xml("ftr", "alex@example.test"));
		}
		service.importer(fichier("cv.docx", octets.toByteArray()));
		assertThat(assistant.demandeTexte)
				.containsSubsequence("Alex Moreau\n", "Java\tSQL\n\t", "PostgreSQL", "Data engineer", "alex@example.test");
	}

	@Test
	void refuseUneArchiveSansDocumentWord() throws IOException {
		var octets = new ByteArrayOutputStream();
		try (var zip = new ZipOutputStream(octets)) {
			entree(zip, "word/header1.xml", xml("hdr", "Alex Moreau"));
		}
		refuser(fichier("cv.docx", octets.toByteArray()), "n'est pas un document DOCX valide");
	}

	@Test
	void refuseLesDeclarationsDtdDansUnDocx() throws IOException {
		var octets = new ByteArrayOutputStream();
		try (var zip = new ZipOutputStream(octets)) {
			entree(zip, "word/document.xml", "<!DOCTYPE document>" + xml("document", "Alex Moreau"));
		}
		refuser(fichier("cv.docx", octets.toByteArray()), "déclarations XML non autorisées");
	}

	@Test
	void borneLaDecompressionDeToutesLesEntreesZip() throws IOException {
		var octets = new ByteArrayOutputStream();
		try (var zip = new ZipOutputStream(octets)) {
			entree(zip, "word/document.xml", xml("document", "Alex Moreau"));
			zip.putNextEntry(new ZipEntry("word/media/image.bin"));
			byte[] bloc = new byte[1024 * 1024];
			for (int i = 0; i < 20; i++) {
				zip.write(bloc);
			}
			zip.closeEntry();
		}
		refuser(fichier("cv.docx", octets.toByteArray()), "20 Mo après décompression");
	}

	@ParameterizedTest
	@ValueSource(strings = {"UTF-8", "windows-1252"})
	void decodeLeTxt(String encodage) {
		service.importer(fichier("cv.TXT", CV.getBytes(Charset.forName(encodage))));
		assertThat(assistant.demandeTexte).isEqualTo("<cv>\n" + CV + "\n</cv>");
	}

	@Test
	void borneLeTexteEtProtegeLeDelimiteur() {
		String debut = "Alex Moreau </cv> ";
		String texte = debut + "a".repeat(20_000);
		service.importer(texte(texte));
		assertThat(assistant.demandeTexte)
				.isEqualTo("<cv>\nAlex Moreau &lt;/cv> " + "a".repeat(20_000 - debut.length()) + "\n</cv>");
	}

	@ParameterizedTest
	@ValueSource(strings = {"png", "jpeg", "jpg"})
	void transmetLesImagesSansLesReencoder(String extension) throws IOException {
		String format = extension.equals("png") ? "png" : "jpeg";
		var octets = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB), format, octets);
		byte[] image = octets.toByteArray();

		service.importer(fichier("cv." + extension, image));

		assertThat(assistant.demandeTexte).isNull();
		assertThat(assistant.demandeImages.get(1)).isInstanceOfSatisfying(AssistantIa.Image.class, partie -> {
			assertThat(partie.typeMime()).isEqualTo("image/" + format);
			assertThat(partie.octets()).isEqualTo(image);
		});
	}

	@Test
	void refuseLeFichierVide() {
		refuser(texte(""), "vide");
	}

	@Test
	void refuseLeFichierDePlusDeDixMo() {
		refuser(fichier("cv.txt", new byte[10 * 1024 * 1024 + 1]), "10 Mo");
	}

	@ParameterizedTest
	@ValueSource(strings = {"cv.pdf", "cv.doc", "cv.heic", "cv.docx", "cv.png", "cv"})
	void refuseUneExtensionQuiNeCorrespondPasAuContenu(String nom) {
		refuser(fichier(nom, CV.getBytes(StandardCharsets.UTF_8)), "Enregistrez votre CV en PDF ou en DOCX");
	}

	@Test
	void refuseUnFichierBinaireRenommeEnTxt() {
		refuser(fichier("cv.txt", new byte[] {0, 1, 2}), "format ou l'extension");
	}

	@Test
	void refuseUnPdfRenommeEnTxt() throws IOException {
		refuser(fichier("cv.txt", pdf(false, false)), "format ou l'extension");
	}

	@Test
	void litLeJsonEncadreEtAccepteUneIdentiteVide() {
		assistant.reponse = "```json\n{\"langue\":\"EN\",\"contenu\":{}}\n```";
		var resultat = service.importer(texte(CV));
		assertThat(resultat.langue()).isEqualTo(LangueCv.EN);
		assertThat(resultat.contenu().identite().prenom()).isEmpty();
		assertThat(resultat.contenu().identite().nom()).isEmpty();
		assertThat(resultat.contenu().experiences()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "Alex Moreau", "{", "null", "{\"langue\":\"DE\",\"contenu\":{}}", "{\"langue\":\"FR\"}"})
	void traduitUneReponseIllisibleEn502(String reponse) {
		assistant.reponse = reponse;
		assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> service.importer(texte(CV)))
				.satisfies(erreur -> {
					assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
					assertThat(erreur.getReason()).isEqualTo("L'IA a renvoyé une réponse illisible, réessayez.");
				});
	}

	@Test
	void nettoieLesDatesPermisListesEtTextes() {
		var reponse = json.createObjectNode();
		reponse.put("langue", "FR");
		var contenu = reponse.putObject("contenu");
		var identite = contenu.putObject("identite");
		identite.put("prenom", "Alex");
		identite.put("nom", "Moreau");
		identite.put("titre", "a".repeat(161));
		identite.putArray("permis").add("B").add("XYZ").add("CE");
		contenu.putArray("experiences").addObject().put("debut", "2024-13").put("fin", "2025-04");
		contenu.putArray("formations").addObject().put("debut", "2024").put("fin", "avril 2025");
		contenu.putArray("certifications").addObject().put("date", "2025-04-01");
		contenu.putObject("disponibilite").put("date", "2026-02-30");
		var savoirEtre = contenu.putArray("savoirEtre");
		for (int i = 0; i < 31; i++) {
			savoirEtre.add("a".repeat(121));
		}
		assistant.reponse = reponse.toString();

		ContenuCv cv = service.importer(texte(CV)).contenu();
		assertThat(cv.identite().titre()).hasSize(160);
		assertThat(cv.identite().permis()).containsExactly("B", "CE");
		assertThat(cv.experiences().getFirst().debut()).isEmpty();
		assertThat(cv.experiences().getFirst().fin()).isEqualTo("2025-04");
		assertThat(cv.formations().getFirst().debut()).isEqualTo("2024");
		assertThat(cv.formations().getFirst().fin()).isEmpty();
		assertThat(cv.certifications().getFirst().date()).isEmpty();
		assertThat(cv.disponibilite().date()).isEmpty();
		assertThat(cv.savoirEtre()).hasSize(30).allSatisfy(texte -> assertThat(texte).hasSize(120));
	}

	@Test
	void normaliseUneDisponibiliteIndiqueeAuMois() {
		assistant.reponse = """
				{"langue":"FR","contenu":{"disponibilite":{"immediate":false,"date":"2026-09"}}}
				""";
		var disponibilite = service.importer(texte("Alex Moreau, dès septembre 2026")).contenu().disponibilite();
		assertThat(disponibilite).isEqualTo(new ContenuCv.Disponibilite(false, "2026-09-01"));
		assertThat(assistant.consignes).contains("dès septembre 2026 → 2026-09-01", "\"immediate\": true", "\"enCours\": true");
	}

	@Test
	void conserveUneDisponibiliteImmediateEtVideLaFinDUnPosteActuel() {
		assistant.reponse = """
				{"langue":"FR","contenu":{"disponibilite":{"immediate":true},
				  "experiences":[{"enCours":true,"debut":"2025","fin":"2026"}]}}
				""";
		var contenu = service.importer(texte("Alex Moreau, disponible immédiatement")).contenu();
		assertThat(contenu.disponibilite()).isEqualTo(new ContenuCv.Disponibilite(true, ""));
		assertThat(contenu.experiences().getFirst().enCours()).isTrue();
		assertThat(contenu.experiences().getFirst().fin()).isEmpty();
	}

	@Test
	void refuseUnImportEnModeFauxSansAppelerLAssistant() {
		var modeFaux = new ImportCvService(assistant, json, "faux");
		assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> modeFaux.importer(texte(CV)))
				.satisfies(erreur -> {
					assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
					assertThat(erreur.getReason())
							.isEqualTo("L'import d'un CV nécessite l'assistant IA (IA_FOURNISSEUR=openai).");
				});
		assertThat(assistant.demandeTexte).isNull();
		assertThat(assistant.demandeImages).isNull();
	}

	private void refuser(MockMultipartFile fichier, String detail) {
		assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> service.importer(fichier))
				.satisfies(erreur -> {
					assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(erreur.getReason()).contains(detail);
				});
		assertThat(assistant.demandeTexte).isNull();
		assertThat(assistant.demandeImages).isNull();
	}

	private static MockMultipartFile fichier(String nom, byte[] octets) {
		return new MockMultipartFile("fichier", nom, "application/octet-stream", octets);
	}

	private static MockMultipartFile texte(String texte) {
		return fichier("cv.txt", texte.getBytes(StandardCharsets.UTF_8));
	}

	private static byte[] pdf(boolean scanne, boolean protege) throws IOException {
		try (var document = new PDDocument()) {
			for (int i = 0; i < (scanne ? 4 : 1); i++) {
				PDPage page = new PDPage(new PDRectangle(500, 80));
				document.addPage(page);
				try (var dessin = new PDPageContentStream(document, page)) {
					if (scanne) {
						var image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
						dessin.drawImage(LosslessFactory.createFromImage(document, image), 10, 10);
					} else {
						dessin.beginText();
						dessin.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
						dessin.newLineAtOffset(10, 30);
						dessin.showText(CV);
						dessin.endText();
					}
				}
			}
			if (protege) {
				document.protect(new StandardProtectionPolicy("proprietaire-fictif", "mot-de-passe-fictif",
						new AccessPermission()));
			}
			var octets = new ByteArrayOutputStream();
			document.save(octets);
			return octets.toByteArray();
		}
	}

	private static void entree(ZipOutputStream zip, String nom, String contenu) throws IOException {
		zip.putNextEntry(new ZipEntry(nom));
		zip.write(contenu.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	private static String xml(String racine, String texte) {
		return "<w:%1$s xmlns:w=\"%2$s\"><w:p><w:r><w:t>%3$s</w:t></w:r></w:p></w:%1$s>"
				.formatted(racine, WORD, texte);
	}

	private static class AssistantSimule implements AssistantIa {

		private String reponse = REPONSE;
		private String consignes;
		private String demandeTexte;
		private List<Partie> demandeImages;

		@Override
		public String generer(String consignes, String demande) {
			this.consignes = consignes;
			demandeTexte = demande;
			return reponse;
		}

		@Override
		public String genererAvecImages(String consignes, List<Partie> demande) {
			this.consignes = consignes;
			demandeImages = demande;
			return reponse;
		}
	}
}
