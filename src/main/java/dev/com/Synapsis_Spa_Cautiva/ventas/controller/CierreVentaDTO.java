package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CierreVentaDTO(@NotNull TipoComprobante tipo, Long idTipoDocComprador, @Size(max = 15) String nroDocComprador,
		@Size(max = 150) String nombreComprador) {
}
