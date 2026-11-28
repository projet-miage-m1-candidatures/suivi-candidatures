package fr.miage.suivicandidatures.candidatures;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidatures")
public class CandidatureController {

    private final CandidatureService candidatureService;
    private final PreparationEntretienService preparationService;

    public CandidatureController(CandidatureService candidatureService,
                                 PreparationEntretienService preparationService) {
        this.candidatureService = candidatureService;
        this.preparationService = preparationService;
    }

    /** « J'ai postulé » : crée la candidature (statut EN_ATTENTE). */
    @PostMapping
    public ResponseEntity<CandidatureResponse> creer(@Valid @RequestBody CreerCandidatureRequest demande) {
        Candidature creee = candidatureService.creer(demande);
        return ResponseEntity.created(URI.create("/api/v1/candidatures/" + creee.getId()))
                .body(CandidatureResponse.de(creee));
    }

    /** Liste, avec filtre optionnel : ?statut=EN_ATTENTE|ENTRETIEN|REFUSEE */
    @GetMapping
    public List<CandidatureResponse> lister(@RequestParam(required = false) StatutCandidature statut) {
        return candidatureService.lister(statut).stream().map(CandidatureResponse::de).toList();
    }

    /** Nombre de candidatures par statut (pour les onglets de filtre). */
    @GetMapping("/compteurs")
    public Map<StatutCandidature, Long> compteurs() {
        return candidatureService.compter();
    }

    @GetMapping("/{id}")
    public CandidatureResponse detail(@PathVariable Long id) {
        return CandidatureResponse.de(candidatureService.trouver(id));
    }

    @PatchMapping("/{id}/statut")
    public CandidatureResponse changerStatut(@PathVariable Long id, @Valid @RequestBody ChangerStatutRequest demande) {
        return CandidatureResponse.de(
                candidatureService.changerStatut(id, demande.statut(), demande.dateEntretien()));
    }

    /** Fiche de préparation IA (statut ENTRETIEN uniquement). ?regenerer=true pour en refaire une. */
    @GetMapping("/{id}/preparation-entretien")
    public PreparationEntretienResponse preparation(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "false") boolean regenerer) {
        PreparationEntretien preparation = preparationService.obtenir(id, regenerer);
        return PreparationEntretienResponse.de(preparation, candidatureService.trouver(id));
    }
}
