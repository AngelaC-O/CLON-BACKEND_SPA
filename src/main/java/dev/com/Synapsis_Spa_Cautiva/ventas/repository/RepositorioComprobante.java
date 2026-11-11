package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioComprobante extends JpaRepository<Comprobante, Long> {

	/** Comprobantes del mes de toda la empresa (un solo RUC), para conciliar con SUNAT (CUS47, RF75, RN31). Lee V_COMPROBANTES_MES. */
	@Query(nativeQuery = true, value = """
			SELECT id_sede AS "idSede", tipo AS "tipo", serie AS "serie", numero AS "numero", fecha_emision AS "fechaEmision",
				id_venta AS "idVenta", importe AS "importe", estado_venta AS "estadoVenta", estado_sunat AS "estadoSunat",
				nc_serie AS "ncSerie", nc_numero AS "ncNumero", nc_estado_sunat AS "ncEstadoSunat"
			FROM V_COMPROBANTES_MES
			WHERE anio = :anio AND mes = :mes
			ORDER BY serie, numero
			""")
	List<FilaComprobanteMes> findDelMes(@Param("anio") Integer anio, @Param("mes") Integer mes);

	/** Boletas, facturas y [It. 2 · CUS23] notas de crédito que aún esperan el envío a SUNAT (RN46): id_documento es el id_comprobante. */
	@Query(nativeQuery = true, value = """
			SELECT id_documento AS "idDocumento", serie AS "serie", numero AS "numero", fecha_limite AS "fechaLimite"
			FROM V_CPE_PENDIENTES
			WHERE estado_sunat = 'PENDIENTE'
			ORDER BY fecha_limite, id_documento
			""")
	List<FilaCpePendiente> findPendientesDeEnvio();

	/** [It. 2 · CUS50, RN46] Cuántos comprobantes y notas de la sede siguen PENDIENTES con la fecha límite de envío a SUNAT hasta esa fecha. */
	@Query(nativeQuery = true, value = """
			SELECT COUNT(*) FROM V_CPE_PENDIENTES
			WHERE id_sede = :idSede AND estado_sunat = 'PENDIENTE' AND fecha_limite <= :limite
			""")
	long contarPendientesPorVencer(@Param("idSede") Long idSede, @Param("limite") LocalDate limite);

}
