package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.SesionOracle;
import dev.com.Synapsis_Spa_Cautiva.personal.api.PersonalApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.SesionDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Auditoria;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.EstadoUsuario;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Usuario;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioAuditoria;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioUsuario;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioAutenticacion {

	private static final String CREDENCIALES_INVALIDAS = "El usuario o la clave no son correctos.";

	private final RepositorioUsuario repositorioUsuario;

	private final RepositorioAuditoria repositorioAuditoria;

	private final CodificadorClave codificadorClave;

	private final SesionOracle sesionOracle;

	private final PersonalApi personalApi;

	/** Corre sin PKG_SESION hasta validar la clave. El error debe confirmar: el intento fallido tiene que quedar. */
	@Transactional(noRollbackFor = ReglaNegocioException.class)
	public SesionDTO iniciarSesion(String nombreUsuario, String clave) {
		Usuario oUsuario = repositorioUsuario.findByNombreUsuario(nombreUsuario)
			.orElseThrow(() -> new ReglaNegocioException("RF77", CREDENCIALES_INVALIDAS, HttpStatus.UNAUTHORIZED));
		exigirCuentaHabilitada(oUsuario);
		if (!oUsuario.autenticar(clave)) {
			oUsuario.registrarIntentoFallido();
			repositorioUsuario.save(oUsuario);
			auditar(oUsuario, "INICIO_SESION_FALLIDO", "Clave incorrecta");
			throw new ReglaNegocioException("RF77", CREDENCIALES_INVALIDAS, HttpStatus.UNAUTHORIZED);
		}
		// Un bloqueo vencido debe llegar a la base antes de iniciar: PKG_SESION rechaza a quien sigue BLOQUEADO
		repositorioUsuario.saveAndFlush(oUsuario);
		sesionOracle.iniciar(oUsuario.getIdUsuario());
		oUsuario.registrarAcceso(LocalDateTime.now());
		repositorioUsuario.save(oUsuario);
		List<String> permisos = repositorioUsuario.findPermisosPorRol(oUsuario.getIdRol()).stream()
			.map(permiso -> permiso.getModulo() + ":" + permiso.getAccion())
			.toList();
		Long idSede = personalApi.obtenerIdSede(oUsuario.getIdPersonal()).orElseThrow();
		return new SesionDTO(oUsuario.getIdUsuario(), oUsuario.getNombreUsuario(), idSede, oUsuario.getDebeCambiarClave(), permisos);
	}

	@Transactional
	public void cerrarSesion(Long idUsuario) {
		repositorioAuditoria.save(new Auditoria(idUsuario, LocalDateTime.now(), ServicioAuditoria.MODULO, "CERRAR_SESION", "USUARIO",
				idUsuario.intValue(), null));
	}

	@Transactional
	public void cambiarClave(Long idUsuario, String claveActual, String claveNueva) {
		Usuario oUsuario = repositorioUsuario.findById(idUsuario).orElseThrow();
		if (!oUsuario.autenticar(claveActual)) {
			throw new ReglaNegocioException("RF82", "La clave actual no es correcta.");
		}
		if (claveActual.equals(claveNueva)) {
			throw new ReglaNegocioException("RF82", "La clave nueva debe ser distinta de la actual.");
		}
		oUsuario.cambiarClave(codificadorClave.codificar(claveNueva));
		repositorioUsuario.save(oUsuario);
	}

	private void exigirCuentaHabilitada(Usuario oUsuario) {
		if (oUsuario.getEstado() == EstadoUsuario.INACTIVO) {
			throw new ReglaNegocioException("RF53", "La cuenta está inactiva. Consulte con la Administradora.", HttpStatus.LOCKED);
		}
		if (repositorioUsuario.tieneBloqueoVigente(oUsuario.getIdUsuario())) {
			throw new ReglaNegocioException("RF77", "La cuenta está bloqueada por intentos fallidos. Intente más tarde.", HttpStatus.LOCKED);
		}
	}

	private void auditar(Usuario oUsuario, String accion, String detalle) {
		repositorioAuditoria.save(new Auditoria(oUsuario.getIdUsuario(), LocalDateTime.now(), ServicioAuditoria.MODULO, accion, "USUARIO",
				oUsuario.getIdUsuario().intValue(), detalle));
	}

}
