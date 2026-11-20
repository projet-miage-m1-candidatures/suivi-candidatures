package fr.miage.suivicandidatures.offres;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/offres")
public class OffreController {

    private final OffreService offreService;

    public OffreController(OffreService offreService) {
        this.offreService = offreService;
    }

    /** Détail d'une offre enregistrée. Répond 404 si elle n'existe pas. */
    @GetMapping("/{id}")
    public OffreDetail trouver(@PathVariable Long id) {
        return offreService.trouver(id);
    }
}
