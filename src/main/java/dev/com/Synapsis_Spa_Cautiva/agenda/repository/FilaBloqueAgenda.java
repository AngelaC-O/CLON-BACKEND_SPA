package dev.com.Synapsis_Spa_Cautiva.agenda.repository;

import java.time.LocalDateTime;

/**
 * Proyección de V_AGENDA_BLOQUES, que no tiene entidad. getFin ocupa al recurso e incluye su limpieza (RN03). [It. 2] getFinServicio ocupa
 * a la especialista, sin limpieza: el fin real de la atención si ya terminó, o el inicio más la duración (RN03, v5.1).
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

	LocalDateTime getFinServicio();

	Long getIdCliente();

	String getNombreClientePaso();

}
