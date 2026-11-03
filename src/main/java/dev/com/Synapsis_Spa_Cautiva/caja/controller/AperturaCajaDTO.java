package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AperturaCajaDTO(@NotNull @PositiveOrZero BigDecimal montoInicial) {
}
