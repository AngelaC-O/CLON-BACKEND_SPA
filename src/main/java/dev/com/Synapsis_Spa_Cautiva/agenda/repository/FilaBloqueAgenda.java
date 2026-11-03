package dev.com.Synapsis_Spa_Cautiva.agenda.repository;

import java.time.LocalDateTime;

/** Proyección de V_AGENDA_BLOQUES, que no tiene entidad. El fin ya incluye la limpieza del recurso (RN03). */
public interface FilaBloqueAgenda {

	String getOrigen();

	Long getIdOrigen();

	Long getIdPersonal();

	Long getIdRecurso();

	Long getIdVariante();

	LocalDateTime getInicio();

	LocalDateTime getFin();

	String getEstado();

}
