package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdelantoDTO(@NotNull @Positive BigDecimal adelantoMonto, @NotNull Long idMedioPago) {
}
