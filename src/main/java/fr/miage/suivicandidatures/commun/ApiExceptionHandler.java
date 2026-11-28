package fr.miage.suivicandidatures.commun;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Transforme les erreurs en réponses JSON (RFC 9457).
 * Les erreurs de validation (400) sont gérées par ResponseEntityExceptionHandler.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail gererApiException(ApiException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatut(), e.getMessage());
    }
}
