package fr.miage.suivicandidatures.candidatures;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatutCandidatureTest {

    @Test
    void enAttentePeutPasserEnEntretienOuEtreRefusee() {
        assertTrue(StatutCandidature.EN_ATTENTE.peutPasserA(StatutCandidature.ENTRETIEN));
        assertTrue(StatutCandidature.EN_ATTENTE.peutPasserA(StatutCandidature.REFUSEE));
    }

    @Test
    void entretienPeutSeulementEtreRefusee() {
        assertTrue(StatutCandidature.ENTRETIEN.peutPasserA(StatutCandidature.REFUSEE));
        assertFalse(StatutCandidature.ENTRETIEN.peutPasserA(StatutCandidature.EN_ATTENTE));
    }

    @Test
    void refuseeEstUnEtatFinal() {
        for (StatutCandidature cible : StatutCandidature.values()) {
            assertFalse(StatutCandidature.REFUSEE.peutPasserA(cible));
        }
    }

    @Test
    void unStatutNePeutPasPasserSurLuiMeme() {
        for (StatutCandidature statut : StatutCandidature.values()) {
            assertFalse(statut.peutPasserA(statut));
        }
    }
}
