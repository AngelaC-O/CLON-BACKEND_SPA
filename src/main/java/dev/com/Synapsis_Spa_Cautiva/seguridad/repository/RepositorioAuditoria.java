package dev.com.Synapsis_Spa_Cautiva.seguridad.repository;

import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioAuditoria extends JpaRepository<Auditoria, Long> {

	@Query(nativeQuery = true, value = """
			SELECT id_auditoria AS "idAuditoria", fecha_hora AS "fechaHora", usuario AS "usuario", rol AS "rol", modulo AS "modulo",
				accion AS "accion", entidad AS "entidad", id_entidad AS "idEntidad", detalle AS "detalle"
			FROM V_AUDITORIA
			WHERE fecha_hora >= :desde AND fecha_hora <= :hasta
			ORDER BY id_auditoria DESC
			""")
	List<FilaAuditoria> consultar(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

}
