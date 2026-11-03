package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Sesión de servidor y CSRF con la cookie XSRF-TOKEN que lee Angular (ADR-07). */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

	private static final String LOGIN = "/api/v1/auth/login";

	/** El webhook de Izipay no tiene sesión: lo valida su firma HMAC (ControladorNotificacionPasarela). */
	private static final String NOTIFICACIONES_PASARELA = "/api/v1/pagos/notificaciones";

	@Bean
	SecurityContextRepository repositorioDeContexto() {
		return new HttpSessionSecurityContextRepository();
	}

	/** Sin usuarios: el ingreso es ServicioAutenticacion. Evita la clave que Spring Boot generaría. */
	@Bean
	UserDetailsService sinUsuariosEnMemoria() {
		return new InMemoryUserDetailsManager();
	}

	@Bean
	SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, SecurityContextRepository repositorioDeContexto,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolutorDeErrores) throws Exception {
		return http
			.csrf(csrf -> csrf.spa().ignoringRequestMatchers(LOGIN, NOTIFICACIONES_PASARELA))
			.securityContext(contexto -> contexto.securityContextRepository(repositorioDeContexto))
			.authorizeHttpRequests(permisos -> permisos
				.requestMatchers(HttpMethod.POST, LOGIN, NOTIFICACIONES_PASARELA).permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(errores -> errores
				.authenticationEntryPoint((request, response, ex) -> resolutorDeErrores.resolveException(request, response, null, ex))
				.accessDeniedHandler((request, response, ex) -> resolutorDeErrores.resolveException(request, response, null, ex)))
			.logout(AbstractHttpConfigurer::disable)
			.addFilterAfter(new FiltroCambioClave(resolutorDeErrores), AuthorizationFilter.class)
			.build();
	}

}
