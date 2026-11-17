package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * idSede e idUsuario salen de la sesión; fechaHoraFin la calcula Oracle. [It. 2] idEspera (opcional): la espera de la que sale la cita en
 * CUS05, que pasa a ASIGNADA (RF09). idCitaOrigen y motivoCancelacion vienen en CUS09 y CUS10. [It. 2.1 · nombres] nombreCliente,
 * nombreEspecialista, nombreServicio («Servicio · Variante») y nombreRecurso solo van en las respuestas; en la petición se ignoran.
 */
public record CitaDTO(Long idCita, @NotNull Long idCliente, Long idSede, @NotNull Long idPersonal, @NotNull Long idRecurso,
		@NotNull Long idVariante, @NotNull LocalDateTime fechaHoraInicio, @NotNull @Positive Integer duracionMin, LocalDateTime fechaHoraFin,
		EstadoCita estado, Long idUsuario, BigDecimal adelantoMonto, Long idMedioPago, LocalDateTime fechaAdelanto, Long idCaja, Long idCitaOrigen,
		String motivoCancelacion, Long idEspera, String nombreCliente, String nombreEspecialista, String nombreServicio, String nombreRecurso) {

	public CitaDTO conNombres(String nombreCliente, String nombreEspecialista, String nombreServicio, String nombreRecurso) {
		return new CitaDTO(idCita, idCliente, idSede, idPersonal, idRecurso, idVariante, fechaHoraInicio, duracionMin, fechaHoraFin, estado, idUsuario,
				adelantoMonto, idMedioPago, fechaAdelanto, idCaja, idCitaOrigen, motivoCancelacion, idEspera, nombreCliente, nombreEspecialista,
				nombreServicio, nombreRecurso);
	}

}
