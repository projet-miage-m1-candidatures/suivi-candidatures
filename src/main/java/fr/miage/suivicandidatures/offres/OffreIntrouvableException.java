package fr.miage.suivicandidatures.offres;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Levée quand on demande une offre qui n'existe pas : l'API répond 404. */
public class OffreIntrouvableException extends ResponseStatusException {

    public OffreIntrouvableException(Long id) {
        super(HttpStatus.NOT_FOUND, "Aucune offre avec l'identifiant " + id);
    }
}
