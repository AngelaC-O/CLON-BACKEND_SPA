package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.util.List;

/** permisos: MODULO:ACCION, con los que la IGU habilita los módulos (RF78). */
public record SesionDTO(Long idUsuario, String nombreUsuario, Long idSede, Boolean debeCambiarClave, List<String> permisos) {
}
