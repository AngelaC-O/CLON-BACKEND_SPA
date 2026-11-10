package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [It. 2 · CUS47] Proyección de V_VENTA_VALIDA y de V_VENTAS_SIN_COMPROBANTE: una venta CERRADA del mes. fecha es un DATE de Oracle, que el
 * driver entrega como LocalDateTime.
 */
public interface FilaVentaMes {

	Long getIdVenta();

	Long getIdSede();

	LocalDateTime getFecha();

	String getTipoComprobante();

	String getSerie();

	String getNumero();

	BigDecimal getImporte();

}
