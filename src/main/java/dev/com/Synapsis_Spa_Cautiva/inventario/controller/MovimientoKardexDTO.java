package dev.com.Synapsis_Spa_Cautiva.inventario.controller;

import java.time.LocalDateTime;

/**
 * CUS35 (RF50, RN18): una fila de V_KARDEX. cantidad lleva el signo del movimiento; el origen es la venta, la compra o la atención.
 * [It. 2.1 · nombres] comprobante (serie-número de la boleta o factura de la venta) y documentoCompra (documento del proveedor) salen de
 * V_KARDEX v5.2; nulos si el movimiento no viene de una venta o de una compra. Con ellos el front-end arma el origen (Duda 21).
 */
public record MovimientoKardexDTO(Long idMovimiento, LocalDateTime fechaHora, String tipo, String motivo, Integer cantidad, Integer saldo,
		String usuario, Long idDetalleVenta, Long idDetalleCompra, Long idAtencion, String comprobante, String documentoCompra) {
}
