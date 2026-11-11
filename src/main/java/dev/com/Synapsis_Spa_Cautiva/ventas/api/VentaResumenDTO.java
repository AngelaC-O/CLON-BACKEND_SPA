package dev.com.Synapsis_Spa_Cautiva.ventas.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Venta CERRADA del mes para la conciliación (CUS47, RN33: las anuladas no se cuentan). serie y numero son los de su boleta o
 * factura, nulos si no la tiene (RF76). Los campos salen de V_VENTA_VALIDA; el diagrama solo nombra el tipo (DF-71).
 */
public record VentaResumenDTO(Long idVenta, Long idSede, LocalDate fecha, String tipoComprobante, String serie, String numero,
		BigDecimal importe) {
}
