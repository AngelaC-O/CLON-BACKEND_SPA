package dev.com.Synapsis_Spa_Cautiva.sedes.api;

import java.util.List;

public interface RecursoApi {

	DatosRecurso obtenerRecurso(Long idRecurso);

	List<DatosRecurso> listarDisponibles(Long idSede);

	/** [It. 2] Todos los recursos de la sede, en cualquier estado (CUS61 y el filtro de la agenda de CUS08). */
	List<DatosRecurso> listarRecursos(Long idSede);

	HorarioSede obtenerHorarioSede(Long idSede);

}
