package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ArqueoDTO(List<@Valid Egreso> egresos, @NotEmpty List<@Valid Medio> arqueos) {

	public record Egreso(@NotBlank @Size(max = 120) String concepto, @NotNull @Positive BigDecimal monto) {
	}

	public record Medio(@NotNull Long idMedioPago, @NotNull @PositiveOrZero BigDecimal montoContado, @Size(max = 300) String observacion) {
	}

}
