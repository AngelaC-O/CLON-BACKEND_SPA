package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.io.Serializable;
import java.util.Set;

/** Lo que la sesión de servidor recuerda del usuario: permisos MODULO:ACCION leídos al ingresar. */
public record UsuarioSesion(Long idUsuario, String nombreUsuario, Long idSede, boolean debeCambiarClave, Set<String> permisos)
		implements Serializable {

	public UsuarioSesion conClaveVigente() {
		return new UsuarioSesion(idUsuario, nombreUsuario, idSede, false, permisos);
	}

}
