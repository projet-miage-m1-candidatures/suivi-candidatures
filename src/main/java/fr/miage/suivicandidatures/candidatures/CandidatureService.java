package fr.miage.suivicandidatures.candidatures;

import fr.miage.suivicandidatures.commun.ApiException;
import fr.miage.suivicandidatures.commun.CandidatCourant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidatureService {

    private final CandidatureRepository repository;
    private final CandidatCourant candidatCourant;

    public CandidatureService(CandidatureRepository repository, CandidatCourant candidatCourant) {
        this.repository = repository;
        this.candidatCourant = candidatCourant;
    }

    /** « J'ai postulé » : la candidature est créée automatiquement au statut EN_ATTENTE. */
    @Transactional
    public Candidature creer(CreerCandidatureRequest demande) {
        Candidature candidature = new Candidature(
                candidatCourant.id(),
                demande.intitule().strip(),
                demande.entreprise().strip(),
                nettoyer(demande.lieu()),
                nettoyer(demande.urlOffre()),
                nettoyer(demande.descriptionOffre()));
        return repository.save(candidature);
    }

    @Transactional(readOnly = true)
    public List<Candidature> lister(StatutCandidature statut) {
        Long utilisateurId = candidatCourant.id();
        if (statut == null) {
            return repository.findByUtilisateurIdOrderByDateCandidatureDescIdDesc(utilisateurId);
        }
        return repository.findByUtilisateurIdAndStatutOrderByDateCandidatureDescIdDesc(utilisateurId, statut);
    }

    /** Une candidature d'un autre candidat répond 404, comme si elle n'existait pas. */
    @Transactional(readOnly = true)
    public Candidature trouver(Long id) {
        return repository.findByIdAndUtilisateurId(id, candidatCourant.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Candidature " + id + " introuvable."));
    }

    @Transactional
    public Candidature changerStatut(Long id, StatutCandidature cible, LocalDate dateEntretien) {
        Candidature candidature = trouver(id);
        if (!candidature.getStatut().peutPasserA(cible)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Transition interdite : " + candidature.getStatut().getLibelle()
                            + " -> " + cible.getLibelle() + ".");
        }
        candidature.appliquerStatut(cible, dateEntretien);
        return repository.save(candidature);
    }

    @Transactional(readOnly = true)
    public Map<StatutCandidature, Long> compter() {
        Long utilisateurId = candidatCourant.id();
        Map<StatutCandidature, Long> compteurs = new EnumMap<>(StatutCandidature.class);
        for (StatutCandidature statut : StatutCandidature.values()) {
            compteurs.put(statut, repository.countByUtilisateurIdAndStatut(utilisateurId, statut));
        }
        return compteurs;
    }

    private static String nettoyer(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        return texte.strip();
    }
}
