package fr.miage.suivicandidatures.commun;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

	Optional<Utilisateur> findByEmail(String email);
}
