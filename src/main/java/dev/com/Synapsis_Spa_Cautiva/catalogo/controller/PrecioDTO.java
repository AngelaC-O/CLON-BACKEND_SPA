package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import java.math.BigDecimal;

/**
 * [It. 2 · CUS58, RF83] Cuerpo de PATCH /servicios/{idServicio}/variantes/{idVariante}/precio. Un precio nulo o negativo responde 422 desde
 * la variante (RN40).
 */
public record PrecioDTO(BigDecimal precioNuevo) {
}
