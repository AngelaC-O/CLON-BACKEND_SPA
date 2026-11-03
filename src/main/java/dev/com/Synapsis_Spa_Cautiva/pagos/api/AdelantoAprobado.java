package dev.com.Synapsis_Spa_Cautiva.pagos.api;

import java.math.BigDecimal;

/** Evento síncrono, en la misma transacción: pagos no conoce a agenda (RN04). */
public record AdelantoAprobado(Long idCita, Long idMedioPago, BigDecimal monto) {
}
