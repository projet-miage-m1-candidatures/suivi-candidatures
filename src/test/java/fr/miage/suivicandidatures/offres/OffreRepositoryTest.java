package fr.miage.suivicandidatures.offres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Vérifie les règles portées par la base (migrations Flyway) sur une vraie base PostgreSQL. */
@SpringBootTest
@Transactional
class OffreRepositoryTest {

    private static final long CANDIDAT = 1L;
    private static final long AUTRE_CANDIDAT = 2L;

    @Autowired
    private OffreRepository offreRepository;

    @Autowired
    private TriOffreRepository triOffreRepository;

    @Test
    void retrouve_une_offre_par_sa_source_et_son_identifiant_externe() {
        offreRepository.save(Offre.builder(SourceOffre.FRANCE_TRAVAIL, "FT-1", "Développeur Java").build());

        assertThat(offreRepository.findBySourceAndIdExterne(SourceOffre.FRANCE_TRAVAIL, "FT-1")).isPresent();
        assertThat(offreRepository.findBySourceAndIdExterne(SourceOffre.LIEN, "FT-1")).isEmpty();
    }

    @Test
    void refuse_deux_fois_la_meme_offre_de_la_meme_source() {
        offreRepository.saveAndFlush(Offre.builder(SourceOffre.FRANCE_TRAVAIL, "FT-1", "Développeur Java").build());
        Offre doublon = Offre.builder(SourceOffre.FRANCE_TRAVAIL, "FT-1", "Autre titre").build();

        assertThatThrownBy(() -> offreRepository.saveAndFlush(doublon))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deux_candidats_peuvent_trier_la_meme_offre() {
        Offre offre = offreRepository.save(
                Offre.builder(SourceOffre.FRANCE_TRAVAIL, "FT-1", "Développeur Java").build());

        triOffreRepository.saveAndFlush(new TriOffre(CANDIDAT, offre, ChoixTri.GARDEE));
        triOffreRepository.saveAndFlush(new TriOffre(AUTRE_CANDIDAT, offre, ChoixTri.PASSEE));

        assertThat(offreRepository.count()).isEqualTo(1);
        assertThat(triOffreRepository.findByUtilisateurIdAndChoixOrderByDateChoixDesc(CANDIDAT, ChoixTri.GARDEE))
                .extracting(tri -> tri.getOffre().getId())
                .containsExactly(offre.getId());
        assertThat(triOffreRepository.findByUtilisateurIdAndChoixOrderByDateChoixDesc(AUTRE_CANDIDAT, ChoixTri.GARDEE))
                .isEmpty();
    }

    @Test
    void refuse_deux_tris_du_meme_candidat_sur_la_meme_offre() {
        Offre offre = offreRepository.save(
                Offre.builder(SourceOffre.FRANCE_TRAVAIL, "FT-1", "Développeur Java").build());
        triOffreRepository.saveAndFlush(new TriOffre(CANDIDAT, offre, ChoixTri.GARDEE));
        TriOffre doublon = new TriOffre(CANDIDAT, offre, ChoixTri.PASSEE);

        assertThatThrownBy(() -> triOffreRepository.saveAndFlush(doublon))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
