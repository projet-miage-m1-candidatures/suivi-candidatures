package fr.miage.suivicandidatures.offres;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OffreTest {

    @Test
    void construit_une_offre_avec_les_champs_obligatoires() {
        Offre offre = Offre.builder(SourceOffre.FRANCE_TRAVAIL, "123ABC", "  Développeur Java  ").build();

        assertThat(offre.getTitre()).isEqualTo("Développeur Java");
        assertThat(offre.getDateImport()).isNotNull();
    }

    @Test
    void refuse_une_offre_sans_titre() {
        Offre.Builder builder = Offre.builder(SourceOffre.FRANCE_TRAVAIL, "123ABC", "   ");

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("titre");
    }

    @Test
    void refuse_une_offre_sans_identifiant_externe() {
        Offre.Builder builder = Offre.builder(SourceOffre.LIEN, null, "Stage data");

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifiant");
    }

    @Test
    void refuse_une_offre_sans_source() {
        Offre.Builder builder = Offre.builder(null, "123ABC", "Stage data");

        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
    }
}
