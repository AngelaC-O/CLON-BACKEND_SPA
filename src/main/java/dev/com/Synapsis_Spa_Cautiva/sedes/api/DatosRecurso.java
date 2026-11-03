package dev.com.Synapsis_Spa_Cautiva.sedes.api;

/** tipo es texto porque el enum del dominio es interno. */
public record DatosRecurso(Long idRecurso, Long idSede, String nombre, String tipo, Integer minutosLimpieza) {
}
