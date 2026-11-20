package fr.miage.suivicandidatures.offres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OffreRepository extends JpaRepository<Offre, Long> {

    /** Retrouve une offre déjà importée, pour ne pas l'enregistrer deux fois. */
    Optional<Offre> findBySourceAndIdExterne(SourceOffre source, String idExterne);
}
