package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.math.BigDecimal;

/** montoEsperado y diferencia los calcula Oracle. */
public record ArqueoCalculadoDTO(Long idMedioPago, BigDecimal montoEsperado, BigDecimal montoContado, BigDecimal diferencia, String observacion) {
}
