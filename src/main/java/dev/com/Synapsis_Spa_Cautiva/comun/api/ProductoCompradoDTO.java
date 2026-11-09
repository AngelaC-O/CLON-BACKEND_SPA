package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [It. 2 · CUS03, RF02] Un producto que la clienta compró: una línea PRODUCTO de una venta CERRADA (V_VENTA_LINEA). El diagrama solo nombra el
 * tipo; los campos salen de la vista.
 */
public record ProductoCompradoDTO(Long idVenta, LocalDate fecha, Long idProducto, String producto, Integer cantidad, BigDecimal precioUnitario,
		BigDecimal subtotal) {
}
