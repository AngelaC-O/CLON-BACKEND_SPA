package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Rol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Usuario;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioRol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioUsuario;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * La sesión solo guarda lo que se leyó al ingresar. El personal y el rol se leen de USUARIO cuando se piden (RN50), para que un cambio de
 * rol (CUS53) valga sin esperar al siguiente ingreso. Los repositorios son @Lazy: el administrador de transacciones depende de este bean.
 */
@Component
class UsuarioActualSesion implements UsuarioActual {

	static final String ADMINISTRADORA = "ADMINISTRADORA";

	private final RepositorioUsuario repositorioUsuario;

	private final RepositorioRol repositorioRol;

	UsuarioActualSesion(@Lazy RepositorioUsuario repositorioUsuario, @Lazy RepositorioRol repositorioRol) {
		this.repositorioUsuario = repositorioUsuario;
		this.repositorioRol = repositorioRol;
	}

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

	@Override
	public Long idPersonal() {
		return buscarUsuario(idUsuario()).getIdPersonal();
	}

	@Override
	public boolean esAdministradora(Long idUsuario) {
		return repositorioRol.findById(buscarUsuario(idUsuario).getIdRol()).map(Rol::getNombre).filter(ADMINISTRADORA::equals).isPresent();
	}

	private Usuario buscarUsuario(Long idUsuario) {
		return repositorioUsuario.findById(idUsuario)
			.orElseThrow(() -> new ReglaNegocioException("SIN_SESION", "Inicie sesión para continuar.", HttpStatus.UNAUTHORIZED));
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
