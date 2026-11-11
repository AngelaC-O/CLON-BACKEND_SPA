package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoItemVentaDetalle;

/** [It. 2] idProducto (CUS26) e idPaquete se agregan al final: el JSON de la Iteración 1 solo crece. */
public record DetalleVentaDTO(Long idDetalle, TipoItemVentaDetalle tipoItem, Long idAtencion, Long idPromocion, Integer cantidad, BigDecimal precioUnitario,
		BigDecimal descuento, BigDecimal subtotal, Long idPersonal, Long idProducto, Long idPaquete) {
}
