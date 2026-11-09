package dev.com.Synapsis_Spa_Cautiva.comun.api;

/**
 * [It. 2] Un aviso del panel de Inicio (CUS50, RF47, RF101). permisoRequerido es MODULO:ACCION: ServicioAviso solo muestra el aviso a
 * quien lo tiene (null: lo ve cualquier usuario con sesión). ruta: la pantalla del front-end que resuelve el aviso.
 */
public record AvisoDTO(String tipo, String mensaje, Integer cantidad, String permisoRequerido, String ruta) {
}
