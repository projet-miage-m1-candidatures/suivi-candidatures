package fr.miage.suivicandidatures.candidatures;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreparationEntretienRepository extends JpaRepository<PreparationEntretien, Long> {

    Optional<PreparationEntretien> findByCandidatureId(Long candidatureId);
}
