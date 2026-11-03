package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Clienta registrada o de paso (RN35), con un solo pago, como en la pantalla de CUS13. */
public record VentaRapidaDTO(Long idCliente, @Size(max = 80) String nombreClientePaso, @NotNull Long idPersonal, @NotNull Long idRecurso,
		@NotNull Long idVariante, @NotNull @Positive Integer tiempoDeclaradoMin, @NotNull Long idMedioPago, @NotNull @Positive BigDecimal monto,
		@Pattern(regexp = "POS|QR|LINK|API") String canal, @NotNull TipoComprobante tipo, Long idTipoDocComprador, @Size(max = 15) String nroDocComprador,
		@Size(max = 150) String nombreComprador) {
}
