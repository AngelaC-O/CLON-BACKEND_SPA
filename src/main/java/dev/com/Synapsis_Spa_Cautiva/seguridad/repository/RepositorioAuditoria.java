package dev.com.Synapsis_Spa_Cautiva.seguridad.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioAuditoria extends JpaRepository<Auditoria, Long> {

	/** CUS55, RF80: auditoría entre dos fechas, incluidas, con filtros opcionales de usuario, módulo y entidad. Lee V_AUDITORIA. */
	default List<FilaAuditoria> findByFiltros(Long idUsuario, String modulo, String entidad, LocalDate fechaInicio, LocalDate fechaFin) {
		return findByFiltrosEntre(idUsuario, modulo, entidad, fechaInicio.atStartOfDay(), fechaFin.plusDays(1).atStartOfDay());
	}

	@Query(nativeQuery = true, value = """
			SELECT id_auditoria AS "idAuditoria", fecha_hora AS "fechaHora", usuario AS "usuario", rol AS "rol", modulo AS "modulo",
				accion AS "accion", entidad AS "entidad", id_entidad AS "idEntidad", detalle AS "detalle"
			FROM V_AUDITORIA
			WHERE fecha_hora >= :desde AND fecha_hora < :hasta
			AND (:idUsuario IS NULL OR usuario = (SELECT nombre_usuario FROM USUARIO WHERE id_usuario = :idUsuario))
			AND (:modulo IS NULL OR modulo = :modulo)
			AND (:entidad IS NULL OR entidad = :entidad)
			ORDER BY id_auditoria DESC
			""")
	List<FilaAuditoria> findByFiltrosEntre(@Param("idUsuario") Long idUsuario, @Param("modulo") String modulo,
			@Param("entidad") String entidad, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

	@Query(nativeQuery = true, value = """
			SELECT id_auditoria AS "idAuditoria", fecha_hora AS "fechaHora", usuario AS "usuario", rol AS "rol", modulo AS "modulo",
				accion AS "accion", entidad AS "entidad", id_entidad AS "idEntidad", detalle AS "detalle"
			FROM V_AUDITORIA
			WHERE fecha_hora >= :desde AND fecha_hora <= :hasta
			ORDER BY id_auditoria DESC
			""")
	List<FilaAuditoria> consultar(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

}
