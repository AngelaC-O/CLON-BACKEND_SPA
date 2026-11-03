package dev.com.Synapsis_Spa_Cautiva.agenda.repository;

import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioAtencion extends JpaRepository<Atencion, Long> {

	/**
	 * Parte ATENCION de findOcupacion (RN34): la sede solo se conoce por el recurso, y un SELECT sobre la vista
	 * V_AGENDA_BLOQUES no se puede bloquear, así que la vista filtra y se bloquean las filas de ATENCION.
	 */
	@Query(nativeQuery = true, value = """
			SELECT a.* FROM ATENCION a
			WHERE a.id_atencion IN (SELECT b.id_origen FROM V_AGENDA_BLOQUES b
				WHERE b.origen = 'ATENCION_SIN_CITA' AND b.id_sede = :idSede AND TRUNC(b.inicio) = :fecha)
			FOR UPDATE
			""")
	List<Atencion> findOcupacionSinCita(@Param("idSede") Long idSede, @Param("fecha") LocalDate fecha);

}
