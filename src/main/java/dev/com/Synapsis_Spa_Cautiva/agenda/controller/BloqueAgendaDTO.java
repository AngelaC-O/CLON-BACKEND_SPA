package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDateTime;

/**
 * Una franja de la agenda (RF11). OCUPADA trae origen, idOrigen, variante y estado, y su fin incluye la limpieza (RN03); LIBRE es de
 * una especialista o de un recurso, según cuál venga con valor.
 */
public record BloqueAgendaDTO(String franja, String origen, Long idOrigen, Long idPersonal, Long idRecurso, Long idVariante, LocalDateTime inicio,
		LocalDateTime fin, String estado) {
}
