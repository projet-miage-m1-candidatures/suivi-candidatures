package fr.miage.suivicandidatures.cv;

import java.text.BreakIterator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class CvService {

	private static final String SUFFIXE_COPIE = " (copie)";

	private final CvRepository cvs;

	CvService(CvRepository cvs) {
		this.cvs = cvs;
	}

	@Transactional(readOnly = true)
	List<Cv> lister(Long candidatId) {
		return cvs.findAllByUtilisateurIdOrderByDateModificationDescIdDesc(candidatId);
	}

	@Transactional(readOnly = true)
	Cv lire(Long candidatId, Long id) {
		return cvs.findByIdAndUtilisateurId(id, candidatId).orElseThrow(CvService::cvIntrouvable);
	}

	@Transactional
	Cv creer(Long candidatId, DemandeCv demande) {
		verifierLongueurTitre(demande.titre());
		return cvs.save(new Cv(candidatId, demande, maintenant()));
	}

	@Transactional
	Cv enregistrer(Long candidatId, Long id, DemandeCv demande) {
		Cv cv = trouverPourEcriture(candidatId, id);
		verifierLongueurTitre(demande.titre());
		cv.modifier(demande, maintenant());
		return cv;
	}

	@Transactional
	void supprimer(Long candidatId, Long id) {
		cvs.delete(trouverPourEcriture(candidatId, id));
	}

	@Transactional
	Cv dupliquer(Long candidatId, Long id) {
		Cv source = lire(candidatId, id);
		DemandeCv copie = new DemandeCv(titreDeCopie(source.getTitre()), source.getLangue(),
				source.getModele(), source.getPolice(), source.getCouleur(), source.getTaille(),
				source.getInterligne(), source.getContenu());
		return creer(candidatId, copie);
	}

	private Cv trouverPourEcriture(Long candidatId, Long id) {
		return cvs.findForUpdateByIdAndUtilisateurId(id, candidatId).orElseThrow(CvService::cvIntrouvable);
	}

	private static ResponseStatusException cvIntrouvable() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce CV n'existe pas.");
	}

	private static void verifierLongueurTitre(String titre) {
		// PostgreSQL compte les points de code, pas les unités UTF-16 d'une String Java.
		if (titre.codePointCount(0, titre.length()) > Cv.LONGUEUR_MAX_TITRE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Le titre du CV doit contenir au plus " + Cv.LONGUEUR_MAX_TITRE + " caractères.");
		}
	}

	private static String titreDeCopie(String titre) {
		String base = titre.stripTrailing();
		int longueurDisponible = Cv.LONGUEUR_MAX_TITRE - SUFFIXE_COPIE.length();
		if (base.codePointCount(0, base.length()) > longueurDisponible) {
			int limite = base.offsetByCodePoints(0, longueurDisponible);
			BreakIterator caracteres = BreakIterator.getCharacterInstance(Locale.ROOT);
			caracteres.setText(base);
			int fin = caracteres.isBoundary(limite) ? limite : caracteres.preceding(limite);
			base = base.substring(0, fin).stripTrailing();
		}
		return base + SUFFIXE_COPIE;
	}

	private static Instant maintenant() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}
}
