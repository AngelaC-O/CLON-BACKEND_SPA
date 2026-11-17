package dev.com.Synapsis_Spa_Cautiva.agenda.repository;

import java.time.LocalDateTime;

/**
 * Proyección de V_AGENDA_BLOQUES, que no tiene entidad. [It. 2.1 · v5.2] getFin es la columna fin de la vista (antes fin_con_limpieza): el fin
 * del servicio, que ocupa por igual a la especialista y al recurso, o el fin real de la atención si ya terminó (RN03).
 */
public interface FilaBloqueAgenda {

	String getOrigen();

	Long getIdOrigen();

	Long getIdPersonal();

	Long getIdRecurso();

	Long getIdVariante();

	LocalDateTime getInicio();

	LocalDateTime getFin();

	String getEstado();

	Long getIdCliente();

	String getNombreClientePaso();

}
