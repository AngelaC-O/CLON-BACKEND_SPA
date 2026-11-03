package dev.com.Synapsis_Spa_Cautiva.pagos.service;

/** Lo único que el dominio recibe del webhook. estado: APROBADA, RECHAZADA o PENDIENTE (aún se puede pagar; no cambia nada). */
public record NotificacionPasarela(String codigoOperacion, String estado) {
}
