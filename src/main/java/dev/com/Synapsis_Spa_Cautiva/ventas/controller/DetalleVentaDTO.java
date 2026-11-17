package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoItemVentaDetalle;

/**
 * [It. 2] idProducto (CUS26) e idPaquete se agregan al final: el JSON de la Iteración 1 solo crece. [It. 2.1 · nombres] descripcion («Servicio ·
 * Variante», el producto o el paquete) y nombreEspecialista (VENTA_DETALLE.id_personal).
 */
public record DetalleVentaDTO(Long idDetalle, TipoItemVentaDetalle tipoItem, Long idAtencion, Long idPromocion, Integer cantidad, BigDecimal precioUnitario,
		BigDecimal descuento, BigDecimal subtotal, Long idPersonal, Long idProducto, Long idPaquete, String descripcion, String nombreEspecialista) {

	public DetalleVentaDTO conNombres(String descripcion, String nombreEspecialista) {
		return new DetalleVentaDTO(idDetalle, tipoItem, idAtencion, idPromocion, cantidad, precioUnitario, descuento, subtotal, idPersonal, idProducto,
				idPaquete, descripcion, nombreEspecialista);
	}

}
