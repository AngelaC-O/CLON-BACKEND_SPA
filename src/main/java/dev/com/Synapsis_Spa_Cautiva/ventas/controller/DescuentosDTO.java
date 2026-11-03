package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;

/** El adelanto no viene aquí: sale de la cita de la atención (DF-44). */
public record DescuentosDTO(Long idPromocion, @PositiveOrZero BigDecimal saldoFavorAplicado) {
}
