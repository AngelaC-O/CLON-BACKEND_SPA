package dev.com.Synapsis_Spa_Cautiva.sedes.api;

/** tipo y estado son texto porque los enums del dominio son internos. [It. 2] estado: CUS61 y el filtro de CUS08 listan todos los recursos. */
public record DatosRecurso(Long idRecurso, Long idSede, String nombre, String tipo, Integer minutosLimpieza, String estado) {
}
