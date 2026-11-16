package dev.com.Synapsis_Spa_Cautiva.sedes.api;

/** [It. 2.1 · nombres] Lo que sedes publica de una sede; lo usa seguridad para el nombre de la sede de la sesión (CUS50, Duda 7). */
public interface SedeApi {

	/** 404 (RN26) si la sede no existe. */
	DatosSede obtenerSede(Long idSede);

}
