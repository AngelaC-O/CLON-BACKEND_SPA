package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoItemVentaDetalle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cotización sin valor tributario: no se guarda (RN09). precioUnitario, subtotal y total los calcula el servidor. */
public record ProformaDTO(Long idCliente, String nombreCliente, LocalDate fechaValidez, @NotEmpty List<@Valid Item> items, BigDecimal total) {

	public record Item(@NotNull TipoItemVentaDetalle tipoItem, Long idVariante, Long idPaquete, Long idPromocion, @NotNull @Positive Integer cantidad,
			BigDecimal precioUnitario, BigDecimal subtotal, String descripcion) {

		/** [It. 2.1 · nombres] «Servicio · Variante» o el nombre del paquete (en la petición se ignora). */
		public Item conDescripcion(String descripcion) {
			return new Item(tipoItem, idVariante, idPaquete, idPromocion, cantidad, precioUnitario, subtotal, descripcion);
		}

	}

}
