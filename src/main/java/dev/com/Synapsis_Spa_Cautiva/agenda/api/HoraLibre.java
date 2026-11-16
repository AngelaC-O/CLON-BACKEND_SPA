package dev.com.Synapsis_Spa_Cautiva.agenda.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Una hora con las especialistas y los recursos libres para la variante pedida (RF06, RF17). [It. 2.1 · nombres] especialistas y recursos
 * repiten los id con su nombre (diferencia 11 del front-end); idsPersonal e idsRecurso se conservan.
 */
public record HoraLibre(LocalDateTime inicio, Integer duracionMin, List<Long> idsPersonal, List<Long> idsRecurso, List<EspecialistaLibre> especialistas,
		List<RecursoLibre> recursos) {

	public HoraLibre conEspecialistas(List<EspecialistaLibre> especialistas) {
		return new HoraLibre(inicio, duracionMin, idsPersonal, idsRecurso, especialistas, recursos);
	}

	public record EspecialistaLibre(Long idPersonal, String nombre) {
	}

	public record RecursoLibre(Long idRecurso, String nombre) {
	}

}
