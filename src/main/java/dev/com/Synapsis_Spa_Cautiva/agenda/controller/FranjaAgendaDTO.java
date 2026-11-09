package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDateTime;

/**
 * Una franja de la agenda (RF11). OCUPADA trae origen, idOrigen, variante y estado, y su fin incluye la limpieza del recurso (RN03); LIBRE es
 * de una especialista o de un recurso, según cuál venga con valor. [It. 2] Antes BloqueAgendaDTO; el nombre es el del diagrama de CUS08.
 * Los campos de la Iteración 1 no cambian. finServicio: hasta cuándo ocupa a la especialista, que queda libre al terminar aunque el recurso
 * siga en limpieza (RN03, v5.1). Los nombres de la clienta, la especialista y el servicio los ponen PersonaApi y CatalogoApi.
 */
public record FranjaAgendaDTO(String franja, String origen, Long idOrigen, Long idPersonal, Long idRecurso, Long idVariante, LocalDateTime inicio,
		LocalDateTime fin, String estado, LocalDateTime finServicio, Long idCliente, String nombreCliente, String nombreEspecialista,
		String nombreServicio) {

	public FranjaAgendaDTO conNombres(String nombreCliente, String nombreEspecialista, String nombreServicio) {
		return new FranjaAgendaDTO(franja, origen, idOrigen, idPersonal, idRecurso, idVariante, inicio, fin, estado, finServicio, idCliente,
				nombreCliente, nombreEspecialista, nombreServicio);
	}

}
