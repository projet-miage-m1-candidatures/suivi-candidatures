package fr.miage.suivicandidatures.authentification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import fr.miage.suivicandidatures.commun.UtilisateurRepository;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InscriptionTests {

	private final String email = "alex-" + UUID.randomUUID() + "@example.test";
	private final String motDePasse = UUID.randomUUID().toString().substring(0, 15);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoSpyBean
	private UtilisateurRepository utilisateurs;

	@Test
	void inscritAlexAvecUnEmailNormaliseEtUnMotDePasseHache() throws Exception {
		String emailSaisi = "  " + email.toUpperCase(Locale.ROOT) + "  ";
		mvc.perform(inscriptionAvecJeton(emailSaisi, motDePasse))
				.andExpect(status().isCreated())
				.andExpect(content().string(""));

		assertThat(utilisateurs.findByEmail(email)).hasValueSatisfying(alex -> {
			assertThat(alex.getPrenom()).isEqualTo("Alex");
			assertThat(alex.getNom()).isEqualTo("Martin");
			assertThat(alex.getMotDePasseHache()).isNotEqualTo(motDePasse);
			assertThat(passwordEncoder.matches(motDePasse, alex.getMotDePasseHache())).isTrue();
		});
	}

	@ParameterizedTest
	@ValueSource(ints = {14, 37})
	void refuseUnMotDePasseTropCourtOuTropLongPourBCrypt(int nombreDeCaracteres) throws Exception {
		// Chaque « é » occupe deux octets en UTF-8.
		mvc.perform(inscriptionAvecJeton(email, "é".repeat(nombreDeCaracteres)))
				.andExpect(status().isBadRequest());
		assertThat(utilisateurs.findByEmail(email)).isEmpty();
	}

	@Test
	void refuseUneAdresseEmailInvalide() throws Exception {
		mvc.perform(inscriptionAvecJeton("alex-sans-arobase", motDePasse))
				.andExpect(status().isBadRequest());
	}

	@Test
	void refuseUneAdresseDejaUtiliseeApresNormalisation() throws Exception {
		mvc.perform(inscriptionAvecJeton(email, motDePasse)).andExpect(status().isCreated());
		mvc.perform(inscriptionAvecJeton(" " + email.toUpperCase(Locale.ROOT) + " ", motDePasse))
				.andExpect(status().isConflict());
	}

	@Test
	void renvoieUnConflitQuandPostgresqlDetecteLeDoublonALaSauvegarde() throws Exception {
		mvc.perform(inscriptionAvecJeton(email, motDePasse)).andExpect(status().isCreated());
		// Simule une lecture qui précède l’écriture de l’autre inscription.
		doReturn(Optional.empty()).when(utilisateurs).findByEmail(email);

		mvc.perform(inscriptionAvecJeton(email, motDePasse)).andExpect(status().isConflict());
	}

	@Test
	void refuseUneInscriptionSansJetonOuAvecUnJetonInvalide() throws Exception {
		mvc.perform(inscription(email, motDePasse)).andExpect(status().isForbidden());
		mvc.perform(inscription(email, motDePasse).cookie(jetonCsrf()).header("X-XSRF-TOKEN", "invalide"))
				.andExpect(status().isForbidden());
		assertThat(utilisateurs.findByEmail(email)).isEmpty();
	}

	@Test
	void conserveLesPagesPubliquesEtProtegeLesAutresRoutesApi() throws Exception {
		mvc.perform(get("/")).andExpect(status().isOk());
		mvc.perform(get("/connexion.html")).andExpect(status().isOk());
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/moi"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Location"));
	}

	private MockHttpServletRequestBuilder inscriptionAvecJeton(String adresse, String motDePasseSaisi)
			throws Exception {
		Cookie jeton = jetonCsrf();
		return inscription(adresse, motDePasseSaisi).cookie(jeton).header("X-XSRF-TOKEN", jeton.getValue());
	}

	private Cookie jetonCsrf() throws Exception {
		return mvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent())
				.andExpect(cookie().httpOnly("XSRF-TOKEN", false))
				.andReturn().getResponse().getCookie("XSRF-TOKEN");
	}

	private MockHttpServletRequestBuilder inscription(String adresse, String motDePasseSaisi) {
		return post("/api/v1/auth/inscription")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"prenom":"Alex","nom":"Martin","email":"%s","motDePasse":"%s"}
						""".formatted(adresse, motDePasseSaisi));
	}
}
