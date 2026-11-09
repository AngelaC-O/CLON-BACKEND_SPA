package dev.com.Synapsis_Spa_Cautiva.agenda.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.LockModeType;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioCita extends JpaRepository<Cita, Long> {

	/** Citas por atender de la ficha de la clienta (CUS03, RF02). */
	@Query("""
			select c from Cita c
			where c.idCliente = :idCliente and c.estado = dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita.PENDIENTE
			order by c.fechaHoraInicio
			""")
	List<Cita> findPendientesPorCliente(@Param("idCliente") Long idCliente);

	/** Mi agenda de la especialista (CUS11, RF13): las citas del día que no se cancelaron ni reprogramaron. */
	default List<Cita> findDelDiaPorPersonal(Long idPersonal, LocalDate fecha) {
		return findDelPersonalEntre(idPersonal, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
	}

	@Query("""
			select c from Cita c
			where c.idPersonal = :idPersonal
			and c.fechaHoraInicio >= :desde and c.fechaHoraInicio < :hasta
			and c.estado in (dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita.PENDIENTE,
				dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita.EN_ATENCION,
				dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita.FINALIZADA)
			order by c.fechaHoraInicio
			""")
	List<Cita> findDelPersonalEntre(@Param("idPersonal") Long idPersonal, @Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	default List<Cita> findOcupacion(Long idSede, LocalDate fecha) {
		return findOcupacionEntre(idSede, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
	}

	/** Bloquea las citas vivas del día de la sede: dos recepcionistas no reservan la misma hora (RN02). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select c from Cita c
			where c.idSede = :idSede
			and c.fechaHoraInicio >= :desde and c.fechaHoraInicio < :hasta
			and c.estado in (dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita.PENDIENTE,
				dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita.EN_ATENCION)
			""")
	List<Cita> findOcupacionEntre(@Param("idSede") Long idSede, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

	/** Lectura sin bloqueo para las consultas de horas libres y de agenda (RF06, RF11). */
	@Query(nativeQuery = true, value = """
			SELECT origen AS "origen", id_origen AS "idOrigen", id_personal AS "idPersonal", id_recurso AS "idRecurso",
				id_variante AS "idVariante", inicio AS "inicio", fin_con_limpieza AS "fin", estado AS "estado"
			FROM V_AGENDA_BLOQUES
			WHERE id_sede = :idSede AND inicio >= :desde AND inicio < :hasta
			ORDER BY inicio
			""")
	List<FilaBloqueAgenda> findBloques(@Param("idSede") Long idSede, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

}
