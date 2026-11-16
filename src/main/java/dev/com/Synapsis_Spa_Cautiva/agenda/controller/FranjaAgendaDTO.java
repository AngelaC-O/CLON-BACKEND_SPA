package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDateTime;

/**
 * Una franja de la agenda (RF11). OCUPADA trae origen, idOrigen, variante y estado; LIBRE es de una especialista o de un recurso, según cuál
 * venga con valor. [It. 2] Antes BloqueAgendaDTO; el nombre es el del diagrama de CUS08. Los nombres de la clienta, la especialista y el
 * servicio los ponen PersonaApi y CatalogoApi. [It. 2.1 · v5.2] Un solo fin, el del servicio, para la especialista y para el recurso: sin tiempo
 * de limpieza (RN03); finServicio se retiró.
 */
public record FranjaAgendaDTO(String franja, String origen, Long idOrigen, Long idPersonal, Long idRecurso, Long idVariante, LocalDateTime inicio,
		LocalDateTime fin, String estado, Long idCliente, String nombreCliente, String nombreEspecialista, String nombreServicio) {

	public FranjaAgendaDTO conNombres(String nombreCliente, String nombreEspecialista, String nombreServicio) {
		return new FranjaAgendaDTO(franja, origen, idOrigen, idPersonal, idRecurso, idVariante, inicio, fin, estado, idCliente, nombreCliente,
				nombreEspecialista, nombreServicio);
	}

}
