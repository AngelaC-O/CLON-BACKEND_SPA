package dev.com.Synapsis_Spa_Cautiva.personal.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PersonalApi {

	/** Vacío si esa persona no es del personal. */
	Optional<Long> obtenerIdSede(Long idPersonal);

	/** Personal activo de la sede que atiende (RF06), con horario ese día y con el área indicada. Sin idArea, con cualquier área. */
	List<EspecialistaHorario> listarEspecialistas(Long idSede, Long idArea, LocalDate fecha);

	/** [It. 2] CUS08: el personal activo de la sede que atiende, para el filtro de la agenda; sin horario. */
	List<EspecialistaHorario> listarEspecialistas(Long idSede);

	/** [It. 2] CUS52: todo el personal ACTIVO, con su cargo y sin horario. Los nombres los pone seguridad (DF-23). */
	List<EspecialistaHorario> listarPersonal();

}
