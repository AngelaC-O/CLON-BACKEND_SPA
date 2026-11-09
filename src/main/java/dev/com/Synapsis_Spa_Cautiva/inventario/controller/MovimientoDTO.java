package dev.com.Synapsis_Spa_Cautiva.inventario.controller;

import java.time.LocalDateTime;

import dev.com.Synapsis_Spa_Cautiva.inventario.domain.TipoMovimientoKardex;

/**
 * CUS33, CUS34: el movimiento de kardex que se registró. En la salida se envían cantidad, motivo e idAtencion (opcional). saldo, usuario y
 * fecha los fija TRG_KARDEX_APLICAR. estaEnMinimo avisa que el producto quedó en su stock mínimo de la sede (RF47).
 */
public record MovimientoDTO(Long idMovimiento, Long idProducto, Long idSede, TipoMovimientoKardex tipo, Integer cantidad, String motivo,
		Long idAtencion, Integer saldo, LocalDateTime fechaHora, Boolean estaEnMinimo) {
}
