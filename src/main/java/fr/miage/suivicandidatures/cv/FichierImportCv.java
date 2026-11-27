package fr.miage.suivicandidatures.cv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import fr.miage.suivicandidatures.commun.ia.AssistantIa.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

record FichierImportCv(String texte, List<Image> images) {

	static final String MESSAGE_TAILLE_MAX = "Le fichier du CV ne doit pas dépasser 10 Mo.";

	private static final long TAILLE_MAX = 10 * 1024 * 1024;
	private static final int TAILLE_ZIP_MAX = 20 * 1024 * 1024;
	private static final int LONGUEUR_TEXTE_MAX = 20_000;
	private static final byte[] PDF = "%PDF".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] ZIP = HexFormat.of().parseHex("504b0304");
	private static final byte[] PNG = HexFormat.of().parseHex("89504e470d0a1a0a");
	private static final byte[] JPEG = HexFormat.of().parseHex("ffd8ff");
	private static final String FORMAT_INVALIDE = "Le format ou l'extension du fichier n'est pas pris en charge. "
			+ "Enregistrez votre CV en PDF ou en DOCX.";

	static FichierImportCv lire(MultipartFile fichier) {
		if (fichier.isEmpty()) {
			throw invalide("Le fichier du CV est vide.");
		}
		if (fichier.getSize() > TAILLE_MAX) {
			throw invalide(MESSAGE_TAILLE_MAX);
		}

		String nom = fichier.getOriginalFilename();
		String extension = nom == null || !nom.contains(".") ? ""
				: nom.substring(nom.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		try {
			byte[] octets = fichier.getBytes();
			if (commencePar(octets, PDF)) {
				verifierExtension(extension, "pdf");
				return lirePdf(octets);
			}
			if (commencePar(octets, ZIP)) {
				verifierExtension(extension, "docx");
				return lireDocx(octets);
			}
			if (commencePar(octets, PNG)) {
				verifierExtension(extension, "png");
				return new FichierImportCv("", List.of(new Image("image/png", octets)));
			}
			if (commencePar(octets, JPEG)) {
				verifierExtension(extension, "jpg", "jpeg");
				return new FichierImportCv("", List.of(new Image("image/jpeg", octets)));
			}
			if (extension.equals("txt")) {
				return lireTxt(octets);
			}
			throw invalide(FORMAT_INVALIDE);
		} catch (InvalidPasswordException erreur) {
			throw invalide("Le PDF est protégé par un mot de passe. Importez une copie sans mot de passe.");
		} catch (IOException | XMLStreamException erreur) {
			throw invalide("Le fichier du CV est illisible. Enregistrez votre CV en PDF ou en DOCX.");
		}
	}

	private static boolean commencePar(byte[] octets, byte[] signature) {
		return octets.length >= signature.length
				&& Arrays.equals(octets, 0, signature.length, signature, 0, signature.length);
	}

	private static void verifierExtension(String extension, String... extensions) {
		if (!List.of(extensions).contains(extension)) {
			throw invalide(FORMAT_INVALIDE);
		}
	}

	private static FichierImportCv lirePdf(byte[] octets) throws IOException {
		try (var document = Loader.loadPDF(octets)) {
			if (document.isEncrypted() && !document.getCurrentAccessPermission().canExtractContent()) {
				throw invalide("Le PDF est protégé. Importez une copie sans protection.");
			}
			TextePdf texte = new TextePdf();
			PDFTextStripper extracteur = new PDFTextStripper();
			extracteur.setSortByPosition(true);
			try {
				extracteur.writeText(document, texte);
			} catch (LimiteTextePdf atteinte) {
				// Le préfixe conservé suffit à la demande.
			}
			String resultat = texte.contenu.toString();
			if (resultat.codePoints().filter(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))
					.limit(50).count() == 50) {
				return depuisTexte(resultat);
			}
			if (document.getNumberOfPages() == 0) {
				throw invalide("Le PDF ne contient aucune page.");
			}

			PDFRenderer rendu = new PDFRenderer(document);
			rendu.setSubsamplingAllowed(true);
			List<Image> images = new ArrayList<>();
			for (int page = 0; page < Math.min(3, document.getNumberOfPages()); page++) {
				var image = rendu.renderImageWithDPI(page, 150);
				ByteArrayOutputStream png = new ByteArrayOutputStream();
				try (var sortie = new MemoryCacheImageOutputStream(png)) {
					ImageIO.write(image, "png", sortie);
				} finally {
					image.flush();
				}
				images.add(new Image("image/png", png.toByteArray()));
			}
			return new FichierImportCv("", List.copyOf(images));
		}
	}

	private static FichierImportCv lireTxt(byte[] octets) throws CharacterCodingException {
		String texte;
		try {
			texte = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(octets)).toString();
		} catch (CharacterCodingException erreur) {
			texte = Charset.forName("windows-1252").newDecoder().decode(ByteBuffer.wrap(octets)).toString();
		}
		if (texte.startsWith("\uFEFF")) {
			texte = texte.substring(1);
		}
		if (texte.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\t' && c != '\r' && c != '\n')) {
			throw invalide(FORMAT_INVALIDE);
		}
		return depuisTexte(texte);
	}

	private static FichierImportCv lireDocx(byte[] octets) throws IOException, XMLStreamException {
		StringBuilder entete = new StringBuilder();
		StringBuilder corps = new StringBuilder();
		StringBuilder pied = new StringBuilder();
		boolean documentTrouve = false;
		int total = 0;
		byte[] tampon = new byte[8192];
		try (var zip = new ZipInputStream(new ByteArrayInputStream(octets))) {
			for (ZipEntry entree; (entree = zip.getNextEntry()) != null;) {
				String nom = entree.getName();
				StringBuilder destination = null;
				if (nom.equals("word/document.xml")) {
					documentTrouve = true;
					destination = corps;
				} else if (nom.matches("word/header[^/]*\\.xml")) {
					destination = entete;
				} else if (nom.matches("word/footer[^/]*\\.xml")) {
					destination = pied;
				}

				ByteArrayOutputStream xml = destination == null ? null : new ByteArrayOutputStream();
				for (int lus; (lus = zip.read(tampon, 0, Math.min(tampon.length, TAILLE_ZIP_MAX - total + 1))) != -1;) {
					total += lus;
					if (total > TAILLE_ZIP_MAX) {
						throw invalide("Le document DOCX dépasse 20 Mo après décompression.");
					}
					if (xml != null) {
						xml.write(tampon, 0, lus);
					}
				}
				if (xml != null) {
					lireXmlWord(xml.toByteArray(), destination);
				}
			}
		}
		if (!documentTrouve) {
			throw invalide("Ce fichier n'est pas un document DOCX valide. Enregistrez votre CV en PDF ou en DOCX.");
		}
		return depuisTexte(entete.toString() + corps + pied);
	}

	private static void lireXmlWord(byte[] octets, StringBuilder texte) throws XMLStreamException {
		XMLInputFactory fabrique = XMLInputFactory.newDefaultFactory();
		fabrique.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		fabrique.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		fabrique.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		var lecteur = fabrique.createXMLStreamReader(new ByteArrayInputStream(octets));
		try {
			boolean dansTexte = false;
			while (lecteur.hasNext()) {
				int evenement = lecteur.next();
				switch (evenement) {
					case XMLStreamConstants.DTD, XMLStreamConstants.ENTITY_REFERENCE ->
						throw invalide("Le document DOCX contient des déclarations XML non autorisées.");
					case XMLStreamConstants.START_ELEMENT -> {
						if (baliseWord(lecteur.getNamespaceURI())) {
							switch (lecteur.getLocalName()) {
								case "t" -> dansTexte = true;
								case "tab" -> ajouter(texte, "\t");
								case "br", "cr" -> ajouter(texte, "\n");
							}
						}
					}
					case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA, XMLStreamConstants.SPACE -> {
						if (dansTexte && texte.length() < LONGUEUR_TEXTE_MAX) {
							ajouter(texte, lecteur.getText());
						}
					}
					case XMLStreamConstants.END_ELEMENT -> {
						if (baliseWord(lecteur.getNamespaceURI())) {
							switch (lecteur.getLocalName()) {
								case "t" -> dansTexte = false;
								case "p", "tr" -> ajouter(texte, "\n");
								case "tc" -> ajouter(texte, "\t");
							}
						}
					}
				}
			}
		} finally {
			lecteur.close();
		}
	}

	private static boolean baliseWord(String espace) {
		return "http://schemas.openxmlformats.org/wordprocessingml/2006/main".equals(espace)
				|| "http://purl.oclc.org/ooxml/wordprocessingml/main".equals(espace);
	}

	private static void ajouter(StringBuilder texte, String supplement) {
		texte.append(supplement, 0, Math.min(supplement.length(), LONGUEUR_TEXTE_MAX - texte.length()));
	}

	private static FichierImportCv depuisTexte(String valeur) {
		String texte = valeur.substring(0, Math.min(valeur.length(), LONGUEUR_TEXTE_MAX));
		if (!texte.isEmpty() && Character.isHighSurrogate(texte.charAt(texte.length() - 1))) {
			texte = texte.substring(0, texte.length() - 1);
		}
		if (ContenuCv.sansEspacesAuxBords(texte).isEmpty()) {
			throw invalide("Le fichier du CV ne contient pas de texte.");
		}
		return new FichierImportCv(texte, List.of());
	}

	private static ResponseStatusException invalide(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

	private static class TextePdf extends Writer {

		private final StringBuilder contenu = new StringBuilder();

		@Override
		public void write(char[] caracteres, int debut, int longueur) throws IOException {
			int aConserver = Math.min(longueur, LONGUEUR_TEXTE_MAX - contenu.length());
			contenu.append(caracteres, debut, aConserver);
			if (aConserver < longueur) {
				throw new LimiteTextePdf();
			}
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}
	}

	private static class LimiteTextePdf extends IOException {
	}
}
