package fr.miage.suivicandidatures.candidatures;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Toutes les requêtes sont filtrées par candidat : on ne lit jamais les candidatures d'un autre. */
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    Optional<Candidature> findByIdAndUtilisateurId(Long id, Long utilisateurId);

    List<Candidature> findByUtilisateurIdOrderByDateCandidatureDescIdDesc(Long utilisateurId);

    List<Candidature> findByUtilisateurIdAndStatutOrderByDateCandidatureDescIdDesc(
            Long utilisateurId, StatutCandidature statut);

    long countByUtilisateurIdAndStatut(Long utilisateurId, StatutCandidature statut);
}
