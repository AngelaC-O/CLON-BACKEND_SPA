package dev.com.Synapsis_Spa_Cautiva.personal.api;

import java.util.Optional;

public interface PersonalApi {

	/** Vacío si esa persona no es del personal. */
	Optional<Long> obtenerIdSede(Long idPersonal);

}
