package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.math.BigDecimal;

/** esperado sale de V_CAJA_ESPERADO; aprobadoPasarela, de las transacciones APROBADAS del día (RF63, RF64). */
public record EsperadoMedioDTO(Long idMedioPago, String medioPago, BigDecimal esperado, BigDecimal aprobadoPasarela) {
}
