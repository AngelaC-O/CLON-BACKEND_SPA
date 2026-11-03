package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioVenta extends JpaRepository<Venta, Long> {

	@Query(nativeQuery = true, value = """
			SELECT id_medio_pago AS "idMedioPago", medio_pago AS "medioPago", esperado AS "esperado"
			FROM V_CAJA_ESPERADO
			WHERE id_caja = :idCaja
			ORDER BY id_medio_pago
			""")
	List<FilaEsperadoCaja> findEsperadoPorCaja(@Param("idCaja") Long idCaja);

	@Query(nativeQuery = true, value = """
			SELECT id_documento AS "idDocumento", tipo AS "tipo", serie AS "serie", numero AS "numero", estado_sunat AS "estadoSunat",
				fecha_limite AS "fechaLimite"
			FROM V_CPE_PENDIENTES
			WHERE id_sede = :idSede AND fecha_emision <= :fecha
			ORDER BY fecha_limite, id_documento
			""")
	List<FilaComprobantePendiente> findComprobantesPendientes(@Param("idSede") Long idSede, @Param("fecha") LocalDate fecha);

}
