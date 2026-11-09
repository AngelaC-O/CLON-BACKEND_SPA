package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.util.List;

/** [It. 2] CUS08: las especialistas y los recursos de la sede para los filtros de la agenda (Duda 2 del front-end). */
public record FiltrosAgendaDTO(List<Especialista> especialistas, List<Recurso> recursos) {

	public record Especialista(Long idPersonal, String nombres, String apellidos) {
	}

	/** tipo y estado son texto, como en RecursoApi. */
	public record Recurso(Long idRecurso, String nombre, String tipo, String estado) {
	}

}
