package fr.miage.suivicandidatures.cv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PDFontSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class CvPdf {

	private final CvHtml html;

	CvPdf(CvHtml html) {
		this.html = html;
	}

	byte[] generer(Cv cv) {
		ByteArrayOutputStream sortie = new ByteArrayOutputStream();
		try (PDDocument document = new PDDocument()) {
			PdfRendererBuilder moteur = new PdfRendererBuilder()
					.usePDDocument(document)
					.withHtmlContent(html.rendre(cv), null)
					.useExternalResourceAccessControl((url, type) -> false,
							ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI)
					.useUriResolver((base, url) -> null)
					.toStream(sortie);
			PoliceCv police = cv.getPolice();
			ajouterPolice(moteur, document, police, "Regular", 400, FontStyle.NORMAL);
			ajouterPolice(moteur, document, police, "Bold", 700, FontStyle.NORMAL);
			if (police.italique() != null) {
				ajouterPolice(moteur, document, police, police.italique(), 400, FontStyle.ITALIC);
			}
			moteur.run();
			return sortie.toByteArray();
		} catch (IOException erreur) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Le PDF n'a pas pu être généré.", erreur);
		}
	}

	static String nomFichier(Cv cv) {
		String nom = Normalizer.normalize(cv.getContenu().identite().nomComplet(), Normalizer.Form.NFKD)
				.replaceAll("\\p{M}+", "")
				.replace("œ", "oe").replace("Œ", "OE")
				.replaceAll("[^a-zA-Z0-9]+", "-")
				.replaceAll("^-|-$", "");
		return "CV-" + (nom.isEmpty() ? "Candidat" : nom) + ".pdf";
	}

	private static void ajouterPolice(PdfRendererBuilder moteur, PDDocument document, PoliceCv police,
			String variante, int poids, FontStyle style) throws IOException {
		ClassPathResource fichier = new ClassPathResource(police.fichier(variante));
		try (InputStream flux = fichier.getInputStream()) {
			TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(flux));
			document.registerTrueTypeFontForClosing(ttf);
			// PDFBox 3.0.8 peut perdre les ligatures lors de l'extraction, par exemple « fi » dans « Profil ».
			ttf.setEnableGsub(false);
			PDType0Font fonte = PDType0Font.load(document, ttf, true);
			moteur.useFont(new PDFontSupplier(fonte), police.famille(), poids, style, true);
		}
	}
}
