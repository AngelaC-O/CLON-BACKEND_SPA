package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record InsumoDTO(@NotNull Long idProducto, @NotNull BigDecimal cantidadEstimada) {
}
