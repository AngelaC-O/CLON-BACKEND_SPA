package dev.com.Synapsis_Spa_Cautiva.sedes.api;

import java.util.List;

public interface RecursoApi {

	DatosRecurso obtenerRecurso(Long idRecurso);

	List<DatosRecurso> listarDisponibles(Long idSede);

	HorarioSede obtenerHorarioSede(Long idSede);

}
