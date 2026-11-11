package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSerieComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import jakarta.validation.constraints.NotNull;

/**
 * [It. 2 · CUS63, RF107] Una serie de comprobante de la sede. En el POST van idSede (sin él, la de la sesión), tipo y serie; ultimoNumero lo lleva
 * Oracle (RN47) y solo se lee.
 */
public record SerieDTO(Long idSerie, Long idSede, @NotNull TipoComprobante tipo, String serie, Integer ultimoNumero, EstadoSerieComprobante estado) {
}
