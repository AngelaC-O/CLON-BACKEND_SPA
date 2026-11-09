package dev.com.Synapsis_Spa_Cautiva.agenda.repository;

import java.util.List;
import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.ClientePaquete;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioClientePaquete extends JpaRepository<ClientePaquete, Long> {

	/** Paquetes y membresías ACTIVOS con sesiones por usar, según V_SESIONES_PAQUETE (CUS03, RN36). */
	@Query(nativeQuery = true, value = """
			SELECT cp.* FROM CLIENTE_PAQUETE cp
			JOIN V_SESIONES_PAQUETE s ON s.id_cliente_paquete = cp.id_cliente_paquete
			WHERE cp.id_cliente = :idCliente AND cp.estado = 'ACTIVO' AND s.sesiones_restantes > 0
			ORDER BY cp.fecha_compra
			""")
	List<ClientePaquete> findActivosPorCliente(@Param("idCliente") Long idCliente);

	/** El paquete cubre el servicio si lo incluye PAQUETE_SERVICIO, que escribe CUS59 (RN36, D-04). */
	@Query(nativeQuery = true, value = """
			SELECT cp.* FROM CLIENTE_PAQUETE cp
			JOIN PAQUETE_SERVICIO ps ON ps.id_paquete = cp.id_paquete
			WHERE cp.id_cliente = :idCliente AND ps.id_servicio = :idServicio AND cp.estado = 'ACTIVO'
			ORDER BY cp.fecha_compra
			FETCH FIRST 1 ROW ONLY
			""")
	Optional<ClientePaquete> findActivoPorCliente(@Param("idCliente") Long idCliente, @Param("idServicio") Long idServicio);

	/** Las sesiones usadas se cuentan, no se guardan (RN36, RN37). */
	@Query(nativeQuery = true, value = "SELECT sesiones_usadas FROM V_SESIONES_PAQUETE WHERE id_cliente_paquete = :idClientePaquete")
	Integer contarSesionesUsadas(@Param("idClientePaquete") Long idClientePaquete);

}
