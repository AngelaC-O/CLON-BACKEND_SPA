package dev.com.Synapsis_Spa_Cautiva.sedes.api;

/**
 * tipo y estado son texto porque los enums del dominio son internos. [It. 2] estado: CUS61 y el filtro de CUS08 listan todos los recursos.
 * [It. 2.1 · v5.2] Sin minutosLimpieza: el recurso queda libre al terminar el servicio (RN03).
 */
public record DatosRecurso(Long idRecurso, Long idSede, String nombre, String tipo, String estado) {
}
