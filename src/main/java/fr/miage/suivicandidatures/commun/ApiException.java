package fr.miage.suivicandidatures.commun;

import org.springframework.http.HttpStatus;

/** Erreur métier renvoyée telle quelle au client (statut HTTP + message). */
public class ApiException extends RuntimeException {

    private final HttpStatus statut;

    public ApiException(HttpStatus statut, String message) {
        super(message);
        this.statut = statut;
    }

    public HttpStatus getStatut() {
        return statut;
    }
}
