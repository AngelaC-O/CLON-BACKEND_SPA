package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.io.IOException;
import java.util.Set;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Con debe_cambiar_clave = 'S', solo se puede cambiar la clave o salir (RF82). */
@RequiredArgsConstructor
class FiltroCambioClave extends OncePerRequestFilter {

	private static final Set<String> PERMITIDAS = Set.of("/api/v1/auth/cambio-clave", "/api/v1/auth/logout");

	private final HandlerExceptionResolver resolutorDeErrores;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
			throws ServletException, IOException {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		boolean debeCambiar = autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioSesion usuario
				&& usuario.debeCambiarClave();
		if (debeCambiar && !PERMITIDAS.contains(request.getRequestURI())) {
			resolutorDeErrores.resolveException(request, response, null,
					new ReglaNegocioException("RF82", "Debe cambiar su clave temporal antes de continuar.", HttpStatus.FORBIDDEN));
			return;
		}
		cadena.doFilter(request, response);
	}

}
