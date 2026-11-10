package dev.com.Synapsis_Spa_Cautiva.ventas.api;

import java.time.LocalDateTime;
import java.util.List;

public interface VentasConsultaApi {

	/** Lo esperado de la caja por medio de pago: V_CAJA_ESPERADO (RF63, RN33). */
	List<TotalMedioPago> totalesPorMedioPago(Long idCaja);

	/** Comprobantes de la sede hasta esa fecha que SUNAT aún no acepta (RN46). */
	List<ComprobantePendiente> comprobantesPendientes(Long idSede, LocalDateTime fecha);

	/** [It. 2 · CUS47, RN33] Las ventas CERRADAS del mes de toda la empresa (V_VENTA_VALIDA): las anuladas no cuentan. */
	List<VentaResumenDTO> ventasDelMes(Integer anio, Integer mes);

	/** [It. 2 · CUS47, RF75, RN31] Las boletas y facturas del mes con su estado en SUNAT y su nota de crédito (V_COMPROBANTES_MES). */
	List<ComprobanteSunatDTO> comprobantesDelMes(Integer anio, Integer mes);

	/** [It. 2 · CUS47, RF76] Las ventas CERRADAS del mes sin boleta ni factura (V_VENTAS_SIN_COMPROBANTE); con los triggers, ninguna. */
	List<VentaResumenDTO> ventasSinComprobante(Integer anio, Integer mes);

}
