package fr.miage.suivicandidatures.commun.ia;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProprietesIa.class)
class ConfigurationIa {

	@Bean
	@ConditionalOnProperty(prefix = "ia", name = "fournisseur", havingValue = "faux", matchIfMissing = true)
	AssistantIaFaux assistantIaFaux() {
		return new AssistantIaFaux();
	}

	@Bean
	@ConditionalOnProperty(prefix = "ia", name = "fournisseur", havingValue = "openai")
	AssistantIaOpenAi assistantIaOpenAi(ProprietesIa proprietes) {
		HttpClient clientHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
		JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(clientHttp);
		fabrique.setReadTimeout(Duration.ofSeconds(60));
		return new AssistantIaOpenAi(proprietes, RestClient.builder().requestFactory(fabrique));
	}
}
