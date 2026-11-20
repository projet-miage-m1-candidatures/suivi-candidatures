package fr.miage.suivicandidatures.offres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TriOffreRepository extends JpaRepository<TriOffre, Long> {

    Optional<TriOffre> findByUtilisateurIdAndOffreId(Long utilisateurId, Long offreId);

    List<TriOffre> findByUtilisateurIdAndChoixOrderByDateChoixDesc(Long utilisateurId, ChoixTri choix);
}
