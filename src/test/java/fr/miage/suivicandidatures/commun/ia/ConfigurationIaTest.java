package fr.miage.suivicandidatures.commun.ia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class ConfigurationIaTest {

	private final ApplicationContextRunner contexteIa = new ApplicationContextRunner()
			.withUserConfiguration(ConfigurationIa.class);

	@Test
	void choisitLeFournisseurFauxSansConfiguration() {
		contexteIa.withInitializer(contexte -> {
			var sources = contexte.getEnvironment().getPropertySources();
			sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
			sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		}).run(contexte -> assertThat(contexte)
				.hasSingleBean(AssistantIa.class)
				.hasSingleBean(AssistantIaFaux.class));
	}

	@Test
	void laProprieteGradlePrimeSurLeFournisseurLocal() {
		contexteIa.withInitializer(contexte -> contexte.getEnvironment().getPropertySources().addLast(
				new MapPropertySource("configurationLocale", Map.of("ia.fournisseur", "openai"))))
				.run(contexte -> assertThat(contexte)
						.hasSingleBean(AssistantIa.class)
						.hasSingleBean(AssistantIaFaux.class));
	}

	@Test
	void choisitOpenAiQuandIlEstConfigure() {
		contexteIa.withPropertyValues("ia.fournisseur=openai", "ia.url=https://assistant.example.test")
				.run(contexte -> assertThat(contexte)
						.hasSingleBean(AssistantIa.class)
						.hasSingleBean(AssistantIaOpenAi.class));
	}
}
