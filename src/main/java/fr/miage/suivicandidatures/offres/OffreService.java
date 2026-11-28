package fr.miage.suivicandidatures.offres;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Point d'entrée pour lire une offre enregistrée.
 * Utilisé par l'API de cette feature, et par les autres features (analyse du CV, suivi des candidatures).
 */
@Service
@Transactional(readOnly = true)
public class OffreService {

    private final OffreRepository offreRepository;

    public OffreService(OffreRepository offreRepository) {
        this.offreRepository = offreRepository;
    }

    public OffreDetail trouver(Long id) {
        return offreRepository.findById(id)
                .map(OffreDetail::depuis)
                .orElseThrow(() -> new OffreIntrouvableException(id));
    }
}
