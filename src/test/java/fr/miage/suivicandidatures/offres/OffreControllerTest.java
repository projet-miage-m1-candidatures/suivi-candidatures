package fr.miage.suivicandidatures.offres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OffreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OffreRepository offreRepository;

    @Test
    @WithMockUser
    void renvoie_le_detail_d_une_offre() throws Exception {
        Offre offre = offreRepository.save(Offre.builder(SourceOffre.FRANCE_TRAVAIL, "FT-1", "Développeur Java")
                .entreprise("ACME")
                .lieu("92 - Nanterre")
                .typeContrat("Alternance")
                .description("Missions : développer des API REST.")
                .url("https://candidat.francetravail.fr/offres/recherche/detail/FT-1")
                .datePublication(LocalDate.of(2026, 11, 18))
                .build());

        mockMvc.perform(get("/api/v1/offres/{id}", offre.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offre.getId()))
                .andExpect(jsonPath("$.source").value("FRANCE_TRAVAIL"))
                .andExpect(jsonPath("$.titre").value("Développeur Java"))
                .andExpect(jsonPath("$.entreprise").value("ACME"))
                .andExpect(jsonPath("$.description").value("Missions : développer des API REST."))
                .andExpect(jsonPath("$.datePublication").value("2026-11-18"));
    }

    @Test
    @WithMockUser
    void renvoie_404_pour_une_offre_inexistante() throws Exception {
        mockMvc.perform(get("/api/v1/offres/{id}", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void renvoie_400_pour_un_identifiant_qui_n_est_pas_un_nombre() throws Exception {
        mockMvc.perform(get("/api/v1/offres/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renvoie_401_sans_utilisateur_connecte() throws Exception {
        Offre offre = offreRepository.save(Offre.builder(SourceOffre.FRANCE_TRAVAIL, "FT-2", "Développeur web").build());

        // Les filtres de sécurité restent actifs : sans @WithMockUser, l'API exige une connexion
        mockMvc.perform(get("/api/v1/offres/{id}", offre.getId()))
                .andExpect(status().isUnauthorized());
    }
}
