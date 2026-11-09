package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [It. 2] Proyección de V_VENTA_LINEA para la ficha de la clienta (CUS03): una línea PRODUCTO de una venta CERRADA. fecha es un DATE de Oracle
 * (TRUNC de la fecha de la venta), que el driver entrega como LocalDateTime.
 */
public interface FilaProductoComprado {

	Long getIdVenta();

	LocalDateTime getFecha();

	Long getIdProducto();

	String getProducto();

	Integer getCantidad();

	BigDecimal getPrecioUnitario();

	BigDecimal getSubtotal();

}
