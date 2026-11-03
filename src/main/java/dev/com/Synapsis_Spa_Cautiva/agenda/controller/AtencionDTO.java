package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDateTime;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoAtencion;

public record AtencionDTO(Long idAtencion, Long idCita, Long idCliente, Long idPersonal, Long idRecurso, Long idVariante, Long idClientePaquete,
		LocalDateTime horaInicio, LocalDateTime horaFin, Integer tiempoDeclaradoMin, String observaciones, EstadoAtencion estado) {
}
