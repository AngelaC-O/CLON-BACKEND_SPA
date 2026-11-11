package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.util.List;

/**
 * permisos: MODULO:ACCION, con los que la IGU habilita los módulos (RF78). [It. 2] idPersonal, nombres, apellidos y rol: GET /auth/sesion
 * devuelve la sesión vigente al recargar, para que el navegador no la guarde (CUS50 modificado).
 */
public record SesionDTO(Long idUsuario, String nombreUsuario, Long idSede, Boolean debeCambiarClave, List<String> permisos, Long idPersonal,
		String nombres, String apellidos, String rol) {
}
