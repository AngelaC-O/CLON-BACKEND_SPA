package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoItemVentaDetalle;

public record DetalleVentaDTO(Long idDetalle, TipoItemVentaDetalle tipoItem, Long idAtencion, Long idPromocion, Integer cantidad, BigDecimal precioUnitario,
		BigDecimal descuento, BigDecimal subtotal, Long idPersonal) {
}
