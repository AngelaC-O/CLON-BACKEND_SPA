package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDateTime;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita;

/**
 * [It. 2] CUS11 «Mi agenda»: una cita del día de la especialista con su atención EN_CURSO, si la tiene (para retomarla y finalizarla).
 * [It. 2.1 · nombres] Con el nombre de la clienta, del servicio («Servicio · Variante») y del recurso (Duda 1).
 */
public record CitaAtencionDTO(Long idCita, Long idCliente, Long idRecurso, Long idVariante, LocalDateTime fechaHoraInicio, Integer duracionMin,
		LocalDateTime fechaHoraFin, EstadoCita estado, Long idAtencion, EstadoAtencion estadoAtencion, String nombreCliente, String nombreServicio,
		String nombreRecurso) {
}
