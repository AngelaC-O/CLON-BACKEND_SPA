package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class UsuarioActualSesion implements UsuarioActual {

	@Override
	public Optional<Long> buscarIdUsuario() {
		return sesion().map(UsuarioSesion::idUsuario);
	}

	@Override
	public Long idUsuario() {
		return exigirSesion().idUsuario();
	}

	@Override
	public Long idSede() {
		return exigirSesion().idSede();
	}

	private UsuarioSesion exigirSesion() {
		return sesion().orElseThrow(() -> new ReglaNegocioException("SIN_SESION", "Inicie sesión para continuar.", HttpStatus.UNAUTHORIZED));
	}

	private Optional<UsuarioSesion> sesion() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioSesion usuario) {
			return Optional.of(usuario);
		}
		return Optional.empty();
	}

}
