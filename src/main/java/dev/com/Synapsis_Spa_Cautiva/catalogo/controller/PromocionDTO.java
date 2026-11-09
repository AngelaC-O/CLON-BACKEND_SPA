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
 * valor y la vigencia los valida el constructor de Promocion (422). idPromocion y estado los completa el servidor.
 */
public record PromocionDTO(Long idPromocion, @NotBlank @Size(max = 80) String nombre, @NotNull TipoDescuentoPromocion tipoDescuento,
		BigDecimal valor, LocalDate fechaInicio, LocalDate fechaFin, EstadoPromocion estado, List<Long> idsServicio) {
}
