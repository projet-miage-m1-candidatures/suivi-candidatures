package fr.miage.suivicandidatures.cv;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface CvRepository extends JpaRepository<Cv, Long> {

	List<Cv> findAllByUtilisateurIdOrderByDateModificationDescIdDesc(Long utilisateurId);

	Optional<Cv> findByIdAndUtilisateurId(Long id, Long utilisateurId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Cv> findForUpdateByIdAndUtilisateurId(Long id, Long utilisateurId);
}
