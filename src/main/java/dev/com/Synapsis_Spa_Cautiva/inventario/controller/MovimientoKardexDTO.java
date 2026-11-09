package dev.com.Synapsis_Spa_Cautiva.inventario.controller;

import java.time.LocalDateTime;

/** CUS35 (RF50, RN18): una fila de V_KARDEX. cantidad lleva el signo del movimiento; el origen es la venta, la compra o la atención. */
public record MovimientoKardexDTO(Long idMovimiento, LocalDateTime fechaHora, String tipo, String motivo, Integer cantidad, Integer saldo,
		String usuario, Long idDetalleVenta, Long idDetalleCompra, Long idAtencion) {
}
