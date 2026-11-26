package fr.miage.suivicandidatures.cv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.Normalizer;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
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
		PdfRendererBuilder moteur = new PdfRendererBuilder()
				.withHtmlContent(html.rendre(cv), null)
				.useExternalResourceAccessControl((url, type) -> false,
						ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI)
				.useUriResolver((base, url) -> null)
				.toStream(sortie);
		PoliceCv police = cv.getPolice();
		ajouterPolice(moteur, police, "Regular", 400, FontStyle.NORMAL);
		ajouterPolice(moteur, police, "Bold", 700, FontStyle.NORMAL);
		if (police == PoliceCv.SOURCE_SERIF_4) {
			ajouterPolice(moteur, police, "It", 400, FontStyle.ITALIC);
		}
		try {
			moteur.run();
			return sortie.toByteArray();
		} catch (IOException | UncheckedIOException erreur) {
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

	private static void ajouterPolice(PdfRendererBuilder moteur, PoliceCv police, String variante, int poids,
			FontStyle style) {
		ClassPathResource fichier = new ClassPathResource(police.fichier(variante));
		moteur.useFont(() -> ouvrir(fichier), police.famille(), poids, style, true);
	}

	private static InputStream ouvrir(ClassPathResource fichier) {
		try {
			return fichier.getInputStream();
		} catch (IOException erreur) {
			throw new UncheckedIOException(erreur);
		}
	}
}
