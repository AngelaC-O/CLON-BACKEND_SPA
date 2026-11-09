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

	/**
	 * Lectura sin bloqueo para las consultas de horas libres y de agenda (RF06, RF11). [It. 2] Con CITA y ATENCION (diagrama de CUS08): fin
	 * ocupa al recurso con su limpieza y finServicio a la especialista sin ella (RN03, v5.1). Si la atención de la cita ya terminó, cuenta su
	 * hora_fin: la especialista queda libre desde ahí y el recurso, después de su limpieza.
	 */
	@Query(nativeQuery = true, value = """
			SELECT b.origen AS "origen", b.id_origen AS "idOrigen", b.id_personal AS "idPersonal", b.id_recurso AS "idRecurso",
				b.id_variante AS "idVariante", b.inicio AS "inicio",
				CASE WHEN f.hora_fin IS NOT NULL
					THEN f.hora_fin + (b.fin_con_limpieza - (c.fecha_hora_inicio + NUMTODSINTERVAL(c.duracion_min, 'MINUTE')))
					ELSE b.fin_con_limpieza END AS "fin",
				CASE WHEN f.hora_fin IS NOT NULL THEN f.hora_fin
					WHEN b.origen = 'CITA' THEN c.fecha_hora_inicio + NUMTODSINTERVAL(c.duracion_min, 'MINUTE')
					ELSE NVL(s.hora_fin, s.hora_inicio + NUMTODSINTERVAL(s.tiempo_declarado_min, 'MINUTE')) END AS "finServicio",
				b.estado AS "estado", NVL(c.id_cliente, s.id_cliente) AS "idCliente", s.nombre_cliente_paso AS "nombreClientePaso"
			FROM V_AGENDA_BLOQUES b
			LEFT JOIN CITA c ON b.origen = 'CITA' AND c.id_cita = b.id_origen
			LEFT JOIN ATENCION s ON b.origen = 'ATENCION_SIN_CITA' AND s.id_atencion = b.id_origen
			LEFT JOIN (SELECT id_cita, MAX(hora_fin) AS hora_fin FROM ATENCION
				WHERE id_cita IS NOT NULL AND hora_fin IS NOT NULL GROUP BY id_cita) f ON b.origen = 'CITA' AND f.id_cita = b.id_origen
			WHERE b.id_sede = :idSede AND b.inicio >= :desde AND b.inicio < :hasta
			ORDER BY b.inicio
			""")
	List<FilaBloqueAgenda> findBloques(@Param("idSede") Long idSede, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

}
