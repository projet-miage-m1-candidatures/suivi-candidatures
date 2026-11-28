package fr.miage.suivicandidatures.candidatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.miage.suivicandidatures.commun.ApiException;
import fr.miage.suivicandidatures.commun.ia.AssistantIa;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** L'IA est remplacée par une lambda : aucun appel réseau, aucun crédit consommé. */
class PreparationEntretienServiceTest {

    private final CandidatureService candidatures = mock(CandidatureService.class);
    private final PreparationEntretienRepository repository = mock(PreparationEntretienRepository.class);
    private Candidature candidature;

    @BeforeEach
    void preparer() {
        candidature = new Candidature(7L, "Développeur Java", "Nordlys", "Puteaux", null, "Spring Boot et SQL");
        when(candidatures.trouver(1L)).thenReturn(candidature);
        when(repository.save(any())).thenAnswer(appel -> appel.getArgument(0));
    }

    private PreparationEntretienService service(AssistantIa assistant) {
        return new PreparationEntretienService(candidatures, repository, assistant, "openai", "mistral");
    }

    @Test
    void refuseUneCandidatureQuiNEstPasAuStatutEntretien() {
        AssistantIa interdit = (consignes, demande) -> { throw new AssertionError("l'IA ne doit pas être appelée"); };
        ApiException erreur = assertThrows(ApiException.class, () -> service(interdit).obtenir(1L, false));
        assertEquals(HttpStatus.CONFLICT, erreur.getStatut());
    }

    @Test
    void genereLaFicheAvecLeModuleIaCommun() {
        candidature.appliquerStatut(StatutCandidature.ENTRETIEN, null);
        when(repository.findByCandidatureId(1L)).thenReturn(Optional.empty());
        String[] recu = new String[2];
        AssistantIa assistant = (consignes, demande) -> {
            recu[0] = consignes;
            recu[1] = demande;
            return "  ## L'entreprise en bref\n";
        };

        PreparationEntretien fiche = service(assistant).obtenir(1L, false);

        assertEquals("## L'entreprise en bref", fiche.getContenu());
        assertEquals("mistral", fiche.getModele());
        assertEquals(PromptEntretien.SYSTEME, recu[0]);
        assertTrue(recu[1].contains("Entreprise : Nordlys"));
        assertTrue(recu[1].contains("<offre>\nSpring Boot et SQL\n</offre>"));
    }

    @Test
    void relitLaFicheEnregistreeSansRappelerLIa() {
        candidature.appliquerStatut(StatutCandidature.ENTRETIEN, null);
        PreparationEntretien existante = new PreparationEntretien(1L);
        existante.remplacer("fiche déjà générée", "mistral");
        when(repository.findByCandidatureId(1L)).thenReturn(Optional.of(existante));
        AssistantIa interdit = (consignes, demande) -> { throw new AssertionError("l'IA ne doit pas être appelée"); };

        assertSame(existante, service(interdit).obtenir(1L, false));
    }

    @Test
    void indiqueLeModeDemonstration() {
        candidature.appliquerStatut(StatutCandidature.ENTRETIEN, null);
        when(repository.findByCandidatureId(1L)).thenReturn(Optional.empty());
        PreparationEntretienService demo = new PreparationEntretienService(
                candidatures, repository, (c, d) -> "texte", "faux", "gpt-6-luna");

        assertEquals("démonstration", demo.obtenir(1L, false).getModele());
    }
}
