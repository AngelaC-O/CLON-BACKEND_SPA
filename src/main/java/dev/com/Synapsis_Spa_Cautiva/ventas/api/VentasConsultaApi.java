package dev.com.Synapsis_Spa_Cautiva.ventas.api;

import java.time.LocalDateTime;
import java.util.List;

public interface VentasConsultaApi {

	/** Lo esperado de la caja por medio de pago: V_CAJA_ESPERADO (RF63, RN33). */
	List<TotalMedioPago> totalesPorMedioPago(Long idCaja);

	/** Comprobantes de la sede hasta esa fecha que SUNAT aún no acepta (RN46). */
	List<ComprobantePendiente> comprobantesPendientes(Long idSede, LocalDateTime fecha);

}
