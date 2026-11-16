package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoPromocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoDescuentoPromocion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS60, RF84] Promoción o descuento (PORCENTAJE o MONTO) con su vigencia y los servicios a los que aplica (PROMOCION_SERVICIO). El
 * valor y la vigencia los valida el constructor de Promocion (422). idPromocion y estado los completa el servidor. [It. 2.1 · nombres]
 * servicios solo va en las respuestas, con el nombre de cada servicio; en la petición se usa idsServicio (servicios se ignora).
 */
public record PromocionDTO(Long idPromocion, @NotBlank @Size(max = 80) String nombre, @NotNull TipoDescuentoPromocion tipoDescuento,
		BigDecimal valor, LocalDate fechaInicio, LocalDate fechaFin, EstadoPromocion estado, List<Long> idsServicio,
		List<ServicioIncluidoDTO> servicios) {

	public PromocionDTO conServicios(List<ServicioIncluidoDTO> servicios) {
		return new PromocionDTO(idPromocion, nombre, tipoDescuento, valor, fechaInicio, fechaFin, estado, idsServicio, servicios);
	}

}
