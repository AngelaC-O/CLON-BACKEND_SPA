package dev.com.Synapsis_Spa_Cautiva.catalogo.api;

/** Lo que agenda necesita de una variante. tipoRecurso es texto porque el enum del dominio es interno. */
public record DatosVariante(Long idVariante, Long idServicio, Long idArea, Integer duracionRefMin, String tipoRecurso, String nombre) {
}
