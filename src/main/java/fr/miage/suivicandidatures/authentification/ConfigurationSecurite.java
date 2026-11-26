package fr.miage.suivicandidatures.authentification;

import fr.miage.suivicandidatures.commun.UtilisateurRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration(proxyBeanMethods = false)
class ConfigurationSecurite {

	@Bean
	SecurityFilterChain chaineSecurite(HttpSecurity http) throws Exception {
		return http
				.csrf(CsrfConfigurer::spa)
				.requestCache(AbstractHttpConfigurer::disable)
				.formLogin(connexion -> connexion
						.loginPage("/connexion.html")
						.loginProcessingUrl("/api/v1/auth/connexion")
						.usernameParameter("email")
						.passwordParameter("motDePasse")
						.successHandler((requete, reponse, candidat) -> reponse.setStatus(HttpStatus.OK.value()))
						.failureHandler((requete, reponse, erreur) -> {
							reponse.setStatus(HttpStatus.UNAUTHORIZED.value());
							reponse.setContentType("text/plain;charset=UTF-8");
							reponse.getWriter().write("Adresse e-mail ou mot de passe incorrect.");
						}))
				.logout(deconnexion -> deconnexion
						.logoutUrl("/api/v1/auth/deconnexion")
						.deleteCookies("JSESSIONID")
						.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
				.exceptionHandling(erreurs -> erreurs
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(acces -> acces
						.requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/inscription", "/api/v1/auth/connexion").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll())
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BcryptCandidat();
	}

	@Bean
	UserDetailsService candidats(UtilisateurRepository utilisateurs) {
		return email -> utilisateurs.findByEmail(AdresseEmail.normaliser(email))
				.map(CandidatAuthentifie::new)
				.orElseThrow(() -> new UsernameNotFoundException("Adresse e-mail inconnue"));
	}
}
