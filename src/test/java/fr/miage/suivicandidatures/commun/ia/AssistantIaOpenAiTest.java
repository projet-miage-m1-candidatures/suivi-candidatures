package fr.miage.suivicandidatures.commun.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(OutputCaptureExtension.class)
class AssistantIaOpenAiTest {

	private static final String URL = "https://assistant.example.test";
	private static final String CHAT_COMPLETIONS = URL + "/v1/chat/completions";
	private static final String CLE_FICTIVE = "cle-fictive";

	private final RestClient.Builder constructeur = RestClient.builder();
	private final MockRestServiceServer serveur = MockRestServiceServer.bindTo(constructeur).build();

	@AfterEach
	void verifierLesRequetes() {
		serveur.verify();
	}

	@Test
	void envoieLesDeuxMessagesEtRenvoieLeTexte() {
		serveur.expect(requestTo(CHAT_COMPLETIONS))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CLE_FICTIVE))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						{
						  "model": "gpt-6-luna",
						  "messages": [
						    {"role": "developer", "content": "Réponds en français."},
						    {"role": "user", "content": "Propose une question SQL."}
						  ]
						}
						""", JsonCompareMode.STRICT))
				.andRespond(withSuccess("""
						{"choices":[{"message":{"role":"assistant","content":"À quoi sert GROUP BY ?"}}]}
						""", MediaType.APPLICATION_JSON));

		assertThat(assistant(CLE_FICTIVE).generer("Réponds en français.", "Propose une question SQL."))
				.isEqualTo("À quoi sert GROUP BY ?");
	}

	@Test
	void refuseLaGenerationSansCleAvantToutAppel() {
		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> assistant(" ").generer("Consignes", "Demande"))
				.satisfies(erreur -> {
					assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
					assertThat(erreur.getReason()).isEqualTo("L'assistant IA n'est pas configuré");
				});
	}

	@Test
	void traduitLaLimiteOpenAiEn503() {
		serveur.expect(requestTo(CHAT_COMPLETIONS)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> assistant(CLE_FICTIVE).generer("Consignes", "Demande"))
				.satisfies(erreur -> {
					assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
					assertThat(erreur.getReason()).isEqualTo("Limite de l'assistant IA atteinte, réessayez plus tard");
				});
	}

	@ParameterizedTest
	@ValueSource(ints = {401, 500})
	void traduitLesAutresErreursHttpEn502(int statut) {
		serveur.expect(requestTo(CHAT_COMPLETIONS)).andRespond(withStatus(HttpStatus.valueOf(statut)));

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> assistant(CLE_FICTIVE).generer("Consignes", "Demande"))
				.satisfies(erreur -> assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
	}

	@Test
	void traduitLaCleInvalideEn502EtJournaliseLesIdentifiants(CapturedOutput sortie) {
		serveur.expect(requestTo(CHAT_COMPLETIONS))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("""
						{"error":{
						  "code":"invalid_api_key",
						  "type":"invalid_request_error",
						  "message":"Clé cle-fictive pour demande-privee et consignes-privees"
						}}
						"""));

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> assistant(CLE_FICTIVE).generer("consignes-privees", "demande-privee"))
				.satisfies(erreur -> assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
		assertThat(sortie.getOut()).contains("OpenAI HTTP 401 (error.code=invalid_api_key, error.type=invalid_request_error)")
				.doesNotContain(CLE_FICTIVE, "demande-privee", "consignes-privees");
	}

	@ParameterizedTest
	@ValueSource(strings = {"<h1>erreur-intermediaire</h1>", "{\"error\":{\"code\":\"invalid_api_key\"}}"})
	void journaliseSeulementLeStatutSansLesDeuxIdentifiants(String corps, CapturedOutput sortie) {
		serveur.expect(requestTo(CHAT_COMPLETIONS))
				.andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON).body(corps));

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> assistant(CLE_FICTIVE).generer("Consignes", "Demande"))
				.satisfies(erreur -> assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
		assertThat(sortie.getOut()).contains("OpenAI HTTP 502")
				.doesNotContain("error.code=", "error.type=", "erreur-intermediaire", "invalid_api_key");
	}

	@Test
	void traduitUnDelaiDepasseEn502(CapturedOutput sortie) {
		serveur.expect(requestTo(CHAT_COMPLETIONS)).andRespond(withException(new SocketTimeoutException()));

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> assistant(CLE_FICTIVE).generer("Consignes", "Demande"))
				.satisfies(erreur -> assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
		assertThat(sortie.getOut()).contains("Appel OpenAI impossible (ResourceAccessException)");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "{\"choices\":[]}", "{\"choices\":[{\"message\":{\"content\":\" \"}}]}"})
	void refuseUneReponseSansTexte(String corps) {
		serveur.expect(requestTo(CHAT_COMPLETIONS)).andRespond(withSuccess(corps, MediaType.APPLICATION_JSON));

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> assistant(CLE_FICTIVE).generer("Consignes", "Demande"))
				.satisfies(erreur -> assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
	}

	private AssistantIaOpenAi assistant(String cle) {
		return new AssistantIaOpenAi(new ProprietesIa("openai", "gpt-6-luna", cle, URL), constructeur);
	}
}
