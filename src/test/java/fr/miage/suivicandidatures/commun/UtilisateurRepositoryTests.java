package fr.miage.suivicandidatures.commun;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UtilisateurRepositoryTests {

	private static final String HACHAGE_FICTIF = "hachage-fictif";

	@Autowired
	private UtilisateurRepository utilisateurs;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void enregistreEtRelitUnUtilisateurParEmail() {
		String email = "alex-" + UUID.randomUUID() + "@example.test";
		Utilisateur alex = utilisateurs.saveAndFlush(new Utilisateur(email, HACHAGE_FICTIF, "Alex", "Martin"));
		assertThat(alex.getId()).isNotNull();

		// Force Hibernate à reconstruire l'objet à partir des données de PostgreSQL.
		entityManager.clear();

		assertThat(utilisateurs.findByEmail(email)).hasValueSatisfying(utilisateur -> {
			assertThat(utilisateur.getId()).isEqualTo(alex.getId());
			assertThat(utilisateur.getEmail()).isEqualTo(email);
			assertThat(utilisateur.getMotDePasseHache()).isEqualTo(HACHAGE_FICTIF);
			assertThat(utilisateur.getPrenom()).isEqualTo("Alex");
			assertThat(utilisateur.getNom()).isEqualTo("Martin");
		});
	}

	@Test
	void refuseDeuxUtilisateursAvecLeMemeEmail() {
		String email = "alex-" + UUID.randomUUID() + "@example.test";
		utilisateurs.saveAndFlush(new Utilisateur(email, HACHAGE_FICTIF, "Alex", "Martin"));

		assertThatThrownBy(() ->
				utilisateurs.saveAndFlush(new Utilisateur(email, HACHAGE_FICTIF, "Camille", "Durand")))
				.isInstanceOf(DataIntegrityViolationException.class)
				.rootCause()
				.isInstanceOfSatisfying(SQLException.class,
						cause -> assertThat(cause.getSQLState()).isEqualTo("23505"));
	}
}
