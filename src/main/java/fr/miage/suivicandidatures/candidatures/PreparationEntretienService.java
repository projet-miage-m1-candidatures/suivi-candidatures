package fr.miage.suivicandidatures.candidatures;

import fr.miage.suivicandidatures.commun.ApiException;
import fr.miage.suivicandidatures.commun.ia.AssistantIa;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Pas de @Transactional ici : l'appel à l'IA peut durer plusieurs secondes
 * et ne doit pas garder une connexion à la base ouverte.
 */
@Service
public class PreparationEntretienService {

    public static final String AVERTISSEMENT =
            "Fiche générée par une IA sans accès à Internet : vérifiez les informations sur l'entreprise "
                    + "(site officiel, LinkedIn, presse) avant l'entretien.";

    private final CandidatureService candidatureService;
    private final PreparationEntretienRepository repository;
    private final AssistantIa assistantIa;
    private final String nomModele;

    public PreparationEntretienService(CandidatureService candidatureService,
                                       PreparationEntretienRepository repository,
                                       AssistantIa assistantIa,
                                       @Value("${ia.fournisseur:faux}") String fournisseur,
                                       @Value("${ia.modele:}") String modele) {
        this.candidatureService = candidatureService;
        this.repository = repository;
        this.assistantIa = assistantIa;
        this.nomModele = "faux".equals(fournisseur) ? "démonstration" : modele;
    }

    /** Retourne la fiche enregistrée, ou la génère (ou la régénère si demandé). */
    public PreparationEntretien obtenir(Long candidatureId, boolean regenerer) {
        Candidature candidature = candidatureService.trouver(candidatureId);
        if (candidature.getStatut() != StatutCandidature.ENTRETIEN) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La préparation n'est disponible que pour une candidature au statut « "
                            + StatutCandidature.ENTRETIEN.getLibelle() + " ».");
        }
        Optional<PreparationEntretien> existante = repository.findByCandidatureId(candidatureId);
        if (existante.isPresent() && !regenerer) {
            return existante.get();
        }
        String contenu = assistantIa.generer(
                PromptEntretien.SYSTEME,
                PromptEntretien.utilisateur(
                        candidature.getIntitule(),
                        candidature.getEntreprise(),
                        candidature.getLieu(),
                        candidature.getDescriptionOffre()));
        PreparationEntretien preparation = existante.orElseGet(() -> new PreparationEntretien(candidatureId));
        preparation.remplacer(contenu.strip(), nomModele);
        return repository.save(preparation);
    }
}
